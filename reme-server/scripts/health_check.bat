@echo off
REM scripts/health_check.bat
SETLOCAL EnableDelayedExpansion

IF NOT DEFINED HTTP_PORT (
    SET "PORT=8002"
) ELSE (
    SET "PORT=%HTTP_PORT%"
)

SET "URL=http://127.0.0.1:%PORT%/health"

echo Checking health endpoint: %URL%
echo.

REM Try 3 times, with 5 seconds interval between attempts
for /L %%i in (1,1,3) do (
    echo Attempt %%i/3...
    
    REM Use curl to check health endpoint
    curl -sf "%URL%" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [SUCCESS] Service healthy ^(attempt %%i^)
        echo.
        ENDLOCAL
        exit /b 0
    )
    
    echo [WAITING] Service not ready yet...
    echo.
    
    REM Wait 5 seconds before next attempt
    if %%i lss 3 (
        timeout /t 5 /nobreak >nul
    )
)

echo [FAILED] Health check failed after 3 attempts
echo.
ENDLOCAL
exit /b 1
