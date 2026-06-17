# PingOne OIDC Test Client

Minimal Spring Boot 4 application for validating PingOne OpenID Connect authentication flows.

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Spring Security OAuth2 Client
- Thymeleaf
- WebClient (metadata and JWKS fetch)

## Prerequisites

- JDK 21+
- Maven 3.9+
- A PingOne environment with an OIDC web application configured

## PingOne Application Setup

In the PingOne admin console, create (or use) a **Web App** OIDC client with:

| Setting | Value |
|---------|-------|
| Grant type | Authorization Code |
| Response type | Code |
| Redirect URI | `http://localhost:8080/login/oauth2/code/pingone` |
| Post-logout redirect URI | `http://localhost:8080/` |
| Scopes | `openid`, `profile`, `email` |

Copy the **Client ID**, **Client Secret**, and **Environment ID** from PingOne.

## Configuration

Edit `src/main/resources/application.yml` or set environment variables:

```bash
export PINGONE_CLIENT_ID="your-client-id"
export PINGONE_CLIENT_SECRET="your-client-secret"
export PINGONE_ISSUER_URI="https://auth.pingone.com/{environment-id}/as"
export PINGONE_REDIRECT_URI="http://localhost:8080/login/oauth2/code/pingone"
export PINGONE_POST_LOGOUT_REDIRECT_URI="http://localhost:8080/"
# Optional override:
# export PINGONE_JWKS_URI="https://auth.pingone.com/{environment-id}/as/jwks"
```

### Option A: Issuer URI (recommended)

Set `PINGONE_ISSUER_URI`. Spring Security auto-discovers authorization, token, userinfo, and JWKS endpoints.

### Option B: Explicit endpoints

Comment out `issuer-uri` in `application.yml` and uncomment:

- `authorization-uri`
- `token-uri`
- `user-info-uri`
- `jwk-set-uri`

## Run Tests

```bash
mvn test
```

Automated tests cover:

- Application context startup
- Public vs protected endpoint security rules
- OIDC logout redirect to PingOne end-session endpoint
- ID token claims rendering (`/me`)
- Masked access token rendering (`/token`)
- JWKS and OIDC metadata fetch behavior

## Run Application

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

## Endpoints

| Endpoint | Access | Description |
|----------|--------|-------------|
| `/` | Public | Landing page with Login |
| `/dashboard` | Authenticated | Action hub |
| `/oauth2/authorization/pingone` | Public | Starts OIDC login |
| `/logout` | Authenticated | Local + PingOne end-session logout |
| `/me` | Authenticated | ID token claims (`sub`, `email`, `name`, `iss`, `aud`, `exp`) |
| `/token` | Authenticated | Masked access token details |
| `/jwks` | Authenticated | Fetches and displays PingOne JWKS JSON |
| `/metadata` | Authenticated | Fetches OIDC discovery document |

## Validation Flow

1. Start the app with valid PingOne credentials.
2. Open `/` and click **Login with PingOne**.
3. Authenticate in PingOne and return to `/dashboard`.
4. Open `/me` to verify ID token claims.
5. Open `/token` to verify access token issuance.
6. Open `/jwks` and `/metadata` to verify provider discovery and keys.
7. Click **Logout** to verify local and PingOne end-session logout.

## Project Structure

```
src/main/java/com/pingone/oidc/
├── PingOneOidcTestClientApplication.java
├── config/
│   ├── SecurityConfig.java
│   └── WebClientConfig.java
├── controller/
│   ├── HomeController.java
│   └── OAuthController.java
└── service/
    └── PingOneMetadataService.java
```

## Notes

- No database, SCIM, or admin APIs — authentication validation only.
- Access tokens are partially masked in the UI.
- Logout uses `OidcClientInitiatedLogoutSuccessHandler` to redirect to PingOne `end_session_endpoint`.
