# Verificar y Configurar Firewall Externo (Gateway/Router)

## Información del Gateway

- **Gateway IP:** `72.62.128.254`
- **Servidor IP:** `72.62.128.98`
- **Objetivo:** Permitir conexiones HTTPS salientes a `demo-facturacion.finkok.com:443`

## Paso 1: Acceder al Panel del Router/Gateway

### Opción A: Acceso Web

1. **Abrir navegador** y acceder a:
   ```
   http://72.62.128.254
   ```
   O
   ```
   https://72.62.128.254
   ```

2. **Credenciales:**
   - Usuario: `admin` (o el que configuraste)
   - Contraseña: `AdminPts@2026` (o la que configuraste)

### Opción B: Acceso por SSH/Telnet

```bash
# Desde el servidor o tu máquina
ssh admin@72.62.128.254
# O
telnet 72.62.128.254
```

### Opción C: Si No Tienes Acceso

Si no tienes las credenciales o acceso al router:
- Contacta al proveedor del servidor
- Solicita acceso al panel del router
- O solicita que configuren las reglas de firewall

## Paso 2: Localizar Configuración de Firewall

Una vez dentro del panel, busca:

### Ubicaciones Comunes:
- **Firewall** → **Rules** → **Outbound Rules**
- **Security** → **Firewall** → **Outgoing**
- **Network** → **Firewall** → **Outbound**
- **Advanced** → **Firewall** → **OUTPUT Chain**

### Nombres Alternativos:
- "Reglas de salida"
- "Outbound firewall"
- "Firewall saliente"
- "OUTPUT rules"

## Paso 3: Verificar Reglas Existentes

Busca reglas que puedan estar bloqueando:

1. **Reglas que bloquean HTTPS (puerto 443):**
   - Busca reglas con destino `443` o `HTTPS`
   - Verifica si hay reglas `DENY` o `REJECT` para puerto 443

2. **Reglas específicas para Finkok:**
   - Busca reglas con destino `demo-facturacion.finkok.com`
   - O IP `69.160.41.169`

3. **Reglas generales que bloquean:**
   - Reglas `DENY ALL` o `REJECT ALL` en OUTPUT
   - Reglas que bloquean por defecto conexiones salientes

## Paso 4: Agregar Regla para Permitir Finkok

### Opción 1: Permitir por Dominio (Recomendado)

Agrega una regla de salida (OUTBOUND):

```
Tipo: Allow/ACCEPT
Dirección: Outbound/OUTPUT
Protocolo: TCP
Puerto Destino: 443
Destino: demo-facturacion.finkok.com
O IP: 69.160.41.169
Origen: 72.62.128.98 (o toda la red 72.62.128.0/24)
Acción: ALLOW/ACCEPT
```

### Opción 2: Permitir por IP

Si no puedes usar dominio, usa la IP:

```
Tipo: Allow/ACCEPT
Dirección: Outbound/OUTPUT
Protocolo: TCP
Puerto Destino: 443
Destino: 69.160.41.169
Origen: 72.62.128.98
Acción: ALLOW/ACCEPT
```

### Opción 3: Permitir Todo HTTPS Saliente (Menos Seguro)

Si prefieres permitir todo HTTPS saliente:

```
Tipo: Allow/ACCEPT
Dirección: Outbound/OUTPUT
Protocolo: TCP
Puerto Destino: 443
Destino: Any/All
Origen: 72.62.128.98 (o toda la red)
Acción: ALLOW/ACCEPT
```

**⚠️ Nota:** Esta opción es menos segura pero más simple.

## Paso 5: Verificar Orden de Reglas

**IMPORTANTE:** Las reglas de firewall se evalúan en orden. Asegúrate de que:

1. La regla de **ALLOW para Finkok** esté **ANTES** de cualquier regla `DENY ALL`
2. O que la regla de `DENY ALL` tenga una excepción para Finkok

### Ejemplo de Orden Correcto:

```
1. ALLOW → 72.62.128.98 → demo-facturacion.finkok.com:443
2. ALLOW → 72.62.128.98 → 8.8.8.8:53 (DNS)
3. DENY ALL → (bloquea todo lo demás)
```

## Paso 6: Aplicar y Guardar Cambios

1. **Aplicar cambios** (botón "Apply", "Save", "Guardar")
2. **Reiniciar el router** si es necesario (algunos routers requieren reinicio)
3. **Verificar** que los cambios se guardaron

## Paso 7: Verificar que Funciona

Desde el servidor (`72.62.128.98`), ejecuta:

```bash
# Probar conectividad
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20

# O verificar puerto 443
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" && echo "✓ Puerto 443 ACCESIBLE" || echo "✗ Puerto 443 NO ACCESIBLE"
```

## Si No Tienes Acceso al Router

### Opción 1: Contactar Proveedor del Servidor

Contacta al proveedor del servidor (`72.62.128.98`) y solicita:

```
Solicitud: Configurar regla de firewall en el router/gateway

Detalles:
- Gateway: 72.62.128.254
- Servidor: 72.62.128.98
- Acción requerida: Permitir conexiones HTTPS salientes (puerto 443 TCP)
- Destino: demo-facturacion.finkok.com (69.160.41.169)
- Protocolo: TCP
- Puerto: 443
- Dirección: Outbound/OUTPUT
- Acción: ALLOW/ACCEPT

Razón: La aplicación necesita conectarse a Finkok para timbrado de facturas.
```

### Opción 2: Verificar con Comandos desde el Servidor

Ejecuta estos comandos para más información:

```bash
# Ver ruta hacia el gateway
ip route | grep default

# Ver si hay reglas de NAT que puedan estar interfiriendo
sudo iptables -t nat -L -n -v

# Verificar conectividad al gateway
ping -c 3 72.62.128.254

# Traceroute para ver dónde se pierde
traceroute demo-facturacion.finkok.com 2>&1 | head -20
```

## Modelos de Router Comunes

### Ubiquiti/UniFi
- Panel: `https://72.62.128.254`
- Ubicación: **Settings** → **Firewall & Security** → **Firewall Rules** → **Outbound**

### pfSense
- Panel: `https://72.62.128.254`
- Ubicación: **Firewall** → **Rules** → **WAN** (o la interfaz de salida)

### MikroTik RouterOS
- Panel: `http://72.62.128.254`
- Ubicación: **IP** → **Firewall** → **Filter Rules** → **Output Chain**

### Cisco
- CLI: `configure terminal` → `ip access-list extended OUTBOUND`
- O GUI: **Security** → **Firewall** → **Outbound Rules**

### TP-Link/Netgear/D-Link
- Panel: `http://72.62.128.254`
- Ubicación: **Advanced** → **Firewall** → **Outbound Rules**

## Comandos Útiles para Diagnóstico

```bash
# 1. Verificar conectividad al gateway
ping -c 3 72.62.128.254

# 2. Ver ruta hacia Finkok
traceroute demo-facturacion.finkok.com 2>&1 | head -20

# 3. Verificar si el gateway responde
curl -v --max-time 5 http://72.62.128.254 2>&1 | head -10

# 4. Ver tabla de routing
ip route show

# 5. Verificar ARP (asociación IP-MAC)
arp -a | grep 72.62.128.254
```

## Próximos Pasos

1. **Intenta acceder al panel del router** usando las credenciales
2. **Busca la sección de firewall** según el modelo de router
3. **Agrega la regla** para permitir Finkok
4. **Verifica** que funciona con los comandos de prueba
5. **Si no tienes acceso**, contacta al proveedor del servidor

## Nota Importante

Si después de configurar el firewall externo **aún no funciona**, el problema puede ser:
- Finkok bloqueando la IP del servidor
- Restricciones a nivel del proveedor de Internet
- Problemas de routing más profundos

En ese caso, la siguiente opción sería **contactar a Finkok** para verificar bloqueos de IP.
