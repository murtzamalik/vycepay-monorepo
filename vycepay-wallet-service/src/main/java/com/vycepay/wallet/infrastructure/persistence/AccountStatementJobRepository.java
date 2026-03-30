package com.vycepay.wallet.infrastructure.persistence;

import com.vycepay.wallet.domain.model.AccountStatementJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountStatementJobRepository extends JpaRepository<AccountStatementJob, Long> {

    Optional<AccountStatementJob> findByChoiceRequestId(String choiceRequestId);

    Optional<AccountStatementJob> findByChoiceRequestIdAndCustomerId(String choiceRequestId, Long customerId);
}
