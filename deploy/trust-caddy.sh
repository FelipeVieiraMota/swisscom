#!/usr/bin/env bash

set -euo pipefail

ENVIRONMENT="${1:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

case "${ENVIRONMENT}" in
  localhost|dev)
    ;;
  *)
    echo "Usage: ./deploy/trust-caddy.sh {localhost|dev}" >&2
    exit 1
    ;;
esac

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Automatic trust is currently supported only on macOS." >&2
  exit 1
fi

COMPOSE_FILE="deploy/docker-compose.${ENVIRONMENT}.yml"
CERTIFICATE_FILE="/tmp/swisscom-caddy-root.crt"

cd "${ROOT_DIR}"

docker compose -f "${COMPOSE_FILE}" cp \
  caddy:/data/caddy/pki/authorities/local/root.crt \
  "${CERTIFICATE_FILE}"

security add-trusted-cert \
  -r trustRoot \
  -k "${HOME}/Library/Keychains/login.keychain-db" \
  "${CERTIFICATE_FILE}"

echo "Caddy local CA trusted for both localhost and DEV."
