package com.vycepay.transaction.infrastructure.persistence;

import com.vycepay.transaction.domain.model.Transaction;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for complex transaction queries.
 * Use for filtering by status, type, date range, amount, etc.
 */
public final class TransactionSpecification {

    private TransactionSpecification() {
    }

    /**
     * Specification for customer-scoped transactions.
     */
    public static Specification<Transaction> forCustomer(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
    }

    /**
     * Filter by status (1=PENDING, 2=PROCESSING, 4=FAILED, 8=SUCCESS).
     */
    public static Specification<Transaction> withStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filter by transaction type (TRANSFER, DEPOSIT).
     */
    public static Specification<Transaction> withType(String type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    /**
     * Filter by created date range (inclusive).
     */
    public static Specification<Transaction> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            return cb.conjunction();
        };
    }

    /**
     * Filter by amount range (inclusive).
     */
    public static Specification<Transaction> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("amount"), min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("amount"), min);
            }
            if (max != null) {
                return cb.lessThanOrEqualTo(root.get("amount"), max);
            }
            return cb.conjunction();
        };
    }

    /**
     * Combines specifications with AND. Null specs are ignored.
     */
    @SafeVarargs
    public static Specification<Transaction> allOf(Specification<Transaction>... specs) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<Transaction> spec : specs) {
                if (spec != null) {
                    predicates.add(spec.toPredicate(root, query, cb));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
