-- Script para verificar las pantallas de "Reportes Facturación Fiscal"
-- Específicamente para el perfil "Jefe de Credito" (ID_PERFIL = 2)

-- 1. Verificar todas las pantallas de Reportes Facturación Fiscal para Jefe de Credito
SELECT 
    mc.ID_CONFIG,
    mc.MENU_LABEL,
    mc.MENU_PATH,
    mc.IS_VISIBLE,
    mc.ORDEN,
    CASE 
        WHEN mc.IS_VISIBLE = 1 THEN 'VISIBLE'
        ELSE 'OCULTO'
    END as ESTADO
FROM MENU_CONFIG mc
WHERE mc.ID_PERFIL = 2 
AND mc.MENU_LABEL LIKE '%Reporte%'
ORDER BY mc.ORDEN;

-- 2. Contar pantallas de Reportes Facturación Fiscal
SELECT 
    COUNT(*) as TOTAL_REPORTES,
    SUM(CASE WHEN IS_VISIBLE = 1 THEN 1 ELSE 0 END) as REPORTES_VISIBLES,
    SUM(CASE WHEN IS_VISIBLE = 0 THEN 1 ELSE 0 END) as REPORTES_OCULTOS
FROM MENU_CONFIG 
WHERE ID_PERFIL = 2 
AND MENU_LABEL LIKE '%Reporte%';

-- 3. Verificar si existe la pestaña principal "Reportes Facturación Fiscal"
SELECT 
    mc.ID_CONFIG,
    mc.MENU_LABEL,
    mc.MENU_PATH,
    mc.IS_VISIBLE
FROM MENU_CONFIG mc
WHERE mc.ID_PERFIL = 2 
AND mc.MENU_LABEL = 'Reportes Facturación Fiscal'
AND mc.MENU_PATH IS NULL;

-- 4. Verificar todas las pantallas que deberían estar en Reportes Facturación Fiscal
SELECT 
    mc.MENU_LABEL,
    mc.IS_VISIBLE,
    CASE 
        WHEN mc.IS_VISIBLE = 1 THEN 'VISIBLE'
        ELSE 'OCULTO'
    END as ESTADO
FROM MENU_CONFIG mc
WHERE mc.ID_PERFIL = 2 
AND mc.MENU_LABEL IN (
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
ORDER BY mc.MENU_LABEL;
