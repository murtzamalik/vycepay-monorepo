package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.AccountStatementJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountStatementJobRepository extends JpaRepository<AccountStatementJob, Long> {

    Optional<AccountStatementJob> findByChoiceRequestId(String choiceRequestId);
}
