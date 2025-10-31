-- Script para agregar las pantallas faltantes para las últimas 3 pestañas
-- Registro CFDI, Monitor, y Configuración

-- 1. Pantallas para Registro CFDI
-- Operador de Crédito (ID_PERFIL = 1)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Registro de Constancias', 'registro-cfdi', 1, 1, 'admin');

-- Jefe de Crédito (ID_PERFIL = 2)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Registro de Constancias', 'registro-cfdi', 1, 1, 'admin');

-- Administrador (ID_PERFIL = 3)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Registro de Constancias', 'registro-cfdi', 1, 1, 'admin');

-- 2. Pantallas para Monitor
-- Operador de Crédito (ID_PERFIL = 1)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Gráficas', 'monitor-graficas', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Bitácora', 'monitor-bitacora', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Disponibilidad', 'monitor-disponibilidad', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Logs', 'monitor-logs', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Permisos', 'monitor-permisos', 1, 5, 'admin');

-- Jefe de Crédito (ID_PERFIL = 2)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Gráficas', 'monitor-graficas', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Bitácora', 'monitor-bitacora', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Disponibilidad', 'monitor-disponibilidad', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Logs', 'monitor-logs', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Permisos', 'monitor-permisos', 1, 5, 'admin');

-- Administrador (ID_PERFIL = 3)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Gráficas', 'monitor-graficas', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Bitácora', 'monitor-bitacora', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Disponibilidad', 'monitor-disponibilidad', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Logs', 'monitor-logs', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Permisos', 'monitor-permisos', 1, 5, 'admin');

-- 3. Pantallas para Configuración (solo para Administrador)
-- Solo Administrador (ID_PERFIL = 3) - Los otros perfiles no tienen acceso a Configuración
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Correo', 'configuracion-correo', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Empresa', 'configuracion-empresa', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Temas', 'configuracion-temas', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Configuración de Menús', 'configuracion-menus', 1, 4, 'admin');

COMMIT;
