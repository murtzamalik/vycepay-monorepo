package com.vycepay.wallet.api.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vycepay.wallet.api.v1.dto.WalletResponse;
import com.vycepay.wallet.application.service.WalletService;
import com.vycepay.wallet.domain.model.Wallet;

/**
 * Wallet API: balance and account info.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Returns wallet for current customer.
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

    private WalletResponse toResponse(Wallet w) {
        return new WalletResponse(
                w.getChoiceAccountId(),
                w.getBalanceCache(),
                w.getCurrency(),
                w.getStatus());
    }
}
