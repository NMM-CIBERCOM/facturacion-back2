-- Script para verificar y corregir estados de pago
-- Ejecutar en Oracle Database

-- ==========================================
-- VERIFICAR ESTADOS DE PAGO ACTUALES
-- ==========================================

-- 1. Verificar usuarios y sus estados de pago
SELECT 'USUARIOS Y ESTADOS DE PAGO' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    u.USER_ROLE,
    ups.PAYMENT_STATUS,
    ups.CREATED_AT,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO
FROM USUARIOS u
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

-- 2. Verificar usuarios sin estado de pago
SELECT 'USUARIOS SIN ESTADO DE PAGO' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    u.USER_ROLE
FROM USUARIOS u
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A'
AND ups.NO_USUARIO IS NULL
ORDER BY u.NO_USUARIO;

-- ==========================================
-- CORREGIR ESTADOS DE PAGO
-- ==========================================

-- 3. Crear estados de pago para usuarios que no los tienen
INSERT INTO USER_PAYMENT_STATUS (
    NO_USUARIO, 
    PAYMENT_STATUS, 
    CREATED_AT, 
    UPDATED_AT, 
    UPDATED_BY
)
SELECT 
    u.NO_USUARIO,
    'PAID' as PAYMENT_STATUS,
    SYSDATE,
    SYSDATE,
    'SYSTEM'
FROM USUARIOS u
WHERE u.ESTATUS_USUARIO = 'A'
AND NOT EXISTS (
    SELECT 1 FROM USER_PAYMENT_STATUS ups 
    WHERE ups.NO_USUARIO = u.NO_USUARIO
);

-- 4. Actualizar estados de pago existentes a PAID
UPDATE USER_PAYMENT_STATUS 
SET PAYMENT_STATUS = 'PAID',
    UPDATED_AT = SYSDATE,
    UPDATED_BY = 'SYSTEM'
WHERE PAYMENT_STATUS != 'PAID';

-- ==========================================
-- CORREGIR FUNCIÓN FN_USER_HAS_ACCESS
-- ==========================================

-- 5. Recrear función con lógica simplificada
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
    
    -- Obtener estado de pago (si existe)
    BEGIN
        SELECT PAYMENT_STATUS
        INTO v_payment_status
        FROM USER_PAYMENT_STATUS
        WHERE NO_USUARIO = p_no_usuario;
        
        -- Si tiene estado PAID, tiene acceso
        IF v_payment_status = 'PAID' THEN
            RETURN 'Y';
        ELSE
            RETURN 'N';
        END IF;
        
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- Si no tiene estado de pago, permitir acceso por defecto
            RETURN 'Y';
    END;
    
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 'N';
    WHEN OTHERS THEN
        -- En caso de error, permitir acceso
        RETURN 'Y';
END;
/

-- ==========================================
-- VERIFICACIÓN FINAL
-- ==========================================

-- 6. Verificar estado final
SELECT 'ESTADO FINAL DESPUÉS DE CORRECCIÓN' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    u.USER_ROLE,
    ups.PAYMENT_STATUS,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO,
    CASE 
        WHEN FN_USER_HAS_ACCESS(u.NO_USUARIO) = 'Y' THEN '✅ PUEDE ACCEDER'
        ELSE '❌ SIN ACCESO'
    END AS ESTADO_ACCESO
FROM USUARIOS u
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

-- 7. Probar acceso específico
SELECT 'PRUEBA DE ACCESO ESPECÍFICA' AS INFO FROM DUAL;
SELECT 
    'admin' AS USUARIO,
    FN_USER_HAS_ACCESS('admin') AS TIENE_ACCESO,
    CASE WHEN FN_USER_HAS_ACCESS('admin') = 'Y' THEN '✅ PUEDE ACCEDER' ELSE '❌ SIN ACCESO' END AS ESTADO
FROM DUAL
UNION ALL
SELECT 
    'jefe' AS USUARIO,
    FN_USER_HAS_ACCESS('jefe') AS TIENE_ACCESO,
    CASE WHEN FN_USER_HAS_ACCESS('jefe') = 'Y' THEN '✅ PUEDE ACCEDER' ELSE '❌ SIN ACCESO' END AS ESTADO
FROM DUAL
UNION ALL
SELECT 
    'operador' AS USUARIO,
    FN_USER_HAS_ACCESS('operador') AS TIENE_ACCESO,
    CASE WHEN FN_USER_HAS_ACCESS('operador') = 'Y' THEN '✅ PUEDE ACCEDER' ELSE '❌ SIN ACCESO' END AS ESTADO
FROM DUAL
UNION ALL
SELECT 
    'SUPERADMIN' AS USUARIO,
    FN_USER_HAS_ACCESS('SUPERADMIN') AS TIENE_ACCESO,
    CASE WHEN FN_USER_HAS_ACCESS('SUPERADMIN') = 'Y' THEN '✅ PUEDE ACCEDER' ELSE '❌ SIN ACCESO' END AS ESTADO
FROM DUAL;

COMMIT;

-- Script completado
SELECT 'CORRECCIÓN DE ESTADOS DE PAGO COMPLETADA' AS RESULTADO FROM DUAL;
