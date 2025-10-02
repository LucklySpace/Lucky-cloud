#!/bin/bash

# Build and start all services
echo "Building and starting all services..."

echo "Building all microservices..."
./build-all.sh

echo "Building Docker images..."
./build-images.sh

echo "Starting all services..."
./start-all.sh

echo "All services built and started successfully!"