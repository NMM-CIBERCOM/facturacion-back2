-- Script ALTER TABLE para MENU_CONFIG
-- Asegura que la tabla tenga la estructura correcta
-- Ejecutar solo si la tabla ya existe y necesita modificaciones

-- Verificar y agregar columnas si no existen
-- Nota: En Oracle, primero verificamos si las columnas existen antes de agregarlas

-- Agregar FECHA_CREACION si no existe
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE MENU_CONFIG ADD FECHA_CREACION DATE DEFAULT SYSDATE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN -- Column already exists
            DBMS_OUTPUT.PUT_LINE('FECHA_CREACION ya existe');
        ELSE
            RAISE;
        END IF;
END;
/

-- Agregar FECHA_MODIFICACION si no existe
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE MENU_CONFIG ADD FECHA_MODIFICACION DATE DEFAULT SYSDATE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN -- Column already exists
            DBMS_OUTPUT.PUT_LINE('FECHA_MODIFICACION ya existe');
        ELSE
            RAISE;
        END IF;
END;
/

-- Agregar USUARIO_CREACION si no existe
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE MENU_CONFIG ADD USUARIO_CREACION VARCHAR2(50) DEFAULT ''SYSTEM''';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN -- Column already exists
            DBMS_OUTPUT.PUT_LINE('USUARIO_CREACION ya existe');
        ELSE
            RAISE;
        END IF;
END;
/

-- Agregar USUARIO_MODIFICACION si no existe
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE MENU_CONFIG ADD USUARIO_MODIFICACION VARCHAR2(50) DEFAULT ''SYSTEM''';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN -- Column already exists
            DBMS_OUTPUT.PUT_LINE('USUARIO_MODIFICACION ya existe');
        ELSE
            RAISE;
        END IF;
END;
/

-- Modificar columnas existentes para asegurar valores por defecto
ALTER TABLE MENU_CONFIG MODIFY (FECHA_CREACION DEFAULT SYSDATE);
ALTER TABLE MENU_CONFIG MODIFY (FECHA_MODIFICACION DEFAULT SYSDATE);
ALTER TABLE MENU_CONFIG MODIFY (USUARIO_CREACION DEFAULT 'SYSTEM');
ALTER TABLE MENU_CONFIG MODIFY (USUARIO_MODIFICACION DEFAULT 'SYSTEM');
ALTER TABLE MENU_CONFIG MODIFY (IS_VISIBLE DEFAULT 1);
ALTER TABLE MENU_CONFIG MODIFY (ORDEN DEFAULT 1);

-- Crear índices si no existen
CREATE INDEX IDX_MENU_CONFIG_PERFIL ON MENU_CONFIG(ID_PERFIL);

CREATE INDEX IDX_MENU_CONFIG_ORDEN ON MENU_CONFIG(ID_PERFIL, ORDEN);

-- Agregar constraint de foreign key si no existe
-- Nota: Si ya existe, esto fallará pero puedes ignorar el error
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE MENU_CONFIG ADD CONSTRAINT FK_MENU_CONFIG_PERFIL FOREIGN KEY (ID_PERFIL) REFERENCES PERFIL(ID_PERFIL)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -2260 OR SQLCODE = -2275 THEN -- Constraint already exists
            DBMS_OUTPUT.PUT_LINE('FK_MENU_CONFIG_PERFIL ya existe');
        ELSE
            RAISE;
        END IF;
END;
/

-- Agregar comentarios en la tabla y columnas
COMMENT ON TABLE MENU_CONFIG IS 'Configuración de visibilidad de menús por perfil de usuario';

COMMENT ON COLUMN MENU_CONFIG.ID_CONFIG IS 'Identificador único de la configuración';
COMMENT ON COLUMN MENU_CONFIG.ID_PERFIL IS 'Identificador del perfil de usuario';
COMMENT ON COLUMN MENU_CONFIG.MENU_LABEL IS 'Etiqueta/nombre del menú o pantalla';
COMMENT ON COLUMN MENU_CONFIG.MENU_PATH IS 'Ruta del menú (NULL para pestañas principales)';
COMMENT ON COLUMN MENU_CONFIG.IS_VISIBLE IS 'Indica si el menú es visible (1) u oculto (0)';
COMMENT ON COLUMN MENU_CONFIG.ORDEN IS 'Orden de visualización del menú';
COMMENT ON COLUMN MENU_CONFIG.FECHA_CREACION IS 'Fecha de creación del registro';
COMMENT ON COLUMN MENU_CONFIG.FECHA_MODIFICACION IS 'Fecha de última modificación';
COMMENT ON COLUMN MENU_CONFIG.USUARIO_CREACION IS 'Usuario que creó el registro';
COMMENT ON COLUMN MENU_CONFIG.USUARIO_MODIFICACION IS 'Usuario que realizó la última modificación';

-- Verificar estructura final
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    DATA_LENGTH,
    DATA_PRECISION,
    NULLABLE,
    DATA_DEFAULT
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = 'MENU_CONFIG'
ORDER BY COLUMN_ID;

