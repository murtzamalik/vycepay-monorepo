-- Extended KYC profile fields persisted locally at onboarding submit
ALTER TABLE kyc_verification
  ADD COLUMN middle_name VARCHAR(100) NULL AFTER id_number,
  ADD COLUMN birthday DATE NULL AFTER middle_name,
  ADD COLUMN gender TINYINT NULL COMMENT '0=Female, 1=Male' AFTER birthday,
  ADD COLUMN address VARCHAR(512) NULL AFTER gender,
  ADD COLUMN kra_pin VARCHAR(32) NULL AFTER address;
