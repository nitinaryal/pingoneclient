# Local dev with mock PingOne (default — no real PingOne credentials required).
mvn spring-boot:run -pl pingone-oidc-test-client -am "-Dmock=true" @args
