# 🔧 Solución - Problema de Perfiles en Configuración de Menús

## 🚨 Problema Identificado

Después de crear los usuarios, el apartado de configuración no carga los perfiles para la visibilidad de pestañas. Esto puede deberse a:

1. **Tabla MENU_CONFIG vacía** - No hay configuración de menús para los perfiles
2. **Perfiles sin configuración** - Los perfiles nuevos no tienen configuración asociada
3. **Problema en la consulta** - El servicio no encuentra los perfiles

## 🔍 Diagnóstico

### **Paso 1: Verificar Estado Actual**
```sql
@database_diagnose_menu_problem.sql
```

Este script te mostrará:
- ✅ **Perfiles existentes** en la tabla PERFIL
- ✅ **Si existe tabla MENU_CONFIG**
- ✅ **Contenido de MENU_CONFIG** (si existe)
- ✅ **Usuarios y sus perfiles asignados**
- ✅ **Prueba de consulta del servicio**

### **Paso 2: Verificar Configuración Completa**
```sql
@database_check_menu_config.sql
```

Este script te mostrará:
- ✅ **Configuración de menús existente**
- ✅ **Perfiles sin configuración**
- ✅ **Configuraciones sin perfil válido**
- ✅ **Conteo de configuraciones por perfil**

## 🛠️ Solución

### **Opción 1: Si la tabla MENU_CONFIG está vacía**
```sql
@database_fix_menu_config.sql
```

Este script:
- ✅ **Crea configuración básica** para todos los perfiles
- ✅ **Crea secuencia SEQ_MENU_CONFIG** si no existe
- ✅ **Asocia perfiles con configuración de menús**
- ✅ **Verifica el resultado final**

### **Opción 2: Si solo faltan algunos perfiles**
Ejecuta solo las partes necesarias del script de corrección.

## 🔍 Posibles Causas

### **Causa 1: Tabla MENU_CONFIG Vacía**
- La tabla existe pero no tiene datos
- Los perfiles no tienen configuración asociada

### **Causa 2: Perfiles Nuevos Sin Configuración**
- Los perfiles creados (ID: 99, 3, 2, 1) no tienen configuración
- Solo los perfiles originales tenían configuración

### **Causa 3: Problema en el Servicio**
- El servicio `MenuConfigService.obtenerPerfiles()` no encuentra perfiles
- Problema en la consulta SQL

## 🚀 Pasos para Solucionar

### **Paso 1: Diagnosticar**
```sql
@database_diagnose_menu_problem.sql
```

### **Paso 2: Verificar Configuración**
```sql
@database_check_menu_config.sql
```

### **Paso 3: Corregir si es Necesario**
```sql
@database_fix_menu_config.sql
```

### **Paso 4: Reiniciar Backend**
Después de ejecutar los scripts, reinicia el servidor backend.

### **Paso 5: Probar Configuración**
Accede a la sección de configuración y verifica que los perfiles aparezcan.

## 🔍 Verificación Post-Fix

### **Verificar que los Perfiles Aparecen**
```sql
SELECT 
    ID_PERFIL, 
    NOMBRE_PERFIL 
FROM PERFIL 
ORDER BY NOMBRE_PERFIL;
```

**Debería mostrar todos los perfiles**: Super Administrador, Administrador, Jefe de Crédito, Operador de Crédito

### **Verificar Configuración de Menús**
```sql
SELECT 
    mc.ID_PERFIL,
    p.NOMBRE_PERFIL,
    COUNT(*) AS TOTAL_CONFIGURACIONES
FROM MENU_CONFIG mc
LEFT JOIN PERFIL p ON mc.ID_PERFIL = p.ID_PERFIL
GROUP BY mc.ID_PERFIL, p.NOMBRE_PERFIL
ORDER BY mc.ID_PERFIL;
```

**Todos los perfiles deberían tener al menos 1 configuración**

## ⚠️ Importante

- ✅ **Las configuraciones existentes se preservan** - No se modifican
- ✅ **Solo se agregan configuraciones faltantes** - Para perfiles nuevos
- ✅ **El sistema de menús seguirá funcionando** - Con las configuraciones existentes
- ✅ **Los perfiles aparecerán en configuración** - Después de la corrección

## 🔧 Si Aún No Funciona

### **Verificar Logs del Backend**
```bash
grep -i "menu\|perfil\|config" logs/server.log
```

### **Probar Endpoint Directamente**
```bash
curl http://localhost:8080/api/menu-config/perfiles
```

### **Verificar Frontend**
Revisar la consola del navegador para errores en la carga de perfiles.

---

**Ejecuta primero `@database_diagnose_menu_problem.sql` para identificar el problema específico, luego `@database_fix_menu_config.sql` para corregirlo.**
