# Configurar Firewall del Servidor

## Comandos para Ejecutar

Ejecuta estos comandos UNO POR UNO en la terminal de WinSCP:

### 1. Verificar reglas actuales de iptables:

```bash
sudo iptables -L OUTPUT -n -v --line-numbers
```

### 2. Agregar regla explícita para permitir salida HTTPS a Finkok:

```bash
# Permitir salida HTTPS a la IP de Finkok
sudo iptables -I OUTPUT 1 -p tcp -d 69.160.41.169 --dport 443 -j ACCEPT

# O permitir salida HTTPS a cualquier destino (más permisivo)
sudo iptables -I OUTPUT 1 -p tcp --dport 443 -j ACCEPT
```

### 3. Verificar que la regla se agregó:

```bash
sudo iptables -L OUTPUT -n -v --line-numbers | head -10
```

### 4. Guardar las reglas de iptables (para que persistan después de reiniciar):

```bash
# En Debian/Ubuntu
sudo iptables-save | sudo tee /etc/iptables/rules.v4

# Si el directorio no existe, crearlo
sudo mkdir -p /etc/iptables
sudo iptables-save | sudo tee /etc/iptables/rules.v4
```

### 5. Configurar iptables para cargar las reglas al iniciar:

```bash
# Instalar iptables-persistent si no está instalado
sudo apt-get update
sudo apt-get install -y iptables-persistent

# O si ya está instalado, guardar las reglas
sudo netfilter-persistent save
```

### 6. Verificar UFW (ya debería estar permitido, pero verificamos):

```bash
# Ver reglas de salida
sudo ufw status numbered | grep -i out

# Si no está, agregar explícitamente
sudo ufw allow out 443/tcp
sudo ufw reload
```

### 7. Probar conectividad después de los cambios:

```bash
# Probar con curl
curl -v --connect-timeout 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20

# O probar conexión TCP directa
timeout 5 bash -c 'cat < /dev/null > /dev/tcp/69.160.41.169/443' && echo "✓ Puerto 443 accesible" || echo "✗ Puerto 443 bloqueado"
```

## Si Sigue Fallando

Si después de estos comandos sigue fallando, el problema es del **firewall del proveedor/hosting** que está fuera de tu control. En ese caso:

1. **Pregunta a tu compañero** si hay proxy corporativo disponible
2. **Verifica si puedes acceder al panel del gateway** en `http://72.62.128.254`
3. **Considera usar un túnel SSH** desde otro servidor que sí pueda conectarse
