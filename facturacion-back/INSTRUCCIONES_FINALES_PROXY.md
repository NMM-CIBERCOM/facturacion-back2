# Solución Final: Deshabilitar Proxy SOCKS en Tomcat

## Problema

El stack trace muestra `SocksSocketImpl.connect`, lo que indica que **Java está detectando y usando un proxy SOCKS automáticamente** del sistema, incluso aunque el código use `Proxy.NO_PROXY`.

## Solución: Configurar Tomcat

El problema **NO se puede resolver solo desde el código** porque Java detecta el proxy antes de que la aplicación inicie. Necesitas configurar Tomcat.

### Opción 1: Usar el Script (Recomendado)

1. **Sube el archivo `configurar-tomcat-no-proxy.sh` al servidor** usando WinSCP
2. **Ejecuta en la terminal de WinSCP:**

```bash
cd /ruta/donde/está/el/script
sudo bash configurar-tomcat-no-proxy.sh
```

3. **Reinicia Tomcat:**

```bash
sudo systemctl restart tomcat
```

### Opción 2: Configuración Manual

1. **Crea/edita el archivo `/opt/tomcat/bin/setenv.sh`:**

```bash
sudo nano /opt/tomcat/bin/setenv.sh
```

2. **Agrega este contenido:**

```bash
#!/bin/sh
# Deshabilitar completamente proxy automático de Java
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -DsocksProxyHost="
export JAVA_OPTS="$JAVA_OPTS -DsocksProxyPort="
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort="

# Forzar IPv4
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv6Addresses=false"
```

3. **Da permisos:**

```bash
sudo chmod +x /opt/tomcat/bin/setenv.sh
```

4. **Reinicia Tomcat:**

```bash
sudo systemctl restart tomcat
```

## Verificar Variables de Proxy del Sistema

Ejecuta estos comandos para verificar si hay proxy configurado:

```bash
# Verificar variables de proxy
env | grep -i proxy

# Verificar /etc/environment
cat /etc/environment | grep -i proxy

# Si hay variables, comentarlas o eliminarlas
sudo nano /etc/environment
```

## Después de Configurar

1. **Reinicia Tomcat**
2. **Intenta generar una factura**
3. **Revisa los logs** - NO debería aparecer `SocksSocketImpl` en el stack trace
4. **Deberías ver** "Conexión directa sin proxy" en los logs

## Nota Importante

**Este problema NO se puede resolver solo desde el código Java** porque:
- Java detecta el proxy SOCKS del sistema **antes** de que la aplicación inicie
- Las propiedades del sistema se configuran **después** de que Java ya detectó el proxy
- `Proxy.NO_PROXY` no previene que Java use `SocksSocketImpl` si hay un proxy detectado

**La única solución efectiva es configurar Tomcat** para que Java no use proxy desde el inicio.
