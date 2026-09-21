package com.eslam.bakingapp.features.signup.data.repository

import android.content.Context
import android.provider.Settings
import com.eslam.bakingapp.core.common.phone.KenyaPhoneNormalizer
import com.eslam.bakingapp.core.common.result.Result
import com.eslam.bakingapp.core.network.adapter.ApiErrorMapper
import com.eslam.bakingapp.core.network.api.SignupKycApi
import com.eslam.bakingapp.core.network.api.VyceAuthApi
import com.eslam.bakingapp.core.network.api.VyceBusinessApi
import com.eslam.bakingapp.core.network.model.signup.DocExtractResponseJson
import com.eslam.bakingapp.core.network.model.signup.EnglishExtractJson
import com.eslam.bakingapp.core.network.model.signup.FaceMatchRequestJson
import com.eslam.bakingapp.core.network.model.signup.AnalysisDetailsJson
import com.eslam.bakingapp.core.network.model.signup.CoverageCheckJson
import com.eslam.bakingapp.core.network.model.signup.DocumentExtractRequestJson
import com.eslam.bakingapp.core.network.model.signup.LivenessCheckRequestJson
import com.eslam.bakingapp.core.network.model.signup.LivenessCheckResponseJson
import com.eslam.bakingapp.core.network.model.signup.MovementAnalysisJson
import com.eslam.bakingapp.core.network.model.signup.PhysicalPropertiesJson
import com.eslam.bakingapp.core.network.model.signup.ReproductionIndicatorsJson
import com.eslam.bakingapp.core.network.model.VyceKycSubmitRequest
import com.eslam.bakingapp.core.network.model.VyceMobileRequest
import com.eslam.bakingapp.core.network.model.VyceSetCredentialsRequest
import com.eslam.bakingapp.core.network.model.VyceVerifyOtpRequest
import com.eslam.bakingapp.core.security.SecureTokenManager
import com.eslam.bakingapp.features.signup.domain.model.DocExtractResult
import com.eslam.bakingapp.features.signup.domain.model.ExtractedDari
import com.eslam.bakingapp.features.signup.domain.model.ExtractedEnglish
import com.eslam.bakingapp.features.signup.domain.model.ExtractedInfo
import com.eslam.bakingapp.features.signup.domain.model.FaceMatchResult
import com.eslam.bakingapp.features.signup.domain.model.LivenessAnalysisDetails
import com.eslam.bakingapp.features.signup.domain.model.LivenessCheckResult
import com.eslam.bakingapp.features.signup.domain.model.LivenessCoverageCheck
import com.eslam.bakingapp.features.signup.domain.model.LivenessMovementAnalysis
import com.eslam.bakingapp.features.signup.domain.model.LivenessPhysicalProperties
import com.eslam.bakingapp.features.signup.domain.model.LivenessReproductionIndicators
import com.eslam.bakingapp.features.signup.domain.model.OtpSendResult
import com.eslam.bakingapp.features.signup.domain.model.OtpVerifyResult
import com.eslam.bakingapp.features.signup.domain.repository.SignupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: VyceAuthApi,
    private val signupKycApi: SignupKycApi,
    private val businessApi: VyceBusinessApi,
    private val tokenManager: SecureTokenManager
) : SignupRepository {

    override suspend fun sendOtp(phone: String): Result<OtpSendResult> {
        return try {
            val mobile = normalizeMobile(phone)
                ?: return Result.Error(
                    IllegalArgumentException("invalid_mobile"),
                    "Enter a valid Kenyan mobile number."
                )
            val response = authApi.register(
                VyceMobileRequest(mobileCountryCode = "254", mobile = mobile)
            )
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("otp_http_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            Result.Success(
                OtpSendResult(
                    otpId = "signup-$mobile",
                    authToken = ""
                )
            )
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun verifyOtp(
        otpId: String,
        otpCode: String,
        phone: String
    ): Result<OtpVerifyResult> {
        return try {
            val mobile = normalizeMobile(phone)
                ?: return Result.Error(
                    IllegalArgumentException("invalid_mobile"),
                    "Enter a valid Kenyan mobile number."
                )
            val imei = deviceImei()
            val response = authApi.verifyOtp(
                VyceVerifyOtpRequest(
                    mobileCountryCode = "254",
                    mobile = mobile,
                    otpCode = otpCode,
                    imei = imei,
                    platform = "ANDROID"
                )
            )
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("otp_verify_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            val body = response.body()
            val data = body?.data
            val token = data?.token ?: body?.token
            if (token.isNullOrBlank()) {
                return Result.Error(
                    IllegalStateException("token_missing"),
                    body?.message ?: "OTP verification failed"
                )
            }
            tokenManager.saveTokens(
                accessToken = token,
                refreshToken = data?.refreshToken.orEmpty(),
                expiresIn = data?.expiresIn ?: body?.expiresIn ?: 600L
            )
            val externalId = data?.externalId ?: body?.externalId ?: mobile
            tokenManager.saveUserInfo(
                userId = externalId,
                email = mobile,
                name = "VycePay User"
            )
            Result.Success(OtpVerifyResult(success = true))
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun docLiveness(base64Video: String): Result<LivenessCheckResult> {
        return try {
            val response = signupKycApi.documentLiveness(
                LivenessCheckRequestJson(base64Video = base64Video)
            )
            mapLivenessResponse(response)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun docExtract(base64Image: String): Result<DocExtractResult> {
        return try {
            val response = signupKycApi.documentExtract(
                DocumentExtractRequestJson(base64Image = base64Image)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(mapDocExtract(body))
            } else {
                Result.Error(
                    IllegalStateException("extract"),
                    ApiErrorMapper.toMessage(response)
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun faceLiveness(base64Video: String): Result<LivenessCheckResult> {
        return try {
            val response = signupKycApi.faceLiveness(
                LivenessCheckRequestJson(base64Video = base64Video)
            )
            mapLivenessResponse(response)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun faceMatch(
        idCardBase64: String,
        selfieBase64: String
    ): Result<FaceMatchResult> {
        return try {
            val response = signupKycApi.faceMatch(
                FaceMatchRequestJson(idCardBase64 = idCardBase64, selfieBase64 = selfieBase64)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(
                    FaceMatchResult(
                        match = body.match,
                        similarity = parseSimilarity(body.similarity)
                    )
                )
            } else {
                Result.Error(
                    IllegalStateException("face_match"),
                    ApiErrorMapper.toMessage(response)
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun register(
        username: String,
        pin: String,
        termsAccepted: Boolean,
        firstName: String,
        lastName: String,
        email: String,
        kraPin: String,
        nationalId: String,
        gender: String,
        dateOfBirthIso: String,
        employmentDetail: String,
        monthlyIncome: String,
        physicalAddress: String,
        frontDocBase64: String?,
        backDocBase64: String?,
        selfieBase64: String?
    ): Result<Unit> {
        if (!termsAccepted) return Result.Error(IllegalStateException("terms"), "Terms must be accepted")
        return try {
            val imei = deviceImei()
            val credResp = authApi.setCredentials(
                VyceSetCredentialsRequest(
                    username = username.trim(),
                    pin = pin,
                    imei = imei,
                    platform = "ANDROID"
                )
            )
            if (!credResp.isSuccessful) {
                return Result.Error(
                    IllegalStateException("credentials_${credResp.code()}"),
                    ApiErrorMapper.toMessage(credResp)
                )
            }
            val mobile = tokenManager.getUserEmail().orEmpty().ifBlank { "712345678" }
            val response = businessApi.submitKyc(
                VyceKycSubmitRequest(
                    firstName = firstName,
                    middleName = null,
                    lastName = lastName,
                    birthday = dateOfBirthIso,
                    gender = if (gender.equals("female", true)) 2 else 1,
                    countryCode = "254",
                    mobile = mobile,
                    idType = "101",
                    idNumber = nationalId,
                    frontSidePhoto = frontDocBase64.orEmpty(),
                    backSidePhoto = backDocBase64.orEmpty(),
                    selfiePhoto = selfieBase64.orEmpty(),
                    address = physicalAddress,
                    kraPin = kraPin,
                    email = email
                )
            )
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(
                    IllegalStateException("kyc_submit_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    private fun normalizeMobile(phone: String): String? =
        KenyaPhoneNormalizer.toNationalMobile(phone)

    private fun deviceImei(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
    }

    private fun mapLivenessResponse(
        response: Response<LivenessCheckResponseJson>
    ): Result<LivenessCheckResult> {
        if (!response.isSuccessful) {
            return Result.Error(
                IllegalStateException("liveness_http_${response.code()}"),
                ApiErrorMapper.toMessage(response)
            )
        }
        val body = response.body()
            ?: return Result.Error(IllegalStateException("liveness_body"), "Empty liveness response")
        return Result.Success(mapLivenessCheckResult(body))
    }

    private fun mapLivenessCheckResult(body: LivenessCheckResponseJson): LivenessCheckResult {
        return LivenessCheckResult(
            livenessResult = body.livenessResult,
            confidenceScore = body.confidenceScore,
            recommendations = body.recommendations,
            analysisDetails = body.analysisDetails?.let { mapAnalysisDetails(it) },
            riskFactors = body.riskFactors.orEmpty(),
            processingNotes = body.processingNotes,
            messages = body.messages
        )
    }

    private fun mapAnalysisDetails(json: AnalysisDetailsJson): LivenessAnalysisDetails {
        return LivenessAnalysisDetails(
            physicalProperties = json.physicalProperties?.let { mapPhysical(it) },
            movementAnalysis = json.movementAnalysis?.let { mapMovement(it) },
            reproductionIndicators = json.reproductionIndicators?.let { mapReproduction(it) },
            coverageCheck = json.coverageCheck?.let { mapCoverage(it) }
        )
    }

    private fun mapPhysical(j: PhysicalPropertiesJson) = LivenessPhysicalProperties(
        paperTextureVisible = j.paperTextureVisible,
        naturalShadows = j.naturalShadows,
        depthPerception = j.depthPerception,
        edgeThickness = j.edgeThickness
    )

    private fun mapMovement(j: MovementAnalysisJson) = LivenessMovementAnalysis(
        naturalMovement = j.naturalMovement,
        handTremors = j.handTremors,
        documentFlexibility = j.documentFlexibility
    )

    private fun mapReproduction(j: ReproductionIndicatorsJson) = LivenessReproductionIndicators(
        screenArtifacts = j.screenArtifacts,
        digitalCompression = j.digitalCompression,
        uniformLighting = j.uniformLighting,
        glassReflection = j.glassReflection
    )

    private fun mapCoverage(j: CoverageCheckJson) = LivenessCoverageCheck(
        allFourEdgesVisible = j.allFourEdgesVisible,
        documentFullyInFrame = j.documentFullyInFrame,
        sufficientVideoQuality = j.sufficientVideoQuality
    )

    private fun mapDocExtract(json: DocExtractResponseJson): DocExtractResult {
        val info = json.resolvedExtractedInfo()
        val en = info?.english
        return DocExtractResult(
            extractedInfo = ExtractedInfo(
                english = en?.let { mapEnglish(it) },
                dari = info?.dari?.let { d ->
                    ExtractedDari(
                        name = d.name,
                        fatherName = d.fatherName,
                        grandfatherName = d.grandfatherName,
                        idNumber = d.idNumber,
                        dateOfBirth = d.dateOfBirth
                    )
                }
            )
        )
    }

    private fun mapEnglish(e: EnglishExtractJson) = ExtractedEnglish(
        name = e.name,
        fatherName = e.fatherName,
        grandfatherName = e.grandfatherName,
        dateOfBirth = e.dateOfBirth,
        placeOfBirth = e.placeOfBirth,
        idNumber = e.idNumber,
        issue = e.issue,
        mrzExpiryDate = e.mrzExpiryDate,
        gender = e.gender,
        mrz_line_1 = e.mrzLine1,
        mrz_line_2 = e.mrzLine2,
        mrz_line_3 = e.mrzLine3,
        currentResidence = e.currentResidence
    )

    private fun parseSimilarity(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().removeSuffix("%").trim()
        cleaned.toDoubleOrNull()?.let { return it }
        return cleaned.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
    }
}
