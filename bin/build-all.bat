@echo off
echo Building all microservices...
echo.

echo [1/12] Building im-ai...
cd im-ai
call mvn clean package -DskipTests
cd ..

echo [2/12] Building im-analysis...
cd im-analysis
call mvn clean package -DskipTests
cd ..

echo [3/12] Building im-auth...
cd im-auth
call mvn clean package -DskipTests
cd ..

echo [4/12] Building im-common...
cd im-common
call mvn clean package -DskipTests
cd ..

echo [5/12] Building im-connect...
cd im-connect
call mvn clean package -DskipTests
cd ..

echo [6/12] Building im-database...
cd im-database
call mvn clean package -DskipTests
cd ..

echo [7/12] Building im-file...
cd im-file
call mvn clean package -DskipTests
cd ..

echo [8/12] Building im-gateway...
cd im-gateway
call mvn clean package -DskipTests
cd ..

echo [9/12] Building im-generator...
cd im-generator
call mvn clean package -DskipTests
cd ..

echo [10/12] Building im-meet...
cd im-meet
call mvn clean package -DskipTests
cd ..

echo [11/12] Building im-proxy...
cd im-proxy
call mvn clean package -DskipTests
cd ..

echo [12/12] Building im-server...
cd im-server
call mvn clean package -DskipTests
cd ..

echo.
echo All microservices built successfully!
pause