#!/bin/bash
# 배포 서버에서 실행되는 명령어
set -e

DEPLOY_PATH="/home/ubuntu"

echo "Deploy script started..."
cd "$DEPLOY_PATH"

echo "[INFO] REGISTRY=$DOCKER_CONTAINER_REGISTRY"
echo "[INFO] SHA=$GITHUB_SHA"

# 전체 이미지 pull
docker compose -f docker-compose.yml pull

# 전체 서비스 up
docker compose -f docker-compose.yml up -d

# nginx 설정 검증 후 reload
echo "[INFO] Validating Nginx configuration"
if ! docker exec pozit-nginx nginx -t; then
  echo "[ERROR] Nginx configuration validation failed"
  docker logs --tail=100 pozit-nginx
  exit 1
fi

# dangling 이미지 정리
docker image prune -f

echo "Deployment completed."

