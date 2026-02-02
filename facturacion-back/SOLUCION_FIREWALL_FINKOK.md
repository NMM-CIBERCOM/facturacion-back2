# Solución: Firewall Bloqueando Conexiones a Finkok

## Problema Confirmado

El servidor **NO puede conectarse** a `https://demo-facturacion.finkok.com` debido a un timeout. Esto indica que:
- El firewall está bloqueando conexiones HTTPS salientes (puerto 443)
- O hay un problema de routing/proxy

## Solución: Permitir Conexiones HTTPS Salientes

### Paso 1: Verificar Estado del Firewall

Ejecuta en la terminal de WinSCP:

```bash
# Verificar si UFW está activo
sudo ufw status

# Verificar reglas de iptables
sudo iptables -L OUTPUT -n -v | grep -E "(443|REJECT|DROP)"
```

### Paso 2: Permitir HTTPS Saliente (Puerto 443)

**Opción A: Si usas UFW (Ubuntu/Debian):**

```bash
# Permitir conexiones HTTPS salientes
sudo ufw allow out 443/tcp

# Verificar que se aplicó
sudo ufw status numbered
```

**Opción B: Si usas iptables directamente:**

```bash
# Permitir conexiones HTTPS salientes
sudo iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT

# Guardar las reglas permanentemente
sudo iptables-save | sudo tee /etc/iptables/rules.v4

# O en sistemas sin iptables-persistent:
sudo sh -c "iptables-save > /etc/iptables/rules.v4"
```

**Opción C: Si usas firewalld (CentOS/RHEL):**

```bash
sudo firewall-cmd --add-service=https --permanent
sudo firewall-cmd --reload
```

### Paso 3: Verificar que Funciona

Después de aplicar los cambios, ejecuta:

```bash
# Probar conectividad
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp

# O verificar puerto 443
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" && echo "✓ Puerto 443 ACCESIBLE" || echo "✗ Puerto 443 NO ACCESIBLE"
```

### Paso 4: Si Aún No Funciona - Verificar Proxy

Si después de permitir el firewall sigue sin funcionar, verifica si necesitas proxy:

```bash
# Verificar variables de proxy
echo $http_proxy
echo $https_proxy
echo $HTTP_PROXY
echo $HTTPS_PROXY

# Verificar configuración de red
cat /etc/environment | grep -i proxy
```

Si necesitas configurar proxy, ver instrucciones en `SOLUCION_TIMEOUT_FINKOK_COMPLETA.md`.

## Comandos Rápidos (Copia y Pega)

Ejecuta estos comandos en secuencia en la terminal de WinSCP:

```bash
# 1. Verificar firewall
echo "=== Estado del Firewall ==="
sudo ufw status 2>/dev/null || sudo iptables -L OUTPUT -n -v | head -10

# 2. Permitir HTTPS saliente
echo ""
echo "=== Permitiendo HTTPS saliente ==="
sudo ufw allow out 443/tcp 2>/dev/null || sudo iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT

# 3. Verificar conectividad
echo ""
echo "=== Verificando conectividad ==="
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" 2>/dev/null && echo "✓ Puerto 443 ACCESIBLE" || echo "✗ Puerto 443 NO ACCESIBLE"

# 4. Probar con curl
echo ""
echo "=== Probando con curl ==="
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20
```

## Después de Aplicar la Solución

1. **Reinicia Tomcat** para que la aplicación use la nueva configuración de red:
   ```bash
   sudo systemctl restart tomcat
   # O
   sudo /etc/init.d/tomcat restart
   ```

2. **Prueba generar una factura** desde la aplicación

3. **Revisa los logs** para confirmar que ya no hay timeout:
   ```bash
   tail -f /var/log/facturacion/server.log
   # O
   tail -f $CATALINA_HOME/logs/catalina.out
   ```

## Nota Importante

Si el servidor está detrás de un **firewall corporativo** o **router** que también bloquea conexiones salientes, necesitarás:
- Contactar al administrador de red
- Solicitar que permitan conexiones HTTPS salientes al puerto 443
- O configurar un proxy corporativo
