# Configurar Tomcat para Deshabilitar Proxy Automático

## Problema

El stack trace muestra `SocksSocketImpl.connect`, lo que indica que Java está detectando y usando un proxy SOCKS automáticamente.

## Solución: Configurar Tomcat

Crea o edita el archivo `/opt/tomcat/bin/setenv.sh`:

```bash
sudo nano /opt/tomcat/bin/setenv.sh
```

**Agregar o reemplazar con:**

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
export JAVA_OPTS="$JAVA_OPTS -Dftp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dftp.proxyPort="

# Forzar IPv4
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv6Addresses=false"
```

**Dar permisos:**

```bash
sudo chmod +x /opt/tomcat/bin/setenv.sh
```

**Reiniciar Tomcat:**

```bash
sudo systemctl restart tomcat
# O
sudo /etc/init.d/tomcat restart
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
3. **Revisa los logs** - deberías ver "Conexión directa sin proxy" y NO debería aparecer `SocksSocketImpl` en el stack trace
