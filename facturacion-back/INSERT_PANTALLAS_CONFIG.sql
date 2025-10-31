-- Script para insertar configuraciones de pantallas específicas
-- Este script agrega las pantallas individuales dentro de cada pestaña

-- Limpiar configuraciones existentes de pantallas (mantener solo las pestañas principales)
DELETE FROM MENU_CONFIG WHERE MENU_PATH IS NOT NULL;

-- Insertar pantallas para Facturación
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Intereses', 'facturacion-intereses', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Carta Factura', 'facturacion-carta-factura', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Global', 'facturacion-global', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Monederos', 'facturacion-monederos', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Captura Libre', 'facturacion-captura-libre', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Cancelación Masiva', 'facturacion-cancelacion-masiva', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Nóminas', 'facturacion-nominas', 1, 7, 'admin');

-- Insertar pantallas para Jefe de Crédito (ID_PERFIL = 2)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Intereses', 'facturacion-intereses', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Carta Factura', 'facturacion-carta-factura', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Global', 'facturacion-global', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Monederos', 'facturacion-monederos', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Captura Libre', 'facturacion-captura-libre', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Cancelación Masiva', 'facturacion-cancelacion-masiva', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Nóminas', 'facturacion-nominas', 1, 7, 'admin');

-- Insertar pantallas para Administrador (ID_PERFIL = 3)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Intereses', 'facturacion-intereses', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Carta Factura', 'facturacion-carta-factura', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Global', 'facturacion-global', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Monederos', 'facturacion-monederos', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Captura Libre', 'facturacion-captura-libre', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Cancelación Masiva', 'facturacion-cancelacion-masiva', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Nóminas', 'facturacion-nominas', 1, 7, 'admin');

-- Insertar pantallas para Consultas
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Facturas', 'consultas-facturas', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'SKU', 'consultas-sku', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Boletas', 'consultas-boletas', 1, 3, 'admin');

INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Facturas', 'consultas-facturas', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'SKU', 'consultas-sku', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Boletas', 'consultas-boletas', 1, 3, 'admin');

INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Facturas', 'consultas-facturas', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'SKU', 'consultas-sku', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Boletas', 'consultas-boletas', 1, 3, 'admin');

-- Insertar pantallas para Administración
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Empleados', 'admin-empleados', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Tiendas', 'admin-tiendas', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Períodos Perfil', 'admin-periodos-perfil', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Períodos Plataforma', 'admin-periodos-plataforma', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Kioscos', 'admin-kioscos', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Excepciones', 'admin-excepciones', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Secciones', 'admin-secciones', 1, 7, 'admin');

INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Empleados', 'admin-empleados', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Tiendas', 'admin-tiendas', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Períodos Perfil', 'admin-periodos-perfil', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Períodos Plataforma', 'admin-periodos-plataforma', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Kioscos', 'admin-kioscos', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Excepciones', 'admin-excepciones', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Secciones', 'admin-secciones', 1, 7, 'admin');

INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Empleados', 'admin-empleados', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Tiendas', 'admin-tiendas', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Períodos Perfil', 'admin-periodos-perfil', 1, 3, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Períodos Plataforma', 'admin-periodos-plataforma', 1, 4, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Kioscos', 'admin-kioscos', 1, 5, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Excepciones', 'admin-excepciones', 1, 6, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Secciones', 'admin-secciones', 1, 7, 'admin');

-- Insertar pantallas para Reportes Facturación Fiscal
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Reporte de Consulta Monederos', 'reportes-consulta-monederos', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Reporte de Ventas Máquina Corporativas Serely Polu', 'reportes-ventas-maquina-corporativas', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (1, 'Régimen de Facturación No Misma Boleta', 'reportes-regimen-facturacion-no-misma-boleta', 1, 3, 'admin');
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

-- Repetir para Jefe de Crédito (ID_PERFIL = 2)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Reporte de Consulta Monederos', 'reportes-consulta-monederos', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Reporte de Ventas Máquina Corporativas Serely Polu', 'reportes-ventas-maquina-corporativas', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (2, 'Régimen de Facturación No Misma Boleta', 'reportes-regimen-facturacion-no-misma-boleta', 1, 3, 'admin');
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

-- Repetir para Administrador (ID_PERFIL = 3)
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Reporte de Consulta Monederos', 'reportes-consulta-monederos', 1, 1, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Reporte de Ventas Máquina Corporativas Serely Polu', 'reportes-ventas-maquina-corporativas', 1, 2, 'admin');
INSERT INTO MENU_CONFIG (ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) VALUES (3, 'Régimen de Facturación No Misma Boleta', 'reportes-regimen-facturacion-no-misma-boleta', 1, 3, 'admin');
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

COMMIT;
