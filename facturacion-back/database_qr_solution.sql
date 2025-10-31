-- Script para generar código QR y configurar 2FA de forma alternativa
-- Ejecutar en Oracle SQL Developer

-- ==========================================
-- SOLUCIÓN ALTERNATIVA CON CÓDIGO QR
-- ==========================================

-- 1. Verificar estado actual
SELECT 'ESTADO ACTUAL' AS INFO FROM DUAL;
SELECT 
    USERNAME,
    SECRET_KEY,
    LENGTH(SECRET_KEY) AS LONGITUD,
    ENABLED
FROM USER_2FA_CONFIG
WHERE USERNAME = 'admin';

-- 2. Crear clave Base32 perfecta de 32 caracteres
UPDATE USER_2FA_CONFIG 
SET SECRET_KEY = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567',
    ENABLED = 'N'
WHERE USERNAME = 'admin';

-- 3. Verificar clave perfecta
SELECT 'VERIFICACIÓN CLAVE PERFECTA' AS INFO FROM DUAL;
SELECT 
    USERNAME,
    SECRET_KEY,
    LENGTH(SECRET_KEY) AS LONGITUD,
    CASE 
        WHEN LENGTH(SECRET_KEY) = 32 THEN '✅ PERFECTO (32)'
        ELSE '❌ INCORRECTO (' || LENGTH(SECRET_KEY) || ')'
    END AS VALIDACION_LONGITUD,
    CASE 
        WHEN REGEXP_LIKE(SECRET_KEY, '^[A-Z2-7]{32}$') THEN '✅ FORMATO BASE32 VÁLIDO'
        ELSE '❌ FORMATO INVÁLIDO'
    END AS VALIDACION_FORMATO
FROM USER_2FA_CONFIG
WHERE USERNAME = 'admin';

-- 4. Generar URL del código QR
SELECT 'CÓDIGO QR PARA GOOGLE AUTHENTICATOR' AS INFO FROM DUAL;
SELECT 
    'URL QR: ' || 'otpauth://totp/Cibercom%20Facturación:admin?secret=' || SECRET_KEY || '&issuer=Cibercom%20Facturación' AS QR_URL,
    'Clave: ' || SECRET_KEY AS SECRET_KEY,
    'Longitud: ' || LENGTH(SECRET_KEY) AS LONGITUD
FROM USER_2FA_CONFIG
WHERE USERNAME = 'admin';

-- 5. Crear función para generar código QR
CREATE OR REPLACE FUNCTION GENERATE_QR_URL(p_username VARCHAR2) 
RETURN VARCHAR2 IS
    v_secret VARCHAR2(50);
    v_qr_url VARCHAR2(500);
BEGIN
    SELECT SECRET_KEY INTO v_secret
    FROM USER_2FA_CONFIG
    WHERE USERNAME = p_username;
    
    v_qr_url := 'otpauth://totp/Cibercom%20Facturación:' || p_username || '?secret=' || v_secret || '&issuer=Cibercom%20Facturación';
    
    RETURN v_qr_url;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END;
/

-- 6. Probar función
SELECT 'PRUEBA DE FUNCIÓN QR' AS INFO FROM DUAL;
SELECT 
    'URL QR: ' || GENERATE_QR_URL('admin') AS QR_URL_COMPLETA
FROM DUAL;

COMMIT;

-- Script completado
SELECT 'CONFIGURACIÓN QR COMPLETADA' AS RESULTADO FROM DUAL;
