#!/bin/bash
set -e

echo "=============================================="
echo "Building Streamworx Microservices Assignment"
echo "=============================================="
mvn clean package -DskipTests

echo ""
echo "=============================================="
echo "Starting Docker Compose Containers"
echo "=============================================="
docker compose up --build
