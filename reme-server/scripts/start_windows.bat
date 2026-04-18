@echo off
REM ReMe Production Startup Script (Windows)
REM Usage: scripts\start_windows.bat [prod|dev] [prod|local]

REM Set console to UTF-8 encoding to support Unicode characters
chcp 65001 >nul

setlocal enabledelayedexpansion

REM ===== Configuration Parameters =====
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "ENV_FILE=%PROJECT_ROOT%\.env"
set "CONFIG_DIR=%PROJECT_ROOT%\config"

REM Default parameters
set "ENV_PROFILE=%~1"
if "%ENV_PROFILE%"=="" set "ENV_PROFILE=prod"

set "CONFIG_PROFILE=%~2"
if "%CONFIG_PROFILE%"=="" set "CONFIG_PROFILE=prod"

echo ReMe Production Startup Script (Windows)
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

REM ===== Create Data Directories =====
echo Setting up data directories...
if not exist "%PROJECT_ROOT%\data\vectors" mkdir "%PROJECT_ROOT%\data\vectors"
if not exist "%PROJECT_ROOT%\data\memories" mkdir "%PROJECT_ROOT%\data\memories"
if not exist "%PROJECT_ROOT%\data\logs" mkdir "%PROJECT_ROOT%\data\logs"

REM ===== Start Service =====
echo Starting ReMe HTTP Service...
echo    Profile: %ENV_PROFILE%
echo    Config:  %CONFIG_PROFILE%.yaml
echo    Port:    %HTTP_PORT:~0,4%
echo.

cd /d "%PROJECT_ROOT%"

REM Start command (Windows paths use backslashes, Python auto-compatible)
reme ^
    backend=http ^
    log.level=%LOG_LEVEL:~0,5% ^

if %errorlevel% neq 0 (
    echo Service exited with error
    pause
)