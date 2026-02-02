# Diagnóstico: Por qué se cae el túnel SSH

## Problema
Los comandos SSH se cierran inmediatamente, incluso con opciones de keepalive.

## Diagnóstico Paso a Paso

### Paso 1: Verificar conexión básica al servidor intermedio

**Ejecuta esto primero (sin túnel, solo conexión):**
```bash
sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -v root@174.136.25.157 "echo 'Conexión exitosa'"
```

**¿Qué esperar?**
- ✅ Si funciona: El servidor acepta la conexión, el problema es con el port forwarding
- ❌ Si falla: El problema es de autenticación o conectividad básica

### Paso 2: Verificar si el servidor permite Port Forwarding

**Ejecuta esto:**
```bash
sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -v root@174.136.25.157 "grep AllowTcpForwarding /etc/ssh/sshd_config"
```

**Posibles resultados:**
- `AllowTcpForwarding yes` → ✅ Port forwarding permitido
- `AllowTcpForwarding no` → ❌ Port forwarding BLOQUEADO (este es el problema)
- No encuentra el archivo o no tiene permisos → Necesitas verificar de otra forma

### Paso 3: Probar túnel en modo interactivo (sin -f)

**Ejecuta esto y deja que corra (NO presiones Ctrl+C):**
```bash
sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -v
```

**Observa:**
- ¿Qué mensaje muestra antes de cerrarse?
- ¿Dice "Permission denied"?
- ¿Dice "Port forwarding failed"?
- ¿Dice "Connection closed"?
- ¿Se queda colgado sin hacer nada?

### Paso 4: Verificar desde el servidor intermedio

**Si tienes acceso al servidor 174.136.25.157, ejecuta:**
```bash
# En el servidor intermedio (174.136.25.157)
curl -v https://demo-facturacion.finkok.com/servicios/soap/stamp
```

**¿Qué esperar?**
- ✅ Si funciona: El servidor intermedio SÍ puede conectarse a Finkok
- ❌ Si falla: El servidor intermedio TAMBIÉN está bloqueado

## Soluciones según el diagnóstico

### Si el Paso 1 falla (no puede conectarse al servidor intermedio)

**Problema:** Autenticación o conectividad básica

**Soluciones:**
1. Verificar que la contraseña es correcta
2. Verificar que el usuario `root` tiene acceso SSH
3. Probar con otro usuario si `root` está deshabilitado
4. Verificar que el puerto 22 está abierto en el firewall

### Si el Paso 2 muestra "AllowTcpForwarding no"

**Problema:** El servidor intermedio NO permite port forwarding

**Soluciones:**
1. **Contactar al administrador del servidor 174.136.25.157** para habilitar port forwarding
2. **Usar otro servidor intermedio** que SÍ permita port forwarding
3. **Usar autenticación por clave SSH** (a veces funciona aunque contraseña no)

### Si el Paso 3 muestra "Port forwarding failed"

**Problema:** El puerto 8443 local ya está en uso o hay conflicto

**Soluciones:**
1. Verificar si el puerto está en uso:
   ```bash
   netstat -tuln | grep 8443
   ```
2. Si está en uso, matar el proceso:
   ```bash
   # Encontrar el PID
   lsof -i :8443
   # Matar el proceso
   kill -9 <PID>
   ```
3. Usar otro puerto (ej: 8444):
   ```bash
   sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -L 8444:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -v
   ```
   Y cambiar en `application.yml`: `local-port: 8444`

### Si el Paso 4 falla (servidor intermedio no puede conectar a Finkok)

**Problema:** El servidor intermedio TAMBIÉN está bloqueado

**Solución:** Necesitas usar OTRO servidor intermedio que SÍ tenga acceso a Finkok

## Alternativa: Usar autenticación por clave SSH

Si la contraseña no funciona, intenta con clave SSH:

1. **Generar clave SSH (en tu servidor):**
   ```bash
   ssh-keygen -t rsa -b 4096 -f ~/.ssh/finkok_tunnel -N ""
   ```

2. **Copiar clave pública al servidor intermedio:**
   ```bash
   sshpass -p 'AdminPts@2026' ssh-copy-id -i ~/.ssh/finkok_tunnel.pub root@174.136.25.157
   ```

3. **Usar la clave para el túnel:**
   ```bash
   ssh -i ~/.ssh/finkok_tunnel -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -f
   ```

## Preguntas para diagnosticar

1. **¿Qué mensaje exacto ves cuando se cae el comando?**
   - Copia y pega el último mensaje antes de que se cierre

2. **¿Puedes conectarte al servidor 174.136.25.157 normalmente?**
   - Prueba: `ssh root@174.136.25.157` (sin sshpass)

3. **¿Tienes acceso al servidor 174.136.25.157 para verificar su configuración?**

4. **¿Hay otro servidor que SÍ tenga acceso a Finkok que puedas usar como intermedio?**
