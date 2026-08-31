package com.vycepay.wallet.application.facade;

import com.vycepay.common.choicebank.errors.ChoiceBankResult;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.security.port.SensitiveDataEncryptionPort;
import com.vycepay.wallet.application.WalletAccountContext;
import com.vycepay.wallet.domain.model.Customer;
import com.vycepay.wallet.domain.model.KycVerification;
import com.vycepay.wallet.infrastructure.persistence.CustomerRepository;
import com.vycepay.wallet.infrastructure.persistence.KycVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Orchestrates Choice Bank account management APIs (query, short code, contact, SME sub-account, verify OTP).
 * Contact-identity fields for email APIs are taken from KYC, not from the client.
 * Returns {@link ChoiceBankResult} so controllers can prefer Choice {@code msg} for customer display.
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class AccountManagementFacade {

    private static final Logger log = LoggerFactory.getLogger(AccountManagementFacade.class);

    private static final String PATH_GET_ACCOUNT_DETAILS = "query/getAccountDetails";
    private static final String PATH_QUERY_ACCOUNT_LIST = "account/queryAccountListByUserId";
    private static final String PATH_GET_ABNORMAL_ACCOUNT_LIST = "query/getAbnormalAccountList";
    private static final String PATH_APPLY_SHORT_CODE = "account/applyForShortCode";
    private static final String PATH_QUERY_SHORT_CODE = "account/queryForShortCode";
    private static final String PATH_QUERY_ACCOUNT_BY_SHORT_CODE = "account/queryAccountByShortCode";
    private static final String PATH_ACTIVATE_ACCOUNT = "account/activateAccount";
    private static final String PATH_ADD_OR_UPDATE_EMAIL = "user/addOrUpdateEmail";
    private static final String PATH_MOBILE_CHANGE_V2 = "account/v2/mobileChange";
    private static final String PATH_CONFIRM_MOBILE_CHANGE = "account/confirmMobileChange";
    private static final String PATH_VERIFY_EMAIL_ADDRESS = "account/verifyEmailAddress";
    private static final String PATH_VERIFY_EMAIL_OR_MOBILE = "account/verifyEmailOrMobile";
    private static final String PATH_EDIT_SUB_ACCOUNT_NAME = "account/editSubAccountName";
    private static final String PATH_VERIFY_ACCOUNT_OTP = "account/verifyOtp";
    private static final String PATH_GET_USER_KYC = "onboarding/getUserKyc";

    private static final String ONBOARD_TYPE_PERSONAL = "personal";
    private static final String DEFAULT_PERSONAL_ID_TYPE = "101";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final BankingProviderPort bankingProvider;
    private final ChoiceBankResponseAssessor choiceAssessor;
    private final CustomerRepository customerRepository;
    private final KycVerificationRepository kycVerificationRepository;
    private final SensitiveDataEncryptionPort encryptionPort;

    public AccountManagementFacade(BankingProviderPort bankingProvider,
                                   ChoiceBankResponseAssessor choiceAssessor,
                                   CustomerRepository customerRepository,
                                   KycVerificationRepository kycVerificationRepository,
                                   @Autowired(required = false) SensitiveDataEncryptionPort encryptionPort) {
        this.bankingProvider = bankingProvider;
        this.choiceAssessor = choiceAssessor;
        this.customerRepository = customerRepository;
        this.kycVerificationRepository = kycVerificationRepository;
        this.encryptionPort = encryptionPort;
    }

    /**
     * Choice {@code query/getAccountDetails} plus Vyce registered {@code email}
     * (Choice response does not include email; needed for statement prefill).
     */
    public ChoiceBankResult getAccountDetails(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        ChoiceBankResult result = choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_GET_ACCOUNT_DETAILS, params), PATH_GET_ACCOUNT_DETAILS);
        Map<String, Object> out = new HashMap<>();
        if (result.data() instanceof Map<?, ?> d) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dm = (Map<String, Object>) d;
            out.putAll(dm);
        }
        String registeredEmail = ctx.customer() != null ? ctx.customer().getEmail() : null;
        out.put("email", registeredEmail != null && !registeredEmail.isBlank()
                ? registeredEmail.trim()
                : null);
        return new ChoiceBankResult(out, result.msg(), result.choiceRequestId());
    }

    public ChoiceBankResult queryAccountListByUserId(WalletAccountContext ctx) {
        String userId = ctx.choiceUserIdOrThrow();
        if (userId == null) {
            throw new BusinessException("CHOICE_USER_ID_MISSING", "Choice user id not available yet; complete onboarding.",
                    HttpStatus.CONFLICT);
        }
        var params = Map.<String, Object>of("userId", userId);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_QUERY_ACCOUNT_LIST, params), PATH_QUERY_ACCOUNT_LIST);
    }

    public ChoiceBankResult getAbnormalAccountList(int pageNo, int pageSize) {
        var params = new HashMap<String, Object>();
        params.put("pageNo", pageNo);
        params.put("pageSize", pageSize);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_GET_ABNORMAL_ACCOUNT_LIST, params), PATH_GET_ABNORMAL_ACCOUNT_LIST);
    }

    public ChoiceBankResult applyForShortCode(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_APPLY_SHORT_CODE, params), PATH_APPLY_SHORT_CODE);
    }

    public ChoiceBankResult queryForShortCode(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_QUERY_SHORT_CODE, params), PATH_QUERY_SHORT_CODE);
    }

    public ChoiceBankResult queryAccountByShortCode(WalletAccountContext ctx, String shortCode) {
        var params = Map.<String, Object>of("shortCode", shortCode);
        ChoiceBankResult result = choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_QUERY_ACCOUNT_BY_SHORT_CODE, params), PATH_QUERY_ACCOUNT_BY_SHORT_CODE);
        if (result.data() instanceof Map<?, ?> m) {
            Object aid = m.get("accountId");
            if (aid != null && !ctx.choiceAccountId().equals(aid.toString())) {
                throw new BusinessException("SHORT_CODE_MISMATCH", "Short code does not belong to this wallet.",
                        HttpStatus.FORBIDDEN);
            }
        }
        return result;
    }

    public ChoiceBankResult activateAccount(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_ACTIVATE_ACCOUNT, params), PATH_ACTIVATE_ACCOUNT);
    }

    /**
     * Choice {@code user/addOrUpdateEmail}. Identity comes from KYC; the client supplies {@code email} only.
     * On Choice success the address is stored on {@code customer.email} for profile / statement prefill.
     */
    @Transactional
    public ChoiceBankResult addOrUpdateEmail(WalletAccountContext ctx, String email) {
        String normalizedEmail = requireValidEmail(email);
        Map<String, Object> params = personalIdentityParams(ctx);
        params.put("email", normalizedEmail);
        ChoiceBankResult result = choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_ADD_OR_UPDATE_EMAIL, params), PATH_ADD_OR_UPDATE_EMAIL);
        persistRegisteredEmail(ctx, normalizedEmail);
        return result;
    }

    public ChoiceBankResult mobileChangeV2(WalletAccountContext ctx, String newMobileCountryCode, String newMobileNumber) {
        var params = new HashMap<String, Object>();
        params.put("accountId", ctx.choiceAccountId());
        params.put("newMobileCountryCode", newMobileCountryCode);
        params.put("newMobileNumber", newMobileNumber);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_MOBILE_CHANGE_V2, params), PATH_MOBILE_CHANGE_V2);
    }

    public ChoiceBankResult confirmMobileChange(WalletAccountContext ctx, String requestId,
                                                String proveIdCode, String confirmChangeCode) {
        var params = new HashMap<String, Object>();
        params.put("requestId", requestId);
        params.put("ProveIdCode", proveIdCode);
        params.put("confirmChangeCode", confirmChangeCode);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_CONFIRM_MOBILE_CHANGE, params), PATH_CONFIRM_MOBILE_CHANGE);
    }

    /**
     * Choice {@code account/verifyEmailAddress}. Identity from KYC; no client body.
     */
    public ChoiceBankResult verifyEmailAddress(WalletAccountContext ctx) {
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_VERIFY_EMAIL_ADDRESS, personalIdentityParams(ctx)),
                PATH_VERIFY_EMAIL_ADDRESS);
    }

    /**
     * Choice {@code account/verifyEmailOrMobile}. Identity from KYC; client supplies {@code verifyType} only.
     */
    public ChoiceBankResult verifyEmailOrMobile(WalletAccountContext ctx, String verifyType) {
        String normalizedType = requireVerifyType(verifyType);
        Map<String, Object> params = personalIdentityParams(ctx);
        params.put("verifyType", normalizedType);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_VERIFY_EMAIL_OR_MOBILE, params), PATH_VERIFY_EMAIL_OR_MOBILE);
    }

    public ChoiceBankResult editSubAccountName(WalletAccountContext ctx, String subAccountName) {
        var params = new HashMap<String, Object>();
        params.put("accountId", ctx.choiceAccountId());
        if (subAccountName != null) {
            params.put("subAccountName", subAccountName);
        }
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_EDIT_SUB_ACCOUNT_NAME, params), PATH_EDIT_SUB_ACCOUNT_NAME);
    }

    /**
     * Choice account-level OTP verification (e.g. after email/mobile flows). Not common/confirmOperation.
     */
    public ChoiceBankResult verifyAccountOtp(WalletAccountContext ctx, String applicationId, String otpCode) {
        var params = new HashMap<String, Object>();
        params.put("applicationId", applicationId);
        params.put("otpCode", otpCode);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_VERIFY_ACCOUNT_OTP, params), PATH_VERIFY_ACCOUNT_OTP);
    }

    /**
     * Builds Choice personal-identity params from KYC. Never taken from the client.
     * If local {@code id_number} is blank (legacy rows), pulls it from Choice {@code getUserKyc}.
     */
    private Map<String, Object> personalIdentityParams(WalletAccountContext ctx) {
        KycVerification kyc = ctx.latestKyc();
        if (kyc == null) {
            throw new BusinessException("KYC_IDENTITY_MISSING",
                    "KYC identity is not on file; complete onboarding first.", HttpStatus.CONFLICT);
        }
        String documentNumber = decryptIdNumber(kyc.getIdNumber());
        String idType = kyc.getIdType();
        if (documentNumber == null) {
            ChoiceIdentity fromChoice = backfillIdentityFromChoice(kyc);
            documentNumber = fromChoice.documentNumber();
            if (idType == null || idType.isBlank()) {
                idType = fromChoice.idType();
            }
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new BusinessException("KYC_IDENTITY_MISSING",
                    "ID number is not on file; complete onboarding first.", HttpStatus.CONFLICT);
        }
        if (idType == null || idType.isBlank()) {
            idType = DEFAULT_PERSONAL_ID_TYPE;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("onboardType", ONBOARD_TYPE_PERSONAL);
        params.put("personalIdType", idType.trim());
        params.put("documentNumber", documentNumber.trim());
        return params;
    }

    private String decryptIdNumber(String storedId) {
        if (storedId == null || storedId.isBlank()) {
            return null;
        }
        String documentNumber = encryptionPort != null ? encryptionPort.decrypt(storedId) : storedId;
        if (documentNumber == null || documentNumber.isBlank()) {
            return null;
        }
        return documentNumber.trim();
    }

    /**
     * Choice {@code onboarding/getUserKyc} for accounts whose local KYC row never stored id_number.
     */
    private ChoiceIdentity backfillIdentityFromChoice(KycVerification kyc) {
        String onboardingRequestId = kyc.getChoiceOnboardingRequestId();
        if (onboardingRequestId == null || onboardingRequestId.isBlank()) {
            throw new BusinessException("KYC_IDENTITY_MISSING",
                    "ID number is not on file; complete onboarding first.", HttpStatus.CONFLICT);
        }
        log.info("Local KYC id_number missing; fetching from Choice getUserKyc onboardingRequestId={}",
                onboardingRequestId);
        ChoiceBankResult result = choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_GET_USER_KYC, Map.of("onboardingRequestId", onboardingRequestId)),
                PATH_GET_USER_KYC);
        String idNumber = null;
        String idType = null;
        if (result.data() instanceof Map<?, ?> data) {
            Object n = data.get("idNumber");
            Object t = data.get("idType");
            if (n != null && !n.toString().isBlank()) {
                idNumber = n.toString().trim();
            }
            if (t != null && !t.toString().isBlank()) {
                idType = t.toString().trim();
            }
        }
        if (idNumber == null) {
            throw new BusinessException("KYC_IDENTITY_MISSING",
                    "ID number is not on file; complete onboarding first.", HttpStatus.CONFLICT);
        }
        kyc.setIdNumber(encryptionPort != null ? encryptionPort.encrypt(idNumber) : idNumber);
        if (idType != null) {
            kyc.setIdType(idType);
        }
        kycVerificationRepository.save(kyc);
        return new ChoiceIdentity(idNumber, idType);
    }

    private record ChoiceIdentity(String documentNumber, String idType) {}

    private void persistRegisteredEmail(WalletAccountContext ctx, String email) {
        Customer customer = ctx.customer();
        if (customer == null) {
            return;
        }
        customer.setEmail(email);
        customerRepository.save(customer);
    }

    private static String requireValidEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("EMAIL_REQUIRED", "email is required", HttpStatus.BAD_REQUEST);
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException("INVALID_EMAIL", "email format is invalid", HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private static String requireVerifyType(String verifyType) {
        if (verifyType == null || verifyType.isBlank()) {
            throw new BusinessException("INVALID_VERIFY_TYPE", "verifyType must be email or mobile",
                    HttpStatus.BAD_REQUEST);
        }
        String normalized = verifyType.trim().toLowerCase(Locale.ROOT);
        if (!"email".equals(normalized) && !"mobile".equals(normalized)) {
            throw new BusinessException("INVALID_VERIFY_TYPE", "verifyType must be email or mobile",
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }
}
