# Diagnóstico Completo: Timeout con Finkok

## Resumen del Problema

**Error:** `Connection timed out` al intentar conectarse a `https://demo-facturacion.finkok.com/servicios/soap/stamp`

## Verificaciones Realizadas

### ✅ Lo que SÍ funciona:
1. **Firewall local (UFW/iptables):** Política ACCEPT, regla para puerto 443 agregada
2. **Conectividad a Internet:** Ping a 8.8.8.8 funciona
3. **DNS:** Resolución de nombres funciona correctamente
4. **HTTPS a otros servicios:** Google y GitHub responden correctamente
5. **Código de la aplicación:** Timeouts configurados (30s connect, 120s read)

### ❌ Lo que NO funciona:
1. **Conexión a Finkok:** Timeout al intentar conectar al puerto 443
2. **Tanto desde curl como desde Java:** Mismo problema

## Análisis del Código

### Código Revisado: `PacClient.java`

```java
// Línea 467: Usa HttpURLConnection estándar de Java
HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();

// Líneas 470-471: Timeouts configurados correctamente
connection.setConnectTimeout(30000); // 30 segundos
connection.setReadTimeout(120000);    // 120 segundos
```

**Conclusión:** El código está correcto. El problema NO es del código.

## Causas Probables (Ordenadas por Probabilidad)

### 1. 🔴 Finkok Bloqueando la IP del Servidor (MÁS PROBABLE)

**Evidencia:**
- HTTPS funciona a otros servicios (Google, GitHub)
- El timeout es específico a Finkok
- El servidor tiene IP: `72.62.128.98`

**Solución:**
- Contactar a Finkok para verificar si la IP está bloqueada
- Solicitar whitelist de la IP
- Verificar restricciones geográficas o de proveedor

### 2. 🟡 Firewall Externo Bloqueando Específicamente a Finkok

**Evidencia:**
- Gateway: `72.62.128.254`
- Puede tener reglas específicas bloqueando ciertos dominios

**Solución:**
- Acceder al panel del router/firewall
- Verificar reglas de firewall
- Permitir conexiones a `demo-facturacion.finkok.com:443`
- O contactar al administrador de red

### 3. 🟡 Problema de Routing Específico

**Evidencia:**
- La conexión se intenta pero nunca se establece
- Timeout después de 10 segundos (curl) o 30 segundos (Java)

**Solución:**
- Ejecutar `traceroute demo-facturacion.finkok.com` para ver dónde se pierde
- Verificar rutas de red
- Contactar al proveedor de Internet

### 4. 🟢 Proxy Requerido pero No Configurado

**Evidencia:**
- Algunas redes corporativas requieren proxy para HTTPS

**Solución:**
- Verificar si hay proxy configurado: `env | grep -i proxy`
- Configurar proxy en Java si es necesario
- O configurar proxy en el sistema

## Soluciones Implementadas en el Código

### ✅ Mejoras Ya Aplicadas:

1. **Timeouts explícitos:**
   - Connect Timeout: 30 segundos
   - Read Timeout: 120 segundos

2. **Mejor manejo de errores:**
   - Logs detallados cuando falla la conexión
   - Mensajes claros sobre posibles causas

3. **Manejo de excepciones:**
   - Captura específica de `ConnectException`
   - Mensajes de error informativos

## Soluciones Recomendadas (Orden de Prioridad)

### Prioridad 1: Contactar a Finkok

**Acción:**
1. Contactar al soporte de Finkok
2. Proporcionar:
   - IP del servidor: `72.62.128.98`
   - Dominio: `demo-facturacion.finkok.com`
   - Error: Connection timed out
3. Solicitar:
   - Verificar si la IP está bloqueada
   - Whitelist de la IP si es necesario
   - Verificar restricciones de red

**Contacto:**
- Portal: https://facturacion.finkok.com
- Email: soporte@finkok.com (verificar en su sitio)

### Prioridad 2: Verificar Firewall Externo

**Acción:**
1. Acceder al panel del router/firewall (`72.62.128.254`)
2. Verificar reglas de firewall
3. Permitir conexiones HTTPS salientes a Finkok
4. O contactar al administrador de red

### Prioridad 3: Usar URL de Producción

**Acción:**
Si tienes acceso a la cuenta de producción, cambiar en `application.yml`:

```yaml
finkok:
  stamp:
    url: https://facturacion.finkok.com/servicios/soap/stamp  # Producción
  cancel:
    url: https://facturacion.finkok.com/servicios/soap/cancel  # Producción
```

### Prioridad 4: Configurar Proxy (si es necesario)

**Acción:**
Si hay un proxy corporativo requerido:

1. **Configurar en Tomcat (`setenv.sh`):**
```bash
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort=8080"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost=proxy.dominio.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort=8080"
```

2. **O modificar `PacClient.java`** para usar proxy programáticamente

## Comandos de Diagnóstico Adicional

Ejecuta estos comandos desde el servidor para más información:

```bash
# 1. Traceroute para ver dónde se pierde la conexión
traceroute demo-facturacion.finkok.com 2>&1 | head -20

# 2. Probar conectividad directa a la IP
timeout 5 bash -c "</dev/tcp/69.160.41.169/443" && echo "✓ IP accesible" || echo "✗ IP NO accesible"

# 3. Verificar si hay proxy configurado
env | grep -i proxy

# 4. Ver todas las IPs de Finkok
dig demo-facturacion.finkok.com +short

# 5. Probar con wget (alternativa a curl)
wget --spider --timeout=10 https://demo-facturacion.finkok.com/servicios/soap/stamp
```

## Conclusión

**El problema NO es del código de la aplicación.** El código está correctamente implementado con timeouts apropiados y manejo de errores.

**El problema es de infraestructura de red:**
- Finkok probablemente está bloqueando la IP del servidor
- O hay un firewall externo bloqueando específicamente a Finkok
- O hay un problema de routing específico

**Acción inmediata requerida:**
1. Contactar a Finkok para verificar bloqueo de IP
2. Verificar firewall externo/router
3. Considerar usar URL de producción si está disponible

## Nota Final

Este es un problema de **política de red/infraestructura**, no de la aplicación. La solución requiere coordinación con:
- Finkok (para whitelist de IP)
- Administrador de red (para firewall externo)
- Proveedor del servidor (para verificar restricciones)
