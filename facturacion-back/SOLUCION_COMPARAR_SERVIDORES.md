# Solución: Comparar Configuraciones entre Servidores

## Situación

✅ **Funcionaba en el servidor anterior**
❌ **No funciona en el servidor nuevo (72.62.128.98)**

Esto indica que el problema es **específico de este servidor/red**, no del código.

## Verificaciones a Realizar

### 1. Comparar Configuración de Red

Ejecuta estos comandos en el servidor nuevo y compáralos con el servidor anterior:

```bash
# Ver configuración de red
ip addr show
ip route show

# Ver DNS
cat /etc/resolv.conf

# Ver configuración de firewall
sudo iptables -L -n -v
sudo ufw status verbose

# Ver variables de entorno
env | sort
```

### 2. Verificar Configuración de Java

```bash
# Ver versión de Java
java -version

# Ver propiedades del sistema Java
java -XshowSettings:properties -version 2>&1 | grep -i proxy
java -XshowSettings:properties -version 2>&1 | grep -i network

# Ver configuración de Tomcat
cat /opt/tomcat/bin/setenv.sh 2>/dev/null || echo "setenv.sh no existe"
```

### 3. Verificar Certificados SSL/TLS

```bash
# Verificar certificados del sistema
ls -la /etc/ssl/certs/ | head -10

# Probar conexión SSL directamente
openssl s_client -connect demo-facturacion.finkok.com:443 -showcerts < /dev/null 2>&1 | head -30
```

### 4. Verificar Configuración de Tomcat

```bash
# Ver configuración de Tomcat
cat /opt/tomcat/conf/server.xml | grep -i proxy
cat /opt/tomcat/conf/server.xml | grep -i connector

# Ver variables de entorno de Tomcat
sudo systemctl show tomcat | grep -i proxy
```

## Soluciones Alternativas

### Opción 1: Usar Cliente HTTP Diferente (Apache HttpClient)

En lugar de `HttpURLConnection`, usar Apache HttpClient que puede tener mejor manejo de conexiones.

### Opción 2: Configurar Java para Ignorar Verificación SSL (Solo Pruebas)

⚠️ **ADVERTENCIA:** Solo para diagnóstico, NO para producción.

```java
// Agregar al inicio de sendSOAPRequest
TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return null; }
        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
    }
};
SSLContext sc = SSLContext.getInstance("SSL");
sc.init(null, trustAllCerts, new java.security.SecureRandom());
HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
```

### Opción 3: Usar URL de Producción de Finkok

Si el servidor anterior usaba producción y este usa demo, puede haber diferencias.

### Opción 4: Verificar si Hay VPN o Túnel Requerido

El servidor anterior puede haber tenido VPN o túnel configurado.

### Opción 5: Configurar Java con Propiedades Específicas

Agregar propiedades Java específicas en `setenv.sh`:

```bash
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -Dsun.net.useExclusiveBind=false"
```

## Preguntas Clave

1. **¿El servidor anterior tenía alguna configuración especial?**
   - VPN
   - Proxy
   - Túnel SSH
   - Configuración de red diferente

2. **¿La IP del servidor anterior era diferente?**
   - Puede haber restricciones por IP

3. **¿El servidor anterior estaba en la misma red?**
   - Puede haber firewall de red diferente

4. **¿Qué versión de Java tenía el servidor anterior?**
   - Puede haber diferencias en manejo de conexiones

## Acción Inmediata

Ejecuta estos comandos y comparte los resultados:

```bash
# 1. Información del sistema
uname -a
java -version

# 2. Configuración de red
ip route show
cat /etc/resolv.conf

# 3. Firewall
sudo iptables -L OUTPUT -n -v | head -20

# 4. Probar conexión SSL
openssl s_client -connect demo-facturacion.finkok.com:443 -showcerts < /dev/null 2>&1 | head -20
```

Con esta información podremos identificar la diferencia específica.
