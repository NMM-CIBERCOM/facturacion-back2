-- Script para crear usuarios con diferentes perfiles
-- Jefe de Crédito
INSERT INTO USUARIOS (NO_USUARIO, NOMBRE_EMPLEADO, PASSWORD, ID_PERFIL, ESTATUS_USUARIO, FECHA_ALTA, USUARIO_CREACION) 
VALUES ('jefe001', 'Jefe de Crédito', 'jefe123', 2, 'ACTIVO', SYSDATE, 'admin');

-- Operador de Crédito  
INSERT INTO USUARIOS (NO_USUARIO, NOMBRE_EMPLEADO, PASSWORD, ID_PERFIL, ESTATUS_USUARIO, FECHA_ALTA, USUARIO_CREACION) 
VALUES ('oper001', 'Operador de Crédito', 'oper123', 1, 'ACTIVO', SYSDATE, 'admin');

COMMIT;
