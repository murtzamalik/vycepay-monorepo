-- Paybill Acc / account number on saved beneficiaries (same semantics as send payeeReferenceNumber).
-- Non-Paybill rows keep empty string. Uniqueness includes reference so same Paybill + different Acc
-- can be saved as separate payees.

ALTER TABLE beneficiary
  ADD COLUMN payee_reference_number VARCHAR(64) NOT NULL DEFAULT '' AFTER payee_account_name;

ALTER TABLE beneficiary
  DROP INDEX uk_beneficiary_identity;

ALTER TABLE beneficiary
  ADD UNIQUE KEY uk_beneficiary_identity (
    customer_id,
    account_type,
    payee_bank_code,
    payee_account_id,
    payee_reference_number
  );
