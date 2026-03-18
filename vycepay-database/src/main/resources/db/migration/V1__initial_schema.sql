-- VycePay Initial Schema
-- Single MySQL database for all services

-- customer: Our user identity (before Choice account exists)
CREATE TABLE customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_id VARCHAR(64) UNIQUE NOT NULL,
  mobile_country_code VARCHAR(5) NOT NULL,
  mobile VARCHAR(20) NOT NULL,
  email VARCHAR(255),
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  status VARCHAR(20) DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mobile (mobile_country_code, mobile)
);

-- kyc_verification: Tracks Choice onboarding and KYC status
CREATE TABLE kyc_verification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  choice_onboarding_request_id VARCHAR(128),
  choice_user_id VARCHAR(128),
  choice_account_id VARCHAR(128),
  choice_account_type VARCHAR(16),
  status VARCHAR(32) NOT NULL,
  id_type VARCHAR(16),
  id_number VARCHAR(64),
  id_front_url VARCHAR(512),
  selfie_url VARCHAR(512),
  rejection_reason_ids TEXT,
  rejection_reason_msgs TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES customer(id),
  UNIQUE KEY uk_choice_onboarding (choice_onboarding_request_id),
  KEY idx_customer_status (customer_id, status)
);

-- wallet: Choice account mapping and cached balance
CREATE TABLE wallet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  choice_account_id VARCHAR(128) NOT NULL,
  choice_account_type VARCHAR(16) DEFAULT 'C002',
  balance_cache DECIMAL(18,2) DEFAULT 0,
  currency VARCHAR(3) DEFAULT 'KES',
  status VARCHAR(20) DEFAULT 'ACTIVE',
  last_balance_update_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES customer(id),
  UNIQUE KEY uk_choice_account (choice_account_id),
  KEY idx_customer (customer_id)
);

-- transaction: Pending and completed transactions
CREATE TABLE transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_id VARCHAR(64) UNIQUE NOT NULL,
  customer_id BIGINT NOT NULL,
  wallet_id BIGINT NOT NULL,
  choice_tx_id VARCHAR(128),
  choice_request_id VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'KES',
  fee_amount DECIMAL(18,2) DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  payee_bank_code VARCHAR(32),
  payee_account_id VARCHAR(128),
  payee_account_name VARCHAR(255),
  remark VARCHAR(256),
  error_code VARCHAR(32),
  error_msg TEXT,
  idempotency_key VARCHAR(128) UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  completed_at TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES customer(id),
  FOREIGN KEY (wallet_id) REFERENCES wallet(id),
  KEY idx_choice_tx (choice_tx_id),
  KEY idx_choice_request (choice_request_id),
  KEY idx_customer_created (customer_id, created_at),
  KEY idx_idempotency (idempotency_key)
);

-- choice_bank_callback: Audit of all callbacks
CREATE TABLE choice_bank_callback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  choice_request_id VARCHAR(128),
  notification_type VARCHAR(8) NOT NULL,
  raw_payload LONGTEXT NOT NULL,
  processed BOOLEAN DEFAULT FALSE,
  processed_at TIMESTAMP NULL,
  processing_error TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_choice_request_type (choice_request_id, notification_type),
  KEY idx_notification_processed (notification_type, processed),
  KEY idx_created (created_at)
);

-- activity_log: Compliance audit trail
CREATE TABLE activity_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT,
  action VARCHAR(64) NOT NULL,
  resource_type VARCHAR(32),
  resource_id VARCHAR(128),
  ip_address VARCHAR(45),
  user_agent VARCHAR(512),
  device_id VARCHAR(128),
  metadata JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_customer_action (customer_id, action),
  KEY idx_created (created_at)
);

-- otp_verification: Our OTP for registration (distinct from Choice OTP for tx/onboarding)
CREATE TABLE otp_verification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mobile_country_code VARCHAR(5) NOT NULL,
  mobile VARCHAR(20) NOT NULL,
  otp_code VARCHAR(10) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_mobile_expires (mobile_country_code, mobile, expires_at)
);
