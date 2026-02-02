# Verificar y Deshabilitar Proxy del Sistema

## Problema Identificado

El stack trace muestra `SocksSocketImpl.connect`, lo que indica que **Java está detectando y usando un proxy SOCKS automáticamente** del sistema.

## Verificación en el Servidor

Ejecuta estos comandos en la terminal de WinSCP para verificar si hay proxy configurado:

```bash
# 1. Verificar variables de proxy del sistema
env | grep -i proxy

# 2. Verificar configuración en /etc/environment
cat /etc/environment | grep -i proxy

# 3. Verificar si Java detecta proxy
java -XshowSettings:properties -version 2>&1 | grep -i proxy

# 4. Verificar configuración de red
cat /etc/resolv.conf

# 5. Verificar si hay proxy SOCKS configurado
echo $SOCKS_PROXY
echo $socks_proxy
```

## Si Hay Proxy Configurado

Si encuentras variables de proxy, necesitas deshabilitarlas o configurarlas correctamente.

### Opción 1: Deshabilitar Variables de Proxy

```bash
# Editar /etc/environment
sudo nano /etc/environment

# Comentar o eliminar líneas que contengan:
# http_proxy=...
# https_proxy=...
# socks_proxy=...
```

### Opción 2: Configurar Java para Ignorar Proxy

Ya está implementado en el código, pero también puedes configurarlo en Tomcat:

```bash
# Editar /opt/tomcat/bin/setenv.sh
sudo nano /opt/tomcat/bin/setenv.sh
```

Agregar:

```bash
#!/bin/sh
# Deshabilitar completamente proxy automático
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -DsocksProxyHost="
export JAVA_OPTS="$JAVA_OPTS -DsocksProxyPort="
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort="
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv6Addresses=false"
```

## Cambios Implementados en el Código

He agregado configuración en el constructor de `PacClient` para deshabilitar proxy al inicio. Esto debería ayudar, pero si hay variables de entorno del sistema, también necesitas deshabilitarlas.

## Próximos Pasos

1. **Ejecuta los comandos de verificación** para ver si hay proxy configurado
2. **Si hay proxy**, deshabilítalo o configúralo correctamente
3. **Recompila y redespliega** con los cambios del constructor
4. **Verifica los logs** - deberías ver "Proxy automático deshabilitado al inicializar PacClient"
