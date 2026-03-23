#!/usr/bin/env bash
# =============================================================================
# Audio Listener — Stop Script
# Stops all containers and optionally removes volumes.
# Works on both macOS and Linux.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[audio-listener]${NC} $1"; }
ok()   { echo -e "${GREEN}[audio-listener]${NC} $1"; }
warn() { echo -e "${YELLOW}[audio-listener]${NC} $1"; }

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
REMOVE_VOLUMES=false

for arg in "$@"; do
    case $arg in
        --clean|-c)
            REMOVE_VOLUMES=true
            ;;
        --help|-h)
            echo "Usage: ./stop.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --clean, -c    Remove volumes (database data, uploads)"
            echo "  --help,  -h    Show this help"
            exit 0
            ;;
    esac
done

# ---------------------------------------------------------------------------
# Stop
# ---------------------------------------------------------------------------
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║    🎧  Audio Listener — Stopping...     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""

if [ "$REMOVE_VOLUMES" = true ]; then
    warn "Stopping containers and removing volumes..."
    docker compose down -v
    ok "Containers stopped and volumes removed."
else
    log "Stopping containers (data preserved)..."
    docker compose down
    ok "Containers stopped. Data volumes preserved."
    echo ""
    log "Tip: Use ./stop.sh --clean to also remove database data and uploads."
fi

echo ""
ok "Audio Listener has been stopped. 👋"
