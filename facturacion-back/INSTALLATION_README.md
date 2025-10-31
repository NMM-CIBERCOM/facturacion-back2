# 🚀 Instalación Completa del Sistema de Facturación

## 📋 Requisitos Previos

### Base de Datos
- **Oracle Database** (versión 11g o superior)
- **Usuario con permisos** para crear tablas, índices y insertar datos
- **Conexión activa** a la base de datos

### Backend (Java)
- **Java 17** o superior
- **Maven 3.6+**
- **Spring Boot 2.7+**

### Frontend (React)
- **Node.js 16+**
- **npm 8+** o **yarn 1.22+**

## 🗄️ Instalación de Base de Datos

### Paso 1: Ejecutar Script de Instalación
```sql
-- Conectar a Oracle como usuario con permisos
sqlplus tu_usuario/tu_password@localhost:1521/XE

-- Ejecutar el script completo
@INSTALLATION_COMPLETE_SYSTEM.sql
```

### Paso 2: Verificar Instalación
```sql
-- Verificar que se crearon las tablas
SELECT table_name FROM user_tables WHERE table_name IN ('PERFIL', 'USUARIOS', 'MENU_CONFIG');

-- Verificar usuarios creados
SELECT no_usuario, nombre_empleado, id_perfil FROM usuarios;

-- Verificar configuración de menús
SELECT COUNT(*) as total_configuraciones FROM menu_config;
```

## 🔧 Configuración del Backend

### Paso 1: Configurar Base de Datos
Editar `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:XE
    username: tu_usuario
    password: tu_password
    driver-class-name: oracle.jdbc.OracleDriver
```

### Paso 2: Instalar Dependencias
```bash
cd facturacion-backend/facturacion-back
mvn clean install
```

### Paso 3: Ejecutar Backend
```bash
mvn spring-boot:run
```

**Verificar**: El backend debe estar disponible en `http://localhost:8080`

## ⚛️ Configuración del Frontend

### Paso 1: Instalar Dependencias
```bash
cd facturacion-cibercom
npm install
```

### Paso 2: Ejecutar Frontend
```bash
npm run dev
```

**Verificar**: El frontend debe estar disponible en `http://localhost:5173`

## 👥 Usuarios del Sistema

| Usuario | Contraseña | Perfil | Descripción |
|---------|-----------|--------|-------------|
| `admin` | `admin123` | Administrador | Acceso completo, puede configurar menús |
| `jefe` | `jefe123` | Jefe de Crédito | Acceso a administración y reportes |
| `operador` | `operador123` | Operador de Crédito | Acceso básico a facturación y consultas |

## 🎯 Funcionalidades Principales

### ✅ Sistema de Menús Dinámicos
- **Configuración por perfil**: Cada perfil ve diferentes menús
- **Visibilidad configurable**: El administrador puede habilitar/deshabilitar pestañas y pantallas
- **30 pantallas de reportes**: 11 originales + 19 nuevas

### ✅ Perfiles de Usuario
- **Operador de Crédito**: Facturación, Consultas, Reportes, Registro CFDI, Monitor
- **Jefe de Crédito**: Todo lo anterior + Administración
- **Administrador**: Acceso completo + Configuración de menús

### ✅ Configuración Dinámica
- **Panel de administración**: Configuración → Configuración de Menús
- **Control granular**: Habilitar/deshabilitar pestañas y pantallas específicas
- **Persistencia**: Los cambios se guardan en la base de datos

## 🔍 Verificación del Sistema

### 1. Login y Navegación
- Iniciar sesión con `admin/admin123`
- Verificar que aparecen todas las pestañas
- Navegar a "Configuración" → "Configuración de Menús"

### 2. Configuración de Menús
- Seleccionar un perfil (ej: "Jefe de Credito")
- Verificar que aparecen las pestañas principales
- Hacer clic en el engranaje de "Reportes Facturación Fiscal"
- Verificar que aparecen las 30 pantallas con toggles

### 3. Filtrado Dinámico
- Deshabilitar algunas pantallas en la configuración
- Cambiar a otro perfil y verificar que las pantallas se ocultan
- Verificar que los cambios persisten al recargar la página

## 🛠️ Solución de Problemas

### Error de Conexión a Base de Datos
```bash
# Verificar que Oracle esté ejecutándose
sqlplus system/password@localhost:1521/XE

# Verificar la configuración en application.yml
```

### Error de Puerto en Uso
```bash
# Backend (puerto 8080)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Frontend (puerto 5173)
netstat -ano | findstr :5173
taskkill /PID <PID> /F
```

### Error de CORS
- Verificar que el backend tenga `@CrossOrigin(origins = "http://localhost:5173")`
- Verificar que el frontend esté en el puerto 5173

## 📊 Estructura de la Base de Datos

### Tabla PERFIL
- `ID_PERFIL`: Identificador único del perfil
- `NOMBRE_PERFIL`: Nombre del perfil (Operador de Credito, Jefe de Credito, Administrador)

### Tabla USUARIOS
- `NO_USUARIO`: Usuario de login
- `PASSWORD`: Contraseña (en texto plano para desarrollo)
- `ID_PERFIL`: Referencia al perfil del usuario

### Tabla MENU_CONFIG
- `ID_CONFIG`: Identificador único de la configuración
- `ID_PERFIL`: Perfil al que pertenece la configuración
- `MENU_LABEL`: Nombre de la pestaña o pantalla
- `MENU_PATH`: Ruta de la pantalla (NULL para pestañas principales)
- `IS_VISIBLE`: 1 = visible, 0 = oculto
- `ORDEN`: Orden de aparición en el menú

## 🎉 ¡Sistema Listo!

Una vez completados todos los pasos, tendrás un sistema de facturación completamente funcional con:

- ✅ **3 perfiles de usuario** con diferentes niveles de acceso
- ✅ **Sistema de menús dinámicos** configurable por administrador
- ✅ **30 pantallas de reportes fiscales** (11 originales + 19 nuevas)
- ✅ **Panel de administración** para configurar visibilidad
- ✅ **Persistencia de configuración** en base de datos

**¡Disfruta tu nuevo sistema de facturación!** 🚀
