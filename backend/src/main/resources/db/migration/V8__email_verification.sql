-- Preserve existing accounts while requiring verification for all future accounts.
ALTER TABLE app_user ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_user ADD COLUMN email_verified_at TIMESTAMPTZ;
-- Legacy accounts are grandfathered; NULL verified_at means no historical verification event is asserted.
ALTER TABLE app_user ALTER COLUMN email_verified SET DEFAULT FALSE;
CREATE TABLE email_verification_token (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
 token_hash CHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 used_at TIMESTAMPTZ,
 invalidated_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX email_verification_user ON email_verification_token(user_id,created_at DESC);
CREATE TABLE email_verification_delivery (
 user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
 requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 attempts INTEGER NOT NULL DEFAULT 0,
 generation INTEGER NOT NULL DEFAULT 1,
 claim_id UUID,
 locked_until TIMESTAMPTZ,
 last_error VARCHAR(40)
);
CREATE INDEX email_delivery_due ON email_verification_delivery(next_attempt_at);
CREATE TABLE auth_request_limit (
 bucket_hash CHAR(64) PRIMARY KEY,
 window_started_at TIMESTAMPTZ NOT NULL,
 last_request_at TIMESTAMPTZ NOT NULL,
 request_count INTEGER NOT NULL
);
