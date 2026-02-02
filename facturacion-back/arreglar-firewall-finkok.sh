#!/bin/bash
# Script para permitir conexiones HTTPS salientes a Finkok
# Ejecutar en el servidor: bash arreglar-firewall-finkok.sh

echo "=========================================="
echo "SOLUCIONANDO FIREWALL PARA FINKOK"
echo "=========================================="
echo ""

# 1. Verificar estado actual
echo "1. Verificando estado del firewall..."
if command -v ufw &> /dev/null; then
    echo "   UFW detectado"
    sudo ufw status
elif command -v firewall-cmd &> /dev/null; then
    echo "   firewalld detectado"
    sudo firewall-cmd --list-all
else
    echo "   iptables detectado"
    sudo iptables -L OUTPUT -n -v | grep -E "(443|REJECT|DROP)" | head -5
fi
echo ""

# 2. Permitir HTTPS saliente
echo "2. Permitiendo conexiones HTTPS salientes (puerto 443)..."
if command -v ufw &> /dev/null; then
    sudo ufw allow out 443/tcp
    echo "   ✓ Regla agregada con UFW"
elif command -v firewall-cmd &> /dev/null; then
    sudo firewall-cmd --add-service=https --permanent
    sudo firewall-cmd --reload
    echo "   ✓ Regla agregada con firewalld"
else
    # Verificar si ya existe la regla
    if sudo iptables -C OUTPUT -p tcp --dport 443 -j ACCEPT 2>/dev/null; then
        echo "   ✓ Regla ya existe en iptables"
    else
        sudo iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT
        echo "   ✓ Regla agregada a iptables"
        
        # Intentar guardar permanentemente
        if [ -d /etc/iptables ]; then
            sudo iptables-save | sudo tee /etc/iptables/rules.v4 > /dev/null
            echo "   ✓ Reglas guardadas permanentemente"
        elif command -v netfilter-persistent &> /dev/null; then
            sudo netfilter-persistent save
            echo "   ✓ Reglas guardadas con netfilter-persistent"
        else
            echo "   ⚠ Regla agregada temporalmente. Para hacerla permanente:"
            echo "      sudo iptables-save | sudo tee /etc/iptables/rules.v4"
        fi
    fi
fi
echo ""

# 3. Verificar conectividad
echo "3. Verificando conectividad con Finkok..."
if timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" 2>/dev/null; then
    echo "   ✓ Puerto 443 ACCESIBLE"
else
    echo "   ✗ Puerto 443 AÚN NO ACCESIBLE"
    echo "   ⚠ Puede requerir reiniciar servicios de red o verificar proxy"
fi
echo ""

# 4. Probar con curl
echo "4. Probando conexión completa con curl..."
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -15
echo ""

echo "=========================================="
echo "FIN DE CONFIGURACIÓN"
echo "=========================================="
echo ""
echo "Si la conectividad funciona ahora:"
echo "1. Reinicia Tomcat: sudo systemctl restart tomcat"
echo "2. Prueba generar una factura desde la aplicación"
echo ""
