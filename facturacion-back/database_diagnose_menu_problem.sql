-- Script simple para diagnosticar problema de perfiles en configuración
-- Ejecutar en Oracle Database

-- ==========================================
-- DIAGNÓSTICO SIMPLE
-- ==========================================

-- 1. Verificar perfiles existentes
SELECT 'PERFILES EXISTENTES' AS INFO FROM DUAL;
SELECT 
    ID_PERFIL,
    NOMBRE_PERFIL
FROM PERFIL
ORDER BY ID_PERFIL;

-- 2. Verificar si existe tabla MENU_CONFIG
SELECT 'VERIFICAR TABLA MENU_CONFIG' AS INFO FROM DUAL;
SELECT COUNT(*) AS EXISTE_TABLA
FROM USER_TABLES 
WHERE TABLE_NAME = 'MENU_CONFIG';

-- 3. Si existe la tabla, verificar su contenido
SELECT 'CONTENIDO DE MENU_CONFIG' AS INFO FROM DUAL;
SELECT 
    ID_CONFIG,
    ID_PERFIL,
    MENU_LABEL,
    MENU_PATH,
    IS_VISIBLE,
    ORDEN
FROM MENU_CONFIG
ORDER BY ID_PERFIL, ORDEN;

-- 4. Verificar usuarios y sus perfiles
SELECT 'USUARIOS Y PERFILES' AS INFO FROM DUAL;
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL
FROM USUARIOS u
LEFT JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

-- 5. Probar consulta del servicio MenuConfigService
SELECT 'PRUEBA CONSULTA DEL SERVICIO' AS INFO FROM DUAL;
SELECT 
    ID_PERFIL, 
    NOMBRE_PERFIL 
FROM PERFIL 
ORDER BY NOMBRE_PERFIL;

-- Script completado
SELECT 'DIAGNÓSTICO COMPLETADO' AS RESULTADO FROM DUAL;
