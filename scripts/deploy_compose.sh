#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/deploy_compose.sh [env_file]
# Example: ./scripts/deploy_compose.sh .env
# Optional env:
#   DEPLOY_RESET=1    # hard reset to remote tracking branch
#   DEPLOY_BRANCH=main# override target branch for reset

ENV_FILE="${1:-env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Env file '$ENV_FILE' not found. Create one from env.example."
  exit 1
fi

# Pull latest code (if repository is present)
if [ -d .git ]; then
  CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)"
  TARGET_BRANCH="${DEPLOY_BRANCH:-$CURRENT_BRANCH}"
  if [ "${DEPLOY_RESET:-0}" = "1" ]; then
    echo "[deploy] Hard resetting to origin/$TARGET_BRANCH"
    git fetch --all --prune || true
    git reset --hard "origin/$TARGET_BRANCH"
  else
    # Stash any local changes to avoid 'cannot pull with rebase'
    STASHED=0
    if ! git diff --quiet || ! git diff --cached --quiet; then
      echo "[deploy] Stashing local changes"
      git stash push -u -m "deploy-stash-$(date +%s)" || true
      STASHED=1
    fi
    echo "[deploy] Pulling latest changes with rebase"
    git pull --rebase || true
    if [ "$STASHED" = "1" ]; then
      echo "[deploy] Attempting to restore stashed changes"
      git stash pop || true
    fi
  fi

  # Detect merge conflict markers in source before building
  if grep -R "^<<<<<<< " -n src || grep -R ">>>>>>> " -n src ; then
    echo "[deploy] Merge conflict markers detected in source. Please resolve or run with DEPLOY_RESET=1."
    exit 1
  fi
fi

# Build and start containers
export COMPOSE_PROJECT_NAME=aibot
DOCKER_BUILDKIT=1 docker compose --env-file "$ENV_FILE" pull || true
DOCKER_BUILDKIT=1 docker compose --env-file "$ENV_FILE" up -d --build

echo "Deployment complete. App should be available on port 80."
