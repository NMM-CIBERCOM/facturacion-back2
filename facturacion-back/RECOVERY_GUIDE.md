# 🔄 Guía de Recuperación Completa - Sin Pérdida de Datos

## 🚨 Situación Actual

No puedes acceder y no estás seguro si los roles anteriores siguen existiendo. He creado scripts seguros que **NO MODIFICAN** tus datos existentes, solo agregan lo necesario.

## 📋 Plan de Recuperación Segura

### **Paso 1: Verificar Estado Actual**
```sql
-- Ejecutar diagnóstico completo
@database_full_diagnostic.sql
```

**Este script te mostrará:**
- ✅ Todos los perfiles/roles existentes
- ✅ Todos los usuarios y sus perfiles actuales
- ✅ Qué columnas nuevas existen o faltan
- ✅ Estado de tablas, funciones e índices

### **Paso 2: Verificar Roles Anteriores**
```sql
-- Verificar específicamente los roles anteriores
@database_check_roles.sql
```

**Este script te mostrará:**
- ✅ Todos los perfiles en la tabla `PERFIL`
- ✅ Qué usuarios tienen qué perfiles asignados
- ✅ Si hay usuarios sin perfil o perfiles sin usuarios
- ✅ Estadísticas de uso de cada perfil

### **Paso 3: Recuperación Segura**
```sql
-- Recuperar funcionalidad sin perder datos
@database_safe_recovery.sql
```

**Este script:**
- ✅ **NO MODIFICA** datos existentes
- ✅ Solo agrega columnas faltantes con valores por defecto
- ✅ Crea tablas nuevas si no existen
- ✅ Preserva todos los perfiles y usuarios existentes

## 🔍 ¿Qué Verificar Primero?

### **1. Verificar Perfiles Existentes**
```sql
-- Ver todos los perfiles que tienes
SELECT ID_PERFIL, NOMBRE_PERFIL, STATUS_PERFIL 
FROM PERFIL 
ORDER BY ID_PERFIL;
```

### **2. Verificar Usuarios y Sus Perfiles**
```sql
-- Ver usuarios y sus perfiles actuales
SELECT 
    u.NO_USUARIO,
    u.NOMBRE_EMPLEADO,
    u.ID_PERFIL,
    p.NOMBRE_PERFIL,
    u.ESTATUS_USUARIO
FROM USUARIOS u
LEFT JOIN PERFIL p ON u.ID_PERFIL = p.ID_PERFIL
ORDER BY u.NO_USUARIO;
```

### **3. Verificar Estado de Acceso**
```sql
-- Ver qué usuarios están activos
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    ESTATUS_USUARIO,
    ID_PERFIL
FROM USUARIOS
WHERE ESTATUS_USUARIO = 'A'
ORDER BY NO_USUARIO;
```

## 🛠️ Soluciones por Problema

### **Problema 1: No Existen las Columnas Nuevas**
**Síntomas**: Error al ejecutar el código Java
**Solución**: El script `database_safe_recovery.sql` las crea automáticamente

### **Problema 2: Usuarios Sin Estado de Pago**
**Síntomas**: Usuarios no pueden acceder
**Solución**: El script crea estados de pago por defecto (`PAID`)

### **Problema 3: Función FN_USER_HAS_ACCESS No Existe**
**Síntomas**: Error en consultas de acceso
**Solución**: El script la crea automáticamente

### **Problema 4: Usuario SUPERADMIN No Existe**
**Síntomas**: No hay usuario de emergencia
**Solución**: El script lo crea con credenciales `SUPERADMIN/admin123`

## 🔐 Credenciales de Emergencia

Después de ejecutar la recuperación:

- **Usuario**: `SUPERADMIN`
- **Contraseña**: `admin123`
- **Rol**: `SUPER_ADMIN`
- **Acceso**: Siempre permitido

## 📊 Verificación Post-Recuperación

### **1. Verificar que Todo Funciona**
```sql
-- Probar función de acceso
SELECT FN_USER_HAS_ACCESS('SUPERADMIN') FROM DUAL;
-- Debe retornar 'Y'

-- Ver todos los usuarios con acceso
SELECT 
    u.NO_USUARIO,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO
FROM USUARIOS u
WHERE u.ESTATUS_USUARIO = 'A';
```

### **2. Verificar Perfiles Preservados**
```sql
-- Verificar que los perfiles siguen igual
SELECT COUNT(*) AS TOTAL_PERFILES FROM PERFIL;
SELECT COUNT(*) AS PERFILES_ACTIVOS FROM PERFIL WHERE STATUS_PERFIL = 'A';
```

### **3. Verificar Usuarios Preservados**
```sql
-- Verificar que los usuarios siguen igual
SELECT COUNT(*) AS TOTAL_USUARIOS FROM USUARIOS;
SELECT COUNT(*) AS USUARIOS_ACTIVOS FROM USUARIOS WHERE ESTATUS_USUARIO = 'A';
```

## 🚀 Orden de Ejecución Recomendado

1. **Primero**: `@database_full_diagnostic.sql`
2. **Segundo**: `@database_check_roles.sql`
3. **Tercero**: `@database_safe_recovery.sql`
4. **Cuarto**: Probar acceso con `SUPERADMIN/admin123`

## ⚠️ Importante

- ✅ **Los scripts son seguros** - No modifican datos existentes
- ✅ **Preservan todos los perfiles** - Solo agregan funcionalidad nueva
- ✅ **Mantienen usuarios existentes** - Solo agregan campos necesarios
- ✅ **Crean usuario de emergencia** - Para casos de problemas

## 🔧 Si Aún No Funciona

### **Verificar Logs del Servidor**
```bash
# Buscar errores específicos
grep -i "error\|exception\|sql" logs/server.log
```

### **Probar Conexión Directa**
```sql
-- Probar consulta básica
SELECT COUNT(*) FROM USUARIOS WHERE ESTATUS_USUARIO = 'A';
```

### **Verificar Configuración Java**
- ✅ `application.yml` tiene configuración JWT
- ✅ `UsuarioService.java` está actualizado
- ✅ Dependencias JWT están en `pom.xml`

---

**Ejecuta los scripts en orden y comparte los resultados. Los scripts te mostrarán exactamente qué está pasando sin modificar nada.**
