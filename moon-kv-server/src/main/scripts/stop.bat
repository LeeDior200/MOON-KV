@echo off
setlocal

set MOON_KV_HOME=%~dp0..
set PID_FILE=%MOON_KV_HOME%\moon-kv.pid

if not exist "%PID_FILE%" (
    echo MOON-KV Server is not running (no PID file found)
    exit /b 1
)

set /p PID=<"%PID_FILE%"

tasklist /FI "PID eq %PID%" 2>NUL | find /I "java.exe" >NUL
if errorlevel 1 (
    echo MOON-KV Server is not running (process %PID% not found)
    del "%PID_FILE%"
    exit /b 1
)

echo Stopping MOON-KV Server (PID: %PID%)...
taskkill /PID %PID% /F

if exist "%PID_FILE%" del "%PID_FILE%"
echo MOON-KV Server stopped

endlocal
