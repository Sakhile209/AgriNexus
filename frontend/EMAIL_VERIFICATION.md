# AgriNexus email verification

New registration now creates an unverified account and returns a confirmation **without an access token**. The user must follow the emailed link before login or any protected API request succeeds. The existing BCrypt password encoder, JWT format, PostgreSQL database, dashboard, and farm features remain in use.

## Delivery configuration (required for real email)

There was no existing SMTP/email integration in the project. The implementation uses **Brevo transactional email over HTTPS**, behind a `VerificationMail` interface. This avoids adding an SMTP library or a new framework. The provider supports ordinary internet email recipients, including South African addresses; inbox placement still depends on domain authentication, sender reputation, recipient filtering, and your account's transactional-email activation/quotas.

References:

- Sending and response contract: https://developers.brevo.com/reference/send-transac-email
- Transactional setup: https://developers.brevo.com/docs/send-a-transactional-email
- Sender/domain verification: https://developers.brevo.com/docs/getting-started-with-senders-and-domains
- Domain authentication: https://help.brevo.com/hc/en-us/articles/12163873383186-Authenticate-your-domain-with-Brevo-Brevo-code-DKIM-DMARC

Configure a domain and sender that you control in Brevo, add its required DNS authentication records, and activate transactional sending. Then set these variables in the **backend's environment**:

| Variable | Required value |
| --- | --- |
| `BREVO_API_KEY` | Brevo transactional API key. Never put it in frontend code, Git, logs, or a public environment file. |
| `EMAIL_FROM` | Your Brevo-verified sender email address. Display name is AgriNexus. |
| `VERIFICATION_BASE_URL` | Trusted frontend base URL, e.g. `https://your-agrinexus-domain.example`. HTTPS is mandatory except for localhost development. Must not contain a query, fragment, or userinfo. |

For local development, `VERIFICATION_BASE_URL=http://localhost:5174` is accepted. That link only works on the same computer; for opening an email on a phone, use an HTTPS development/deployed frontend address accessible to that phone. The base URL is read from configuration, never an incoming Host header.

The application does **not** fall back to console emails, fake delivery, an automatic verification bypass, or a test recipient. Without valid configuration, registration/resend returns a clear `503 EMAIL_UNAVAILABLE` and does not create a new account or report an email sent. Existing verified accounts can still log in. A provider key and verified sender were not available during implementation, so actual inbox delivery must be verified after configuring your account.

Provider HTTP `201` plus a message ID means the provider accepted the message, not that the recipient's inbox received it. Use Brevo delivery/bounce monitoring for actual delivery assurance. No automatic real-message test is sent to another person.

## Registration, verification, and resend

- Existing server-side field validation remains enforced. Email is normalized to trimmed lowercase.
- Passwords use the existing BCrypt encoder (cost 12); registration rejects values above its 72-byte UTF-8 input limit. Registration performs the encoding for duplicate addresses as well to reduce timing differences.
- The unique email constraint and `INSERT ... ON CONFLICT` protect against concurrent duplicate registrations. Duplicate registration does not change the existing password, profile, or verification status.
- Valid registration requests return `202` with `verificationRequired=true`, the requested check-your-email message, and a 60-second resend interval. The response never contains a JWT.
- Verification emails have AgriNexus branding, a verification button, a plain link/text alternative, 15-minute expiry information, and instructions for someone who did not register. Passwords and private farmer records are never included.
- The link uses `/#verify-email=<token>`. The fragment is not sent to web-server access logs or in HTTP Referer headers. The frontend immediately removes it from the address bar and sends it in the POST body. It is never put in local/session storage.
- Tokens use 32 cryptographically random bytes, Base64URL-encoded to 43 characters. Only the SHA-256 hash is stored. Each token expires 15 minutes after generation.
- Verification locks the account row and conditionally consumes the token in one transaction. Expired, used, superseded, or unknown tokens cannot verify an account. Concurrent confirmation has exactly one successful response.
- Successful verification sets `email_verified` and `email_verified_at`, records `used_at`, invalidates remaining tokens, and removes pending delivery work. It does not issue a login token. The frontend displays success, then opens login after four seconds, with an immediate Log In button too.
- Resend responses are identical for missing, already verified, disabled, and unverified accounts. Only an active, unverified account queues email. No delivery result/status endpoint exposes account existence.
- Tokens are rotated when a delivery attempt prepares the newest verification email, so old links are superseded. Users should use the newest email.

## Reliable delivery without plaintext token storage

`email_verification_delivery` is a durable PostgreSQL queue containing user IDs and scheduling metadata, **not raw tokens or email bodies**. A scheduled worker runs every five seconds. It claims due jobs with a lease and `FOR UPDATE SKIP LOCKED`, locks the user, generates a fresh token, commits only its hash, and then calls Brevo. Thus a promptly clicked email link can find its token in the database.

A successful provider acceptance removes the claimed job. A failure preserves it with a sanitized error code and bounded exponential backoff (60, 120, 240, 480, then at most 900 seconds), with at most five attempts per queued request. A crash/lease expiry permits another attempt and a fresh token; most-recent-link semantics still apply. Raw token values exist only in process memory during sending and in the intended verification email.

Logs contain only a generic delivery-failure warning, never request bodies, recipient addresses, API keys, provider response bodies, verification links, or raw tokens. Do not enable HTTP body logging for these endpoints/provider requests in production. Monitor pending/failed jobs operationally. An accepted registration queues delivery; it does not claim that an inbox receipt has already occurred.

Useful backend-only operational query:

```sql
SELECT attempts, last_error, next_attempt_at, locked_until, count(*)
FROM email_verification_delivery
GROUP BY attempts, last_error, next_attempt_at, locked_until;
```

Resending after a failed delivery creates a new generation of pending work. The 60-second cooldown and hourly caps still apply. If a provider configuration is later removed, existing queued work remains pending rather than being silently delivered through a fake channel.

## Backend enforcement and abuse controls

- Correct password + unverified account: `403 EMAIL_NOT_VERIFIED` with “Please verify your email address before logging in.” Incorrect credentials still get the existing generic authentication failure.
- `JwtService` refuses to issue tokens for an unverified account.
- The JWT decoder validates signature, issuer/expiry, current account verification, active status, and matching `userId`. An otherwise valid signed token for an unverified, disabled, deleted, or replaced account cannot access protected APIs.
- Database-backed atomic fixed-window rate limits apply across API replicas: registration 20/IP/hour, resend 20/IP/hour, email delivery requests 5/email/hour with a 60-second cooldown, login 30/IP/15 minutes and 10/email/15 minutes, confirmation 60/IP/5 minutes.
- Email/IP bucket identifiers are hashed. Rate-limited responses are generic, including for nonexistent addresses, with `429` and `Retry-After: 60`. Frontend timers are convenience only; server limits remain authoritative after a refresh.
- The implementation uses `request.getRemoteAddr()`, not arbitrary client-supplied `X-Forwarded-For`. If deploying behind a proxy, configure trusted forwarding at the deployment boundary; otherwise all users behind that proxy share an IP bucket.
- Tokens and delivery jobs are transactional where appropriate; provider networking occurs after the token transaction commits.

## Migration

`backend/src/main/resources/db/migration/V8__email_verification.sql` adds:

- `app_user.email_verified` and `email_verified_at`.
- `email_verification_token`: token/user IDs, unique token hash, expiry, used/invalidated times, creation time.
- `email_verification_delivery`: durable queue, attempts, retry/lease times, generation and claim IDs, sanitized error code.
- `auth_request_limit`: atomic rate-limit buckets.

All accounts present when the migration runs are grandfathered as verified so they retain access. Their `email_verified_at` remains NULL, because the app cannot assert a historical verification event. The default is then changed to FALSE for future accounts. Existing passwords, farmer profiles, farms, records, and account statuses are retained. No prior migration is edited, and the migration deletes no user records.

There is no automatic retention cleanup in this change; operations should retain only the required expired token/rate-limit metadata and prune expired rows through their normal database maintenance policy.

## Files and endpoints

Backend changes:

- `auth/model/User.java`: verification fields (new Java users start unverified).
- `auth/service/AuthService.java`, `JwtService.java`: pending registration and verified login/token issuance.
- `auth/web/AuthController.java`: confirmation/resend endpoints and request rate limits.
- `auth/verification/`: modular token utilities, PostgreSQL repository, verification lifecycle, durable worker, Brevo mail service, and typed errors.
- `shared/config/SecurityConfig.java`: public verification endpoints and verified-account JWT validation.
- `shared/exception/GlobalExceptionHandler.java`: safe verification/rate-limit errors.
- V8 migration and verification tests under `backend/src/test/`.

Endpoints:

| Method / path | Behaviour |
| --- | --- |
| `POST /api/v1/auth/register` | Existing registration fields; `202` pending confirmation, no automatic login. |
| `POST /api/v1/auth/login` | Existing login shape; unverified accounts cannot log in. |
| `POST /api/v1/auth/verification/confirm` | JSON `{ "token": "..." }`; `200` success, `410` expired, `400` invalid/used/superseded. |
| `POST /api/v1/auth/verification/resend` | JSON `{ "email": "..." }`; generic acknowledgement, `429` cooldown/cap, or `503` globally missing email configuration. |

Frontend changes:

- `src/email-verification.js` and `.css`, synchronized into `dist/`.
- `dist/index.html`: loads the verification module/styles; preserves the existing React bundle/design.
- `scripts/sync-assets.mjs`: packages the new sources with the existing assets.
- Existing registration forms are intercepted before their automatic-login callback. Verified login continues through the original authentication UI and session code.
- Confirmation/resend, loading, success, expiry/invalid-link, and service-unavailable screens; unverified login includes a resend option. The verification dialog makes background content inert.

## Run and test

Stop the currently running API before rebuilding its JAR. From the repository backend directory:

```bash
mvn clean test package
```

Then, from `frontend/`, with the real provider variables exported in that terminal:

```bash
npm run api
```

In another terminal:

```bash
npm run dev
```

Register an address whose inbox you control. Confirm that no authenticated session is created, a real Brevo email arrives, and the link verifies the account. Check that login fails before verification, succeeds afterwards, and reusing the same link fails. Resend is available after 60 seconds; the newest delivered link supersedes the old one.

Unit and browser checks:

```bash
npm test
# Optional isolated browser checks (Playwright in your tooling environment):
PLAYWRIGHT_MODULE=/tmp/agrinexus-browser/node_modules/playwright/index.mjs \
CHROMIUM_PATH=/absolute/path/to/chrome \
node scripts/verify-email-ui.mjs
```

`VerificationDatabaseTest` runs only when `EMAIL_TEST_DB_URL` names a disposable database containing `agrinexus_email_test_`. It includes test-only cleanup and must never target real farmer data. Set `EMAIL_TEST_DB_USER` and `EMAIL_TEST_DB_PASSWORD` when using different test credentials. Test mail is mocked only inside the test context; there is no production mock-mail mode.

Coverage includes registration/duplicate safety, provider request contract and rejection, hash-only tokens, valid/expired/invalid/reused/superseded tokens, resend/cooldown/privacy, concurrent replay and atomic limits, unverified login and signed-JWT denial, login after verification, delivery retry failure, and legacy-account migration. Actual inbox receipt remains a required deployment verification once real provider configuration is supplied.

### Implementation verification results

- **43 backend tests passed**, including eight tests against a disposable PostgreSQL database, provider HTTP contract/rejection tests, concurrent token replay, atomic rate limits, token rotation, and legacy-account migration. The disposable test databases were removed after each run.
- **12 frontend tests passed**.
- **11 isolated verification browser checks passed**, covering registration without login, resend/cooldown, unverified login, URL token removal, verification success/login redirect, invalid/expired links, mobile layout, missing provider configuration, and browser errors.
- Real inbox receipt was not tested: no Brevo key or verified sender was supplied. Configuration must be completed before new registrations can receive real messages. There is no production mock-delivery mode.
