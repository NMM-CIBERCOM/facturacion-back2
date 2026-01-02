# Guía de Perfiles de Spring Boot

Este proyecto utiliza perfiles de Spring Boot para manejar diferentes entornos (desarrollo y producción).

## Perfiles Disponibles

### 🛠️ Perfil de Desarrollo (`dev`)
Configuración para desarrollo local.

**Uso:**
```bash
# Para usar desarrollo local (requiere Oracle local):
mvn spring-boot:run -Dspring-boot.run.profiles=dev,oracle
```

**Características:**
- Base de datos: Oracle en `localhost:1521/XE`
- Servidor: `http://localhost:8080`
- CORS: Permite localhost (5173, 5174)
- Show SQL: Habilitado para debugging
- Finkok: Modo demo

### 🚀 Perfil de Producción (`prod`)
Configuración para el entorno de producción.

**Uso:**
```bash
# Perfil activo por defecto (prod,oracle)
mvn spring-boot:run

# O explícitamente:
mvn spring-boot:run -Dspring-boot.run.profiles=prod,oracle

# O en el JAR:
java -jar app.jar --spring.profiles.active=prod,oracle
```

**Características:**
- Base de datos: Oracle en servidor de producción (`174.136.25.157:1521/XE`)
- Servidor: `http://174.136.25.157:8080`
- CORS: Permite producción y localhost
- Show SQL: Deshabilitado
- Finkok: Configurado (puede ser demo o producción según necesidad)

### 📊 Perfil de Base de Datos (`oracle`)
Perfil específico para configuración de Oracle. Se activa automáticamente con `dev` o `prod`.

### 🍃 Perfil de Base de Datos (`mongo`)
Perfil opcional para MongoDB (no se usa por defecto).

## Cómo Cambiar de Perfil

### Opción 1: Variable de Entorno
```bash
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

### Opción 2: Parámetro de Línea de Comandos
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Opción 3: En el JAR
```bash
java -jar facturacion-back.jar --spring.profiles.active=prod
```

### Opción 4: En application.yml (temporal)
```yaml
spring:
  profiles:
    active: prod
```

## Configuración Actual

### Desarrollo (dev) ⚠️
- **Base de datos:** `jdbc:oracle:thin:@//localhost:1521/XE`
- **Usuario:** `nick`
- **Puerto servidor:** `8080`
- **URL base:** `http://localhost:8080`
- **Estado:** Requiere Oracle local corriendo (actualmente no configurado)

### Producción (prod) ✅
- **Base de datos:** `jdbc:oracle:thin:@//174.136.25.157:1521/XE`
- **Usuario:** `nick`
- **Puerto servidor:** `8080`
- **URL base:** `http://174.136.25.157:8080`
- **Estado:** Funcionando correctamente

## Notas Importantes

1. **Perfil por defecto:** Por defecto se usa `prod,oracle` (servidor de producción) ya que funciona correctamente.

2. **Para usar desarrollo local:** Activa el perfil `dev,oracle` cuando tengas Oracle corriendo localmente:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev,oracle
   ```

3. **Combinación de perfiles:** Los perfiles `dev` y `prod` deben combinarse con `oracle` para la base de datos: `dev,oracle` o `prod,oracle`.

3. **CORS:** La configuración de CORS se ajusta automáticamente según el perfil activo.

4. **Finkok:** Actualmente ambos perfiles usan el modo demo. Para producción real, actualiza las URLs en el perfil `prod`.

## Solución de Problemas

### Error: "Cannot determine embedded database driver class"
**Solución:** Asegúrate de que el perfil `oracle` esté activo o que estés usando `dev` o `prod`.

### Error de conexión a base de datos
**Solución:** 
- En desarrollo: Verifica que Oracle esté corriendo en `localhost:1521`
- En producción: Verifica la conectividad al servidor `174.136.25.157:1521`

### CORS bloqueado
**Solución:** Verifica que la URL del frontend esté incluida en `cors.allowed-origins` del perfil activo.

