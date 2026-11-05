-- Local only: wipe wallet data and restart IDs at 1.
--
-- DBeaver: select ALL lines → right-click → Execute SQL Script
--          (or Alt+X). Do NOT use Ctrl+Enter (that runs only 1 statement).
--
-- Also enable multi-queries if needed:
--   connection → Edit → Driver properties → allowMultiQueries = true

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM wallet_history;
DELETE FROM wallet_transfer;
DELETE FROM wallet;

ALTER TABLE wallet_history AUTO_INCREMENT = 1;
ALTER TABLE wallet_transfer AUTO_INCREMENT = 1;
ALTER TABLE wallet AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
