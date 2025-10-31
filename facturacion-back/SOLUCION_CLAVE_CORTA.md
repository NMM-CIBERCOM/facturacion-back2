# 🔧 Solución: "El valor de la clave es demasiado corto"

## 🚨 Problema Identificado

Google Authenticator espera claves en formato **Base32**, pero el backend está generando claves en **Base64**.

- **Clave actual (Base64)**: `JLfxWrtH4nVwxjtS26DUcGy8QhU=`
- **Clave necesaria (Base32)**: `JN7XWRTG4NVWXJTS26DUCGY8QHU`

## ⚡ Solución Rápida (2 minutos)

### Paso 1: Ejecutar conversión en base de datos
```sql
-- Ejecutar en Oracle SQL Developer
@database_convert_to_base32.sql
```

### Paso 2: Agregar en Google Authenticator

1. **Abrir Google Authenticator**
2. **Tocar "Agregar cuenta"**
3. **Seleccionar "Introducir una clave de configuración"**
4. **Ingresar:**
   - **Cuenta**: `Cibercom - admin`
   - **Clave**: `JN7XWRTG4NVWXJTS26DUCGY8QHU`
   - **Tipo**: "Basado en tiempo"
5. **Tocar "Agregar"**

### Paso 3: Activar 2FA
```bash
curl -X POST "http://localhost:8080/api/auth/2fa/enable?username=admin&code=123456"
```
*(Reemplaza `123456` con el código de 6 dígitos de Google Authenticator)*

## 🔍 Verificación

Después de ejecutar el script, verifica que la clave esté en Base32:

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
- `LONGITUD`: `26` (típico para Base32)

## 🎯 Prueba Final

1. **Login normal:**
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","password":"admin"}'
```

2. **Verificar con código 2FA:**
```bash
curl -X POST "http://localhost:8080/api/auth/2fa/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "code": "123456",
    "sessionToken": "TOKEN_DEL_LOGIN"
  }'
```

## 📝 Nota Técnica

El problema ocurre porque:
- **Base64** usa caracteres: `A-Z`, `a-z`, `0-9`, `+`, `/`, `=`
- **Base32** usa caracteres: `A-Z`, `2-7` (sin `=`, `+`, `/`)

Google Authenticator solo acepta Base32, por eso da el error "clave demasiado corta" cuando intentas usar Base64.

---

**¡Ejecuta el script y prueba con la nueva clave Base32!**
