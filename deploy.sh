#!/bin/bash
# 배포 서버에서 실행되는 명령어
set -e

DEPLOY_PATH="/home/ubuntu"

echo "Deploy script started..."
cd "$DEPLOY_PATH"

echo "[INFO] REGISTRY=$DOCKER_CONTAINER_REGISTRY"
echo "[INFO] SHA=$GITHUB_SHA"

# 새 이미지 레이어를 풀 공간 확보
docker system prune -af || true
docker builder prune -af || true

# 전체 이미지 pull
docker compose -f docker-compose.yml pull

# 전체 서비스 up
docker compose -f docker-compose.yml up -d

echo "[INFO] Waiting for pozit-server to be healthy"
for i in $(seq 1 36); do
  STATUS=$(docker inspect -f '{{.State.Health.Status}}' pozit-server 2>/dev/null || echo "unknown")
  if [ "$STATUS" = "healthy" ]; then
    echo "[INFO] pozit-server is healthy"
    break
  fi
  if [ "$i" -eq 36 ]; then
    echo "[ERROR] pozit-server did not become healthy (status: $STATUS)"
    docker logs --tail=200 pozit-server
    exit 1
  fi
  sleep 5
done

# nginx 설정 검증 후 reload
echo "[INFO] Validating Nginx configuration"
if ! docker exec pozit-nginx nginx -t; then
  echo "[ERROR] Nginx configuration validation failed"
  docker logs --tail=100 pozit-nginx
  exit 1
fi

echo "[INFO] Reloading Nginx"
docker exec pozit-nginx nginx -s reload

# dangling 이미지 정리
docker image prune -af

docker builder prune -af

echo "Deployment completed."
