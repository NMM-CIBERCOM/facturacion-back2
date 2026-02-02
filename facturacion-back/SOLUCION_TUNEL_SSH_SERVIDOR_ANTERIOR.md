# Solución: Túnel SSH desde Servidor Anterior

## Concepto

El servidor anterior SÍ puede conectarse a Finkok. Podemos crear un túnel SSH que redirija el tráfico de Finkok desde el servidor anterior al nuevo servidor.

## Opción 1: Túnel SSH Reverso (Recomendado)

### Desde el Servidor Anterior (que SÍ funciona):

```bash
# Crear túnel SSH reverso desde el servidor anterior al nuevo
ssh -R 8443:demo-facturacion.finkok.com:443 root@72.62.128.98 -N
```

Esto crea un túnel donde:
- El servidor anterior escucha en `localhost:8443`
- Redirige el tráfico a `demo-facturacion.finkok.com:443`
- El nuevo servidor puede acceder a Finkok a través de `localhost:8443`

### Modificar el código para usar el túnel:

Necesitamos cambiar la URL de Finkok en `application.yml` a `https://localhost:8443`

## Opción 2: Túnel SSH Directo (Si tienes acceso SSH al servidor anterior)

### Desde el Nuevo Servidor:

```bash
# Crear túnel SSH directo al servidor anterior
ssh -L 8443:demo-facturacion.finkok.com:443 usuario@ip-servidor-anterior -N
```

Esto crea un túnel donde:
- El nuevo servidor escucha en `localhost:8443`
- Redirige el tráfico a través del servidor anterior a Finkok

## Opción 3: Comparar Configuraciones

Si tienes acceso al servidor anterior, compara las configuraciones:

```bash
# En el servidor anterior, ejecuta:
ip route show
cat /etc/resolv.conf
sudo iptables -L OUTPUT -n -v | head -20
env | grep -i proxy
```

Y compara con el nuevo servidor para ver qué diferencia permite que funcione.

## Implementación de Túnel SSH Permanente

Para que el túnel sea permanente, puedes usar `autossh` o crear un servicio systemd.

### Instalar autossh:

```bash
sudo apt-get update
sudo apt-get install -y autossh
```

### Crear túnel permanente:

```bash
# Desde el servidor anterior
autossh -M 20000 -R 8443:demo-facturacion.finkok.com:443 root@72.62.128.98 -N
```

## Modificar Código para Usar Túnel

Si usas el túnel, necesitas cambiar la URL en `application.yml`:

```yaml
finkok:
  stamp:
    url: https://localhost:8443/servicios/soap/stamp
```

Pero esto requerirá deshabilitar la validación del certificado SSL o configurar el certificado correctamente.
