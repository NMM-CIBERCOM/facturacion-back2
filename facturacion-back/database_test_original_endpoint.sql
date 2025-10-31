    -- Script para probar el endpoint original de perfiles
    -- Ejecutar en Oracle Database

    -- ==========================================
    -- PROBAR ENDPOINT ORIGINAL
    -- ==========================================

    -- 1. Verificar que la consulta del servicio funciona
    SELECT 'CONSULTA DEL SERVICIO MenuConfigService.obtenerPerfiles()' AS INFO FROM DUAL;
    SELECT 
        ID_PERFIL, 
        NOMBRE_PERFIL 
    FROM PERFIL 
    ORDER BY NOMBRE_PERFIL;

    -- 2. Verificar que no hay perfiles con datos NULL
    SELECT 'VERIFICACIÓN DE DATOS NULL' AS INFO FROM DUAL;
    SELECT 
        ID_PERFIL,
        NOMBRE_PERFIL,
        CASE 
            WHEN ID_PERFIL IS NULL THEN 'ID NULL'
            WHEN NOMBRE_PERFIL IS NULL THEN 'NOMBRE NULL'
            WHEN LENGTH(TRIM(NOMBRE_PERFIL)) = 0 THEN 'NOMBRE VACÍO'
            ELSE 'OK'
        END AS ESTADO
    FROM PERFIL
    ORDER BY ID_PERFIL;

    -- 3. Verificar estructura de la tabla PERFIL
    SELECT 'ESTRUCTURA DE TABLA PERFIL' AS INFO FROM DUAL;
    SELECT 
        COLUMN_NAME,
        DATA_TYPE,
        NULLABLE
    FROM USER_TAB_COLUMNS 
    WHERE TABLE_NAME = 'PERFIL' 
    ORDER BY COLUMN_ID;

    -- 4. Probar consulta con alias como en el servicio
    SELECT 'CONSULTA CON ALIAS COMO EN EL SERVICIO' AS INFO FROM DUAL;
    SELECT 
        ID_PERFIL as idPerfil, 
        NOMBRE_PERFIL as nombrePerfil 
    FROM PERFIL 
    ORDER BY NOMBRE_PERFIL;

    -- Script completado
    SELECT 'PRUEBA DE ENDPOINT ORIGINAL COMPLETADA' AS RESULTADO FROM DUAL;
