# Diagnóstico Avanzado: Timeout Persistente con Finkok

## Situación Actual

✅ **Regla de firewall UFW agregada correctamente**
❌ **Timeout persiste** - La conexión no se puede establecer

Esto indica que el problema **NO es UFW**, sino:
1. Firewall externo (router, firewall corporativo)
2. Reglas de iptables que UFW no maneja
3. Problema de routing
4. Finkok bloqueando la IP del servidor
5. Proxy requerido

## Verificaciones Adicionales

### 1. Verificar Reglas de iptables Directamente

UFW es solo un frontend de iptables. Puede haber reglas que UFW no muestra:

```bash
# Ver todas las reglas de OUTPUT en iptables
sudo iptables -L OUTPUT -n -v --line-numbers

# Ver reglas de FORWARD (si hay NAT)
sudo iptables -L FORWARD -n -v

# Ver reglas de INPUT (aunque no debería afectar salidas)
sudo iptables -L INPUT -n -v | grep -E "(443|REJECT|DROP)"
```

### 2. Verificar Política por Defecto de OUTPUT

```bash
# Ver política por defecto
sudo iptables -L OUTPUT -n -v | head -5

# Si dice "policy DROP", cambiarla:
sudo iptables -P OUTPUT ACCEPT
```

### 3. Verificar Routing

```bash
# Ver tabla de routing
ip route

# Verificar si puede alcanzar Internet
ping -c 3 8.8.8.8

# Verificar si puede resolver DNS
nslookup demo-facturacion.finkok.com
```

### 4. Verificar si hay Firewall Externo

El servidor puede estar detrás de un firewall/router que también bloquea:

```bash
# Verificar gateway
ip route | grep default

# Verificar si hay NAT
sudo iptables -t nat -L -n -v
```

### 5. Probar desde Otra IP/Dirección

Si tienes acceso desde otra máquina en la misma red, prueba desde ahí para ver si es un problema específico de este servidor.

### 6. Verificar si Finkok Bloquea la IP

Contacta a Finkok para verificar si están bloqueando la IP del servidor (`72.62.128.98`).

## Soluciones Alternativas

### Opción 1: Deshabilitar Temporalmente iptables (Solo para Prueba)

⚠️ **ADVERTENCIA: Solo para diagnóstico, NO para producción**

```bash
# Ver reglas actuales antes de cambiar
sudo iptables -L OUTPUT -n -v > /tmp/iptables-backup.txt

# Permitir todo el tráfico saliente temporalmente
sudo iptables -P OUTPUT ACCEPT
sudo iptables -F OUTPUT

# Probar conectividad
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp

# Si funciona, el problema es iptables. Restaurar reglas:
# (No ejecutar esto todavía, primero diagnostica)
```

### Opción 2: Usar Proxy/VPN

Si hay un firewall externo bloqueando, puedes usar un proxy:

```bash
# Configurar proxy temporal
export http_proxy=http://proxy:puerto
export https_proxy=http://proxy:puerto

# Probar
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp
```

### Opción 3: Contactar Administrador de Red

Si el servidor está en una red corporativa, contacta al administrador para:
- Verificar firewall externo
- Permitir conexiones HTTPS salientes al puerto 443
- Verificar si hay proxy corporativo requerido

## Comandos de Diagnóstico Completo

Ejecuta estos comandos y comparte los resultados:

```bash
echo "=== 1. Reglas iptables OUTPUT ==="
sudo iptables -L OUTPUT -n -v --line-numbers | head -20

echo ""
echo "=== 2. Política por defecto OUTPUT ==="
sudo iptables -L OUTPUT -n -v | grep "Chain OUTPUT"

echo ""
echo "=== 3. Routing ==="
ip route

echo ""
echo "=== 4. Gateway ==="
ip route | grep default

echo ""
echo "=== 5. Conectividad básica ==="
ping -c 3 8.8.8.8

echo ""
echo "=== 6. DNS ==="
nslookup demo-facturacion.finkok.com

echo ""
echo "=== 7. Probar con telnet (si está instalado) ==="
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" && echo "✓ Puerto accesible" || echo "✗ Puerto NO accesible"
```

## Próximos Pasos

1. **Ejecuta los comandos de diagnóstico** y comparte los resultados
2. **Verifica si hay un firewall externo** (router, firewall corporativo)
3. **Contacta al administrador de red** si es necesario
4. **Considera usar un proxy** si hay un firewall corporativo
