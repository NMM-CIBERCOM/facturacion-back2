# Diagnóstico: Error 500 al hacer login en Tomcat remoto

## Síntoma
- Al abrir la aplicación en el servidor (ej. `https://redcibercom.cloud/facturacion-backend/`) y hacer login, aparece **"Error interno del servidor"** y en la consola del navegador: **POST 500 (Internal Server Error)**.

## Causas habituales

### 1. Conexión a Oracle
El login usa `UsuarioService.autenticarUsuario()`, que consulta las tablas `USUARIOS` y `PERFIL` en Oracle. Si el backend en Tomcat no puede conectar a la base de datos, se lanza una excepción y se devuelve 500.

**Qué revisar en el servidor:**
- Variables de entorno o `application-prod.yml` / JNDI con la URL JDBC de Oracle.
- Que el servidor donde corre Tomcat tenga red hacia el servidor Oracle (puerto 1521 o el que uses).
- Que el driver de Oracle esté en el classpath de Tomcat (o incluido en el WAR).

### 2. CORS (ya cubierto en código)
Si el frontend está en `https://redcibercom.cloud` y el backend en el mismo dominio, el origen debe estar permitido. En este proyecto ya están añadidos `https://redcibercom.cloud` y `http://redcibercom.cloud` en `SecurityConfig` y en `AuthController` (@CrossOrigin).

### 3. Tablas o esquema distintos
Si en el Oracle del servidor remoto las tablas se llaman distinto o no existen (`USUARIOS`, `PERFIL`), la consulta fallará y se verá como 500.

---

## Cómo ver el error real

### Opción A: Ver el error en el navegador (sin acceder al servidor)

Si el backend está desplegado con la versión que incluye soporte de depuración:

1. En la **consola del navegador** (F12 → Console) ejecuta:  
   `localStorage.setItem('cibercom.debugLogin', 'true')`
2. Vuelve a intentar **Iniciar sesión**.  
   El mensaje de error real (ej. conexión a Oracle, tabla no existe) aparecerá en rojo debajo del botón.

O bien abre la URL de login con parámetro:  
`https://redcibercom.cloud/facturacion-backend/?debug=1` y vuelve a intentar login.

### Opción B: Logs del backend en Tomcat

El mensaje "Error interno del servidor" es genérico; la causa también aparece en los **logs del backend en Tomcat**.

**En el servidor donde corre Tomcat:**

1. **Logs de la aplicación** (si configuraste `logging.file.name` o similar):
   - Ej. `/var/log/facturacion/server.log` o la ruta que tengas en `application.yml`.

2. **Logs estándar de Tomcat:**
   - `catalina.out`: `tail -f /opt/tomcat/logs/catalina.out` (o la ruta de tu instalación).
   - O `localhost.*.log` en la misma carpeta.

3. **Buscar la excepción del login:**
   - Tras reproducir el error (intentar login), buscar líneas que contengan:
     - `Error durante el login`
     - `autenticarUsuario`
     - `DataAccessException` / `SQLException` / `CannotCreateTransactionException`
   - Ahí aparecerá el mensaje real (ej. "Connection refused", "ORA-00942: table or view does not exist", etc.).

**Ejemplo:**
```text
ERROR ... Error durante el login [BadSqlGrammarException]: ...
  Caused by: java.sql.SQLSyntaxErrorException: ORA-00942: table or view does not exist
```
Eso indicaría que la tabla no existe o el usuario de Oracle no tiene permisos sobre ella.

---

## Resumen de cambios en código (para este error)

1. **CORS**: Añadidos orígenes `https://redcibercom.cloud` y `http://redcibercom.cloud` en:
   - `SecurityConfig.java` (CorsConfigurationSource)
   - `AuthController.java` (@CrossOrigin)

2. **AuthController**: Manejo más seguro del resultado de `autenticarUsuario` (evitar NPE si `resultado` o campos son null) y log con el tipo de excepción para facilitar el diagnóstico en logs de Tomcat.

3. **Próximo paso**: Revisar en el servidor los logs indicados arriba para ver la excepción concreta (Oracle, tabla, etc.) y corregir configuración o esquema según corresponda.
