package com.eslam.bakingapp.features.login.data.repository

import android.content.Context
import android.provider.Settings
import com.eslam.bakingapp.core.common.phone.KenyaPhoneNormalizer
import com.eslam.bakingapp.core.common.result.Result
import com.eslam.bakingapp.core.network.adapter.ApiErrorMapper
import com.eslam.bakingapp.core.network.api.VyceAuthApi
import com.eslam.bakingapp.core.network.model.VyceForgotPinConfirmRequest
import com.eslam.bakingapp.core.network.model.VyceForgotPinRequest
import com.eslam.bakingapp.core.network.model.VyceLoginRequest
import com.eslam.bakingapp.core.network.model.VyceSetCredentialsRequest
import com.eslam.bakingapp.core.network.model.VyceVerifyDeviceOtpRequest
import com.eslam.bakingapp.core.network.model.VyceVerifyMigrateOtpRequest
import com.eslam.bakingapp.core.security.SecureTokenManager
import com.eslam.bakingapp.features.login.domain.model.LoginCredentials
import com.eslam.bakingapp.features.login.domain.model.LoginResult
import com.eslam.bakingapp.features.login.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: SecureTokenManager,
    private val authApi: VyceAuthApi
) : AuthRepository {

    override suspend fun login(credentials: LoginCredentials): Result<LoginResult> {
        return try {
            val imei = deviceImei()
            val identifier = credentials.identifier.trim()
            val request = if (identifier.all { it.isDigit() }) {
                val mobile = normalizeMobile(identifier)
                    ?: return Result.Error(
                        IllegalArgumentException("invalid_mobile"),
                        "Enter a valid Kenyan mobile number."
                    )
                VyceLoginRequest(
                    mobileCountryCode = "254",
                    mobile = mobile,
                    pin = credentials.pin,
                    imei = imei,
                    platform = "ANDROID"
                )
            } else {
                VyceLoginRequest(
                    username = identifier,
                    pin = credentials.pin,
                    imei = imei,
                    platform = "ANDROID"
                )
            }
            val response = authApi.login(request)
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("login_http_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            val body = response.body()
            val data = body?.data
            if (data?.mustSetCredentials == true) {
                val mobile = data.mobile ?: request.mobile ?: normalizeMobile(identifier)
                val cc = data.mobileCountryCode ?: "254"
                return Result.Success(
                    LoginResult(
                        userId = data.externalId.orEmpty(),
                        email = mobile,
                        name = "",
                        accessToken = "",
                        refreshToken = "",
                        expiresIn = 0,
                        mustSetCredentials = true,
                        otpSent = data.otpSent == true,
                        maskedMobile = data.maskedMobile,
                        mobileCountryCode = cc,
                        mobile = mobile
                    )
                )
            }
            if (data?.deviceOtpRequired == true) {
                val mobile = data.mobile ?: request.mobile ?: normalizeMobile(identifier)
                val cc = data.mobileCountryCode ?: "254"
                return Result.Success(
                    LoginResult(
                        userId = data.externalId.orEmpty(),
                        email = mobile,
                        name = "",
                        accessToken = "",
                        refreshToken = "",
                        expiresIn = 0,
                        deviceOtpRequired = true,
                        otpSent = data.otpSent == true,
                        maskedMobile = data.maskedMobile,
                        mobileCountryCode = cc,
                        mobile = mobile
                    )
                )
            }
            val token = data?.token ?: body?.token
            if (token.isNullOrBlank()) {
                return Result.Error(
                    IllegalStateException("token_missing"),
                    body?.message ?: "Login failed"
                )
            }
            val externalId = data?.externalId ?: body?.externalId.orEmpty()
            val result = LoginResult(
                userId = externalId,
                email = request.mobile ?: identifier,
                name = "VycePay User",
                accessToken = token,
                refreshToken = data?.refreshToken.orEmpty(),
                expiresIn = data?.expiresIn ?: body?.expiresIn ?: 600L
            )
            persistLogin(result)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    override suspend fun verifyDeviceOtp(
        mobileCountryCode: String,
        mobile: String,
        otpCode: String
    ): Result<Unit> {
        return try {
            val response = authApi.verifyDeviceOtp(
                VyceVerifyDeviceOtpRequest(
                    mobileCountryCode = mobileCountryCode,
                    mobile = mobile,
                    otpCode = otpCode,
                    imei = deviceImei(),
                    platform = "ANDROID"
                )
            )
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("device_otp_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    override suspend fun verifyMigrateOtp(
        mobileCountryCode: String,
        mobile: String,
        otpCode: String
    ): Result<LoginResult> {
        return try {
            val response = authApi.verifyMigrateOtp(
                VyceVerifyMigrateOtpRequest(
                    mobileCountryCode = mobileCountryCode,
                    mobile = mobile,
                    otpCode = otpCode
                )
            )
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("migrate_otp_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            val body = response.body()
            val data = body?.data
            val token = data?.token ?: body?.token
            if (token.isNullOrBlank()) {
                return Result.Error(IllegalStateException("token_missing"), body?.message ?: "Failed")
            }
            val result = LoginResult(
                userId = data?.externalId ?: body?.externalId.orEmpty(),
                email = mobile,
                name = "VycePay User",
                accessToken = token,
                refreshToken = data?.refreshToken.orEmpty(),
                expiresIn = data?.expiresIn ?: 600L,
                mustSetCredentials = true,
                mobileCountryCode = mobileCountryCode,
                mobile = mobile
            )
            persistLogin(result)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    override suspend fun setCredentials(username: String, pin: String): Result<Unit> {
        return try {
            val response = authApi.setCredentials(
                VyceSetCredentialsRequest(
                    username = username,
                    pin = pin,
                    imei = deviceImei(),
                    platform = "ANDROID"
                )
            )
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("credentials_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    override suspend fun requestForgotPin(mobileCountryCode: String, mobile: String): Result<Unit> {
        return try {
            val response = authApi.forgotPinRequest(
                VyceForgotPinRequest(mobileCountryCode = mobileCountryCode, mobile = mobile)
            )
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("forgot_pin_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    override suspend fun confirmForgotPin(
        mobileCountryCode: String,
        mobile: String,
        otpCode: String,
        newPin: String
    ): Result<Unit> {
        return try {
            val response = authApi.forgotPinConfirm(
                VyceForgotPinConfirmRequest(
                    mobileCountryCode = mobileCountryCode,
                    mobile = mobile,
                    otpCode = otpCode,
                    newPin = newPin,
                    imei = deviceImei(),
                    platform = "ANDROID"
                )
            )
            if (!response.isSuccessful) {
                return Result.Error(
                    IllegalStateException("forgot_confirm_${response.code()}"),
                    ApiErrorMapper.toMessage(response)
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            runCatching { authApi.logout() }
            tokenManager.clearAll()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    override fun isLoggedIn(): Boolean = tokenManager.hasValidToken()

    override suspend fun refreshToken(): Result<LoginResult> {
        return try {
            val response = authApi.refreshToken()
            if (!response.isSuccessful) {
                return Result.Error(IllegalStateException("refresh_http_${response.code()}"), "Please sign in again")
            }
            val body = response.body()
            val data = body?.data
            val token = data?.token ?: body?.token ?: data?.accessToken
            if (token.isNullOrBlank()) {
                return Result.Error(IllegalStateException("refresh_missing"), "Please sign in again")
            }
            val result = LoginResult(
                userId = tokenManager.getUserId().orEmpty(),
                email = tokenManager.getUserEmail().orEmpty(),
                name = tokenManager.getUserName().orEmpty(),
                accessToken = token,
                refreshToken = data?.refreshToken.orEmpty(),
                expiresIn = data?.expiresIn ?: body?.expiresIn ?: 600L
            )
            tokenManager.saveTokens(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                expiresIn = result.expiresIn
            )
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e, "Check your connection and try again.")
        }
    }

    private fun persistLogin(result: LoginResult) {
        tokenManager.saveTokens(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            expiresIn = result.expiresIn
        )
        tokenManager.saveUserInfo(
            userId = result.userId,
            email = result.email,
            name = result.name
        )
    }

    private fun normalizeMobile(phone: String): String? =
        KenyaPhoneNormalizer.toNationalMobile(phone)

    private fun deviceImei(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
    }
}
