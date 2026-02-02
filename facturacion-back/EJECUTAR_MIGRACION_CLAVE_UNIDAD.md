# Solución: Error ORA-00904 CLAVE_UNIDAD

## Problema
La columna `CLAVE_UNIDAD` no existe en la tabla `CATALOGOS_PRODUCTOS_SERVICIOS` de tu base de datos local, pero la entidad JPA la espera.

## Solución

Ejecuta el script de migración en tu base de datos Oracle local:

### Opción 1: Desde SQL*Plus o SQL Developer

1. Conéctate a tu base de datos Oracle local:
   ```sql
   sqlplus nick/N1C0LASm@localhost:1521/xepdb1
   ```

2. Ejecuta el script:
   ```sql
   @src/main/resources/db/migration/add_clave_unidad_to_catalogos.sql
   ```

   O copia y pega el contenido del script directamente.

### Opción 2: Verificar si la columna existe primero

Si quieres verificar si la columna ya existe antes de ejecutar el script:

```sql
-- Verificar si la columna existe
SELECT column_name 
FROM user_tab_columns 
WHERE table_name = 'CATALOGOS_PRODUCTOS_SERVICIOS' 
  AND column_name = 'CLAVE_UNIDAD';

-- Si no existe, ejecutar:
ALTER TABLE CATALOGOS_PRODUCTOS_SERVICIOS 
ADD CLAVE_UNIDAD VARCHAR2(10);

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
```

### Opción 3: Desde la aplicación (temporal)

Si no puedes ejecutar el script ahora, puedes comentar temporalmente el campo en la entidad:

```java
// @Column(name = "CLAVE_UNIDAD", length = 10, nullable = true)
// private String claveUnidad;
```

Pero esto hará que el campo no se persista cuando la columna sí exista.

## Verificación

Después de ejecutar el script, verifica que la columna existe:

```sql
SELECT column_name, data_type, nullable 
FROM user_tab_columns 
WHERE table_name = 'CATALOGOS_PRODUCTOS_SERVICIOS' 
  AND column_name = 'CLAVE_UNIDAD';
```

Deberías ver:
- COLUMN_NAME: CLAVE_UNIDAD
- DATA_TYPE: VARCHAR2
- NULLABLE: N (o Y si aún no se ejecutó el MODIFY)

## Nota

Este script ya se ejecutó en producción, pero parece que no se ejecutó en tu base de datos local de desarrollo. Asegúrate de ejecutarlo para mantener la consistencia entre desarrollo y producción.
