-- Money-event SMS outbox: park provider failures for scheduled retry (callback-service only).
-- Does not alter sms_message (OTP / admin bulk).
CREATE TABLE sms_outbox (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  public_id CHAR(36) NOT NULL,
  customer_id BIGINT NULL,
  notification_id BIGINT NULL,
  dedupe_key VARCHAR(160) NOT NULL COMMENT 'e.g. TX:{choiceTxId} — one outbox row per money event',
  recipient VARCHAR(20) NOT NULL COMMENT 'E.164 digits without plus',
  message_body VARCHAR(640) NOT NULL,
  status VARCHAR(16) NOT NULL COMMENT 'PENDING, SENDING, SENT, DEAD',
  attempt_count INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 10,
  next_attempt_at TIMESTAMP NOT NULL,
  last_error VARCHAR(255) NULL,
  provider_uid VARCHAR(64) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL,
  UNIQUE KEY uk_sms_outbox_public_id (public_id),
  UNIQUE KEY uk_sms_outbox_dedupe (dedupe_key),
  KEY idx_sms_outbox_due (status, next_attempt_at),
  KEY idx_sms_outbox_customer_created (customer_id, created_at),
  CONSTRAINT fk_sms_outbox_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  CONSTRAINT fk_sms_outbox_notification FOREIGN KEY (notification_id) REFERENCES customer_notification(id)
);
