# Cómo Diagnosticar el Error 705 de Finkok

## Paso 1: Revisar los Logs del Backend

Después de enviar el request por Postman, busca en los logs del backend estas líneas:

```
=== XML FINAL A TIMBRAR ===
XML completo: ...
=== FIN XML FINAL ===
```

**Copia el XML completo** que aparece ahí.

## Paso 2: Validar el XML en Finkok

1. Ve a: https://validador.finkok.com/fixschema/
2. Pega el XML que copiaste de los logs
3. Haz clic en "Validar"
4. El validador te dirá **exactamente** qué está mal

## Paso 3: Verificar el SOAP Request

En los logs también deberías ver:

```
=== SOAP REQUEST COMPLETO ===
...
=== FIN SOAP REQUEST ===
```

Verifica que el SOAP tenga esta estructura:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:stam="http://facturacion.finkok.com/stamp">
   <soapenv:Header/>
   <soapenv:Body>
      <stam:quick_stamp>
         <stam:xml>BASE64_XML_AQUI</stam:xml>
         <stam:username>integrador@finkok.com</stam:username>
         <stam:password>Fin2023kok*</stam:password>
      </stam:quick_stamp>
   </soapenv:Body>
</soapenv:Envelope>
```

## Problemas Comunes que Causan Error 705

1. **Atributos vacíos**: `Certificado=""`, `Sello=""`, `NoCertificado=""` - El código los elimina automáticamente
2. **Fecha futura**: El código la corrige automáticamente
3. **Espacios en blanco**: El código los normaliza automáticamente
4. **Estructura XML inválida**: Necesita validación manual
5. **Encoding incorrecto**: Debe ser UTF-8
6. **Namespaces incorrectos**: Debe incluir `xmlns:cfdi="http://www.sat.gob.mx/cfd/3"`

## XML de Prueba Mínimo Válido

Este XML debería funcionar:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<cfdi:Comprobante xmlns:cfdi="http://www.sat.gob.mx/cfd/3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" Version="3.3" Fecha="2024-11-19T14:00:00" FormaPago="03" LugarExpedicion="58000" MetodoPago="PUE" Moneda="MXN" SubTotal="1000.00" TipoDeComprobante="I" Total="1160.00" xsi:schemaLocation="http://www.sat.gob.mx/cfd/3 http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd"><cfdi:Emisor Nombre="INNOVACION VALOR Y DESARROLLO SA" RegimenFiscal="601" Rfc="IVD920810GU2"/><cfdi:Receptor Nombre="CESAR OSBALDO CRUZ SOLORZANO" Rfc="CUSC850516316" UsoCFDI="G01"/><cfdi:Conceptos><cfdi:Concepto Cantidad="1" ClaveProdServ="81112100" ClaveUnidad="E20" Descripcion="Servicio de consultoría" Importe="1000.00" ValorUnitario="1000.00"><cfdi:Impuestos><cfdi:Traslados><cfdi:Traslado Base="1000.00" Importe="160.00" Impuesto="002" TasaOCuota="0.160000" TipoFactor="Tasa"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Concepto></cfdi:Conceptos><cfdi:Impuestos TotalImpuestosTrasladados="160.00"><cfdi:Traslados><cfdi:Traslado Importe="160.00" Impuesto="002" TasaOCuota="0.160000" TipoFactor="Tasa"/></cfdi:Traslados></cfdi:Impuestos></cfdi:Comprobante>
```

**Nota**: Este XML está en una sola línea (sin saltos de línea) para evitar problemas con espacios en blanco.

## Acción Inmediata

1. **Envía el request** por Postman con el XML
2. **Revisa los logs** del backend y busca `=== XML FINAL A TIMBRAR ===`
3. **Copia el XML** que aparece en los logs
4. **Valida el XML** en https://validador.finkok.com/fixschema/
5. **Comparte el resultado** de la validación o el XML de los logs

Con esa información podré identificar el problema exacto.

