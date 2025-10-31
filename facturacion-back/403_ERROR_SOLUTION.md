# 🔧 Solución - Error 403 en Endpoints de Prueba

## 🚨 Problema Identificado

Los endpoints de prueba están devolviendo **HTTP ERROR 403** - "Se denegó el acceso a localhost". Esto indica que Spring Security está bloqueando el acceso, aunque la configuración ya incluye los endpoints.

## 🔍 Diagnóstico

### **Paso 1: Verificar Base de Datos Primero**
```sql
@database_simple_perfiles_check.sql
```

Este script te mostrará:
- ✅ **Perfiles existentes** en la tabla PERFIL
- ✅ **Usuarios activos** y sus perfiles
- ✅ **Configuración de menús** por perfil
- ✅ **Perfiles sin configuración**

### **Paso 2: Probar Endpoint Original Directamente**
```sql
@database_test_original_endpoint.sql
```

Este script te mostrará:
- ✅ **Consulta exacta del servicio** - Lo que debería devolver
- ✅ **Verificación de datos NULL** - Si hay problemas de datos
- ✅ **Estructura de tabla PERFIL** - Si está correcta
- ✅ **Consulta con alias** - Como en el servicio

## 🛠️ Solución

### **Opción 1: Reiniciar Servidor Backend**
El problema más probable es que el servidor no se reinició después de crear los nuevos controladores:

1. **Detener el servidor backend**
2. **Reiniciar el servidor backend**
3. **Probar los endpoints nuevamente**

### **Opción 2: Probar Endpoint Original Directamente**
En lugar de usar los endpoints de prueba, prueba directamente:

```bash
# Probar endpoint original de perfiles
http://localhost:8080/api/menu-config/perfiles
```

### **Opción 3: Verificar Logs del Backend**
```bash
# Buscar errores específicos
grep -i "error\|exception\|perfil" logs/server.log
```

## 🔍 Verificación Post-Solución

### **Verificar que los Perfiles Existen**
```sql
SELECT 
    ID_PERFIL,
    NOMBRE_PERFIL
FROM PERFIL
ORDER BY NOMBRE_PERFIL;
```

**Debería mostrar 4 perfiles:**
- Super Administrador (ID: 99)
- Administrador (ID: 3)
- Jefe de Crédito (ID: 2)
- Operador de Crédito (ID: 1)

### **Verificar que el Endpoint Funciona**
```bash
# Probar en navegador:
http://localhost:8080/api/menu-config/perfiles
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

## ⚠️ Importante

- ✅ **La configuración de seguridad está correcta** - Incluye todos los endpoints
- ✅ **El problema es de reinicio** - El servidor necesita reiniciarse
- ✅ **Los scripts de BD son seguros** - Solo consultan datos
- ✅ **El endpoint original debería funcionar** - Después del reinicio

## 🔧 Si Aún No Funciona

### **Verificar que el Servidor Está Corriendo**
```bash
# Verificar puerto 8080
netstat -an | findstr :8080
```

### **Verificar Logs de Inicio**
```bash
# Buscar errores de inicio
grep -i "started\|error\|failed" logs/server.log
```

### **Probar Endpoint de Health Check**
```bash
# Probar health check
http://localhost:8080/api/menu-config/health
```

---

**Ejecuta primero `@database_simple_perfiles_check.sql` para verificar que los perfiles existen, luego reinicia el servidor backend y prueba `http://localhost:8080/api/menu-config/perfiles`.**
