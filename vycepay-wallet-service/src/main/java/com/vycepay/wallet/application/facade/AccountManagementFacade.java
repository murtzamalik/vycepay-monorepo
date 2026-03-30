package com.vycepay.wallet.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.application.WalletAccountContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates Choice Bank account management APIs (query, short code, contact, SME sub-account, verify OTP).
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class AccountManagementFacade {

    private static final Logger log = LoggerFactory.getLogger(AccountManagementFacade.class);

    private final BankingProviderPort bankingProvider;

    public AccountManagementFacade(BankingProviderPort bankingProvider) {
        this.bankingProvider = bankingProvider;
    }

    public Object getAccountDetails(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return dataOrThrow(bankingProvider.post("query/getAccountDetails", params));
    }

    public Object queryAccountListByUserId(WalletAccountContext ctx) {
        String userId = ctx.choiceUserIdOrThrow();
        if (userId == null) {
            throw new BusinessException("CHOICE_USER_ID_MISSING", "Choice user id not available yet; complete onboarding.",
                    HttpStatus.CONFLICT);
        }
        var params = Map.<String, Object>of("userId", userId);
        return dataOrThrow(bankingProvider.post("account/queryAccountListByUserId", params));
    }

    public Object getAbnormalAccountList(int pageNo, int pageSize) {
        var params = new HashMap<String, Object>();
        params.put("pageNo", pageNo);
        params.put("pageSize", pageSize);
        return dataOrThrow(bankingProvider.post("query/getAbnormalAccountList", params));
    }

    public Object applyForShortCode(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return dataOrThrow(bankingProvider.post("account/applyForShortCode", params));
    }

    public Object queryForShortCode(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return dataOrThrow(bankingProvider.post("account/queryForShortCode", params));
    }

    public Object queryAccountByShortCode(WalletAccountContext ctx, String shortCode) {
        var params = Map.<String, Object>of("shortCode", shortCode);
        Object data = dataOrThrow(bankingProvider.post("account/queryAccountByShortCode", params));
        if (data instanceof Map<?, ?> m) {
            Object aid = m.get("accountId");
            if (aid != null && !ctx.choiceAccountId().equals(aid.toString())) {
                throw new BusinessException("SHORT_CODE_MISMATCH", "Short code does not belong to this wallet.",
                        HttpStatus.FORBIDDEN);
            }
        }
        return data;
    }

    public Object activateAccount(WalletAccountContext ctx) {
        var params = Map.<String, Object>of("accountId", ctx.choiceAccountId());
        return dataOrThrow(bankingProvider.post("account/activateAccount", params));
    }

    public Object addOrUpdateEmail(WalletAccountContext ctx, Map<String, Object> body) {
        var params = new HashMap<String, Object>();
        params.putAll(body);
        return dataOrThrow(bankingProvider.post("user/addOrUpdateEmail", params));
    }

    public Object mobileChangeV2(WalletAccountContext ctx, String newMobileCountryCode, String newMobileNumber) {
        var params = new HashMap<String, Object>();
        params.put("accountId", ctx.choiceAccountId());
        params.put("newMobileCountryCode", newMobileCountryCode);
        params.put("newMobileNumber", newMobileNumber);
        return dataOrThrow(bankingProvider.post("account/v2/mobileChange", params));
    }

    public Object confirmMobileChange(WalletAccountContext ctx, String requestId,
                                      String proveIdCode, String confirmChangeCode) {
        var params = new HashMap<String, Object>();
        params.put("requestId", requestId);
        params.put("ProveIdCode", proveIdCode);
        params.put("confirmChangeCode", confirmChangeCode);
        ChoiceBankResponse response = bankingProvider.post("account/confirmMobileChange", params);
        requireSuccess(response);
        return Map.of();
    }

    public Object verifyEmailAddress(WalletAccountContext ctx, Map<String, Object> body) {
        var params = new HashMap<String, Object>();
        params.putAll(body);
        return dataOrThrow(bankingProvider.post("account/verifyEmailAddress", params));
    }

    public Object verifyEmailOrMobile(WalletAccountContext ctx, Map<String, Object> body) {
        var params = new HashMap<String, Object>();
        params.putAll(body);
        return dataOrThrow(bankingProvider.post("account/verifyEmailOrMobile", params));
    }

    public Object editSubAccountName(WalletAccountContext ctx, String subAccountName) {
        var params = new HashMap<String, Object>();
        params.put("accountId", ctx.choiceAccountId());
        if (subAccountName != null) {
            params.put("subAccountName", subAccountName);
        }
        return dataOrThrow(bankingProvider.post("account/editSubAccountName", params));
    }

    /**
     * Choice account-level OTP verification (e.g. after email/mobile flows). Not common/confirmOperation.
     */
    public Object verifyAccountOtp(WalletAccountContext ctx, String applicationId, String otpCode) {
        var params = new HashMap<String, Object>();
        params.put("applicationId", applicationId);
        params.put("otpCode", otpCode);
        return dataOrThrow(bankingProvider.post("account/verifyOtp", params));
    }

    private void requireSuccess(ChoiceBankResponse response) {
        if (!response.isSuccess()) {
            log.warn("Choice Bank error: code={} msg={}", response.getCode(), response.getMsg());
            throw new BusinessException("CHOICE_BANK_ERROR",
                    response.getMsg() != null ? response.getMsg() : "Choice Bank error",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private Object dataOrThrow(ChoiceBankResponse response) {
        requireSuccess(response);
        return response.getData() != null ? response.getData() : Map.of();
    }
}
