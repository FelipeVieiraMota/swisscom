# Swisscom URL Shortener Assignment

## Local environment

Application configuration uses Spring placeholders in the form
`${VARIABLE:default}`. Safe development defaults stay in the committed YAML
files; machine-specific overrides and secrets belong in a local `.env` file.

### Run with the defaults

The only local prerequisite is Docker with Docker Compose. No `.env`, Maven or
local Java installation is required:

```bash
docker compose -f deploy/docker-compose.yml up --build
```

Only the public entry point is exposed to the host:

- Gateway: http://localhost

Eureka and Spring Boot Admin are attached only to the internal backend network.
To make their UIs available through development-only gateway routes, apply the
development override:

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.dev.yml \
  up --build
```

The development-only endpoints are:

- Eureka: http://eureka.localhost
- Spring Boot Admin: http://admin.localhost

These routes are created only when the gateway's `dev` Spring profile is
active. The infrastructure containers remain unexposed to the host.

### Optional local HTTPS

The TLS override adds Caddy as an edge proxy on port 443. Caddy terminates TLS
and forwards requests to the gateway over the isolated Docker network:

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.dev.yml \
  -f deploy/docker-compose.tls.yml \
  up --build
```

The HTTPS endpoints are:

- Gateway: https://gateway.localhost
- Eureka: https://eureka.localhost
- Spring Boot Admin: https://admin.localhost

Caddy generates the development certificates and private keys in a Docker
volume; they are never committed. Its local CA is not trusted by the host by
default. To obtain the public root certificate after Caddy starts:

```bash
docker compose \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.dev.yml \
  -f deploy/docker-compose.tls.yml \
  cp caddy:/data/caddy/pki/authorities/local/root.crt /tmp/swisscom-caddy-root.crt
```

Import `/tmp/swisscom-caddy-root.crt` into the operating system or browser
trust store to remove the local certificate warning. Never copy or share the
CA private key. A production deployment would replace the internal CA with
Let's Encrypt or the organization's certificate authority.

On macOS, trust the extracted public certificate in the current user's login
keychain:

```bash
security add-trusted-cert \
  -r trustRoot \
  -k "$HOME/Library/Keychains/login.keychain-db" \
  /tmp/swisscom-caddy-root.crt
```

The CA is persisted in the `caddy_data` Docker volume. Running
`docker compose down -v` deletes that CA, so a newly generated root certificate
must be trusted again.

### Override local values

Creating a `.env` file is optional. Use it when a default port conflicts with
another application or when future services require local credentials:

```bash
cp .env.example .env
docker compose --env-file .env -f deploy/docker-compose.yml up --build
```

The `.env` file is intentionally ignored by Git. When a new required variable
is introduced, add its name and a non-sensitive example value to
`.env.example`.

Service-level, non-sensitive configuration is versioned under `deploy/env/`
and loaded into each container by Docker Compose. For example,
`deploy/env/gateway.env` contains the gateway port, Eureka addresses and log
levels required by every developer.

Do not place passwords, tokens or private keys in the committed files under
`deploy/env/`. Secrets belong in the ignored root `.env` for local development
and in the deployment platform's secret store in production.

For an IDE run, either keep the YAML defaults or add the same variables to the
Spring Boot run configuration. Production secrets must be supplied by the
deployment platform and must never be committed.
