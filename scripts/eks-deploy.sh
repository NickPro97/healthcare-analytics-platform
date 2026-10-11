#!/usr/bin/env bash
# Deploys the platform to a short-lived AWS EKS cluster.
# COSTS MONEY (about $5-6/day). Run scripts/eks-teardown.sh as soon as you have your screenshots.
# Needs: aws CLI (logged in), eksctl, kubectl, docker with buildx.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { echo "Missing .env"; exit 1; }

REGION=us-east-1
CLUSTER=hap-demo
TAG="${TAG:-0.1.0}"
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
SERVICES="patient-service analytics-service notification-service ai-assistant-service"

if ! eksctl get cluster --name "$CLUSTER" --region "$REGION" >/dev/null 2>&1; then
  echo "==> Creating EKS cluster (15-20 minutes)"
  eksctl create cluster -f infra/eks/cluster.yaml
fi
aws eks update-kubeconfig --name "$CLUSTER" --region "$REGION"

echo "==> Pushing images to ECR (built for linux/amd64 because EKS nodes are x86)"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
build_push() {  # name, context
  aws ecr describe-repositories --repository-names "hap/$1" --region "$REGION" >/dev/null 2>&1 \
    || aws ecr create-repository --repository-name "hap/$1" --region "$REGION" >/dev/null
  docker buildx build --platform linux/amd64 -t "$REGISTRY/hap/$1:$TAG" --push "$2"
}
for s in $SERVICES; do build_push "$s" "backend/$s"; done
build_push ml-service ml-service
build_push frontend frontend

scripts/k8s-secrets.sh
kubectl kustomize infra/k8s/overlays/eks \
  | sed "s#image: hap/\(.*\):0.1.0#image: $REGISTRY/hap/\1:$TAG#" \
  | kubectl apply -f -

kubectl -n hap wait --for=condition=Available deploy --all --timeout=900s
echo "==> Public URL (DNS can take a few minutes):"
kubectl -n hap get svc frontend -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'; echo
