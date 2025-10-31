-- Script simple para verificar y corregir la clave
-- Ejecutar en Oracle SQL Developer

-- ==========================================
-- VERIFICACIÓN SIMPLE Y CORRECCIÓN
-- ==========================================

-- 1. Verificar estado actual
SELECT 'ESTADO ACTUAL' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    TWO_FACTOR_SECRET,
    LENGTH(TWO_FACTOR_SECRET) AS LONGITUD
FROM USUARIOS
WHERE NO_USUARIO = 'admin';

-- 2. Limpiar completamente
UPDATE USUARIOS 
SET TWO_FACTOR_SECRET = NULL,
    TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin';

-- 3. Insertar clave usando función de concatenación para evitar problemas
UPDATE USUARIOS 
SET TWO_FACTOR_SECRET = 'ABCD' || 'EFGH' || 'IJKL' || 'MNOP' || 'QRST' || 'UVWX' || 'YZ23' || '4567',
    TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin';

-- 4. Verificar resultado
SELECT 'VERIFICACIÓN POST-CORRECCIÓN' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    TWO_FACTOR_SECRET,
    LENGTH(TWO_FACTOR_SECRET) AS LONGITUD,
    CASE 
        WHEN LENGTH(TWO_FACTOR_SECRET) = 32 THEN '✅ CORRECTO (32)'
        ELSE '❌ INCORRECTO (' || LENGTH(TWO_FACTOR_SECRET) || ')'
    END AS VALIDACION
FROM USUARIOS
WHERE NO_USUARIO = 'admin';

-- 5. Si aún no funciona, probar con clave más simple
UPDATE USUARIOS 
SET TWO_FACTOR_SECRET = 'A' || 'B' || 'C' || 'D' || 'E' || 'F' || 'G' || 'H' || 'I' || 'J' || 'K' || 'L' || 'M' || 'N' || 'O' || 'P' || 'Q' || 'R' || 'S' || 'T' || 'U' || 'V' || 'W' || 'X' || 'Y' || 'Z' || '2' || '3' || '4' || '5' || '6' || '7',
    TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin';

-- 6. Verificación final
SELECT 'VERIFICACIÓN FINAL' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    TWO_FACTOR_SECRET,
    LENGTH(TWO_FACTOR_SECRET) AS LONGITUD,
    CASE 
        WHEN LENGTH(TWO_FACTOR_SECRET) = 32 THEN '✅ CORRECTO (32)'
        ELSE '❌ INCORRECTO (' || LENGTH(TWO_FACTOR_SECRET) || ')'
    END AS VALIDACION
FROM USUARIOS
WHERE NO_USUARIO = 'admin';

COMMIT;

-- Script completado
SELECT 'CORRECCIÓN SIMPLE COMPLETADA' AS RESULTADO FROM DUAL;
