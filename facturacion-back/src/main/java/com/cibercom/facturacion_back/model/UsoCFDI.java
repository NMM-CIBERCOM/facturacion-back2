package com.cibercom.facturacion_back.model;

public enum UsoCFDI {
    ADQUISICION_MERCANCIAS("G01", "Adquisición de mercancías"),
    DEVOLUCIONES_DESCUENTOS_BONIFICACIONES("G02", "Devoluciones, descuentos o bonificaciones"),
    GASTOS_EN_GENERAL("G03", "Gastos en general"),
    CONSTRUCCIONES("I01", "Construcciones"),
    MOBILIARIO_EQUIPO_OFICINA_INVERSIONES("I02", "Mobiliario y Equipo de Oficina por Inversiones"),
    EQUIPO_TRANSPORTE("I03", "Equipo de Transporte"),
    EQUIPO_COMPUTO_ACCESORIOS("I04", "Equipo de Computo y Accesorios"),
    DADOS_TROQUELES_MOLDES_MATRICES_HERRAMENTAL("I05", "Dados, Troqueles, Moldes, Matrices y Herramental"),
    COMUNICACIONES_TELEFONICAS("I06", "Comunicaciones Telefónicas"),
    COMUNICACIONES_SATELITALES("I07", "Comunicaciones Satelitales"),
    OTRA_MAQUINARIA_EQUIPO("I08", "Otra maquinaria y equipo"),
    HONORARIOS_MEDICOS("D01", "Honorarios médicos, dentales y gastos hospitalarios"),
    GASTOS_MEDICOS_POR_INCAPACIDAD("D02", "Gastos médicos por incapacidad o discapacidad"),
    GASTOS_FUNERALES("D03", "Gastos funerales"),
    DONATIVOS("D04", "Donativos"),
    INTERESES_REALES_CREDITOS_HIPOTECARIOS("D05", "Intereses reales efectivamente pagados por créditos hipotecarios (casa habitación)"),
    APORTACIONES_VOLUNTARIAS_SAR("D06", "Aportaciones voluntarias al SAR"),
    PRIMAS_SEGUROS_GASTOS_MEDICOS("D07", "Primas por seguros de gastos médicos"),
    GASTOS_TRANSPORTACION_ESCOLAR("D08", "Gastos de transportación escolar obligatoria"),
    DEPOSITOS_CUENTAS_AHORRO("D09", "Depósitos en cuentas para el ahorro, primas que tengan como base planes de pensiones"),
    PAGOS_SERVICIOS_EDUCATIVOS("D10", "Pagos por servicios educativos (colegiaturas)"),
    SIN_EFECTOS_FISCALES("S01", "Sin efectos fiscales"),
    PAGOS("CP01", "Pagos");

    private final String clave;
    private final String descripcion;

    UsoCFDI(String clave, String descripcion) {
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

