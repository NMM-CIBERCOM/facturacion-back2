# Prueba: Forzar IPv4 y Aumentar Timeouts

## Cambios Implementados

He agregado configuraciones que pueden ayudar si el problema es específico de este servidor:

1. ✅ **Forzar IPv4** (si hay problemas con IPv6)
2. ✅ **Timeouts configurables** (puedes aumentarlos)
3. ✅ **Mejor logging** de configuración

## Prueba Rápida: Forzar IPv4

### Paso 1: Editar application.yml

En el perfil de **producción** (línea ~219), modifica:

```yaml
finkok:
  connection:
    use-ipv4-only: true  # ← Cambiar a true
    connect-timeout: 60000  # ← Aumentar a 60 segundos
    read-timeout: 180000    # ← Aumentar a 180 segundos
```

### Paso 2: Recompilar y Redesplegar

```bash
mvn clean package
# Subir WAR y reiniciar Tomcat
```

### Paso 3: Probar

Intenta generar una factura y revisa los logs.

## Otra Prueba: Configurar Java Directamente

Si la configuración en application.yml no funciona, configura directamente en Tomcat:

### Crear/Editar setenv.sh

```bash
sudo nano /opt/tomcat/bin/setenv.sh
```

**Agregar:**

```bash
#!/bin/sh
# Forzar IPv4 y mejorar conexiones
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv6Addresses=false"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -Dsun.net.useExclusiveBind=false"
```

**Dar permisos y reiniciar:**

```bash
sudo chmod +x /opt/tomcat/bin/setenv.sh
sudo systemctl restart tomcat
```

## Verificar desde el Servidor

Antes de probar, ejecuta estos comandos para diagnóstico:

```bash
# 1. Verificar si hay IPv6 configurado
ip -6 addr show

# 2. Probar con IPv4 forzado usando curl
curl -4 -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20

# 3. Verificar versión de Java
java -version

# 4. Ver configuración de red
ip route show
```

Si `curl -4` funciona pero el código no, entonces el problema puede ser que Java está intentando usar IPv6.

## Comparar con Servidor Anterior

Si puedes acceder al servidor anterior, ejecuta estos comandos y compara:

```bash
# En servidor anterior
java -version
ip route show
ip -6 addr show
cat /etc/resolv.conf
```

Las diferencias pueden indicar qué está causando el problema.
