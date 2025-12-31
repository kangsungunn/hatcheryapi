#!/bin/bash
# Gateway를 Docker로 실행하는 스크립트

echo "🚀 Building Gateway Docker image..."
docker build -t gateway .

echo "🐳 Starting Gateway container on localhost:8080..."
docker run -d \
  --name gateway \
  -p 8080:8080 \
  --env-file ../.env \
  --rm \
  gateway

echo "✅ Gateway is running on http://localhost:8080"
echo "📚 Swagger UI: http://localhost:8080/docs"
echo ""
echo "To stop the container:"
echo "  docker stop gateway"

