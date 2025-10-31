-- Script para limpiar todas las configuraciones de menús guardadas

-- 1. Eliminar todas las configuraciones de menús existentes
DELETE FROM MENU_CONFIG;

-- 2. Verificar que se eliminaron todas las configuraciones
SELECT COUNT(*) as CONFIGURACIONES_RESTANTES FROM MENU_CONFIG;

-- 3. Recrear solo las configuraciones básicas para el administrador
-- Pestañas principales para Administrador (ID_PERFIL = 3)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Facturación', NULL, 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Consultas', NULL, 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Administración', NULL, 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Reportes Facturación Fiscal', NULL, 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Registro CFDI', NULL, 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Monitor', NULL, 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración', NULL, 1, 7, 'admin');

-- 4. Agregar pantallas básicas para Configuración (solo para administrador)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Correo', 'configuracion-correo', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Empresa', 'configuracion-empresa', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Temas', 'configuracion-temas', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Menús', 'configuracion-menus', 1, 4, 'admin');

-- 5. Verificar la configuración final
SELECT ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN 
FROM MENU_CONFIG 
WHERE ID_PERFIL = 3 
ORDER BY ORDEN;

COMMIT;
