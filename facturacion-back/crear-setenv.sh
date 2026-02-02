#!/bin/bash
# Script simple para crear setenv.sh en Tomcat

cat > /opt/tomcat/bin/setenv.sh << 'ENDOFFILE'
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
ENDOFFILE

chmod +x /opt/tomcat/bin/setenv.sh
echo "Archivo setenv.sh creado correctamente"
