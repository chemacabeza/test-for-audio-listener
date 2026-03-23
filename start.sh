#!/usr/bin/env bash
# =============================================================================
# Audio Listener — Start Script
# Builds and starts all containers, then opens the browser when ready.
# Works on both macOS and Linux.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

APP_URL="http://localhost:3000"
BACKEND_HEALTH_URL="http://localhost:8080/api/transcriptions"
MAX_WAIT=120  # seconds

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
log()   { echo -e "${CYAN}[audio-listener]${NC} $1"; }
ok()    { echo -e "${GREEN}[audio-listener]${NC} $1"; }
warn()  { echo -e "${YELLOW}[audio-listener]${NC} $1"; }
fail()  { echo -e "${RED}[audio-listener]${NC} $1"; exit 1; }

open_browser() {
    local url="$1"
    if command -v xdg-open &>/dev/null; then
        xdg-open "$url" &>/dev/null &    # Linux
    elif command -v open &>/dev/null; then
        open "$url"                       # macOS
    else
        warn "Could not detect a browser opener. Please open manually: $url"
    fi
}

wait_for_service() {
    local url="$1"
    local label="$2"
    local elapsed=0

    log "Waiting for $label to be ready..."
    while [ $elapsed -lt $MAX_WAIT ]; do
        if curl -s -o /dev/null -w '' --max-time 2 "$url" 2>/dev/null; then
            ok "$label is ready! (${elapsed}s)"
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        printf "."
    done

    echo ""
    fail "$label did not become ready within ${MAX_WAIT}s. Check logs: docker compose logs"
}

# ---------------------------------------------------------------------------
# Pre-flight checks
# ---------------------------------------------------------------------------
if ! command -v docker &>/dev/null; then
    fail "Docker is not installed. Please install Docker first."
fi

if ! docker info &>/dev/null; then
    fail "Docker daemon is not running. Please start Docker."
fi

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
NOCACHE=""

for arg in "$@"; do
    case $arg in
        --no-cache)
            NOCACHE="--no-cache"
            ;;
    esac
done

# Check for .env file
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        warn ".env file not found. Copying from .env.example..."
        cp .env.example .env
        warn "⚠️  Please edit .env and set your OPENAI_API_KEY before using the app."
    else
        fail ".env file not found and no .env.example available."
    fi
fi

# Validate OPENAI_API_KEY is set
source .env 2>/dev/null || true
if [ -z "${OPENAI_API_KEY:-}" ] || [ "$OPENAI_API_KEY" = "sk-your-openai-api-key-here" ]; then
    warn "⚠️  OPENAI_API_KEY is not set or still has the placeholder value."
    warn "   Edit .env and set a valid key for transcriptions to work."
fi

# ---------------------------------------------------------------------------
# Start
# ---------------------------------------------------------------------------
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║    🎧  Audio Listener — Starting...     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""

log "Building and starting containers..."
docker compose up -d --build $NOCACHE

echo ""
wait_for_service "$BACKEND_HEALTH_URL" "Backend (Spring Boot)"
wait_for_service "$APP_URL" "Frontend (Next.js)"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║    ✅  Audio Listener is running!        ║${NC}"
echo -e "${GREEN}║                                          ║${NC}"
echo -e "${GREEN}║    Frontend:  http://localhost:3000       ║${NC}"
echo -e "${GREEN}║    Backend:   http://localhost:8080       ║${NC}"
echo -e "${GREEN}║    Database:  localhost:5432              ║${NC}"
echo -e "${GREEN}║                                          ║${NC}"
echo -e "${GREEN}║    Stop with: ./stop.sh                  ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════╝${NC}"
echo ""

log "Opening browser..."
open_browser "$APP_URL"
