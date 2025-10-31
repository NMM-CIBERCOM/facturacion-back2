# 📱 Guía Visual - Configurar 2FA con Google Authenticator en Localhost

## 🎯 Objetivo

Configurar la autenticación de dos pasos (2FA) para cualquier usuario usando Google Authenticator en tu aplicación local.

## 🚀 Pasos para Configurar 2FA

### **Paso 1: Resetear 2FA Actual (si ya está configurado)**

Si el usuario ya tiene 2FA habilitado, primero debemos resetearlo:

```sql
-- Ejecutar en Oracle SQL Developer
@database_reset_2fa.sql
```

O ejecutar manualmente:
```sql
UPDATE USUARIOS 
SET TWO_FACTOR_ENABLED = 'N',
    TWO_FACTOR_SECRET = NULL
WHERE NO_USUARIO = 'admin';
COMMIT;
```

### **Paso 2: Configurar 2FA desde el Backend**

Abre una terminal o Postman y ejecuta:

```bash
curl -X POST "http://localhost:8080/api/auth/2fa/setup?username=admin"
```

**Respuesta esperada:**
```json
{
  "success": true,
  "setup": {
    "qrCodeUrl": "otpauth://totp/Cibercom%20Facturaci%C3%B3n:admin?secret=ABC123XYZ456...",
    "secretKey": "ABC123XYZ456...",
    "enabled": false,
    "message": "Escanea el código QR con Google Authenticator y luego activa la autenticación"
  }
}
```

**Importante**: Copia el valor de `secretKey` que te devuelve.

### **Paso 3: Configurar Base de Datos con el Secret**

Edita el archivo `database_setup_2fa_manual.sql` y reemplaza `'TU_SECRET_KEY_AQUI'` con el secretKey que obtuviste:

```sql
UPDATE USUARIOS 
SET TWO_FACTOR_SECRET = 'ABC123XYZ456...',  -- Pega aquí el secretKey
    TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin';
COMMIT;
```

Luego ejecuta el script en Oracle SQL Developer.

### **Paso 4: Instalar Google Authenticator**

Si aún no lo tienes, descarga **Google Authenticator** desde:
- 📱 **Android**: [Google Play Store](https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2)
- 📱 **iOS**: [App Store](https://apps.apple.com/us/app/google-authenticator/id388497605)

### **Paso 5: Escanear el Código QR**

#### **Opción A: Usando el QR Code URL**

1. Copia el `qrCodeUrl` del paso 2
2. Convierte la URL a imagen QR en: https://www.qr-code-generator.com/
3. Escanea el QR con Google Authenticator

#### **Opción B: Usando la Clave Secreta**

1. Abre **Google Authenticator** en tu teléfono
2. Toca **"Agregar cuenta"**
3. Selecciona **"Introducir una clave de configuración"**
4. Ingresa:
   - **Cuenta**: `Cibercom Facturación - admin`
   - **Clave**: El `secretKey` del paso 2
   - **Tipo**: Selecciona "Basado en tiempo"
5. Toca **"Agregar"**

### **Paso 6: Activar 2FA**

Una vez que veas el código de 6 dígitos en Google Authenticator, activa el 2FA:

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

### **Paso 7: Verificar en Base de Datos**

```sql
SELECT 
    NO_USUARIO,
    TWO_FACTOR_ENABLED,
    CASE 
        WHEN TWO_FACTOR_ENABLED = 'Y' THEN '✅ 2FA ACTIVO'
        ELSE '❌ 2FA NO ACTIVO'
    END AS ESTADO
FROM USUARIOS
WHERE NO_USUARIO = 'admin';
```

**Debería mostrar**: `✅ 2FA ACTIVO`

## 🧪 Prueba del Login con 2FA

### **Paso 1: Hacer Login Normal**

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

### **Paso 2: Obtener Código de Google Authenticator**

Abre Google Authenticator y verás un código de 6 dígitos que cambia cada 30 segundos.

### **Paso 3: Verificar Código 2FA**

```bash
curl -X POST "http://localhost:8080/api/auth/2fa/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "code": "123456",
    "sessionToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```
*(Reemplaza `123456` con el código actual de Google Authenticator y `token` con el token del paso 1)*

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Autenticación en dos pasos completada exitosamente",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." (nuevo token con acceso completo)
}
```

## 🔧 Solución de Problemas

### **Problema: "Código de verificación inválido"**

**Solución**: Asegúrate de usar el código más reciente de Google Authenticator (cambia cada 30 segundos)

### **Problema: "Autenticación en dos pasos no configurada"**

**Solución**: Verifica que el usuario tenga `TWO_FACTOR_ENABLED = 'Y'` en la base de datos

### **Problema: QR Code no se escanea**

**Solución**: Usa la clave secreta manualmente en Google Authenticator en lugar del QR

## 📋 Resumen Rápido

1. ✅ Ejecutar `@database_reset_2fa.sql` para resetear
2. ✅ Llamar `/api/auth/2fa/setup?username=admin`
3. ✅ Copiar el `secretKey` de la respuesta
4. ✅ Configurar en BD con `database_setup_2fa_manual.sql`
5. ✅ Agregar cuenta en Google Authenticator con la clave
6. ✅ Activar con `/api/auth/2fa/enable?username=admin&code=XXXXXX`
7. ✅ Probar login con `/api/auth/login`

## 🎯 Para Múltiples Usuarios

Repite los pasos cambiando `username=admin` por el usuario deseado:
- `username=jefe`
- `username=operador`
- `username=SUPERADMIN`

---

**Sigue los pasos en orden. El proceso completo toma aproximadamente 2-3 minutos.**
