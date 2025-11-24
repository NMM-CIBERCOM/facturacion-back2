package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.CreditNoteSaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Construye el XML CFDI 4.0 para notas de crédito (Tipo E) antes de ser firmado y timbrado.
 */
@Component
public class CreditNoteXmlBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CreditNoteXmlBuilder.class);
    private static final ZoneId ZONE_MEXICO = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter CFDI_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Pattern CP_PATTERN = Pattern.compile("\\b(\\d{5})\\b");
    private static final String CFDI_NAMESPACE = "http://www.sat.gob.mx/cfd/4";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String CFDI_SCHEMA = "http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd";
    private static final String DEFAULT_CLAVE_PROD_SERV = "84111506";
    private static final String DEFAULT_CLAVE_UNIDAD = "E48";

    @Value("${facturacion.emisor.rfc:EEM123456789}")
    private String emisorRfc;

    @Value("${facturacion.emisor.nombre:INNOVACION VALOR Y DESARROLLO SA}")
    private String emisorNombre;

    @Value("${facturacion.emisor.regimen:601}")
    private String emisorRegimen;

    @Value("${facturacion.emisor.cp:58000}")
    private String emisorCp;
    
    // Valor hardcodeado como respaldo para evitar "00000"
    private static final String CP_DEFAULT = "58000";

    @Value("${facturacion.creditnote.serie:NC}")
    private String defaultSerie;

    @Value("${facturacion.ivaTasa:0.16}")
    private BigDecimal defaultIvaTasa;

    @jakarta.annotation.PostConstruct
    public void init() {
        // Validar y corregir el código postal al inicializar
        logger.info("CreditNoteXmlBuilder.init() - emisorCp desde @Value ANTES de validación: '{}'", emisorCp);
        if (emisorCp == null || emisorCp.trim().isEmpty() || "00000".equals(emisorCp.trim())) {
            logger.warn("⚠️ Código postal del emisor inválido o '00000' detectado. Corrigiendo a '58000'");
            emisorCp = "58000";
        }
        logger.info("✓ CreditNoteXmlBuilder inicializado - emisorCp FINAL: '{}'", emisorCp);
    }

    public String buildXml(CreditNoteSaveRequest request) {
        String serie = valueOrDefault(request.getSerieNc(), defaultSerie);
        String folio = valueOrDefault(request.getFolioNc(), "1");
        
        // CRÍTICO: La fecha debe estar en hora local de México (UTC-6 o UTC-5 según horario de verano)
        // El SAT rechaza fechas fuera de rango (error CFDI401)
        // IMPORTANTE: SIEMPRE usar la hora actual de México para evitar problemas de zona horaria
        // SOLUCIÓN: Usar ZonedDateTime directamente y formatearlo sin convertir a LocalDateTime
        // para mantener la zona horaria correcta
        ZonedDateTime ahoraMexico = ZonedDateTime.now(ZONE_MEXICO);
        LocalDateTime fecha = ahoraMexico.toLocalDateTime();
        
        // Log para depuración con información completa de zona horaria
        if (request.getFechaEmision() != null) {
            LocalDateTime fechaRequest = request.getFechaEmision();
            logger.info("⚠️ Fecha de emisión del request ignorada ({}) para evitar error CFDI401. " +
                    "Usando hora actual de México: {} (ZonedDateTime: {})", 
                    fechaRequest, fecha, ahoraMexico);
        } else {
            logger.info("✓ Fecha de emisión generada en zona horaria de México: {} (ZonedDateTime: {})", 
                    fecha, ahoraMexico);
        }
        
        // Actualizar el request con la fecha final (siempre en zona horaria de México)
        request.setFechaEmision(fecha);
        logger.info("✓ Fecha final para XML (zona horaria México): {} (ZonedDateTime completo: {})", 
                fecha, ahoraMexico);

        BigDecimal cantidad = request.getCantidad() != null ? request.getCantidad() : BigDecimal.ONE;
        BigDecimal precioUnitario = request.getPrecioUnitario() != null ? request.getPrecioUnitario() : BigDecimal.ZERO;
        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : precioUnitario.multiply(cantidad);
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal ivaTasa = request.getIvaPorcentaje() != null ? request.getIvaPorcentaje() : defaultIvaTasa;
        if (ivaTasa == null || ivaTasa.signum() <= 0) {
            ivaTasa = new BigDecimal("0.16");
        }
        ivaTasa = ivaTasa.setScale(6, RoundingMode.HALF_UP);

        BigDecimal ivaImporte = request.getIvaImporte() != null
                ? request.getIvaImporte().setScale(2, RoundingMode.HALF_UP)
                : subtotal.multiply(ivaTasa).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = request.getTotal() != null
                ? request.getTotal().setScale(2, RoundingMode.HALF_UP)
                : subtotal.add(ivaImporte).setScale(2, RoundingMode.HALF_UP);

        String usoCfdiOriginal = valueOrDefault(request.getUsoCfdi(), "G02");
        String regimenReceptor = valueOrDefault(request.getRegimenFiscal(), "601");
        String motivoRelacion = valueOrDefault(request.getMotivo(), "01");
        
        // Validar y corregir UsoCFDI antes de agregar al XML
        String rfcReceptor = valueOrDefault(request.getRfcReceptor(), "XAXX010101000");
        String usoCfdi = validarYCorregirUsoCFDI(usoCfdiOriginal, rfcReceptor, regimenReceptor);
        if (!usoCfdiOriginal.equals(usoCfdi)) {
            logger.warn("⚠️ CreditNoteXmlBuilder - UsoCFDI corregido de '{}' a '{}' para RFC {} (régimen: {})", 
                    usoCfdiOriginal, usoCfdi, rfcReceptor, regimenReceptor);
        }

        String rfcEmisor = valueOrDefault(request.getRfcEmisor(), emisorRfc);
        String nombreEmisorFinal = xmlEscape(valueOrDefault(emisorNombre, "EMISOR"));
        // LugarExpedicion debe ser un código postal válido del catálogo SAT
        // CRÍTICO: Nunca usar "00000" (inválido según SAT)
        // Usar el CP del application.yml (58000) como valor por defecto
        logger.info("CreditNoteXmlBuilder.buildXml() - emisorCp desde @Value: '{}'", emisorCp);
        
        // Validar y corregir el código postal (validación doble para asegurar)
        // CRÍTICO: NUNCA usar "00000" - siempre usar CP_DEFAULT si es inválido
        String lugarExpedicion;
        String emisorCpTrimmed = emisorCp != null ? emisorCp.trim() : "";
        
        if (emisorCpTrimmed.isEmpty() || "00000".equals(emisorCpTrimmed)) {
            logger.warn("⚠️ CreditNoteXmlBuilder - emisorCp inválido ('{}'), usando CP_DEFAULT='{}'", emisorCp, CP_DEFAULT);
            lugarExpedicion = CP_DEFAULT;
        } else {
            lugarExpedicion = emisorCpTrimmed;
        }
        
        // Validación final ABSOLUTA: NUNCA permitir "00000"
        if ("00000".equals(lugarExpedicion)) {
            logger.error("✗✗✗ ERROR CRÍTICO: lugarExpedicion es '00000' después de validación. Forzando CP_DEFAULT='{}'", CP_DEFAULT);
            lugarExpedicion = CP_DEFAULT;
        }
        
        logger.info("✓ CreditNoteXmlBuilder - lugarExpedicion final validado: '{}' (emisorCp original: '{}')", lugarExpedicion, emisorCp);

        String nombreReceptor = xmlEscape(valueOrDefault(request.getNombreReceptor(), rfcReceptor));
        String cpReceptor = extractCodigoPostal(request.getDomicilioFiscalReceptor());
        if (!StringUtils.hasText(cpReceptor)) {
            cpReceptor = lugarExpedicion;
        }

        String conceptoDescripcion = xmlEscape(valueOrDefault(request.getConcepto(), "Nota de crédito"));
        String claveUnidad = valueOrDefault(request.getUnidad(), DEFAULT_CLAVE_UNIDAD);
        BigDecimal objetoImpRate = ivaImporte.compareTo(BigDecimal.ZERO) > 0 ? ivaTasa : BigDecimal.ZERO;
        String objetoImp = ivaImporte.compareTo(BigDecimal.ZERO) > 0 ? "02" : "01";

        // Log crítico: Verificar la fecha que se va a usar en el XML
        String fechaFormateada = CFDI_DATE_FORMAT.format(ahoraMexico);
        logger.info("🔍 FECHA PARA XML - ZonedDateTime: {}, LocalDateTime: {}, Formateada: {}", 
                ahoraMexico, fecha, fechaFormateada);
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante xmlns:cfdi=\"").append(CFDI_NAMESPACE).append("\" ")
                .append("xmlns:xsi=\"").append(XSI_NAMESPACE).append("\" ")
                .append("xsi:schemaLocation=\"").append(CFDI_SCHEMA).append("\" ")
                .append("Version=\"4.0\" ")
                .append("Serie=\"").append(xmlEscape(serie)).append("\" ")
                .append("Folio=\"").append(xmlEscape(folio)).append("\" ")
                // Formatear fecha directamente desde ZonedDateTime para mantener zona horaria de México
                .append("Fecha=\"").append(fechaFormateada).append("\" ")
                .append("SubTotal=\"").append(formatAmount(subtotal)).append("\" ")
                .append("Total=\"").append(formatAmount(total)).append("\" ")
                .append("Moneda=\"MXN\" ")
                .append("TipoDeComprobante=\"E\" ")
                .append("Exportacion=\"01\" ");
        
        // Validación final antes de agregar al XML
        if ("00000".equals(lugarExpedicion)) {
            logger.error("✗✗✗ ERROR CRÍTICO: lugarExpedicion es '00000' justo antes de agregar al XML. Forzando '58000'");
            lugarExpedicion = "58000";
        }
        
        logger.info("✓✓✓ CreditNoteXmlBuilder - Agregando LugarExpedicion='{}' al XML", lugarExpedicion);
        xml.append("LugarExpedicion=\"").append(xmlEscape(lugarExpedicion)).append("\"");

        if (StringUtils.hasText(request.getFormaPago())) {
            xml.append(" FormaPago=\"").append(xmlEscape(request.getFormaPago())).append("\"");
        }
        if (StringUtils.hasText(request.getMetodoPago())) {
            xml.append(" MetodoPago=\"").append(xmlEscape(request.getMetodoPago())).append("\"");
        }
        xml.append(">\n");

        if (StringUtils.hasText(request.getUuidFacturaOrig())) {
            xml.append("  <cfdi:CfdiRelacionados TipoRelacion=\"").append(xmlEscape(motivoRelacion)).append("\">\n");
            xml.append("    <cfdi:CfdiRelacionado UUID=\"").append(xmlEscape(request.getUuidFacturaOrig().toUpperCase(Locale.ROOT))).append("\"/>\n");
            xml.append("  </cfdi:CfdiRelacionados>\n");
        }

        xml.append("  <cfdi:Emisor Rfc=\"").append(xmlEscape(rfcEmisor)).append("\" ")
                .append("Nombre=\"").append(nombreEmisorFinal).append("\" ")
                .append("RegimenFiscal=\"").append(xmlEscape(emisorRegimen)).append("\"/>\n");

        xml.append("  <cfdi:Receptor Rfc=\"").append(xmlEscape(rfcReceptor)).append("\" ")
                .append("Nombre=\"").append(nombreReceptor).append("\" ")
                .append("DomicilioFiscalReceptor=\"").append(xmlEscape(cpReceptor)).append("\" ")
                .append("RegimenFiscalReceptor=\"").append(xmlEscape(regimenReceptor)).append("\" ")
                .append("UsoCFDI=\"").append(xmlEscape(usoCfdi)).append("\"/>\n");

        xml.append("  <cfdi:Conceptos>\n");
        xml.append("    <cfdi:Concepto ClaveProdServ=\"").append(DEFAULT_CLAVE_PROD_SERV).append("\" ")
                .append("Cantidad=\"").append(formatQuantity(cantidad)).append("\" ")
                .append("ClaveUnidad=\"").append(xmlEscape(claveUnidad)).append("\" ")
                .append("Descripcion=\"").append(conceptoDescripcion).append("\" ")
                .append("ValorUnitario=\"").append(formatAmount(precioUnitario)).append("\" ")
                .append("Importe=\"").append(formatAmount(subtotal)).append("\" ")
                .append("ObjetoImp=\"").append(objetoImp).append("\">");
        if (ivaImporte.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("\n      <cfdi:Impuestos>\n");
            xml.append("        <cfdi:Traslados>\n");
            xml.append("          <cfdi:Traslado Base=\"").append(formatAmount(subtotal)).append("\" ")
                    .append("Impuesto=\"002\" TipoFactor=\"Tasa\" ")
                    .append("TasaOCuota=\"").append(formatRate(objetoImpRate)).append("\" ")
                    .append("Importe=\"").append(formatAmount(ivaImporte)).append("\"/>\n");
            xml.append("        </cfdi:Traslados>\n");
            xml.append("      </cfdi:Impuestos>\n");
            xml.append("    </cfdi:Concepto>\n");
        } else {
            xml.append("</cfdi:Concepto>\n");
        }
        xml.append("  </cfdi:Conceptos>\n");

        if (ivaImporte.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("  <cfdi:Impuestos TotalImpuestosTrasladados=\"").append(formatAmount(ivaImporte)).append("\">\n");
            xml.append("    <cfdi:Traslados>\n");
            xml.append("      <cfdi:Traslado Base=\"").append(formatAmount(subtotal)).append("\" ")
                    .append("Impuesto=\"002\" TipoFactor=\"Tasa\" ")
                    .append("TasaOCuota=\"").append(formatRate(objetoImpRate)).append("\" ")
                    .append("Importe=\"").append(formatAmount(ivaImporte)).append("\"/>\n");
            xml.append("    </cfdi:Traslados>\n");
            xml.append("  </cfdi:Impuestos>\n");
        }

        xml.append("</cfdi:Comprobante>");
        return xml.toString();
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String formatAmount(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatQuantity(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatRate(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String extractCodigoPostal(String domicilio) {
        if (!StringUtils.hasText(domicilio)) {
            return null;
        }
        String trimmed = domicilio.trim();
        Matcher matcher = CP_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (trimmed.matches("\\d{5}")) {
            return trimmed;
        }
        return null;
    }

    /**
     * Valida y corrige el UsoCFDI según el tipo de persona (física o moral) y régimen fiscal
     * CRÍTICO: El UsoCFDI debe corresponder con el tipo de persona y régimen conforme al catálogo c_UsoCFDI
     * 
     * @param usoCfdi UsoCFDI proporcionado
     * @param rfc RFC del receptor (para determinar tipo de persona)
     * @param regimenFiscal Régimen fiscal del receptor
     * @return UsoCFDI válido corregido si es necesario
     */
    private String validarYCorregirUsoCFDI(String usoCfdi, String rfc, String regimenFiscal) {
        if (usoCfdi == null || usoCfdi.trim().isEmpty()) {
            logger.warn("⚠️ UsoCFDI vacío, usando valor por defecto según tipo de persona");
            // Determinar tipo de persona por longitud del RFC
            boolean esPersonaFisica = rfc != null && rfc.length() == 13;
            return esPersonaFisica ? "D01" : "G01"; // D01 para física, G01 para moral
        }

        String usoCfdiUpper = usoCfdi.trim().toUpperCase();
        
        // Determinar tipo de persona por longitud del RFC
        boolean esPersonaFisica = rfc != null && rfc.length() == 13;
        boolean esPersonaMoral = rfc != null && rfc.length() == 12;
        
        // Regímenes fiscales de persona física (principalmente)
        String[] regimenesPersonaFisica = {"605", "606", "607", "608", "610", "611", "612", "614", "615", "616", "621", "625", "626"};
        boolean esRegimenPersonaFisica = false;
        if (regimenFiscal != null) {
            for (String regimen : regimenesPersonaFisica) {
                if (regimen.equals(regimenFiscal)) {
                    esRegimenPersonaFisica = true;
                    break;
                }
            }
        }

        // Validar UsoCFDI según tipo de persona
        if (esPersonaFisica || esRegimenPersonaFisica) {
            // Persona Física: UsoCFDI válidos son principalmente D01-D10 y algunos G específicos (NO G01)
            if (usoCfdiUpper.startsWith("D") || 
                usoCfdiUpper.equals("G02") || usoCfdiUpper.equals("G03") || 
                usoCfdiUpper.equals("CP01") || usoCfdiUpper.equals("CN01")) {
                logger.debug("✓ UsoCFDI válido para persona física: {}", usoCfdiUpper);
                return usoCfdiUpper;
            } else if (usoCfdiUpper.equals("G01")) {
                // G01 NO es válido para persona física
                logger.warn("⚠️ UsoCFDI G01 no es válido para persona física. Corrigiendo a D01 (Gastos en general).");
                logger.warn("⚠️ Para persona física con régimen {}, los UsoCFDI válidos son: D01-D10, G02, G03, CP01, CN01", regimenFiscal);
                return "D01"; // Valor por defecto seguro para persona física
            } else {
                logger.warn("⚠️ UsoCFDI '{}' puede no ser válido para persona física. Verificando...", usoCfdiUpper);
                // Permitir otros códigos pero advertir
                return usoCfdiUpper;
            }
        } else if (esPersonaMoral) {
            // Persona Moral: UsoCFDI válidos son principalmente G01, G02, G03, etc.
            if (usoCfdiUpper.startsWith("G") || 
                usoCfdiUpper.equals("CP01") || usoCfdiUpper.equals("CN01")) {
                logger.debug("✓ UsoCFDI válido para persona moral: {}", usoCfdiUpper);
                return usoCfdiUpper;
            } else if (usoCfdiUpper.startsWith("D")) {
                // D01-D10 son principalmente para persona física
                logger.warn("⚠️ UsoCFDI '{}' (deducciones) generalmente es para persona física. Para persona moral se recomienda G01, G02, G03.", usoCfdiUpper);
                // Permitir pero advertir
                return usoCfdiUpper;
            } else {
                logger.warn("⚠️ UsoCFDI '{}' puede no ser válido para persona moral. Verificando...", usoCfdiUpper);
                return usoCfdiUpper;
            }
        } else {
            // Tipo de persona no determinado, usar el valor proporcionado pero advertir
            logger.warn("⚠️ No se pudo determinar el tipo de persona del RFC: {}. Usando UsoCFDI proporcionado: {}", rfc, usoCfdiUpper);
            return usoCfdiUpper;
        }
    }
}

