@echo off
echo ========================================
echo RECOMPILANDO PROYECTO FACTURACION
echo ========================================
echo.

echo [1/3] Limpiando compilaciones anteriores...
call mvn clean
if %errorlevel% neq 0 (
    echo ERROR: Fallo al limpiar el proyecto
    pause
    exit /b 1
)

echo.
echo [2/3] Compilando el proyecto...
call mvn compile
if %errorlevel% neq 0 (
    echo ERROR: Fallo al compilar el proyecto
    pause
    exit /b 1
)

echo.
echo [3/3] Empaquetando el proyecto...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Fallo al empaquetar el proyecto
    pause
    exit /b 1
)

echo.
echo ========================================
echo COMPILACION EXITOSA
echo ========================================
echo.
echo El proyecto se ha recompilado correctamente.
echo Ahora puedes ejecutar la aplicacion con:
echo   mvn spring-boot:run
echo.
pause





