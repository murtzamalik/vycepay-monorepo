package com.vycepay.wallet.api.v1;

import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.api.v1.dto.StatementApplyRequest;
import com.vycepay.wallet.api.v1.dto.StatementJobResponse;
import com.vycepay.wallet.api.v1.dto.StatementQueryRequest;
import com.vycepay.wallet.application.facade.AccountStatementFacade;
import com.vycepay.wallet.application.service.WalletAccountContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Periodic account statement apply/query (Choice statement/applyAccountStatement, queryAccountStatement).
 */
@RestController
@RequestMapping("/api/v1/wallets/statements")
public class WalletStatementController {

    private final AccountStatementFacade statementFacade;
    private final WalletAccountContextService contextService;

    public WalletStatementController(
            @Autowired(required = false) AccountStatementFacade statementFacade,
            WalletAccountContextService contextService) {
        this.statementFacade = statementFacade;
        this.contextService = contextService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> apply(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody StatementApplyRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        Map<String, Object> data = statementFacade.applyAccountStatement(ctx,
                request.getStatementStartTime(), request.getStatementEndTime(), request.getFileType());
        return ResponseEntity.ok(ApiSuccessResponses.ok("WALLET_STATEMENT_APPLIED", "Statement generation requested.", data));
    }

    @PostMapping("/query")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> query(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody StatementQueryRequest request) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        Map<String, Object> data = statementFacade.queryAccountStatement(ctx, request.getRequestId());
        return ResponseEntity.ok(ApiSuccessResponses.ok("WALLET_STATEMENT_QUERY", "Statement status retrieved.", data));
    }

    @GetMapping("/jobs/{choiceRequestId}")
    public ResponseEntity<ApiSuccessResponse<StatementJobResponse>> getJob(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String choiceRequestId) {
        requireFacade();
        var ctx = contextService.requireContext(externalId);
        var job = statementFacade.getLocalStatementJob(ctx, choiceRequestId);
        return ResponseEntity.ok(ApiSuccessResponses.ok("WALLET_STATEMENT_JOB",
                "Statement job retrieved.", StatementJobResponse.from(job)));
    }

    private void requireFacade() {
        if (statementFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Choice Bank is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
