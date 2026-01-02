# Diagnóstico: ¿Por qué no funciona el perfil DEV?

## 🔍 Problema Identificado

El error `ORA-01017: nombre de usuario/contraseña no válidos` en desarrollo puede deberse a varias causas:

## ✅ Posibles Causas y Soluciones

### 1. **Oracle no está instalado o corriendo localmente**

**Síntoma:** Error `ORA-01017` o `Connection refused`

**Verificación:**
```bash
# Verificar si Oracle está corriendo (Windows)
Get-Service | Where-Object {$_.DisplayName -like "*Oracle*"}

# Verificar si el puerto 1521 está escuchando
Test-NetConnection -ComputerName localhost -Port 1521
```

**Solución:**
- Instalar Oracle Express Edition (XE) o Oracle Database
- Iniciar el servicio Oracle (generalmente `OracleServiceXE` o similar)
- Asegurarse de que el listener esté corriendo

---

### 2. **Las credenciales son diferentes en local**

**Síntoma:** Error `ORA-01017: nombre de usuario/contraseña no válidos`

**Configuración actual en `application.yml` (perfil `dev`):**
```yaml
datasource:
  url: jdbc:oracle:thin:@//localhost:1521/XE
  username: nick
  password: N1C0LASm
```

**Solución:**
1. Verificar las credenciales correctas de tu Oracle local:
   ```sql
   -- Conectarse con un usuario administrador (SYSTEM o SYS)
   sqlplus system/password@localhost:1521/XE
   ```

2. Si las credenciales son diferentes, actualiza `application.yml`:
   ```yaml
   datasource:
     username: TU_USUARIO_LOCAL
     password: TU_PASSWORD_LOCAL
   ```

3. Si el usuario `nick` no existe en local, créalo:
   ```sql
   CREATE USER nick IDENTIFIED BY N1C0LASm;
   GRANT CONNECT, RESOURCE, DBA TO nick;
   ```

---

### 3. **La URL de conexión es incorrecta**

**Configuración actual:**
```yaml
url: jdbc:oracle:thin:@//localhost:1521/XE
```

**Formatos de URL posibles:**

- **Con SERVICE_NAME (recomendado):**
  ```yaml
  url: jdbc:oracle:thin:@//localhost:1521/XE
  ```

- **Con SID (si tu instalación usa SID):**
  ```yaml
  url: jdbc:oracle:thin:@localhost:1521:XE
  ```
  Nota: Sin `//` y usando `:` en lugar de `/` antes del nombre de la base.

- **Con TNS:**
  ```yaml
  url: jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=XE)))
  ```

**Verificación del formato correcto:**
1. Revisa tu archivo `tnsnames.ora` (generalmente en `ORACLE_HOME/network/admin/`)
2. O prueba la conexión con SQL*Plus:
   ```bash
   sqlplus nick/N1C0LASm@localhost:1521/XE
   ```
   Si funciona, el formato es correcto. Si no, prueba:
   ```bash
   sqlplus nick/N1C0LASm@localhost:1521:XE
   ```

---

### 4. **El listener de Oracle no está corriendo**

**Síntoma:** Error `Connection refused` o `ORA-12541: TNS:no listener`

**Verificación:**
```bash
# Verificar el listener (Windows)
Get-Service | Where-Object {$_.DisplayName -like "*Listener*"}

# O usando lsnrctl
lsnrctl status
```

**Solución:**
- Iniciar el servicio `OracleOraDB21Home1TNSListener` o similar
- O iniciar manualmente: `lsnrctl start`

---

### 5. **Firewall bloqueando el puerto 1521**

**Síntoma:** Timeout o conexión rechazada

**Verificación:**
```powershell
Test-NetConnection -ComputerName localhost -Port 1521
```

**Solución:**
- Permitir el puerto 1521 en el firewall de Windows
- O deshabilitar temporalmente el firewall para probar

---

## 🔧 Pasos de Diagnóstico Recomendados

### Paso 1: Verificar que Oracle esté corriendo
```powershell
# Listar servicios Oracle
Get-Service | Where-Object {$_.DisplayName -like "*Oracle*"}

# Verificar que estén "Running"
```

### Paso 2: Probar conexión manual con SQL*Plus
```bash
sqlplus nick/N1C0LASm@localhost:1521/XE
```

Si esto falla, el problema es de configuración de Oracle, no de Spring Boot.

### Paso 3: Verificar formato de URL
Si SQL*Plus funciona con una URL específica, usa ese mismo formato en `application.yml`.

### Paso 4: Revisar logs de Oracle
```bash
# Logs del listener
%ORACLE_HOME%\network\log\listener.log

# Logs de la base de datos
%ORACLE_HOME%\database\XE\XE\XE.log
```

---

## 📝 Configuración Mejorada para DEV

He agregado configuración adicional al datasource en `application.yml` para mejor diagnóstico:

```yaml
datasource:
  hikari:
    connection-timeout: 10000
    maximum-pool-size: 5
    minimum-idle: 1
    validation-timeout: 5000
    leak-detection-threshold: 60000
    # No validar la conexión al inicio (permite que la app inicie aunque Oracle no esté disponible)
    initialization-fail-timeout: -1
```

---

## 🎯 Próximos Pasos

1. **Ejecuta el diagnóstico paso a paso** comenzando por verificar si Oracle está corriendo
2. **Prueba la conexión con SQL*Plus** para confirmar credenciales y URL
3. **Actualiza la configuración** en `application.yml` si encuentras diferencias
4. **Reinicia el backend** y prueba de nuevo

---

## 💡 Alternativa: Usar Producción para Desarrollo

Si no puedes resolver el problema de Oracle local, puedes usar el perfil `prod` que ya funciona:

```bash
# El perfil por defecto ya está configurado como prod,oracle
mvn spring-boot:run
```

Esto conectará a la base de datos del servidor `174.136.25.157:1521/XE` que ya funciona correctamente.

