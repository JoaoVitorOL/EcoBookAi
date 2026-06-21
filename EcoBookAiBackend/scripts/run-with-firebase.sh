#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 3 ]]; then
  echo "Uso: ./scripts/run-with-firebase.sh /caminho/firebase-adminsdk.json [porta] [java_home]" >&2
  exit 1
fi

SERVICE_ACCOUNT_PATH="$1"
PORT="${2:-8080}"
JAVA_HOME_OVERRIDE="${3:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ ! -f "$SERVICE_ACCOUNT_PATH" ]]; then
  echo "Arquivo de credencial nao encontrado: $SERVICE_ACCOUNT_PATH" >&2
  exit 1
fi

if [[ -n "$JAVA_HOME_OVERRIDE" ]]; then
  export JAVA_HOME="$JAVA_HOME_OVERRIDE"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export FIREBASE_SERVICE_ACCOUNT_PATH="$SERVICE_ACCOUNT_PATH"
export GOOGLE_APPLICATION_CREDENTIALS="$SERVICE_ACCOUNT_PATH"

echo "JAVA_HOME=${JAVA_HOME:-}"
echo "FIREBASE_SERVICE_ACCOUNT_PATH=$FIREBASE_SERVICE_ACCOUNT_PATH"
echo "GOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_APPLICATION_CREDENTIALS"
echo "Iniciando backend em http://127.0.0.1:$PORT/api"

cd "$BACKEND_DIR"
exec mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.port=$PORT"
