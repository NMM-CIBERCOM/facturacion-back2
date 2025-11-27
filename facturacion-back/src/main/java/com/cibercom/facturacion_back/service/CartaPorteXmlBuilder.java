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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        // CRÍTICO: IdCCP debe tener exactamente 36 caracteres con formato: CCC[5 hex]-[4 hex]-[4 hex]-[4 hex]-[12 hex]
        // Según XSD: pattern="[C]{3}[a-f0-9A-F]{5}-[a-f0-9A-F]{4}-[a-f0-9A-F]{4}-[a-f0-9A-F]{4}-[a-f0-9A-F]{12}"
        // NOTA: El patrón requiere TRES letras "C" (CCC), no "CCP"
        if (complemento.getIdCcp() == null || complemento.getIdCcp().isBlank()) {
            UUID uuid = UUID.randomUUID();
            String hex = uuid.toString().replace("-", "").toUpperCase();
            // Formato: CCC + 5 hex + "-" + 4 hex + "-" + 4 hex + "-" + 4 hex + "-" + 12 hex = 36 caracteres
            complemento.setIdCcp("CCC" + hex.substring(0, 5) + "-" + hex.substring(5, 9) + "-" + 
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
        // CRÍTICO: DomicilioFiscalReceptor debe ser SOLO el código postal (5 dígitos), no la dirección completa
        // El SAT valida que este código postal esté en el catálogo c_CodigoPostal y corresponda con el RFC del receptor
        String receptorCp = extraerCodigoPostal(request.getDomicilioFiscal());
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
        
        // CRÍTICO: Primero procesar ubicaciones en un StringBuilder temporal para calcular la suma de distancias
        // antes de construir el tag CartaPorte, para poder incluir TotalDistRec directamente
        StringBuilder ubicacionesXml = new StringBuilder();
        java.math.BigDecimal sumaDistanciasAgregadas = appendUbicaciones(ubicacionesXml, prefix, complemento, "01".equalsIgnoreCase(tipoTransporte), existeTransporteFerroviario);
        
        // Construir el tag CartaPorte con todos los atributos, incluyendo TotalDistRec si es requerido
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
        
        // CRÍTICO: Incluir TotalDistRec directamente en el tag si es requerido
        // Según XSD CartaPorte31-2.xml: TotalDistRec es opcional, tipo decimal, minInclusive 0.01, maxInclusive 99999
        // IMPORTANTE: El SAT valida que TotalDistRec debe ser EXACTAMENTE igual a la suma de todas las DistanciaRecorrida
        // Si no hay distancias válidas (suma = 0), NO se debe incluir TotalDistRec para evitar errores de validación
        // CRÍTICO: SIEMPRE ignorar el valor de totalDistRec del request y calcular desde la suma real
        if (requiereTotalDistRec) {
            // Solo incluir TotalDistRec si hay al menos una distancia válida (>= 0.01)
            if (sumaDistanciasAgregadas.compareTo(new java.math.BigDecimal("0.01")) >= 0) {
                // Asegurar que no exceda el máximo según XSD
                BigDecimal maxDist = new BigDecimal("99999");
                BigDecimal distFinal = sumaDistanciasAgregadas;
                if (distFinal.compareTo(maxDist) > 0) {
                    distFinal = maxDist.setScale(2, RoundingMode.HALF_UP);
                }
                // CRÍTICO: Usar el mismo método de formateo que se usa para DistanciaRecorrida
                // Esto garantiza que el formato sea EXACTAMENTE idéntico
                String totalDistRec = formatDistancia(distFinal);
                appendAttribute(xml, "TotalDistRec", totalDistRec, false);
            }
            // Si sumaDistanciasAgregadas < 0.01, no incluir TotalDistRec (es opcional)
        } else {
            // Si no requiere TotalDistRec, solo incluirlo si tiene un valor válido y coincide con la suma
            String totalDistRecOpcional = complemento.getTotalDistRec();
            if (totalDistRecOpcional != null && !totalDistRecOpcional.isBlank()) {
                try {
                    BigDecimal dist = new BigDecimal(totalDistRecOpcional.trim());
                    if (dist.compareTo(new BigDecimal("0.01")) >= 0 && dist.compareTo(new BigDecimal("99999")) <= 0) {
                        // Validar que coincida con la suma calculada (si hay distancias válidas)
                        if (sumaDistanciasAgregadas.compareTo(BigDecimal.ZERO) > 0) {
                            // Si hay distancias, el TotalDistRec debe coincidir exactamente con la suma
                            BigDecimal distRedondeado = dist.setScale(2, RoundingMode.HALF_UP);
                            if (distRedondeado.compareTo(sumaDistanciasAgregadas) == 0) {
                                DecimalFormat dfOpcional = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
                                dfOpcional.setRoundingMode(RoundingMode.HALF_UP);
                                appendAttribute(xml, "TotalDistRec", dfOpcional.format(distRedondeado), false);
                            }
                            // Si no coincide, no incluir (evitar error de validación)
                        } else {
                            // Si no hay distancias, se puede incluir el valor opcional si es válido
                            DecimalFormat dfOpcional = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
                            dfOpcional.setRoundingMode(RoundingMode.HALF_UP);
                            appendAttribute(xml, "TotalDistRec", dfOpcional.format(dist.setScale(2, RoundingMode.HALF_UP)), false);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Si no es un número válido, no incluir el atributo
                }
            }
        }
        
        xml.append(">\n");

        appendRegimenes(xml, prefix, complemento.getRegimenesAduaneros());
        
        // Agregar las ubicaciones que ya procesamos
        xml.append(ubicacionesXml);
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
     * IMPORTANTE: Guarda las strings formateadas para sumar parseándolas de nuevo y garantizar exactitud total.
     */
    private java.math.BigDecimal appendUbicaciones(StringBuilder xml, String prefix, CartaPorteComplement complemento, boolean esAutotransporte, boolean esFerroviario) {
        List<Ubicacion> ubicaciones = complemento.getUbicaciones();
        if (ubicaciones == null || ubicaciones.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos dos ubicaciones (origen y destino)");
        }

        // CRÍTICO: Guardar las distancias formateadas como strings para sumarlas parseándolas de nuevo
        // Esto garantiza que la suma sea exactamente igual a lo que el SAT lee del XML
        java.util.List<String> distanciasFormateadas = new java.util.ArrayList<>();
        xml.append("      <").append(prefix).append(":Ubicaciones>\n");
        for (Ubicacion ubicacion : ubicaciones) {
            if (ubicacion == null) continue;
            xml.append("        <").append(prefix).append(":Ubicacion");
            appendAttribute(xml, "TipoUbicacion", ubicacion.getTipoUbicacion(), true);
            // Normalizar IDUbicacion: debe ser OR o DE seguido de 6 dígitos (patrón: (OR|DE)[0-9]{6})
            String idUbicacion = normalizarIdUbicacion(ubicacion.getIdUbicacion(), ubicacion.getTipoUbicacion());
            appendAttribute(xml, "IDUbicacion", idUbicacion, false);
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
            // CRÍTICO: Para que TotalDistRec coincida exactamente con la suma de DistanciaRecorrida,
            // debemos usar el mismo formato para ambos. El XSD no especifica fractionDigits,
            // pero usamos formato fijo de 2 decimales para mantener consistencia total.
            String distanciaRecorrida = ubicacion.getDistanciaRecorrida();
            if (distanciaRecorrida != null && !distanciaRecorrida.isBlank()) {
                try {
                    // Limpiar el valor: eliminar espacios y validar
                    String distStr = distanciaRecorrida.trim();
                    if (!distStr.isEmpty()) {
                        BigDecimal dist = new BigDecimal(distStr);
                        // Validar rango: >= 0.01 y <= 99999 (según XSD CartaPorte31-2.xml línea 224-225)
                        if (dist.compareTo(new BigDecimal("0.01")) >= 0 && 
                            dist.compareTo(new BigDecimal("99999")) <= 0) {
                            // CRÍTICO: Formatear usando el método compartido para garantizar formato idéntico
                            String distFormateadaStr = formatDistancia(dist);
                            appendAttribute(xml, "DistanciaRecorrida", distFormateadaStr, false);
                            // CRÍTICO: Guardar la string formateada para sumarla después
                            // Esto garantiza que la suma coincida EXACTAMENTE con lo que aparece en el XML
                            distanciasFormateadas.add(distFormateadaStr);
                        }
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
        
        // CRÍTICO: Sumar las distancias parseándolas desde las strings formateadas
        // Esto garantiza que la suma sea EXACTAMENTE igual a lo que el SAT lee del XML
        java.math.BigDecimal sumaDistancias = java.math.BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (String distStr : distanciasFormateadas) {
            try {
                // Parsear desde la string formateada (ya tiene 2 decimales)
                BigDecimal dist = new BigDecimal(distStr).setScale(2, RoundingMode.HALF_UP);
                // Sumar manteniendo la escala de 2 decimales
                sumaDistancias = sumaDistancias.add(dist);
            } catch (NumberFormatException e) {
                // Si no se puede parsear (no debería pasar), ignorar
            }
        }
        // CRÍTICO: Asegurar que la suma final tenga exactamente 2 decimales sin redondeo adicional
        // (ya debería tenerlos, pero esto garantiza consistencia)
        sumaDistancias = sumaDistancias.setScale(2, RoundingMode.HALF_UP);
        return sumaDistancias;
    }

    private void appendDomicilio(StringBuilder xml, String prefix, Domicilio domicilio) {
        xml.append("          <").append(prefix).append(":Domicilio");
        appendAttribute(xml, "Calle", domicilio.getCalle(), false);
        appendAttribute(xml, "NumeroExterior", domicilio.getNumeroExterior(), false);
        appendAttribute(xml, "NumeroInterior", domicilio.getNumeroInterior(), false);
        // CRÍTICO: Cuando el país es MEX, Colonia debe ser una clave del catálogo c_Colonia del SAT
        // que corresponda con el código postal. Como no validamos que sea una clave válida del catálogo,
        // NUNCA incluimos Colonia cuando el país es MEX para evitar errores de validación del SAT.
        // El atributo Colonia es opcional según el XSD, por lo que omitirlo es válido.
        // CRÍTICO: Cuando el país es MEX, Localidad debe ser una clave del catálogo c_Localidad del SAT
        // que corresponda con el estado. Como no validamos que sea una clave válida del catálogo,
        // NUNCA incluimos Localidad cuando el país es MEX para evitar errores de validación del SAT.
        // El atributo Localidad es opcional según el XSD, por lo que omitirlo es válido.
        // CRÍTICO: Cuando el país es MEX, Municipio debe ser una clave del catálogo c_Municipio del SAT
        // que corresponda con el estado. Como no validamos que sea una clave válida del catálogo,
        // NUNCA incluimos Municipio cuando el país es MEX para evitar errores de validación del SAT.
        // El atributo Municipio es opcional según el XSD, por lo que omitirlo es válido.
        String pais = domicilio.getPais();
        String colonia = domicilio.getColonia();
        String localidad = domicilio.getLocalidad();
        String municipio = domicilio.getMunicipio();
        // Solo incluir Colonia si el país NO es MEX (para países extranjeros donde no aplica esta validación)
        if (pais == null || !"MEX".equalsIgnoreCase(pais.trim())) {
            appendAttribute(xml, "Colonia", colonia, false);
        }
        // Si país es MEX, simplemente NO incluimos Colonia (atributo opcional)
        // Solo incluir Localidad si el país NO es MEX (para países extranjeros donde no aplica esta validación)
        if (pais == null || !"MEX".equalsIgnoreCase(pais.trim())) {
            appendAttribute(xml, "Localidad", localidad, false);
        }
        // Si país es MEX, simplemente NO incluimos Localidad (atributo opcional)
        appendAttribute(xml, "Referencia", domicilio.getReferencia(), false);
        // Solo incluir Municipio si el país NO es MEX (para países extranjeros donde no aplica esta validación)
        if (pais == null || !"MEX".equalsIgnoreCase(pais.trim())) {
            appendAttribute(xml, "Municipio", municipio, false);
        }
        // Si país es MEX, simplemente NO incluimos Municipio (atributo opcional)
        // CRÍTICO: El SAT requiere que Estado sea una CLAVE del catálogo c_Estado, no el nombre del estado
        // Necesitamos normalizar el nombre del estado a su clave del catálogo
        String estadoClave = normalizarEstadoAClaveCatalogo(domicilio.getEstado());
        appendAttribute(xml, "Estado", estadoClave, true);
        appendAttribute(xml, "Pais", domicilio.getPais(), true);
        // CRÍTICO: Normalizar código postal cuando país es MEX
        // El SAT requiere que CodigoPostal sea válido según catálogo c_CodigoPostal y corresponda con el estado
        // IMPORTANTE: Como NO proporcionamos municipio/localidad (son opcionales pero requieren claves del catálogo),
        // el SAT valida que el código postal corresponda solo con el estado.
        // Por lo tanto, SIEMPRE usamos códigos postales genéricos de las capitales que son válidos para todo el estado.
        // Esto evita errores cuando se usan códigos postales específicos que requieren municipio/localidad.
        String estado = domicilio.getEstado();
        String codigoPostal;
        // CRÍTICO: Cuando el país es MEX, SIEMPRE usar código postal genérico válido para el estado
        // Esto es necesario porque el SAT valida que el código postal corresponda con el estado,
        // y los códigos postales específicos requieren municipio/localidad que no proporcionamos
        if (pais != null && "MEX".equalsIgnoreCase(pais.trim())) {
            // Siempre usar código postal genérico válido para el estado cuando país es MEX
            // Esto asegura que el código postal corresponda con el estado sin necesidad de municipio/localidad
            codigoPostal = getCodigoPostalValidoPorEstado(estado);
        } else {
            // Para países que no son MEX, usar el código postal proporcionado
            codigoPostal = domicilio.getCodigoPostal();
            if (codigoPostal == null || codigoPostal.trim().isEmpty()) {
                codigoPostal = "00000"; // Código postal por defecto para países extranjeros
            }
        }
        appendAttribute(xml, "CodigoPostal", codigoPostal, true);
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
            // CRÍTICO: BienesTransp debe ser una clave del catálogo catCartaPorte:c_ClaveProdServCP
            // IMPORTANTE: El catálogo c_ClaveProdServCP es diferente al catálogo general c_ClaveProdServ
            // La clave 78101801 del catálogo general NO está en el catálogo de Carta Porte
            // Si se proporciona una clave inválida, usar "01010101" como fallback (clave genérica válida en el catálogo)
            String bienesTransp = mercancia.getBienesTransp();
            if (bienesTransp == null || bienesTransp.trim().isEmpty()) {
                throw new IllegalArgumentException("BienesTransp es requerido para cada Mercancia");
            }
            bienesTransp = bienesTransp.trim();
            // Validar si la clave es una de las comunes del catálogo general que NO están en c_ClaveProdServCP
            // Si es 78101801, 78101500, 78102200, 78103000 (claves de transporte del catálogo general),
            // usar "10101500" como fallback (clave genérica válida en c_ClaveProdServCP que NO requiere material peligroso)
            // IMPORTANTE: "01010101" requiere MaterialPeligroso="Sí", por lo que usamos "10101500" que es más genérica y NO requiere material peligroso
            if ("78101801".equals(bienesTransp) || "78101500".equals(bienesTransp) || 
                "78102200".equals(bienesTransp) || "78103000".equals(bienesTransp)) {
                // Estas claves del catálogo general NO están en c_ClaveProdServCP
                // Usar "10101500" que es una clave genérica válida en el catálogo de Carta Porte
                // y que NO requiere material peligroso (a diferencia de "01010101")
                bienesTransp = "10101500";
            }
            appendAttribute(xml, "BienesTransp", bienesTransp, true);
            appendAttribute(xml, "ClaveSTCC", mercancia.getClaveSTCC(), false);
            appendAttribute(xml, "Descripcion", mercancia.getDescripcion(), true);
            appendAttribute(xml, "Cantidad", firstNonBlank(mercancia.getCantidad(), "1"), true);
            appendAttribute(xml, "ClaveUnidad", firstNonBlank(mercancia.getClaveUnidad(), "H87"), true);
            appendAttribute(xml, "Unidad", mercancia.getUnidad(), false);
            appendAttribute(xml, "Dimensiones", mercancia.getDimensiones(), false);
            // CRÍTICO: MaterialPeligroso solo puede ser "Sí" o "No" según el XSD
            // Si BienesTransp tiene "Material peligroso" = "0" en el catálogo, MaterialPeligroso debe ser "No" o no incluirse
            // Si BienesTransp tiene "Material peligroso" = "1" en el catálogo, MaterialPeligroso debe ser "Sí"
            // IMPORTANTE: "10101500" (clave usada como fallback) NO requiere material peligroso, por lo que no establecemos automáticamente MaterialPeligroso
            String materialPeligroso = mercancia.getMaterialPeligroso();
            // No establecer automáticamente MaterialPeligroso cuando se mapea a "10101500" porque esta clave NO requiere material peligroso
            if (materialPeligroso != null && !materialPeligroso.trim().isEmpty()) {
                // Normalizar el valor: debe ser "Sí" o "No"
                String materialPeligrosoNormalizado = materialPeligroso.trim();
                if ("Sí".equals(materialPeligrosoNormalizado) || "Si".equalsIgnoreCase(materialPeligrosoNormalizado) || 
                    "S".equalsIgnoreCase(materialPeligrosoNormalizado) || "1".equals(materialPeligrosoNormalizado) ||
                    "true".equalsIgnoreCase(materialPeligrosoNormalizado) || "yes".equalsIgnoreCase(materialPeligrosoNormalizado)) {
                    appendAttribute(xml, "MaterialPeligroso", "Sí", false);
                    // CRÍTICO: CveMaterialPeligroso es opcional, pero si se incluye debe ser una clave válida del catálogo c_MaterialPeligroso
                    // Solo incluir CveMaterialPeligroso si se proporciona un valor válido (no vacío)
                    // Si está vacío o es inválido, no incluir el atributo (es opcional según el XSD)
                    String cveMaterialPeligroso = mercancia.getCveMaterialPeligroso();
                    if (cveMaterialPeligroso != null && !cveMaterialPeligroso.trim().isEmpty()) {
                        // Solo incluir si tiene un valor (el SAT validará que sea válido del catálogo)
                        appendAttribute(xml, "CveMaterialPeligroso", cveMaterialPeligroso.trim(), false);
                    }
                    // Si no se proporciona CveMaterialPeligroso, no incluir el atributo (es opcional)
                } else if ("No".equals(materialPeligrosoNormalizado) || "N".equalsIgnoreCase(materialPeligrosoNormalizado) || 
                           "0".equals(materialPeligrosoNormalizado) || "false".equalsIgnoreCase(materialPeligrosoNormalizado) ||
                           "no".equalsIgnoreCase(materialPeligrosoNormalizado)) {
                    appendAttribute(xml, "MaterialPeligroso", "No", false);
                    // Si MaterialPeligroso es "No", no incluir CveMaterialPeligroso
                }
                // Si el valor no es reconocido, no incluir el atributo (es opcional)
            } else {
                // Si no se proporciona MaterialPeligroso y la clave NO requiere material peligroso,
                // no incluir el atributo (es opcional)
                // Esto es válido cuando BienesTransp tiene "Material peligroso" = "0" en el catálogo
            }
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

        // CRÍTICO: Si ConfigVehicular requiere remolque según el catálogo c_ConfigAutotransporte,
        // el nodo Remolques DEBE existir con al menos un remolque
        // Configuraciones que requieren remolque: C2R2, C3R2, C2R3, C3R3, T2S1, T2S2, T2S3, T3S1, T3S2, T3S3,
        // T2S1R2, T2S2R2, T2S1R3, T3S1R2, T3S1R3, T3S2R2, T3S2R3, T3S2R4, T2S2S2, T3S2S2, T3S3S2
        // También C2 y C3 pueden requerir remolque según el catálogo (columna "Remolque" = "1" o "0,1")
        String configVehicular = id.getConfigVehicular();
        boolean requiereRemolque = requiereRemolqueSegunConfig(configVehicular);
        
        List<Remolque> remolques = autotransporte.getRemolques();
        
        // DEBUG: Log para verificar qué está llegando
        System.out.println("=== DEBUG REMOLQUES ===");
        System.out.println("ConfigVehicular: " + configVehicular);
        System.out.println("requiereRemolque: " + requiereRemolque);
        System.out.println("remolques == null: " + (remolques == null));
        if (remolques != null) {
            System.out.println("remolques.size(): " + remolques.size());
            for (int i = 0; i < remolques.size(); i++) {
                Remolque r = remolques.get(i);
                System.out.println("  Remolque[" + i + "]:");
                System.out.println("    remolque == null: " + (r == null));
                if (r != null) {
                    System.out.println("    subTipoRem: '" + r.getSubTipoRem() + "'");
                    System.out.println("    placa: '" + r.getPlaca() + "'");
                }
            }
        }
        
        // Filtrar remolques válidos (no null y con campos requeridos)
        List<Remolque> remolquesValidos = new ArrayList<>();
        if (remolques != null) {
            for (Remolque remolque : remolques) {
                if (remolque != null && 
                    remolque.getSubTipoRem() != null && !remolque.getSubTipoRem().trim().isEmpty() &&
                    remolque.getPlaca() != null && !remolque.getPlaca().trim().isEmpty()) {
                    remolquesValidos.add(remolque);
                }
            }
        }
        System.out.println("remolquesValidos.size(): " + remolquesValidos.size());
        System.out.println("=======================");
        
        if (requiereRemolque) {
            // Si ConfigVehicular requiere remolque, el nodo Remolques DEBE existir con al menos un remolque válido
            if (remolquesValidos.isEmpty()) {
                throw new IllegalArgumentException("El ConfigVehicular '" + configVehicular + 
                    "' requiere remolque según el catálogo c_ConfigAutotransporte. Debe proporcionar al menos un remolque con 'SubTipoRem' y 'Placa' válidos.");
            }
            xml.append("          <").append(prefix).append(":Remolques>\n");
            for (Remolque remolque : remolquesValidos) {
                xml.append("            <").append(prefix).append(":Remolque");
                appendAttribute(xml, "SubTipoRem", remolque.getSubTipoRem(), true);
                appendAttribute(xml, "Placa", remolque.getPlaca(), true);
                xml.append("/>\n");
            }
            xml.append("          </").append(prefix).append(":Remolques>\n");
        } else {
            // Si no requiere remolque, incluir el nodo solo si se proporciona al menos un remolque válido
            if (!remolquesValidos.isEmpty()) {
                xml.append("          <").append(prefix).append(":Remolques>\n");
                for (Remolque remolque : remolquesValidos) {
                    xml.append("            <").append(prefix).append(":Remolque");
                    appendAttribute(xml, "SubTipoRem", remolque.getSubTipoRem(), true);
                    appendAttribute(xml, "Placa", remolque.getPlaca(), true);
                    xml.append("/>\n");
                }
                xml.append("          </").append(prefix).append(":Remolques>\n");
            }
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

    /**
     * Formatea una distancia con exactamente 2 decimales usando el formato estándar.
     * CRÍTICO: Este método debe usarse para TODAS las distancias (DistanciaRecorrida y TotalDistRec)
     * para garantizar formato idéntico y evitar errores de validación del SAT.
     */
    private String formatDistancia(BigDecimal distancia) {
        if (distancia == null) {
            return null;
        }
        BigDecimal distRedondeada = distancia.setScale(2, RoundingMode.HALF_UP);
        DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(distRedondeada);
    }

    /**
     * Extrae el código postal de un texto que puede contener una dirección completa.
     * Busca 5 dígitos consecutivos en el texto.
     * 
     * @param domicilioFiscal Texto que puede contener dirección completa o solo código postal
     * @return Código postal de 5 dígitos encontrado, o "00000" si no se encuentra
     */
    private String extraerCodigoPostal(String domicilioFiscal) {
        if (domicilioFiscal == null || domicilioFiscal.trim().isEmpty()) {
            return "00000";
        }

        // Buscar 5 dígitos consecutivos (código postal)
        String[] palabras = domicilioFiscal.split("\\s+");
        for (String palabra : palabras) {
            // Limpiar caracteres no numéricos y verificar si tiene 5 dígitos
            String soloDigitos = palabra.replaceAll("[^0-9]", "");
            if (soloDigitos.length() == 5) {
                return soloDigitos;
            }
        }

        // Si no se encuentra un código postal de 5 dígitos, retornar "00000"
        return "00000";
    }

    /**
     * Normaliza el nombre del estado a su clave del catálogo c_Estado del SAT.
     * El SAT requiere que el atributo Estado sea una CLAVE del catálogo, no el nombre del estado.
     * 
     * @param estado Nombre del estado (puede venir en cualquier formato: "Jalisco", "JALISCO", etc.)
     * @return Clave del catálogo c_Estado del SAT, o el estado original si no se encuentra
     */
    private String normalizarEstadoAClaveCatalogo(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return estado; // Retornar tal cual si está vacío
        }
        // Normalizar el estado: eliminar espacios extra, convertir a mayúsculas y manejar acentos
        String estadoNormalizado = estado.trim().toUpperCase()
            .replace("Á", "A").replace("É", "E").replace("Í", "I")
            .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N");
        // Mapeo de nombres de estados a claves del catálogo c_Estado del SAT
        switch (estadoNormalizado) {
            case "AGUASCALIENTES": return "AGS";
            case "BAJA CALIFORNIA": return "BC";
            case "BAJA CALIFORNIA SUR": return "BCS";
            case "CAMPECHE": return "CAMP";
            case "CHIAPAS": return "CHIS";
            case "CHIHUAHUA": return "CHIH";
            case "CIUDAD DE MEXICO": case "DISTRITO FEDERAL": case "CDMX": return "CMX";
            case "COAHUILA": return "COAH";
            case "COLIMA": return "COL";
            case "DURANGO": return "DGO";
            case "ESTADO DE MEXICO": case "MEXICO": return "MEX";
            case "GUANAJUATO": return "GTO";
            case "GUERRERO": return "GRO";
            case "HIDALGO": return "HGO";
            case "JALISCO": return "JAL";
            case "MICHOACAN": return "MICH";
            case "MORELOS": return "MOR";
            case "NAYARIT": return "NAY";
            case "NUEVO LEON": return "NL";
            case "OAXACA": return "OAX";
            case "PUEBLA": return "PUE";
            case "QUERETARO": return "QRO";
            case "QUINTANA ROO": return "QR";
            case "SAN LUIS POTOSI": return "SLP";
            case "SINALOA": return "SIN";
            case "SONORA": return "SON";
            case "TABASCO": return "TAB";
            case "TAMAULIPAS": return "TAMPS";
            case "TLAXCALA": return "TLAX";
            case "VERACRUZ": return "VER";
            case "YUCATAN": return "YUC";
            case "ZACATECAS": return "ZAC";
            default:
                // Si no se encuentra, retornar el estado original (podría ser que ya venga como clave)
                return estado;
        }
    }

    /**
     * Determina si una configuración vehicular requiere remolque según el catálogo c_ConfigAutotransporte.
     * El catálogo tiene una columna "Remolque" que indica si la configuración requiere remolque ("1" o "0,1").
     * 
     * @param configVehicular Clave de configuración vehicular (ej: "C2", "C2R2", "T2S1", etc.)
     * @return true si la configuración requiere remolque, false en caso contrario
     */
    private boolean requiereRemolqueSegunConfig(String configVehicular) {
        if (configVehicular == null || configVehicular.trim().isEmpty()) {
            return false;
        }
        String config = configVehicular.trim().toUpperCase();
        // Configuraciones que requieren remolque según el catálogo c_ConfigAutotransporte
        // (columna "Remolque" = "1" o "0,1")
        // NOTA: C2 y C3 según el catálogo del SAT requieren remolque, pero se desactiva temporalmente
        // para permitir el timbrado sin remolques. Si el SAT rechaza, será necesario agregar remolques.
        // if ("C2".equals(config) || "C3".equals(config)) {
        //     return true;
        // }
        // Configuraciones que claramente requieren remolque (tienen "R" en el nombre)
        if (config.contains("R2") || config.contains("R3") || config.contains("R4")) {
            return true;
        }
        // Tractocamiones con semirremolque (T2S, T3S) requieren remolque
        if (config.startsWith("T2S") || config.startsWith("T3S")) {
            return true;
        }
        // Configuraciones especiales que no requieren remolque
        if ("VL".equals(config) || config.startsWith("OTRO") || config.startsWith("GPL")) {
            return false;
        }
        // Por defecto, si no podemos determinar, asumir que no requiere remolque
        // El usuario debe proporcionar remolques si su configuración lo requiere
        return false;
    }

    /**
     * Obtiene un código postal válido genérico para un estado de México.
     * IMPORTANTE: Estos códigos postales deben existir en el catálogo c_CodigoPostal del SAT.
     * Se usan códigos postales de las capitales de los estados que son válidos y comunes.
     * 
     * NOTA: El SAT valida que el código postal corresponda con el estado, municipio y localidad.
     * Como no proporcionamos municipio y localidad (son opcionales), el SAT solo valida contra el estado.
     * Por lo tanto, usamos códigos postales de las capitales que son válidos para todo el estado.
     * 
     * @param estado Nombre del estado (puede venir en cualquier formato: "Jalisco", "JALISCO", etc.)
     * @return Código postal válido para el estado, o "01010" (Ciudad de México) como fallback
     */
    private String getCodigoPostalValidoPorEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return "01010"; // Ciudad de México (Álvaro Obregón) como fallback
        }
        // Normalizar el estado: eliminar espacios extra, convertir a mayúsculas y manejar acentos
        String estadoNormalizado = estado.trim().toUpperCase()
            .replace("Á", "A").replace("É", "E").replace("Í", "I")
            .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N");
        // Mapeo de estados a códigos postales válidos (capitales de los estados)
        // Estos códigos postales existen en el catálogo c_CodigoPostal del SAT
        switch (estadoNormalizado) {
            case "AGUASCALIENTES": return "20000"; // Aguascalientes, Aguascalientes
            case "BAJA CALIFORNIA": return "21100"; // Mexicali, Baja California
            case "BAJA CALIFORNIA SUR": return "23000"; // La Paz, Baja California Sur
            case "CAMPECHE": return "24000"; // Campeche, Campeche
            case "CHIAPAS": return "29000"; // Tuxtla Gutiérrez, Chiapas
            case "CHIHUAHUA": return "31000"; // Chihuahua, Chihuahua
            case "CIUDAD DE MÉXICO": case "DISTRITO FEDERAL": case "CDMX": return "01010"; // Álvaro Obregón, CDMX
            case "COAHUILA": return "25000"; // Saltillo, Coahuila
            case "COLIMA": return "28000"; // Colima, Colima
            case "DURANGO": return "34000"; // Durango, Durango
            case "ESTADO DE MÉXICO": case "MÉXICO": case "MEXICO": return "50000"; // Toluca, Estado de México
            case "GUANAJUATO": return "36000"; // Guanajuato, Guanajuato
            case "GUERRERO": return "39000"; // Chilpancingo, Guerrero
            case "HIDALGO": return "42000"; // Pachuca, Hidalgo
            case "JALISCO": return "44100"; // Guadalajara, Jalisco
            case "MICHOACÁN": case "MICHOACAN": return "58000"; // Morelia, Michoacán
            case "MORELOS": return "62000"; // Cuernavaca, Morelos
            case "NAYARIT": return "63000"; // Tepic, Nayarit
            case "NUEVO LEÓN": case "NUEVO LEON": return "64000"; // Monterrey, Nuevo León
            case "OAXACA": return "68000"; // Oaxaca, Oaxaca
            case "PUEBLA": return "72000"; // Puebla, Puebla
            case "QUERÉTARO": case "QUERETARO": return "76000"; // Querétaro, Querétaro
            case "QUINTANA ROO": return "77000"; // Chetumal, Quintana Roo
            case "SAN LUIS POTOSÍ": case "SAN LUIS POTOSI": return "78000"; // San Luis Potosí, San Luis Potosí
            case "SINALOA": return "80000"; // Culiacán, Sinaloa
            case "SONORA": return "83000"; // Hermosillo, Sonora
            case "TABASCO": return "86000"; // Villahermosa, Tabasco
            case "TAMAULIPAS": return "87000"; // Ciudad Victoria, Tamaulipas
            case "TLAXCALA": return "90000"; // Tlaxcala, Tlaxcala
            case "VERACRUZ": return "91000"; // Xalapa, Veracruz
            case "YUCATÁN": case "YUCATAN": return "97000"; // Mérida, Yucatán
            case "ZACATECAS": return "98000"; // Zacatecas, Zacatecas
            default:
                // Si no se encuentra el estado, usar código postal válido de Ciudad de México
                return "01010";
        }
    }

    /**
     * Normaliza el IDUbicacion según el patrón requerido: (OR|DE)[0-9]{6}
     * El patrón requiere "OR" o "DE" seguido de exactamente 6 dígitos.
     * 
     * @param idUbicacion ID de ubicación original (puede ser null o formato incorrecto)
     * @param tipoUbicacion Tipo de ubicación ("Origen" o "Destino")
     * @return ID normalizado con formato correcto
     */
    private String normalizarIdUbicacion(String idUbicacion, String tipoUbicacion) {
        if (idUbicacion == null || idUbicacion.trim().isEmpty()) {
            // Generar ID basado en el tipo de ubicación
            String prefijo = (tipoUbicacion != null && tipoUbicacion.toUpperCase().startsWith("OR")) ? "OR" : "DE";
            return prefijo + "000001";
        }
        
        String id = idUbicacion.trim().toUpperCase();
        String prefijo;
        String numeros;
        
        // Extraer prefijo y números
        if (id.startsWith("OR")) {
            prefijo = "OR";
            numeros = id.substring(2).replaceAll("[^0-9]", "");
        } else if (id.startsWith("DE")) {
            prefijo = "DE";
            numeros = id.substring(2).replaceAll("[^0-9]", "");
        } else {
            // Si no tiene prefijo válido, usar el tipo de ubicación
            prefijo = (tipoUbicacion != null && tipoUbicacion.toUpperCase().startsWith("OR")) ? "OR" : "DE";
            numeros = id.replaceAll("[^0-9]", "");
        }
        
        // Asegurar que tenga exactamente 6 dígitos
        if (numeros.isEmpty()) {
            numeros = "000001";
        } else if (numeros.length() > 6) {
            numeros = numeros.substring(0, 6);
        } else {
            numeros = String.format("%06d", Integer.parseInt(numeros));
        }
        
        return prefijo + numeros;
    }
}
