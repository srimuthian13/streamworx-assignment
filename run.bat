@echo off
echo ==============================================
echo Building Streamworx Microservices Assignment
echo ==============================================
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven build failed.
    exit /b %ERRORLEVEL%
)

echo.
echo ==============================================
echo Starting Docker Compose Containers
echo ==============================================
docker compose up --build
