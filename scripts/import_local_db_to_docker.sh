#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Expected root .env at ${ENV_FILE}"
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

compose() {
  docker compose -f "${PROJECT_ROOT}/docker-compose.yml" "$@"
}

echo "Stopping app containers before database sync..."
compose stop frontend backend pgadmin >/dev/null 2>&1 || true

echo "Starting Docker Postgres..."
compose up -d db

echo "Waiting for Docker Postgres to become ready..."
until compose exec -T db pg_isready -U "${DB_USERNAME}" -d "${DB_NAME}" >/dev/null 2>&1; do
  sleep 2
done

echo "Importing local Postgres data into the Docker database..."
PGPASSWORD="${DB_PASSWORD}" pg_dump \
  -h localhost \
  -U "${DB_USERNAME}" \
  -d "${DB_NAME}" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  | compose exec -T db psql -U "${DB_USERNAME}" -d "${DB_NAME}"

echo "Starting the full Docker stack..."
compose up -d backend frontend pgadmin

echo "Docker database has been refreshed from your local Postgres instance."
