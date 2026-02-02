# Solución: Configurar Túnel SSH para Finkok

## Problema
El servidor no puede conectarse directamente a Finkok debido a bloqueos de firewall. El error muestra "Connect timed out" al intentar conectarse a `https://demo-facturacion.finkok.com:443`.

## Solución: Túnel SSH
Crear un túnel SSH desde un servidor que SÍ tenga acceso a Finkok hacia el servidor actual, redirigiendo el tráfico a través del túnel.

## Paso 1: Crear el Túnel SSH en el Servidor

### Opción A: Usando sshpass (Recomendado)

1. **Instalar sshpass en el servidor:**
   ```bash
   sudo apt-get update
   sudo apt-get install -y sshpass
   ```

2. **Crear el túnel SSH (versión mejorada que no se cae):**
   ```bash
   sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ExitOnForwardFailure=yes -o BatchMode=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -f
   ```
   
   **Explicación de opciones para evitar que se caiga:**
   - `-o ServerAliveInterval=30`: Envía señal cada 30 segundos para mantener conexión viva
   - `-o ServerAliveCountMax=3`: Reintenta 3 veces antes de cerrar
   - `-o TCPKeepAlive=yes`: Mantiene conexión TCP activa
   - `-o ExitOnForwardFailure=yes`: Sale si el forwarding falla (evita túnel roto)
   - `-L 8443:demo-facturacion.finkok.com:443`: Redirige el puerto local 8443 hacia Finkok:443
   - `root@174.136.25.157`: Servidor intermedio que SÍ tiene acceso a Finkok
   - `-N`: No ejecutar comandos, solo crear el túnel
   - `-f`: Ejecutar en segundo plano

3. **Verificar que el túnel está funcionando:**
   ```bash
   # Ver procesos SSH
   ps aux | grep ssh | grep 8443
   
   # Verificar que el puerto está escuchando
   netstat -tuln | grep 8443
   
   # Probar el túnel
   curl -v --connect-timeout 5 -k https://localhost:8443/servicios/soap/stamp 2>&1 | head -15
   ```

### Opción B: Usando expect (Si sshpass no funciona)

1. **Instalar expect:**
   ```bash
   sudo apt-get install -y expect
   ```

2. **Crear script `crear-tunel-finkok.sh` (versión mejorada):**
   ```bash
   #!/usr/bin/expect -f
   set timeout 30
   spawn ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ExitOnForwardFailure=yes -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N
   expect {
       "password:" {
           send "AdminPts@2026\r"
           exp_continue
       }
       "Permission denied" {
           exit 1
       }
       timeout {
           exit 1
       }
   }
   # Mantener el script corriendo indefinidamente
   set timeout -1
   expect eof
   ```

3. **Dar permisos y ejecutar:**
   ```bash
   chmod +x crear-tunel-finkok.sh
   nohup ./crear-tunel-finkok.sh > /tmp/tunnel.log 2>&1 &
   ```

4. **Verificar que está corriendo:**
   ```bash
   # Esperar 2 segundos
   sleep 2
   
   # Verificar proceso
   ps aux | grep expect | grep crear-tunel
   
   # Ver logs si hay problemas
   tail -f /tmp/tunnel.log
   ```

## Paso 2: Hacer el Túnel Permanente (Opcional pero Recomendado)

Crear un servicio systemd para que el túnel se inicie automáticamente:

1. **Crear archivo de servicio:**
   ```bash
   sudo nano /etc/systemd/system/finkok-tunnel.service
   ```

2. **Contenido del servicio (versión mejorada que se reconecta automáticamente):**
   ```ini
   [Unit]
   Description=SSH Tunnel to Finkok
   After=network.target
   Wants=network-online.target

   [Service]
   Type=simple
   User=root
   ExecStart=/usr/bin/sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ExitOnForwardFailure=yes -o BatchMode=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N
   Restart=always
   RestartSec=5
   StandardOutput=journal
   StandardError=journal

   [Install]
   WantedBy=multi-user.target
   ```

3. **Habilitar y iniciar el servicio:**
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable finkok-tunnel.service
   sudo systemctl start finkok-tunnel.service
   sudo systemctl status finkok-tunnel.service
   ```

## Paso 3: Configurar la Aplicación

1. **Editar `application.yml`:**
   ```yaml
   finkok:
     tunnel:
       enabled: true          # Habilitar túnel SSH
       local-port: 8443       # Puerto local del túnel
   ```

2. **Reiniciar la aplicación** para que tome la nueva configuración.

## Paso 4: Verificar que Funciona

1. **Verificar que el túnel está activo:**
   ```bash
   ps aux | grep ssh | grep 8443
   netstat -tuln | grep 8443
   ```

2. **Probar desde la aplicación:**
   - Intentar timbrar una factura
   - Si funciona, el túnel está configurado correctamente

## Troubleshooting

### Los comandos se caen en la terminal

**⚠️ IMPORTANTE: Si TODOS los comandos se caen inmediatamente, ve a `DIAGNOSTICO_TUNEL_SSH.md` para diagnóstico detallado.**

Si los comandos SSH se cierran inmediatamente después de ejecutarlos:

1. **Verificar que el proceso sigue corriendo:**
   ```bash
   ps aux | grep ssh | grep 8443
   ```
   Si no aparece, el proceso se cerró.

2. **Verificar logs del túnel:**
   ```bash
   # Si usaste nohup, ver el log:
   tail -f /tmp/tunnel.log
   
   # Si usas el servicio systemd:
   sudo journalctl -u finkok-tunnel.service -f
   ```

3. **Ejecutar en modo interactivo primero (para ver errores):**
   ```bash
   sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -v
   ```
   El `-v` muestra información detallada. Presiona Ctrl+C para salir.

4. **Si el túnel se cae inmediatamente, usar el servicio systemd** (recomendado):
   - El servicio se reconecta automáticamente cada 5 segundos si se cae
   - Ver sección "Paso 2: Hacer el Túnel Permanente"

### El túnel se cae frecuentemente
- **Usar las opciones mejoradas:** `ServerAliveInterval=30`, `ServerAliveCountMax=3`, `TCPKeepAlive=yes`
- **Usar el servicio systemd** con `Restart=always` y `RestartSec=5` para reconexión rápida
- **Verificar logs del servicio:** `sudo journalctl -u finkok-tunnel.service -f`
- **Verificar conectividad del servidor intermedio:** `ping 174.136.25.157`

### Error "Connection refused" en localhost:8443
- Verificar que el túnel está activo: `ps aux | grep ssh | grep 8443`
- Verificar que el puerto está escuchando: `netstat -tuln | grep 8443`
- Reiniciar el túnel si es necesario

### Error "Permission denied"
- Verificar que la contraseña es correcta
- Verificar que el usuario tiene permisos SSH en el servidor intermedio

## Notas Importantes

- El túnel debe estar activo ANTES de que la aplicación intente conectarse
- Si el túnel se cae, la aplicación fallará con "Connect timed out"
- El servicio systemd asegura que el túnel se reinicie automáticamente si se cae
- El puerto 8443 es local, no necesita estar abierto en el firewall
