package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.CartaPorteSaveRequest;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Autotransporte;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Carro;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.DerechosDePaso;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Domicilio;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.FiguraTransporte;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.IdentificacionVehicular;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Mercancia;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Mercancias;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.ParteTransporte;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.RegimenAduanero;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Remolque;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Seguros;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.TipoFigura;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.TransporteFerroviario;
import com.cibercom.facturacion_back.dto.cartaporte.CartaPorteComplement.Ubicacion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class CartaPorteXmlBuilder {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final BigDecimal IVA_RATE = new BigDecimal("0.16");

    public String construirXml(CartaPorteSaveRequest request,
                               String rfcEmisor,
                               String nombreEmisor,
                               String regimenEmisor,
                               String cpEmisor) {
        CartaPorteComplement complemento = request.getComplemento();
        if (complemento == null) {
            throw new IllegalArgumentException("El complemento Carta Porte es requerido");
        }

        String version = firstNonBlank(request.getVersionComplemento(), complemento.getVersion(), "3.1");
        complemento.setVersion(version);
        // CRÍTICO: IdCCP debe tener exactamente 36 caracteres con formato: CCP[5 hex]-[4 hex]-[4 hex]-[4 hex]-[12 hex]
        // Según XSD: pattern="[C]{3}[a-f0-9A-F]{5}-[a-f0-9A-F]{4}-[a-f0-9A-F]{4}-[a-f0-9A-F]{4}-[a-f0-9A-F]{12}"
        if (complemento.getIdCcp() == null || complemento.getIdCcp().isBlank()) {
            UUID uuid = UUID.randomUUID();
            String hex = uuid.toString().replace("-", "").toUpperCase();
            // Formato: CCP + 5 hex + "-" + 4 hex + "-" + 4 hex + "-" + 4 hex + "-" + 12 hex = 36 caracteres
            complemento.setIdCcp("CCP" + hex.substring(0, 5) + "-" + hex.substring(5, 9) + "-" + 
                                  hex.substring(9, 13) + "-" + hex.substring(13, 17) + "-" + hex.substring(17, 29));
        }

        String prefix = resolvePrefix(version);
        String namespace = resolveNamespace(prefix);
        String schemaLocation = resolveSchema(version, namespace);

        String tipoTransporteSolicitado = firstNonBlank(request.getTipoTransporte(), "01");
        BigDecimal subtotal = parseMonto(request.getPrecio());
        BigDecimal iva = subtotal.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);
        
        String serie = "CP";
        String folio = request.getNumeroSerie() != null && !request.getNumeroSerie().isBlank()
                ? request.getNumeroSerie().trim()
                : String.valueOf(System.currentTimeMillis() % 100000);
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("xmlns:").append(prefix).append("=\"").append(namespace).append("\" ");
        xml.append("xsi:schemaLocation=\"").append(schemaLocation).append("\" ");
        xml.append("Version=\"4.0\"");
        appendAttribute(xml, "Serie", serie, false);
        appendAttribute(xml, "Folio", folio, false);
        appendAttribute(xml, "Fecha", LocalDateTime.now().format(FECHA_FMT), true);
        // IMPORTANTE: Para Carta Porte, el TipoDeComprobante SIEMPRE debe ser "I" (Ingreso)
        // según las reglas del SAT. No se permite "T" (Traslado) en Carta Porte.
        String tipoComprobante = "I";
        appendAttribute(xml, "SubTotal", formatMonto(subtotal, false), true);
        appendAttribute(xml, "Moneda", "MXN", true);
        appendAttribute(xml, "Total", formatMonto(total, false), true);
        appendAttribute(xml, "TipoDeComprobante", tipoComprobante, true);
        appendAttribute(xml, "Exportacion", "01", true);
        appendAttribute(xml, "LugarExpedicion", cpEmisor, true);
        appendAttribute(xml, "FormaPago", "99", true);
        appendAttribute(xml, "MetodoPago", "PUE", true);
        xml.append(">\n");

        xml.append("  <cfdi:Emisor");
        appendAttribute(xml, "Rfc", rfcEmisor, true);
        appendAttribute(xml, "Nombre", nombreEmisor, true);
        appendAttribute(xml, "RegimenFiscal", regimenEmisor, true);
        xml.append("/>\n");

        String nombreReceptor = request.getRazonSocial();
        if (nombreReceptor == null || nombreReceptor.isBlank()) {
            nombreReceptor = String.join(" ",
                    emptyToSpace(request.getNombre()),
                    emptyToSpace(request.getPaterno()),
                    emptyToSpace(request.getMaterno())).trim();
        }
        String receptorRfc = request.getRfcCompleto();
        String receptorNombre = nombreReceptor;
        String receptorCp = request.getDomicilioFiscal();
        String receptorRegimen = request.getRegimenFiscal();
        String usoCfdi = firstNonBlank(request.getUsoCfdi(), "S01");

        xml.append("  <cfdi:Receptor");
        appendAttribute(xml, "Rfc", receptorRfc, true);
        appendAttribute(xml, "Nombre", receptorNombre, true);
        appendAttribute(xml, "DomicilioFiscalReceptor", receptorCp, true);
        appendAttribute(xml, "RegimenFiscalReceptor", receptorRegimen, true);
        appendAttribute(xml, "UsoCFDI", usoCfdi, true);
        xml.append("/>\n");

        // IMPORTANTE: Para Carta Porte, la ClaveProdServ debe ser una clave válida de servicios de transporte
        // Claves válidas según catálogo SAT:
        // - 78101801: Servicios de autotransporte
        // - 78101500: Servicios de transporte aéreo
        // - 78102200: Servicios de transporte marítimo
        // - 78103000: Servicios de transporte ferroviario
        String claveProdServConcepto = resolveClaveProdServ(tipoTransporteSolicitado);
        CartaPorteComplement.Mercancias mercanciasComplemento = complemento.getMercancias();
        if (mercanciasComplemento != null &&
                mercanciasComplemento.getMercancias() != null &&
                !mercanciasComplemento.getMercancias().isEmpty()) {
            CartaPorteComplement.Mercancia mercanciaPrincipal = mercanciasComplemento.getMercancias().get(0);
            // Si la mercancía tiene BienesTransp, validar que sea una clave válida de transporte
            String bienesTransp = mercanciaPrincipal.getBienesTransp();
            if (bienesTransp != null && !bienesTransp.isBlank()) {
                // Validar que sea una clave válida de servicios de transporte
                if (esClaveProdServValidaTransporte(bienesTransp)) {
                    claveProdServConcepto = bienesTransp;
                } else {
                    // Si no es válida, usar la clave por defecto según tipo de transporte
                    // y registrar un warning
                    System.out.println("WARNING: ClaveProdServ '" + bienesTransp + 
                            "' no es válida para servicios de transporte. Usando: " + claveProdServConcepto);
                }
            }
        }

        xml.append("  <cfdi:Conceptos>\n");
        xml.append("    <cfdi:Concepto");
        appendAttribute(xml, "ClaveProdServ", claveProdServConcepto, true);
        appendAttribute(xml, "Cantidad", "1", true);
        appendAttribute(xml, "ClaveUnidad", "E48", true);
        appendAttribute(xml, "Unidad", "SERV", true);
        appendAttribute(xml, "Descripcion", firstNonBlank(request.getDescripcion(), descripcionPorDefecto(tipoTransporteSolicitado)), true);
        appendAttribute(xml, "ValorUnitario", formatMonto(subtotal, false), true);
        appendAttribute(xml, "Importe", formatMonto(subtotal, false), true);
        appendAttribute(xml, "ObjetoImp", "02", true);
        xml.append(">\n");
        if (iva.compareTo(BigDecimal.ZERO) > 0) {
        xml.append("      <cfdi:Impuestos>\n");
        xml.append("        <cfdi:Traslados>\n");
            xml.append("          <cfdi:Traslado");
            appendAttribute(xml, "Base", formatMonto(subtotal, false), true);
            appendAttribute(xml, "Impuesto", "002", true);
            appendAttribute(xml, "TipoFactor", "Tasa", true);
            appendAttribute(xml, "TasaOCuota", "0.160000", true);
            appendAttribute(xml, "Importe", formatMonto(iva, false), true);
            xml.append("/>\n");
        xml.append("        </cfdi:Traslados>\n");
        xml.append("      </cfdi:Impuestos>\n");
        }
        xml.append("    </cfdi:Concepto>\n");
        xml.append("  </cfdi:Conceptos>\n");
        
        if (iva.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("  <cfdi:Impuestos");
            appendAttribute(xml, "TotalImpuestosTrasladados", formatMonto(iva, false), true);
            xml.append(">\n");
        xml.append("    <cfdi:Traslados>\n");
            xml.append("      <cfdi:Traslado");
            appendAttribute(xml, "Base", formatMonto(subtotal, false), true);
            appendAttribute(xml, "Impuesto", "002", true);
            appendAttribute(xml, "TipoFactor", "Tasa", true);
            appendAttribute(xml, "TasaOCuota", "0.160000", true);
            appendAttribute(xml, "Importe", formatMonto(iva, false), true);
            xml.append("/>\n");
        xml.append("    </cfdi:Traslados>\n");
        xml.append("  </cfdi:Impuestos>\n");
        }
        
        xml.append("  <cfdi:Complemento>\n");
        appendCartaPorte(xml, prefix, complemento, tipoTransporteSolicitado);
        xml.append("  </cfdi:Complemento>\n");
        xml.append("</cfdi:Comprobante>");
        
        return xml.toString();
    }
    
    private void appendCartaPorte(StringBuilder xml, String prefix, CartaPorteComplement complemento, String tipoTransporte) {
        // CRÍTICO: Verificar si existe Autotransporte o TransporteFerroviario ANTES de procesar ubicaciones
        // para determinar si TotalDistRec es requerido
        boolean tieneAutotransporte = complemento.getMercancias() != null && 
                                      complemento.getMercancias().getAutotransporte() != null;
        boolean tieneTransporteFerroviario = complemento.getMercancias() != null && 
                                             complemento.getMercancias().getTransporteFerroviario() != null;
        boolean requiereTotalDistRec = tieneAutotransporte || tieneTransporteFerroviario;
        boolean existeTransporteFerroviario = tieneTransporteFerroviario;
        
        // Construir el tag CartaPorte con todos los atributos (sin TotalDistRec todavía)
        xml.append("    <").append(prefix).append(":CartaPorte");
        appendAttribute(xml, "Version", complemento.getVersion(), true);
        appendAttribute(xml, "IdCCP", complemento.getIdCcp(), true);
        appendAttribute(xml, "TranspInternac", firstNonBlank(complemento.getTranspInternac(), "No"), true);
        appendAttribute(xml, "EntradaSalidaMerc", complemento.getEntradaSalidaMerc(), false);
        appendAttribute(xml, "PaisOrigenDestino", complemento.getPaisOrigenDestino(), false);
        appendAttribute(xml, "ViaEntradaSalida", complemento.getViaEntradaSalida(), false);
        appendAttribute(xml, "RegistroISTMO", complemento.getRegistroIstmo(), false);
        appendAttribute(xml, "UbicacionPoloOrigen", complemento.getUbicacionPoloOrigen(), false);
        appendAttribute(xml, "UbicacionPoloDestino", complemento.getUbicacionPoloDestino(), false);
        xml.append(">\n");

        appendRegimenes(xml, prefix, complemento.getRegimenesAduaneros());
        
        // CRÍTICO: appendUbicaciones retorna la suma EXACTA de las distancias que realmente se agregaron al XML
        // Esto asegura que TotalDistRec coincida perfectamente con la suma de DistanciaRecorrida en el XML
        java.math.BigDecimal sumaDistanciasAgregadas = appendUbicaciones(xml, prefix, complemento, "01".equalsIgnoreCase(tipoTransporte), existeTransporteFerroviario);
        
        // CRÍTICO: Ahora que tenemos la suma exacta de las distancias agregadas al XML,
        // insertar TotalDistRec en el tag CartaPorte antes del cierre ">"
        // Según XSD CartaPorte31-2.xml: TotalDistRec es opcional, tipo decimal, minInclusive 0.01, maxInclusive 99999
        // No hay restricción de fractionDigits, pero usamos formato consistente
        if (requiereTotalDistRec) {
            String totalDistRec;
            if (sumaDistanciasAgregadas.compareTo(new java.math.BigDecimal("0.01")) < 0) {
                totalDistRec = "0.01"; // Valor mínimo válido según XSD
            } else {
                // Asegurar que no exceda el máximo según XSD (línea 1925)
                java.math.BigDecimal maxDist = new java.math.BigDecimal("99999");
                java.math.BigDecimal distFinal = sumaDistanciasAgregadas.compareTo(maxDist) > 0 ? maxDist : sumaDistanciasAgregadas;
                // CRÍTICO: Usar el mismo formato que DistanciaRecorrida (2 decimales fijos SIEMPRE) para mantener consistencia total
                // El SAT valida que TotalDistRec = suma de todas las DistanciaRecorrida
                // Usar String.format para asegurar que siempre tenga 2 decimales (251.00, no 251.0 o 251)
                totalDistRec = String.format("%.2f", distFinal.setScale(2, java.math.RoundingMode.HALF_UP));
            }
            // Insertar TotalDistRec en el tag CartaPorte antes del cierre ">"
            String xmlStr = xml.toString();
            // Buscar el cierre del tag CartaPorte (el primer ">" después de "<" + prefix + ":CartaPorte")
            // Buscar tanto ">\n" como solo ">" para mayor robustez
            int posInicioCartaPorte = xmlStr.indexOf("<" + prefix + ":CartaPorte");
            if (posInicioCartaPorte >= 0) {
                // Buscar el cierre del tag, puede ser ">\n" o solo ">"
                int posCierreCartaPorte = xmlStr.indexOf(">\n", posInicioCartaPorte);
                if (posCierreCartaPorte < 0) {
                    // Si no encuentra ">\n", buscar solo ">"
                    posCierreCartaPorte = xmlStr.indexOf(">", posInicioCartaPorte);
                }
                if (posCierreCartaPorte > posInicioCartaPorte) {
                    // Verificar que no haya TotalDistRec ya presente (para evitar duplicados)
                    String tagCartaPorte = xmlStr.substring(posInicioCartaPorte, posCierreCartaPorte);
                    if (!tagCartaPorte.contains("TotalDistRec")) {
                        // Insertar TotalDistRec antes del cierre del tag CartaPorte
                        xml.setLength(0);
                        xml.append(xmlStr.substring(0, posCierreCartaPorte));
                        xml.append(" TotalDistRec=\"").append(totalDistRec).append("\"");
                        xml.append(xmlStr.substring(posCierreCartaPorte));
                    }
                }
            }
        } else {
            // Si no requiere TotalDistRec, solo incluirlo si tiene un valor válido
            String totalDistRecOpcional = complemento.getTotalDistRec();
            if (totalDistRecOpcional != null && !totalDistRecOpcional.isBlank()) {
                try {
                    double dist = Double.parseDouble(totalDistRecOpcional.trim());
                    if (dist >= 0.01 && dist <= 99999) {
                        String xmlStr = xml.toString();
                        int posInicioCartaPorte = xmlStr.indexOf("<" + prefix + ":CartaPorte");
                        if (posInicioCartaPorte >= 0) {
                            int posCierreCartaPorte = xmlStr.indexOf(">\n", posInicioCartaPorte);
                            if (posCierreCartaPorte > 0) {
                                xml.setLength(0);
                                xml.append(xmlStr.substring(0, posCierreCartaPorte));
                                xml.append(" TotalDistRec=\"").append(totalDistRecOpcional.trim()).append("\"");
                                xml.append(xmlStr.substring(posCierreCartaPorte));
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Si no es un número válido, no incluir el atributo
                }
            }
        }
        appendMercancias(xml, prefix, complemento);
        appendFiguraTransporte(xml, prefix, complemento.getFiguraTransporte());

        xml.append("    </").append(prefix).append(":CartaPorte>\n");
    }

    private void appendRegimenes(StringBuilder xml, String prefix, List<RegimenAduanero> regimenes) {
        if (regimenes == null || regimenes.isEmpty()) {
            return;
        }
        xml.append("      <").append(prefix).append(":RegimenesAduaneros>\n");
        for (RegimenAduanero regimen : regimenes) {
            if (regimen == null) continue;
            xml.append("        <").append(prefix).append(":RegimenAduaneroCCP");
            appendAttribute(xml, "RegimenAduanero", regimen.getRegimenAduanero(), true);
            xml.append("/>\n");
        }
        xml.append("      </").append(prefix).append(":RegimenesAduaneros>\n");
    }

    /**
     * Agrega las ubicaciones al XML y retorna la suma EXACTA de las distancias recorridas que realmente se agregaron.
     * CRÍTICO: Solo suma las distancias que realmente se agregan al XML (que cumplen validación >= 0.01 y <= 99999).
     */
    private java.math.BigDecimal appendUbicaciones(StringBuilder xml, String prefix, CartaPorteComplement complemento, boolean esAutotransporte, boolean esFerroviario) {
        List<Ubicacion> ubicaciones = complemento.getUbicaciones();
        if (ubicaciones == null || ubicaciones.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos dos ubicaciones (origen y destino)");
        }

        java.math.BigDecimal sumaDistancias = java.math.BigDecimal.ZERO;
        xml.append("      <").append(prefix).append(":Ubicaciones>\n");
        for (Ubicacion ubicacion : ubicaciones) {
            if (ubicacion == null) continue;
            xml.append("        <").append(prefix).append(":Ubicacion");
            appendAttribute(xml, "TipoUbicacion", ubicacion.getTipoUbicacion(), true);
            appendAttribute(xml, "IDUbicacion", ubicacion.getIdUbicacion(), false);
            appendAttribute(xml, "RFCRemitenteDestinatario", ubicacion.getRfcRemitenteDestinatario(), true);
            appendAttribute(xml, "NombreRemitenteDestinatario", ubicacion.getNombreRemitenteDestinatario(), false);
            appendAttribute(xml, "NumRegIdTrib", ubicacion.getNumRegIdTrib(), false);
            appendAttribute(xml, "ResidenciaFiscal", ubicacion.getResidenciaFiscal(), false);
            appendAttribute(xml, "NumEstacion", ubicacion.getNumEstacion(), false);
            appendAttribute(xml, "NombreEstacion", ubicacion.getNombreEstacion(), false);
            appendAttribute(xml, "NavegacionTrafico", ubicacion.getNavegacionTrafico(), false);
            appendAttribute(xml, "FechaHoraSalidaLlegada", ubicacion.getFechaHoraSalidaLlegada(), true);
            String tipoEstacion = normalizeTipoEstacion(ubicacion.getTipoEstacion());
            
            // CRÍTICO: Según reglas del SAT (Carta Porte 3.1), cuando TipoEstacion="02" 
            // y existe el nodo Mercancias:TransporteFerroviario, el nodo Ubicacion:Domicilio 
            // NO debe existir. Esta regla se aplica independientemente de si el atributo 
            // TipoEstacion se agrega al XML o no.
            // Verificar si es TipoEstacion="02" (normalizado)
            boolean esTipoEstacion02 = tipoEstacion != null && "02".equals(tipoEstacion);
            // Verificar si debe excluir domicilio: cuando esFerroviario=true Y TipoEstacion="02"
            boolean debeExcluirDomicilio = esFerroviario && esTipoEstacion02;
            
            if (!esAutotransporte && !isBlank(tipoEstacion)) {
                appendAttribute(xml, "TipoEstacion", tipoEstacion, false);
            }
            // DistanciaRecorrida es opcional según XSD. Si se incluye, debe ser >= 0.01 y <= 99999
            // CRÍTICO: Usar BigDecimal para formatear con 2 decimales y mantener consistencia con TotalDistRec
            // CRÍTICO: Sumar los valores FORMATEADOS que realmente aparecen en el XML, no los originales
            // Esto asegura que la suma coincida exactamente con lo que el SAT valida
            String distanciaRecorrida = ubicacion.getDistanciaRecorrida();
            if (distanciaRecorrida != null && !distanciaRecorrida.isBlank()) {
                try {
                    java.math.BigDecimal dist = new java.math.BigDecimal(distanciaRecorrida.trim());
                    // Validar rango: >= 0.01 y <= 99999 (según XSD CartaPorte31-2.xml línea 224-225)
                    if (dist.compareTo(new java.math.BigDecimal("0.01")) >= 0 && 
                        dist.compareTo(new java.math.BigDecimal("99999")) <= 0) {
                        // CRÍTICO: Para que TotalDistRec coincida exactamente con la suma de DistanciaRecorrida,
                        // usamos formato fijo de 2 decimales SIEMPRE (incluso si son ceros) para mantener consistencia total.
                        // Esto asegura que el SAT pueda sumar los valores correctamente.
                        // Formatear con 2 decimales fijos usando DecimalFormat para garantizar formato consistente
                        java.math.BigDecimal distFormateada = dist.setScale(2, java.math.RoundingMode.HALF_UP);
                        // Usar String.format para asegurar que siempre tenga 2 decimales (125.50, no 125.5)
                        String distFormateadaStr = String.format("%.2f", distFormateada);
                        appendAttribute(xml, "DistanciaRecorrida", distFormateadaStr, false);
                        // CRÍTICO: Sumar el valor FORMATEADO (con 2 decimales) que realmente aparece en el XML
                        // Esto asegura que la suma coincida exactamente con lo que el SAT valida
                        sumaDistancias = sumaDistancias.add(distFormateada);
                    }
                } catch (NumberFormatException | java.lang.ArithmeticException e) {
                    // Si no es un número válido, no incluir el atributo ni sumar
                }
            }
            
            // CRÍTICO: Si debe excluir domicilio (TipoEstacion="02" y existe TransporteFerroviario),
            // forzar domicilio a null para que NO se incluya en el XML, incluso si existe en los datos.
            // Esto previene que el nodo Domicilio se agregue al XML cuando no debe existir según el SAT.
            Domicilio domicilio = debeExcluirDomicilio ? null : ubicacion.getDomicilio();
            if (domicilio != null) {
                xml.append(">\n");
                appendDomicilio(xml, prefix, domicilio);
                xml.append("        </").append(prefix).append(":Ubicacion>\n");
            } else {
                xml.append("/>\n");
            }
        }
        xml.append("      </").append(prefix).append(":Ubicaciones>\n");
        return sumaDistancias;
    }

    private void appendDomicilio(StringBuilder xml, String prefix, Domicilio domicilio) {
        xml.append("          <").append(prefix).append(":Domicilio");
        appendAttribute(xml, "Calle", domicilio.getCalle(), false);
        appendAttribute(xml, "NumeroExterior", domicilio.getNumeroExterior(), false);
        appendAttribute(xml, "NumeroInterior", domicilio.getNumeroInterior(), false);
        appendAttribute(xml, "Colonia", domicilio.getColonia(), false);
        appendAttribute(xml, "Localidad", domicilio.getLocalidad(), false);
        appendAttribute(xml, "Referencia", domicilio.getReferencia(), false);
        appendAttribute(xml, "Municipio", domicilio.getMunicipio(), false);
        appendAttribute(xml, "Estado", domicilio.getEstado(), true);
        appendAttribute(xml, "Pais", domicilio.getPais(), true);
        appendAttribute(xml, "CodigoPostal", domicilio.getCodigoPostal(), true);
        xml.append("/>\n");
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

    private void appendMercancias(StringBuilder xml, String prefix, CartaPorteComplement complemento) {
        Mercancias mercancias = complemento.getMercancias();
        if (mercancias == null) {
            throw new IllegalArgumentException("El nodo Mercancias es requerido");
        }
        xml.append("      <").append(prefix).append(":Mercancias");
        // PesoBrutoTotal es requerido según XSD, debe ser >= 0.001 y tener máximo 3 decimales
        String pesoBrutoTotal = firstNonBlank(mercancias.getPesoBrutoTotal(), "0.001");
        try {
            double peso = Double.parseDouble(pesoBrutoTotal.trim());
            if (peso < 0.001) {
                pesoBrutoTotal = "0.001"; // Asegurar mínimo
            }
            // Formatear con máximo 3 decimales
            pesoBrutoTotal = String.format("%.3f", Math.max(0.001, peso));
        } catch (NumberFormatException e) {
            pesoBrutoTotal = "0.001"; // Valor por defecto válido
        }
        appendAttribute(xml, "PesoBrutoTotal", pesoBrutoTotal, true);
        appendAttribute(xml, "UnidadPeso", firstNonBlank(mercancias.getUnidadPeso(), "KGM"), true);
        appendAttribute(xml, "PesoNetoTotal", mercancias.getPesoNetoTotal(), false);
        String totalMercancias = mercancias.getNumTotalMercancias();
        if ((totalMercancias == null || totalMercancias.isBlank()) && mercancias.getMercancias() != null) {
            totalMercancias = String.valueOf(mercancias.getMercancias().size());
        }
        appendAttribute(xml, "NumTotalMercancias", firstNonBlank(totalMercancias, "1"), true);
        appendAttribute(xml, "CargoPorTasacion", mercancias.getCargoPorTasacion(), false);
        appendAttribute(xml, "LogisticaInversaRecoleccionDevolucion", mercancias.getLogisticaInversaRecoleccionDevolucion(), false);
        xml.append(">\n");

        List<Mercancia> list = mercancias.getMercancias();
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos una Mercancia");
        }
        for (Mercancia mercancia : list) {
            if (mercancia == null) continue;
            xml.append("        <").append(prefix).append(":Mercancia");
            appendAttribute(xml, "BienesTransp", mercancia.getBienesTransp(), true);
            appendAttribute(xml, "ClaveSTCC", mercancia.getClaveSTCC(), false);
            appendAttribute(xml, "Descripcion", mercancia.getDescripcion(), true);
            appendAttribute(xml, "Cantidad", firstNonBlank(mercancia.getCantidad(), "1"), true);
            appendAttribute(xml, "ClaveUnidad", firstNonBlank(mercancia.getClaveUnidad(), "H87"), true);
            appendAttribute(xml, "Unidad", mercancia.getUnidad(), false);
            appendAttribute(xml, "Dimensiones", mercancia.getDimensiones(), false);
            appendAttribute(xml, "MaterialPeligroso", mercancia.getMaterialPeligroso(), false);
            appendAttribute(xml, "CveMaterialPeligroso", mercancia.getCveMaterialPeligroso(), false);
            appendAttribute(xml, "Embalaje", mercancia.getEmbalaje(), false);
            appendAttribute(xml, "DescripEmbalaje", mercancia.getDescripEmbalaje(), false);
            // PesoEnKg es requerido según XSD, debe ser >= 0.001 y tener máximo 3 decimales
            String pesoEnKg = firstNonBlank(mercancia.getPesoEnKg(), "0.001");
            try {
                double peso = Double.parseDouble(pesoEnKg.trim());
                if (peso < 0.001) {
                    pesoEnKg = "0.001"; // Asegurar mínimo
                } else {
                    // Formatear con máximo 3 decimales
                    pesoEnKg = String.format("%.3f", peso);
                }
            } catch (NumberFormatException e) {
                pesoEnKg = "0.001"; // Valor por defecto válido
            }
            appendAttribute(xml, "PesoEnKg", pesoEnKg, true);
            appendAttribute(xml, "ValorMercancia", mercancia.getValorMercancia(), false);
            appendAttribute(xml, "Moneda", mercancia.getMoneda(), false);
            appendAttribute(xml, "FraccionArancelaria", mercancia.getFraccionArancelaria(), false);
            appendAttribute(xml, "UUIDComercioExt", mercancia.getUuidComercioExt(), false);
            xml.append("/>\n");
        }

        if (mercancias.getAutotransporte() != null) {
            appendAutotransporte(xml, prefix, mercancias.getAutotransporte());
        }

        if (mercancias.getTransporteFerroviario() != null) {
            appendTransporteFerroviario(xml, prefix, mercancias.getTransporteFerroviario());
        }

        xml.append("      </").append(prefix).append(":Mercancias>\n");
    }

    private void appendAutotransporte(StringBuilder xml, String prefix, Autotransporte autotransporte) {
        xml.append("        <").append(prefix).append(":Autotransporte");
        appendAttribute(xml, "PermSCT", autotransporte.getPermSct(), true);
        appendAttribute(xml, "NumPermisoSCT", autotransporte.getNumPermisoSct(), true);
        xml.append(">\n");

        IdentificacionVehicular id = autotransporte.getIdentificacionVehicular();
        if (id == null) {
            throw new IllegalArgumentException("IdentificacionVehicular es requerida para Autotransporte");
        }
        xml.append("          <").append(prefix).append(":IdentificacionVehicular");
        appendAttribute(xml, "ConfigVehicular", id.getConfigVehicular(), true);
        appendAttribute(xml, "PesoBrutoVehicular", id.getPesoBrutoVehicular(), true);
        appendAttribute(xml, "PlacaVM", id.getPlacaVm(), true);
        appendAttribute(xml, "AnioModeloVM", id.getAnioModeloVm(), true);
        xml.append("/>\n");

        Seguros seguros = autotransporte.getSeguros();
        if (seguros == null) {
            throw new IllegalArgumentException("Seguros es requerido para Autotransporte");
        }
        xml.append("          <").append(prefix).append(":Seguros");
        appendAttribute(xml, "AseguraRespCivil", seguros.getAseguraRespCivil(), true);
        appendAttribute(xml, "PolizaRespCivil", seguros.getPolizaRespCivil(), true);
        appendAttribute(xml, "AseguraMedAmbiente", seguros.getAseguraMedAmbiente(), false);
        appendAttribute(xml, "PolizaMedAmbiente", seguros.getPolizaMedAmbiente(), false);
        appendAttribute(xml, "AseguraCarga", seguros.getAseguraCarga(), false);
        appendAttribute(xml, "PolizaCarga", seguros.getPolizaCarga(), false);
        appendAttribute(xml, "PrimaSeguro", seguros.getPrimaSeguro(), false);
        xml.append("/>\n");

        List<Remolque> remolques = autotransporte.getRemolques();
        if (remolques != null && !remolques.isEmpty()) {
            xml.append("          <").append(prefix).append(":Remolques>\n");
            for (Remolque remolque : remolques) {
                if (remolque == null) continue;
                xml.append("            <").append(prefix).append(":Remolque");
                appendAttribute(xml, "SubTipoRem", remolque.getSubTipoRem(), true);
                appendAttribute(xml, "Placa", remolque.getPlaca(), true);
                xml.append("/>\n");
            }
            xml.append("          </").append(prefix).append(":Remolques>\n");
        }

        xml.append("        </").append(prefix).append(":Autotransporte>\n");
    }

    private void appendTransporteFerroviario(StringBuilder xml, String prefix, TransporteFerroviario ferroviario) {
        xml.append("        <").append(prefix).append(":TransporteFerroviario");
        appendAttribute(xml, "TipoDeServicio", ferroviario.getTipoDeServicio(), true);
        appendAttribute(xml, "TipoDeTrafico", ferroviario.getTipoDeTrafico(), true);
        appendAttribute(xml, "NombreAseg", ferroviario.getNombreAseg(), false);
        appendAttribute(xml, "NumPolizaSeguro", ferroviario.getNumPolizaSeguro(), false);
        xml.append(">\n");

        List<DerechosDePaso> derechos = ferroviario.getDerechosDePaso();
        if (derechos != null && !derechos.isEmpty()) {
            for (DerechosDePaso derecho : derechos) {
                if (derecho == null) continue;
                xml.append("          <").append(prefix).append(":DerechosDePaso");
                appendAttribute(xml, "TipoDerechoDePaso", derecho.getTipoDerechoDePaso(), true);
                appendAttribute(xml, "KilometrajePagado", derecho.getKilometrajePagado(), true);
                xml.append("/>\n");
            }
        }

        List<Carro> carros = ferroviario.getCarros();
        if (carros == null || carros.isEmpty()) {
            throw new IllegalArgumentException("Al menos un Carro es requerido para TransporteFerroviario");
        }
        for (Carro carro : carros) {
            if (carro == null) continue;
            xml.append("          <").append(prefix).append(":Carro");
            appendAttribute(xml, "TipoCarro", carro.getTipoCarro(), true);
            appendAttribute(xml, "MatriculaCarro", carro.getMatriculaCarro(), true);
            appendAttribute(xml, "GuiaCarro", carro.getGuiaCarro(), true);
            appendAttribute(xml, "ToneladasNetasCarro", carro.getToneladasNetasCarro(), true);
            xml.append("/>\n");
        }

        xml.append("        </").append(prefix).append(":TransporteFerroviario>\n");
    }

    private void appendFiguraTransporte(StringBuilder xml, String prefix, FiguraTransporte figuraTransporte) {
        if (figuraTransporte == null || figuraTransporte.getTiposFigura() == null || figuraTransporte.getTiposFigura().isEmpty()) {
            throw new IllegalArgumentException("FiguraTransporte con al menos un TipoFigura es requerido");
        }
        xml.append("      <").append(prefix).append(":FiguraTransporte>\n");
        for (TipoFigura figura : figuraTransporte.getTiposFigura()) {
            if (figura == null) continue;
            xml.append("        <").append(prefix).append(":TiposFigura");
            appendAttribute(xml, "TipoFigura", figura.getTipoFigura(), true);
            appendAttribute(xml, "RFCFigura", figura.getRfcFigura(), false);
            appendAttribute(xml, "NumLicencia", figura.getNumLicencia(), false);
            appendAttribute(xml, "NombreFigura", figura.getNombreFigura(), true);
            appendAttribute(xml, "NumRegIdTribFigura", figura.getNumRegIdTribFigura(), false);
            appendAttribute(xml, "ResidenciaFiscalFigura", figura.getResidenciaFiscalFigura(), false);
            boolean hasChild = (figura.getPartesTransporte() != null && !figura.getPartesTransporte().isEmpty())
                    || figura.getDomicilio() != null;
            if (hasChild) {
                xml.append(">\n");
                if (figura.getPartesTransporte() != null) {
                    for (ParteTransporte parte : figura.getPartesTransporte()) {
                        if (parte == null) continue;
                        xml.append("          <").append(prefix).append(":PartesTransporte");
                        appendAttribute(xml, "ParteTransporte", parte.getParteTransporte(), true);
                        xml.append("/>\n");
                    }
                }
                if (figura.getDomicilio() != null) {
                    appendDomicilio(xml, prefix, figura.getDomicilio());
                }
                xml.append("        </").append(prefix).append(":TiposFigura>\n");
            } else {
                xml.append("/>\n");
            }
        }
        xml.append("      </").append(prefix).append(":FiguraTransporte>\n");
    }

    private String resolvePrefix(String version) {
        if ("3.1".equals(version)) {
            return "cartaporte31";
        }
        if ("3.0".equals(version)) {
            return "cartaporte30";
        }
        return "cartaporte20";
    }

    private String resolveNamespace(String prefix) {
        switch (prefix) {
            case "cartaporte31":
                return "http://www.sat.gob.mx/CartaPorte31";
            case "cartaporte30":
                return "http://www.sat.gob.mx/CartaPorte30";
            default:
                return "http://www.sat.gob.mx/CartaPorte20";
        }
    }

    private String resolveSchema(String version, String namespace) {
        String schemaUrl;
        if ("3.1".equals(version)) {
            schemaUrl = "http://www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte31.xsd";
        } else if ("3.0".equals(version)) {
            schemaUrl = "http://www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte30.xsd";
        } else {
            schemaUrl = "http://www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte20.xsd";
        }
        return "http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd "
                + namespace + " " + schemaUrl;
    }

    private BigDecimal parseMonto(String monto) {
        if (monto == null || monto.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(monto.trim().replace(",", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String formatMonto(BigDecimal monto, boolean sinDecimales) {
        if (monto == null) {
            return sinDecimales ? "0" : "0.00";
        }
        BigDecimal scaled = sinDecimales ? monto.setScale(0, RoundingMode.HALF_UP) : monto.setScale(2, RoundingMode.HALF_UP);
        return scaled.toPlainString();
    }

    private void appendAttribute(StringBuilder xml, String nombre, String valor, boolean requerido) {
        String trimmed = valor != null ? valor.trim() : null;
        if (trimmed == null || trimmed.isEmpty()) {
            if (requerido) {
                throw new IllegalArgumentException("El atributo " + nombre + " es requerido en el complemento Carta Porte");
            }
            return;
        }
        xml.append(" ").append(nombre).append("=\"").append(escape(trimmed)).append("\"");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String emptyToSpace(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveClaveProdServ(String tipoTransporte) {
        if ("02".equalsIgnoreCase(tipoTransporte)) {
            return "78102200"; // Servicios de transporte marítimo
        }
        if ("03".equalsIgnoreCase(tipoTransporte)) {
            return "78101500"; // Servicios de transporte aéreo
        }
        if ("04".equalsIgnoreCase(tipoTransporte)) {
            return "78103000"; // Servicios de transporte ferroviario
        }
        return "78101801"; // Autotransporte
    }

    /**
     * Valida si una clave de producto/servicio es válida para servicios de transporte en Carta Porte.
     * Según el catálogo del SAT, las claves válidas son:
     * - 78101801: Servicios de autotransporte
     * - 78101500: Servicios de transporte aéreo
     * - 78102200: Servicios de transporte marítimo
     * - 78103000: Servicios de transporte ferroviario
     */
    private boolean esClaveProdServValidaTransporte(String clave) {
        if (clave == null || clave.isBlank()) {
            return false;
        }
        String claveTrimmed = clave.trim();
        return "78101801".equals(claveTrimmed) ||  // Autotransporte
               "78101500".equals(claveTrimmed) ||  // Transporte aéreo
               "78102200".equals(claveTrimmed) ||  // Transporte marítimo
               "78103000".equals(claveTrimmed);    // Transporte ferroviario
    }

    private String descripcionPorDefecto(String tipoTransporte) {
        if ("02".equalsIgnoreCase(tipoTransporte)) {
            return "Servicio de transporte marítimo";
        }
        if ("03".equalsIgnoreCase(tipoTransporte)) {
            return "Servicio de transporte aéreo";
        }
        if ("04".equalsIgnoreCase(tipoTransporte)) {
            return "Servicio de transporte ferroviario";
        }
        return "Servicio de autotransporte";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
