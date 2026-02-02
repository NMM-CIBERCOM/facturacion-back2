# Solución: Bloqueo Específico de Finkok

## Diagnóstico Final

✅ **HTTPS saliente funciona** (Google y GitHub responden)
✅ **Firewall local:** OK
✅ **Conectividad a Internet:** OK
❌ **Finkok específicamente:** Timeout

## Causa Probable

Finkok está **bloqueando la IP del servidor** (`72.62.128.98`) o hay restricciones específicas.

## Soluciones

### Opción 1: Contactar a Finkok (RECOMENDADO)

Contacta al soporte de Finkok y solicita:

1. **Verificar si la IP está bloqueada:**
   - IP del servidor: `72.62.128.98`
   - Servidor: `demo-facturacion.finkok.com`

2. **Solicitar whitelist de la IP** si está bloqueada

3. **Verificar restricciones de red:**
   - Si hay restricciones geográficas
   - Si hay restricciones de proveedor

**Información de contacto de Finkok:**
- Email: soporte@finkok.com
- Teléfono: Verificar en su sitio web
- Portal: https://facturacion.finkok.com

### Opción 2: Usar URL de Producción

Si tienes acceso a la cuenta de producción de Finkok, prueba con la URL de producción en lugar de demo:

```yaml
# En application.yml, cambiar:
finkok:
  stamp:
    url: https://facturacion.finkok.com/servicios/soap/stamp  # Producción
  cancel:
    url: https://facturacion.finkok.com/servicios/soap/cancel  # Producción
```

La URL de producción puede tener menos restricciones.

### Opción 3: Verificar desde Otra Red

Prueba acceder a Finkok desde otra red/IP para confirmar si es un bloqueo específico de esta IP.

### Opción 4: Usar Proxy/VPN

Si Finkok está bloqueando la IP, puedes usar un proxy:

1. Configurar un proxy HTTP/HTTPS
2. Configurar la aplicación Java para usar el proxy
3. O usar una VPN

## Verificaciones Adicionales

### Probar Traceroute

```bash
# Ver la ruta hacia Finkok
traceroute demo-facturacion.finkok.com 2>&1 | head -20

# O con mtr si está instalado
mtr demo-facturacion.finkok.com
```

Esto puede mostrar dónde se pierde la conexión.

### Probar con wget

```bash
# Probar con wget para ver si es específico de curl
wget --spider --timeout=10 https://demo-facturacion.finkok.com/servicios/soap/stamp
```

### Verificar IPs de Finkok

```bash
# Ver todas las IPs de Finkok
nslookup demo-facturacion.finkok.com
dig demo-facturacion.finkok.com +short

# Probar conectividad directa a la IP
timeout 5 bash -c "</dev/tcp/69.160.41.169/443" && echo "✓ IP accesible" || echo "✗ IP NO accesible"
```

## Configuración de Proxy en Java (si es necesario)

Si decides usar un proxy, configura en Tomcat (`setenv.sh`):

```bash
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort=8080"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort=8080"
```

O modifica `PacClient.java` para usar proxy programáticamente.

## Próximos Pasos

1. **Contacta a Finkok** para verificar bloqueo de IP
2. **Prueba con URL de producción** si tienes acceso
3. **Verifica traceroute** para ver dónde se pierde la conexión
4. **Considera usar proxy** si Finkok confirma el bloqueo

## Nota Importante

Este es un problema de **política de red de Finkok**, no de tu aplicación. Necesitas coordinarte con Finkok para resolverlo.
