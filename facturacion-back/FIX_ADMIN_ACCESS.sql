-- Script completo para asegurar que el usuario admin tenga acceso a Configuración

-- 1. Eliminar usuario admin existente si existe
DELETE FROM USUARIOS WHERE NO_USUARIO = 'admin';

-- 2. Crear usuario admin como Administrador
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
) VALUES (
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
);

-- 2. Asegurar que existe la pestaña de Configuración para el administrador
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
SELECT 3, 'Configuración', NULL, 1, 8, 'admin'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM MENU_CONFIG 
    WHERE ID_PERFIL = 3 AND MENU_LABEL = 'Configuración'
);

-- 3. Asegurar que existen las pantallas de Configuración para el administrador
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Correo', 'configuracion-correo', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Empresa', 'configuracion-empresa', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Temas', 'configuracion-temas', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Menús', 'configuracion-menus', 1, 4, 'admin');

-- 4. Verificar el resultado
SELECT 'Usuario admin:' as INFO, NO_USUARIO, NOMBRE_EMPLEADO, ID_PERFIL, ESTATUS_USUARIO 
FROM USUARIOS 
WHERE NO_USUARIO = 'admin'
UNION ALL
SELECT 'Configuración visible:', MENU_LABEL, NULL, IS_VISIBLE, NULL
FROM MENU_CONFIG 
WHERE ID_PERFIL = 3 AND MENU_LABEL = 'Configuración';

COMMIT;
