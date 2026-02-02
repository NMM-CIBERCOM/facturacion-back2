# Verificar Proxy a Nivel del Sistema Operativo

## Problema

Aunque `setenv.sh` está configurado correctamente y Java está usando las opciones, el stack trace sigue mostrando `SocksSocketImpl.connect`. Esto sugiere que hay configuración de proxy a nivel del sistema operativo.

## Verificaciones

Ejecuta estos comandos en la terminal de WinSCP:

### 1. Verificar configuración de proxy en el sistema:

```bash
# Verificar si hay proxy configurado en variables de entorno del sistema
cat /etc/environment
cat /etc/profile
cat ~/.bashrc
cat ~/.profile
```

### 2. Verificar configuración de red del sistema:

```bash
# Verificar configuración de red
ip route show
cat /etc/resolv.conf
```

### 3. Verificar si hay proxy configurado en Java a nivel del sistema:

```bash
# Verificar propiedades del sistema Java
java -XshowSettings:properties -version 2>&1 | grep -i proxy
```

### 4. Verificar conectividad directa a Finkok:

```bash
# Probar conexión directa
curl -v --connect-timeout 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20
```

### 5. Verificar si hay proxy SOCKS configurado:

```bash
# Verificar si hay proxy SOCKS en el sistema
netstat -tuln | grep -i socks
cat /etc/proxychains.conf 2>/dev/null || echo "No hay proxychains"
```

### 6. Verificar configuración de Java en Tomcat:

```bash
# Ver todas las propiedades del sistema que Java está usando
ps aux | grep tomcat | grep java | grep -o '\-D[^ ]*' | sort
```

## Posible Solución: Forzar Socket Directo

Si todo lo anterior está bien pero sigue usando `SocksSocketImpl`, el problema puede ser que Java está detectando un proxy SOCKS a nivel de red. En ese caso, necesitamos usar una biblioteca HTTP diferente que no use el sistema de proxy de Java.

Pero primero, verifica los comandos de arriba para ver si hay algo configurado.
