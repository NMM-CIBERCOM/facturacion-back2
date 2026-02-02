#!/bin/bash
# Script para configurar Tomcat y deshabilitar proxy automático

echo "=== Configurando Tomcat para deshabilitar proxy automático ==="

TOMCAT_BIN="/opt/tomcat/bin"
SETENV_FILE="$TOMCAT_BIN/setenv.sh"

# Crear setenv.sh si no existe
if [ ! -f "$SETENV_FILE" ]; then
    echo "Creando $SETENV_FILE..."
    touch "$SETENV_FILE"
    chmod +x "$SETENV_FILE"
fi

# Backup del archivo existente
if [ -f "$SETENV_FILE" ] && [ -s "$SETENV_FILE" ]; then
    echo "Haciendo backup de $SETENV_FILE..."
    cp "$SETENV_FILE" "${SETENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
fi

# Escribir configuración
cat > "$SETENV_FILE" << 'EOF'
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
EOF

chmod +x "$SETENV_FILE"

echo "✓ Archivo $SETENV_FILE configurado correctamente"
echo ""
echo "Para aplicar los cambios, reinicia Tomcat:"
echo "  sudo systemctl restart tomcat"
echo "  o"
echo "  sudo /etc/init.d/tomcat restart"
