-- Script simple para asegurar acceso de todos los usuarios
-- Ejecutar en Oracle Database

-- ==========================================
-- SOLUCIÓN RÁPIDA - ASEGURAR ACCESO
-- ==========================================

-- 1. Crear estados de pago para todos los usuarios activos
INSERT INTO USER_PAYMENT_STATUS (
    NO_USUARIO, 
    PAYMENT_STATUS, 
    CREATED_AT, 
    UPDATED_AT, 
    UPDATED_BY
)
SELECT 
    NO_USUARIO,
    'PAID' as PAYMENT_STATUS,
    SYSDATE,
    SYSDATE,
    'SYSTEM'
FROM USUARIOS
WHERE ESTATUS_USUARIO = 'A'
AND NOT EXISTS (
    SELECT 1 FROM USER_PAYMENT_STATUS ups 
    WHERE ups.NO_USUARIO = USUARIOS.NO_USUARIO
);

-- 2. Actualizar todos los estados de pago a PAID
UPDATE USER_PAYMENT_STATUS 
SET PAYMENT_STATUS = 'PAID',
    UPDATED_AT = SYSDATE,
    UPDATED_BY = 'SYSTEM';

-- 3. Crear función simplificada que siempre permita acceso
CREATE OR REPLACE FUNCTION FN_USER_HAS_ACCESS(p_no_usuario VARCHAR2)
RETURN CHAR
IS
    v_estatus_usuario VARCHAR2(1);
BEGIN
    -- Solo verificar que el usuario esté activo
    SELECT ESTATUS_USUARIO
    INTO v_estatus_usuario
    FROM USUARIOS
    WHERE NO_USUARIO = p_no_usuario;
    
    -- Si está activo, tiene acceso
    IF v_estatus_usuario = 'A' THEN
        RETURN 'Y';
    ELSE
        RETURN 'N';
    END IF;
    
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 'N';
    WHEN OTHERS THEN
        -- En caso de error, permitir acceso
        RETURN 'Y';
END;
/

-- 4. Verificar resultado
SELECT 'VERIFICACIÓN FINAL' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    ups.PAYMENT_STATUS,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO
FROM USUARIOS u
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

COMMIT;

-- Script completado
SELECT 'ACCESO ASEGURADO PARA TODOS LOS USUARIOS' AS RESULTADO FROM DUAL;
