@echo off
REM Apply ReMe patches for HiveMind integration
REM Usage: patches\apply_patches.bat
REM Requires: Python environment with reme installed

setlocal

set "SCRIPT_DIR=%~dp0"

echo Applying ReMe patches for HiveMind...
echo.

python "%SCRIPT_DIR%apply_patch.py"

if %errorlevel% neq 0 (
    echo.
    echo Patch failed. See error above.
    pause
    exit /b 1
)

pause
