-- Script para crear la tabla MENU_CONFIG
-- Esta tabla almacena las configuraciones de visibilidad de menús por perfil
-- NOTA: Si la tabla ya existe, este script puede fallar. Usar ALTER TABLE si es necesario.

CREATE TABLE MENU_CONFIG (
    ID_CONFIG NUMBER(10) PRIMARY KEY,
    ID_PERFIL NUMBER(10) NOT NULL,
    MENU_LABEL VARCHAR2(100) NOT NULL,
    MENU_PATH VARCHAR2(200),
    IS_VISIBLE NUMBER(1) DEFAULT 1,
    ORDEN NUMBER(10) DEFAULT 1,
    FECHA_CREACION DATE DEFAULT SYSDATE,
    FECHA_MODIFICACION DATE DEFAULT SYSDATE,
    USUARIO_CREACION VARCHAR2(50) DEFAULT 'SYSTEM',
    USUARIO_MODIFICACION VARCHAR2(50) DEFAULT 'SYSTEM',
    CONSTRAINT FK_MENU_CONFIG_PERFIL FOREIGN KEY (ID_PERFIL) REFERENCES PERFIL(ID_PERFIL)
);

-- Crear índice para búsquedas por perfil
CREATE INDEX IDX_MENU_CONFIG_PERFIL ON MENU_CONFIG(ID_PERFIL);

-- Crear índice para búsquedas por orden
CREATE INDEX IDX_MENU_CONFIG_ORDEN ON MENU_CONFIG(ID_PERFIL, ORDEN);

-- Crear secuencia para ID_CONFIG (si no se usa auto-increment)
CREATE SEQUENCE SEQ_MENU_CONFIG
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Crear trigger para auto-incrementar ID_CONFIG (opcional)
CREATE OR REPLACE TRIGGER TRG_MENU_CONFIG_ID
    BEFORE INSERT ON MENU_CONFIG
    FOR EACH ROW
BEGIN
    IF :NEW.ID_CONFIG IS NULL THEN
        SELECT SEQ_MENU_CONFIG.NEXTVAL INTO :NEW.ID_CONFIG FROM DUAL;
    END IF;
END;
/

-- Comentarios en la tabla
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

