# PingOne OIDC Test Client (Template)

Reusable Spring Boot template for validating PingOne integrations. **OIDC Web App** is fully implemented; other PingOne application types are supported via configuration hooks for future extension.

## Quick start (configuration only)

1. Copy this project (or use as a Git template).
2. Set PingOne values in `application.yml` or environment variables.
3. Register matching redirect and post-logout URIs in PingOne Admin.
4. Run: `mvn spring-boot:run`

### Local mock mode (no PingOne tenant required)

Run with a built-in mock OIDC provider instead of real PingOne:

```bash
mvn spring-boot:run -Dmock=true
```

| Setting | Mock value |
|---------|------------|
| `mock` | `true` (default: `false`) |
| Issuer | `http://localhost:8080/mock/pingone/as` |
| Client ID | `mock-client-id` |
| Client secret | `mock-client-secret` |

The app exposes mock PingOne endpoints under `/mock/pingone/as` (discovery, authorize, token, userinfo, JWKS, signoff). Click **Login with PingOne** to use the mock sign-in page and exercise the full login, dashboard, OIDC validation, and logout flows offline.

No Java code changes are required for a standard OIDC Web App integration.

---

## Architecture

```
com.pingone.oidc
├── config/
│   ├── PingOneClientAutoConfiguration.java   # Startup validation
│   ├── properties/
│   │   ├── PingOneClientProperties.java      # All client-specific settings
│   │   └── PingOneApplicationType.java       # oidc-web-app | oidc-spa | ...
│   └── security/
│       ├── PingOneSecurityConfigurer.java    # Extension point per app type
│       ├── OidcWebAppSecurityConfigurer.java # OIDC Web App (implemented)
│       ├── PingOneSecurityConfigurerFactory.java
│       └── PingOneSecurityConfig.java
├── controller/                               # Generic OIDC validation UI
└── service/PingOneMetadataService.java
```

### Extension pattern (future application types)

| PingOne type | Config value | Status | Implement by adding |
|--------------|--------------|--------|---------------------|
| OIDC Web App | `oidc-web-app` | **Done** | — |
| Single-Page App | `oidc-spa` | Planned | `OidcSpaSecurityConfigurer` (PKCE, public client) |
| Native | `oidc-native` | Planned | `OidcNativeSecurityConfigurer` (PKCE, loopback) |
| Worker | `worker` | Planned | `WorkerSecurityConfigurer` (client credentials) |
| Device | `device` | Planned | `DeviceSecurityConfigurer` (device code flow) |
| SAML | `saml` | Planned | `SamlSecurityConfigurer` (Spring Security SAML) |

Add a `@Component` implementing `PingOneSecurityConfigurer` and register the type in `PingOneApplicationType`.

---

## Configuration reference

### PingOne template settings (`pingone.*`)

| Property | Default | Description |
|----------|---------|-------------|
| `pingone.application-type` | `oidc-web-app` | Selects security configurer implementation |
| `pingone.registration-id` | `pingone` | Must match Spring OAuth2 registration key |
| `pingone.provider-id` | `pingone` | Must match Spring OAuth2 provider key |
| `pingone.ui.home-path` | `/` | Public landing page |
| `pingone.ui.post-login-path` | `/dashboard` | Redirect after successful login |
| `pingone.security.public-paths` | `/`, `/error`, `/css/**`, ... | Unauthenticated paths |
| `pingone.security.post-logout-redirect-uri` | `http://localhost:8080/` | PingOne post-logout redirect |
| `pingone.metadata.jwks-uri-override` | _(empty)_ | Optional JWKS URL override |
| `pingone.metadata.discovery-document-path` | `/.well-known/openid-configuration` | OIDC discovery path |
| `pingone.oidc.id-token-claim-keys` | sub, email, name, iss, aud, exp | Claims shown on `/me` |

### Spring OAuth2 client (`spring.security.oauth2.client.*`)

These remain standard Spring Boot properties (required for token exchange):

| Property | Env variable | Description |
|----------|--------------|-------------|
| `registration.<id>.client-id` | `PINGONE_CLIENT_ID` | PingOne application client ID |
| `registration.<id>.client-secret` | `PINGONE_CLIENT_SECRET` | Client secret (OIDC Web App) |
| `registration.<id>.redirect-uri` | `PINGONE_REDIRECT_URI` | Must match PingOne redirect URI |
| `provider.<id>.issuer-uri` | `PINGONE_ISSUER_URI` | `https://auth.pingone.com/{env-id}/as` |

**Important:** `pingone.registration-id` must equal the registration key under `spring.security.oauth2.client.registration`.

---

## PingOne Admin setup (OIDC Web App)

| Setting | Value |
|---------|--------|
| Application type | OIDC Web App |
| Grant type | Authorization Code |
| Redirect URI | `http://localhost:8080/login/oauth2/code/pingone` |
| Post-logout redirect URI | `http://localhost:8080/` |
| Scopes | `openid`, `profile`, `email` |

If you change `pingone.registration-id` to `my-client`, update:

- Spring registration key: `spring.security.oauth2.client.registration.my-client`
- Redirect URI: `http://localhost:8080/login/oauth2/code/my-client`
- PingOne redirect URI to match

---

## Example: client-specific `application.yml`

```yaml
pingone:
  application-type: oidc-web-app
  registration-id: acme-pingone
  security:
    post-logout-redirect-uri: https://app.acme.com/

spring:
  security:
    oauth2:
      client:
        registration:
          acme-pingone:
            client-id: ${ACME_PINGONE_CLIENT_ID}
            client-secret: ${ACME_PINGONE_CLIENT_SECRET}
            redirect-uri: https://app.acme.com/login/oauth2/code/acme-pingone
            provider: acme-pingone
        provider:
          acme-pingone:
            issuer-uri: https://auth.pingone.com/{env-id}/as
```

---

## Client Integration Tool

Open **http://localhost:8080/tool** (public, no login required) for the pre-production integration wizard:

1. **Select application type** — OIDC Web App, SPA, Native, Worker, Device, SAML
2. **Configuration wizard** — mandatory/optional fields with tooltips
3. **Generate adoption artifacts** — copy `application.yml`, env vars, Java notes, PingOne admin checklist
4. **Runtime diagnostics** — connectivity checks for metadata + JWKS
5. **Test suite** — step-by-step login, logout, claims, token, JWKS, metadata tests with investigation details

OIDC Web App tests are runnable in this template. Other types show configuration guidance and planned test flows.

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `/tool` | Public client integration wizard |
| `/tool/test/{id}` | Test investigation guide with steps |
| `/` | Public home |
| `/oauth2/authorization/{registration-id}` | Start login |
| `/dashboard` | Post-login hub |
| `/me` | ID token claims |
| `/token` | Masked access token |
| `/jwks` | JWKS JSON |
| `/metadata` | OIDC discovery document |
| `POST /logout` | Local + PingOne end-session logout |

---

## Tests

```bash
mvn test
```

---

## Tech stack

- Java 21, Spring Boot 4.0.6
- Spring Security OAuth2 Client
- Thymeleaf, WebClient
