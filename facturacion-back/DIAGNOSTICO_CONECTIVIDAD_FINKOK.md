# Diagnóstico de Conectividad a Finkok

## Problema Real

El `curl` también da timeout, lo que significa que **el servidor NO puede conectarse a Finkok**. No es un problema de proxy, sino de **conectividad de red/firewall**.

## Diagnóstico

Ejecuta estos comandos para diagnosticar:

### 1. Verificar si el puerto 443 está bloqueado:

```bash
# Probar conexión TCP al puerto 443
timeout 5 bash -c 'cat < /dev/null > /dev/tcp/69.160.41.169/443' && echo "✓ Puerto 443 accesible" || echo "✗ Puerto 443 bloqueado"
```

### 2. Verificar firewall (UFW):

```bash
# Ver reglas de firewall
sudo ufw status verbose

# Verificar si hay reglas que bloqueen salida
sudo ufw status numbered | grep -i out
```

### 3. Verificar iptables (firewall del sistema):

```bash
# Ver reglas de iptables
sudo iptables -L -n -v | grep -i 443
sudo iptables -L OUTPUT -n -v
```

### 4. Probar conectividad a otros sitios HTTPS:

```bash
# Probar Google (debería funcionar)
curl -v --connect-timeout 5 https://www.google.com 2>&1 | head -10

# Probar GitHub (debería funcionar)
curl -v --connect-timeout 5 https://github.com 2>&1 | head -10
```

### 5. Verificar routing:

```bash
# Ver ruta a Finkok
traceroute -m 10 69.160.41.169 2>&1 | head -15
```

### 6. Verificar si hay firewall del proveedor:

```bash
# Verificar configuración de red
ip route show
cat /etc/resolv.conf
```

## Soluciones Posibles

### Si el puerto 443 está bloqueado por UFW:

```bash
# Permitir salida HTTPS
sudo ufw allow out 443/tcp
sudo ufw reload
```

### Si iptables está bloqueando:

```bash
# Permitir salida HTTPS en iptables
sudo iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT
sudo iptables-save | sudo tee /etc/iptables/rules.v4
```

### Si el proveedor tiene firewall:

Necesitarás contactar al proveedor del servidor para que permitan conexiones salientes HTTPS al dominio `demo-facturacion.finkok.com` o a la IP `69.160.41.169`.

## Verificar después de aplicar cambios

```bash
# Probar nuevamente
curl -v --connect-timeout 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20
```

Si `curl` funciona, entonces Java también debería funcionar.
