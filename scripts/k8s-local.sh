#!/usr/bin/env bash
# Runs the whole platform on a local Kubernetes cluster with kind (free).
# Needs: docker, kind, kubectl.  Train the model first (scripts/demo.sh does it).
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { echo "Missing .env"; exit 1; }
set -a; source .env; set +a

CLUSTER=hap
TAG=0.1.0
SERVICES="patient-service analytics-service notification-service ai-assistant-service"

kind get clusters | grep -qx "$CLUSTER" || kind create cluster --name "$CLUSTER"
kubectl config use-context "kind-$CLUSTER"

echo "==> Building images"
for s in $SERVICES; do docker build -t "hap/$s:$TAG" "backend/$s"; done
docker build -t "hap/ml-service:$TAG" ml-service
docker build -t "hap/frontend:$TAG" frontend

echo "==> Loading images into kind"
for s in $SERVICES ml-service frontend; do kind load docker-image "hap/$s:$TAG" --name "$CLUSTER"; done

scripts/k8s-secrets.sh
kubectl apply -k infra/k8s/overlays/local

echo "==> Waiting for pods (Spring services can take 1-2 minutes)"
kubectl -n hap wait --for=condition=Available deploy --all --timeout=600s
kubectl -n hap get pods
echo
echo "Open the app:  kubectl -n hap port-forward svc/frontend 3000:80   then http://localhost:3000"
