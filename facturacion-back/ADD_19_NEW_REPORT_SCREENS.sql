-- =====================================================
-- SCRIPT PARA AGREGAR LAS 19 PANTALLAS NUEVAS DE REPORTES
-- =====================================================

-- IMPORTANTE: Este script agrega las 19 pantallas nuevas de "Reportes Facturación Fiscal"
-- que faltan después de ejecutar el script principal

-- =====================================================
-- AGREGAR 19 PANTALLAS NUEVAS DE REPORTES FACTURACIÓN FISCAL
-- =====================================================

-- =====================================================
-- OPERADOR DE CRÉDITO (ID_PERFIL = 1)
-- =====================================================

-- Pantallas nuevas de Reportes Facturación Fiscal - Operador de Crédito
INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Reporte de Consulta Monederos', 'reportes-consulta-monederos', 1, 12, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Reporte de Ventas Máquina Corporativas Serely Polu', 'reportes-ventas-maquina-corporativas', 1, 13, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Régimen de Facturación No Misma Boleta', 'reportes-regimen-facturacion-no-misma-boleta', 1, 14, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Doble Facturación Pendiente por Defencia', 'reportes-doble-facturacion-pendiente-defencia', 1, 15, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Sustitución en Proceso', 'reportes-sustitucion-en-proceso', 1, 16, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Cancelación Sustitución de Facturación', 'reportes-cancelacion-sustitucion-facturacion', 1, 17, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Saldo a Favor de Clientes', 'reportes-saldo-favor-clientes', 1, 18, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Orden de Módulos y Facturación', 'reportes-orden-modulos-facturacion', 1, 19, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Consulta de Usuarios', 'reportes-consulta-usuarios', 1, 20, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Consulta Tiendas de Total de Facturas Diarias', 'reportes-consulta-tiendas-facturas-diarias', 1, 21, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Validación por Importe Intereses', 'reportes-validacion-importe-intereses', 1, 22, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Conciliación Cambio de Sistema de Facturación', 'reportes-conciliacion-cambio-sistema-facturacion', 1, 23, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Control de Complementos de Pago (REP) Generados por Ventas Corporativas', 'reportes-control-complementos-pago-rep', 1, 24, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Reporte por Factura de Mercancía de Monederos', 'reportes-factura-mercancia-monederos', 1, 25, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Ventas Corporativas vs SAT', 'reportes-ventas-corporativas-vs-sat', 1, 26, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Captura Libre Complemento de Pago (REP)', 'reportes-captura-libre-complemento-pago-rep', 1, 27, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Conciliación Sistema de Facturación de Boletas vs SAT', 'reportes-conciliacion-boletas-vs-sat', 1, 28, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Reporte de Trazabilidad de Boletas Canceladas', 'reportes-trazabilidad-boletas-canceladas', 1, 29, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 1, 'Estatus Actualizar SAT de CFDI por Petición', 'reportes-estatus-actualizar-sat-cfdi', 1, 30, 'admin');

-- =====================================================
-- JEFE DE CRÉDITO (ID_PERFIL = 2)
-- =====================================================

-- Pantallas nuevas de Reportes Facturación Fiscal - Jefe de Crédito
INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Reporte de Consulta Monederos', 'reportes-consulta-monederos', 1, 12, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Reporte de Ventas Máquina Corporativas Serely Polu', 'reportes-ventas-maquina-corporativas', 1, 13, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Régimen de Facturación No Misma Boleta', 'reportes-regimen-facturacion-no-misma-boleta', 1, 14, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Doble Facturación Pendiente por Defencia', 'reportes-doble-facturacion-pendiente-defencia', 1, 15, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Sustitución en Proceso', 'reportes-sustitucion-en-proceso', 1, 16, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Cancelación Sustitución de Facturación', 'reportes-cancelacion-sustitucion-facturacion', 1, 17, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Saldo a Favor de Clientes', 'reportes-saldo-favor-clientes', 1, 18, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Orden de Módulos y Facturación', 'reportes-orden-modulos-facturacion', 1, 19, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Consulta de Usuarios', 'reportes-consulta-usuarios', 1, 20, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Consulta Tiendas de Total de Facturas Diarias', 'reportes-consulta-tiendas-facturas-diarias', 1, 21, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Validación por Importe Intereses', 'reportes-validacion-importe-intereses', 1, 22, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Conciliación Cambio de Sistema de Facturación', 'reportes-conciliacion-cambio-sistema-facturacion', 1, 23, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Control de Complementos de Pago (REP) Generados por Ventas Corporativas', 'reportes-control-complementos-pago-rep', 1, 24, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Reporte por Factura de Mercancía de Monederos', 'reportes-factura-mercancia-monederos', 1, 25, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Ventas Corporativas vs SAT', 'reportes-ventas-corporativas-vs-sat', 1, 26, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Captura Libre Complemento de Pago (REP)', 'reportes-captura-libre-complemento-pago-rep', 1, 27, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Conciliación Sistema de Facturación de Boletas vs SAT', 'reportes-conciliacion-boletas-vs-sat', 1, 28, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Reporte de Trazabilidad de Boletas Canceladas', 'reportes-trazabilidad-boletas-canceladas', 1, 29, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 2, 'Estatus Actualizar SAT de CFDI por Petición', 'reportes-estatus-actualizar-sat-cfdi', 1, 30, 'admin');

-- =====================================================
-- ADMINISTRADOR (ID_PERFIL = 3)
-- =====================================================

-- Pantallas nuevas de Reportes Facturación Fiscal - Administrador
INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Reporte de Consulta Monederos', 'reportes-consulta-monederos', 1, 12, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Reporte de Ventas Máquina Corporativas Serely Polu', 'reportes-ventas-maquina-corporativas', 1, 13, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Régimen de Facturación No Misma Boleta', 'reportes-regimen-facturacion-no-misma-boleta', 1, 14, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Doble Facturación Pendiente por Defencia', 'reportes-doble-facturacion-pendiente-defencia', 1, 15, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Sustitución en Proceso', 'reportes-sustitucion-en-proceso', 1, 16, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Cancelación Sustitución de Facturación', 'reportes-cancelacion-sustitucion-facturacion', 1, 17, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Saldo a Favor de Clientes', 'reportes-saldo-favor-clientes', 1, 18, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Orden de Módulos y Facturación', 'reportes-orden-modulos-facturacion', 1, 19, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Consulta de Usuarios', 'reportes-consulta-usuarios', 1, 20, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Consulta Tiendas de Total de Facturas Diarias', 'reportes-consulta-tiendas-facturas-diarias', 1, 21, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Validación por Importe Intereses', 'reportes-validacion-importe-intereses', 1, 22, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Conciliación Cambio de Sistema de Facturación', 'reportes-conciliacion-cambio-sistema-facturacion', 1, 23, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Control de Complementos de Pago (REP) Generados por Ventas Corporativas', 'reportes-control-complementos-pago-rep', 1, 24, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Reporte por Factura de Mercancía de Monederos', 'reportes-factura-mercancia-monederos', 1, 25, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Ventas Corporativas vs SAT', 'reportes-ventas-corporativas-vs-sat', 1, 26, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Captura Libre Complemento de Pago (REP)', 'reportes-captura-libre-complemento-pago-rep', 1, 27, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Conciliación Sistema de Facturación de Boletas vs SAT', 'reportes-conciliacion-boletas-vs-sat', 1, 28, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Reporte de Trazabilidad de Boletas Canceladas', 'reportes-trazabilidad-boletas-canceladas', 1, 29, 'admin');

INSERT INTO MENU_CONFIG (ID_CONFIG, ID_PERFIL, MENU_LABEL, MENU_PATH, IS_VISIBLE, ORDEN, USUARIO_CREACION) 
VALUES (MENU_CONFIG_SEQ.NEXTVAL, 3, 'Estatus Actualizar SAT de CFDI por Petición', 'reportes-estatus-actualizar-sat-cfdi', 1, 30, 'admin');

-- =====================================================
-- VERIFICAR CONFIGURACIÓN AGREGADA
-- =====================================================

COMMIT;

-- Verificar total de pantallas de Reportes Facturación Fiscal por perfil
SELECT 
    ID_PERFIL,
    COUNT(*) as TOTAL_PANTALLAS_REPORTES
FROM MENU_CONFIG 
WHERE MENU_LABEL LIKE '%Reporte%' 
   OR MENU_LABEL LIKE '%Conciliación%'
   OR MENU_LABEL LIKE '%Control%'
   OR MENU_LABEL LIKE '%Validación%'
   OR MENU_LABEL LIKE '%Consulta%'
   OR MENU_LABEL LIKE '%Ventas%'
   OR MENU_LABEL LIKE '%Captura%'
   OR MENU_LABEL LIKE '%Trazabilidad%'
   OR MENU_LABEL LIKE '%Estatus%'
   OR MENU_LABEL LIKE '%Sustitución%'
   OR MENU_LABEL LIKE '%Cancelación%'
   OR MENU_LABEL LIKE '%Saldo%'
   OR MENU_LABEL LIKE '%Orden%'
   OR MENU_LABEL LIKE '%Régimen%'
   OR MENU_LABEL LIKE '%Doble%'
GROUP BY ID_PERFIL
ORDER BY ID_PERFIL;

-- Verificar que todas las 19 pantallas nuevas estén presentes
SELECT 
    ID_PERFIL,
    MENU_LABEL,
    MENU_PATH,
    ORDEN
FROM MENU_CONFIG 
WHERE MENU_LABEL IN (
    'Reporte de Consulta Monederos',
    'Reporte de Ventas Máquina Corporativas Serely Polu',
    'Régimen de Facturación No Misma Boleta',
    'Doble Facturación Pendiente por Defencia',
    'Sustitución en Proceso',
    'Cancelación Sustitución de Facturación',
    'Saldo a Favor de Clientes',
    'Orden de Módulos y Facturación',
    'Consulta de Usuarios',
    'Consulta Tiendas de Total de Facturas Diarias',
    'Validación por Importe Intereses',
    'Conciliación Cambio de Sistema de Facturación',
    'Control de Complementos de Pago (REP) Generados por Ventas Corporativas',
    'Reporte por Factura de Mercancía de Monederos',
    'Ventas Corporativas vs SAT',
    'Captura Libre Complemento de Pago (REP)',
    'Conciliación Sistema de Facturación de Boletas vs SAT',
    'Reporte de Trazabilidad de Boletas Canceladas',
    'Estatus Actualizar SAT de CFDI por Petición'
)
ORDER BY ID_PERFIL, ORDEN;

-- =====================================================
-- 19 PANTALLAS NUEVAS AGREGADAS
-- =====================================================
-- Ahora cada perfil debería tener 30 pantallas en "Reportes Facturación Fiscal":
-- - 11 pantallas originales
-- - 19 pantallas nuevas
-- =====================================================
