-- Script simple para probar la persistencia

-- 1. Verificar estado actual
SELECT 
    ID_PERFIL,
    MENU_LABEL,
    MENU_PATH,
    IS_VISIBLE
FROM MENU_CONFIG 
WHERE ID_PERFIL = 2 
AND MENU_LABEL IN ('Intereses', 'Carta Factura', 'Captura Libre', 'Factura Global')
ORDER BY MENU_LABEL;

-- 2. Deshabilitar pantallas específicas
UPDATE MENU_CONFIG 
SET IS_VISIBLE = 0,
    FECHA_MODIFICACION = SYSDATE,
    USUARIO_MODIFICACION = 'admin'
WHERE ID_PERFIL = 2 
AND MENU_LABEL IN ('Intereses', 'Carta Factura', 'Captura Libre', 'Factura Global');

-- 3. Verificar cambios
SELECT 
    ID_PERFIL,
    MENU_LABEL,
    MENU_PATH,
    IS_VISIBLE,
    FECHA_MODIFICACION,
    USUARIO_MODIFICACION
FROM MENU_CONFIG 
WHERE ID_PERFIL = 2 
AND MENU_LABEL IN ('Intereses', 'Carta Factura', 'Captura Libre', 'Factura Global')
ORDER BY MENU_LABEL;

COMMIT;
