#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Uso: ./scripts/run-backend-with-firebase.sh /caminho/firebase-adminsdk.json [porta]" >&2
  exit 1
fi

SERVICE_ACCOUNT_PATH="$1"
PORT="${2:-8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$REPO_ROOT/EcoBookAiBackend"

if [[ ! -f "$SERVICE_ACCOUNT_PATH" ]]; then
  echo "Arquivo de credencial nao encontrado: $SERVICE_ACCOUNT_PATH" >&2
  exit 1
fi

export FIREBASE_SERVICE_ACCOUNT_PATH="$SERVICE_ACCOUNT_PATH"
export GOOGLE_APPLICATION_CREDENTIALS="$SERVICE_ACCOUNT_PATH"

echo "FIREBASE_SERVICE_ACCOUNT_PATH=$FIREBASE_SERVICE_ACCOUNT_PATH"
echo "GOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_APPLICATION_CREDENTIALS"
echo "Iniciando backend em http://localhost:$PORT/api"

cd "$BACKEND_DIR"
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=$PORT"
