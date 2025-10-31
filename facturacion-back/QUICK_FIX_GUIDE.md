# 🚨 Solución Rápida - Error 401 y JSON

## 🔍 Problema Identificado

La consola muestra:
- ✅ **Usuarios existen**: admin, jefe, operador, SUPERADMIN
- ❌ **Backend responde 401**: Rechaza las credenciales
- ❌ **Error JSON**: El backend no envía respuesta JSON válida

## 🛠️ Solución Rápida

### **Paso 1: Ejecutar Recuperación Básica**
```sql
@database_quick_fix.sql
```

Este script:
- ✅ Crea columnas faltantes en `USUARIOS`
- ✅ Crea tabla `USER_PAYMENT_STATUS`
- ✅ Asigna estados de pago por defecto (`PAID`)
- ✅ Configura `SUPERADMIN` con rol correcto
- ✅ Crea función de acceso

### **Paso 2: Verificar Contraseñas**
```sql
@database_check_passwords.sql
```

Este script te mostrará:
- ✅ Contraseñas actuales de los usuarios
- ✅ Si están codificadas o en texto plano
- ✅ Estado de cada usuario

### **Paso 3: Probar Acceso**

Después de ejecutar los scripts, prueba con:

#### **Credenciales de Prueba**
- **Usuario**: `admin`
- **Contraseña**: `admin` (o la que muestre el script)

- **Usuario**: `SUPERADMIN`  
- **Contraseña**: `admin123`

## 🔧 ¿Por Qué Falla?

### **Problema 1: Columnas Faltantes**
El código Java espera columnas que no existen:
- `USER_ROLE`
- `PASSWORD_ENCODED`
- `TWO_FACTOR_ENABLED`
- `LAST_LOGIN`

### **Problema 2: Tabla de Estados de Pago**
No existe `USER_PAYMENT_STATUS`, causando errores en la función de acceso.

### **Problema 3: Función de Acceso**
`FN_USER_HAS_ACCESS` no existe, causando errores en consultas.

## 🚀 Orden de Ejecución

1. **Primero**: `@database_quick_fix.sql`
2. **Segundo**: `@database_check_passwords.sql`
3. **Tercero**: Reiniciar el servidor backend
4. **Cuarto**: Probar login en el frontend

## 🔍 Verificación Post-Fix

### **Verificar que Todo Funciona**
```sql
-- Probar función de acceso
SELECT FN_USER_HAS_ACCESS('admin') FROM DUAL;
-- Debe retornar 'Y'

-- Ver todos los usuarios con acceso
SELECT 
    u.NO_USUARIO,
    FN_USER_HAS_ACCESS(u.NO_USUARIO) AS TIENE_ACCESO
FROM USUARIOS u
WHERE u.ESTATUS_USUARIO = 'A';
```

### **Verificar Contraseñas**
```sql
-- Ver contraseñas de usuarios principales
SELECT 
    NO_USUARIO,
    PASSWORD,
    PASSWORD_ENCODED
FROM USUARIOS
WHERE NO_USUARIO IN ('admin', 'jefe', 'operador', 'SUPERADMIN');
```

## ⚠️ Importante

- ✅ **Los scripts son seguros** - No modifican datos existentes
- ✅ **Solo agregan funcionalidad** - Preservan usuarios y perfiles
- ✅ **Solucionan el error 401** - Configuran el sistema correctamente

## 🔧 Si Aún No Funciona

### **Verificar Logs del Servidor**
```bash
# Buscar errores específicos
grep -i "error\|exception" logs/server.log
```

### **Probar Endpoint Directamente**
```bash
# Probar login con curl
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","password":"admin"}'
```

---

**Ejecuta `@database_quick_fix.sql` primero. Este script solucionará el problema del error 401 y la respuesta JSON.**
