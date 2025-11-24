@echo off
echo ========================================
echo RECOMPILANDO Y EJECUTANDO PROYECTO
echo ========================================
echo.

echo [1/4] Limpiando compilaciones anteriores...
call mvn clean
if %errorlevel% neq 0 (
    echo ERROR: Fallo al limpiar el proyecto
    pause
    exit /b 1
)

echo.
echo [2/4] Compilando el proyecto...
call mvn compile
if %errorlevel% neq 0 (
    echo ERROR: Fallo al compilar el proyecto
    pause
    exit /b 1
)

echo.
echo [3/4] Empaquetando el proyecto...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Fallo al empaquetar el proyecto
    pause
    exit /b 1
)

echo.
echo [4/4] Ejecutando la aplicacion...
echo.
echo ========================================
echo INICIANDO APLICACION
echo ========================================
echo.
call mvn spring-boot:run





