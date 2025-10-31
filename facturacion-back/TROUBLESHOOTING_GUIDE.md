# 🔧 Solución de Problemas - Acceso Denegado

## 🚨 Problema Identificado

No puedes acceder ni con las nuevas credenciales ni con las existentes. Esto puede deberse a varios factores:

## 📋 Pasos de Diagnóstico

### **Paso 1: Ejecutar Diagnóstico de Base de Datos**
```sql
-- Ejecutar este script para verificar el estado
@database_diagnostic.sql
```

### **Paso 2: Corregir Usuarios Existentes**
```sql
-- Ejecutar este script para corregir problemas
@database_fix_users.sql
```

### **Paso 3: Probar Autenticación**
```sql
-- Ejecutar este script para verificar acceso
@database_test_auth.sql
```

## 🔍 Posibles Causas del Problema

### **1. Columnas Nuevas No Existen**
- Las columnas `USER_ROLE`, `PASSWORD_ENCODED`, etc. pueden no haberse creado
- **Solución**: Ejecutar `database_security_setup_simple.sql` nuevamente

### **2. Usuarios Sin Estado de Pago**
- Los usuarios existentes no tienen registro en `USER_PAYMENT_STATUS`
- **Solución**: El script `database_fix_users.sql` los crea automáticamente

### **3. Contraseñas Codificadas Incorrectamente**
- Las contraseñas pueden estar en formato incorrecto
- **Solución**: Verificar el tipo de codificación

### **4. Estado de Usuario Incorrecto**
- El campo `ESTATUS_USUARIO` puede no ser 'A' (Activo)
- **Solución**: Actualizar a 'A'

## 🛠️ Soluciones Rápidas

### **Solución 1: Crear Usuario de Prueba Simple**
```sql
-- Crear usuario de prueba con contraseña simple
INSERT INTO USUARIOS (
    NO_USUARIO, NOMBRE_EMPLEADO, PASSWORD, ESTATUS_USUARIO, ID_PERFIL, 
    FECHA_ALTA, FECHA_MOD, USUARIO_MOD, ID_DFI, ID_ESTACIONAMIENTO, 
    MODIFICA_UBICACION, USER_ROLE, PASSWORD_ENCODED
) VALUES (
    'TEST', 'Usuario de Prueba', 'test123', 'A', 1,
    SYSDATE, SYSDATE, 'SYSTEM', 1, 1,
    'N', 'USER', 'N'
);

-- Crear estado de pago
INSERT INTO USER_PAYMENT_STATUS (
    NO_USUARIO, PAYMENT_STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY
) VALUES (
    'TEST', 'PAID', SYSDATE, SYSDATE, 'SYSTEM'
);

COMMIT;
```

### **Solución 2: Verificar Credenciales del Super Admin**
```sql
-- Verificar contraseña del Super Admin
SELECT 
    NO_USUARIO,
    PASSWORD,
    PASSWORD_ENCODED,
    ESTATUS_USUARIO,
    USER_ROLE
FROM USUARIOS 
WHERE NO_USUARIO = 'SUPERADMIN';
```

### **Solución 3: Resetear Contraseña del Super Admin**
```sql
-- Resetear contraseña del Super Admin a texto plano
UPDATE USUARIOS 
SET PASSWORD = 'admin123',
    PASSWORD_ENCODED = 'N',
    USER_ROLE = 'SUPER_ADMIN',
    ESTATUS_USUARIO = 'A'
WHERE NO_USUARIO = 'SUPERADMIN';

COMMIT;
```

## 🧪 Pruebas de Acceso

### **Prueba 1: Verificar Usuario Existe**
```sql
SELECT COUNT(*) FROM USUARIOS WHERE NO_USUARIO = 'SUPERADMIN' AND ESTATUS_USUARIO = 'A';
-- Debe retornar 1
```

### **Prueba 2: Verificar Contraseña**
```sql
SELECT PASSWORD FROM USUARIOS WHERE NO_USUARIO = 'SUPERADMIN';
-- Debe mostrar la contraseña (admin123 si es texto plano)
```

### **Prueba 3: Verificar Estado de Pago**
```sql
SELECT PAYMENT_STATUS FROM USER_PAYMENT_STATUS WHERE NO_USUARIO = 'SUPERADMIN';
-- Debe retornar 'PAID'
```

### **Prueba 4: Verificar Función de Acceso**
```sql
SELECT FN_USER_HAS_ACCESS('SUPERADMIN') FROM DUAL;
-- Debe retornar 'Y'
```

## 🔧 Código Java Corregido

He actualizado el `UsuarioService.java` para ser más robusto:

- ✅ **Consulta simplificada**: Solo campos que siempre existen
- ✅ **RowMapper robusto**: Maneja columnas opcionales
- ✅ **Manejo de errores**: Mejor logging de problemas

## 📞 Próximos Pasos

1. **Ejecutar scripts de diagnóstico** en orden
2. **Verificar logs del servidor** para errores específicos
3. **Probar con usuario simple** (TEST/test123)
4. **Verificar configuración JWT** en application.yml

## 🆘 Si Aún No Funciona

### **Verificar Logs del Servidor**
```bash
# Buscar errores en los logs
grep -i "error\|exception" logs/server.log
```

### **Probar Endpoint Directamente**
```bash
# Probar login con curl
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"SUPERADMIN","password":"admin123"}'
```

### **Verificar Conexión a Base de Datos**
```bash
# Verificar que la aplicación se conecta a Oracle
grep -i "oracle\|database" logs/server.log
```

---

**Nota**: Los scripts de diagnóstico te mostrarán exactamente qué está causando el problema. Ejecútalos en orden y comparte los resultados.
