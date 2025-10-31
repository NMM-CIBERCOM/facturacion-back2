-- Script para agregar usuarios Jefe de Crédito y Operador de Crédito
-- Basado en la estructura existente de la tabla USUARIOS

-- Jefe de Crédito (ID_PERFIL = 2)
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
    'jefe001',                    -- NO_USUARIO: Identificador único del usuario
    'Jefe de Crédito',            -- NOMBRE_EMPLEADO: Nombre completo del empleado
    'jefe123',                    -- PASSWORD: Contraseña del usuario
    'ACTIVO',                     -- ESTATUS_USUARIO: ACTIVO o INACTIVO
    2,                            -- ID_PERFIL: Jefe de Crédito
    SYSDATE,                      -- FECHA_ALTA: Fecha de creación
    SYSDATE,                      -- FECHA_MOD: Fecha de última modificación
    'SISTEMA',                    -- USUARIO_MOD: Usuario que realizó la modificación
    1,                            -- ID_DFI: Identificador de datos fiscales
    1,                            -- ID_ESTACIONAMIENTO: Identificador de estacionamiento
    'S'                           -- MODIFICA_UBICACION: S/N (Sí/No)
);

-- Operador de Crédito (ID_PERFIL = 1)
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
    'oper001',                    -- NO_USUARIO: Identificador único del usuario
    'Operador de Crédito',        -- NOMBRE_EMPLEADO: Nombre completo del empleado
    'oper123',                    -- PASSWORD: Contraseña del usuario
    'ACTIVO',                     -- ESTATUS_USUARIO: ACTIVO o INACTIVO
    1,                            -- ID_PERFIL: Operador de Crédito
    SYSDATE,                      -- FECHA_ALTA: Fecha de creación
    SYSDATE,                      -- FECHA_MOD: Fecha de última modificación
    'SISTEMA',                    -- USUARIO_MOD: Usuario que realizó la modificación
    1,                            -- ID_DFI: Identificador de datos fiscales
    1,                            -- ID_ESTACIONAMIENTO: Identificador de estacionamiento
    'S'                           -- MODIFICA_UBICACION: S/N (Sí/No)
);

COMMIT;

-- Verificar que se crearon correctamente
SELECT NO_USUARIO, NOMBRE_EMPLEADO, ID_PERFIL, ESTATUS_USUARIO 
FROM USUARIOS 
WHERE NO_USUARIO IN ('jefe001', 'oper001')
ORDER BY ID_PERFIL;
