#!/bin/bash

# Start all services with Docker Compose
echo "Starting all services with Docker Compose..."

docker-compose up -d

echo "All services started!"
echo ""
echo "You can access the services at:"
echo "Gateway: http://localhost:9191"
echo "Auth Service: http://localhost:8084"
echo "Business Service: http://localhost:8085"
echo "Database Service: http://localhost:8086"
echo "File Service: http://localhost:8087"
echo "AI Service: http://localhost:8088"
echo "Analysis Service: http://localhost:8089"
echo "Generator Service: http://localhost:8090"
echo "Meet Service: http://localhost:8091"
echo "Proxy Service: http://localhost:8092"
echo "Update Service: http://localhost:8093"
echo ""
echo "Nacos Console: http://localhost:8848/nacos"
echo "RabbitMQ Management: http://localhost:15672"
echo "MinIO Console: http://localhost:9090"
echo "SRS Service: http://localhost:8080"
echo "PostgreSQL: localhost:35432"
echo "Redis: localhost:6379"