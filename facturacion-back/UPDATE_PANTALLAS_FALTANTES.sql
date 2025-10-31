-- Script para agregar las pantallas faltantes y corregir problemas

-- 1. Agregar Artículos a Facturación para todos los perfiles
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Artículos', 'facturacion-articulos', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Artículos', 'facturacion-articulos', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Artículos', 'facturacion-articulos', 1, 1, 'admin');

-- 2. Agregar Decodificador a Monitor para todos los perfiles
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Decodificador', 'monitor-decodificador', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Decodificador', 'monitor-decodificador', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Decodificador', 'monitor-decodificador', 1, 6, 'admin');

-- 3. Agregar todas las pantallas faltantes para Reportes Facturación Fiscal
-- Operador de Crédito (ID_PERFIL = 1)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Doble Facturación Pendiente por Defencia', 'reportes-doble-facturacion-pendiente', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Sustitución en Proceso', 'reportes-sustitucion-en-proceso', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Cancelación Sustitución de Facturación', 'reportes-cancelacion-sustitucion-facturacion', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Saldo a Favor de Clientes', 'reportes-saldo-favor-clientes', 1, 7, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Orden de Módulos y Facturación', 'reportes-orden-modulos-facturacion', 1, 8, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Consulta de Usuarios', 'reportes-consulta-usuarios', 1, 9, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Consulta Tiendas de Total de Facturas Diarias', 'reportes-consulta-tiendas-facturas-diarias', 1, 10, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Validación por Importe Intereses', 'reportes-validacion-importe-intereses', 1, 11, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Conciliación Cambio de Sistema de Facturación', 'reportes-conciliacion-cambio-sistema', 1, 12, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Control de Complementos de Pago (REP) Generados por Ventas Corporativas', 'reportes-control-complementos-pago-rep', 1, 13, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Reporte por Factura de Mercancía de Monederos', 'reportes-factura-mercancia-monederos', 1, 14, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Ventas Corporativas vs SAT', 'reportes-ventas-corporativas-vs-sat', 1, 15, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Captura Libre Complemento de Pago (REP)', 'reportes-captura-libre-complemento-pago', 1, 16, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Conciliación Sistema de Facturación de Boletas vs SAT', 'reportes-conciliacion-boletas-vs-sat', 1, 17, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Reporte de Trazabilidad de Boletas Canceladas', 'reportes-trazabilidad-boletas-canceladas', 1, 18, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Estatus Actualizar SAT de CFDI por Petición', 'reportes-estatus-actualizar-sat-cfdi', 1, 19, 'admin');

-- Jefe de Crédito (ID_PERFIL = 2)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Doble Facturación Pendiente por Defencia', 'reportes-doble-facturacion-pendiente', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Sustitución en Proceso', 'reportes-sustitucion-en-proceso', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Cancelación Sustitución de Facturación', 'reportes-cancelacion-sustitucion-facturacion', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Saldo a Favor de Clientes', 'reportes-saldo-favor-clientes', 1, 7, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Orden de Módulos y Facturación', 'reportes-orden-modulos-facturacion', 1, 8, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Consulta de Usuarios', 'reportes-consulta-usuarios', 1, 9, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Consulta Tiendas de Total de Facturas Diarias', 'reportes-consulta-tiendas-facturas-diarias', 1, 10, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Validación por Importe Intereses', 'reportes-validacion-importe-intereses', 1, 11, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Conciliación Cambio de Sistema de Facturación', 'reportes-conciliacion-cambio-sistema', 1, 12, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Control de Complementos de Pago (REP) Generados por Ventas Corporativas', 'reportes-control-complementos-pago-rep', 1, 13, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Reporte por Factura de Mercancía de Monederos', 'reportes-factura-mercancia-monederos', 1, 14, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Ventas Corporativas vs SAT', 'reportes-ventas-corporativas-vs-sat', 1, 15, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Captura Libre Complemento de Pago (REP)', 'reportes-captura-libre-complemento-pago', 1, 16, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Conciliación Sistema de Facturación de Boletas vs SAT', 'reportes-conciliacion-boletas-vs-sat', 1, 17, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Reporte de Trazabilidad de Boletas Canceladas', 'reportes-trazabilidad-boletas-canceladas', 1, 18, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Estatus Actualizar SAT de CFDI por Petición', 'reportes-estatus-actualizar-sat-cfdi', 1, 19, 'admin');

-- Administrador (ID_PERFIL = 3)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Doble Facturación Pendiente por Defencia', 'reportes-doble-facturacion-pendiente', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Sustitución en Proceso', 'reportes-sustitucion-en-proceso', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Cancelación Sustitución de Facturación', 'reportes-cancelacion-sustitucion-facturacion', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Saldo a Favor de Clientes', 'reportes-saldo-favor-clientes', 1, 7, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Orden de Módulos y Facturación', 'reportes-orden-modulos-facturacion', 1, 8, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Consulta de Usuarios', 'reportes-consulta-usuarios', 1, 9, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Consulta Tiendas de Total de Facturas Diarias', 'reportes-consulta-tiendas-facturas-diarias', 1, 10, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Validación por Importe Intereses', 'reportes-validacion-importe-intereses', 1, 11, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Conciliación Cambio de Sistema de Facturación', 'reportes-conciliacion-cambio-sistema', 1, 12, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Control de Complementos de Pago (REP) Generados por Ventas Corporativas', 'reportes-control-complementos-pago-rep', 1, 13, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Reporte por Factura de Mercancía de Monederos', 'reportes-factura-mercancia-monederos', 1, 14, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Ventas Corporativas vs SAT', 'reportes-ventas-corporativas-vs-sat', 1, 15, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Captura Libre Complemento de Pago (REP)', 'reportes-captura-libre-complemento-pago', 1, 16, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Conciliación Sistema de Facturación de Boletas vs SAT', 'reportes-conciliacion-boletas-vs-sat', 1, 17, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Reporte de Trazabilidad de Boletas Canceladas', 'reportes-trazabilidad-boletas-canceladas', 1, 18, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Estatus Actualizar SAT de CFDI por Petición', 'reportes-estatus-actualizar-sat-cfdi', 1, 19, 'admin');

-- 4. Eliminar completamente la pestaña de Configuración para perfiles no administradores
DELETE FROM MENU_CONFIG WHERE ID_PERFIL IN (1, 2) AND MENU_LABEL = 'Configuración';

COMMIT;
