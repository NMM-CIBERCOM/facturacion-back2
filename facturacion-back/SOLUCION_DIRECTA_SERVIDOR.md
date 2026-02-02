# Soluciones Directas desde el Servidor (WinSCP)

## Opción 1: Configurar Proxy en Java (Si Tienes Proxy Disponible)

### Paso 1: Verificar si Hay Proxy Configurado

Ejecuta en la terminal de WinSCP:

```bash
# Verificar variables de proxy del sistema
env | grep -i proxy

# Verificar configuración de red
cat /etc/environment | grep -i proxy
```

### Paso 2: Configurar Proxy en Tomcat

Si tienes un proxy disponible, configura en Tomcat:

**Ubicación:** `/opt/tomcat/bin/setenv.sh` (o crear si no existe)

```bash
# Crear/editar setenv.sh
sudo nano /opt/tomcat/bin/setenv.sh
```

**Agregar:**

```bash
#!/bin/sh
# Configuración de proxy para Finkok
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost=TU_PROXY_HOST"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort=TU_PROXY_PUERTO"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost=TU_PROXY_HOST"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort=TU_PROXY_PUERTO"
```

**Dar permisos:**

```bash
sudo chmod +x /opt/tomcat/bin/setenv.sh
```

**Reiniciar Tomcat:**

```bash
sudo systemctl restart tomcat
```

## Opción 2: Usar Proxy HTTP Público (Temporal, No Recomendado para Producción)

⚠️ **ADVERTENCIA:** Solo para pruebas. Los proxies públicos no son seguros para producción.

### Configurar Proxy Público Temporal

```bash
# Editar setenv.sh
sudo nano /opt/tomcat/bin/setenv.sh
```

**Agregar (ejemplo con proxy público - NO recomendado para producción):**

```bash
#!/bin/sh
# Proxy temporal para pruebas
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost=proxy.example.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyPort=8080"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost=proxy.example.com"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort=8080"
```

## Opción 3: Modificar PacClient.java para Usar Proxy Programáticamente

Esta es la mejor opción si quieres control total desde el código.

### Modificar PacClient.java

Agregar configuración de proxy en el método `sendSOAPRequest`:

```java
private String sendSOAPRequest(SOAPMessage soapMessage) throws Exception {
    // ... código existente ...
    
    URL serviceUrl = new URL(endpointUrl);
    
    // Configurar proxy si está disponible
    Proxy proxy = null;
    String proxyHost = System.getProperty("http.proxyHost");
    String proxyPort = System.getProperty("http.proxyPort");
    
    if (proxyHost != null && proxyPort != null) {
        int port = Integer.parseInt(proxyPort);
        proxy = new Proxy(Proxy.Type.HTTP, new java.net.InetSocketAddress(proxyHost, port));
        logger.info("Usando proxy: {}:{}", proxyHost, port);
    }
    
    HttpURLConnection connection;
    if (proxy != null) {
        connection = (HttpURLConnection) serviceUrl.openConnection(proxy);
    } else {
        connection = (HttpURLConnection) serviceUrl.openConnection();
    }
    
    // ... resto del código ...
}
```

## Opción 4: Configurar Proxy a Nivel de Sistema

### Configurar Variables de Entorno del Sistema

```bash
# Editar /etc/environment
sudo nano /etc/environment
```

**Agregar:**

```
http_proxy="http://proxy_host:puerto"
https_proxy="http://proxy_host:puerto"
HTTP_PROXY="http://proxy_host:puerto"
HTTPS_PROXY="http://proxy_host:puerto"
```

**Aplicar cambios:**

```bash
source /etc/environment
sudo systemctl restart tomcat
```

## Opción 5: Usar Túnel SSH (Si Tienes Otro Servidor Accesible)

Si tienes acceso a otro servidor que SÍ puede acceder a Finkok:

### Crear Túnel SSH Local

```bash
# Crear túnel SSH (desde tu máquina o desde el servidor)
ssh -L 8080:demo-facturacion.finkok.com:443 usuario@otro_servidor

# Luego configurar proxy en Java apuntando a localhost:8080
```

### Configurar en setenv.sh

```bash
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost=localhost"
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyPort=8080"
```

## Opción 6: Modificar application.yml para Agregar Configuración de Proxy

Agregar propiedades de proxy en `application.yml`:

```yaml
# Configuración de proxy (si está disponible)
proxy:
  enabled: true
  host: proxy_host
  port: 8080
```

Y modificar `PacClient.java` para leer estas propiedades.

## Solución Recomendada: Modificar PacClient.java

La mejor solución es modificar `PacClient.java` para que pueda usar proxy si está configurado, pero funcione sin proxy si no está disponible.

### Implementación Completa

Modificar el método `sendSOAPRequest` para:
1. Leer propiedades de proxy desde `application.yml`
2. Usar proxy si está configurado
3. Funcionar normalmente si no hay proxy

¿Quieres que implemente esta solución modificando el código?
