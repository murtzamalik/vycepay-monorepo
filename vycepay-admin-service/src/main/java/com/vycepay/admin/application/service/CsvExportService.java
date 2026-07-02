package com.vycepay.admin.application.service;

import com.vycepay.admin.config.AdminProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Produces bounded CSV exports and records export audit events. */
@Service
public class CsvExportService {
    private final JdbcTemplate jdbcTemplate;
    private final AdminSecurityContext securityContext;
    private final AdminAuditService auditService;
    private final PiiMaskingService maskingService;
    private final AdminProperties properties;

    public CsvExportService(JdbcTemplate jdbcTemplate, AdminSecurityContext securityContext, AdminAuditService auditService, PiiMaskingService maskingService, AdminProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.securityContext = securityContext;
        this.auditService = auditService;
        this.maskingService = maskingService;
        this.properties = properties;
    }

    public String customers(HttpServletRequest request, String search, String status, String fromDate, String toDate) {
        int limit = properties.getExport().getMaxRows();
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (status != null && !status.isBlank()) { where.append(" AND status=?"); p.add(status); }
        if (search != null && !search.isBlank()) {
            where.append(" AND (external_id LIKE ? OR mobile LIKE ? OR email LIKE ?)");
            String s = "%" + search + "%";
            p.add(s); p.add(s); p.add(s);
        }
        DateRangeQuery.of(fromDate, toDate).apply("created_at", where, p);
        p.add(limit);
        var rows = jdbcTemplate.queryForList("SELECT external_id, mobile_country_code, mobile, email, status, created_at FROM customer WHERE " + where + " ORDER BY created_at DESC LIMIT ?", p.toArray());
        boolean pii = securityContext.hasPermission("customer:view_pii");
        rows.forEach(row -> {
            row.put("mobile", maskingService.maskMobile((String) row.remove("mobile_country_code"), (String) row.get("mobile"), pii));
            row.put("email", maskingService.maskEmail((String) row.get("email"), pii));
        });
        auditService.log(securityContext.currentAdmin(), "EXPORT_CUSTOMERS", "customer", null, null, "{\"rows\":" + rows.size() + ",\"piiIncluded\":" + pii + "}", request);
        return toCsv(rows);
    }

    public String transactions(HttpServletRequest request, String type, String status, String search, String fromDate, String toDate) {
        int limit = properties.getExport().getMaxRows();
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (type != null && !type.isBlank()) { where.append(" AND type=?"); p.add(type); }
        if (status != null && !status.isBlank()) { where.append(" AND status=?"); p.add(status); }
        if (search != null && !search.isBlank()) { where.append(" AND external_id LIKE ?"); p.add("%" + search + "%"); }
        DateRangeQuery.of(fromDate, toDate).apply("created_at", where, p);
        p.add(limit);
        var rows = jdbcTemplate.queryForList("SELECT external_id,type,amount,currency,status,created_at FROM `transaction` WHERE " + where + " ORDER BY created_at DESC LIMIT ?", p.toArray());
        auditService.log(securityContext.currentAdmin(), "EXPORT_TRANSACTIONS", "transaction", null, null, "{\"rows\":" + rows.size() + "}", request);
        return toCsv(rows);
    }

    public String auditLog(HttpServletRequest request, String action, String fromDate, String toDate) {
        int limit = properties.getExport().getMaxRows();
        StringBuilder where = new StringBuilder("1=1");
        List<Object> p = new ArrayList<>();
        if (action != null && !action.isBlank()) { where.append(" AND action=?"); p.add(action); }
        DateRangeQuery.of(fromDate, toDate).apply("created_at", where, p);
        p.add(limit);
        var rows = jdbcTemplate.queryForList("SELECT id,action,resource_type,resource_id,created_at FROM activity_log WHERE " + where + " ORDER BY created_at DESC LIMIT ?", p.toArray());
        auditService.log(securityContext.currentAdmin(), "EXPORT_AUDIT_LOG", "activity_log", null, null, "{\"rows\":" + rows.size() + "}", request);
        return toCsv(rows);
    }

    private String toCsv(java.util.List<java.util.Map<String, Object>> rows) {
        if (rows.isEmpty()) return "\n";
        StringBuilder out = new StringBuilder(String.join(",", rows.get(0).keySet())).append('\n');
        for (var row : rows) {
            boolean first = true;
            for (Object val : row.values()) {
                if (!first) out.append(',');
                first = false;
                out.append('"').append(String.valueOf(val == null ? "" : val).replace("\"", "\"\"")).append('"');
            }
            out.append('\n');
        }
        return out.toString();
    }
}
