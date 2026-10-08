package com.vycepay.admin.application.service;

import com.vycepay.common.sms.KenyaPhoneNormalizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves registered-customer SMS audiences (ACTIVE/PENDING/SUSPENDED).
 * DEACTIVATED customers are excluded. Invalid Kenya mobiles are skipped.
 */
@Service
public class BulkSmsAudienceService {

    public static final int MAX_ALL_CUSTOMERS = 10_000;
    public static final String ELIGIBLE_STATUSES_SQL = "('ACTIVE','PENDING','SUSPENDED')";

    private final JdbcTemplate jdbcTemplate;

    public BulkSmsAudienceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Preview counts for the admin Bulk SMS "all customers" confirm UI.
     */
    public Map<String, Object> preview() {
        ResolvedAudience resolved = resolve();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCustomers", resolved.totalCustomers());
        data.put("withValidMobile", resolved.recipients().size());
        data.put("skippedInvalid", resolved.skippedInvalid());
        return data;
    }

    /**
     * Loads eligible customers and returns deduped valid recipients (first customer wins).
     * Caps SQL at MAX_ALL_CUSTOMERS + 1 so oversize can be detected without a full table scan.
     */
    public ResolvedAudience resolve() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, mobile_country_code mobileCountryCode, mobile FROM customer "
                        + "WHERE status IN " + ELIGIBLE_STATUSES_SQL
                        + " ORDER BY id ASC LIMIT " + (MAX_ALL_CUSTOMERS + 1));
        return fromRows(rows);
    }

    /**
     * Pure resolution from customer rows (package-visible for unit tests).
     */
    static ResolvedAudience fromRows(List<Map<String, Object>> rows) {
        int totalCustomers = rows.size();
        int skippedInvalid = 0;
        Map<String, Long> byRecipient = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long customerId = ((Number) row.get("id")).longValue();
            String cc = row.get("mobileCountryCode") != null ? String.valueOf(row.get("mobileCountryCode")) : "";
            String mobile = row.get("mobile") != null ? String.valueOf(row.get("mobile")) : "";
            var normalized = KenyaPhoneNormalizer.toRecipient(cc + mobile);
            if (normalized.isEmpty()) {
                skippedInvalid++;
                continue;
            }
            byRecipient.putIfAbsent(normalized.get(), customerId);
        }
        List<AudienceRecipient> recipients = new ArrayList<>(byRecipient.size());
        for (Map.Entry<String, Long> e : byRecipient.entrySet()) {
            recipients.add(new AudienceRecipient(e.getValue(), e.getKey()));
        }
        return new ResolvedAudience(totalCustomers, skippedInvalid, recipients);
    }

    /**
     * One send target: customer PK + normalized MobiWave recipient.
     */
    public record AudienceRecipient(long customerId, String recipient) {
    }

    /**
     * Result of resolving the all-customers audience from the customer table.
     */
    public record ResolvedAudience(int totalCustomers, int skippedInvalid, List<AudienceRecipient> recipients) {
    }
}
