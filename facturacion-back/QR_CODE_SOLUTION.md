# 📱 Solución Alternativa: Usar Código QR para 2FA

## 🔧 **Problema Identificado**
Google Authenticator no está aceptando las claves manuales. Vamos a usar el código QR directamente.

## ⚡ **Solución con Código QR**

### **Paso 1: Configurar clave perfecta en BD**
```sql
-- Ejecutar en Oracle SQL Developer
@database_qr_solution.sql
```

### **Paso 2: Obtener código QR del backend**
```bash
POST http://localhost:8080/api/auth/2fa/setup?username=admin
```

**Respuesta esperada:**
```json
{
  "success": true,
  "setup": {
    "qrCodeUrl": "otpauth://totp/Cibercom%20Facturación:admin?secret=ABCDEFGHIJKLMNOPQRSTUVWXYZ234567&issuer=Cibercom%20Facturación",
    "secretKey": "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567",
    "enabled": false,
    "message": "Escanea el código QR con Google Authenticator y luego activa la autenticación"
  }
}
```

### **Paso 3: Generar imagen QR**
1. **Copiar el `qrCodeUrl`** de la respuesta
2. **Ir a**: https://www.qr-code-generator.com/
3. **Pegar la URL** en el generador
4. **Generar código QR**
5. **Descargar la imagen**

### **Paso 4: Escanear con Google Authenticator**
1. **Abrir Google Authenticator**
2. **Tocar "Agregar cuenta"**
3. **Seleccionar "Escanear código QR"**
4. **Escanear la imagen QR generada**
5. **Verificar que aparezca la cuenta "Cibercom Facturación - admin"**

## 🔄 **Alternativa: Usar URL directa**

### **Opción A: Convertir URL a imagen**
```bash
# Usar la URL directamente para generar QR
https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=otpauth://totp/Cibercom%20Facturación:admin?secret=ABCDEFGHIJKLMNOPQRSTUVWXYZ234567&issuer=Cibercom%20Facturación
```

### **Opción B: Usar aplicación web**
1. **Ir a**: https://www.qr-code-generator.com/
2. **Pegar la URL completa del `qrCodeUrl`**
3. **Generar y descargar QR**
4. **Escanear con Google Authenticator**

## 🚀 **Prueba del 2FA**

### **1. Activar 2FA:**
```bash
POST http://localhost:8080/api/auth/2fa/enable?username=admin&code=123456
```
*(Usar el código de 6 dígitos de Google Authenticator)*

### **2. Probar login completo:**
1. **Abrir frontend** (http://localhost:3000)
2. **Login**: `admin` / `admin`
3. **Debería aparecer pantalla de 2FA**
4. **Ingresar código de 6 dígitos**
5. **¡Acceso completo!**

## 🔍 **Verificación**

Si todo funciona correctamente:
- ✅ **Google Authenticator**: Muestra código de 6 dígitos
- ✅ **Backend**: Acepta el código de activación
- ✅ **Frontend**: Muestra pantalla de 2FA

## 🚨 **Si sigue fallando**

### **Alternativa: Deshabilitar 2FA temporalmente**
```sql
-- Deshabilitar 2FA para testing
UPDATE USER_2FA_CONFIG 
SET ENABLED = 'N'
WHERE USERNAME = 'admin';
COMMIT;
```

---

**¡Usa el código QR en lugar de la clave manual!**
