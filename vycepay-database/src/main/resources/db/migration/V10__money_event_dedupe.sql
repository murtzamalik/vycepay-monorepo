-- Money-event push dedupe: one TRANSACTION_RESULT inbox row per Choice txId per customer.
-- Unique choice_tx_id prevents concurrent 0002+0003 from inserting two inbound deposits.

ALTER TABLE customer_notification
  ADD COLUMN dedupe_key VARCHAR(160) NULL COMMENT 'TX:{choiceTxId} for money events; NULL for KYC/statement/admin';

ALTER TABLE customer_notification
  ADD UNIQUE KEY uk_customer_notification_dedupe (customer_id, dedupe_key);

ALTER TABLE `transaction`
  ADD UNIQUE KEY uk_transaction_choice_tx (choice_tx_id);
