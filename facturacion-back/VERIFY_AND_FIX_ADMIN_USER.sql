-- Script para verificar y corregir el usuario admin

-- 1. Verificar si existe el usuario admin
SELECT NO_USUARIO, NOMBRE_EMPLEADO, ID_PERFIL, ESTATUS_USUARIO 
FROM USUARIOS 
WHERE NO_USUARIO = 'admin';

-- 2. Verificar los perfiles disponibles
SELECT ID_PERFIL, NOMBRE_PERFIL FROM PERFIL ORDER BY ID_PERFIL;

-- 3. Si el usuario admin no existe, crearlo como Administrador
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
    MODIFICA_UBICACION
) 
SELECT 
    'admin',                    -- NO_USUARIO: Identificador único del usuario
    'Administrador Sistema',    -- NOMBRE_EMPLEADO: Nombre completo del empleado
    'admin123',                -- PASSWORD: Contraseña del usuario
    'ACTIVO',                  -- ESTATUS_USUARIO: ACTIVO o INACTIVO
    3,                         -- ID_PERFIL: Administrador (ID_PERFIL = 3)
    SYSDATE,                   -- FECHA_ALTA: Fecha de creación
    SYSDATE,                   -- FECHA_MOD: Fecha de última modificación
    'SISTEMA',                 -- USUARIO_MOD: Usuario que realizó la modificación
    1,                         -- ID_DFI: Identificador de datos fiscales
    1,                         -- ID_ESTACIONAMIENTO: Identificador de estacionamiento
    'S'                        -- MODIFICA_UBICACION: S/N (Sí/No)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM USUARIOS WHERE NO_USUARIO = 'admin'
);

-- 4. Si el usuario admin existe pero tiene un perfil incorrecto, actualizarlo
UPDATE USUARIOS 
SET ID_PERFIL = 3, 
    NOMBRE_EMPLEADO = 'Administrador Sistema',
    FECHA_MOD = SYSDATE,
    USUARIO_MOD = 'SISTEMA'
WHERE NO_USUARIO = 'admin' AND ID_PERFIL != 3;

-- 5. Verificar el resultado final
SELECT NO_USUARIO, NOMBRE_EMPLEADO, ID_PERFIL, ESTATUS_USUARIO 
FROM USUARIOS 
WHERE NO_USUARIO = 'admin';

-- 6. Verificar que existe la configuración de menús para el administrador
SELECT ID_PERFIL, MENU_LABEL, IS_VISIBLE 
FROM MENU_CONFIG 
WHERE ID_PERFIL = 3 AND MENU_LABEL = 'Configuración';

COMMIT;
