#!/usr/bin/env bash
# One-command local demo with Docker Compose:
#   1. builds and starts every service
#   2. trains the ML model on your local MIMIC-III demo folder (MIMIC_DATA_DIR in .env)
#   3. loads the 100 demo patients through the REST API
# Then run:  scripts/simulate.sh   to stream live vitals and watch alerts appear.
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || { echo "Missing .env - copy .env.example to .env and fill it in."; exit 1; }
set -a; source .env; set +a
[ -d "${MIMIC_DATA_DIR:-}" ] || { echo "MIMIC_DATA_DIR ($MIMIC_DATA_DIR) not found - set it in .env"; exit 1; }

echo "==> Building and starting containers (first build takes several minutes)"
docker compose up -d --build

echo "==> Training the risk model on the MIMIC-III demo"
docker compose run --rm ml-service python -m training.train --data /data
curl -fsS -X POST http://localhost:8000/model/reload && echo

echo "==> Waiting for patient-service"
for i in $(seq 1 90); do
  curl -fsS http://localhost:8080/actuator/health/readiness >/dev/null 2>&1 && break
  sleep 2
done

echo "==> Loading MIMIC demo patients, admissions and vitals"
docker compose run --rm -e HAP_USERNAME=nikhil -e HAP_PASSWORD="$APP_ADMIN_PASSWORD" ml-service \
  python -m pipeline.ingest_mimic --data /data --api http://patient-service:8080

echo
echo "Ready: open http://localhost:3000  (user: nikhil, password: the APP_ADMIN_PASSWORD in your .env)"
echo "Stream live vitals:  scripts/simulate.sh"
