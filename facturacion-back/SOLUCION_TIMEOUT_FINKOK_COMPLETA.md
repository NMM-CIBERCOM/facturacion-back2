# Solución Completa: Timeout con Finkok

## Situación Actual

El error "Connection timed out" ocurre tanto desde tu máquina Windows como (probablemente) desde el servidor Linux. Esto indica un problema de **conectividad de red** más que un problema del código.

## Diagnóstico

### ✅ Lo que sabemos:
1. El DNS resuelve correctamente (`69.160.41.X`)
2. El timeout ocurre al intentar establecer la conexión (no es un error de aplicación)
3. El problema afecta tanto a tu máquina local como al servidor

### ❓ Lo que necesitamos verificar:
1. ¿Finkok está disponible desde otros lugares?
2. ¿Hay un firewall/proxy bloqueando conexiones HTTPS salientes?
3. ¿El servidor tiene acceso a Internet?

## Soluciones Inmediatas

### Opción 1: Verificar desde el Servidor (WinSCP)

**Paso 1:** Conéctate por WinSCP al servidor (`72.62.128.98`)

**Paso 2:** Abre la terminal integrada:
- Menú: **Commands** → **Open Terminal**
- O presiona: `Ctrl+P`

**Paso 3:** Ejecuta estos comandos:

```bash
# Verificar conectividad
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp

# Verificar DNS
nslookup demo-facturacion.finkok.com

# Verificar puerto 443
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" && echo "Puerto 443 accesible" || echo "Puerto 443 NO accesible"

# Verificar firewall
sudo iptables -L OUTPUT -n -v | grep -E "(443|REJECT|DROP)"

# Verificar acceso a Internet
ping -c 3 8.8.8.8
```

**Comparte los resultados** de estos comandos.

### Opción 2: Verificar si Finkok está Disponible

Prueba desde diferentes ubicaciones/redes:
- Desde tu móvil con datos (no WiFi)
- Desde otra red
- Usa un servicio online como: https://www.isitdownrightnow.com/demo-facturacion.finkok.com.html

### Opción 3: Configurar Firewall en el Servidor

Si el servidor tiene firewall bloqueando conexiones salientes:

```bash
# Permitir HTTPS saliente (puerto 443)
sudo ufw allow out 443/tcp

# O con iptables
sudo iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT
sudo iptables-save
```

### Opción 4: Configurar Proxy (si es necesario)

Si el servidor requiere proxy para acceder a Internet:

```bash
# Configurar variables de entorno
export http_proxy=http://proxy:puerto
export https_proxy=http://proxy:puerto

# O permanente en /etc/environment
sudo nano /etc/environment
# Agregar:
# http_proxy="http://proxy:puerto"
# https_proxy="http://proxy:puerto"
```

Y configurar en Tomcat (`setenv.sh`):

```bash
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort=8080"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort=8080"
```

### Opción 5: Usar URL de Producción de Finkok

Si tienes acceso a la cuenta de producción de Finkok, puedes cambiar la URL en `application.yml`:

```yaml
finkok:
  stamp:
    url: https://facturacion.finkok.com/servicios/soap/stamp  # Producción
  cancel:
    url: https://facturacion.finkok.com/servicios/soap/cancel  # Producción
```

**Nota:** La URL de producción puede tener mejor disponibilidad que la demo.

## Cambios Ya Implementados en el Código

✅ **Timeouts configurados:**
- Connect Timeout: 30 segundos
- Read Timeout: 120 segundos

✅ **Mejor manejo de errores:**
- Logs detallados cuando falla la conexión
- Mensajes claros sobre posibles causas

## Próximos Pasos Recomendados

1. **Ejecuta los comandos de diagnóstico desde el servidor** usando WinSCP
2. **Comparte los resultados** para diagnóstico preciso
3. **Verifica si Finkok está disponible** desde otras ubicaciones
4. **Si hay firewall**, permite conexiones HTTPS salientes
5. **Si necesitas proxy**, configúralo según las instrucciones
6. **Considera usar la URL de producción** si tienes acceso

## Nota sobre Connection Leaks

Los warnings de "Connection leak detection" de HikariCP son **secundarios**. Ocurren porque las transacciones de base de datos esperan la respuesta de Finkok, y como la conexión falla, las transacciones se quedan abiertas demasiado tiempo.

Una vez resuelto el problema de conectividad, estos warnings desaparecerán.

## Contacto con Finkok

Si el problema persiste, contacta a Finkok para verificar:
- Estado de sus servidores demo
- Si hay restricciones de IP
- Si necesitas whitelist de IPs

---

**¿Qué hacer ahora?**

1. Usa WinSCP para conectarte al servidor
2. Ejecuta los comandos de diagnóstico
3. Comparte los resultados
