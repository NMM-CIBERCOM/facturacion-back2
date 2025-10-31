-- Script alternativo para trabajar con la columna LONG
-- Ejecutar en Oracle SQL Developer

-- ==========================================
-- SOLUCIÓN ALTERNATIVA PARA COLUMNA LONG
-- ==========================================

-- 1. Verificar estado actual
SELECT 'ESTADO ACTUAL' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    CASE 
        WHEN TWO_FACTOR_SECRET IS NULL THEN 'NULL'
        ELSE 'TIENE DATOS'
    END AS ESTADO_CLAVE
FROM USUARIOS
WHERE NO_USUARIO = 'admin';

-- 2. Limpiar la columna LONG
UPDATE USUARIOS 
SET TWO_FACTOR_SECRET = NULL,
    TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin';

-- 3. Insertar clave usando TO_LOB para convertir a LONG
-- Nota: Esto puede no funcionar si la columna ya tiene datos
UPDATE USUARIOS 
SET TWO_FACTOR_SECRET = TO_LOB('ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'),
    TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin';

-- 4. Verificar si funcionó
SELECT 'VERIFICACIÓN POST-INSERCIÓN' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    CASE 
        WHEN TWO_FACTOR_SECRET IS NULL THEN 'NULL'
        ELSE 'TIENE DATOS'
    END AS ESTADO_CLAVE
FROM USUARIOS
WHERE NO_USUARIO = 'admin';

-- 5. Si no funcionó, usar método alternativo
-- Crear tabla temporal para manejar la clave
CREATE TABLE TEMP_2FA_KEY (
    NO_USUARIO VARCHAR2(20),
    SECRET_KEY VARCHAR2(50)
);

-- 6. Insertar clave en tabla temporal
INSERT INTO TEMP_2FA_KEY (NO_USUARIO, SECRET_KEY) 
VALUES ('admin', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567');

-- 7. Verificar clave en tabla temporal
SELECT 'VERIFICACIÓN EN TABLA TEMPORAL' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    SECRET_KEY,
    LENGTH(SECRET_KEY) AS LONGITUD,
    CASE 
        WHEN LENGTH(SECRET_KEY) = 32 THEN '✅ LONGITUD CORRECTA (32)'
        ELSE '❌ LONGITUD INCORRECTA (' || LENGTH(SECRET_KEY) || ')'
    END AS VALIDACION
FROM TEMP_2FA_KEY
WHERE NO_USUARIO = 'admin';

-- 8. Mostrar clave para Google Authenticator
SELECT 'CLAVE PARA GOOGLE AUTHENTICATOR' AS INFO FROM DUAL;
SELECT 
    'Clave: ' || SECRET_KEY AS CLAVE_COMPLETA,
    'Longitud: ' || LENGTH(SECRET_KEY) AS LONGITUD_INFO
FROM TEMP_2FA_KEY
WHERE NO_USUARIO = 'admin';

COMMIT;

-- Script completado
SELECT 'SOLUCIÓN ALTERNATIVA COMPLETADA - USAR TABLA TEMPORAL' AS RESULTADO FROM DUAL;
