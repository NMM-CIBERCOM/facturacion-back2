# Solución: Error "Connection timed out" con Finkok

## Problema Identificado

El error `java.net.ConnectException: Connection timed out` ocurre cuando la aplicación intenta conectarse a `https://demo-facturacion.finkok.com/servicios/soap/stamp` pero no puede establecer la conexión.

## Causas Posibles

1. **Firewall bloqueando conexiones HTTPS salientes** desde el servidor hacia Internet
2. **Problemas de conectividad de red** desde el servidor
3. **Finkok temporalmente no disponible** o bloqueando conexiones
4. **Proxy requerido** pero no configurado en el servidor
5. **Timeouts de conexión demasiado cortos** (ya corregido en el código)

## Soluciones Implementadas

### 1. Timeouts Configurados

Se agregaron timeouts explícitos en `PacClient.java`:
- **Connect Timeout**: 30 segundos (tiempo para establecer conexión)
- **Read Timeout**: 120 segundos (tiempo para leer respuesta)

### 2. Mejor Manejo de Errores

Se mejoró el logging para identificar claramente problemas de conectividad.

## Verificaciones Necesarias en el Servidor

### Paso 1: Verificar Conectividad desde el Servidor

Conéctate al servidor por SSH y ejecuta:

```bash
# Verificar si puedes alcanzar Finkok
curl -v https://demo-facturacion.finkok.com/servicios/soap/stamp

# O usando wget
wget --spider https://demo-facturacion.finkok.com/servicios/soap/stamp

# Verificar resolución DNS
nslookup demo-facturacion.finkok.com

# Verificar conectividad al puerto 443
telnet demo-facturacion.finkok.com 443
```

**Si estos comandos fallan**, el problema es de conectividad de red/firewall.

### Paso 2: Verificar Firewall

```bash
# Verificar reglas de firewall (Ubuntu/Debian)
sudo ufw status
sudo iptables -L -n | grep 443

# Verificar si hay firewall bloqueando salidas HTTPS
sudo iptables -L OUTPUT -n -v | grep 443
```

**Si el firewall está bloqueando**, permite conexiones HTTPS salientes:

```bash
# Permitir HTTPS saliente (puerto 443)
sudo ufw allow out 443/tcp

# O con iptables
sudo iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT
```

### Paso 3: Verificar Proxy

Si el servidor requiere proxy para acceder a Internet:

```bash
# Verificar variables de proxy
echo $http_proxy
echo $https_proxy

# Si no están configuradas, configúralas:
export http_proxy=http://proxy:puerto
export https_proxy=http://proxy:puerto

# O configurar en /etc/environment (permanente)
sudo nano /etc/environment
# Agregar:
# http_proxy="http://proxy:puerto"
# https_proxy="http://proxy:puerto"
```

### Paso 4: Verificar Acceso a Internet

```bash
# Verificar que el servidor tenga acceso a Internet
ping -c 3 8.8.8.8
ping -c 3 google.com

# Verificar rutas de red
ip route
```

## Configuración de Proxy en Java (si es necesario)

Si el servidor requiere proxy, puedes configurarlo en Java agregando estas propiedades al iniciar Tomcat:

```bash
# En setenv.sh de Tomcat
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort=8080"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort=8080"
```

O configurar en `application.yml`:

```yaml
# Si necesitas configurar proxy a nivel de aplicación
# (requiere modificar PacClient para usar estas propiedades)
```

## Próximos Pasos

1. **Ejecuta las verificaciones** del Paso 1 desde el servidor
2. **Comparte los resultados** de los comandos de conectividad
3. **Si hay firewall**, permite conexiones HTTPS salientes
4. **Si hay proxy**, configúralo según las instrucciones
5. **Recompila y redespliega** el WAR con los cambios de timeouts

## Nota sobre Connection Leaks

Los warnings de "Connection leak detection" de HikariCP son **secundarios** al problema principal. Ocurren porque las transacciones de base de datos están esperando la respuesta de Finkok, y como la conexión a Finkok falla, las transacciones se quedan abiertas demasiado tiempo.

Una vez resuelto el problema de conectividad con Finkok, estos warnings deberían desaparecer.

## Comandos de Diagnóstico Rápido

```bash
# Desde el servidor, ejecuta esto para diagnóstico completo:
echo "=== Verificando conectividad ==="
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20

echo ""
echo "=== Verificando DNS ==="
nslookup demo-facturacion.finkok.com

echo ""
echo "=== Verificando puerto 443 ==="
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" && echo "Puerto 443 accesible" || echo "Puerto 443 NO accesible"

echo ""
echo "=== Verificando firewall ==="
sudo iptables -L OUTPUT -n -v | grep -E "(443|REJECT|DROP)"
```

Comparte los resultados de estos comandos para diagnóstico más preciso.
