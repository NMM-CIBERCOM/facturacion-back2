CREATE TABLE configuracion_mensajes (
    id_configuracion     NUMBER(19)     NOT NULL,
    mensaje_seleccionado VARCHAR2(20)   NOT NULL,
    tipo_mensaje         VARCHAR2(50)   NOT NULL,
    asunto_personalizado VARCHAR2(200),
    mensaje_personalizado CLOB,
    activo               CHAR(1)        NOT NULL,
    fecha_creacion       TIMESTAMP(6)   NOT NULL,
    fecha_modificacion   TIMESTAMP(6),
    usuario_creacion     VARCHAR2(50),
    usuario_modificacion VARCHAR2(50),
    tipo_fuente          VARCHAR2(50),
    tamano_fuente        NUMBER(3),
    es_cursiva           CHAR(1),
    es_subrayado         CHAR(1),
    color_texto          VARCHAR2(10),
    
    CONSTRAINT pk_configuracion_mensajes PRIMARY KEY (id_configuracion)
);
-- =====================================================
-- ALTER TABLE: CONFIGURACION_MENSAJES
-- Agrega campos de formato de correo (fuente, tamaño, cursiva, subrayado, color)
-- =====================================================

-- Agregar columnas de formato
ALTER TABLE CONFIGURACION_MENSAJES ADD (
    TIPO_FUENTE     VARCHAR2(50),
    TAMANO_FUENTE   NUMBER(10),
    ES_CURSIVA      CHAR(1) DEFAULT 'N' NOT NULL,
    ES_SUBRAYADO    CHAR(1) DEFAULT 'N' NOT NULL,
    COLOR_TEXTO     VARCHAR2(20) DEFAULT '#000000' NOT NULL
);

-- Comentarios para las nuevas columnas
COMMENT ON COLUMN CONFIGURACION_MENSAJES.TIPO_FUENTE   IS 'Tipo de fuente para el formato de correo (p.ej., Arial, Verdana)';
COMMENT ON COLUMN CONFIGURACION_MENSAJES.TAMANO_FUENTE IS 'Tamaño de la fuente en puntos';
COMMENT ON COLUMN CONFIGURACION_MENSAJES.ES_CURSIVA    IS 'Indica si el texto es cursiva (S/N)';
COMMENT ON COLUMN CONFIGURACION_MENSAJES.ES_SUBRAYADO  IS 'Indica si el texto es subrayado (S/N)';
COMMENT ON COLUMN CONFIGURACION_MENSAJES.COLOR_TEXTO   IS 'Color del texto en formato hexadecimal (p.ej., #000000)';

-- Confirmar cambios
COMMIT;