# 🔧 Diagnóstico - Perfiles No Cargan en Frontend

## 🚨 Problema Identificado

El frontend no está cargando los perfiles en el selector de configuración de menús. El endpoint `/api/menu-config/perfiles` existe pero no está devolviendo datos correctamente.

## 🔍 Diagnóstico Paso a Paso

### **Paso 1: Verificar Base de Datos**
```sql
@database_test_perfiles_endpoint.sql
```

Este script te mostrará:
- ✅ **Consulta exacta del servicio** - Lo que debería devolver
- ✅ **Estado de los perfiles** - Si tienen datos válidos
- ✅ **Usuarios y perfiles asignados** - Relaciones correctas
- ✅ **Configuración de menús** - Por perfil
- ✅ **Perfiles sin configuración** - Que pueden causar problemas

### **Paso 2: Probar Endpoints de Diagnóstico**

#### **2.1: Probar Conexión a Base de Datos**
```bash
# Abrir navegador y ir a:
http://localhost:8080/api/test-menu/db-test
```

**Debería mostrar:**
```json
{
  "success": true,
  "totalPerfiles": 4,
  "message": "Conexión a base de datos exitosa"
}
```

#### **2.2: Probar Estructura de Tabla**
```bash
# Abrir navegador y ir a:
http://localhost:8080/api/test-menu/perfil-structure
```

**Debería mostrar la estructura de la tabla PERFIL**

#### **2.3: Probar Endpoint de Perfiles**
```bash
# Abrir navegador y ir a:
http://localhost:8080/api/test-menu/perfiles
```

**Debería mostrar:**
```json
[
  {
    "idPerfil": 1,
    "nombrePerfil": "Operador de Crédito"
  },
  {
    "idPerfil": 2,
    "nombrePerfil": "Jefe de Crédito"
  },
  {
    "idPerfil": 3,
    "nombrePerfil": "Administrador"
  },
  {
    "idPerfil": 99,
    "nombrePerfil": "Super Administrador"
  }
]
```

### **Paso 3: Probar Endpoint Original**
```bash
# Abrir navegador y ir a:
http://localhost:8080/api/menu-config/perfiles
```

**Debería mostrar el mismo resultado que el endpoint de prueba**

### **Paso 4: Verificar Logs del Backend**
```bash
# En la carpeta del proyecto backend:
grep -i "perfil\|menu" logs/server.log
```

## 🛠️ Posibles Soluciones

### **Solución 1: Si el Endpoint de Prueba Funciona**
Si `/api/test-menu/perfiles` funciona pero `/api/menu-config/perfiles` no:
- El problema está en el `MenuConfigService`
- Revisar logs del servicio

### **Solución 2: Si Ningún Endpoint Funciona**
Si ambos endpoints fallan:
- Problema de conexión a base de datos
- Verificar configuración de `application.yml`

### **Solución 3: Si la Base de Datos Está Vacía**
Si `totalPerfiles` es 0:
- Ejecutar `@database_create_all_users.sql` nuevamente
- Verificar que los perfiles se crearon correctamente

### **Solución 4: Si Hay Error en la Consulta**
Si hay errores SQL:
- Verificar que la tabla `PERFIL` existe
- Verificar permisos de la base de datos

## 🔧 Archivos Creados para Diagnóstico

### **TestMenuController.java**
- ✅ Endpoint `/api/test-menu/perfiles` - Prueba directa de perfiles
- ✅ Endpoint `/api/test-menu/db-test` - Prueba de conexión
- ✅ Endpoint `/api/test-menu/perfil-structure` - Estructura de tabla

### **database_test_perfiles_endpoint.sql**
- ✅ Consulta exacta del servicio
- ✅ Verificación de datos de perfiles
- ✅ Estado de configuraciones de menús

## 🚀 Orden de Diagnóstico

1. **Ejecutar script de base de datos**: `@database_test_perfiles_endpoint.sql`
2. **Probar conexión**: `http://localhost:8080/api/test-menu/db-test`
3. **Probar perfiles**: `http://localhost:8080/api/test-menu/perfiles`
4. **Probar endpoint original**: `http://localhost:8080/api/menu-config/perfiles`
5. **Revisar logs del backend**
6. **Aplicar solución según el problema encontrado**

## ⚠️ Importante

- ✅ **Los endpoints de prueba son seguros** - Solo consultan datos
- ✅ **No modifican nada** - Solo diagnostican el problema
- ✅ **Identifican la causa exacta** - Del problema de carga
- ✅ **Permiten solución dirigida** - Según el problema específico

## 🔧 Si Aún No Funciona

### **Verificar Configuración de Base de Datos**
```yaml
# En application.yml verificar:
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:XE
    username: tu_usuario
    password: tu_password
```

### **Verificar Logs Específicos**
```bash
grep -i "MenuConfigService\|obtenerPerfiles" logs/server.log
```

---

**Ejecuta primero `@database_test_perfiles_endpoint.sql` y luego prueba los endpoints de diagnóstico para identificar la causa exacta del problema.**
