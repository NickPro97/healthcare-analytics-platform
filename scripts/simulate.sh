#!/usr/bin/env bash
# Streams vital signs into the running platform so NEWS2 alerts appear live in the browser.
#   scripts/simulate.sh                 # replay 5 MIMIC demo patients
#   scripts/simulate.sh deteriorating   # one synthetic patient who becomes septic
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
SCENARIO="${1:-mimic}"
docker compose run --rm -e HAP_USERNAME=nikhil -e HAP_PASSWORD="$APP_ADMIN_PASSWORD" ml-service \
  python -m pipeline.simulate_stream --api http://patient-service:8080 --scenario "$SCENARIO" --data /data --interval 2 --utc-offset "$(date +%z)"
