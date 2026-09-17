package com.vycepay.wallet.application.service;

import com.vycepay.common.choicebank.errors.ChoiceBankResult;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.application.WalletAccountContext;
import com.vycepay.wallet.application.facade.AccountManagementFacade;
import com.vycepay.wallet.domain.model.Wallet;
import com.vycepay.wallet.infrastructure.persistence.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Pulls live balance from Choice {@code query/getAccountDetails} and updates {@code balance_cache}.
 * Complements callback 0003 which is the usual push-based cache update path.
 */
@Service
public class WalletBalanceRefreshService {

    private static final Logger log = LoggerFactory.getLogger(WalletBalanceRefreshService.class);

    /** Choice field names seen across BaaS payloads; first match wins. */
    private static final List<String> BALANCE_KEYS = List.of(
            "balance", "availableBalance", "currentBalance", "accountBalance", "availBalance");

    private final WalletAccountContextService contextService;
    private final WalletRepository walletRepository;
    private final AccountManagementFacade accountManagementFacade;

    public WalletBalanceRefreshService(WalletAccountContextService contextService,
                                       WalletRepository walletRepository,
                                       @Autowired(required = false) AccountManagementFacade accountManagementFacade) {
        this.contextService = contextService;
        this.walletRepository = walletRepository;
        this.accountManagementFacade = accountManagementFacade;
    }

    /**
     * Fetches Choice account details, persists balance_cache, returns updated wallet.
     *
     * @param externalId Customer external ID from JWT / X-Customer-Id
     * @throws BusinessException if Choice client unavailable, Choice fails, or balance missing
     */
    @Transactional
    public Wallet refreshFromChoice(String externalId) {
        if (accountManagementFacade == null) {
            throw new BusinessException("CHOICE_BANK_ERROR",
                    "Banking provider is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        WalletAccountContext ctx = contextService.requireContext(externalId);
        ChoiceBankResult result = accountManagementFacade.getAccountDetails(ctx);
        BigDecimal balance = extractBalance(result.data());
        if (balance == null) {
            throw new BusinessException("INVALID_RESPONSE",
                    "Account details did not include a balance.", HttpStatus.BAD_GATEWAY);
        }

        Wallet wallet = ctx.wallet();
        wallet.setBalanceCache(balance);
        wallet.setLastBalanceUpdateAt(Instant.now());
        Wallet saved = walletRepository.save(wallet);
        log.info("Refreshed balance_cache for choiceAccountId={} balance={}",
                wallet.getChoiceAccountId(), balance);
        return saved;
    }

    /**
     * Reads balance from Choice data map using known field aliases.
     */
    static BigDecimal extractBalance(Object data) {
        if (!(data instanceof Map<?, ?> map)) {
            return null;
        }
        for (String key : BALANCE_KEYS) {
            Object raw = map.get(key);
            BigDecimal parsed = toBigDecimal(raw);
            if (parsed != null) {
                return parsed;
            }
        }
        // Nested account object sometimes used by Choice
        Object nested = map.get("account");
        if (nested instanceof Map<?, ?> accountMap) {
            for (String key : BALANCE_KEYS) {
                BigDecimal parsed = toBigDecimal(accountMap.get(key));
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private static BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number) {
            // Avoid double binary artifacts (42.0 vs 42.00) by parsing decimal string form
            return new BigDecimal(raw.toString());
        }
        String s = raw.toString().trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
