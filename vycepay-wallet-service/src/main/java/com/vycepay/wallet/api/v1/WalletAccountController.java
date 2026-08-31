package com.vycepay.wallet.api.v1;

import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.choicebank.errors.ChoiceBankResult;
import com.vycepay.common.choicebank.errors.ChoiceCustomerMessage;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.api.v1.dto.AddOrUpdateEmailRequest;
import com.vycepay.wallet.api.v1.dto.ConfirmMobileChangeRequest;
import com.vycepay.wallet.api.v1.dto.EditSubAccountNameRequest;
import com.vycepay.wallet.api.v1.dto.MobileChangeV2Request;
import com.vycepay.wallet.api.v1.dto.ShortCodeResolveRequest;
import com.vycepay.wallet.api.v1.dto.VerifyAccountOtpRequest;
import com.vycepay.wallet.api.v1.dto.VerifyEmailOrMobileRequest;
import com.vycepay.wallet.application.facade.AccountManagementFacade;
import com.vycepay.wallet.application.service.WalletAccountContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Choice Bank account management APIs (proxied through VycePay with customer scoping).
 * Envelope {@code message} prefers Choice Bank {@code msg} when present.
 */
@RestController
@RequestMapping("/api/v1/wallets/account")
public class WalletAccountController {

    private final AccountManagementFacade accountManagementFacade;
    private final WalletAccountContextService contextService;

    public WalletAccountController(
            @Autowired(required = false) AccountManagementFacade accountManagementFacade,
            WalletAccountContextService contextService) {
        this.accountManagementFacade = accountManagementFacade;
        this.contextService = contextService;
    }

    @GetMapping("/details")
    public ResponseEntity<ApiSuccessResponse<Object>> getDetails(@RequestHeader("X-Customer-Id") String externalId) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_ACCOUNT_DETAILS", "Account details retrieved.",
                accountManagementFacade.getAccountDetails(ctx));
    }

    @GetMapping("/list-by-user")
    public ResponseEntity<ApiSuccessResponse<Object>> listByUser(@RequestHeader("X-Customer-Id") String externalId) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_ACCOUNT_LIST", "Accounts retrieved.",
                accountManagementFacade.queryAccountListByUserId(ctx));
    }

    @GetMapping("/abnormal")
    public ResponseEntity<ApiSuccessResponse<Object>> abnormalList(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireFacade();
        contextService.requireContext(externalId);
        return ok("WALLET_ABNORMAL_ACCOUNTS", "Abnormal account list retrieved.",
                accountManagementFacade.getAbnormalAccountList(pageNo, pageSize));
    }

    @PostMapping("/short-code/apply")
    public ResponseEntity<ApiSuccessResponse<Object>> applyShortCode(@RequestHeader("X-Customer-Id") String externalId) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_SHORT_CODE_APPLIED", "Short code applied.",
                accountManagementFacade.applyForShortCode(ctx));
    }

    @PostMapping("/short-code/query")
    public ResponseEntity<ApiSuccessResponse<Object>> queryShortCode(@RequestHeader("X-Customer-Id") String externalId) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_SHORT_CODE_QUERY", "Short code retrieved.",
                accountManagementFacade.queryForShortCode(ctx));
    }

    @PostMapping("/short-code/resolve")
    public ResponseEntity<ApiSuccessResponse<Object>> resolveShortCode(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody ShortCodeResolveRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_SHORT_CODE_RESOLVED", "Account resolved from short code.",
                accountManagementFacade.queryAccountByShortCode(ctx, request.getShortCode()));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiSuccessResponse<Object>> activate(@RequestHeader("X-Customer-Id") String externalId) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_ACCOUNT_ACTIVATED", "Dormant account activation requested.",
                accountManagementFacade.activateAccount(ctx));
    }

    @PostMapping("/email")
    public ResponseEntity<ApiSuccessResponse<Object>> addOrUpdateEmail(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody AddOrUpdateEmailRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_EMAIL_UPDATED", "Email update requested.",
                accountManagementFacade.addOrUpdateEmail(ctx, request.getEmail()));
    }

    @PostMapping("/mobile-change")
    public ResponseEntity<ApiSuccessResponse<Object>> mobileChange(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody MobileChangeV2Request request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_MOBILE_CHANGE_REQUESTED", "Mobile change requested.",
                accountManagementFacade.mobileChangeV2(ctx, request.getNewMobileCountryCode(), request.getNewMobileNumber()));
    }

    @PostMapping("/mobile-change/confirm")
    public ResponseEntity<ApiSuccessResponse<Void>> confirmMobileChange(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody ConfirmMobileChangeRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        ChoiceBankResult result = accountManagementFacade.confirmMobileChange(ctx, request.getRequestId(),
                request.getProveIdCode(), request.getConfirmChangeCode());
        return ResponseEntity.ok(ApiSuccessResponses.ok("WALLET_MOBILE_CHANGE_CONFIRMED",
                ChoiceCustomerMessage.prefer(result.msg(), "Mobile change confirmed.")));
    }

    @PostMapping("/verify-email-address")
    public ResponseEntity<ApiSuccessResponse<Object>> verifyEmailAddress(
            @RequestHeader("X-Customer-Id") String externalId) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_VERIFY_EMAIL_REQUESTED", "Email verification requested.",
                accountManagementFacade.verifyEmailAddress(ctx));
    }

    @PostMapping("/verify-email-or-mobile")
    public ResponseEntity<ApiSuccessResponse<Object>> verifyEmailOrMobile(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody VerifyEmailOrMobileRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_VERIFY_CONTACT_REQUESTED", "Contact verification requested.",
                accountManagementFacade.verifyEmailOrMobile(ctx, request.getVerifyType()));
    }

    @PostMapping("/sub-account-name")
    public ResponseEntity<ApiSuccessResponse<Object>> editSubAccountName(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody EditSubAccountNameRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_SUB_ACCOUNT_NAME_UPDATED", "Sub-account name update requested.",
                accountManagementFacade.editSubAccountName(ctx, request.getSubAccountName()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiSuccessResponse<Object>> verifyAccountOtp(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody VerifyAccountOtpRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        return ok("WALLET_ACCOUNT_OTP_VERIFIED", "Account OTP verified.",
                accountManagementFacade.verifyAccountOtp(ctx, request.getApplicationId(), request.getOtpCode()));
    }

    private ResponseEntity<ApiSuccessResponse<Object>> ok(String code, String fallback, ChoiceBankResult result) {
        return ResponseEntity.ok(ApiSuccessResponses.ok(code,
                ChoiceCustomerMessage.prefer(result.msg(), fallback), result.data()));
    }

    private void requireFacade() {
        if (accountManagementFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Choice Bank is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
