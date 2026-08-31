-- Destination email for Choice applyBankAccountStatement (email delivery). Nullable for legacy URL jobs.
ALTER TABLE account_statement_job
  ADD COLUMN email VARCHAR(255) NULL;
