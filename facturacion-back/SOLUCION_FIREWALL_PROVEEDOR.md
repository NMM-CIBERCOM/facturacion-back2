# Solución: Firewall del Proveedor Bloqueando Finkok

## Diagnóstico

- ✅ UFW permite salida en 443
- ✅ iptables OUTPUT está en ACCEPT
- ❌ Pero el puerto 443 a Finkok está bloqueado

Esto indica que **hay un firewall del proveedor/hosting bloqueando la conexión**.

## Verificaciones

Ejecuta estos comandos para confirmar:

### 1. Probar otros sitios HTTPS:

```bash
# Probar Google (debería funcionar si el problema es específico de Finkok)
curl -v --connect-timeout 5 https://www.google.com 2>&1 | head -10

# Probar GitHub
curl -v --connect-timeout 5 https://github.com 2>&1 | head -10
```

### 2. Verificar routing a Finkok:

```bash
# Ver ruta a Finkok
traceroute -m 10 69.160.41.169 2>&1 | head -15
```

### 3. Probar con telnet/nc:

```bash
# Probar conexión TCP directa
nc -zv -w 5 69.160.41.169 443
```

## Soluciones

### Opción 1: Contactar al Proveedor del Servidor

Si otros sitios HTTPS funcionan pero Finkok no, necesitas contactar al proveedor del servidor (quien te dio las credenciales) y pedirles que:

1. **Permitan conexiones salientes HTTPS** al dominio `demo-facturacion.finkok.com`
2. O permitan conexiones a la IP `69.160.41.169` en el puerto 443

**Información para el proveedor:**
- Dominio: `demo-facturacion.finkok.com`
- IP: `69.160.41.169`
- Puerto: `443` (HTTPS)
- Protocolo: TCP

### Opción 2: Verificar si hay Firewall de Red Interno

Si el servidor está detrás de un router/firewall, puede que necesites configurar reglas allí.

### Opción 3: Usar un Proxy HTTP/HTTPS (si está disponible)

Si el proveedor tiene un proxy disponible, podrías configurarlo, pero esto requeriría modificar el código para usar proxy explícito.

## Verificar después de que el proveedor permita la conexión

```bash
# Probar nuevamente
curl -v --connect-timeout 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20
```

Si `curl` funciona, entonces Java también debería funcionar automáticamente.
