-- Script simple para ejecutar recuperación básica
-- Ejecutar en Oracle Database

-- ==========================================
-- RECUPERACIÓN BÁSICA SIN MODIFICAR DATOS
-- ==========================================

-- 1. Crear columnas faltantes en USUARIOS
ALTER TABLE USUARIOS ADD USER_ROLE VARCHAR2(50) DEFAULT 'USER';
ALTER TABLE USUARIOS ADD PASSWORD_ENCODED CHAR(1) DEFAULT 'N';
ALTER TABLE USUARIOS ADD TWO_FACTOR_ENABLED CHAR(1) DEFAULT 'N';
ALTER TABLE USUARIOS ADD TWO_FACTOR_SECRET VARCHAR2(100);
ALTER TABLE USUARIOS ADD LAST_LOGIN TIMESTAMP;

-- 2. Crear tabla USER_PAYMENT_STATUS
CREATE TABLE USER_PAYMENT_STATUS (
    NO_USUARIO VARCHAR2(50) NOT NULL,
    PAYMENT_STATUS VARCHAR2(20) NOT NULL,
    LAST_PAYMENT_DATE DATE,
    NEXT_PAYMENT_DATE DATE,
    AMOUNT NUMBER(10,2),
    PAYMENT_METHOD VARCHAR2(50),
    NOTES VARCHAR2(500),
    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP,
    UPDATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP,
    UPDATED_BY VARCHAR2(50),
    CONSTRAINT PK_USER_PAYMENT_STATUS PRIMARY KEY (NO_USUARIO)
);

-- 3. Crear estados de pago para usuarios existentes
INSERT INTO USER_PAYMENT_STATUS (NO_USUARIO, PAYMENT_STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY)
SELECT 
    NO_USUARIO,
    'PAID' as PAYMENT_STATUS,
    SYSDATE,
    SYSDATE,
    'SYSTEM'
FROM USUARIOS
WHERE ESTATUS_USUARIO = 'A';

-- 4. Actualizar SUPERADMIN con rol correcto
UPDATE USUARIOS 
SET USER_ROLE = 'SUPER_ADMIN'
WHERE NO_USUARIO = 'SUPERADMIN';

-- 5. Crear función de acceso
CREATE OR REPLACE FUNCTION FN_USER_HAS_ACCESS(p_no_usuario VARCHAR2)
RETURN CHAR
IS
    v_user_role VARCHAR2(50);
    v_payment_status VARCHAR2(20);
    v_estatus_usuario VARCHAR2(1);
BEGIN
    -- Obtener datos del usuario
    SELECT USER_ROLE, ESTATUS_USUARIO
    INTO v_user_role, v_estatus_usuario
    FROM USUARIOS
    WHERE NO_USUARIO = p_no_usuario;
    
    -- Si el usuario no está activo, no tiene acceso
    IF v_estatus_usuario != 'A' THEN
        RETURN 'N';
    END IF;
    
    -- Si es Super Admin, siempre tiene acceso
    IF v_user_role = 'SUPER_ADMIN' THEN
        RETURN 'Y';
    END IF;
    
    -- Obtener estado de pago
    BEGIN
        SELECT PAYMENT_STATUS
        INTO v_payment_status
        FROM USER_PAYMENT_STATUS
        WHERE NO_USUARIO = p_no_usuario;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            v_payment_status := 'PAID'; -- Por defecto, permitir acceso
    END;
    
    -- Verificar estado de pago
    IF v_payment_status IN ('PAID') THEN
        RETURN 'Y';
    ELSE
        RETURN 'N';
    END IF;
    
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 'N';
    WHEN OTHERS THEN
        RETURN 'N';
END;
/

-- 6. Verificar resultado
SELECT 'VERIFICACIÓN FINAL' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    u.USER_ROLE,
    u.PASSWORD_ENCODED,
    ups.PAYMENT_STATUS,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO
FROM USUARIOS u
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

COMMIT;

-- Script completado
SELECT 'RECUPERACIÓN BÁSICA COMPLETADA' AS RESULTADO FROM DUAL;
