#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-8080}"
JAVA_HOME_OVERRIDE="${2:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ -n "$JAVA_HOME_OVERRIDE" ]]; then
  export JAVA_HOME="$JAVA_HOME_OVERRIDE"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "JAVA_HOME=${JAVA_HOME:-}"
echo "Iniciando backend local em http://127.0.0.1:$PORT/api"

if [[ -z "${GEMINI_API_KEY:-}" ]]; then
  echo "GEMINI_API_KEY ausente: o backend local mantera o preview mock do Gemini."
else
  echo "GEMINI_API_KEY detectada: o backend local usara o Gemini real. Para forcar mock, defina GEMINI_MOCK_FORCE=true."
fi

cd "$BACKEND_DIR"
exec mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.port=$PORT"
