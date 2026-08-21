package com.vycepay.wallet.application.facade;

import com.vycepay.common.choicebank.errors.ChoiceBankResult;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.application.WalletAccountContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates Choice Bank account management APIs (query, short code, contact, SME sub-account, verify OTP).
 * Returns {@link ChoiceBankResult} so controllers can prefer Choice {@code msg} for customer display.
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class AccountManagementFacade {

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

    private final BankingProviderPort bankingProvider;
    private final ChoiceBankResponseAssessor choiceAssessor;

    public AccountManagementFacade(BankingProviderPort bankingProvider,
                                   ChoiceBankResponseAssessor choiceAssessor) {
        this.bankingProvider = bankingProvider;
        this.choiceAssessor = choiceAssessor;
    }

    public ChoiceBankResult getAccountDetails(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_GET_ACCOUNT_DETAILS, params), PATH_GET_ACCOUNT_DETAILS);
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

    public ChoiceBankResult addOrUpdateEmail(WalletAccountContext ctx, Map<String, Object> body) {
        var params = new HashMap<String, Object>();
        params.putAll(body);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_ADD_OR_UPDATE_EMAIL, params), PATH_ADD_OR_UPDATE_EMAIL);
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

    public ChoiceBankResult verifyEmailAddress(WalletAccountContext ctx, Map<String, Object> body) {
        var params = new HashMap<String, Object>();
        params.putAll(body);
        return choiceAssessor.requireSuccessResult(
                bankingProvider.post(PATH_VERIFY_EMAIL_ADDRESS, params), PATH_VERIFY_EMAIL_ADDRESS);
    }

    public ChoiceBankResult verifyEmailOrMobile(WalletAccountContext ctx, Map<String, Object> body) {
        var params = new HashMap<String, Object>();
        params.putAll(body);
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
}
