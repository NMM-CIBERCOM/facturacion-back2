-- Script para verificar estado actual y restaurar usuarios
-- Ejecutar en Oracle Database

-- ==========================================
-- VERIFICAR ESTADO ACTUAL
-- ==========================================

-- 1. Verificar usuarios existentes
SELECT 'USUARIOS EXISTENTES ACTUALMENTE' AS INFO FROM DUAL;
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    ESTATUS_USUARIO,
    ID_PERFIL,
    USER_ROLE,
    PASSWORD_ENCODED
FROM USUARIOS
ORDER BY NO_USUARIO;

-- 2. Verificar perfiles existentes
SELECT 'PERFILES EXISTENTES' AS INFO FROM DUAL;
SELECT 
    ID_PERFIL,
    NOMBRE_PERFIL
FROM PERFIL
ORDER BY ID_PERFIL;

-- 3. Verificar relación SUPERADMIN-perfil
SELECT 'SUPERADMIN Y SU PERFIL' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL,
    u.USER_ROLE
FROM USUARIOS u
LEFT JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
WHERE u.NO_USUARIO = 'SUPERADMIN';

-- ==========================================
-- CREAR PERFIL SUPER ADMINISTRADOR
-- ==========================================

-- 4. Crear perfil Super Administrador
INSERT INTO PERFIL (
    ID_PERFIL,
    NOMBRE_PERFIL,
    FECHA_ALTA,
    FECHA_MOD,
    USUARIO_MOD
) VALUES (
    99,  -- ID único para Super Admin
    'Super Administrador',
    SYSDATE,
    SYSDATE,
    'SYSTEM'
);

-- 5. Actualizar SUPERADMIN con perfil correcto
UPDATE USUARIOS 
SET ID_PERFIL = 99,
    USER_ROLE = 'SUPER_ADMIN',
    NOMBRE_EMPLEADO = 'Super Administrador Cibercom'
WHERE NO_USUARIO = 'SUPERADMIN';

-- ==========================================
-- RESTAURAR USUARIOS ELIMINADOS
-- ==========================================

-- 6. Restaurar usuario ADMIN
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
    PASSWORD_ENCODED
) VALUES (
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
    'N'
);

-- 7. Restaurar usuario JEFE
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
    PASSWORD_ENCODED
) VALUES (
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
    'N'
);

-- 8. Restaurar usuario OPERADOR
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
    PASSWORD_ENCODED
) VALUES (
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
    'N'
);

-- ==========================================
-- CREAR ESTADOS DE PAGO PARA TODOS
-- ==========================================

-- 9. Crear estados de pago para usuarios restaurados
INSERT INTO USER_PAYMENT_STATUS (NO_USUARIO, PAYMENT_STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY)
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

-- ==========================================
-- VERIFICACIÓN FINAL
-- ==========================================

-- 10. Verificar estado final
SELECT 'ESTADO FINAL DESPUÉS DE RESTAURACIÓN' AS INFO FROM DUAL;
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
ORDER BY 
    CASE u.USER_ROLE 
        WHEN 'SUPER_ADMIN' THEN 1
        WHEN 'ADMINISTRADOR' THEN 2
        WHEN 'OPERADOR' THEN 3
        ELSE 4
    END,
    u.NO_USUARIO;

COMMIT;

-- Script completado
SELECT 'RESTAURACIÓN COMPLETADA EXITOSAMENTE' AS RESULTADO FROM DUAL;
