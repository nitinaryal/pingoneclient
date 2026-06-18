# Real PingOne — set environment variables first, e.g.:
#   $env:PINGONE_ISSUER_URI = "https://auth.pingone.com/<your-env-id>/as"
#   $env:PINGONE_CLIENT_ID = "your-client-id"
#   $env:PINGONE_CLIENT_SECRET = "your-client-secret"
mvn spring-boot:run -pl pingone-oidc-test-client -am @args
