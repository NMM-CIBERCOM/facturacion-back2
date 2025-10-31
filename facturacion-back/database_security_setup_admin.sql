-- Script SQL para elementos que requieren privilegios especiales
-- Ejecutar como administrador de base de datos

-- Crear vista para usuarios con información de pago
CREATE OR REPLACE VIEW V_USUARIOS_CON_PAGO AS
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL,
    u.TWO_FACTOR_ENABLED,
    u.LAST_LOGIN,
    u.USER_ROLE,
    COALESCE(ups.PAYMENT_STATUS, 'PAID') as PAYMENT_STATUS,
    ups.LAST_PAYMENT_DATE,
    ups.NEXT_PAYMENT_DATE,
    ups.AMOUNT,
    ups.PAYMENT_METHOD,
    ups.NOTES,
    CASE 
        WHEN u.USER_ROLE = 'SUPER_ADMIN' THEN 'Y'
        WHEN COALESCE(ups.PAYMENT_STATUS, 'PAID') IN ('PAID') THEN 'Y'
        ELSE 'N'
    END as HAS_ACCESS
FROM USUARIOS u
LEFT JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A';

-- Crear sinónimos para facilitar el acceso
CREATE SYNONYM UPS FOR USER_PAYMENT_STATUS;
CREATE SYNONYM VUCP FOR V_USUARIOS_CON_PAGO;

-- Otorgar permisos de lectura a la vista
GRANT SELECT ON V_USUARIOS_CON_PAGO TO PUBLIC;

-- Script completado
SELECT 'Elementos con privilegios especiales creados exitosamente' AS RESULTADO FROM DUAL;
