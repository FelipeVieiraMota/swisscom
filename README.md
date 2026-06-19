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

The services are then available at:

- Gateway: http://localhost:8080
- Eureka: http://localhost:8761
- Spring Boot Admin: http://localhost:10000

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
