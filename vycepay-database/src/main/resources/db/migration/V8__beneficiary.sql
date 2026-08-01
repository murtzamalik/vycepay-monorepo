-- Saved transfer beneficiaries (per customer). Soft-delete via deleted_at.
-- Unique key on identity fields; re-save restores soft-deleted rows (no second insert).

CREATE TABLE beneficiary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_id CHAR(36) NOT NULL,
  customer_id BIGINT NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  account_type TINYINT NOT NULL,
  payee_bank_code VARCHAR(32) NOT NULL DEFAULT '',
  payee_account_id VARCHAR(64) NOT NULL,
  payee_account_name VARCHAR(120) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,
  UNIQUE KEY uk_beneficiary_external_id (external_id),
  UNIQUE KEY uk_beneficiary_identity (customer_id, account_type, payee_bank_code, payee_account_id),
  KEY idx_beneficiary_customer_updated (customer_id, deleted_at, updated_at),
  CONSTRAINT fk_beneficiary_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);
