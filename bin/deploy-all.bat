@echo off
echo Building and starting all services...
echo.

echo Building all microservices...
call build-all.bat

echo Building Docker images...
call build-images.bat

echo Starting all services...
call start-all.bat

echo.
echo All services built and started successfully!
pause