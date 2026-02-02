# Configurar Tomcat para Deshabilitar Proxy SOCKS

## Pasos a Seguir

### Opción A: Usar el Script (Más Fácil)

1. **Sube el archivo `crear-setenv.sh` al servidor** usando WinSCP (a cualquier carpeta, por ejemplo `/tmp` o `/home/root`)

2. **En la terminal de WinSCP, ejecuta:**

```bash
sudo bash /ruta/donde/está/crear-setenv.sh
```

Por ejemplo, si lo subiste a `/tmp`:
```bash
sudo bash /tmp/crear-setenv.sh
```

3. **Verifica que se creó:**

```bash
cat /opt/tomcat/bin/setenv.sh
```

4. **Reinicia Tomcat:**

```bash
sudo systemctl restart tomcat
```

O si no funciona systemctl:
```bash
sudo /etc/init.d/tomcat restart
```

### Opción B: Comandos Directos (Sin Script)

Si prefieres no usar el script, ejecuta estos comandos UNO POR UNO en la terminal de WinSCP:

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
export JAVA_OPTS="$JAVA_OPTS -Dftp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dftp.proxyPort="
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv6Addresses=false"
EOF'
```

Luego:

```bash
sudo chmod +x /opt/tomcat/bin/setenv.sh
sudo systemctl restart tomcat
```

## Verificar que Funcionó

1. **Revisa los logs de Tomcat** después de reiniciar
2. **Intenta generar una factura**
3. **Revisa el stack trace** - NO debería aparecer `SocksSocketImpl`
4. **Deberías ver** "Conexión directa sin proxy" en los logs

## Si Sigue Fallando

Si después de configurar Tomcat sigue apareciendo `SocksSocketImpl`, verifica si hay variables de proxy en el sistema:

```bash
env | grep -i proxy
cat /etc/environment | grep -i proxy
```

Si hay variables de proxy, coméntalas o elimínalas en `/etc/environment`.
