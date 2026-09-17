package com.vycepay.wallet.api.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vycepay.wallet.api.v1.dto.WalletResponse;
import com.vycepay.wallet.application.service.WalletBalanceRefreshService;
import com.vycepay.wallet.application.service.WalletService;
import com.vycepay.wallet.domain.model.Wallet;

/**
 * Wallet API: balance and account info.
 * {@code GET /me} reads cache; {@code POST /me/refresh-balance} pulls from Choice and updates cache.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;
    private final WalletBalanceRefreshService balanceRefreshService;

    public WalletController(WalletService walletService,
                            WalletBalanceRefreshService balanceRefreshService) {
        this.walletService = walletService;
        this.balanceRefreshService = balanceRefreshService;
    }

    /**
     * Returns wallet for current customer (cached balance).
     *
     * @param XCustomerId Header with customer external ID (from JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMe(@RequestHeader("X-Customer-Id") String externalId) {
        return walletService.getWalletByCustomerExternalId(externalId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Forces a Choice {@code getAccountDetails} pull, updates {@code balance_cache}, returns wallet.
     * Mobile should call this on pull-to-refresh / Refresh; use {@code GET /me} for ordinary reads.
     */
    @PostMapping("/me/refresh-balance")
    public ResponseEntity<WalletResponse> refreshBalance(@RequestHeader("X-Customer-Id") String externalId) {
        Wallet updated = balanceRefreshService.refreshFromChoice(externalId);
        return ResponseEntity.ok(toResponse(updated));
    }

    private WalletResponse toResponse(Wallet w) {
        return new WalletResponse(
                w.getChoiceAccountId(),
                w.getBalanceCache(),
                w.getCurrency(),
                w.getStatus(),
                w.getLastBalanceUpdateAt());
    }
}
