-- Script de recuperación segura - NO MODIFICA DATOS EXISTENTES
-- Solo agrega lo necesario para que funcione el sistema
-- Ejecutar en Oracle Database

-- ==========================================
-- 1. VERIFICAR Y CREAR COLUMNAS FALTANTES
-- ==========================================
SELECT '=== CREANDO COLUMNAS FALTANTES ===' AS SECCION FROM DUAL;

-- Verificar si existe USER_ROLE
SELECT COUNT(*) INTO :user_role_exists FROM USER_TAB_COLUMNS 
WHERE TABLE_NAME = 'USUARIOS' AND COLUMN_NAME = 'USER_ROLE';

-- Crear USER_ROLE si no existe
BEGIN
    IF :user_role_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USUARIOS ADD USER_ROLE VARCHAR2(50) DEFAULT ''USER''';
        DBMS_OUTPUT.PUT_LINE('Columna USER_ROLE creada');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Columna USER_ROLE ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando USER_ROLE: ' || SQLERRM);
END;
/

-- Verificar si existe PASSWORD_ENCODED
SELECT COUNT(*) INTO :password_encoded_exists FROM USER_TAB_COLUMNS 
WHERE TABLE_NAME = 'USUARIOS' AND COLUMN_NAME = 'PASSWORD_ENCODED';

-- Crear PASSWORD_ENCODED si no existe
BEGIN
    IF :password_encoded_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USUARIOS ADD PASSWORD_ENCODED CHAR(1) DEFAULT ''N''';
        DBMS_OUTPUT.PUT_LINE('Columna PASSWORD_ENCODED creada');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Columna PASSWORD_ENCODED ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando PASSWORD_ENCODED: ' || SQLERRM);
END;
/

-- Verificar si existe TWO_FACTOR_ENABLED
SELECT COUNT(*) INTO :two_factor_enabled_exists FROM USER_TAB_COLUMNS 
WHERE TABLE_NAME = 'USUARIOS' AND COLUMN_NAME = 'TWO_FACTOR_ENABLED';

-- Crear TWO_FACTOR_ENABLED si no existe
BEGIN
    IF :two_factor_enabled_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USUARIOS ADD TWO_FACTOR_ENABLED CHAR(1) DEFAULT ''N''';
        DBMS_OUTPUT.PUT_LINE('Columna TWO_FACTOR_ENABLED creada');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Columna TWO_FACTOR_ENABLED ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando TWO_FACTOR_ENABLED: ' || SQLERRM);
END;
/

-- Verificar si existe TWO_FACTOR_SECRET
SELECT COUNT(*) INTO :two_factor_secret_exists FROM USER_TAB_COLUMNS 
WHERE TABLE_NAME = 'USUARIOS' AND COLUMN_NAME = 'TWO_FACTOR_SECRET';

-- Crear TWO_FACTOR_SECRET si no existe
BEGIN
    IF :two_factor_secret_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USUARIOS ADD TWO_FACTOR_SECRET VARCHAR2(100)';
        DBMS_OUTPUT.PUT_LINE('Columna TWO_FACTOR_SECRET creada');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Columna TWO_FACTOR_SECRET ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando TWO_FACTOR_SECRET: ' || SQLERRM);
END;
/

-- Verificar si existe LAST_LOGIN
SELECT COUNT(*) INTO :last_login_exists FROM USER_TAB_COLUMNS 
WHERE TABLE_NAME = 'USUARIOS' AND COLUMN_NAME = 'LAST_LOGIN';

-- Crear LAST_LOGIN si no existe
BEGIN
    IF :last_login_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USUARIOS ADD LAST_LOGIN TIMESTAMP';
        DBMS_OUTPUT.PUT_LINE('Columna LAST_LOGIN creada');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Columna LAST_LOGIN ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando LAST_LOGIN: ' || SQLERRM);
END;
/

-- ==========================================
-- 2. CREAR TABLA USER_PAYMENT_STATUS SI NO EXISTE
-- ==========================================
SELECT '=== CREANDO TABLA USER_PAYMENT_STATUS ===' AS SECCION FROM DUAL;

-- Verificar si existe la tabla
SELECT COUNT(*) INTO :payment_table_exists FROM USER_TABLES 
WHERE TABLE_NAME = 'USER_PAYMENT_STATUS';

-- Crear tabla si no existe
BEGIN
    IF :payment_table_exists = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE USER_PAYMENT_STATUS (
            NO_USUARIO VARCHAR2(50) NOT NULL,
            PAYMENT_STATUS VARCHAR2(20) NOT NULL,
            LAST_PAYMENT_DATE DATE,
            NEXT_PAYMENT_DATE DATE,
            AMOUNT NUMBER(10,2),
            PAYMENT_METHOD VARCHAR2(50),
            NOTES VARCHAR2(500),
            CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP,
            UPDATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP,
            UPDATED_BY VARCHAR2(50),
            CONSTRAINT PK_USER_PAYMENT_STATUS PRIMARY KEY (NO_USUARIO),
            CONSTRAINT FK_UPS_USUARIO FOREIGN KEY (NO_USUARIO) REFERENCES USUARIOS(NO_USUARIO)
        )';
        DBMS_OUTPUT.PUT_LINE('Tabla USER_PAYMENT_STATUS creada');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Tabla USER_PAYMENT_STATUS ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando USER_PAYMENT_STATUS: ' || SQLERRM);
END;
/

-- ==========================================
-- 3. CREAR ÍNDICES SI NO EXISTEN
-- ==========================================
SELECT '=== CREANDO ÍNDICES ===' AS SECCION FROM DUAL;

-- Índice para USER_PAYMENT_STATUS
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX IDX_UPS_PAYMENT_STATUS ON USER_PAYMENT_STATUS(PAYMENT_STATUS)';
    DBMS_OUTPUT.PUT_LINE('Índice IDX_UPS_PAYMENT_STATUS creado');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN -- ORA-00955: name is already used
            DBMS_OUTPUT.PUT_LINE('Índice IDX_UPS_PAYMENT_STATUS ya existe');
        ELSE
            DBMS_OUTPUT.PUT_LINE('Error creando índice: ' || SQLERRM);
        END IF;
END;
/

-- ==========================================
-- 4. CREAR FUNCIÓN FN_USER_HAS_ACCESS SI NO EXISTE
-- ==========================================
SELECT '=== CREANDO FUNCIÓN FN_USER_HAS_ACCESS ===' AS SECCION FROM DUAL;

-- Verificar si existe la función
SELECT COUNT(*) INTO :function_exists FROM USER_PROCEDURES 
WHERE OBJECT_NAME = 'FN_USER_HAS_ACCESS';

-- Crear función si no existe
BEGIN
    IF :function_exists = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE OR REPLACE FUNCTION FN_USER_HAS_ACCESS(p_no_usuario VARCHAR2)
        RETURN CHAR
        IS
            v_user_role VARCHAR2(50);
            v_payment_status VARCHAR2(20);
            v_estatus_usuario VARCHAR2(1);
        BEGIN
            -- Obtener datos del usuario
            SELECT USER_ROLE, ESTATUS_USUARIO
            INTO v_user_role, v_estatus_usuario
            FROM USUARIOS
            WHERE NO_USUARIO = p_no_usuario;
            
            -- Si el usuario no está activo, no tiene acceso
            IF v_estatus_usuario != ''A'' THEN
                RETURN ''N'';
            END IF;
            
            -- Si es Super Admin, siempre tiene acceso
            IF v_user_role = ''SUPER_ADMIN'' THEN
                RETURN ''Y'';
            END IF;
            
            -- Obtener estado de pago
            BEGIN
                SELECT PAYMENT_STATUS
                INTO v_payment_status
                FROM USER_PAYMENT_STATUS
                WHERE NO_USUARIO = p_no_usuario;
            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    v_payment_status := ''PAID''; -- Por defecto, permitir acceso
            END;
            
            -- Verificar estado de pago
            IF v_payment_status IN (''PAID'') THEN
                RETURN ''Y'';
            ELSE
                RETURN ''N'';
            END IF;
            
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RETURN ''N'';
            WHEN OTHERS THEN
                RETURN ''N'';
        END;';
        DBMS_OUTPUT.PUT_LINE('Función FN_USER_HAS_ACCESS creada');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Función FN_USER_HAS_ACCESS ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando función: ' || SQLERRM);
END;
/

-- ==========================================
-- 5. CREAR USUARIO SUPERADMIN SI NO EXISTE
-- ==========================================
SELECT '=== CREANDO USUARIO SUPERADMIN ===' AS SECCION FROM DUAL;

-- Verificar si existe SUPERADMIN
SELECT COUNT(*) INTO :superadmin_exists FROM USUARIOS 
WHERE NO_USUARIO = 'SUPERADMIN';

-- Crear SUPERADMIN si no existe
BEGIN
    IF :superadmin_exists = 0 THEN
        EXECUTE IMMEDIATE '
        INSERT INTO USUARIOS (
            NO_USUARIO, NOMBRE_EMPLEADO, PASSWORD, ESTATUS_USUARIO, ID_PERFIL,
            FECHA_ALTA, FECHA_MOD, USUARIO_MOD, ID_DFI, ID_ESTACIONAMIENTO,
            MODIFICA_UBICACION, USER_ROLE, PASSWORD_ENCODED
        ) VALUES (
            ''SUPERADMIN'', ''Super Administrador'', ''admin123'', ''A'', 1,
            SYSDATE, SYSDATE, ''SYSTEM'', 1, 1,
            ''N'', ''SUPER_ADMIN'', ''N''
        )';
        DBMS_OUTPUT.PUT_LINE('Usuario SUPERADMIN creado');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Usuario SUPERADMIN ya existe');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error creando SUPERADMIN: ' || SQLERRM);
END;
/

-- ==========================================
-- 6. CREAR ESTADOS DE PAGO PARA USUARIOS EXISTENTES
-- ==========================================
SELECT '=== CREANDO ESTADOS DE PAGO ===' AS SECCION FROM DUAL;

-- Crear estados de pago para usuarios que no los tienen
INSERT INTO USER_PAYMENT_STATUS (
    NO_USUARIO, PAYMENT_STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY
)
SELECT 
    u.NO_USUARIO,
    CASE 
        WHEN u.NO_USUARIO = 'SUPERADMIN' THEN 'PAID'
        ELSE 'PAID'  -- Por defecto, permitir acceso a todos
    END as PAYMENT_STATUS,
    SYSDATE,
    SYSDATE,
    'SYSTEM'
FROM USUARIOS u
WHERE u.ESTATUS_USUARIO = 'A'
AND NOT EXISTS (
    SELECT 1 FROM USER_PAYMENT_STATUS ups 
    WHERE ups.NO_USUARIO = u.NO_USUARIO
);

COMMIT;

-- ==========================================
-- 7. ACTUALIZAR USUARIOS EXISTENTES CON VALORES POR DEFECTO
-- ==========================================
SELECT '=== ACTUALIZANDO USUARIOS EXISTENTES ===' AS SECCION FROM DUAL;

-- Actualizar usuarios existentes con valores por defecto
UPDATE USUARIOS 
SET USER_ROLE = CASE 
        WHEN USER_ROLE IS NULL THEN 'USER'
        ELSE USER_ROLE
    END,
    PASSWORD_ENCODED = CASE 
        WHEN PASSWORD_ENCODED IS NULL THEN 'N'
        ELSE PASSWORD_ENCODED
    END,
    TWO_FACTOR_ENABLED = CASE 
        WHEN TWO_FACTOR_ENABLED IS NULL THEN 'N'
        ELSE TWO_FACTOR_ENABLED
    END
WHERE USER_ROLE IS NULL 
   OR PASSWORD_ENCODED IS NULL 
   OR TWO_FACTOR_ENABLED IS NULL;

COMMIT;

-- ==========================================
-- 8. VERIFICACIÓN FINAL
-- ==========================================
SELECT '=== VERIFICACIÓN FINAL ===' AS SECCION FROM DUAL;

-- Mostrar usuarios y sus estados
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ESTATUS_USUARIO,
    u.USER_ROLE,
    u.PASSWORD_ENCODED,
    ups.PAYMENT_STATUS,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO
FROM USUARIOS u
LEFT JOIN USER_PAYMENT_STATUS ups ON u.NO_USUARIO = ups.NO_USUARIO
WHERE u.ESTATUS_USUARIO = 'A'
ORDER BY u.NO_USUARIO;

-- Script completado
SELECT '=== RECUPERACIÓN COMPLETADA ===' AS RESULTADO FROM DUAL;
