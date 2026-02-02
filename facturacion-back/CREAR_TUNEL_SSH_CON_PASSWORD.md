# Crear Túnel SSH con Contraseña

## Problema

SSH está pidiendo contraseña pero no puede ingresarla de forma interactiva desde WinSCP.

## Solución: Usar sshpass

### 1. Instalar sshpass en el nuevo servidor:

```bash
sudo apt-get update
sudo apt-get install -y sshpass
```

### 2. Crear túnel con contraseña:

```bash
# Usar la contraseña que ya conoces
sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -f
```

### 3. Verificar que el túnel está funcionando:

```bash
# Ver procesos SSH
ps aux | grep ssh | grep 8443

# Probar el túnel
curl -v --connect-timeout 5 -k https://localhost:8443/servicios/soap/stamp 2>&1 | head -15
```

### 4. Si funciona, hacer el túnel permanente:

Crear un servicio systemd para que el túnel se inicie automáticamente.

## Alternativa: Si sshpass no funciona

Podemos modificar el código para que intente conectarse directamente, y si falla, use un proxy HTTP si está disponible. Pero primero intenta con sshpass.
