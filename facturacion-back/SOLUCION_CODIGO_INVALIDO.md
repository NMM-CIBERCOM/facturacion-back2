# 🔧 Solución: "Código de verificación inválido"

## 🚨 Problema Identificado

El código `681718` está siendo rechazado como inválido. Esto puede ser por:

1. **Clave Base32 incorrecta en la base de datos**
2. **Desincronización de tiempo entre Google Authenticator y el servidor**
3. **Problema con la implementación del backend**

## ⚡ Solución Paso a Paso

### **Paso 1: Diagnosticar la base de datos**
```sql
-- Ejecutar en Oracle SQL Developer
@database_diagnose_2fa.sql
```

**Verificar que muestre:**
- ✅ **LONGITUD**: `32`
- ✅ **VALIDACION_LONGITUD**: `✅ LONGITUD CORRECTA (32)`
- ✅ **VALIDACION_FORMATO**: `✅ FORMATO BASE32 VÁLIDO`

### **Paso 2: Verificar Google Authenticator**

1. **Abrir Google Authenticator**
2. **Verificar que la cuenta `Cibercom - admin` esté configurada**
3. **Verificar que el código de 6 dígitos esté cambiando cada 30 segundos**
4. **Si no está configurada, agregarla con la clave de 32 caracteres**

### **Paso 3: Probar con código más reciente**

El código TOTP cambia cada 30 segundos. Asegúrate de usar el código más reciente:

```bash
# Esperar a que aparezca un nuevo código en Google Authenticator
curl -X POST "http://localhost:8080/api/auth/2fa/enable?username=admin&code=NUEVO_CODIGO"
```

### **Paso 4: Verificar sincronización de tiempo**

Si el problema persiste, puede ser desincronización de tiempo:

```bash
# Verificar que el servidor esté sincronizado
curl -X GET "http://localhost:8080/api/auth/2fa/health"
```

### **Paso 5: Probar con código manual**

Si Google Authenticator no funciona, prueba con un código manual:

```bash
# Usar código de prueba (solo para testing)
curl -X POST "http://localhost:8080/api/auth/2fa/enable?username=admin&code=123456"
```

## 🔍 Verificaciones Adicionales

### **1. Verificar que el backend esté funcionando:**
```bash
# Verificar endpoint de setup
curl -X POST "http://localhost:8080/api/auth/2fa/setup?username=admin"
```

### **2. Verificar logs del backend:**
- Revisar la consola del backend para errores
- Verificar que no haya excepciones en la verificación TOTP

### **3. Verificar configuración de tiempo:**
- Asegurarse de que el servidor tenga la hora correcta
- Verificar que no haya diferencia de más de 30 segundos

## 🚨 Solución Alternativa

Si el problema persiste, podemos deshabilitar temporalmente el 2FA:

```sql
-- Deshabilitar 2FA temporalmente
UPDATE USUARIOS 
SET TWO_FACTOR_ENABLED = 'N'
WHERE NO_USUARIO = 'admin';
COMMIT;
```

## 📱 Pasos para Reconfigurar Google Authenticator

1. **Eliminar la cuenta existente** en Google Authenticator
2. **Ejecutar el script de diagnóstico** para obtener la clave correcta
3. **Agregar nueva cuenta** con la clave de 32 caracteres
4. **Probar con el nuevo código**

---

**¡Ejecuta primero el script de diagnóstico para verificar el estado de la clave!**
