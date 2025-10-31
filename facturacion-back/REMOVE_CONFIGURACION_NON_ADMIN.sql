-- Script para eliminar completamente la pestaña de Configuración para perfiles no administradores
-- Solo los administradores (ID_PERFIL = 3) deben tener acceso a Configuración

-- Eliminar la pestaña de Configuración para Operador de Crédito (ID_PERFIL = 1)
DELETE FROM MENU_CONFIG WHERE ID_PERFIL = 1 AND MENU_LABEL = 'Configuración';

-- Eliminar la pestaña de Configuración para Jefe de Crédito (ID_PERFIL = 2)
DELETE FROM MENU_CONFIG WHERE ID_PERFIL = 2 AND MENU_LABEL = 'Configuración';

-- Asegurar que solo el Administrador (ID_PERFIL = 3) tenga acceso a Configuración
-- Si no existe, crearla
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
SELECT 3, 'Configuración', NULL, 1, 8, 'admin'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM MENU_CONFIG 
    WHERE ID_PERFIL = 3 AND MENU_LABEL = 'Configuración'
);

COMMIT;
