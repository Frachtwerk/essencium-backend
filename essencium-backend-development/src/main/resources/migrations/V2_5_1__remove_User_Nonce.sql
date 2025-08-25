-- Migration Script to remove nonce from FW_USER table
ALTER TABLE "FW_USER"
DROP COLUMN IF EXISTS "nonce";