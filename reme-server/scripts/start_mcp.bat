@echo off
REM ReMe MCP Server Startup Script (Windows)
REM Usage: scripts\start_mcp.bat [transport] [log_level]
REM Example: scripts\start_mcp.bat sse INFO

REM Set console to UTF-8 encoding to support Unicode characters
chcp 65001 >nul

setlocal enabledelayedexpansion

REM ===== Configuration Parameters =====
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "ENV_FILE=%PROJECT_ROOT%\.env"

REM Default parameters
set "MCP_TRANSPORT=%~1"
if "%MCP_TRANSPORT%"=="" set "MCP_TRANSPORT=sse"

set "LOG_LEVEL=%~2"
if "%LOG_LEVEL%"=="" set "LOG_LEVEL=INFO"

echo ReMe MCP Server Startup Script (Windows)
echo ==========================================

REM ===== Environment Check =====
if not exist "%ENV_FILE%" (
    echo Error: .env file not found at %ENV_FILE%
    echo Please copy .env.example to .env and fill in your API keys
    pause
    exit /b 1
)

REM ===== Load Environment Variables (using python-dotenv) =====
echo Loading environment from %ENV_FILE%...
python -c "import os; from dotenv import dotenv_values; [os.environ.setdefault(k,v) for k,v in dotenv_values(r'%ENV_FILE%').items() if v]"

REM ===== Start Service =====
echo Starting ReMe MCP Server...
echo    Transport: %MCP_TRANSPORT%
echo    Log Level: %LOG_LEVEL%
echo.

cd /d "%PROJECT_ROOT%"

REM Start command with MCP backend
reme ^
    start ^
    service.backend=mcp ^
    mcp.transport=%MCP_TRANSPORT% ^
    log.level=%LOG_LEVEL%

if %errorlevel% neq 0 (
    echo Service exited with error
    pause
)
