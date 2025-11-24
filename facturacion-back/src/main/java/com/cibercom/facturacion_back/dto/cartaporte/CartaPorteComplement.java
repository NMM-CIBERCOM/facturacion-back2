package com.cibercom.facturacion_back.dto.cartaporte;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartaPorteComplement {

    private String version = "3.1";
    private String idCcp;
    private String transpInternac = "No";
    private String entradaSalidaMerc;
    private String paisOrigenDestino;
    private String viaEntradaSalida;
    private String totalDistRec;
    private String registroIstmo;
    private String ubicacionPoloOrigen;
    private String ubicacionPoloDestino;
    private List<RegimenAduanero> regimenesAduaneros;
    private List<Ubicacion> ubicaciones;
    private Mercancias mercancias;
    private FiguraTransporte figuraTransporte;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getIdCcp() {
        return idCcp;
    }

    public void setIdCcp(String idCcp) {
        this.idCcp = idCcp;
    }

    public String getTranspInternac() {
        return transpInternac;
    }

    public void setTranspInternac(String transpInternac) {
        this.transpInternac = transpInternac;
    }

    public String getEntradaSalidaMerc() {
        return entradaSalidaMerc;
    }

    public void setEntradaSalidaMerc(String entradaSalidaMerc) {
        this.entradaSalidaMerc = entradaSalidaMerc;
    }

    public String getPaisOrigenDestino() {
        return paisOrigenDestino;
    }

    public void setPaisOrigenDestino(String paisOrigenDestino) {
        this.paisOrigenDestino = paisOrigenDestino;
    }

    public String getViaEntradaSalida() {
        return viaEntradaSalida;
    }

    public void setViaEntradaSalida(String viaEntradaSalida) {
        this.viaEntradaSalida = viaEntradaSalida;
    }

    public String getTotalDistRec() {
        return totalDistRec;
    }

    public void setTotalDistRec(String totalDistRec) {
        this.totalDistRec = totalDistRec;
    }

    public String getRegistroIstmo() {
        return registroIstmo;
    }

    public void setRegistroIstmo(String registroIstmo) {
        this.registroIstmo = registroIstmo;
    }

    public String getUbicacionPoloOrigen() {
        return ubicacionPoloOrigen;
    }

    public void setUbicacionPoloOrigen(String ubicacionPoloOrigen) {
        this.ubicacionPoloOrigen = ubicacionPoloOrigen;
    }

    public String getUbicacionPoloDestino() {
        return ubicacionPoloDestino;
    }

    public void setUbicacionPoloDestino(String ubicacionPoloDestino) {
        this.ubicacionPoloDestino = ubicacionPoloDestino;
    }

    public List<RegimenAduanero> getRegimenesAduaneros() {
        return regimenesAduaneros;
    }

    public void setRegimenesAduaneros(List<RegimenAduanero> regimenesAduaneros) {
        this.regimenesAduaneros = regimenesAduaneros;
    }

    public List<Ubicacion> getUbicaciones() {
        return ubicaciones;
    }

    public void setUbicaciones(List<Ubicacion> ubicaciones) {
        this.ubicaciones = ubicaciones;
    }

    public Mercancias getMercancias() {
        return mercancias;
    }

    public void setMercancias(Mercancias mercancias) {
        this.mercancias = mercancias;
    }

    public FiguraTransporte getFiguraTransporte() {
        return figuraTransporte;
    }

    public void setFiguraTransporte(FiguraTransporte figuraTransporte) {
        this.figuraTransporte = figuraTransporte;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RegimenAduanero {
        private String regimenAduanero;

        public String getRegimenAduanero() {
            return regimenAduanero;
        }

        public void setRegimenAduanero(String regimenAduanero) {
            this.regimenAduanero = regimenAduanero;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Ubicacion {
        private String tipoUbicacion;
        private String idUbicacion;
        private String rfcRemitenteDestinatario;
        private String nombreRemitenteDestinatario;
        private String numRegIdTrib;
        private String residenciaFiscal;
        private String numEstacion;
        private String nombreEstacion;
        private String navegacionTrafico;
        private String fechaHoraSalidaLlegada;
        private String tipoEstacion;
        private String distanciaRecorrida;
        private Domicilio domicilio;

        public String getTipoUbicacion() {
            return tipoUbicacion;
        }

        public void setTipoUbicacion(String tipoUbicacion) {
            this.tipoUbicacion = tipoUbicacion;
        }

        public String getIdUbicacion() {
            return idUbicacion;
        }

        public void setIdUbicacion(String idUbicacion) {
            this.idUbicacion = idUbicacion;
        }

        public String getRfcRemitenteDestinatario() {
            return rfcRemitenteDestinatario;
        }

        public void setRfcRemitenteDestinatario(String rfcRemitenteDestinatario) {
            this.rfcRemitenteDestinatario = rfcRemitenteDestinatario;
        }

        public String getNombreRemitenteDestinatario() {
            return nombreRemitenteDestinatario;
        }

        public void setNombreRemitenteDestinatario(String nombreRemitenteDestinatario) {
            this.nombreRemitenteDestinatario = nombreRemitenteDestinatario;
        }

        public String getNumRegIdTrib() {
            return numRegIdTrib;
        }

        public void setNumRegIdTrib(String numRegIdTrib) {
            this.numRegIdTrib = numRegIdTrib;
        }

        public String getResidenciaFiscal() {
            return residenciaFiscal;
        }

        public void setResidenciaFiscal(String residenciaFiscal) {
            this.residenciaFiscal = residenciaFiscal;
        }

        public String getNumEstacion() {
            return numEstacion;
        }

        public void setNumEstacion(String numEstacion) {
            this.numEstacion = numEstacion;
        }

        public String getNombreEstacion() {
            return nombreEstacion;
        }

        public void setNombreEstacion(String nombreEstacion) {
            this.nombreEstacion = nombreEstacion;
        }

        public String getNavegacionTrafico() {
            return navegacionTrafico;
        }

        public void setNavegacionTrafico(String navegacionTrafico) {
            this.navegacionTrafico = navegacionTrafico;
        }

        public String getFechaHoraSalidaLlegada() {
            return fechaHoraSalidaLlegada;
        }

        public void setFechaHoraSalidaLlegada(String fechaHoraSalidaLlegada) {
            this.fechaHoraSalidaLlegada = fechaHoraSalidaLlegada;
        }

        public String getTipoEstacion() {
            return tipoEstacion;
        }

        public void setTipoEstacion(String tipoEstacion) {
            this.tipoEstacion = tipoEstacion;
        }

        public String getDistanciaRecorrida() {
            return distanciaRecorrida;
        }

        public void setDistanciaRecorrida(String distanciaRecorrida) {
            this.distanciaRecorrida = distanciaRecorrida;
        }

        public Domicilio getDomicilio() {
            return domicilio;
        }

        public void setDomicilio(Domicilio domicilio) {
            this.domicilio = domicilio;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Domicilio {
        private String calle;
        private String numeroExterior;
        private String numeroInterior;
        private String colonia;
        private String localidad;
        private String referencia;
        private String municipio;
        private String estado;
        private String pais;
        private String codigoPostal;

        public String getCalle() {
            return calle;
        }

        public void setCalle(String calle) {
            this.calle = calle;
        }

        public String getNumeroExterior() {
            return numeroExterior;
        }

        public void setNumeroExterior(String numeroExterior) {
            this.numeroExterior = numeroExterior;
        }

        public String getNumeroInterior() {
            return numeroInterior;
        }

        public void setNumeroInterior(String numeroInterior) {
            this.numeroInterior = numeroInterior;
        }

        public String getColonia() {
            return colonia;
        }

        public void setColonia(String colonia) {
            this.colonia = colonia;
        }

        public String getLocalidad() {
            return localidad;
        }

        public void setLocalidad(String localidad) {
            this.localidad = localidad;
        }

        public String getReferencia() {
            return referencia;
        }

        public void setReferencia(String referencia) {
            this.referencia = referencia;
        }

        public String getMunicipio() {
            return municipio;
        }

        public void setMunicipio(String municipio) {
            this.municipio = municipio;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public String getPais() {
            return pais;
        }

        public void setPais(String pais) {
            this.pais = pais;
        }

        public String getCodigoPostal() {
            return codigoPostal;
        }

        public void setCodigoPostal(String codigoPostal) {
            this.codigoPostal = codigoPostal;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Mercancias {
        private String pesoBrutoTotal;
        private String unidadPeso;
        private String pesoNetoTotal;
        private String numTotalMercancias;
        private String cargoPorTasacion;
        private String logisticaInversaRecoleccionDevolucion;
        private List<Mercancia> mercancias;
        private Autotransporte autotransporte;
        private TransporteFerroviario transporteFerroviario;

        public String getPesoBrutoTotal() {
            return pesoBrutoTotal;
        }

        public void setPesoBrutoTotal(String pesoBrutoTotal) {
            this.pesoBrutoTotal = pesoBrutoTotal;
        }

        public String getUnidadPeso() {
            return unidadPeso;
        }

        public void setUnidadPeso(String unidadPeso) {
            this.unidadPeso = unidadPeso;
        }

        public String getPesoNetoTotal() {
            return pesoNetoTotal;
        }

        public void setPesoNetoTotal(String pesoNetoTotal) {
            this.pesoNetoTotal = pesoNetoTotal;
        }

        public String getNumTotalMercancias() {
            return numTotalMercancias;
        }

        public void setNumTotalMercancias(String numTotalMercancias) {
            this.numTotalMercancias = numTotalMercancias;
        }

        public String getCargoPorTasacion() {
            return cargoPorTasacion;
        }

        public void setCargoPorTasacion(String cargoPorTasacion) {
            this.cargoPorTasacion = cargoPorTasacion;
        }

        public String getLogisticaInversaRecoleccionDevolucion() {
            return logisticaInversaRecoleccionDevolucion;
        }

        public void setLogisticaInversaRecoleccionDevolucion(String logisticaInversaRecoleccionDevolucion) {
            this.logisticaInversaRecoleccionDevolucion = logisticaInversaRecoleccionDevolucion;
        }

        public List<Mercancia> getMercancias() {
            return mercancias;
        }

        public void setMercancias(List<Mercancia> mercancias) {
            this.mercancias = mercancias;
        }

        public Autotransporte getAutotransporte() {
            return autotransporte;
        }

        public void setAutotransporte(Autotransporte autotransporte) {
            this.autotransporte = autotransporte;
        }

        public TransporteFerroviario getTransporteFerroviario() {
            return transporteFerroviario;
        }

        public void setTransporteFerroviario(TransporteFerroviario transporteFerroviario) {
            this.transporteFerroviario = transporteFerroviario;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Mercancia {
        private String bienesTransp;
        private String claveSTCC;
        private String descripcion;
        private String cantidad;
        private String claveUnidad;
        private String unidad;
        private String dimensiones;
        private String materialPeligroso;
        private String cveMaterialPeligroso;
        private String embalaje;
        private String descripEmbalaje;
        private String pesoEnKg;
        private String valorMercancia;
        private String moneda;
        private String fraccionArancelaria;
        private String uuidComercioExt;

        public String getBienesTransp() {
            return bienesTransp;
        }

        public void setBienesTransp(String bienesTransp) {
            this.bienesTransp = bienesTransp;
        }

        public String getClaveSTCC() {
            return claveSTCC;
        }

        public void setClaveSTCC(String claveSTCC) {
            this.claveSTCC = claveSTCC;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getCantidad() {
            return cantidad;
        }

        public void setCantidad(String cantidad) {
            this.cantidad = cantidad;
        }

        public String getClaveUnidad() {
            return claveUnidad;
        }

        public void setClaveUnidad(String claveUnidad) {
            this.claveUnidad = claveUnidad;
        }

        public String getUnidad() {
            return unidad;
        }

        public void setUnidad(String unidad) {
            this.unidad = unidad;
        }

        public String getDimensiones() {
            return dimensiones;
        }

        public void setDimensiones(String dimensiones) {
            this.dimensiones = dimensiones;
        }

        public String getMaterialPeligroso() {
            return materialPeligroso;
        }

        public void setMaterialPeligroso(String materialPeligroso) {
            this.materialPeligroso = materialPeligroso;
        }

        public String getCveMaterialPeligroso() {
            return cveMaterialPeligroso;
        }

        public void setCveMaterialPeligroso(String cveMaterialPeligroso) {
            this.cveMaterialPeligroso = cveMaterialPeligroso;
        }

        public String getEmbalaje() {
            return embalaje;
        }

        public void setEmbalaje(String embalaje) {
            this.embalaje = embalaje;
        }

        public String getDescripEmbalaje() {
            return descripEmbalaje;
        }

        public void setDescripEmbalaje(String descripEmbalaje) {
            this.descripEmbalaje = descripEmbalaje;
        }

        public String getPesoEnKg() {
            return pesoEnKg;
        }

        public void setPesoEnKg(String pesoEnKg) {
            this.pesoEnKg = pesoEnKg;
        }

        public String getValorMercancia() {
            return valorMercancia;
        }

        public void setValorMercancia(String valorMercancia) {
            this.valorMercancia = valorMercancia;
        }

        public String getMoneda() {
            return moneda;
        }

        public void setMoneda(String moneda) {
            this.moneda = moneda;
        }

        public String getFraccionArancelaria() {
            return fraccionArancelaria;
        }

        public void setFraccionArancelaria(String fraccionArancelaria) {
            this.fraccionArancelaria = fraccionArancelaria;
        }

        public String getUuidComercioExt() {
            return uuidComercioExt;
        }

        public void setUuidComercioExt(String uuidComercioExt) {
            this.uuidComercioExt = uuidComercioExt;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Autotransporte {
        private String permSct;
        private String numPermisoSct;
        private IdentificacionVehicular identificacionVehicular;
        private Seguros seguros;
        private List<Remolque> remolques;

        public String getPermSct() {
            return permSct;
        }

        public void setPermSct(String permSct) {
            this.permSct = permSct;
        }

        public String getNumPermisoSct() {
            return numPermisoSct;
        }

        public void setNumPermisoSct(String numPermisoSct) {
            this.numPermisoSct = numPermisoSct;
        }

        public IdentificacionVehicular getIdentificacionVehicular() {
            return identificacionVehicular;
        }

        public void setIdentificacionVehicular(IdentificacionVehicular identificacionVehicular) {
            this.identificacionVehicular = identificacionVehicular;
        }

        public Seguros getSeguros() {
            return seguros;
        }

        public void setSeguros(Seguros seguros) {
            this.seguros = seguros;
        }

        public List<Remolque> getRemolques() {
            return remolques;
        }

        public void setRemolques(List<Remolque> remolques) {
            this.remolques = remolques;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IdentificacionVehicular {
        private String configVehicular;
        private String pesoBrutoVehicular;
        private String placaVm;
        private String anioModeloVm;

        public String getConfigVehicular() {
            return configVehicular;
        }

        public void setConfigVehicular(String configVehicular) {
            this.configVehicular = configVehicular;
        }

        public String getPesoBrutoVehicular() {
            return pesoBrutoVehicular;
        }

        public void setPesoBrutoVehicular(String pesoBrutoVehicular) {
            this.pesoBrutoVehicular = pesoBrutoVehicular;
        }

        public String getPlacaVm() {
            return placaVm;
        }

        public void setPlacaVm(String placaVm) {
            this.placaVm = placaVm;
        }

        public String getAnioModeloVm() {
            return anioModeloVm;
        }

        public void setAnioModeloVm(String anioModeloVm) {
            this.anioModeloVm = anioModeloVm;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Seguros {
        private String aseguraRespCivil;
        private String polizaRespCivil;
        private String aseguraMedAmbiente;
        private String polizaMedAmbiente;
        private String aseguraCarga;
        private String polizaCarga;
        private String primaSeguro;

        public String getAseguraRespCivil() {
            return aseguraRespCivil;
        }

        public void setAseguraRespCivil(String aseguraRespCivil) {
            this.aseguraRespCivil = aseguraRespCivil;
        }

        public String getPolizaRespCivil() {
            return polizaRespCivil;
        }

        public void setPolizaRespCivil(String polizaRespCivil) {
            this.polizaRespCivil = polizaRespCivil;
        }

        public String getAseguraMedAmbiente() {
            return aseguraMedAmbiente;
        }

        public void setAseguraMedAmbiente(String aseguraMedAmbiente) {
            this.aseguraMedAmbiente = aseguraMedAmbiente;
        }

        public String getPolizaMedAmbiente() {
            return polizaMedAmbiente;
        }

        public void setPolizaMedAmbiente(String polizaMedAmbiente) {
            this.polizaMedAmbiente = polizaMedAmbiente;
        }

        public String getAseguraCarga() {
            return aseguraCarga;
        }

        public void setAseguraCarga(String aseguraCarga) {
            this.aseguraCarga = aseguraCarga;
        }

        public String getPolizaCarga() {
            return polizaCarga;
        }

        public void setPolizaCarga(String polizaCarga) {
            this.polizaCarga = polizaCarga;
        }

        public String getPrimaSeguro() {
            return primaSeguro;
        }

        public void setPrimaSeguro(String primaSeguro) {
            this.primaSeguro = primaSeguro;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Remolque {
        private String subTipoRem;
        private String placa;

        public String getSubTipoRem() {
            return subTipoRem;
        }

        public void setSubTipoRem(String subTipoRem) {
            this.subTipoRem = subTipoRem;
        }

        public String getPlaca() {
            return placa;
        }

        public void setPlaca(String placa) {
            this.placa = placa;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TransporteFerroviario {
        private String tipoDeServicio;
        private String tipoDeTrafico;
        private String nombreAseg;
        private String numPolizaSeguro;
        private List<DerechosDePaso> derechosDePaso;
        private List<Carro> carros;

        public String getTipoDeServicio() {
            return tipoDeServicio;
        }

        public void setTipoDeServicio(String tipoDeServicio) {
            this.tipoDeServicio = tipoDeServicio;
        }

        public String getTipoDeTrafico() {
            return tipoDeTrafico;
        }

        public void setTipoDeTrafico(String tipoDeTrafico) {
            this.tipoDeTrafico = tipoDeTrafico;
        }

        public String getNombreAseg() {
            return nombreAseg;
        }

        public void setNombreAseg(String nombreAseg) {
            this.nombreAseg = nombreAseg;
        }

        public String getNumPolizaSeguro() {
            return numPolizaSeguro;
        }

        public void setNumPolizaSeguro(String numPolizaSeguro) {
            this.numPolizaSeguro = numPolizaSeguro;
        }

        public List<DerechosDePaso> getDerechosDePaso() {
            return derechosDePaso;
        }

        public void setDerechosDePaso(List<DerechosDePaso> derechosDePaso) {
            this.derechosDePaso = derechosDePaso;
        }

        public List<Carro> getCarros() {
            return carros;
        }

        public void setCarros(List<Carro> carros) {
            this.carros = carros;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DerechosDePaso {
        private String tipoDerechoDePaso;
        private String kilometrajePagado;

        public String getTipoDerechoDePaso() {
            return tipoDerechoDePaso;
        }

        public void setTipoDerechoDePaso(String tipoDerechoDePaso) {
            this.tipoDerechoDePaso = tipoDerechoDePaso;
        }

        public String getKilometrajePagado() {
            return kilometrajePagado;
        }

        public void setKilometrajePagado(String kilometrajePagado) {
            this.kilometrajePagado = kilometrajePagado;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Carro {
        private String tipoCarro;
        private String matriculaCarro;
        private String guiaCarro;
        private String toneladasNetasCarro;

        public String getTipoCarro() {
            return tipoCarro;
        }

        public void setTipoCarro(String tipoCarro) {
            this.tipoCarro = tipoCarro;
        }

        public String getMatriculaCarro() {
            return matriculaCarro;
        }

        public void setMatriculaCarro(String matriculaCarro) {
            this.matriculaCarro = matriculaCarro;
        }

        public String getGuiaCarro() {
            return guiaCarro;
        }

        public void setGuiaCarro(String guiaCarro) {
            this.guiaCarro = guiaCarro;
        }

        public String getToneladasNetasCarro() {
            return toneladasNetasCarro;
        }

        public void setToneladasNetasCarro(String toneladasNetasCarro) {
            this.toneladasNetasCarro = toneladasNetasCarro;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FiguraTransporte {
        private List<TipoFigura> tiposFigura;

        public List<TipoFigura> getTiposFigura() {
            return tiposFigura;
        }

        public void setTiposFigura(List<TipoFigura> tiposFigura) {
            this.tiposFigura = tiposFigura;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TipoFigura {
        private String tipoFigura;
        private String rfcFigura;
        private String numLicencia;
        private String nombreFigura;
        private String numRegIdTribFigura;
        private String residenciaFiscalFigura;
        private List<ParteTransporte> partesTransporte;
        private Domicilio domicilio;

        public String getTipoFigura() {
            return tipoFigura;
        }

        public void setTipoFigura(String tipoFigura) {
            this.tipoFigura = tipoFigura;
        }

        public String getRfcFigura() {
            return rfcFigura;
        }

        public void setRfcFigura(String rfcFigura) {
            this.rfcFigura = rfcFigura;
        }

        public String getNumLicencia() {
            return numLicencia;
        }

        public void setNumLicencia(String numLicencia) {
            this.numLicencia = numLicencia;
        }

        public String getNombreFigura() {
            return nombreFigura;
        }

        public void setNombreFigura(String nombreFigura) {
            this.nombreFigura = nombreFigura;
        }

        public String getNumRegIdTribFigura() {
            return numRegIdTribFigura;
        }

        public void setNumRegIdTribFigura(String numRegIdTribFigura) {
            this.numRegIdTribFigura = numRegIdTribFigura;
        }

        public String getResidenciaFiscalFigura() {
            return residenciaFiscalFigura;
        }

        public void setResidenciaFiscalFigura(String residenciaFiscalFigura) {
            this.residenciaFiscalFigura = residenciaFiscalFigura;
        }

        public List<ParteTransporte> getPartesTransporte() {
            return partesTransporte;
        }

        public void setPartesTransporte(List<ParteTransporte> partesTransporte) {
            this.partesTransporte = partesTransporte;
        }

        public Domicilio getDomicilio() {
            return domicilio;
        }

        public void setDomicilio(Domicilio domicilio) {
            this.domicilio = domicilio;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParteTransporte {
        private String parteTransporte;

        public String getParteTransporte() {
            return parteTransporte;
        }

        public void setParteTransporte(String parteTransporte) {
            this.parteTransporte = parteTransporte;
        }
    }
}

