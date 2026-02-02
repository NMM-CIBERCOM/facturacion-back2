# Soluciones Alternativas Sin Proxy

## Situación

✅ **Funcionaba en servidor anterior**
❌ **No funciona en servidor nuevo (72.62.128.98)**

## Soluciones Implementadas

He agregado configuraciones adicionales que pueden ayudar:

### 1. Configuración de IPv4 Forzado

En `application.yml`, puedes forzar el uso de IPv4:

```yaml
finkok:
  connection:
    use-ipv4-only: true  # Forzar IPv4 si hay problemas con IPv6
```

### 2. Timeouts Configurables

```yaml
finkok:
  connection:
    connect-timeout: 60000  # Aumentar a 60 segundos
    read-timeout: 180000    # Aumentar a 180 segundos
```

## Otras Soluciones a Probar

### Opción 1: Verificar si el Servidor Anterior Tenía Configuración Especial

Ejecuta estos comandos para comparar:

```bash
# Ver configuración de red
ip addr show
ip route show

# Ver DNS
cat /etc/resolv.conf

# Ver configuración de Java
java -version
java -XshowSettings:properties -version 2>&1 | grep -i network

# Ver si hay VPN o túnel
ip tunnel show
ip link show type tun
```

### Opción 2: Configurar Java con Propiedades Específicas

Crear/editar `/opt/tomcat/bin/setenv.sh`:

```bash
#!/bin/sh
# Propiedades Java para mejorar conexiones
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -Dsun.net.useExclusiveBind=false"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv6Addresses=false"
```

### Opción 3: Verificar si Hay Restricciones por IP en Finkok

El servidor anterior puede haber estado en una whitelist. Contacta a Finkok y pregunta:
- Si la IP del servidor anterior estaba en whitelist
- Si necesitas agregar la nueva IP (72.62.128.98) a la whitelist

### Opción 4: Usar URL de Producción

Si el servidor anterior usaba producción, prueba cambiar:

```yaml
finkok:
  stamp:
    url: https://facturacion.finkok.com/servicios/soap/stamp  # Producción
```

### Opción 5: Verificar Certificados SSL

```bash
# Verificar certificados del sistema
ls -la /etc/ssl/certs/ | grep -i finkok

# Probar conexión SSL
openssl s_client -connect demo-facturacion.finkok.com:443 -showcerts < /dev/null 2>&1 | head -30
```

### Opción 6: Configurar DNS Específico

Si hay problemas de DNS:

```bash
# Editar /etc/resolv.conf
sudo nano /etc/resolv.conf

# Agregar DNS de Google
nameserver 8.8.8.8
nameserver 8.8.4.4
```

## Preguntas Clave

1. **¿Qué IP tenía el servidor anterior?**
   - Puede estar en whitelist de Finkok

2. **¿El servidor anterior tenía alguna configuración especial?**
   - VPN
   - Proxy
   - Túnel
   - Configuración de red diferente

3. **¿Qué versión de Java tenía el servidor anterior?**
   ```bash
   # En el servidor nuevo
   java -version
   ```

4. **¿El servidor anterior estaba en la misma red/proveedor?**
   - Puede haber firewall de red diferente

## Acción Inmediata

Ejecuta estos comandos y comparte los resultados:

```bash
# 1. Información del sistema
uname -a
java -version

# 2. Configuración de red completa
ip addr show
ip route show
cat /etc/resolv.conf

# 3. Probar conexión SSL
openssl s_client -connect demo-facturacion.finkok.com:443 -showcerts < /dev/null 2>&1 | head -30

# 4. Verificar si hay IPv6
ip -6 addr show

# 5. Probar con IPv4 forzado
curl -4 -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20
```

## Cambios en el Código

He agregado:
1. ✅ Soporte para forzar IPv4
2. ✅ Timeouts configurables
3. ✅ Mejor logging de configuración

Ahora puedes probar configurando en `application.yml`:

```yaml
finkok:
  connection:
    use-ipv4-only: true
    connect-timeout: 60000
    read-timeout: 180000
```

Recompila y redespliega para probar.
