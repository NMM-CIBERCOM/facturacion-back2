# Instrucciones para Probar Finkok con Postman

## Opción 1: Enviar XML Directo (Recomendado)

### Endpoint
```
POST http://localhost:8080/api/factura/test-finkok-xml
```

### Headers
```
Content-Type: application/xml
```

### Body (raw - XML)
Selecciona "raw" y luego "XML" en Postman, y pega este XML:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<cfdi:Comprobante xmlns:cfdi="http://www.sat.gob.mx/cfd/3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" Version="3.3" Fecha="2024-11-19T14:00:00" FormaPago="03" LugarExpedicion="58000" MetodoPago="PUE" Moneda="MXN" SubTotal="1000.00" TipoDeComprobante="I" Total="1160.00" xsi:schemaLocation="http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd">
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

**Ventajas:**
- ✅ Más simple: envías el XML directamente
- ✅ No necesitas escapar comillas
- ✅ Más fácil de leer y editar

---

## Opción 2: Enviar JSON con XML (Original)

### Endpoint
```
POST http://localhost:8080/api/factura/test-finkok
```

### Headers
```
Content-Type: application/json
```

### Body (raw - JSON)
```json
{
  "xmlContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Version=\"3.3\" Fecha=\"2024-11-19T14:00:00\" FormaPago=\"03\" LugarExpedicion=\"58000\" MetodoPago=\"PUE\" Moneda=\"MXN\" SubTotal=\"1000.00\" TipoDeComprobante=\"I\" Total=\"1160.00\" xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd\"><cfdi:Emisor Nombre=\"INNOVACION VALOR Y DESARROLLO SA\" RegimenFiscal=\"601\" Rfc=\"IVD920810GU2\"/><cfdi:Receptor Nombre=\"CESAR OSBALDO CRUZ SOLORZANO\" Rfc=\"CUSC850516316\" UsoCFDI=\"G01\"/><cfdi:Conceptos><cfdi:Concepto Cantidad=\"1\" ClaveProdServ=\"81112100\" ClaveUnidad=\"E20\" Descripcion=\"Servicio de consultoría\" Importe=\"1000.00\" ValorUnitario=\"1000.00\"><cfdi:Impuestos><cfdi:Traslados><cfdi:Traslado Base=\"1000.00\" Importe=\"160.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Concepto></cfdi:Conceptos><cfdi:Impuestos TotalImpuestosTrasladados=\"160.00\"><cfdi:Traslados><cfdi:Traslado Importe=\"160.00\" Impuesto=\"002\" TasaOCuota=\"0.160000\" TipoFactor=\"Tasa\"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Comprobante>"
}
```

---

## Respuesta Esperada (Éxito)

```json
{
  "ok": true,
  "status": "TIMBRADO",
  "codEstatus": "Comprobante timbrado satisfactoriamente",
  "uuid": "XXXX-XXXX-XXXX-XXXX",
  "fecha": "2024-11-19T14:00:00",
  "satSeal": "...",
  "noCertificadoSAT": "...",
  "xmlTimbrado": "...",
  "message": null,
  "codigoError": null,
  "mensajeIncidencia": null
}
```

## Respuesta Esperada (Error 705)

```json
{
  "ok": false,
  "status": "ERROR",
  "codEstatus": null,
  "uuid": null,
  "fecha": null,
  "satSeal": null,
  "noCertificadoSAT": null,
  "xmlTimbrado": null,
  "message": "XML Estructura inválida",
  "codigoError": "705",
  "mensajeIncidencia": "XML Estructura inválida"
}
```

---

## Si Recibes Error 705

1. **Revisa los logs del backend** y busca:
   ```
   === XML FINAL A TIMBRAR ===
   XML completo: ...
   === FIN XML FINAL ===
   ```

2. **Copia el XML** que aparece en los logs

3. **Valida el XML** en: https://validador.finkok.com/fixschema/

4. **Comparte el XML** de los logs para que pueda identificar el problema exacto

---

## Notas Importantes

- ✅ El XML **NO debe tener** atributos vacíos como `Certificado=""`, `Sello=""`, `NoCertificado=""` (el backend los elimina automáticamente)
- ✅ La fecha **NO debe ser futura** (el backend la corrige automáticamente si es más de 5 minutos en el futuro)
- ✅ El XML debe tener `encoding="UTF-8"`
- ✅ El XML debe incluir `xmlns:cfdi="http://www.sat.gob.mx/cfd/3"`
- ✅ El XML debe tener `Version="3.3"`

---

## Recomendación

**Usa la Opción 1 (XML directo)** porque es más simple y fácil de depurar. Solo copia y pega el XML directamente en Postman.

