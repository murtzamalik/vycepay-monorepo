-- Manual data fix: align kyc_verification + wallet with Choice when callback 0001 was not processed.
-- Run against the vycepay schema (review in a transaction first).
--
-- Example (single-container server; set DB password as appropriate):
--   sudo docker exec -i vycepay-server mysql -u vycepay -p'<password>' vycepay < scripts/manual-fix-onboarding-accounts.sql
--
-- ONBRD026692b602502000 → 46012000042508 / 8f27f589-4b59-48cc-8ce5-ff50cfdfc577
-- ONBRD02669394e8902000 → 46012000042516 / d08d8937-f89b-4f5a-848d-ba58eacfa140

START TRANSACTION;

UPDATE kyc_verification
SET status = '7',
    choice_user_id = '8f27f589-4b59-48cc-8ce5-ff50cfdfc577',
    choice_account_id = '46012000042508',
    choice_account_type = 'C002'
WHERE choice_onboarding_request_id = 'ONBRD026692b602502000';

UPDATE kyc_verification
SET status = '7',
    choice_user_id = 'd08d8937-f89b-4f5a-848d-ba58eacfa140',
    choice_account_id = '46012000042516',
    choice_account_type = 'C002'
WHERE choice_onboarding_request_id = 'ONBRD02669394e8902000';

INSERT INTO wallet (customer_id, choice_account_id, choice_account_type, balance_cache, currency, status)
SELECT k.customer_id, k.choice_account_id, 'C002', 0, 'KES', 'ACTIVE'
FROM kyc_verification k
WHERE k.choice_onboarding_request_id = 'ONBRD026692b602502000'
  AND NOT EXISTS (SELECT 1 FROM wallet w WHERE w.choice_account_id = k.choice_account_id);

INSERT INTO wallet (customer_id, choice_account_id, choice_account_type, balance_cache, currency, status)
SELECT k.customer_id, k.choice_account_id, 'C002', 0, 'KES', 'ACTIVE'
FROM kyc_verification k
WHERE k.choice_onboarding_request_id = 'ONBRD02669394e8902000'
  AND NOT EXISTS (SELECT 1 FROM wallet w WHERE w.choice_account_id = k.choice_account_id);

COMMIT;
