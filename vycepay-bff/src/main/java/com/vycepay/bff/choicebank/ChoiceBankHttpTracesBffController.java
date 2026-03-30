package com.vycepay.bff.choicebank;

import com.vycepay.bff.config.BffBackendProperties;
import com.vycepay.common.choicebank.dto.ChoiceBankHttpTraceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregates in-memory Choice BaaS HTTP traces from KYC, Wallet, and Transaction services.
 * Unauthenticated; restrict at the network layer in production.
 */
@RestController
@RequestMapping(value = "/internal/choice-bank", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChoiceBankHttpTracesBffController {

    private static final String PATH = "/internal/choice-bank/http-traces";
    private static final ParameterizedTypeReference<List<ChoiceBankHttpTraceDto>> TRACE_LIST =
            new ParameterizedTypeReference<>() {};

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankHttpTracesBffController.class);

    private final BffBackendProperties backend;
    private final RestTemplate restTemplate = new RestTemplate();

    public ChoiceBankHttpTracesBffController(BffBackendProperties backend) {
        this.backend = backend;
    }

    /**
     * Returns merged traces from all backends that expose the audit endpoint, newest first.
     * Unreachable or failing backends are skipped (partial results).
     */
    @GetMapping("/http-traces")
    public List<ChoiceBankHttpTraceWithSource> httpTraces() {
        List<ChoiceBankHttpTraceWithSource> merged = new ArrayList<>();
        fetchInto(mergeBase(backend.getKycUrl()), "kyc", merged);
        fetchInto(mergeBase(backend.getWalletsUrl()), "wallets", merged);
        fetchInto(mergeBase(backend.getTransactionsUrl()), "transactions", merged);
        merged.sort(Comparator.comparing(
                (ChoiceBankHttpTraceWithSource e) -> e.trace() != null ? e.trace().getTimestamp() : null,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return merged;
    }

    private void fetchInto(String baseUrl, String source, List<ChoiceBankHttpTraceWithSource> out) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.debug("Skipping Choice HTTP traces for {}: empty backend URL", source);
            return;
        }
        String url = baseUrl + PATH;
        try {
            ResponseEntity<List<ChoiceBankHttpTraceDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    TRACE_LIST);
            List<ChoiceBankHttpTraceDto> body = response.getBody();
            if (body == null) {
                return;
            }
            for (ChoiceBankHttpTraceDto t : body) {
                out.add(new ChoiceBankHttpTraceWithSource(source, t));
            }
        } catch (RestClientException e) {
            log.warn("BFF could not fetch Choice HTTP traces from {} ({}): {}", source, url, e.getMessage());
        }
    }

    private static String mergeBase(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
