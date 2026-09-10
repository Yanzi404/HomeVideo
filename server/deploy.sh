#!/bin/bash
set -e

echo "=== HomeVideo Server Deploy ==="

echo "[1/5] Building Maven project..."
mvn clean package -DskipTests -q

echo "[2/5] Uploading jar and Dockerfile to server..."
scp target/homevideo-server-0.1.0.jar server2:/opt/homevideo-server/
scp Dockerfile server2:/opt/homevideo-server/

echo "[3/5] Rebuilding Docker image..."
ssh server2 "cd /opt/hometok-platform && docker compose build --no-cache homevideo"

echo "[4/5] Restarting container..."
ssh server2 "cd /opt/hometok-platform && docker compose up -d homevideo"

echo "[5/5] Verifying deployment..."
sleep 3
ssh server2 "docker ps | grep homevideo"
