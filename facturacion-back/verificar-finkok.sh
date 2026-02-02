#!/bin/bash
# Script para verificar conectividad con Finkok
# Ejecutar en el servidor: bash verificar-finkok.sh

echo "=========================================="
echo "VERIFICACIÓN DE CONECTIVIDAD CON FINKOK"
echo "=========================================="
echo ""

echo "1. Verificando resolución DNS..."
nslookup demo-facturacion.finkok.com
echo ""

echo "2. Verificando conectividad al puerto 443..."
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" 2>/dev/null && echo "✓ Puerto 443 ACCESIBLE" || echo "✗ Puerto 443 NO ACCESIBLE"
echo ""

echo "3. Verificando con curl (con timeout de 10 segundos)..."
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -30
echo ""

echo "4. Verificando firewall (reglas OUTPUT)..."
sudo iptables -L OUTPUT -n -v 2>/dev/null | grep -E "(443|REJECT|DROP)" || echo "No se encontraron reglas bloqueando 443"
echo ""

echo "5. Verificando acceso general a Internet..."
ping -c 3 8.8.8.8 2>&1 | head -5
echo ""

echo "=========================================="
echo "FIN DE VERIFICACIÓN"
echo "=========================================="
