package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dao.CartaPorteDAO;
import com.cibercom.facturacion_back.dao.UuidFacturaOracleDAO;
import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoRequest;
import com.cibercom.facturacion_back.dto.PacTimbradoResponse;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Autotransporte;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.FiguraTransporte;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Mercancia;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Mercancias;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.TipoFigura;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.TransporteFerroviario;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Ubicacion;
import com.cibercom.facturacion_back.integration.PacClient;
import com.cibercom.facturacion_back.model.ClienteCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("oracle")
public class CartaPorteService {

    private static final Logger logger = LoggerFactory.getLogger(CartaPorteService.class);
    private static final String SERIE_CP = "CP";

    private final CartaPorteDAO cartaPorteDAO;
    private final UuidFacturaOracleDAO uuidFacturaOracleDAO;
    private final ClienteCatalogoService clienteCatalogoService;
    private final CartaPorteXmlBuilder cartaPorteXmlBuilder;
    private final CartaPorteXmlSanitizer cartaPorteXmlSanitizer;
    private final PacClient pacClient;

    @Value("${facturacion.emisor.rfc:XAXX010101000}")
    private String rfcEmisorDefault;

    @Value("${facturacion.emisor.nombre:EMISOR DEFAULT}")
    private String nombreEmisorDefault;

    @Value("${facturacion.emisor.regimen:601}")
    private String regimenEmisorDefault;

    @Value("${facturacion.emisor.cp:58000}")
    private String cpEmisorDefault;

    public CartaPorteService(CartaPorteDAO cartaPorteDAO,
                             UuidFacturaOracleDAO uuidFacturaOracleDAO,
                             ClienteCatalogoService clienteCatalogoService,
                             CartaPorteXmlBuilder cartaPorteXmlBuilder,
                             CartaPorteXmlSanitizer cartaPorteXmlSanitizer,
                             PacClient pacClient) {
        this.cartaPorteDAO = cartaPorteDAO;
        this.uuidFacturaOracleDAO = uuidFacturaOracleDAO;
        this.clienteCatalogoService = clienteCatalogoService;
        this.cartaPorteXmlBuilder = cartaPorteXmlBuilder;
        this.cartaPorteXmlSanitizer = cartaPorteXmlSanitizer;
        this.pacClient = pacClient;
    }

    public SaveResult guardar(CartaPorteSaveRequest request) {
        logger.info("Procesando solicitud de Carta Porte para RFC: {}", request.getRfcCompleto());
        normalizeRequest(request);

        BigDecimal subtotal = parsePrecio(request.getPrecio()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subtotal.multiply(new BigDecimal("0.16")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

        String rfcReceptor = request.getRfcCompleto();
        Long idReceptor = resolverIdReceptorPorRfc(rfcReceptor, request.getRazonSocial());

        String uuidFactura = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String folio = request.getNumeroSerie();
        if (folio == null || folio.isBlank()) {
            folio = String.valueOf(System.currentTimeMillis());
            request.setNumeroSerie(folio);
        }

        boolean okFactura = uuidFacturaOracleDAO.insertarBasicoConIdReceptor(
                uuidFactura,
                null,
                SERIE_CP,
                folio,
                subtotal,
                iva,
                BigDecimal.ZERO,
                total,
                "99",
                request.getUsoCfdi(),
                "EN_CAPTURA",
                "Carta Porte registrada desde frontend",
                "PUE",
                rfcReceptor,
                rfcEmisorDefault,
                null,
                idReceptor,
                Integer.valueOf(3)
        );

        if (!okFactura) {
            String err = uuidFacturaOracleDAO.getLastInsertError();
            throw new RuntimeException(err != null && !err.isBlank()
                    ? err
                    : "No se pudo insertar registro en FACTURAS");
        }

        try {
            Long idGenerado = cartaPorteDAO.insertar(request);
            PacTimbradoResponse pacResponse = timbrarConFinkok(request, folio, uuidFactura, total);
            return new SaveResult(idGenerado,
                    pacResponse != null && pacResponse.getUuid() != null ? pacResponse.getUuid() : uuidFactura,
                    pacResponse);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al guardar carta porte: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar carta porte: " + e.getMessage(), e);
        }
    }

    public String renderXml(CartaPorteSaveRequest request) {
        normalizeRequest(request);
        return buildXml(request);
    }

    private PacTimbradoResponse timbrarConFinkok(CartaPorteSaveRequest request,
                                                String folio,
                                                String uuidFactura,
                                                BigDecimal total) {
        try {
            String xml = buildXml(request);
            guardarXmlSanitizadoEnArchivo(xml);

            PacTimbradoRequest pacReq = PacTimbradoRequest.builder()
                    .uuid(uuidFactura)
                    .xmlContent(xml)
                    .rfcEmisor(rfcEmisorDefault)
                    .rfcReceptor(request.getRfcCompleto())
                    .total(total.doubleValue())
                    .tipo("CARTAPORTE")
                    .fechaFactura(LocalDateTime.now().toString())
                    .publicoGeneral(false)
                    .serie(SERIE_CP)
                    .folio(folio)
                    .medioPago("PUE")
                    .formaPago("99")
                    .usoCFDI(request.getUsoCfdi())
                    .regimenFiscalEmisor(regimenEmisorDefault)
                    .regimenFiscalReceptor(request.getRegimenFiscal())
                    .build();

            PacTimbradoResponse resp = pacClient.solicitarTimbrado(pacReq);
            if (resp == null || Boolean.FALSE.equals(resp.getOk())) {
                String mensaje = resp != null && resp.getMessage() != null ? resp.getMessage() : "PAC no disponible";
                throw new RuntimeException("Error al timbrar Carta Porte: " + mensaje);
            }

            logger.info("Carta Porte timbrada exitosamente. UUID: {}", resp.getUuid());
            return resp;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Fallo inesperado al timbrar Carta Porte: {}", e.getMessage(), e);
            throw new RuntimeException("Fallo al timbrar Carta Porte: " + e.getMessage(), e);
        }
    }

    private void normalizeRequest(CartaPorteSaveRequest request) {
        validarCamposBasicos(request);
        CartaPorteComplement complemento = request.getComplemento();
        if (complemento == null) {
            throw new IllegalArgumentException("El complemento Carta Porte es requerido");
        }
        aplicarReglasComplemento(complemento);
        validarComplemento(complemento);
        mapearCamposLegacy(request);
    }

    private void validarCamposBasicos(CartaPorteSaveRequest request) {
        if (isBlank(request.getRfcCompleto())) {
            throw new IllegalArgumentException("RFC es requerido");
        }
        if (isBlank(request.getCorreoElectronico())) {
            throw new IllegalArgumentException("Correo electrónico es requerido");
        }
        boolean esFisica = "fisica".equalsIgnoreCase(request.getTipoPersona());
        if (esFisica) {
            if (isBlank(request.getNombre())) {
                throw new IllegalArgumentException("Nombre es requerido");
            }
            if (isBlank(request.getPaterno())) {
                throw new IllegalArgumentException("Apellido paterno es requerido");
            }
        } else if (isBlank(request.getRazonSocial())) {
            throw new IllegalArgumentException("Razón social es requerida");
        }
        if (isBlank(request.getDomicilioFiscal())) {
            throw new IllegalArgumentException("Domicilio fiscal es requerido");
        }
        if (isBlank(request.getRegimenFiscal())) {
            throw new IllegalArgumentException("Régimen fiscal es requerido");
        }
        if (isBlank(request.getUsoCfdi())) {
            throw new IllegalArgumentException("Uso CFDI es requerido");
        }
        if (isBlank(request.getDescripcion())) {
            throw new IllegalArgumentException("Descripción es requerida");
        }
        if (isBlank(request.getNumeroSerie())) {
            throw new IllegalArgumentException("Número de serie es requerido");
        } else {
            request.setNumeroSerie(request.getNumeroSerie().trim());
        }
        if (isBlank(request.getPrecio())) {
            throw new IllegalArgumentException("Precio es requerido");
        }
        if (isBlank(request.getPersonaAutoriza())) {
            throw new IllegalArgumentException("Persona que autoriza es requerida");
        }
        if (isBlank(request.getPuesto())) {
            throw new IllegalArgumentException("Puesto es requerido");
        }
    }

    private void validarComplemento(CartaPorteComplement complemento) {
        List<Ubicacion> ubicaciones = complemento.getUbicaciones();
        if (ubicaciones == null || ubicaciones.size() < 2) {
            throw new IllegalArgumentException("Capture al menos una ubicación de origen y una de destino");
        }
        for (Ubicacion u : ubicaciones) {
            if (isBlank(u.getTipoUbicacion())) {
                throw new IllegalArgumentException("Tipo de ubicación es requerido");
            }
            if (isBlank(u.getRfcRemitenteDestinatario())) {
                throw new IllegalArgumentException("El RFC del remitente/destinatario es requerido");
            }
            if (isBlank(u.getFechaHoraSalidaLlegada())) {
                throw new IllegalArgumentException("La fecha y hora de la ubicación " + u.getTipoUbicacion() + " es requerida");
            }
        }

        Mercancias mercancias = complemento.getMercancias();
        if (mercancias == null) {
            throw new IllegalArgumentException("El bloque de Mercancías es requerido");
        }
        if (mercancias.getMercancias() == null || mercancias.getMercancias().isEmpty()) {
            throw new IllegalArgumentException("Debe capturar al menos una mercancía");
        }
        for (Mercancia mercancia : mercancias.getMercancias()) {
            if (isBlank(mercancia.getBienesTransp())) {
                throw new IllegalArgumentException("La clave BienesTransp de la mercancía es requerida");
            }
            if (isBlank(mercancia.getDescripcion())) {
                throw new IllegalArgumentException("La descripción de la mercancía es requerida");
            }
            if (isBlank(mercancia.getPesoEnKg())) {
                throw new IllegalArgumentException("El peso en KG de la mercancía es requerido");
            }
        }
        if (mercancias.getAutotransporte() == null && mercancias.getTransporteFerroviario() == null) {
            throw new IllegalArgumentException("Debe capturar Autotransporte o Transporte Ferroviario");
        }
        if (mercancias.getAutotransporte() != null) {
            Autotransporte auto = mercancias.getAutotransporte();
            if (auto.getIdentificacionVehicular() == null || isBlank(auto.getIdentificacionVehicular().getConfigVehicular())) {
                throw new IllegalArgumentException("ConfigVehicular es requerida para Autotransporte");
            }
            if (auto.getSeguros() == null || isBlank(auto.getSeguros().getAseguraRespCivil())
                    || isBlank(auto.getSeguros().getPolizaRespCivil())) {
                throw new IllegalArgumentException("Los datos de seguros son requeridos para Autotransporte");
            }
        }
        if (mercancias.getTransporteFerroviario() != null) {
            TransporteFerroviario ferro = mercancias.getTransporteFerroviario();
            if (ferro.getCarros() == null || ferro.getCarros().isEmpty()) {
                throw new IllegalArgumentException("Capture al menos un carro para Transporte Ferroviario");
            }
        }

        FiguraTransporte figura = complemento.getFiguraTransporte();
        if (figura == null || figura.getTiposFigura() == null || figura.getTiposFigura().isEmpty()) {
            throw new IllegalArgumentException("Debe capturar al menos una Figura de Transporte");
        }
        for (TipoFigura tipoFigura : figura.getTiposFigura()) {
            if (isBlank(tipoFigura.getTipoFigura())) {
                throw new IllegalArgumentException("El TipoFigura es requerido");
            }
            if (isBlank(tipoFigura.getNombreFigura())) {
                throw new IllegalArgumentException("El nombre de la Figura de Transporte es requerido");
            }
        }
    }

    private void aplicarReglasComplemento(CartaPorteComplement complemento) {
        List<Ubicacion> ubicaciones = complemento.getUbicaciones();
        if (ubicaciones == null) {
            return;
        }
        boolean tieneFerro = complemento.getMercancias() != null && complemento.getMercancias().getTransporteFerroviario() != null;
        for (Ubicacion ubicacion : ubicaciones) {
            if (ubicacion == null) continue;
            ubicacion.setFechaHoraSalidaLlegada(normalizeDateTime(ubicacion.getFechaHoraSalidaLlegada()));
            String tipoEstacionNormalizada = normalizeTipoEstacion(ubicacion.getTipoEstacion());
            ubicacion.setTipoEstacion(tipoEstacionNormalizada);
            if (tieneFerro && "02".equals(tipoEstacionNormalizada)) {
                ubicacion.setDomicilio(null);
            }
        }
    }

    private String normalizeTipoEstacion(String tipoEstacion) {
        if (tipoEstacion == null) {
            return null;
        }
        String trimmed = tipoEstacion.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.matches("\\d+")) {
            try {
                int value = Integer.parseInt(trimmed);
                return String.format("%02d", value);
            } catch (NumberFormatException e) {
                return trimmed;
            }
        }
        return trimmed;
    }

    private void mapearCamposLegacy(CartaPorteSaveRequest request) {
        CartaPorteComplement complemento = request.getComplemento();
        Mercancias mercancias = complemento.getMercancias();
        Autotransporte auto = mercancias != null ? mercancias.getAutotransporte() : null;
        TransporteFerroviario ferro = mercancias != null ? mercancias.getTransporteFerroviario() : null;

        if (isBlank(request.getRazonSocial())) {
            if ("fisica".equalsIgnoreCase(request.getTipoPersona())) {
                String nombreCompleto = String.join(" ",
                        firstNonBlank(request.getNombre(), ""),
                        firstNonBlank(request.getPaterno(), ""),
                        firstNonBlank(request.getMaterno(), "")).trim();
                if (!nombreCompleto.isBlank()) {
                    request.setRazonSocial(nombreCompleto);
                }
            }
            if (isBlank(request.getRazonSocial())) {
                request.setRazonSocial(firstNonBlank(request.getNombre(), request.getRfcCompleto()));
            }
        }

        if (isBlank(request.getTipoTransporte())) {
            if (ferro != null) {
                request.setTipoTransporte("04");
            } else {
                request.setTipoTransporte("01");
            }
        }

        if (auto != null) {
            request.setPermisoSCT(firstNonBlank(auto.getPermSct(), request.getPermisoSCT()));
            request.setNumeroPermisoSCT(firstNonBlank(auto.getNumPermisoSct(), request.getNumeroPermisoSCT()));
            if (auto.getIdentificacionVehicular() != null) {
                request.setConfigVehicular(firstNonBlank(auto.getIdentificacionVehicular().getConfigVehicular(), request.getConfigVehicular()));
                request.setPlacasVehiculo(firstNonBlank(auto.getIdentificacionVehicular().getPlacaVm(), request.getPlacasVehiculo()));
            }
        }

        FiguraTransporte figura = complemento.getFiguraTransporte();
        if (figura != null && figura.getTiposFigura() != null && !figura.getTiposFigura().isEmpty()) {
            TipoFigura tipoFigura = figura.getTiposFigura().get(0);
            request.setNombreTransportista(firstNonBlank(tipoFigura.getNombreFigura(), request.getNombreTransportista()));
            request.setRfcTransportista(firstNonBlank(tipoFigura.getRfcFigura(), request.getRfcTransportista()));
        }

        if (mercancias != null && mercancias.getMercancias() != null && !mercancias.getMercancias().isEmpty()) {
            Mercancia mercancia = mercancias.getMercancias().get(0);
            String resumen = String.format("Clave:%s Desc:%s Cant:%s Peso:%s",
                    firstNonBlank(mercancia.getBienesTransp(), "N/A"),
                    firstNonBlank(mercancia.getDescripcion(), "N/A"),
                    firstNonBlank(mercancia.getCantidad(), "1"),
                    firstNonBlank(mercancia.getPesoEnKg(), "0"));
            request.setBienesTransportados(resumen);
        }

        Ubicacion origen = encontrarUbicacion(complemento, "Origen");
        Ubicacion destino = encontrarUbicacion(complemento, "Destino");
        request.setOrigen(firstNonBlank(request.getOrigen(), resumenUbicacion(origen)));
        request.setDestino(firstNonBlank(request.getDestino(), resumenUbicacion(destino)));
        request.setFechaSalida(firstNonBlank(request.getFechaSalida(), origen != null ? origen.getFechaHoraSalidaLlegada() : null));
        request.setFechaLlegada(firstNonBlank(request.getFechaLlegada(), destino != null ? destino.getFechaHoraSalidaLlegada() : null));
        if (destino != null && !isBlank(destino.getDistanciaRecorrida())) {
            request.setDistanciaRecorrida(destino.getDistanciaRecorrida());
        }
    }

    private String buildXml(CartaPorteSaveRequest request) {
        String xml = cartaPorteXmlBuilder.construirXml(
                request,
                rfcEmisorDefault,
                nombreEmisorDefault,
                regimenEmisorDefault,
                cpEmisorDefault
        );
        return cartaPorteXmlSanitizer.sanitizeUbicacionDomicilioForFerroviario(xml);
    }

    private BigDecimal parsePrecio(String precio) {
        if (precio == null || precio.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(precio.replaceAll("[,\\s]", ""));
        } catch (NumberFormatException e) {
            logger.warn("Error al parsear precio '{}', usando 0.00", precio);
            return BigDecimal.ZERO;
        }
    }

    private Long resolverIdReceptorPorRfc(String rfc, String razonSocial) {
        if (rfc == null || rfc.trim().isEmpty()) {
            return null;
        }
        String normalized = rfc.trim().toUpperCase();
        Optional<ClienteCatalogo> existente = clienteCatalogoService.buscarPorRfc(normalized);
        if (existente.isPresent()) {
            return existente.get().getIdCliente();
        }
        ClienteCatalogo nuevo = new ClienteCatalogo();
        nuevo.setRfc(normalized);
        nuevo.setRazonSocial((razonSocial != null && !razonSocial.trim().isEmpty()) ? razonSocial.trim() : normalized);
        ClienteCatalogo guardado = clienteCatalogoService.guardar(nuevo);
        return guardado != null ? guardado.getIdCliente() : null;
    }

    private Ubicacion encontrarUbicacion(CartaPorteComplement complemento, String tipo) {
        if (complemento == null || complemento.getUbicaciones() == null) {
            return null;
        }
        return complemento.getUbicaciones().stream()
                .filter(u -> u.getTipoUbicacion() != null && u.getTipoUbicacion().equalsIgnoreCase(tipo))
                .findFirst()
                .orElse(null);
    }

    private String resumenUbicacion(Ubicacion ubicacion) {
        if (ubicacion == null) {
            return null;
        }
        if (ubicacion.getDomicilio() != null) {
            StringBuilder sb = new StringBuilder();
            appendSegment(sb, ubicacion.getDomicilio().getCalle());
            appendSegment(sb, ubicacion.getDomicilio().getMunicipio());
            appendSegment(sb, ubicacion.getDomicilio().getEstado());
            appendSegment(sb, ubicacion.getDomicilio().getCodigoPostal());
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return firstNonBlank(ubicacion.getNombreRemitenteDestinatario(), ubicacion.getRfcRemitenteDestinatario());
    }

    private void appendSegment(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(value.trim());
    }

    private String normalizeDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 16) {
            return trimmed + ":00";
        }
        return trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class SaveResult {
        private final Long cartaPorteId;
        private final String uuidTimbrado;
        private final PacTimbradoResponse pacResponse;

        public SaveResult(Long cartaPorteId, String uuidTimbrado, PacTimbradoResponse pacResponse) {
            this.cartaPorteId = cartaPorteId;
            this.uuidTimbrado = uuidTimbrado;
            this.pacResponse = pacResponse;
        }

        public Long getCartaPorteId() {
            return cartaPorteId;
        }

        public String getUuidTimbrado() {
            return uuidTimbrado;
        }

        public PacTimbradoResponse getPacResponse() {
            return pacResponse;
        }
    }

    private void guardarXmlSanitizadoEnArchivo(String xmlSanitizado) {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "cfdi_xml_sanitizados");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path archivo = tempDir.resolve("carta_porte_sanitizado_" + timestamp + ".xml");
            Files.write(archivo, xmlSanitizado.getBytes(StandardCharsets.UTF_8));

            logger.info("XML sanitizado guardado en {}", archivo.toAbsolutePath());
        } catch (Exception e) {
            logger.warn("No se pudo guardar el XML sanitizado: {}", e.getMessage());
        }
    }
}