# 🔐 Guía Completa - 2FA con Google Authenticator

## ✅ Problemas Solucionados

1. **Conversión Base64 → Base32**: Corregida la conversión para que Google Authenticator acepte la clave
2. **Frontend 2FA**: Agregado componente `TwoFactorAuthPage` y flujo completo en `App.tsx`

## 🚀 Pasos para Probar 2FA

### **Paso 1: Configurar Base de Datos**

```sql
-- Ejecutar en Oracle SQL Developer
@database_convert_base32_correct.sql
```

**Verificar que la clave tenga 32 caracteres:**
```sql
SELECT 
    NO_USUARIO,
    TWO_FACTOR_SECRET,
    LENGTH(TWO_FACTOR_SECRET) AS LONGITUD
FROM USUARIOS
WHERE NO_USUARIO = 'admin';
```

**Debería mostrar:**
- `TWO_FACTOR_SECRET`: `JN7XWRTG4NVWXJTS26DUCGY8QHU`
- `LONGITUD`: `32`

### **Paso 2: Configurar Google Authenticator**

1. **Abrir Google Authenticator**
2. **Tocar "Agregar cuenta"**
3. **Seleccionar "Introducir una clave de configuración"**
4. **Ingresar:**
   - **Cuenta**: `Cibercom - admin`
   - **Clave**: `JN7XWRTG4NVWXJTS26DUCGY8QHU`
   - **Tipo**: "Basado en tiempo"
5. **Tocar "Agregar"**

### **Paso 3: Activar 2FA en Backend**

```bash
# Obtener código actual de Google Authenticator (6 dígitos)
curl -X POST "http://localhost:8080/api/auth/2fa/enable?username=admin&code=123456"
```

### **Paso 4: Probar Login con 2FA**

1. **Abrir la aplicación frontend** (http://localhost:3000)
2. **Hacer login con:**
   - Usuario: `admin`
   - Contraseña: `admin`
3. **Debería aparecer la pantalla de 2FA**
4. **Ingresar el código de 6 dígitos** de Google Authenticator
5. **Tocar "Verificar Código"**

## 🔧 Archivos Modificados

### **Frontend:**
- ✅ `components/TwoFactorAuthPage.tsx` - Nuevo componente para 2FA
- ✅ `App.tsx` - Flujo completo de 2FA integrado

### **Backend:**
- ✅ `database_convert_base32_correct.sql` - Conversión correcta Base64 → Base32

## 🧪 Pruebas con cURL

### **1. Login normal (debería requerir 2FA):**
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","password":"admin"}'
```

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Autenticación exitosa",
  "requiresTwoFactor": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "usuario": {
    "noUsuario": "admin",
    "nombreEmpleado": "Administrador Sistema Y",
    "idPerfil": 3,
    "nombrePerfil": "Administrador"
  }
}
```

### **2. Verificar código 2FA:**
```bash
curl -X POST "http://localhost:8080/api/auth/2fa/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "code": "123456",
    "sessionToken": "TOKEN_DEL_LOGIN"
  }'
```

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Autenticación en dos pasos completada exitosamente",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "usuario": {
    "noUsuario": "admin",
    "nombreEmpleado": "Administrador Sistema Y",
    "idPerfil": 3,
    "nombrePerfil": "Administrador"
  }
}
```

## 🎯 Flujo Completo en Frontend

1. **Usuario ingresa credenciales** → Login normal
2. **Backend responde con `requiresTwoFactor: true`** → Muestra pantalla 2FA
3. **Usuario ingresa código de Google Authenticator** → Verificación
4. **Backend valida código** → Login completo
5. **Usuario accede a la aplicación** → Dashboard

## 🔍 Verificación de Estado

### **En Base de Datos:**
```sql
SELECT 
    NO_USUARIO,
    TWO_FACTOR_ENABLED,
    TWO_FACTOR_SECRET,
    CASE 
        WHEN TWO_FACTOR_ENABLED = 'Y' THEN '✅ 2FA ACTIVO'
        WHEN TWO_FACTOR_ENABLED = 'N' AND TWO_FACTOR_SECRET IS NOT NULL THEN '⚙️ PENDIENTE DE ACTIVAR'
        ELSE '❌ 2FA NO CONFIGURADO'
    END AS ESTADO_2FA
FROM USUARIOS
WHERE NO_USUARIO = 'admin';
```

### **En Frontend:**
- Abrir DevTools (F12)
- Verificar que aparezca la pantalla de 2FA después del login
- Verificar que el código se envíe correctamente al backend

## 🚨 Solución de Problemas

### **Error: "Clave demasiado corta"**
- ✅ **Solucionado**: Usar `database_convert_base32_correct.sql`

### **Error: "Código de verificación inválido"**
- Verificar que el código de Google Authenticator sea el más reciente (cambia cada 30 segundos)
- Verificar que la clave en la BD sea correcta (32 caracteres Base32)

### **No aparece pantalla de 2FA**
- Verificar que `TWO_FACTOR_ENABLED = 'Y'` en la base de datos
- Verificar que el backend esté devolviendo `requiresTwoFactor: true`

---

**¡El 2FA está completamente implementado y listo para usar!** 🎉
