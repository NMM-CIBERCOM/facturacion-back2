package com.cibercom.facturacion_back.model;

public enum RegimenFiscal {
    REGIMEN_GENERAL("601", "Régimen General de Ley Personas Morales"),
    REGIMEN_SIMPLIFICADO("603", "Régimen Simplificado"),
    SUELDOS_Y_SALARIOS("605", "Régimen de Sueldos y Salarios e Ingresos Asimilados a Salarios"),
    ARRENDAMIENTO("606", "Régimen de Arrendamiento"),
    REGIMEN_ENAJENACION_O_ADQUISICION("607", "Régimen de Enajenación o Adquisición de Bienes"),
    DEMAS_INGRESOS("608", "Régimen de los Demás Ingresos"),
    RESIDENTES_EXTRANJERO("610", "Régimen de de los Residentes en el Extranjero sin Establecimiento Permanente en México"),
    INGRESOS_DIVIDENDOS("611", "Régimen de Ingresos por Dividendos (socios y accionistas)"),
    PERSONAS_FISICAS_ACTIVIDADES_EMPRESARIALES("612", "Régimen de las Personas Físicas con Actividades Empresariales y Profesionales"),
    INGRESOS_POR_INTERESES("614", "Régimen de los ingresos por intereses"),
    REGIMEN_INGRESOS_POR_OBTENCION_PREMIOS("615", "Régimen de los Ingresos por Obtención de Premios"),
    SIN_OBLIGACIONES_FISCALES("616", "Sin Obligaciones Fiscales"),
    INCORPORACION_FISCAL("621", "Incorporación Fiscal"),
    ACTIVIDADES_EMPRESARIALES_INGRESOS_PLATAFORMAS("625", "Régimen de las Actividades Empresariales con Ingresos a través de Plataformas Tecnológicas"),
    REGIMEN_SIMPLIFICADO_CONFIANZA("626", "Régimen Simplificado de Confianza");

    private final String clave;
    private final String descripcion;

    RegimenFiscal(String clave, String descripcion) {
        this.clave = clave;
        this.descripcion = descripcion;
    }

    public String getClave() {
        return clave;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

