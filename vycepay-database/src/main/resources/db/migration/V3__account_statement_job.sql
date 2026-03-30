-- Periodic account statement jobs (Choice apply/query + callback 0009)
CREATE TABLE account_statement_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  choice_request_id VARCHAR(128) NOT NULL,
  account_id VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  download_url VARCHAR(1024),
  file_name VARCHAR(256),
  error_msg TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_choice_request (choice_request_id),
  KEY idx_customer (customer_id),
  CONSTRAINT fk_statement_job_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);
