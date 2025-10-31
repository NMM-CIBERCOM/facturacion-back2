-- Script para verificar el estado de visibilidad de las pantallas
-- Específicamente para el perfil "Jefe de Credito" (ID_PERFIL = 2)

-- 1. Verificar el estado actual de las pantallas para Jefe de Credito
SELECT 
    mc.ID_CONFIG,
    mc.ID_PERFIL,
    mc.MENU_LABEL,
    mc.MENU_PATH,
    mc.IS_VISIBLE,
    mc.ORDEN,
    p.NOMBRE_PERFIL
FROM MENU_CONFIG mc
JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL
WHERE mc.ID_PERFIL = 2 
AND mc.MENU_PATH IS NOT NULL
ORDER BY mc.ORDEN;

-- 2. Contar pantallas visibles vs ocultas para Jefe de Credito
SELECT 
    COUNT(*) as TOTAL_PANTALLAS,
    SUM(CASE WHEN IS_VISIBLE = 1 THEN 1 ELSE 0 END) as PANTALLAS_VISIBLES,
    SUM(CASE WHEN IS_VISIBLE = 0 THEN 1 ELSE 0 END) as PANTALLAS_OCULTAS
FROM MENU_CONFIG 
WHERE ID_PERFIL = 2 
AND MENU_PATH IS NOT NULL;

-- 3. Verificar específicamente las pantallas de Facturación para Jefe de Credito
SELECT 
    mc.MENU_LABEL,
    mc.IS_VISIBLE,
    CASE 
        WHEN mc.IS_VISIBLE = 1 THEN 'VISIBLE'
        ELSE 'OCULTO'
    END as ESTADO
FROM MENU_CONFIG mc
WHERE mc.ID_PERFIL = 2 
AND mc.MENU_LABEL IN ('Artículos', 'Intereses', 'Carta Factura', 'Global', 'Monederos', 'Captura Libre', 'Cancelación Masiva', 'Nóminas')
ORDER BY mc.MENU_LABEL;

-- 4. Verificar si hay registros con IS_VISIBLE = 0 para Jefe de Credito
SELECT 
    COUNT(*) as PANTALLAS_OCULTAS
FROM MENU_CONFIG 
WHERE ID_PERFIL = 2 
AND MENU_PATH IS NOT NULL
AND IS_VISIBLE = 0;
