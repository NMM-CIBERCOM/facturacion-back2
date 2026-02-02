# Soluciones para Finkok Bloqueado

## Diagnóstico Confirmado

- ✅ Google funciona (HTTPS saliente está permitido)
- ❌ Finkok NO funciona (bloqueado específicamente)
- ✅ Firewall del servidor está bien configurado
- ❌ Firewall del proveedor está bloqueando Finkok

## Soluciones Disponibles

### Opción 1: Verificar Panel del Gateway (Más Probable)

El gateway `72.62.128.254` puede tener un panel de administración donde puedas configurar reglas de firewall:

1. **Intenta acceder desde tu navegador:**
   - `http://72.62.128.254`
   - `https://72.62.128.254`
   - `http://72.62.128.254:8080`
   - `http://72.62.128.254:8443`

2. **Credenciales comunes:**
   - Usuario: `admin` / Contraseña: `admin`
   - Usuario: `root` / Contraseña: `AdminPts@2026` (la misma del servidor)
   - Usuario: `admin` / Contraseña: (vacía)

3. **Si puedes acceder, busca:**
   - Firewall Rules
   - Outbound Rules
   - Access Control
   - Y agrega regla para permitir `69.160.41.169:443`

### Opción 2: Preguntar a tu Compañero

Pregunta a tu compañero:
- ¿Hay un proxy HTTP/HTTPS corporativo disponible?
- ¿Puedes acceder al panel del gateway/router?
- ¿Hay otro servidor que SÍ pueda conectarse a Finkok? (para usar túnel SSH)

### Opción 3: Usar Túnel SSH (Si hay otro servidor disponible)

Si tienes acceso a otro servidor que SÍ puede conectarse a Finkok:

```bash
# Desde tu máquina local, crear túnel SSH
ssh -L 8443:demo-facturacion.finkok.com:443 usuario@servidor-que-si-funciona -N

# Luego modificar el código para usar localhost:8443
```

### Opción 4: Verificar si hay Proxy en la Red

Ejecuta estos comandos para verificar:

```bash
# Verificar configuración de red
cat /etc/resolv.conf

# Verificar si hay proxy configurado en algún lugar
grep -r "proxy" /etc/ 2>/dev/null | grep -i http | head -10
```

## Próximo Paso Recomendado

**Intenta acceder al panel del gateway** en `http://72.62.128.254` desde tu navegador. Es la solución más probable sin depender del proveedor.
