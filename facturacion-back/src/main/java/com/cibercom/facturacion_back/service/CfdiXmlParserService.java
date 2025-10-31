package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.CfdiConsultaResponse;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CfdiXmlParserService {

    public CfdiConsultaResponse.Basicos parseBasicos(String xml) {
        try {
            Document doc = build(xml);
            Element comprobante = getFirstByLocalName(doc, "Comprobante");
            Element receptor = getFirstByLocalName(doc, "Receptor");

            String serie = attr(comprobante, "Serie");
            String folio = attr(comprobante, "Folio");
            String subtotalStr = attr(comprobante, "SubTotal");
            String descuentoStr = attr(comprobante, "Descuento");
            String totalStr = attr(comprobante, "Total");
            String metodo = attr(comprobante, "MetodoPago");
            String forma = attr(comprobante, "FormaPago");
            String uso = attr(receptor, "UsoCFDI");

            BigDecimal subtotal = toBD(subtotalStr);
            BigDecimal descuento = toBD(descuentoStr);
            BigDecimal total = toBD(totalStr);

            BigDecimal iva = BigDecimal.ZERO;
            BigDecimal ieps = BigDecimal.ZERO;
            // Desgloses
            BigDecimal iva16 = BigDecimal.ZERO;
            BigDecimal iva8 = BigDecimal.ZERO;
            BigDecimal iva0 = BigDecimal.ZERO;
            BigDecimal ivaExento = BigDecimal.ZERO;
            BigDecimal ieps26 = BigDecimal.ZERO;
            BigDecimal ieps160 = BigDecimal.ZERO;
            BigDecimal ieps8 = BigDecimal.ZERO;
            BigDecimal ieps30 = BigDecimal.ZERO;
            BigDecimal ieps304 = BigDecimal.ZERO;
            BigDecimal ieps7 = BigDecimal.ZERO;
            BigDecimal ieps53 = BigDecimal.ZERO;
            BigDecimal ieps25 = BigDecimal.ZERO;
            BigDecimal ieps6 = BigDecimal.ZERO;
            BigDecimal ieps50 = BigDecimal.ZERO;
            BigDecimal ieps9 = BigDecimal.ZERO;
            BigDecimal ieps3 = BigDecimal.ZERO;
            BigDecimal ieps43 = BigDecimal.ZERO;

            // Impuestos a nivel comprobante
            Element impuestos = getFirstByLocalName(doc, "Impuestos");
            if (impuestos != null) {
                Element traslados = getFirstChildByLocalName(impuestos, "Traslados");
                if (traslados != null) {
                    NodeList list = traslados.getChildNodes();
                    for (int i = 0; i < list.getLength(); i++) {
                        Node n = list.item(i);
                        if (n.getNodeType() == Node.ELEMENT_NODE) {
                            Element t = (Element) n;
                            String impuesto = attr(t, "Impuesto");
                            String tipoFactor = attr(t, "TipoFactor");
                            BigDecimal importe = toBD(attr(t, "Importe"));
                            BigDecimal tasa = toBD(attr(t, "TasaOCuota"));
                            if (importe == null) importe = BigDecimal.ZERO;
                            if ("002".equals(impuesto)) { // IVA
                                iva = iva.add(importe);
                                if ("Exento".equalsIgnoreCase(tipoFactor)) {
                                    // Importe de IVA exento es 0; conservamos registro como 0
                                    // Podríamos sumar Base si se requiere en otro campo.
                                } else if (tasa != null) {
                                    if (tasa.compareTo(new BigDecimal("0.16")) == 0) iva16 = iva16.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.08")) == 0) iva8 = iva8.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.00")) == 0) iva0 = iva0.add(importe);
                                }
                            } else if ("003".equals(impuesto)) { // IEPS
                                ieps = ieps.add(importe);
                                if (tasa != null) {
                                    if (tasa.compareTo(new BigDecimal("0.26")) == 0) ieps26 = ieps26.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("1.60")) == 0) ieps160 = ieps160.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.08")) == 0) ieps8 = ieps8.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.30")) == 0) ieps30 = ieps30.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.304")) == 0) ieps304 = ieps304.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.07")) == 0) ieps7 = ieps7.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.53")) == 0) ieps53 = ieps53.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.25")) == 0) ieps25 = ieps25.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.06")) == 0) ieps6 = ieps6.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.50")) == 0) ieps50 = ieps50.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.09")) == 0) ieps9 = ieps9.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.03")) == 0) ieps3 = ieps3.add(importe);
                                    else if (tasa.compareTo(new BigDecimal("0.43")) == 0) ieps43 = ieps43.add(importe);
                                }
                            }
                        }
                    }
                }
            }

            // Impuestos a nivel conceptos (por si no están sumados en comprobante)
            Element conceptos = getFirstByLocalName(doc, "Conceptos");
            if (conceptos != null) {
                NodeList cList = conceptos.getChildNodes();
                for (int ci = 0; ci < cList.getLength(); ci++) {
                    Node cn = cList.item(ci);
                    if (cn.getNodeType() == Node.ELEMENT_NODE && hasLocalName((Element) cn, "Concepto")) {
                        Element c = (Element) cn;
                        Element impC = getFirstChildByLocalName(c, "Impuestos");
                        if (impC != null) {
                            Element trasC = getFirstChildByLocalName(impC, "Traslados");
                            if (trasC != null) {
                                NodeList tl = trasC.getChildNodes();
                                for (int ti = 0; ti < tl.getLength(); ti++) {
                                    Node tn = tl.item(ti);
                                    if (tn.getNodeType() == Node.ELEMENT_NODE && hasLocalName((Element) tn, "Traslado")) {
                                        Element t = (Element) tn;
                                        String impuesto = attr(t, "Impuesto");
                                        String tipoFactor = attr(t, "TipoFactor");
                                        BigDecimal importe = toBD(attr(t, "Importe"));
                                        BigDecimal tasa = toBD(attr(t, "TasaOCuota"));
                                        if (importe == null) importe = BigDecimal.ZERO;
                                        if ("002".equals(impuesto)) {
                                            iva = iva.add(importe);
                                            if ("Exento".equalsIgnoreCase(tipoFactor)) {
                                                // IVA exento, importe 0
                                            } else if (tasa != null) {
                                                if (tasa.compareTo(new BigDecimal("0.16")) == 0) iva16 = iva16.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.08")) == 0) iva8 = iva8.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.00")) == 0) iva0 = iva0.add(importe);
                                            }
                                        } else if ("003".equals(impuesto)) {
                                            ieps = ieps.add(importe);
                                            if (tasa != null) {
                                                if (tasa.compareTo(new BigDecimal("0.26")) == 0) ieps26 = ieps26.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("1.60")) == 0) ieps160 = ieps160.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.08")) == 0) ieps8 = ieps8.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.30")) == 0) ieps30 = ieps30.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.304")) == 0) ieps304 = ieps304.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.07")) == 0) ieps7 = ieps7.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.53")) == 0) ieps53 = ieps53.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.25")) == 0) ieps25 = ieps25.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.06")) == 0) ieps6 = ieps6.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.50")) == 0) ieps50 = ieps50.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.09")) == 0) ieps9 = ieps9.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.03")) == 0) ieps3 = ieps3.add(importe);
                                                else if (tasa.compareTo(new BigDecimal("0.43")) == 0) ieps43 = ieps43.add(importe);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return CfdiConsultaResponse.Basicos.builder()
                    .serie(serie)
                    .folio(folio)
                    .subtotal(subtotal)
                    .descuento(descuento)
                    .iva(iva)
                    .ieps(ieps)
                    .total(total)
                    .metodoPago(metodo)
                    .formaPago(forma)
                    .usoCfdi(uso)
                    .iva16(iva16)
                    .iva8(iva8)
                    .iva0(iva0)
                    .ivaExento(ivaExento)
                    .ieps26(ieps26)
                    .ieps160(ieps160)
                    .ieps8(ieps8)
                    .ieps30(ieps30)
                    .ieps304(ieps304)
                    .ieps7(ieps7)
                    .ieps53(ieps53)
                    .ieps25(ieps25)
                    .ieps6(ieps6)
                    .ieps50(ieps50)
                    .ieps9(ieps9)
                    .ieps3(ieps3)
                    .ieps43(ieps43)
                    .build();
        } catch (Exception e) {
            return CfdiConsultaResponse.Basicos.builder().build();
        }
    }

    public String parseRfcReceptor(String xml) {
        try {
            Document doc = build(xml);
            Element receptor = getFirstByLocalName(doc, "Receptor");
            return attr(receptor, "Rfc");
        } catch (Exception e) {
            return null;
        }
    }

    public CfdiConsultaResponse.Relacionados parseRelacionados(String xml) {
        try {
            Document doc = build(xml);
            Element rels = getFirstByLocalName(doc, "CfdiRelacionados");
            if (rels == null) {
                return CfdiConsultaResponse.Relacionados.builder().uuids(new ArrayList<>()).build();
            }
            String tipoRel = attr(rels, "TipoRelacion");
            List<String> uuids = new ArrayList<>();
            String uuidOriginal = null;

            NodeList hijos = rels.getChildNodes();
            for (int i = 0; i < hijos.getLength(); i++) {
                Node n = hijos.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE && hasLocalName((Element) n, "CfdiRelacionado")) {
                    String u = attr((Element) n, "UUID");
                    if (u != null && !u.isEmpty()) {
                        uuids.add(u);
                    }
                }
            }
            if ("04".equals(tipoRel) && !uuids.isEmpty()) {
                uuidOriginal = uuids.get(0);
            }
            return CfdiConsultaResponse.Relacionados.builder()
                    .tipoRelacion(tipoRel)
                    .uuids(uuids)
                    .uuidOriginal(uuidOriginal)
                    .build();
        } catch (Exception e) {
            return CfdiConsultaResponse.Relacionados.builder().uuids(new ArrayList<>()).build();
        }
    }

    public CfdiConsultaResponse.Pago parseComplementoPago(String xml) {
        try {
            Document doc = build(xml);
            // Complemento -> Pagos (pago10 / pago20). Buscar por localName "Pagos"
            Element pagos = getFirstByLocalName(doc, "Pagos");
            if (pagos == null) {
                return null; // No es complemento de pago
            }
            Element pago = getFirstChildByLocalName(pagos, "Pago");
            if (pago == null) {
                return null;
            }
            String formaDePagoP = attr(pago, "FormaDePagoP");
            String fechaPago = attr(pago, "FechaPago");
            String monedaP = attr(pago, "MonedaP");
            BigDecimal monto = toBD(attr(pago, "Monto"));

            Element docto = getFirstChildByLocalName(pago, "DoctoRelacionado");
            if (docto == null) {
                // Algunos CFDI Pago pueden tener múltiples doctos; tomar el primero disponible
                NodeList hijos = pago.getChildNodes();
                for (int i = 0; i < hijos.getLength(); i++) {
                    Node n = hijos.item(i);
                    if (n.getNodeType() == Node.ELEMENT_NODE && hasLocalName((Element) n, "DoctoRelacionado")) {
                        docto = (Element) n;
                        break;
                    }
                }
            }

            String idDocumento = attr(docto, "IdDocumento");
            String serieDR = attr(docto, "Serie");
            String folioDR = attr(docto, "Folio");
            String monedaDR = attr(docto, "MonedaDR");
            String metodoDePagoDR = attr(docto, "MetodoDePagoDR");
            String numParcialidad = attr(docto, "NumParcialidad");
            BigDecimal impSaldoAnt = toBD(attr(docto, "ImpSaldoAnt"));
            BigDecimal impPagado = toBD(attr(docto, "ImpPagado"));
            BigDecimal impSaldoInsoluto = toBD(attr(docto, "ImpSaldoInsoluto"));

            return CfdiConsultaResponse.Pago.builder()
                    .formaDePagoP(formaDePagoP)
                    .fechaPago(fechaPago)
                    .monedaP(monedaP)
                    .monto(monto)
                    .idDocumento(idDocumento)
                    .serieDR(serieDR)
                    .folioDR(folioDR)
                    .monedaDR(monedaDR)
                    .metodoDePagoDR(metodoDePagoDR)
                    .numParcialidad(numParcialidad)
                    .impSaldoAnt(impSaldoAnt)
                    .impPagado(impPagado)
                    .impSaldoInsoluto(impSaldoInsoluto)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    // Helpers
    private Document build(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Element getFirstByLocalName(Document doc, String localName) {
        NodeList all = doc.getElementsByTagNameNS("*", localName);
        if (all.getLength() > 0) {
            return (Element) all.item(0);
        }
        // Fallback sin namespace
        NodeList nl = doc.getElementsByTagName(localName);
        return nl.getLength() > 0 ? (Element) nl.item(0) : null;
    }

    private Element getFirstChildByLocalName(Element parent, String localName) {
        if (parent == null) return null;
        NodeList hijos = parent.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node n = hijos.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if (hasLocalName(e, localName)) return e;
            }
        }
        return null;
    }

    private boolean hasLocalName(Element e, String name) {
        String ln = e.getLocalName();
        return name.equals(ln) || name.equals(e.getTagName());
    }

    private String attr(Element e, String name) {
        if (e == null) return null;
        return e.hasAttribute(name) ? e.getAttribute(name) : null;
    }

    private BigDecimal toBD(String v) {
        try {
            if (v == null || v.isEmpty()) return null;
            return new BigDecimal(v);
        } catch (Exception ex) {
            return null;
        }
    }
}