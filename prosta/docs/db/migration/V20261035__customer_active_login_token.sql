-- Apply before deploying the single-login implementation. Existing sessions must log in again.
ALTER TABLE customer ADD (active_login_token VARCHAR2(36 CHAR));
COMMENT ON COLUMN customer.active_login_token IS 'Currently valid login identifier; NULL means logged out';
