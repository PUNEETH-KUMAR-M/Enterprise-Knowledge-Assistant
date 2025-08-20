#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/deploy_compose.sh [env_file]
# Example: ./scripts/deploy_compose.sh .env

ENV_FILE="${1:-env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Env file '$ENV_FILE' not found. Create one from env.example."
  exit 1
fi

# Pull latest code (if repository is present)
if [ -d .git ]; then
  git pull --rebase || true
fi

# Build and start containers
export COMPOSE_PROJECT_NAME=aibot
DOCKER_BUILDKIT=1 docker compose --env-file "$ENV_FILE" pull || true
DOCKER_BUILDKIT=1 docker compose --env-file "$ENV_FILE" up -d --build

echo "Deployment complete. App should be available on port 80."
