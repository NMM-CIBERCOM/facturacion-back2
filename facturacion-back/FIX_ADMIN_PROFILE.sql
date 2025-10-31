-- Script para corregir el perfil del usuario admin

-- 1. Verificar el estado actual del usuario admin
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL
FROM USUARIOS u
JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
WHERE u.NO_USUARIO = 'admin001';

-- 2. Verificar que existe el perfil de Administrador (ID_PERFIL = 3)
SELECT ID_PERFIL, NOMBRE_PERFIL 
FROM PERFIL 
WHERE NOMBRE_PERFIL LIKE '%Admin%' OR ID_PERFIL = 3;

-- 3. Actualizar el usuario admin001 para que tenga el perfil de Administrador
UPDATE USUARIOS 
SET ID_PERFIL = 3,
    FECHA_MOD = SYSDATE,
    USUARIO_MOD = 'SISTEMA'
WHERE NO_USUARIO = 'admin001';

-- 4. Verificar el cambio
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL
FROM USUARIOS u
JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
WHERE u.NO_USUARIO = 'admin001';

COMMIT;
