# 🔐 Guía Completa - Autenticación de Dos Pasos (2FA) con Google Authenticator

## 📋 Resumen de Implementación

La autenticación de dos pasos (2FA) está implementada usando el algoritmo **TOTP (Time-based One-Time Password)** que es compatible con **Google Authenticator** y otras aplicaciones similares.

## 🔧 Arquitectura del Sistema

### **Componentes Backend**
- ✅ **TwoFactorAuthUtil**: Genera claves secretas y verifica códigos TOTP
- ✅ **TwoFactorAuthController**: Endpoints REST para configuración y verificación
- ✅ **UsuarioService**: Métodos para configurar, activar y verificar 2FA
- ✅ **Campo TWO_FACTOR_ENABLED**: Almacena si el usuario tiene 2FA activo
- ✅ **Campo TWO_FACTOR_SECRET**: Almacena la clave secreta del usuario

### **Endpoints Disponibles**
- ✅ **POST /api/auth/2fa/setup**: Configura 2FA (genera clave y QR)
- ✅ **POST /api/auth/2fa/enable**: Activa 2FA después de verificar código
- ✅ **POST /api/auth/2fa/verify**: Verifica código durante login
- ✅ **POST /api/auth/2fa/disable**: Desactiva 2FA
- ✅ **GET /api/auth/2fa/health**: Health check

## 🚀 Flujo de Autenticación con 2FA

### **Paso 1: Usuario se registra por primera vez**
1. Usuario ingresa credenciales (usuario/contraseña)
2. Backend valida credenciales
3. Backend genera JWT token
4. Backend verifica si tiene 2FA habilitado
5. Si tiene 2FA, devuelve `requiresTwoFactor: true` con el token temporal
6. Usuario debe ingresar código de Google Authenticator

### **Paso 2: Usuario configura 2FA (primera vez)**
1. Usuario llama a `/api/auth/2fa/setup?username=admin`
2. Backend genera clave secreta
3. Backend genera URL QR en formato `otpauth://totp/...`
4. Usuario escanea QR con Google Authenticator
5. Backend guarda clave secreta en BD (TWO_FACTOR_ENABLED = 'N')

### **Paso 3: Usuario activa 2FA**
1. Usuario obtiene código de Google Authenticator
2. Usuario llama a `/api/auth/2fa/enable?username=admin&code=123456`
3. Backend verifica código
4. Si es correcto, activa 2FA (TWO_FACTOR_ENABLED = 'Y')

### **Paso 4: Login con 2FA activo**
1. Usuario ingresa credenciales
2. Backend valida y genera token temporal
3. Frontend pide código 2FA
4. Usuario ingresa código de Google Authenticator
5. Backend verifica código con `/api/auth/2fa/verify`
6. Si es correcto, genera token final con acceso completo

## 🧪 Prueba Paso a Paso

### **Paso 1: Configurar 2FA para un Usuario**
```bash
curl -X POST "http://localhost:8080/api/auth/2fa/setup?username=admin"
```

**Respuesta esperada:**
```json
{
  "success": true,
  "setup": {
    "qrCodeUrl": "otpauth://totp/Cibercom%20Facturaci%C3%B3n:admin?secret=ABC123...",
    "secretKey": "ABC123...",
    "enabled": false,
    "message": "Escanea el código QR con Google Authenticator y luego activa la autenticación"
  }
}
```

### **Paso 2: Escanear QR con Google Authenticator**
1. Abre **Google Authenticator** en tu teléfono
2. Toca **"Agregar cuenta"**
3. Selecciona **"Escanear código QR"**
4. Escanea el código QR devuelto por el endpoint
5. Aparecerá un código de 6 dígitos que cambia cada 30 segundos

### **Paso 3: Activar 2FA**
```bash
curl -X POST "http://localhost:8080/api/auth/2fa/enable?username=admin&code=123456"
```
*(Reemplaza 123456 con el código actual de Google Authenticator)*

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Autenticación en dos pasos activada exitosamente"
}
```

### **Paso 4: Probar Login con 2FA**
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
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### **Paso 5: Verificar Código 2FA**
```bash
curl -X POST "http://localhost:8080/api/auth/2fa/verify" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","code":"123456","sessionToken":"eyJhbGciOi..."}'
```
*(Usa el token del paso 4 y el código actual de Google Authenticator)*

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Autenticación en dos pasos completada exitosamente",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." (nuevo token con acceso completo)
}
```

## 🔍 Verificación en Base de Datos

```sql
-- Verificar estado de 2FA para usuarios
SELECT 
    NO_USUARIO,
    NOMBRE_EMPLEADO,
    TWO_FACTOR_ENABLED,
    TWO_FACTOR_SECRET,
    CASE 
        WHEN TWO_FACTOR_ENABLED = 'Y' THEN '✅ 2FA ACTIVO'
        WHEN TWO_FACTOR_ENABLED = 'N' AND TWO_FACTOR_SECRET IS NOT NULL THEN '⚙️ PENDIENTE DE ACTIVAR'
        ELSE '❌ 2FA DESACTIVADO'
    END AS ESTADO_2FA
FROM USUARIOS
WHERE ESTATUS_USUARIO = 'A'
ORDER BY NO_USUARIO;
```

## 📱 Aplicaciones Compatibles

- ✅ **Google Authenticator** (Android/iOS)
- ✅ **Microsoft Authenticator** (Android/iOS)
- ✅ **Authy** (Android/iOS/Desktop)
- ✅ **1Password** (Con soporte TOTP)
- ✅ **LastPass Authenticator** (Android/iOS)

## 🔧 Integración en Frontend

### **Ejemplo de Flujo en React/TypeScript**
```typescript
// 1. Login normal
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  body: JSON.stringify({ usuario: 'admin', password: 'admin' })
});

const loginData = await loginResponse.json();

// 2. Si requiere 2FA
if (loginData.requiresTwoFactor) {
  const code = prompt('Ingresa el código de Google Authenticator:');
  
  // 3. Verificar código 2FA
  const verifyResponse = await fetch('/api/auth/2fa/verify', {
    method: 'POST',
    body: JSON.stringify({
      username: 'admin',
      code: code,
      sessionToken: loginData.token
    })
  });
  
  const verifyData = await verifyResponse.json();
  
  // 4. Usar token final
  localStorage.setItem('token', verifyData.token);
}
```

## ⚠️ Seguridad

- ✅ **Códigos TOTP válidos por 30 segundos**
- ✅ **Acepta códigos de los últimos 2 períodos** (1 minuto de margen)
- ✅ **Claves secretas almacenadas en Base64**
- ✅ **Tokens JWT con expiración de 15 minutos**
- ✅ **Soporte para múltiples dispositivos** (misma clave)

## 🧪 Prueba Completa End-to-End

### **Script de Prueba Completo**
```bash
# 1. Configurar 2FA
RESPONSE=$(curl -X POST "http://localhost:8080/api/auth/2fa/setup?username=admin")
echo "Setup Response: $RESPONSE"

# 2. (Manual) Escanear QR con Google Authenticator

# 3. (Manual) Obtener código de Google Authenticator

# 4. Activar 2FA (reemplazar CODE con código actual)
curl -X POST "http://localhost:8080/api/auth/2fa/enable?username=admin&code=CODE"

# 5. Login
LOGIN_RESPONSE=$(curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","password":"admin"}')
echo "Login Response: $LOGIN_RESPONSE"

# 6. (Manual) Obtener código de Google Authenticator

# 7. Verificar 2FA (reemplazar CODE y TOKEN)
curl -X POST "http://localhost:8080/api/auth/2fa/verify" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","code":"CODE","sessionToken":"TOKEN"}'
```

---

**El sistema de 2FA está completamente funcional. Sigue los pasos de prueba para configurarlo y probarlo en tiempo real.**
