package com.vycepay.transaction.application;

import com.vycepay.transaction.domain.model.Transaction;

/**
 * Local transaction plus Choice Bank customer {@code msg} from the creating call.
 */
public record TransactionChoiceOutcome(Transaction transaction, String choiceMsg) {
}
