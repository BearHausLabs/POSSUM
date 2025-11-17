@echo off
REM ========================================
REM  Test KeyLock Device
REM ========================================

echo.
echo ========================================
echo   Testing Keylock1 Device
echo ========================================
echo.

echo [1] Testing health endpoint...
curl -X GET http://localhost:8080/v1/keylock/health
echo.
echo.

echo [2] Testing current position...
curl -X GET http://localhost:8080/v1/keylock/position
echo.
echo.

echo [3] Turn the keylock on your keyboard and watch for changes!
echo     Press Ctrl+C to stop monitoring...
echo.

REM Stream position changes
curl -X GET http://localhost:8080/v1/keylock/stream

echo.
pause

