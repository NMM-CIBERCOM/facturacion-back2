# Solución: Deshabilitar Proxy Automático de Java

## Problema Identificado

En los logs veo que Java está intentando usar `SocksSocketImpl`, lo que indica que **Java está detectando automáticamente un proxy** del sistema y tratando de usarlo, causando el timeout.

## Cambios Implementados

He modificado el código para:
1. ✅ **Deshabilitar detección automática de proxy** (`java.net.useSystemProxies=false`)
2. ✅ **Forzar conexión directa** usando `Proxy.NO_PROXY`
3. ✅ **Mejorar configuración IPv4**

## Próximos Pasos

### Paso 1: Recompilar

```bash
mvn clean package
```

### Paso 2: Redesplegar

Sube el nuevo WAR y reinicia Tomcat.

### Paso 3: Verificar Logs

Después de redesplegar, intenta generar una factura y revisa los logs. Deberías ver:
- "Conexión directa sin proxy" (en lugar de intentar usar proxy)
- Ya no debería aparecer `SocksSocketImpl` en el stack trace

## Si Aún No Funciona

Si después de estos cambios sigue sin funcionar, el problema definitivamente es de **infraestructura de red externa**:

1. **Contacta al proveedor del servidor** para que configuren el firewall
2. **Contacta a Finkok** para verificar si la IP está bloqueada
3. **Verifica si el servidor anterior tenía VPN o túnel** configurado

## Verificar Variables de Proxy del Sistema

Ejecuta en el servidor para verificar si hay proxy configurado:

```bash
# Verificar variables de proxy
env | grep -i proxy

# Verificar configuración de red
cat /etc/environment | grep -i proxy

# Verificar si Java detecta proxy
java -XshowSettings:properties -version 2>&1 | grep -i proxy
```

Si hay variables de proxy configuradas, Java las usará automáticamente. Con los cambios implementados, ahora las ignorará.
