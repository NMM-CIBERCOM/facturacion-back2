# Solución: Túnel SSH Corregido

## Error: Host key verification failed

Este error ocurre porque SSH no reconoce la clave del host. Hay dos opciones:

## Opción 1: Aceptar la Clave del Host (Recomendado)

### Desde el Nuevo Servidor (72.62.128.98):

```bash
# Primero, aceptar la clave del host del servidor anterior
ssh -o StrictHostKeyChecking=accept-new usuario@ip-servidor-anterior "echo 'Clave aceptada'"

# O si conoces la IP del servidor anterior, reemplaza 'ip-servidor-anterior'
```

## Opción 2: Túnel SSH Directo (Más Simple)

### Desde el Nuevo Servidor (72.62.128.98):

Si conoces la IP y credenciales del servidor anterior:

```bash
# Crear túnel SSH directo (reemplaza IP_SERVIDOR_ANTERIOR con la IP real)
ssh -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 usuario@IP_SERVIDOR_ANTERIOR -N -f
```
ssh -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -f
```
Esto:
- Crea un túnel local en el nuevo servidor en `localhost:8443`
- Redirige el tráfico a través del servidor anterior a Finkok
- `-o StrictHostKeyChecking=no` evita el error de verificación de clave
- `-N` no ejecuta comandos remotos
- `-f` ejecuta en segundo plano

## Opción 3: Si NO tienes acceso SSH al servidor anterior

En este caso, necesitas:

1. **Preguntar a tu compañero:**
   - ¿Cuál es la IP del servidor anterior?
   - ¿Cuáles son las credenciales SSH?
   - ¿Puedes darme acceso SSH al servidor anterior?

2. **O comparar configuraciones:**
   - Pide a tu compañero que ejecute en el servidor anterior:
     ```bash
     ip route show
     cat /etc/resolv.conf
     sudo iptables -L OUTPUT -n -v | head -20
     ```
   - Y compara con el nuevo servidor para ver qué diferencia hay

## Después de Crear el Túnel

Una vez que el túnel esté funcionando, modificaré el código para usar `https://localhost:8443` en lugar de la URL directa de Finkok.
