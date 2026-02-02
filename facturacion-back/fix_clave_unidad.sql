-- Script rápido para agregar la columna CLAVE_UNIDAD si no existe
-- Ejecuta este script en tu base de datos Oracle local

-- Verificar y agregar la columna si no existe
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE CATALOGOS_PRODUCTOS_SERVICIOS ADD CLAVE_UNIDAD VARCHAR2(10)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN  -- Columna ya existe
            DBMS_OUTPUT.PUT_LINE('La columna CLAVE_UNIDAD ya existe');
        ELSE
            RAISE;
        END IF;
END;
/

-- Comentario en la columna
COMMENT ON COLUMN CATALOGOS_PRODUCTOS_SERVICIOS.CLAVE_UNIDAD IS 'Código del catálogo SAT c_ClaveUnidad (ej: E48=Servicio, H87=Pieza, MTR=Metro)';

-- Actualizar registros existentes
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

COMMIT;

-- Verificar que se creó correctamente
SELECT column_name, data_type, nullable, data_default
FROM user_tab_columns 
WHERE table_name = 'CATALOGOS_PRODUCTOS_SERVICIOS' 
  AND column_name = 'CLAVE_UNIDAD';
