-- Script para habilitar autenticación de dos pasos (2FA) para un usuario
-- Ejecutar en Oracle Database

-- ==========================================
-- HABILITAR 2FA PARA USUARIO
-- ==========================================

-- 1. Verificar usuarios y su estado de 2FA
SELECT 'ESTADO ACTUAL DE 2FA' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.TWO_FACTOR_ENABLED,
    u.TWO_FACTOR_SECRET,
    u.ESTATUS_USUARIO
FROM USUARIOS u
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

-- 2. Habilitar 2FA para un usuario específico (ejemplo: admin)
-- Nota: El secret debe ser generado por el backend, este es solo un ejemplo
UPDATE USUARIOS 
SET TWO_FACTOR_ENABLED = 'Y',
    TWO_FACTOR_SECRET = 'DEFAULT_SECRET_KEY_EXAMPLE'  -- Este será reemplazado por el backend
WHERE NO_USUARIO = 'admin'
AND ESTATUS_USUARIO = 'A';

-- 3. Verificar que el usuario tiene 2FA habilitado
SELECT 'VERIFICACIÓN DE 2FA HABILITADO' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    TWO_FACTOR_ENABLED,
    CASE 
        WHEN TWO_FACTOR_ENABLED = 'Y' THEN '✅ 2FA HABILITADO'
        WHEN TWO_FACTOR_ENABLED = 'N' THEN '❌ 2FA DESHABILITADO'
        ELSE 'Sin configurar'
    END AS ESTADO_2FA
FROM USUARIOS
WHERE NO_USUARIO = 'admin';

-- Script completado
SELECT 'CONFIGURACIÓN DE 2FA COMPLETADA' AS RESULTADO FROM DUAL;
