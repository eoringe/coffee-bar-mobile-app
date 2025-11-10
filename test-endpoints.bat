@echo off
echo Testing Coffee Bar API Endpoints...
echo.

echo ============================================
echo 1. Testing Health Check...
echo ============================================
curl http://localhost:8080/health
echo.
echo.

echo ============================================
echo 2. Testing Root Endpoint...
echo ============================================
curl http://localhost:8080/
echo.
echo.

echo ============================================
echo 3. Testing Menu Items...
echo ============================================
curl http://localhost:8080/menu-items
echo.
echo.

echo ============================================
echo Testing Complete!
echo ============================================


