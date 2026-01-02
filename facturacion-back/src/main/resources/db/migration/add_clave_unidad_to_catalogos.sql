-- Script para agregar la columna CLAVE_UNIDAD a la tabla CATALOGOS_PRODUCTOS_SERVICIOS
-- Esta columna almacena el código del catálogo SAT c_ClaveUnidad (ej: E48, H87, MTR, etc.)

ALTER TABLE CATALOGOS_PRODUCTOS_SERVICIOS 
ADD CLAVE_UNIDAD VARCHAR2(10);

-- Comentario en la columna
COMMENT ON COLUMN CATALOGOS_PRODUCTOS_SERVICIOS.CLAVE_UNIDAD IS 'Código del catálogo SAT c_ClaveUnidad (ej: E48=Servicio, H87=Pieza, MTR=Metro)';

-- Actualizar registros existentes: si UNIDAD contiene un código válido, copiarlo a CLAVE_UNIDAD
-- Si no, usar 'E48' (Unidad de Servicio) por defecto
-- Lista completa de códigos válidos del catálogo SAT c_ClaveUnidad
UPDATE CATALOGOS_PRODUCTOS_SERVICIOS 
SET CLAVE_UNIDAD = CASE 
    WHEN UPPER(TRIM(UNIDAD)) IN ('H87', 'EA', 'E48', 'ACT', 'KGM', 'E51', 'A9', 'MTR', 'AB', 'BB', 'KT', 'SET', 'LTR', 
                                 'XBX', 'MON', 'HUR', 'MTK', '11', 'MGM', 'XPK', 'XKI', 'AS', 'GRM', 'PR', 'DPC', 
                                 'XUN', 'DAY', 'XLT', '10', 'MLT', 'E54') 
    THEN UPPER(TRIM(UNIDAD))
    WHEN LOWER(TRIM(UNIDAD)) = 'xun'
    THEN 'xun'
    ELSE 'E48'
END
WHERE CLAVE_UNIDAD IS NULL;

-- Hacer la columna NOT NULL después de actualizar los valores
ALTER TABLE CATALOGOS_PRODUCTOS_SERVICIOS 
MODIFY CLAVE_UNIDAD VARCHAR2(10) DEFAULT 'E48';
