@echo off
setlocal enabledelayedexpansion

set MOON_KV_HOME=%~dp0..
set LIB_DIR=%MOON_KV_HOME%\lib
set CONFIG_DIR=%MOON_KV_HOME%\config
set LOG_DIR=%MOON_KV_HOME%\logs
set DATA_DIR=%MOON_KV_HOME%\data

echo ========================================
echo   MOON-KV Server Startup Script
echo ========================================
echo.

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"

if "%JAVA_HOME%"=="" (
    set JAVA_CMD=java
) else (
    set JAVA_CMD=%JAVA_HOME%\bin\java
)

%JAVA_CMD% -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] Java is not installed or not in PATH
    echo [ERROR] Please install Java 17 or later
    echo.
    pause
    exit /b 1
)

for /f "tokens=3" %%i in ('%JAVA_CMD% -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%i
    goto :check_version
)

:check_version
if not defined JAVA_VERSION (
    echo.
    echo [WARNING] Could not detect Java version, proceeding anyway...
    goto :skip_version_check
)

set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "tokens=1,2 delims=." %%a in ("%JAVA_VERSION%") do (
    set MAJOR_VERSION=%%a
    set MINOR_VERSION=%%b
)

if %MAJOR_VERSION% lss 17 (
    echo.
    echo [ERROR] Java 17 or later is required
    echo [ERROR] Current version: %JAVA_VERSION%
    echo [ERROR] Please upgrade your Java installation
    echo.
    pause
    exit /b 1
)

echo [OK] Java version: %JAVA_VERSION%

:skip_version_check

if not exist "%LIB_DIR%" (
    echo.
    echo [ERROR] lib directory not found: %LIB_DIR%
    echo [ERROR] Please make sure the server is properly installed
    echo.
    pause
    exit /b 1
)

if not exist "%LIB_DIR%\moon-kv-server-1.0.0.jar" (
    echo.
    echo [ERROR] moon-kv-server-1.0.0.jar not found in lib directory
    echo [ERROR] Please rebuild the project or check the installation
    echo.
    pause
    exit /b 1
)

if not exist "%CONFIG_DIR%\server.properties" (
    echo.
    echo [WARNING] server.properties not found, using default configuration
)

if not exist "%CONFIG_DIR%\logback.xml" (
    echo.
    echo [WARNING] logback.xml not found, using default logging configuration
)

set JVM_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

if "%PORT%"=="" set PORT=4070

echo.
echo Starting MOON-KV Server...
echo   Home: %MOON_KV_HOME%
echo   Port: %PORT%
echo   Java: %JAVA_CMD%
echo   JVM Options: %JVM_OPTS%
echo   Log Dir: %LOG_DIR%
echo   Data Dir: %DATA_DIR%
echo.

%JAVA_CMD% %JVM_OPTS% ^
    -Dkv.wal.path="%DATA_DIR%\kv_store.wal" ^
    -Dlogback.configurationFile="%CONFIG_DIR%\logback.xml" ^
    -jar "%LIB_DIR%\moon-kv-server-1.0.0.jar" ^
    --port %PORT%

if errorlevel 1 (
    echo.
    echo ========================================
    echo [ERROR] Failed to start MOON-KV Server
    echo ========================================
    echo.
    echo Please check:
    echo   1. Port %PORT% is not in use
    echo   2. Java version is 17 or later
    echo   3. Log files in: %LOG_DIR%
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo MOON-KV Server stopped
echo ========================================
echo.
pause
