-- Script para limpiar usuarios administradores y mantener solo admin-admin123

-- 1. Verificar el estado actual de los usuarios
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    ID_PERFIL,
    ESTATUS_USUARIO
FROM USUARIOS 
WHERE NO_USUARIO IN ('admin', 'admin001')
ORDER BY NO_USUARIO;

-- 2. Verificar que admin tiene el perfil correcto (ID_PERFIL = 3)
SELECT 
    u.NO_USUARIO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL
FROM USUARIOS u
JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
WHERE u.NO_USUARIO = 'admin';

-- 3. Eliminar el usuario admin001
DELETE FROM USUARIOS 
WHERE NO_USUARIO = 'admin001';

-- 4. Verificar que admin001 fue eliminado
SELECT COUNT(*) as ADMIN001_EXISTS
FROM USUARIOS 
WHERE NO_USUARIO = 'admin001';

-- 5. Verificar el estado final de los usuarios
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    ID_PERFIL,
    ESTATUS_USUARIO
FROM USUARIOS 
ORDER BY NO_USUARIO;

-- 6. Verificar que admin tiene acceso a la configuración
SELECT 
    u.NO_USUARIO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL,
    CASE 
        WHEN u.ID_PERFIL = 3 THEN 'ADMINISTRADOR - ACCESO COMPLETO'
        ELSE 'NO ES ADMINISTRADOR'
    END as ESTADO_ACCESO
FROM USUARIOS u
JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
WHERE u.NO_USUARIO = 'admin';

COMMIT;
