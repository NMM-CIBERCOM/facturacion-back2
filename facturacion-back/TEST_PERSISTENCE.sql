-- Script para probar la persistencia de la configuración

-- 1. Deshabilitar algunas pantallas para Jefe de Crédito (ID_PERFIL = 2)
UPDATE MENU_CONFIG 
SET IS_VISIBLE = 0, 
    FECHA_MODIFICACION = SYSDATE,
    USUARIO_MODIFICACION = 'admin'
WHERE ID_PERFIL = 2 
AND MENU_LABEL IN ('Intereses', 'Carta Factura', 'Captura Libre', 'Factura Global');

-- 2. Verificar los cambios
SELECT 
    mc.ID_CONFIG,
    mc.ID_PERFIL,
    p.NOMBRE_PERFIL,
    mc.MENU_LABEL,
    mc.MENU_PATH,
    mc.IS_VISIBLE,
    mc.FECHA_MODIFICACION,
    mc.USUARIO_MODIFICACION
FROM MENU_CONFIG mc
JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL
WHERE mc.ID_PERFIL = 2 
AND mc.MENU_LABEL IN ('Intereses', 'Carta Factura', 'Captura Libre', 'Factura Global')
ORDER BY mc.MENU_LABEL;

-- 3. Verificar configuración completa para Jefe de Crédito
SELECT 
    mc.MENU_LABEL,
    mc.MENU_PATH,
    mc.IS_VISIBLE,
    CASE 
        WHEN mc.MENU_PATH IS NULL THEN 'PESTAÑA PRINCIPAL'
        ELSE 'PANTALLA ESPECÍFICA'
    END as TIPO
FROM MENU_CONFIG mc
WHERE mc.ID_PERFIL = 2
ORDER BY 
    CASE WHEN mc.MENU_PATH IS NULL THEN 0 ELSE 1 END,
    mc.ORDEN;

COMMIT;
