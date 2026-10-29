package com.vycepay.common.apprelease;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Reads active {@code app_release_policy} by platform.
 */
public class JdbcAppReleasePolicyAdapter implements AppReleasePolicyPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAppReleasePolicyAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AppReleasePolicy> findActiveByPlatform(String platform) {
        String normalized = AppReleasePolicyDefaults.normalizePlatform(platform);
        List<AppReleasePolicy> rows = jdbcTemplate.query(
                "SELECT platform, min_supported_version_code, latest_version_code, latest_version_name, "
                        + "update_title, update_message, store_url, apk_url, maintenance, maintenance_message, active "
                        + "FROM app_release_policy WHERE platform=? AND active=1 LIMIT 1",
                (rs, i) -> new AppReleasePolicy(
                        rs.getString("platform"),
                        rs.getInt("min_supported_version_code"),
                        rs.getInt("latest_version_code"),
                        rs.getString("latest_version_name"),
                        rs.getString("update_title"),
                        rs.getString("update_message"),
                        rs.getString("store_url"),
                        rs.getString("apk_url"),
                        rs.getInt("maintenance") == 1,
                        rs.getString("maintenance_message"),
                        rs.getInt("active") == 1
                ),
                normalized);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }
}
