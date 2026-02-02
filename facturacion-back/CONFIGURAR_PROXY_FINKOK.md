# Configurar Proxy para Finkok - Guía Paso a Paso

## ✅ Cambios Implementados

He modificado el código para que soporte proxy configurable. Ahora puedes configurar un proxy directamente desde `application.yml` sin necesidad de modificar código.

## Paso 1: Editar application.yml

Abre el archivo `application.yml` y busca la sección de Finkok. En el perfil de **producción** (línea ~213), descomenta y configura el proxy:

```yaml
finkok:
  enabled: true
  username: nm9282672@gmail.com
  password: N1C0LASm_
  stamp:
    url: https://demo-facturacion.finkok.com/servicios/soap/stamp
  # Configuración de proxy - DESCOMENTAR Y CONFIGURAR
  proxy:
    enabled: true
    host: TU_PROXY_HOST  # Ejemplo: proxy.example.com o 192.168.1.100
    port: 8080           # Puerto del proxy
  cancel:
    url: https://demo-facturacion.finkok.com/servicios/soap/cancel
```

## Paso 2: Obtener Información del Proxy

### Opción A: Si Tienes un Proxy Corporativo

Pregunta a tu administrador de red:
- Host del proxy
- Puerto del proxy
- Si requiere autenticación

### Opción B: Usar Proxy Público (Solo para Pruebas)

⚠️ **ADVERTENCIA:** Los proxies públicos NO son seguros para producción.

Algunos servicios de proxy públicos (solo para pruebas):
- No recomendado para producción
- Pueden interceptar datos
- Pueden ser lentos o inestables

### Opción C: Configurar tu Propio Proxy

Si tienes acceso a otro servidor que SÍ puede acceder a Finkok, puedes configurar un proxy allí.

## Paso 3: Ejemplo de Configuración

### Ejemplo 1: Proxy Corporativo

```yaml
finkok:
  proxy:
    enabled: true
    host: proxy.empresa.com
    port: 3128
```

### Ejemplo 2: Proxy Local (si tienes otro servidor)

```yaml
finkok:
  proxy:
    enabled: true
    host: 192.168.1.100  # IP del servidor con proxy
    port: 8080
```

### Ejemplo 3: Deshabilitar Proxy (por defecto)

```yaml
finkok:
  proxy:
    enabled: false
    # host y port no son necesarios si enabled es false
```

## Paso 4: Recompilar y Redesplegar

Después de configurar el proxy:

1. **Recompilar el proyecto:**
   ```bash
   mvn clean package
   ```

2. **Redesplegar el WAR:**
   - Sube el nuevo WAR al servidor
   - Reinicia Tomcat

3. **Verificar logs:**
   ```bash
   tail -f /var/log/facturacion/server.log
   ```

Deberías ver en los logs:
```
Usando proxy: proxy.example.com:8080
```

## Paso 5: Verificar que Funciona

1. **Intenta generar una factura** desde la aplicación
2. **Revisa los logs** para confirmar que usa el proxy
3. **Verifica** que ya no hay timeout

## Solución Alternativa: Proxy a Nivel de Sistema

Si prefieres configurar el proxy a nivel del sistema operativo (sin modificar application.yml):

### Configurar Variables de Entorno

```bash
# Editar /etc/environment
sudo nano /etc/environment
```

Agregar:
```
http_proxy="http://proxy_host:puerto"
https_proxy="http://proxy_host:puerto"
```

Luego, el código Java usará automáticamente estas variables si están configuradas.

## Notas Importantes

1. **El proxy debe soportar HTTPS** (conexiones SSL/TLS)
2. **Si el proxy requiere autenticación**, necesitarás modificar el código adicionalmente
3. **Prueba primero** con un proxy de prueba antes de usar en producción
4. **El código funciona sin proxy** si `enabled: false` o si no está configurado

## Troubleshooting

### Si el proxy no funciona:

1. **Verifica que el proxy esté accesible:**
   ```bash
   curl -v --proxy http://proxy_host:puerto https://demo-facturacion.finkok.com/servicios/soap/stamp
   ```

2. **Verifica los logs** para ver si está usando el proxy

3. **Verifica la configuración** en `application.yml`

4. **Reinicia Tomcat** después de cambiar la configuración

## ¿Necesitas un Proxy?

Si no tienes un proxy disponible, opciones:

1. **Contactar al proveedor del servidor** para que configuren un proxy
2. **Usar un servicio de proxy** (solo para pruebas, no producción)
3. **Configurar un proxy en otro servidor** que sí tenga acceso a Finkok
