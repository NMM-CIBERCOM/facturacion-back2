# Diagnosticar Túnel SSH

## Problema

El proceso SSH está corriendo pero el puerto 8443 no está escuchando. Esto significa que el túnel no se estableció correctamente.

## Solución: Ver el Error

### 1. Matar los procesos SSH existentes:

```bash
sudo pkill -f "ssh.*8443"
```

### 2. Ejecutar el túnel SIN el flag -f para ver el error:

```bash
sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -v
```

**IMPORTANTE:** NO cierres la terminal. Deja que se ejecute y comparte TODO el output que aparezca. Esto mostrará por qué el túnel no se está estableciendo.

## Posibles Problemas

1. **Autenticación fallida:** La contraseña puede ser incorrecta
2. **El servidor anterior no puede conectarse a Finkok:** Aunque dijiste que funcionaba antes, puede que ahora también esté bloqueado
3. **Permisos SSH:** El servidor anterior puede tener restricciones

## Alternativa: Verificar desde el Servidor Anterior

Si tienes acceso al servidor anterior, verifica que SÍ puede conectarse a Finkok:

```bash
# Desde el servidor anterior (174.136.25.157)
curl -v --connect-timeout 5 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -15
```

Si el servidor anterior TAMBIÉN da timeout, entonces el problema es más amplio y necesitamos otra solución.
