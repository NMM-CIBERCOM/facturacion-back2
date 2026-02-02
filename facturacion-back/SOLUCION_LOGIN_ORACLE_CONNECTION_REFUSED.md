# Solución: Error de login por Oracle (Connection refused localhost 1521)

## Qué dice el log

Los mensajes clave son:

1. **`Connection refused, socket connect lapse 0 ms. localhost 1521`**  
   La aplicación intenta conectar a Oracle en **localhost:1521** y la conexión es rechazada.

2. **`HikariPool-5 - Connection is not available, request timed out after 30000ms`**  
   El pool de conexiones no puede obtener (ni crear) una conexión a la base de datos.

3. **`CannotCreateTransactionException: Could not open JPA EntityManager for transaction`**  
   Al hacer login, el backend no puede abrir una transacción porque no hay conexión a Oracle.

## Causa

El backend está arrancando con el **perfil de desarrollo (`dev`)**. En ese perfil la URL de Oracle está configurada así:

- **dev:** `jdbc:oracle:thin:@//localhost:1521/xepdb1`
- **prod:** `jdbc:oracle:thin:@//174.136.25.157:1521/XE`

Si la aplicación corre en un servidor (por ejemplo redcibercom.cloud) **sin** activar el perfil `prod`, usa `localhost:1521`. En ese servidor no hay Oracle escuchando en localhost, por eso aparece **Connection refused**.

## Solución

Hay que hacer que en el servidor (Tomcat) la aplicación use el perfil **prod** (o la URL de Oracle correcta para ese entorno).

### Opción 1: Variable de entorno en el servidor (recomendado)

En el servidor donde corre Tomcat, definir:

**Linux (ej. en `setenv.sh` de Tomcat o en el servicio):**
```bash
export SPRING_PROFILES_ACTIVE=prod,oracle
```

**Windows (variable de entorno del sistema o del servicio Tomcat):**
```
SPRING_PROFILES_ACTIVE=prod,oracle
```

Luego reiniciar Tomcat. Con `prod` activo, la app usará `174.136.25.157:1521/XE` en lugar de localhost.

### Opción 2: Parámetro JVM al arrancar Tomcat

Añadir a la línea de arranque de Tomcat (por ejemplo en `catalina.sh` o en la configuración del servicio):

```
-Dspring.profiles.active=prod,oracle
```

### Opción 3: Oracle en otro host/puerto

Si en producción Oracle está en **otro host o puerto** (no en `174.136.25.157:1521`), hay que:

1. Crear o usar un perfil (ej. `prod`) en `application.yml` con la URL correcta, o  
2. Sobrescribir la URL con variables de entorno:

   ```bash
   export SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//TU_HOST_ORACLE:1521/TU_SERVICIO
   export SPRING_DATASOURCE_USERNAME=usuario
   export SPRING_DATASOURCE_PASSWORD=contraseña
   ```

Y asegurarse de que el perfil activo sea el que usa esas propiedades (por ejemplo `prod`).

## Comprobar que Oracle está accesible

Desde el **mismo servidor** donde corre la aplicación (o Tomcat), verificar que se puede conectar a Oracle:

- Si usas perfil **prod** y Oracle está en `174.136.25.157:1521`:
  - Que el servidor tenga red hasta `174.136.25.157` (firewall, VPN, etc.).
  - Que el puerto 1521 esté abierto hacia ese host.

- Si Oracle estuviera en el mismo servidor (localhost):
  - Que el servicio Oracle esté levantado.
  - Que escuche en el puerto 1521 (listener).

## Resumen

| Síntoma                         | Causa probable                          | Acción principal                          |
|---------------------------------|-----------------------------------------|------------------------------------------|
| Connection refused localhost 1521| App usa perfil `dev` (localhost)        | Activar perfil `prod` en el servidor     |
| Timeout / pool sin conexiones   | No llega a Oracle (URL o red incorrecta)| Revisar URL en `prod` y conectividad     |

Una vez activado el perfil correcto y comprobada la conectividad a Oracle, el login debería funcionar (si el usuario/contraseña existen en la base).
