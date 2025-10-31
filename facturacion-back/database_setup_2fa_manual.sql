-- Script para configurar 2FA con clave secreta específica
-- Ejecutar en Oracle Database DESPUÉS de obtener el secret del backend

-- ==========================================
-- CONFIGURACIÓN MANUAL DE 2FA
-- ==========================================

-- IMPORTANTE: 
-- 1. Primero ejecuta: curl -X POST "http://localhost:8080/api/auth/2fa/setup?username=admin"
-- 2. Copia el "secretKey" que te devuelve el endpoint
-- 3. Reemplaza 'TU_SECRET_KEY_AQUI' con el secretKey real
-- 4. Ejecuta este script

-- 1. Actualizar usuario con clave secreta (REEMPLAZAR CON SECRET REAL)
UPDATE USUARIOS 
SET TWO_FACTOR_SECRET = 'JLfxWrtH4nVwxjtS26DUcGy8QhU=',  -- REEMPLAZAR CON EL SECRET DEL BACKEND
    TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin'
AND ESTATUS_USUARIO = 'A';

-- 2. Verificar configuración
SELECT 'VERIFICACIÓN DE CONFIGURACIÓN' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    TWO_FACTOR_ENABLED,
    SUBSTR(TWO_FACTOR_SECRET, 1, 20) || '...' AS SECRET_PREVIEW,
    CASE 
        WHEN TWO_FACTOR_ENABLED = 'Y' THEN '✅ 2FA ACTIVO'
        WHEN TWO_FACTOR_ENABLED = 'N' AND TWO_FACTOR_SECRET IS NOT NULL THEN '⚙️ PENDIENTE DE ACTIVAR'
        ELSE '❌ 2FA NO CONFIGURADO'
    END AS ESTADO_2FA
FROM USUARIOS
WHERE NO_USUARIO = 'admin';

COMMIT;

-- Script completado
SELECT 'CONFIGURACIÓN MANUAL DE 2FA COMPLETADA' AS RESULTADO FROM DUAL;
