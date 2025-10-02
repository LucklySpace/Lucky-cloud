#!/bin/bash

# Build Docker images for all services
echo "Building Docker images for all services..."

echo "[1/12] Building im-ai..."
cd im-ai && docker build -t im-ai . && cd ..

echo "[2/12] Building im-analysis..."
cd im-analysis && docker build -t im-analysis . && cd ..

echo "[3/12] Building im-auth..."
cd im-auth && docker build -t im-auth . && cd ..

echo "[4/12] Building im-connect..."
cd im-connect && docker build -t im-connect . && cd ..

echo "[5/12] Building im-database..."
cd im-database && docker build -t im-database . && cd ..

echo "[6/12] Building im-file..."
cd im-file && docker build -t im-file . && cd ..

echo "[7/12] Building im-gateway..."
cd im-gateway && docker build -t im-gateway . && cd ..

echo "[8/12] Building im-generator..."
cd im-generator && docker build -t im-generator . && cd ..

echo "[9/12] Building im-meet..."
cd im-meet && docker build -t im-meet . && cd ..

echo "[10/12] Building im-proxy..."
cd im-proxy && docker build -t im-proxy . && cd ..

echo "[11/12] Building im-server..."
cd im-server && docker build -t im-server . && cd ..

echo "[12/12] Building im-update..."
cd im-update && docker build -t im-update . && cd ..

echo "All Docker images built successfully!"