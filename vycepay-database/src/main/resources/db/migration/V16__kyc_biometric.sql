-- Biometric pre-KYC sessions (WeVipers OCR / liveness / face match) and attempt audit trail.

CREATE TABLE kyc_biometric_session (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  public_id         CHAR(36) NOT NULL,
  customer_id       BIGINT NOT NULL,
  id_type           VARCHAR(16) NULL,
  document_country  VARCHAR(8) NULL,
  status            VARCHAR(32) NOT NULL,
  current_step      VARCHAR(32) NULL,
  fail_reason_code  VARCHAR(64) NULL,
  fail_reason_msg   VARCHAR(512) NULL,
  id_front_url      VARCHAR(512) NULL,
  id_back_url       VARCHAR(512) NULL,
  id_portrait_url   VARCHAR(512) NULL,
  selfie_url        VARCHAR(512) NULL,
  doc_video_url     VARCHAR(512) NULL,
  face_video_url    VARCHAR(512) NULL,
  ocr_payload_json  JSON NULL,
  face_similarity   DECIMAL(6,2) NULL,
  face_match        TINYINT NULL,
  attempt_count     INT NOT NULL DEFAULT 0,
  max_attempts      INT NOT NULL DEFAULT 5,
  expires_at        TIMESTAMP NULL,
  kyc_verification_id BIGINT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_kyc_bio_session_public (public_id),
  KEY idx_kyc_bio_session_customer_status (customer_id, status),
  CONSTRAINT fk_kyc_bio_session_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE kyc_biometric_attempt (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  public_id            CHAR(36) NOT NULL,
  session_id           BIGINT NOT NULL,
  customer_id          BIGINT NOT NULL,
  step                 VARCHAR(32) NOT NULL,
  attempt_no           INT NOT NULL,
  outcome              VARCHAR(16) NOT NULL,
  vendor_http_status   INT NULL,
  latency_ms           INT NULL,
  liveness_result      VARCHAR(32) NULL,
  confidence_score     DECIMAL(6,4) NULL,
  match_result         TINYINT NULL,
  similarity           DECIMAL(6,2) NULL,
  fail_code            VARCHAR(64) NULL,
  fail_message         VARCHAR(512) NULL,
  media_url_1          VARCHAR(512) NULL,
  media_url_2          VARCHAR(512) NULL,
  request_meta_json    JSON NULL,
  response_json        JSON NULL,
  request_id           VARCHAR(64) NULL,
  created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_kyc_bio_attempt_public (public_id),
  KEY idx_kyc_bio_attempt_session_step (session_id, step, attempt_no),
  KEY idx_kyc_bio_attempt_customer_created (customer_id, created_at),
  KEY idx_kyc_bio_attempt_outcome_fail (outcome, fail_code),
  CONSTRAINT fk_kyc_bio_attempt_session FOREIGN KEY (session_id) REFERENCES kyc_biometric_session(id)
);

ALTER TABLE kyc_verification
  ADD COLUMN biometric_session_id BIGINT NULL AFTER customer_id,
  ADD COLUMN id_back_url VARCHAR(512) NULL AFTER id_front_url,
  ADD COLUMN id_portrait_url VARCHAR(512) NULL AFTER id_back_url;

ALTER TABLE kyc_verification
  ADD KEY idx_kyc_verification_biometric_session (biometric_session_id);
