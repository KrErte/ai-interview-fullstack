#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"

git pull
./gradlew build -x test
npm --prefix frontend install
npm --prefix frontend run build
docker compose -f docker-compose.prod.yml up -d --build
docker system prune -f

echo "Deployment finished."

