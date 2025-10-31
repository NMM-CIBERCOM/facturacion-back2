-- Script para crear todos los usuarios necesarios
-- Ejecutar en Oracle Database

-- ==========================================
-- CREAR USUARIOS FALTANTES
-- ==========================================

-- 1. Verificar usuarios existentes primero
SELECT 'USUARIOS ANTES DE CREAR' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    ESTATUS_USUARIO,
    ID_PERFIL
FROM USUARIOS
ORDER BY NO_USUARIO;

-- 2. Crear usuario ADMIN si no existe
INSERT INTO USUARIOS (
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    PASSWORD,
    ESTATUS_USUARIO,
    ID_PERFIL,
    FECHA_ALTA,
    FECHA_MOD,
    USUARIO_MOD,
    ID_DFI,
    ID_ESTACIONAMIENTO,
    MODIFICA_UBICACION,
    USER_ROLE,
    PASSWORD_ENCODED,
    TWO_FACTOR_ENABLED
) 
SELECT 
    'admin',
    'Administrador Sistema',
    'admin',
    'A',
    3,  -- ID del perfil Administrador
    SYSDATE,
    SYSDATE,
    'SYSTEM',
    1,
    1,
    'N',
    'ADMINISTRADOR',
    'N',
    'N'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM USUARIOS WHERE NO_USUARIO = 'admin'
);

-- 3. Crear usuario JEFE si no existe
INSERT INTO USUARIOS (
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    PASSWORD,
    ESTATUS_USUARIO,
    ID_PERFIL,
    FECHA_ALTA,
    FECHA_MOD,
    USUARIO_MOD,
    ID_DFI,
    ID_ESTACIONAMIENTO,
    MODIFICA_UBICACION,
    USER_ROLE,
    PASSWORD_ENCODED,
    TWO_FACTOR_ENABLED
) 
SELECT 
    'jefe',
    'Jefe de Crédito',
    'jefe',
    'A',
    2,  -- ID del perfil Jefe de Crédito
    SYSDATE,
    SYSDATE,
    'SYSTEM',
    1,
    1,
    'N',
    'ADMINISTRADOR',
    'N',
    'N'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM USUARIOS WHERE NO_USUARIO = 'jefe'
);

-- 4. Crear usuario OPERADOR si no existe
INSERT INTO USUARIOS (
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    PASSWORD,
    ESTATUS_USUARIO,
    ID_PERFIL,
    FECHA_ALTA,
    FECHA_MOD,
    USUARIO_MOD,
    ID_DFI,
    ID_ESTACIONAMIENTO,
    MODIFICA_UBICACION,
    USER_ROLE,
    PASSWORD_ENCODED,
    TWO_FACTOR_ENABLED
) 
SELECT 
    'operador',
    'Operador de Crédito',
    'operador',
    'A',
    1,  -- ID del perfil Operador de Crédito
    SYSDATE,
    SYSDATE,
    'SYSTEM',
    1,
    1,
    'N',
    'OPERADOR',
    'N',
    'N'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM USUARIOS WHERE NO_USUARIO = 'operador'
);

-- 5. Asegurar que SUPERADMIN esté activo
UPDATE USUARIOS 
SET ESTATUS_USUARIO = 'A',
    USER_ROLE = 'SUPER_ADMIN',
    ID_PERFIL = 99
WHERE NO_USUARIO = 'SUPERADMIN';

-- 6. Asegurar que todos los usuarios existentes estén activos
UPDATE USUARIOS 
SET ESTATUS_USUARIO = 'A'
WHERE NO_USUARIO IN ('admin', 'jefe', 'operador')
AND (ESTATUS_USUARIO != 'A' OR ESTATUS_USUARIO IS NULL);

-- ==========================================
-- CREAR ESTADOS DE PAGO
-- ==========================================

-- 7. Crear estados de pago para todos los usuarios activos
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

-- 8. Actualizar todos los estados de pago a PAID
UPDATE USER_PAYMENT_STATUS 
SET PAYMENT_STATUS = 'PAID',
    UPDATED_AT = SYSDATE,
    UPDATED_BY = 'SYSTEM';

-- ==========================================
-- VERIFICACIÓN FINAL
-- ==========================================

-- 9. Verificar usuarios después de crear
SELECT 'USUARIOS DESPUÉS DE CREAR' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL,
    u.USER_ROLE,
    ups.PAYMENT_STATUS,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO
FROM USUARIOS u
LEFT JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

-- 10. Contar usuarios activos
SELECT 'CONTEO FINAL DE USUARIOS ACTIVOS' AS INFO FROM DUAL;
SELECT COUNT(*) AS TOTAL_USUARIOS_ACTIVOS
FROM USUARIOS
WHERE ESTATUS_USUARIO = 'A';

COMMIT;

-- Script completado
SELECT 'CREACIÓN DE USUARIOS COMPLETADA' AS RESULTADO FROM DUAL;
