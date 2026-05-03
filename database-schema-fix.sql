-- Ranaka Procurement System - Existing Database Compatibility Fix
--
-- Why this exists:
-- Hibernate `ddl-auto=update` can add new columns and tables, but it does not
-- reliably remove old columns from an existing schema.
--
-- In this project, line-item data now belongs in `request_line_items`.
-- If an older local database still has a legacy `quantity` column on
-- `procurement_requests`, inserts into the request table can fail with:
--
--   Field 'quantity' doesn't have a default value
--
-- Run this script once against your current database before restarting the app.

USE zero_trust;

SET @current_db = DATABASE();

-- Remove the obsolete request-level quantity column when it exists.
SET @drop_legacy_quantity = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @current_db
          AND table_name = 'procurement_requests'
          AND column_name = 'quantity'
    ),
    'ALTER TABLE procurement_requests DROP COLUMN quantity',
    'SELECT "procurement_requests.quantity already removed"'
);

PREPARE stmt FROM @drop_legacy_quantity;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify the final request-table shape after cleanup.
SHOW COLUMNS FROM procurement_requests;
