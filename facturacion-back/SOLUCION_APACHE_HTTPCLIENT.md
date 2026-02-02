# Solución: Migración a Apache HttpClient

## Problema
El código estaba usando `HttpURLConnection` que automáticamente detecta y usa proxies del sistema, causando problemas cuando el servidor tiene configuraciones de proxy que interfieren con las conexiones a Finkok.

## Solución Implementada
Se migró el código para usar **Apache HttpClient 5**, que:
- No detecta automáticamente proxies del sistema
- Tiene mejor control sobre las conexiones
- Maneja mejor los timeouts y errores de conexión
- Es más robusto para conexiones HTTPS

## Cambios Realizados

### 1. Dependencia Agregada
Se agregó Apache HttpClient 5 al `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>
```

### 2. Métodos Actualizados
- `sendSOAPRequest()`: Ahora usa `CloseableHttpClient` y `HttpPost`
- `sendCancelSOAPRequest()`: También migrado a Apache HttpClient

### 3. Características
- **Sin detección automática de proxy**: `System.setProperty("java.net.useSystemProxies", "false")`
- **Timeouts configurables**: Usa `RequestConfig` con timeouts personalizables
- **Mejor manejo de errores**: Captura `IOException` y proporciona mensajes detallados
- **Conexión directa**: No usa proxies intermedios

## Ventajas
1. ✅ Evita problemas de proxy automático
2. ✅ Mejor control sobre las conexiones
3. ✅ Manejo más robusto de errores
4. ✅ Compatible con la configuración existente

## Próximos Pasos
1. Compilar el proyecto: `mvn clean install`
2. Desplegar en el servidor
3. Probar timbrado y cancelación
4. Si aún hay problemas de conectividad, verificar:
   - Firewall del servidor
   - Reglas de red
   - Conectividad desde el servidor: `curl -v https://demo-facturacion.finkok.com/servicios/soap/stamp`

## Nota Importante
Si el problema persiste después de esta migración (error "Connect timed out"), **es un problema de firewall o conectividad de red desde el servidor, NO un problema del código**.

### Diagnóstico del Problema Actual
El error "Connect timed out" después de migrar a Apache HttpClient indica que:
1. ✅ El código está funcionando correctamente (no detecta proxies automáticamente)
2. ❌ El servidor **NO puede conectarse** a `demo-facturacion.finkok.com:443` debido a:
   - Firewall bloqueando conexiones HTTPS salientes (puerto 443)
   - Reglas de red del proveedor de hosting
   - Problemas de conectividad de red del servidor

### Soluciones Recomendadas
1. **Verificar conectividad desde el servidor:**
   ```bash
   curl -v --connect-timeout 10 https://demo-facturacion.finkok.com/servicios/soap/stamp
   ```

2. **Si curl también falla:** El problema es de red/firewall del servidor, no del código.

3. **Si curl funciona pero el código no:** Verificar configuración de Java/JVM.

4. **Alternativa:** Usar un túnel SSH desde otro servidor que SÍ tenga acceso a Finkok (ver `SOLUCION_FINAL_TUNEL.md`).
