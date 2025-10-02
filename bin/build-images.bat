@echo off
echo Building Docker images for all services...
echo.

echo [1/12] Building im-ai...
cd im-ai
call docker build -t im-ai .
cd ..

echo [2/12] Building im-analysis...
cd im-analysis
call docker build -t im-analysis .
cd ..

echo [3/12] Building im-auth...
cd im-auth
call docker build -t im-auth .
cd ..

echo [4/12] Building im-connect...
cd im-connect
call docker build -t im-connect .
cd ..

echo [5/12] Building im-database...
cd im-database
call docker build -t im-database .
cd ..

echo [6/12] Building im-file...
cd im-file
call docker build -t im-file .
cd ..

echo [7/12] Building im-gateway...
cd im-gateway
call docker build -t im-gateway .
cd ..

echo [8/12] Building im-generator...
cd im-generator
call docker build -t im-generator .
cd ..

echo [9/12] Building im-meet...
cd im-meet
call docker build -t im-meet .
cd ..

echo [10/12] Building im-proxy...
cd im-proxy
call docker build -t im-proxy .
cd ..

echo [11/12] Building im-server...
cd im-server
call docker build -t im-server .
cd ..

echo [12/12] Building im-update...
cd im-update
call docker build -t im-update .
cd ..

echo.
echo All Docker images built successfully!
pause