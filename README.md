# Swisscom URL Shortener Assignment

## Deployment modes

The repository has two explicit modes:

```text
deploy/
├── docker-compose.yml           # shared, environment-neutral services
├── docker-compose.dev.yml       # DEV, including local/IDE development
├── docker-compose.dev-tls.yml   # optional HTTPS for DEV
├── docker-compose.prod.yml      # production-like security boundary
├── env/
│   ├── dev/
│   └── prod/
└── caddy/
    ├── dev/
    └── prod/
```

The base Compose file is not intended to run by itself. Always combine it with
either the DEV or PROD-like override.

## DEV

DEV contains the former default and local configurations. It exposes the
gateway, the development infrastructure routes and PostgreSQL on loopback for
IDE runs.

### Full stack in Docker

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.dev.yml \
  up --build
```

Available endpoints:

- Gateway: http://localhost
- Auth API: http://localhost/api/v1/auth
- Links API: http://localhost/api/v1/links
- Eureka: http://eureka.localhost
- Spring Boot Admin: http://admin.localhost
- Auth PostgreSQL: `127.0.0.1:5432`
- Links PostgreSQL: `127.0.0.1:5433`

Development administrator:

```text
email:    admin@swisscom.local
password: ChangeMe-Admin-2026!
```

Override these values in the ignored root `.env` file using
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`.

### Run a service from the IDE

Start only the databases using the same DEV configuration:

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.dev.yml \
  up -d auth-postgres links-postgres
```

Run the Authenticator with the `local` Spring profile and a local JWT secret:

```bash
export SPRING_PROFILES_ACTIVE=local
export APP_SECURITY_JWT_SECRET="$(openssl rand -base64 64)"
mvn -pl services/authenticator spring-boot:run
```

Direct Swagger UI: http://localhost:4000/api/v1/auth/swagger-ui

### Optional HTTPS in DEV

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.dev.yml \
  -f deploy/docker-compose.dev-tls.yml \
  up --build
```

HTTPS endpoints:

- Gateway: https://gateway.localhost
- Eureka: https://eureka.localhost
- Spring Boot Admin: https://admin.localhost

Caddy uses a local CA. Extract its public root certificate with:

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.dev.yml \
  -f deploy/docker-compose.dev-tls.yml \
  cp caddy:/data/caddy/pki/authorities/local/root.crt /tmp/swisscom-caddy-root.crt
```

On macOS, trust it in the current user's login keychain:

```bash
security add-trusted-cert \
  -r trustRoot \
  -k "$HOME/Library/Keychains/login.keychain-db" \
  /tmp/swisscom-caddy-root.crt
```

## PROD-like

PROD-like publishes only Caddy on ports 80 and 443. Gateway, services,
databases, Eureka, Spring Boot Admin and Actuator remain internal. Development
routes, Swagger UI and the bootstrap administrator are disabled.

Create local secrets:

```bash
cp .env.example .env
```

Replace `JWT_SECRET` with Base64 output from `openssl rand -base64 64` and set a
strong `POSTGRES_PASSWORD`. Then run:

```bash
docker compose \
  --env-file .env \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.prod.yml \
  up --build
```

The only public endpoint is https://localhost. The local Caddy CA simulates TLS
termination; a real deployment would use a publicly trusted certificate and a
platform secret store.

## Business routes

The same business paths are used in DEV and PROD-like:

- Authentication: `/api/v1/auth/**`
- Link management: `/api/v1/links/**`
- Public redirect: `/r/{code}`

Infrastructure routes exist only with the gateway's `dev` profile.

## Local overrides and secrets

The root `.env` file is ignored by Git. `.env.example` documents supported
variables without providing production credentials. Files under
`deploy/env/dev` and `deploy/env/prod` contain only environment-specific,
non-secret service configuration.

Never commit passwords, JWT signing secrets, private keys or production
certificates. Running `docker compose down -v` deletes database and Caddy
volumes; omit `-v` when data should be preserved.
