# Identificar el Gateway/Router Real

## Problema Identificado

El acceso a `http://72.62.128.254` muestra Tomcat, no el panel del router. Esto significa que:
- `72.62.128.254` NO es el router/gateway real
- O el servidor está configurado como gateway también
- Necesitamos encontrar el router real

## Paso 1: Identificar el Gateway Real

Ejecuta estos comandos desde el servidor para encontrar el gateway real:

```bash
# 1. Ver la tabla de routing (muestra el gateway real)
ip route show

# 2. Ver solo la ruta por defecto (default gateway)
ip route | grep default

# 3. Ver información de red completa
ip addr show

# 4. Ver configuración de red
cat /etc/netplan/*.yaml 2>/dev/null || cat /etc/network/interfaces 2>/dev/null || cat /etc/sysconfig/network-scripts/ifcfg-* 2>/dev/null
```

## Paso 2: Verificar Información de Red

```bash
# Ver todas las interfaces de red
ip link show

# Ver IPs asignadas
hostname -I

# Ver gateway actual
route -n | grep '^0.0.0.0'

# O con ip route
ip route get 8.8.8.8
```

## Paso 3: Identificar el Router Real

El gateway real debería ser diferente de `72.62.128.254`. Posibles escenarios:

### Escenario 1: Gateway en Otra IP

El gateway real puede estar en:
- `72.62.128.1` (común en redes /24)
- `72.62.128.253` (otra IP en la red)
- O una IP completamente diferente

### Escenario 2: El Servidor ES el Gateway

Si el servidor está configurado como router/gateway:
- Necesitas configurar iptables en el mismo servidor
- O verificar si hay un firewall adicional

### Escenario 3: Router en Otra Red

El router puede estar en otra subred o ser un dispositivo externo.

## Paso 4: Verificar Firewall en el Mismo Servidor

Si el servidor actúa como gateway, verifica reglas de FORWARD:

```bash
# Ver reglas de FORWARD (tráfico que pasa por el servidor)
sudo iptables -L FORWARD -n -v --line-numbers

# Ver reglas de NAT (si el servidor hace NAT)
sudo iptables -t nat -L -n -v

# Ver todas las reglas
sudo iptables -L -n -v
```

## Paso 5: Buscar el Router Real

### Opción A: Desde el Servidor

```bash
# Ver gateway
ip route | grep default

# Hacer ping al gateway para ver si responde
GATEWAY=$(ip route | grep default | awk '{print $3}')
echo "Gateway encontrado: $GATEWAY"
ping -c 3 $GATEWAY

# Intentar acceder al panel del gateway
echo "Intenta acceder a: http://$GATEWAY"
```

### Opción B: Verificar con Traceroute

```bash
# Ver la ruta hacia Internet
traceroute 8.8.8.8 2>&1 | head -10

# El primer salto después del servidor es el gateway
```

### Opción C: Verificar Tabla ARP

```bash
# Ver tabla ARP (asociación IP-MAC)
arp -a

# Buscar el gateway en la tabla ARP
GATEWAY=$(ip route | grep default | awk '{print $3}')
arp -a | grep $GATEWAY
```

## Paso 6: Si el Servidor ES el Gateway

Si el servidor mismo es el gateway/router, necesitas:

### 1. Verificar Reglas de FORWARD

```bash
# Ver reglas de FORWARD
sudo iptables -L FORWARD -n -v --line-numbers

# Si hay reglas que bloquean, agregar excepción
sudo iptables -I FORWARD -p tcp --dport 443 -d 69.160.41.169 -j ACCEPT
```

### 2. Verificar IP Forwarding

```bash
# Verificar si IP forwarding está habilitado
cat /proc/sys/net/ipv4/ip_forward

# Si es 0, habilitarlo (si el servidor es router)
sudo sysctl -w net.ipv4.ip_forward=1
```

## Paso 7: Contactar al Proveedor

Si no puedes identificar el router real, contacta al proveedor del servidor y proporciona:

```
Información de Red del Servidor:
- IP del servidor: 72.62.128.98
- Gateway configurado: [resultado de "ip route | grep default"]
- Problema: No puedo acceder al panel del router para configurar firewall
- Necesito: Permitir conexiones HTTPS salientes a demo-facturacion.finkok.com:443

Solicitud:
1. ¿Cuál es la IP del router/gateway real?
2. ¿Puedo tener acceso al panel del router?
3. ¿O pueden configurar la regla de firewall para permitir conexiones a Finkok?
```

## Comandos Completos para Diagnóstico

Ejecuta estos comandos en secuencia desde el servidor:

```bash
echo "=== 1. Gateway Configurado ==="
ip route | grep default

echo ""
echo "=== 2. Tabla de Routing Completa ==="
ip route show

echo ""
echo "=== 3. Interfaces de Red ==="
ip addr show

echo ""
echo "=== 4. Traceroute a Internet ==="
traceroute 8.8.8.8 2>&1 | head -5

echo ""
echo "=== 5. Reglas de FORWARD (si el servidor es router) ==="
sudo iptables -L FORWARD -n -v --line-numbers | head -10

echo ""
echo "=== 6. IP Forwarding ==="
cat /proc/sys/net/ipv4/ip_forward
```

## Solución Temporal: Configurar en el Servidor

Mientras identificas el router real, puedes intentar configurar reglas de FORWARD en el servidor (si actúa como router):

```bash
# Permitir tráfico saliente a Finkok (si el servidor hace routing)
sudo iptables -I FORWARD -p tcp --dport 443 -d 69.160.41.169 -j ACCEPT

# Guardar reglas
sudo iptables-save | sudo tee /etc/iptables/rules.v4
```

Pero esto solo funciona si el servidor está haciendo routing/NAT para otros dispositivos.

## Próximos Pasos

1. **Ejecuta los comandos de diagnóstico** para identificar el gateway real
2. **Comparte los resultados** de `ip route | grep default`
3. **Intenta acceder al gateway real** (no `72.62.128.254`)
4. **Si no encuentras el router**, contacta al proveedor del servidor
