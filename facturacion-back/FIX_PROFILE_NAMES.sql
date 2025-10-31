-- Script para corregir los nombres de perfiles en la base de datos
-- Actualizar nombres para que coincidan con el frontend

-- Actualizar "Usuario operativo" a "Operador de Credito"
UPDATE PERFIL 
SET NOMBRE_PERFIL = 'Operador de Credito' 
WHERE ID_PERFIL = 1;

-- Actualizar "Usuario Jefe" a "Jefe de Credito"  
UPDATE PERFIL 
SET NOMBRE_PERFIL = 'Jefe de Credito' 
WHERE ID_PERFIL = 2;

-- El "Administrador" ya está correcto (ID_PERFIL = 3)

-- Eliminar "Contraloria" ya que no se usa
DELETE FROM PERFIL WHERE ID_PERFIL = 4;

COMMIT;

-- Verificar los cambios
SELECT ID_PERFIL, NOMBRE_PERFIL, STATUS_PERFIL FROM PERFIL ORDER BY ID_PERFIL;
