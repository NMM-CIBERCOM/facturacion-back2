# Verificar Configuración de Tomcat

## Problema

El stack trace sigue mostrando `SocksSocketImpl.connect` aunque el código dice "Conexión directa sin proxy". Esto significa que Java todavía está usando proxy SOCKS automáticamente.

## Verificación

Ejecuta estos comandos en la terminal de WinSCP para verificar:

### 1. Verificar que setenv.sh existe y tiene el contenido correcto:

```bash
cat /opt/tomcat/bin/setenv.sh
```

**Deberías ver:**
```bash
#!/bin/sh
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
...
```

### 2. Verificar que Tomcat está usando las opciones de Java:

```bash
ps aux | grep tomcat | grep java
```

**Deberías ver** `-Djava.net.useSystemProxies=false` en la línea de comandos de Java.

### 3. Verificar variables de proxy del sistema:

```bash
env | grep -i proxy
```

Si hay variables como `http_proxy`, `https_proxy`, `socks_proxy`, etc., esas están forzando el uso de proxy.

### 4. Verificar /etc/environment:

```bash
cat /etc/environment | grep -i proxy
```

## Si setenv.sh NO existe o está vacío

Ejecuta este comando para crearlo:

```bash
sudo bash -c 'cat > /opt/tomcat/bin/setenv.sh << "EOF"
#!/bin/sh
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -DsocksProxyHost="
export JAVA_OPTS="$JAVA_OPTS -DsocksProxyPort="
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort="
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv6Addresses=false"
EOF' && sudo chmod +x /opt/tomcat/bin/setenv.sh && echo "Archivo creado"
```

Luego **REINICIA Tomcat**:

```bash
sudo systemctl restart tomcat
```

## Si hay variables de proxy en el sistema

Si encuentras variables de proxy, necesitas eliminarlas o comentarlas:

```bash
# Ver qué variables hay
env | grep -i proxy

# Si hay variables, editar /etc/environment
sudo bash -c 'cat /etc/environment | grep -v -i proxy > /tmp/environment_clean && sudo mv /tmp/environment_clean /etc/environment'
```

Luego reinicia el servidor o al menos reinicia Tomcat.

## Verificar después de reiniciar

Después de reiniciar Tomcat, verifica nuevamente:

```bash
ps aux | grep tomcat | grep java
```

Deberías ver las opciones `-Djava.net.useSystemProxies=false` en el proceso de Java.
