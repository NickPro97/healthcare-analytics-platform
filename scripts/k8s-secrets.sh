#!/usr/bin/env bash
# Creates/updates the Kubernetes Secret and the ML model ConfigMap in the current kube context.
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a

kubectl create namespace hap --dry-run=client -o yaml | kubectl apply -f -
kubectl -n hap create secret generic hap-secrets \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
  --from-literal=APP_ADMIN_PASSWORD="$APP_ADMIN_PASSWORD" \
  --from-literal=APP_CLINICIAN_PASSWORD="$APP_CLINICIAN_PASSWORD" \
  --from-literal=OPENAI_API_KEY="${OPENAI_API_KEY:-not-set}" \
  --dry-run=client -o yaml | kubectl apply -f -

if [ -f ml-service/models/risk_model.joblib ]; then
  kubectl -n hap create configmap ml-model --from-file=ml-service/models/risk_model.joblib \
    --dry-run=client -o yaml | kubectl apply -f -
else
  echo "WARNING: ml-service/models/risk_model.joblib not found - risk scores will return 503 until you train it."
fi
