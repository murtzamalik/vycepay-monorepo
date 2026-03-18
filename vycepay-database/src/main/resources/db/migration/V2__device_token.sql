-- device_token: FCM/push notification device registrations per customer
CREATE TABLE device_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  fcm_token VARCHAR(512) NOT NULL,
  platform VARCHAR(16) NOT NULL COMMENT 'ANDROID or IOS',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES customer(id),
  UNIQUE KEY uk_customer_token (customer_id, fcm_token),
  KEY idx_customer (customer_id)
);
