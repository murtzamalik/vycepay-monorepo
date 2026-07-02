package com.vycepay.admin.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Parses and validates admin date-range query parameters for SQL filters. */
public final class DateRangeQuery {
    private static final int MAX_DAYS = 366;

    private final LocalDate from;
    private final LocalDate to;

    private DateRangeQuery(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    public static DateRangeQuery of(String fromDate, String toDate) {
        if ((fromDate == null || fromDate.isBlank()) && (toDate == null || toDate.isBlank())) {
            return new DateRangeQuery(null, null);
        }
        if (fromDate == null || fromDate.isBlank() || toDate == null || toDate.isBlank()) {
            throw new BusinessException("INVALID_DATE_RANGE", "Both fromDate and toDate are required", HttpStatus.BAD_REQUEST);
        }
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        if (from.isAfter(to)) {
            throw new BusinessException("INVALID_DATE_RANGE", "fromDate must be on or before toDate", HttpStatus.BAD_REQUEST);
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new BusinessException("INVALID_DATE_RANGE", "Date range cannot exceed " + MAX_DAYS + " days", HttpStatus.BAD_REQUEST);
        }
        return new DateRangeQuery(from, to);
    }

    public boolean hasRange() {
        return from != null && to != null;
    }

    public LocalDate from() {
        return from;
    }

    public LocalDate to() {
        return to;
    }

    /** Appends `column >= ? AND column < ?+1day` to where clause; adds params. */
    public void apply(String column, StringBuilder where, List<Object> params) {
        if (!hasRange()) {
            return;
        }
        where.append(" AND ").append(column).append(" >= ? AND ").append(column).append(" < DATE_ADD(?, INTERVAL 1 DAY)");
        params.add(from.toString());
        params.add(to.toString());
    }
}
