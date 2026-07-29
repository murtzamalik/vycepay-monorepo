-- Customer credentials (username + PIN hash), single-device binding, OTP purpose, auth audit

ALTER TABLE customer
  ADD COLUMN username VARCHAR(20) NULL AFTER mobile,
  ADD COLUMN username_normalized VARCHAR(20) NULL AFTER username,
  ADD COLUMN pin_hash VARCHAR(100) NULL AFTER username_normalized,
  ADD COLUMN pin_failed_attempts INT NOT NULL DEFAULT 0 AFTER pin_hash,
  ADD COLUMN pin_locked_until TIMESTAMP NULL AFTER pin_failed_attempts,
  ADD COLUMN credentials_set_at TIMESTAMP NULL AFTER pin_locked_until,
  ADD UNIQUE KEY uk_username_normalized (username_normalized);

-- One active device (IMEI / ANDROID_ID fingerprint) per customer
CREATE TABLE customer_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  imei VARCHAR(128) NOT NULL,
  platform VARCHAR(16) NULL,
  bound_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_customer_device_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  UNIQUE KEY uk_customer_device_customer (customer_id),
  KEY idx_customer_device_imei (imei)
);

-- OTP purpose: SIGNUP | DEVICE_BIND | PIN_RESET | CREDENTIALS_MIGRATE
ALTER TABLE otp_verification
  ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'SIGNUP' AFTER mobile,
  ADD KEY idx_otp_purpose_mobile (purpose, mobile_country_code, mobile, expires_at);

-- Security audit for login / lockout / device bind / pin reset (no secrets)
CREATE TABLE auth_audit_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NULL,
  event_type VARCHAR(64) NOT NULL,
  outcome VARCHAR(32) NOT NULL,
  identifier_masked VARCHAR(64) NULL,
  detail VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_auth_audit_customer (customer_id, created_at),
  KEY idx_auth_audit_type (event_type, created_at)
);
