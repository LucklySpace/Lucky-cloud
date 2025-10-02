#!/bin/bash

# Build all microservices
echo "Building all microservices..."

echo "[1/12] Building im-ai..."
cd im-ai && mvn clean package -DskipTests && cd ..

echo "[2/12] Building im-analysis..."
cd im-analysis && mvn clean package -DskipTests && cd ..

echo "[3/12] Building im-auth..."
cd im-auth && mvn clean package -DskipTests && cd ..

echo "[4/12] Building im-connect..."
cd im-connect && mvn clean package -DskipTests && cd ..

echo "[5/12] Building im-database..."
cd im-database && mvn clean package -DskipTests && cd ..

echo "[6/12] Building im-file..."
cd im-file && mvn clean package -DskipTests && cd ..

echo "[7/12] Building im-gateway..."
cd im-gateway && mvn clean package -DskipTests && cd ..

echo "[8/12] Building im-generator..."
cd im-generator && mvn clean package -DskipTests && cd ..

echo "[9/12] Building im-meet..."
cd im-meet && mvn clean package -DskipTests && cd ..

echo "[10/12] Building im-proxy..."
cd im-proxy && mvn clean package -DskipTests && cd ..

echo "[11/12] Building im-server..."
cd im-server && mvn clean package -DskipTests && cd ..

echo "[12/12] Building im-update..."
cd im-update && mvn clean package -DskipTests && cd ..

echo "All microservices built successfully!"