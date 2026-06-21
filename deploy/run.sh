#!/usr/bin/env bash

set -euo pipefail

ENVIRONMENT="${1:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

case "${ENVIRONMENT}" in
  localhost|dev)
    ;;
  *)
    echo "Usage: ./deploy/run.sh {localhost|dev} [service ...]" >&2
    exit 1
    ;;
esac

shift
SERVICES=("$@")

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required to generate the shared JWT secret." >&2
  exit 1
fi

SECRET_FILE="${SCRIPT_DIR}/env/${ENVIRONMENT}/jwt-secret.env"

if [[ ! -s "${SECRET_FILE}" ]]; then
  umask 077
  SECRET="$(openssl rand -base64 64 | tr -d '\n')"
  printf 'APP_SECURITY_JWT_SECRET=%s\n' "${SECRET}" > "${SECRET_FILE}"
  echo "Generated shared JWT secret: deploy/env/${ENVIRONMENT}/jwt-secret.env"
else
  echo "Reusing shared JWT secret: deploy/env/${ENVIRONMENT}/jwt-secret.env"
fi

cd "${ROOT_DIR}"

case "${ENVIRONMENT}" in
  localhost)
    docker compose -f deploy/docker-compose.localhost.yml up -d
    echo "Attach deploy/env/localhost/jwt-secret.env to both API run configurations in IntelliJ."
    ;;
  dev)
    if (( ${#SERVICES[@]} > 0 )); then
      docker compose -f deploy/docker-compose.dev.yml up -d --build "${SERVICES[@]}"
    else
      docker compose -f deploy/docker-compose.dev.yml up -d --build
    fi
    ;;
esac
