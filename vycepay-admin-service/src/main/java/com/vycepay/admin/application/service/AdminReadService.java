package com.vycepay.admin.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

import com.vycepay.admin.config.AdminProperties;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Provides read models and aggregate queries for admin list, detail, dashboard, and report screens. */
@Service
public class AdminReadService {
    private final JdbcTemplate jdbcTemplate;
    private final AdminSecurityContext securityContext;
    private final PiiMaskingService maskingService;
    private final AdminProperties properties;

    public AdminReadService(JdbcTemplate jdbcTemplate, AdminSecurityContext securityContext, PiiMaskingService maskingService, AdminProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.securityContext = securityContext;
        this.maskingService = maskingService;
        this.properties = properties;
    }

    public Map<String, Object> dashboardSummary(String fromDate, String toDate) {
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        Map<String, Object> m = new LinkedHashMap<>();
        if (range.hasRange()) {
            m.put("totalCustomers", countWithRange("customer", "1=1", "created_at", range));
            m.put("activeWallets", countWithRange("wallet", "status='ACTIVE'", "created_at", range));
            m.put("todayTxVolume", sumTxInRange(range));
            m.put("todayTxCount", countWithRange("`transaction`", "1=1", "created_at", range));
        } else {
            m.put("totalCustomers", count("customer", "1=1"));
            m.put("activeWallets", count("wallet", "status='ACTIVE'"));
            m.put("todayTxVolume", jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount),0) FROM `transaction` WHERE DATE(created_at)=CURRENT_DATE", BigDecimal.class));
            m.put("todayTxCount", count("`transaction`", "DATE(created_at)=CURRENT_DATE"));
        }
        m.put("kycApprovalRate", jdbcTemplate.queryForObject("SELECT COALESCE(ROUND(100 * SUM(status='APPROVED') / NULLIF(COUNT(*),0), 2), 0) FROM kyc_verification", BigDecimal.class));
        m.put("pendingCallbacks", count("choice_bank_callback", "processed=FALSE"));
        m.put("stuckTxOver1h", count("`transaction`", "status='PENDING' AND created_at < DATE_SUB(NOW(), INTERVAL 1 HOUR)"));
        m.put("notificationsSentToday", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM push_delivery_log WHERE status IN ('SENT','PARTIAL') AND DATE(created_at)=CURRENT_DATE",
                Long.class));
        return m;
    }

    public List<Map<String, Object>> txVolumeChart(String fromDate, String toDate, Integer days) {
        DateRangeQuery range = resolveRange(fromDate, toDate, days);
        if (range.hasRange()) {
            return jdbcTemplate.queryForList(
                    "SELECT DATE(created_at) date, SUM(CASE WHEN type='TRANSFER' THEN amount ELSE 0 END) transferAmount, SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) depositAmount FROM `transaction` WHERE created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY) GROUP BY DATE(created_at) ORDER BY date",
                    range.from().toString(), range.to().toString());
        }
        int d = days == null ? 30 : days;
        return jdbcTemplate.queryForList(
                "SELECT DATE(created_at) date, SUM(CASE WHEN type='TRANSFER' THEN amount ELSE 0 END) transferAmount, SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) depositAmount FROM `transaction` WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) GROUP BY DATE(created_at) ORDER BY date", d);
    }

    public Map<String, Object> txTypeDonut(String fromDate, String toDate, Integer days) {
        DateRangeQuery range = resolveRange(fromDate, toDate, days);
        if (range.hasRange()) {
            return jdbcTemplate.queryForMap(
                    "SELECT SUM(type='TRANSFER') transferCount, SUM(type='DEPOSIT') depositCount, SUM(CASE WHEN type='TRANSFER' THEN amount ELSE 0 END) transferVolume, SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) depositVolume FROM `transaction` WHERE created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY)",
                    range.from().toString(), range.to().toString());
        }
        int d = days == null ? 30 : days;
        return jdbcTemplate.queryForMap(
                "SELECT SUM(type='TRANSFER') transferCount, SUM(type='DEPOSIT') depositCount, SUM(CASE WHEN type='TRANSFER' THEN amount ELSE 0 END) transferVolume, SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) depositVolume FROM `transaction` WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY)", d);
    }

    public List<Map<String, Object>> kycStatusChart() {
        return jdbcTemplate.queryForList("SELECT status, COUNT(*) count FROM kyc_verification GROUP BY status ORDER BY count DESC");
    }

    public Map<String, Object> alerts() {
        return Map.of(
                "unprocessedCallbacks", count("choice_bank_callback", "processed=FALSE"),
                "pendingTxOver1h", count("`transaction`", "status='PENDING' AND created_at < DATE_SUB(NOW(), INTERVAL 1 HOUR)"),
                "pendingTxOver24h", count("`transaction`", "status='PENDING' AND created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)"));
    }

    public List<Map<String, Object>> recentTransactions(int limit, String fromDate, String toDate) {
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        int lim = Math.min(Math.max(limit, 1), 50);
        if (range.hasRange()) {
            return jdbcTemplate.queryForList(
                    "SELECT t.external_id externalId, t.type, t.amount, t.currency, t.status, t.created_at createdAt, c.external_id customerExternalId FROM `transaction` t JOIN customer c ON c.id=t.customer_id WHERE t.created_at >= ? AND t.created_at < DATE_ADD(?, INTERVAL 1 DAY) ORDER BY t.created_at DESC LIMIT ?",
                    range.from().toString(), range.to().toString(), lim);
        }
        return jdbcTemplate.queryForList(
                "SELECT t.external_id externalId, t.type, t.amount, t.currency, t.status, t.created_at createdAt, c.external_id customerExternalId FROM `transaction` t JOIN customer c ON c.id=t.customer_id ORDER BY t.created_at DESC LIMIT ?", lim);
    }

    private static final Map<String, String> CUSTOMER_SORT = Map.ofEntries(
            entry("createdAt", "c.created_at"), entry("status", "c.status"), entry("externalId", "c.external_id"),
            entry("firstName", "c.first_name"), entry("lastName", "c.last_name"), entry("walletBalance", "w.balance_cache"));
    private static final Map<String, String> TX_SORT = Map.ofEntries(
            entry("createdAt", "t.created_at"), entry("amount", "t.amount"), entry("status", "t.status"),
            entry("type", "t.type"), entry("externalId", "t.external_id"), entry("customerExternalId", "c.external_id"),
            entry("errorCode", "t.error_code"));
    private static final Map<String, String> KYC_SORT = Map.ofEntries(
            entry("createdAt", "k.created_at"), entry("status", "k.status"), entry("id", "k.id"),
            entry("idType", "k.id_type"), entry("customerExternalId", "c.external_id"));
    private static final Map<String, String> WALLET_SORT = Map.ofEntries(
            entry("createdAt", "w.created_at"), entry("id", "w.id"), entry("balance", "w.balance_cache"),
            entry("status", "w.status"), entry("choiceAccountId", "w.choice_account_id"), entry("customerExternalId", "c.external_id"));
    private static final Map<String, String> CALLBACK_SORT = Map.ofEntries(
            entry("createdAt", "created_at"), entry("id", "id"), entry("notificationType", "notification_type"),
            entry("processed", "processed"));
    private static final Map<String, String> NOTIFICATION_SORT = Map.ofEntries(
            entry("createdAt", "n.created_at"), entry("id", "n.id"), entry("pushType", "n.push_type"),
            entry("source", "n.source"), entry("customerExternalId", "c.external_id"));
    private static final Map<String, String> SMS_SORT = Map.ofEntries(
            entry("createdAt", "s.created_at"), entry("id", "s.id"), entry("status", "s.status"),
            entry("purpose", "s.purpose"), entry("recipient", "s.recipient"));
    private static final Map<String, String> ADMIN_USER_SORT = Map.ofEntries(
            entry("id", "id"), entry("username", "username"), entry("status", "status"),
            entry("lastLoginAt", "last_login_at"), entry("email", "email"), entry("createdAt", "created_at"));
    private static final Map<String, String> ACTIVITY_SORT = Map.ofEntries(
            entry("createdAt", "created_at"), entry("id", "id"), entry("action", "action"));
    private static final Map<String, String> ADMIN_AUDIT_SORT = Map.ofEntries(
            entry("createdAt", "a.created_at"), entry("id", "a.id"), entry("action", "a.action"),
            entry("adminUsername", "u.username"));
    private static final Map<String, String> AUTH_AUDIT_SORT = Map.ofEntries(
            entry("createdAt", "a.created_at"), entry("id", "a.id"), entry("eventType", "a.event_type"),
            entry("outcome", "a.outcome"));

    public Map<String, Object> customers(Integer pageReq, Integer sizeReq, String search, String status, String fromDate, String toDate, String sort, String order) {
        int page = Pagination.page(pageReq), size = Pagination.size(sizeReq);
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        Query q = customerQuery(search, status, false, false, range);
        String orderBy = resolveSort(sort, order, CUSTOMER_SORT, "ORDER BY c.created_at DESC");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(q.sql + " " + orderBy + " LIMIT ? OFFSET ?", params(q.params, size, Pagination.offset(page, size)));
        maskCustomers(rows);
        rows.forEach(row -> row.put("kycStatusLabel", kycStatusLabel((String) row.get("kycStatus"))));
        long total = jdbcTemplate.queryForObject(customerQuery(search, status, true, false, range).sql, Long.class, q.params.toArray());
        return Pagination.response(rows, page, size, total);
    }

    public Map<String, Object> customerDetail(String id) {
        String sql = """
                SELECT c.id, c.external_id externalId, c.mobile_country_code mobileCountryCode, c.mobile, c.email,
                c.first_name firstName, c.last_name lastName, c.status, c.created_at createdAt, c.updated_at updatedAt,
                w.id walletId, w.balance_cache walletBalance, w.status walletStatus, w.choice_account_id choiceAccountId,
                w.currency walletCurrency, w.last_balance_update_at walletBalanceUpdatedAt,
                k.id kycId, k.status kycStatus, k.choice_onboarding_request_id choiceOnboardingRequestId,
                k.choice_user_id choiceUserId, k.choice_account_id kycChoiceAccountId, k.choice_account_type choiceAccountType,
                k.id_type idType, k.id_number idNumber, k.middle_name middleName, k.birthday, k.gender, k.address, k.kra_pin kraPin,
                k.rejection_reason_msgs rejectionReasonMsgs, k.id_front_url idFrontUrl, k.selfie_url selfieUrl,
                k.created_at kycSubmittedAt, k.updated_at kycUpdatedAt
                FROM customer c
                LEFT JOIN wallet w ON w.customer_id = c.id
                LEFT JOIN kyc_verification k ON k.id = (
                    SELECT k2.id FROM kyc_verification k2 WHERE k2.customer_id = c.id ORDER BY k2.created_at DESC LIMIT 1
                )
                WHERE (c.external_id = ? OR c.id = ?)
                """;
        var rows = jdbcTemplate.queryForList(sql, idParams(id));
        if (rows.isEmpty()) throw notFound("CUSTOMER_NOT_FOUND");
        Map<String, Object> row = rows.get(0);
        maskCustomer(row);
        row.put("idNumber", maskingService.maskIdNumber((String) row.get("idNumber"), securityContext.hasPermission("customer:view_pii")));
        row.put("kycStatusLabel", kycStatusLabel((String) row.get("kycStatus")));
        row.put("idTypeLabel", idTypeLabel((String) row.get("idType")));
        row.put("genderLabel", genderLabel(row.get("gender")));
        if (!securityContext.hasPermission("kyc:view_documents")) {
            row.remove("idFrontUrl");
            row.remove("selfieUrl");
        }
        Long customerId = ((Number) row.get("id")).longValue();
        row.put("deviceCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM device_token WHERE customer_id=?", Long.class, customerId));
        return row;
    }

    public Map<String, Object> customerSummary(String id) {
        Long customerId = customerPk(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalSent", jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount),0) FROM `transaction` WHERE customer_id=? AND type='TRANSFER' AND status='COMPLETED'", BigDecimal.class, customerId));
        m.put("totalDeposited", jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount),0) FROM `transaction` WHERE customer_id=? AND type='DEPOSIT' AND status='COMPLETED'", BigDecimal.class, customerId));
        m.put("transactionCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `transaction` WHERE customer_id=?", Long.class, customerId));
        m.put("failedTransactionCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `transaction` WHERE customer_id=? AND status='FAILED'", Long.class, customerId));
        var lastTx = jdbcTemplate.queryForList("SELECT MAX(created_at) lastAt FROM `transaction` WHERE customer_id=?", customerId);
        var lastAct = jdbcTemplate.queryForList("SELECT MAX(created_at) lastAt FROM activity_log WHERE customer_id=?", customerId);
        Object lastTxAt = lastTx.isEmpty() ? null : lastTx.get(0).get("lastAt");
        Object lastActAt = lastAct.isEmpty() ? null : lastAct.get(0).get("lastAt");
        m.put("lastActiveAt", lastTxAt != null ? lastTxAt : lastActAt);
        return m;
    }

    public Map<String, Object> customerTransactions(String id, Integer pageReq, Integer sizeReq, String type, String status, String fromDate, String toDate, String sort, String order) {
        Long customerId = customerPk(id);
        StringBuilder where = new StringBuilder("t.customer_id=?");
        List<Object> p = new ArrayList<>();
        p.add(customerId);
        if (type != null && !type.isBlank()) { where.append(" AND t.type=?"); p.add(type); }
        if (status != null && !status.isBlank()) { where.append(" AND t.status=?"); p.add(status); }
        DateRangeQuery.of(fromDate, toDate).apply("t.created_at", where, p);
        return page("SELECT t.external_id externalId, t.type, t.amount, t.currency, t.status, t.created_at createdAt FROM `transaction` t WHERE " + where,
                "SELECT COUNT(*) FROM `transaction` t WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, TX_SORT, "ORDER BY t.created_at DESC"), p.toArray());
    }

    public Map<String, Object> customerKyc(String id) {
        Long customerId = customerPk(id);
        var rows = jdbcTemplate.queryForList(
                "SELECT id, customer_id customerId, choice_onboarding_request_id choiceOnboardingRequestId, choice_user_id choiceUserId, "
                        + "choice_account_id choiceAccountId, choice_account_type choiceAccountType, status, id_type idType, id_number idNumber, "
                        + "middle_name middleName, birthday, gender, address, kra_pin kraPin, rejection_reason_msgs rejectionReasonMsgs, "
                        + "id_front_url idFrontUrl, selfie_url selfieUrl, created_at createdAt, updated_at updatedAt "
                        + "FROM kyc_verification WHERE customer_id=? ORDER BY created_at DESC LIMIT 1",
                customerId);
        if (rows.isEmpty()) return Map.of();
        Map<String, Object> row = rows.get(0);
        boolean docs = securityContext.hasPermission("kyc:view_documents");
        if (!docs) {
            row.remove("idFrontUrl");
            row.remove("selfieUrl");
        }
        row.put("idNumber", maskingService.maskIdNumber((String) row.get("idNumber"), securityContext.hasPermission("customer:view_pii")));
        row.put("kycStatusLabel", kycStatusLabel((String) row.get("status")));
        row.put("idTypeLabel", idTypeLabel((String) row.get("idType")));
        row.put("genderLabel", genderLabel(row.get("gender")));
        return row;
    }

    public Map<String, Object> customerActivity(String id, Integer pageReq, Integer sizeReq, String sort, String order) {
        Long customerId = customerPk(id);
        return page("SELECT id, action, resource_type resourceType, resource_id resourceId, created_at createdAt FROM activity_log WHERE customer_id=?",
                "SELECT COUNT(*) FROM activity_log WHERE customer_id=?", pageReq, sizeReq,
                resolveSort(sort, order, ACTIVITY_SORT, "ORDER BY created_at DESC"), customerId);
    }

    public Map<String, Object> kyc(Integer pageReq, Integer sizeReq, String status, String search, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (status != null && !status.isBlank()) { where.append(" AND k.status=?"); p.add(status); }
        if (search != null && !search.isBlank()) {
            where.append(" AND (c.external_id LIKE ? OR c.mobile LIKE ? OR k.choice_onboarding_request_id LIKE ?)");
            String s = "%" + search + "%";
            p.add(s); p.add(s); p.add(s);
        }
        DateRangeQuery.of(fromDate, toDate).apply("k.created_at", where, p);
        return page("SELECT k.id, k.choice_onboarding_request_id choiceOnboardingRequestId, k.status, k.id_type idType, k.created_at createdAt, c.external_id customerExternalId, c.first_name firstName, c.last_name lastName FROM kyc_verification k JOIN customer c ON c.id=k.customer_id WHERE " + where,
                "SELECT COUNT(*) FROM kyc_verification k JOIN customer c ON c.id=k.customer_id WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, KYC_SORT, "ORDER BY k.created_at DESC"), p.toArray());
    }

    public Map<String, Object> kycDetail(Long id) {
        var rows = jdbcTemplate.queryForList("SELECT k.*, c.external_id customerExternalId, c.first_name firstName, c.last_name lastName FROM kyc_verification k JOIN customer c ON c.id=k.customer_id WHERE k.id=?", id);
        if (rows.isEmpty()) throw notFound("KYC_NOT_FOUND");
        Map<String, Object> row = rows.get(0);
        if (!securityContext.hasPermission("kyc:view_documents")) { row.remove("id_front_url"); row.remove("selfie_url"); }
        row.put("id_number", maskingService.maskIdNumber((String) row.get("id_number"), securityContext.hasPermission("customer:view_pii")));
        return row;
    }

    public Map<String, Object> wallets(Integer pageReq, Integer sizeReq, String search, String status, String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (status != null && !status.isBlank()) { where.append(" AND w.status=?"); p.add(status); }
        if (search != null && !search.isBlank()) {
            where.append(" AND (c.external_id LIKE ? OR c.mobile LIKE ? OR w.choice_account_id LIKE ?)");
            String s = "%" + search + "%";
            p.add(s); p.add(s); p.add(s);
        }
        Map<String, Object> res = page("SELECT w.id, w.choice_account_id choiceAccountId, w.balance_cache balance, w.currency, w.status, w.created_at createdAt, c.external_id customerExternalId, c.mobile_country_code mobileCountryCode, c.mobile FROM wallet w JOIN customer c ON c.id=w.customer_id WHERE " + where,
                "SELECT COUNT(*) FROM wallet w JOIN customer c ON c.id=w.customer_id WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, WALLET_SORT, "ORDER BY w.created_at DESC"), p.toArray());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) res.get("content");
        content.forEach(this::maskCustomer);
        return res;
    }

    public Map<String, Object> walletDetail(Long id) {
        var rows = jdbcTemplate.queryForList("SELECT w.*, c.external_id customerExternalId, c.mobile_country_code mobileCountryCode, c.mobile FROM wallet w JOIN customer c ON c.id=w.customer_id WHERE w.id=?", id);
        if (rows.isEmpty()) throw notFound("WALLET_NOT_FOUND");
        Map<String, Object> row = rows.get(0);
        maskCustomer(row);
        return row;
    }

    public Map<String, Object> transactions(Integer pageReq, Integer sizeReq, String type, String status, String search, String customerId, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (type != null && !type.isBlank()) { where.append(" AND t.type=?"); p.add(type); }
        if (status != null && !status.isBlank()) { where.append(" AND t.status=?"); p.add(status); }
        if (customerId != null && !customerId.isBlank()) { where.append(" AND c.external_id=?"); p.add(customerId); }
        if (search != null && !search.isBlank()) {
            where.append(" AND (t.external_id LIKE ? OR t.choice_request_id LIKE ? OR c.external_id LIKE ?)");
            String s = "%" + search + "%";
            p.add(s); p.add(s); p.add(s);
        }
        DateRangeQuery.of(fromDate, toDate).apply("t.created_at", where, p);
        return page("SELECT t.external_id externalId, t.choice_request_id choiceRequestId, t.type, t.amount, t.currency, t.status, t.error_code errorCode, t.error_msg errorMsg, t.created_at createdAt, c.external_id customerExternalId FROM `transaction` t JOIN customer c ON c.id=t.customer_id WHERE " + where,
                "SELECT COUNT(*) FROM `transaction` t JOIN customer c ON c.id=t.customer_id WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, TX_SORT, "ORDER BY t.created_at DESC"), p.toArray());
    }

    public Map<String, Object> transactionDetail(String id) {
        var rows = jdbcTemplate.queryForList("SELECT t.*, c.external_id customerExternalId, w.id walletId, w.choice_account_id choiceAccountId FROM `transaction` t JOIN customer c ON c.id=t.customer_id JOIN wallet w ON w.id=t.wallet_id WHERE t.external_id=? OR t.id=?", id, numericId(id));
        if (rows.isEmpty()) throw notFound("TX_NOT_FOUND");
        return rows.get(0);
    }

    public Map<String, Object> failedTransactions(Integer pageReq, Integer sizeReq, String errorCode, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("t.status='FAILED'");
        List<Object> p = new ArrayList<>();
        if (errorCode != null && !errorCode.isBlank()) { where.append(" AND t.error_code=?"); p.add(errorCode); }
        DateRangeQuery.of(fromDate, toDate).apply("t.created_at", where, p);
        return page("SELECT t.external_id externalId, t.type, t.amount, t.currency, t.status, t.error_code errorCode, t.error_msg errorMsg, t.created_at createdAt, c.external_id customerExternalId FROM `transaction` t JOIN customer c ON c.id=t.customer_id WHERE " + where,
                "SELECT COUNT(*) FROM `transaction` t JOIN customer c ON c.id=t.customer_id WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, TX_SORT, "ORDER BY t.created_at DESC"), p.toArray());
    }

    public Map<String, Object> callbacks(Integer pageReq, Integer sizeReq, String type, Boolean processed, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (type != null && !type.isBlank()) { where.append(" AND notification_type=?"); p.add(type); }
        if (processed != null) { where.append(" AND processed=?"); p.add(processed); }
        DateRangeQuery.of(fromDate, toDate).apply("created_at", where, p);
        return page("SELECT id, choice_request_id choiceRequestId, notification_type notificationType, processed, processed_at processedAt, processing_error processingError, created_at createdAt FROM choice_bank_callback WHERE " + where,
                "SELECT COUNT(*) FROM choice_bank_callback WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, CALLBACK_SORT, "ORDER BY created_at DESC"), p.toArray());
    }

    public Map<String, Object> callbackDetail(Long id) {
        var rows = jdbcTemplate.queryForList("SELECT * FROM choice_bank_callback WHERE id=?", id);
        if (rows.isEmpty()) throw notFound("CALLBACK_NOT_FOUND");
        Map<String, Object> row = rows.get(0);
        if (!securityContext.hasPermission("callback:view")) { row.remove("raw_payload"); }
        return row;
    }

    public Map<String, Object> notifications(Integer pageReq, Integer sizeReq, String customerId, String pushType,
                                             String source, String batchId, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("n.deleted_at IS NULL");
        List<Object> p = new ArrayList<>();
        if (customerId != null && !customerId.isBlank()) {
            where.append(" AND (n.customer_id=? OR c.external_id=?)");
            p.add(numericId(customerId));
            p.add(customerId);
        }
        if (pushType != null && !pushType.isBlank()) { where.append(" AND n.push_type=?"); p.add(pushType); }
        if (source != null && !source.isBlank()) { where.append(" AND n.source=?"); p.add(source); }
        if (batchId != null && !batchId.isBlank()) { where.append(" AND n.batch_id=?"); p.add(batchId); }
        DateRangeQuery.of(fromDate, toDate).apply("n.created_at", where, p);
        return page(
                "SELECT n.id, n.public_id publicId, n.customer_id customerId, c.external_id customerExternalId, n.source, n.push_type pushType, n.notification_type notificationType, n.title, n.body, n.batch_id batchId, n.read_at readAt, n.created_at createdAt FROM customer_notification n LEFT JOIN customer c ON c.id=n.customer_id WHERE " + where,
                "SELECT COUNT(*) FROM customer_notification n LEFT JOIN customer c ON c.id=n.customer_id WHERE " + where,
                pageReq, sizeReq, resolveSort(sort, order, NOTIFICATION_SORT, "ORDER BY n.created_at DESC"), p.toArray());
    }

    public Map<String, Object> notificationDetail(Long id) {
        var rows = jdbcTemplate.queryForList(
                "SELECT n.*, c.external_id customerExternalId FROM customer_notification n LEFT JOIN customer c ON c.id=n.customer_id WHERE n.id=?", id);
        if (rows.isEmpty()) throw notFound("NOTIFICATION_NOT_FOUND");
        Map<String, Object> row = new LinkedHashMap<>(rows.get(0));
        row.put("attempts", jdbcTemplate.queryForList(
                "SELECT id, status, skip_reason skipReason, token_count tokenCount, success_count successCount, failure_count failureCount, error_message errorMessage, trigger_source triggerSource, created_by_admin_id createdByAdminId, created_at createdAt FROM push_delivery_log WHERE notification_id=? ORDER BY created_at DESC",
                id));
        return row;
    }

    public Map<String, Object> notificationSummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allTime", jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) count FROM push_delivery_log GROUP BY status"));
        m.put("today", jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) count FROM push_delivery_log WHERE DATE(created_at)=CURRENT_DATE GROUP BY status"));
        m.put("inboxTotal", count("customer_notification", "deleted_at IS NULL"));
        m.put("sentToday", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM push_delivery_log WHERE status IN ('SENT','PARTIAL') AND DATE(created_at)=CURRENT_DATE",
                Long.class));
        return m;
    }

    public Map<String, Object> smsMessages(Integer pageReq, Integer sizeReq, String status, String purpose,
                                           String recipient, String batchId, String fromDate, String toDate,
                                           String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" AND s.status=?");
            p.add(status);
        }
        if (purpose != null && !purpose.isBlank()) {
            where.append(" AND s.purpose=?");
            p.add(purpose);
        }
        if (recipient != null && !recipient.isBlank()) {
            where.append(" AND s.recipient LIKE ?");
            p.add("%" + recipient.trim() + "%");
        }
        if (batchId != null && !batchId.isBlank()) {
            where.append(" AND s.batch_id=?");
            p.add(batchId);
        }
        DateRangeQuery.of(fromDate, toDate).apply("s.created_at", where, p);
        Map<String, Object> result = page(
                "SELECT s.id, s.public_id publicId, s.batch_id batchId, s.recipient, s.customer_id customerId, "
                        + "c.external_id customerExternalId, s.purpose, s.otp_purpose otpPurpose, "
                        + "s.message_redacted messageRedacted, s.provider, s.provider_uid providerUid, "
                        + "s.status, s.error_message errorMessage, s.created_by_admin_id createdByAdminId, "
                        + "s.created_at createdAt, s.sent_at sentAt "
                        + "FROM sms_message s LEFT JOIN customer c ON c.id=s.customer_id WHERE " + where,
                "SELECT COUNT(*) FROM sms_message s LEFT JOIN customer c ON c.id=s.customer_id WHERE " + where,
                pageReq, sizeReq, resolveSort(sort, order, SMS_SORT, "ORDER BY s.created_at DESC"), p.toArray());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        if (content != null) {
            for (Map<String, Object> row : content) {
                row.put("recipientMasked", maskSmsRecipient((String) row.get("recipient")));
                row.remove("recipient");
            }
        }
        return result;
    }

    public Map<String, Object> smsDetail(Long id) {
        var rows = jdbcTemplate.queryForList(
                "SELECT s.id, s.public_id publicId, s.batch_id batchId, s.recipient, s.customer_id customerId, "
                        + "c.external_id customerExternalId, s.purpose, s.otp_purpose otpPurpose, "
                        + "s.otp_verification_id otpVerificationId, s.message_body messageBody, "
                        + "s.message_redacted messageRedacted, s.provider, s.provider_uid providerUid, "
                        + "s.status, s.error_message errorMessage, s.created_by_admin_id createdByAdminId, "
                        + "s.created_at createdAt, s.sent_at sentAt "
                        + "FROM sms_message s LEFT JOIN customer c ON c.id=s.customer_id WHERE s.id=?", id);
        if (rows.isEmpty()) {
            throw notFound("SMS_NOT_FOUND");
        }
        Map<String, Object> row = new LinkedHashMap<>(rows.get(0));
        String purpose = (String) row.get("purpose");
        row.put("recipientMasked", maskSmsRecipient((String) row.get("recipient")));
        row.remove("recipient");
        // Never expose raw AUTH_OTP body to admin UI
        if ("AUTH_OTP".equals(purpose)) {
            row.remove("messageBody");
        }
        row.put("attempts", jdbcTemplate.queryForList(
                "SELECT id, trigger_source triggerSource, status, provider_uid providerUid, "
                        + "error_message errorMessage, created_by_admin_id createdByAdminId, created_at createdAt "
                        + "FROM sms_delivery_attempt WHERE sms_message_id=? ORDER BY created_at DESC",
                id));
        return row;
    }

    private static String maskSmsRecipient(String recipient) {
        if (recipient == null || recipient.length() < 8) {
            return "****";
        }
        return recipient.substring(0, 3) + "****" + recipient.substring(recipient.length() - 4);
    }

    public List<Map<String, Object>> reportVolume(String groupBy, String fromDate, String toDate) {
        String fmt = groupBy(groupBy);
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        if (range.hasRange()) {
            return jdbcTemplate.queryForList(
                    "SELECT DATE_FORMAT(created_at, ?) period, SUM(CASE WHEN type='TRANSFER' THEN amount ELSE 0 END) transferVol, SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) depositVol, COUNT(*) txCount FROM `transaction` WHERE created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY) GROUP BY DATE_FORMAT(created_at, ?) ORDER BY period",
                    fmt, range.from().toString(), range.to().toString(), fmt);
        }
        return jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, ?) period, SUM(CASE WHEN type='TRANSFER' THEN amount ELSE 0 END) transferVol, SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) depositVol, COUNT(*) txCount FROM `transaction` GROUP BY DATE_FORMAT(created_at, ?) ORDER BY period", fmt, fmt);
    }

    public Map<String, Object> reportVolumeSummary(String fromDate, String toDate) {
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        String where = range.hasRange() ? " WHERE created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY)" : "";
        Object[] params = range.hasRange() ? new Object[]{range.from().toString(), range.to().toString()} : new Object[]{};
        Map<String, Object> m = jdbcTemplate.queryForMap(
                "SELECT COALESCE(SUM(amount),0) totalVolume, COUNT(*) totalTransactions, COALESCE(SUM(status='COMPLETED'),0) completedCount FROM `transaction`" + where, params);
        BigDecimal totalVol = (BigDecimal) m.get("totalVolume");
        long totalTx = ((Number) m.get("totalTransactions")).longValue();
        long completed = ((Number) m.get("completedCount")).longValue();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalVolume", totalVol);
        summary.put("totalTransactions", totalTx);
        long days = range.hasRange() ? java.time.temporal.ChronoUnit.DAYS.between(range.from(), range.to()) + 1 : 30;
        summary.put("avgDailyVolume", days > 0 ? totalVol.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP) : totalVol);
        summary.put("successRate", totalTx > 0 ? BigDecimal.valueOf(100.0 * completed / totalTx).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return summary;
    }

    public Map<String, Object> reportKycFunnel(String fromDate, String toDate) {
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        String custWhere = range.hasRange() ? " WHERE created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY)" : "";
        Object[] rangeParams = range.hasRange() ? new Object[]{range.from().toString(), range.to().toString()} : new Object[]{};
        long registered = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer" + custWhere, Long.class, rangeParams);
        String kycWhere = range.hasRange() ? " WHERE k.created_at >= ? AND k.created_at < DATE_ADD(?, INTERVAL 1 DAY)" : "";
        long kycSubmitted = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT k.customer_id) FROM kyc_verification k" + kycWhere, Long.class, rangeParams);
        long kycApproved = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT k.customer_id) FROM kyc_verification k" + kycWhere + (kycWhere.isEmpty() ? " WHERE" : " AND") + " k.status='APPROVED'", Long.class, rangeParams);
        String txWhere = range.hasRange() ? " WHERE t.status='COMPLETED' AND t.created_at >= ? AND t.created_at < DATE_ADD(?, INTERVAL 1 DAY)" : " WHERE t.status='COMPLETED'";
        long firstTx = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT t.customer_id) FROM `transaction` t" + txWhere, Long.class, rangeParams);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("registered", registered);
        m.put("kycSubmitted", kycSubmitted);
        m.put("kycApproved", kycApproved);
        m.put("firstTransaction", firstTx);
        m.put("conversionRegisteredToKyc", pct(kycSubmitted, registered));
        m.put("conversionKycToApproved", pct(kycApproved, kycSubmitted));
        m.put("conversionApprovedToTx", pct(firstTx, kycApproved));
        m.put("overallConversion", pct(firstTx, registered));
        var rejections = jdbcTemplate.queryForList("SELECT rejection_reason_msgs FROM kyc_verification WHERE status='REJECTED' AND rejection_reason_msgs IS NOT NULL LIMIT 100");
        m.put("topRejectionReasons", List.of());
        return m;
    }

    /**
     * Customer acquisition by period with wallets created for those registrants and a running total.
     * Uses a derived table so MySQL ONLY_FULL_GROUP_BY accepts the aggregations.
     */
    public List<Map<String, Object>> reportGrowth(String groupBy, String fromDate, String toDate) {
        String fmt = groupBy(groupBy);
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        String sql = """
                SELECT g.period, g.newCustomers, g.activeWallets,
                       SUM(g.newCustomers) OVER (ORDER BY g.period) cumulativeTotal
                FROM (
                  SELECT DATE_FORMAT(c.created_at, ?) period,
                         COUNT(DISTINCT c.id) newCustomers,
                         COUNT(DISTINCT w.id) activeWallets
                  FROM customer c
                  LEFT JOIN wallet w ON w.customer_id = c.id
                  %s
                  GROUP BY DATE_FORMAT(c.created_at, ?)
                ) g
                ORDER BY g.period
                """.formatted(range.hasRange()
                ? "WHERE c.created_at >= ? AND c.created_at < DATE_ADD(?, INTERVAL 1 DAY)"
                : "");
        if (range.hasRange()) {
            return jdbcTemplate.queryForList(sql, fmt, range.from().toString(), range.to().toString(), fmt);
        }
        return jdbcTemplate.queryForList(sql, fmt, fmt);
    }

    /**
     * Customer activity from activity_log (product actions: transfer, KYC, wallet, etc.).
     */
    public Map<String, Object> auditLog(Integer pageReq, Integer sizeReq, String action, String customerId, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (action != null && !action.isBlank()) { where.append(" AND action=?"); p.add(action); }
        if (customerId != null && !customerId.isBlank()) { where.append(" AND customer_id IN (SELECT id FROM customer WHERE external_id=? OR id=?)"); p.add(customerId); p.add(numericId(customerId)); }
        DateRangeQuery.of(fromDate, toDate).apply("created_at", where, p);
        return page("SELECT id, customer_id customerId, action, resource_type resourceType, resource_id resourceId, created_at createdAt FROM activity_log WHERE " + where,
                "SELECT COUNT(*) FROM activity_log WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, ACTIVITY_SORT, "ORDER BY created_at DESC"), p.toArray());
    }

    /**
     * Admin mutation audit trail from admin_audit_log.
     */
    public Map<String, Object> adminAuditLog(Integer pageReq, Integer sizeReq, String action, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (action != null && !action.isBlank()) { where.append(" AND a.action=?"); p.add(action); }
        DateRangeQuery.of(fromDate, toDate).apply("a.created_at", where, p);
        return page("SELECT a.id, a.action, a.entity_type entityType, a.entity_id entityId, a.reason, a.created_at createdAt, u.username adminUsername FROM admin_audit_log a JOIN admin_user u ON u.id=a.admin_user_id WHERE " + where,
                "SELECT COUNT(*) FROM admin_audit_log a WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, ADMIN_AUDIT_SORT, "ORDER BY a.created_at DESC"), p.toArray());
    }

    /**
     * Auth security events from auth_audit_event (login, lockout, device bind, PIN).
     */
    public Map<String, Object> authAuditLog(Integer pageReq, Integer sizeReq, String eventType, String fromDate, String toDate, String sort, String order) {
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (eventType != null && !eventType.isBlank()) { where.append(" AND a.event_type=?"); p.add(eventType); }
        DateRangeQuery.of(fromDate, toDate).apply("a.created_at", where, p);
        return page(
                "SELECT a.id, a.event_type eventType, a.outcome, a.identifier_masked identifierMasked, a.detail, a.created_at createdAt, c.external_id customerExternalId FROM auth_audit_event a LEFT JOIN customer c ON c.id=a.customer_id WHERE " + where,
                "SELECT COUNT(*) FROM auth_audit_event a WHERE " + where, pageReq, sizeReq,
                resolveSort(sort, order, AUTH_AUDIT_SORT, "ORDER BY a.created_at DESC"), p.toArray());
    }

    public List<Map<String, Object>> menus() {
        List<Map<String, Object>> flat = jdbcTemplate.queryForList("SELECT id, name, route, icon, parent_id parentId, sort_order sortOrder FROM admin_menu ORDER BY COALESCE(parent_id, id), sort_order");
        return buildMenuTree(flat);
    }

    public List<Map<String, Object>> roles() {
        return jdbcTemplate.queryForList("SELECT r.id, r.name, r.description, COUNT(DISTINCT rm.menu_id) menuCount, COUNT(DISTINCT rp.permission_id) permissionCount FROM admin_role r LEFT JOIN admin_role_menu rm ON rm.role_id=r.id LEFT JOIN admin_role_permission rp ON rp.role_id=r.id GROUP BY r.id ORDER BY r.name");
    }

    public Map<String, Object> roleDetail(Long id) {
        Map<String, Object> r = jdbcTemplate.queryForMap("SELECT id, name, description FROM admin_role WHERE id=?", id);
        r.put("menuIds", jdbcTemplate.queryForList("SELECT menu_id FROM admin_role_menu WHERE role_id=?", Long.class, id));
        r.put("permissionCodes", jdbcTemplate.queryForList("SELECT p.code FROM admin_role_permission rp JOIN admin_permission p ON p.id=rp.permission_id WHERE rp.role_id=?", String.class, id));
        return r;
    }

    public Map<String, Object> adminUsers(Integer pageReq, Integer sizeReq, String sort, String order) {
        return page("SELECT id, external_id externalId, username, email, full_name fullName, status, last_login_at lastLoginAt, created_at createdAt FROM admin_user",
                "SELECT COUNT(*) FROM admin_user", pageReq, sizeReq,
                resolveSort(sort, order, ADMIN_USER_SORT, "ORDER BY created_at DESC"));
    }

    public Map<String, Object> adminUser(Long id) {
        Map<String, Object> u = jdbcTemplate.queryForMap("SELECT id, external_id externalId, username, email, full_name fullName, status, last_login_at lastLoginAt FROM admin_user WHERE id=?", id);
        u.put("roleIds", jdbcTemplate.queryForList("SELECT role_id FROM admin_user_role WHERE user_id=?", Long.class, id));
        return u;
    }

    public List<Map<String, Object>> permissions() {
        return jdbcTemplate.queryForList("SELECT code, description FROM admin_permission ORDER BY code");
    }

    private List<Map<String, Object>> buildMenuTree(List<Map<String, Object>> flat) {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> row : flat) {
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(((Number) row.get("id")).longValue(), node);
        }
        for (Map<String, Object> row : flat) {
            Object parentId = row.get("parentId");
            Map<String, Object> node = byId.get(((Number) row.get("id")).longValue());
            if (parentId == null) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) byId.get(((Number) parentId).longValue()).get("children");
                children.add(node);
            }
        }
        return roots;
    }

    private BigDecimal pct(long part, long whole) {
        if (whole == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(100.0 * part / whole).setScale(1, RoundingMode.HALF_UP);
    }

    private String groupBy(String groupBy) {
        return "month".equals(groupBy) ? "%Y-%m" : "week".equals(groupBy) ? "%x-W%v" : "%Y-%m-%d";
    }

    private DateRangeQuery resolveRange(String fromDate, String toDate, Integer days) {
        DateRangeQuery range = DateRangeQuery.of(fromDate, toDate);
        return range;
    }

    private long countWithRange(String table, String cond, String col, DateRangeQuery range) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(table).append(" WHERE ").append(cond);
        List<Object> p = new ArrayList<>();
        range.apply(col, sql, p);
        Long v = jdbcTemplate.queryForObject(sql.toString(), Long.class, p.toArray());
        return v == null ? 0 : v;
    }

    private BigDecimal sumTxInRange(DateRangeQuery range) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM `transaction` WHERE created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY)",
                BigDecimal.class, range.from().toString(), range.to().toString());
    }

    private long count(String table, String where) {
        Long v = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
        return v == null ? 0 : v;
    }

    /**
     * Paginated query with an explicit ORDER BY clause (never client-supplied raw SQL).
     * @param orderBySql resolved via {@link #resolveSort}; must start with {@code ORDER BY}
     */
    private Map<String, Object> page(String select, String count, Integer pageReq, Integer sizeReq, String orderBySql, Object... params) {
        int page = Pagination.page(pageReq), size = Pagination.size(sizeReq);
        List<Object> p = new ArrayList<>(List.of(params));
        List<Object> listParams = new ArrayList<>(p);
        listParams.add(size);
        listParams.add(Pagination.offset(page, size));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(select + " " + orderBySql + " LIMIT ? OFFSET ?", listParams.toArray());
        Long totalObj = jdbcTemplate.queryForObject(count, Long.class, p.toArray());
        long total = totalObj == null ? 0 : totalObj;
        return Pagination.response(rows, page, size, total);
    }

    /**
     * Maps a client sort key to a whitelist column; falls back to {@code defaultOrderBy} when unknown.
     */
    private String resolveSort(String sort, String order, Map<String, String> allowed, String defaultOrderBy) {
        if (sort == null || sort.isBlank() || !allowed.containsKey(sort)) {
            return defaultOrderBy;
        }
        String dir = "asc".equalsIgnoreCase(order) ? "ASC" : "DESC";
        return "ORDER BY " + allowed.get(sort) + " " + dir;
    }

    private Query customerQuery(String search, String status, boolean count, boolean detail, DateRangeQuery range) {
        String select = count ? "SELECT COUNT(*)" : "SELECT c.id, c.external_id externalId, c.mobile_country_code mobileCountryCode, c.mobile, c.email, c.first_name firstName, c.last_name lastName, c.status, c.created_at createdAt, w.balance_cache walletBalance, w.status walletStatus, k.status kycStatus";
        StringBuilder sql = new StringBuilder(select).append(" FROM customer c LEFT JOIN wallet w ON w.customer_id=c.id LEFT JOIN kyc_verification k ON k.customer_id=c.id WHERE 1=1");
        List<Object> p = new ArrayList<>();
        if (detail) {
            sql.append(" AND (c.external_id=? OR c.id=?)");
        } else {
            if (status != null && !status.isBlank()) { sql.append(" AND c.status=?"); p.add(status); }
            if (search != null && !search.isBlank()) {
                sql.append(" AND (c.external_id LIKE ? OR c.mobile LIKE ? OR c.email LIKE ? OR c.first_name LIKE ? OR c.last_name LIKE ?)");
                String s = "%" + search + "%";
                p.add(s); p.add(s); p.add(s); p.add(s); p.add(s);
            }
            range.apply("c.created_at", sql, p);
        }
        return new Query(sql.toString(), p);
    }

    private Object[] idParams(String id) { return new Object[]{id, numericId(id)}; }

    private Long numericId(String id) { try { return Long.parseLong(id); } catch (Exception e) { return -1L; } }

    private Long customerPk(String id) {
        var rows = jdbcTemplate.queryForList("SELECT id FROM customer WHERE external_id=? OR id=?", id, numericId(id));
        if (rows.isEmpty()) throw notFound("CUSTOMER_NOT_FOUND");
        return ((Number) rows.get(0).get("id")).longValue();
    }

    private void maskCustomers(List<Map<String, Object>> rows) { rows.forEach(this::maskCustomer); }

    private void maskCustomer(Map<String, Object> row) {
        boolean pii = securityContext.hasPermission("customer:view_pii");
        row.put("mobile", maskingService.maskMobile((String) row.get("mobileCountryCode"), (String) row.get("mobile"), pii));
        row.put("email", maskingService.maskEmail((String) row.get("email"), pii));
    }

    private String kycStatusLabel(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) return "Not started";
        return switch (rawStatus) {
            case "NOT_STARTED" -> "Not started";
            case "1" -> "Pending review";
            case "7" -> "Approved";
            default -> "Rejected";
        };
    }

    private String idTypeLabel(String idType) {
        if (idType == null || idType.isBlank()) return null;
        return switch (idType) {
            case "101" -> "National ID";
            case "102" -> "Alien ID";
            case "103" -> "Passport";
            default -> idType;
        };
    }

    private String genderLabel(Object gender) {
        if (gender == null) return null;
        int value = gender instanceof Number n ? n.intValue() : Integer.parseInt(gender.toString());
        return value == 0 ? "Female" : "Male";
    }

    private BusinessException notFound(String code) { return new BusinessException(code, "Resource not found", HttpStatus.NOT_FOUND); }

    private Object[] params(List<Object> base, Object... tail) {
        List<Object> all = new ArrayList<>(base);
        all.addAll(List.of(tail));
        return all.toArray();
    }

    private record Query(String sql, List<Object> params) {}
}
