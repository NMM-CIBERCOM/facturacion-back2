# Verificar Túnel SSH

## Comandos para Verificar

Ejecuta estos comandos para verificar si el túnel SSH se creó correctamente:

### 1. Verificar si el proceso SSH está corriendo:

```bash
ps aux | grep ssh | grep 8443
```

### 2. Verificar si el puerto 8443 está escuchando:

```bash
netstat -tuln | grep 8443
```

O:

```bash
ss -tuln | grep 8443
```

### 3. Probar el túnel:

```bash
curl -v --connect-timeout 5 -k https://localhost:8443/servicios/soap/stamp 2>&1 | head -15
```

## Si el Túnel NO está Funcionando

Ejecuta el comando sin el flag `-f` para ver el error:

```bash
sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -v
```

El `-v` mostrará información detallada de lo que está pasando. **NO cierres la terminal** hasta ver el resultado.

## Si el Túnel SÍ está Funcionando

Si el `curl` funciona, entonces el túnel está activo y puedo modificar el código para usarlo.
