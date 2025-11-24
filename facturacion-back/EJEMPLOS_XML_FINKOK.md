# Ejemplos de XML CFDI 3.3 para Finkok Quick Stamp

## Credenciales Finkok
- **Usuario**: integrador@finkok.com
- **Contraseña**: Fin2023kok*
- **URL**: https://demo-facturacion.finkok.com/servicios/soap/stamp

## RFCs Disponibles

### Emisores
1. **INNOVACION VALOR Y DESARROLLO SA**
   - RFC: IVD920810GU2
   - CP: 58000

2. **CECILIA MIRANDA SANCHEZ**
   - RFC: MISC491214B86
   - CP: 80290

### Receptores
1. **CESAR OSBALDO CRUZ SOLORZANO** (Persona Física)
   - RFC: CUSC850516316
   - Email: imenam@redcibercom.com.mx
   - Regímenes: 605, 606, 607, 608, 610, 611, 612, 614, 615, 616, 621, 625, 626

2. **INMOBILIARIA CVA** (Persona Moral)
   - RFC: ICV060329BY0
   - Email: lcancelah@redcibercom.com.mx
   - Regímenes: 601, 603, 610, 620, 622, 623, 624, 626

3. **John Kennedy** (Extranjero)
   - RFC: XEXX010101000
   - Email: nmirandam@redcibercom.com.mx

---

## Ejemplo 1: Emisor IVD920810GU2 → Receptor CUSC850516316

### XML (formato legible)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<cfdi:Comprobante xmlns:cfdi="http://www.sat.gob.mx/cfd/3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" Version="3.3" Fecha="2025-11-19T15:00:00" FormaPago="03" LugarExpedicion="58000" MetodoPago="PUE" Moneda="MXN" SubTotal="1000.00" TipoDeComprobante="I" Total="1160.00" xsi:schemaLocation="http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd">
    <cfdi:Emisor Nombre="INNOVACION VALOR Y DESARROLLO SA" RegimenFiscal="601" Rfc="IVD920810GU2"/>
    <cfdi:Receptor Nombre="CESAR OSBALDO CRUZ SOLORZANO" Rfc="CUSC850516316" UsoCFDI="G01"/>
    <cfdi:Conceptos>
        <cfdi:Concepto Cantidad="1" ClaveProdServ="81112100" ClaveUnidad="E20" Descripcion="Servicio de consultoría" Importe="1000.00" ValorUnitario="1000.00">
            <cfdi:Impuestos>
                <cfdi:Traslados>
                    <cfdi:Traslado Base="1000.00" Importe="160.00" Impuesto="002" TasaOCuota="0.160000" TipoFactor="Tasa"/>
                </cfdi:Traslados>
            </cfdi:Impuestos>
        </cfdi:Concepto>
    </cfdi:Conceptos>
    <cfdi:Impuestos TotalImpuestosTrasladados="160.00">
        <cfdi:Traslados>
            <cfdi:Traslado Importe="160.00" Impuesto="002" TasaOCuota="0.160000" TipoFactor="Tasa"/>
        </cfdi:Traslados>
    </cfdi:Impuestos>
</cfdi:Comprobante>
```

### JSON para Postman (POST /api/factura/test-finkok)
```json
{
  "xmlContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Version=\"3.3\" Fecha=\"2025-11-19T15:00:00\" FormaPago=\"03\" LugarExpedicion=\"58000\" MetodoPago=\"PUE\" Moneda=\"MXN\" SubTotal=\"1000.00\" TipoDeComprobante=\"I\" Total=\"1160.00\" xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd\"><cfdi:Emisor Nombre=\"INNOVACION VALOR Y DESARROLLO SA\" RegimenFiscal=\"601\" Rfc=\"IVD920810GU2\"/><cfdi:Receptor Nombre=\"CESAR OSBALDO CRUZ SOLORZANO\" Rfc=\"CUSC850516316\" UsoCFDI=\"G01\"/><cfdi:Conceptos><cfdi:Concepto Cantidad=\"1\" ClaveProdServ=\"81112100\" ClaveUnidad=\"E20\" Descripcion=\"Servicio de consultoría\" Importe=\"1000.00\" ValorUnitario=\"1000.00\"><cfdi:Impuestos><cfdi:Traslados><cfdi:Traslado Base=\"1000.00\" Importe=\"160.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Concepto></cfdi:Conceptos><cfdi:Impuestos TotalImpuestosTrasladados=\"160.00\"><cfdi:Traslados><cfdi:Traslado Importe=\"160.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Comprobante>"
}
```

---

## Ejemplo 2: Emisor IVD920810GU2 → Receptor ICV060329BY0 (Persona Moral)

### JSON para Postman
```json
{
  "xmlContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Version=\"3.3\" Fecha=\"2025-11-19T15:00:00\" FormaPago="03" LugarExpedicion=\"58000\" MetodoPago=\"PUE\" Moneda=\"MXN\" SubTotal=\"2000.00\" TipoDeComprobante=\"I\" Total=\"2320.00\" xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd\"><cfdi:Emisor Nombre=\"INNOVACION VALOR Y DESARROLLO SA\" RegimenFiscal=\"601\" Rfc=\"IVD920810GU2\"/><cfdi:Receptor Nombre=\"INMOBILIARIA CVA\" Rfc=\"ICV060329BY0\" UsoCFDI=\"G01\"/><cfdi:Conceptos><cfdi:Concepto Cantidad=\"1\" ClaveProdServ=\"84111506\" ClaveUnidad=\"ACT\" Descripcion=\"Servicios profesionales\" Importe=\"2000.00\" ValorUnitario=\"2000.00\"><cfdi:Impuestos><cfdi:Traslados><cfdi:Traslado Base=\"2000.00\" Importe=\"320.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Concepto></cfdi:Conceptos><cfdi:Impuestos TotalImpuestosTrasladados=\"320.00\"><cfdi:Traslados><cfdi:Traslado Importe=\"320.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Comprobante>"
}
```

---

## Ejemplo 3: Emisor MISC491214B86 → Receptor XEXX010101000 (Extranjero)

### JSON para Postman
```json
{
  "xmlContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Version=\"3.3\" Fecha=\"2025-11-19T15:00:00\" FormaPago=\"03\" LugarExpedicion=\"80290\" MetodoPago=\"PUE\" Moneda=\"MXN\" SubTotal=\"1500.00\" TipoDeComprobante=\"I\" Total=\"1740.00\" xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd\"><cfdi:Emisor Nombre=\"CECILIA MIRANDA SANCHEZ\" RegimenFiscal=\"605\" Rfc=\"MISC491214B86\"/><cfdi:Receptor Nombre=\"John Kennedy\" Rfc=\"XEXX010101000\" UsoCFDI=\"S01\"/><cfdi:Conceptos><cfdi:Concepto Cantidad=\"1\" ClaveProdServ=\"84111506\" ClaveUnidad=\"ACT\" Descripcion=\"Servicio profesional\" Importe=\"1500.00\" ValorUnitario=\"1500.00\"><cfdi:Impuestos><cfdi:Traslados><cfdi:Traslado Base=\"1500.00\" Importe=\"240.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Concepto></cfdi:Conceptos><cfdi:Impuestos TotalImpuestosTrasladados=\"240.00\"><cfdi:Traslados><cfdi:Traslado Importe=\"240.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Comprobante>"
}
```

---

## Instrucciones para Postman

### Endpoint
```
POST http://localhost:8080/api/factura/test-finkok
```

### Headers
```
Content-Type: application/json
```

### Body (raw JSON)
Usa cualquiera de los ejemplos JSON de arriba.

---

## Notas Importantes

1. **Sin atributos vacíos**: El XML NO debe tener `Certificado=""`, `Sello=""`, `NoCertificado=""` antes del timbrado
2. **Fecha actual**: Asegúrate de usar una fecha actual (no futura)
3. **Encoding UTF-8**: El XML debe tener `encoding="UTF-8"`
4. **Namespaces correctos**: Debe incluir `xmlns:cfdi="http://www.sat.gob.mx/cfd/3"`
5. **Base64**: El backend codificará el XML a Base64 automáticamente antes de enviarlo a Finkok

---

## Validación

Si recibes error 705, valida el XML en:
- https://validador.finkok.com/fixschema/

