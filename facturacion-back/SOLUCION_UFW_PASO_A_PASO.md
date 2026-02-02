# Solución Paso a Paso: Permitir Conexiones HTTPS Salientes

## Estado Actual
- UFW está activo
- Solo hay reglas de entrada (IN)
- No hay reglas explícitas de salida (OUT)

## Pasos a Ejecutar

### Paso 1: Verificar Política de Salida por Defecto

```bash
sudo ufw status verbose | grep "Default:"
```

Esto mostrará algo como:
- `Default: deny (incoming), allow (outgoing)` ← Esto es lo normal
- `Default: deny (incoming), deny (outgoing)` ← Esto bloquearía todo

### Paso 2: Agregar Regla Explícita para HTTPS Saliente

```bash
sudo ufw allow out 443/tcp
```

### Paso 3: Verificar que se Agregó la Regla

```bash
sudo ufw status numbered
```

Deberías ver algo como:
```
     To                         Action      From
     --                         ------      ----
[1]  22/tcp                     ALLOW IN    Anywhere
[2]  80,443/tcp (Nginx Full)    ALLOW IN    Anywhere
[3]  443/tcp                    ALLOW OUT   Anywhere    ← Esta es la nueva regla
```

### Paso 4: Probar Conectividad

```bash
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp
```

**Si funciona:** Verás una respuesta XML (aunque sea un error SOAP, eso significa que la conexión funciona)

**Si no funciona:** Puede ser que la política de salida esté en DENY, entonces:

```bash
# Cambiar política de salida a ALLOW
sudo ufw default allow outgoing

# Probar de nuevo
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp
```

### Paso 5: Reiniciar Tomcat

Una vez que la conectividad funcione:

```bash
sudo systemctl restart tomcat
# O
sudo /etc/init.d/tomcat restart
```

## Comandos Completos (Copia y Pega)

Ejecuta estos comandos en secuencia:

```bash
# 1. Ver política de salida
echo "=== Política de Salida ==="
sudo ufw status verbose | grep "Default:"

# 2. Agregar regla HTTPS saliente
echo ""
echo "=== Agregando regla HTTPS saliente ==="
sudo ufw allow out 443/tcp

# 3. Verificar regla agregada
echo ""
echo "=== Reglas actuales ==="
sudo ufw status numbered

# 4. Probar conectividad
echo ""
echo "=== Probando conectividad ==="
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -20
```

## Si Aún No Funciona

Si después de agregar la regla sigue sin funcionar:

1. **Verificar política de salida:**
   ```bash
   sudo ufw status verbose
   ```

2. **Si dice `deny (outgoing)`, cambiarla:**
   ```bash
   sudo ufw default allow outgoing
   ```

3. **Verificar si hay un firewall externo o router bloqueando:**
   - Contacta al administrador de red
   - Solicita que permitan conexiones HTTPS salientes (puerto 443)

4. **Verificar si necesitas proxy:**
   ```bash
   echo $http_proxy
   echo $https_proxy
   ```
