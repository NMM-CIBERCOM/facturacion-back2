-- Script SQL para migrar contraseñas existentes a Base64
-- Ejecutar después de crear las tablas

-- Procedimiento para migrar contraseñas existentes a Base64
CREATE OR REPLACE PROCEDURE SP_MIGRATE_PASSWORDS_TO_BASE64
AS
    CURSOR c_users IS
        SELECT NO_USUARIO, PASSWORD 
        FROM USUARIOS 
        WHERE PASSWORD_ENCODED = 'N' AND PASSWORD IS NOT NULL;
    
    v_encoded_password VARCHAR2(200);
BEGIN
    FOR user_rec IN c_users LOOP
        -- Codificar contraseña en Base64
        v_encoded_password := UTL_ENCODE.BASE64_ENCODE(UTL_RAW.CAST_TO_RAW(user_rec.PASSWORD));
        
        -- Actualizar contraseña codificada
        UPDATE USUARIOS 
        SET PASSWORD = v_encoded_password,
            PASSWORD_ENCODED = 'Y'
        WHERE NO_USUARIO = user_rec.NO_USUARIO;
        
        COMMIT;
    END LOOP;
    
    DBMS_OUTPUT.PUT_LINE('Migración de contraseñas completada');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Error en migración: ' || SQLERRM);
END;
/

-- Ejecutar migración
EXEC SP_MIGRATE_PASSWORDS_TO_BASE64;

-- Verificar migración
SELECT 
    NO_USUARIO,
    CASE 
        WHEN PASSWORD_ENCODED = 'Y' THEN 'Codificada'
        ELSE 'Sin codificar'
    END as ESTADO_CONTRASEÑA,
    LENGTH(PASSWORD) as LONGITUD_PASSWORD
FROM USUARIOS
ORDER BY NO_USUARIO;

-- Script completado
SELECT 'Migración de contraseñas completada' AS RESULTADO FROM DUAL;
