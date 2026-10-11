#!/usr/bin/env bash
# Deletes EVERYTHING created by eks-deploy.sh so billing stops.
set -euo pipefail
cd "$(dirname "$0")/.."
REGION=us-east-1
CLUSTER=hap-demo

# Delete the namespace first so the load balancer and EBS volumes are removed by Kubernetes.
kubectl delete namespace hap --wait=true --timeout=10m || true
eksctl delete cluster -f infra/eks/cluster.yaml --wait
for repo in patient-service analytics-service notification-service ai-assistant-service ml-service frontend; do
  aws ecr delete-repository --repository-name "hap/$repo" --region "$REGION" --force >/dev/null 2>&1 || true
done
echo "Done. Check the AWS console (EC2 > Load Balancers, EC2 > Volumes) shows nothing left for hap-demo."
