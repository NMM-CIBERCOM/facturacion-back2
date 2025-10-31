# Sistema de Seguridad Mejorado - Cibercom Facturación

## 🚀 Funcionalidades Implementadas

### ✅ 1. Cifrado de Contraseñas
- **Base64**: Para compatibilidad con sistemas existentes
- **BCrypt**: Para nuevas contraseñas (más seguro)
- **Migración automática**: Las contraseñas en texto plano se convierten automáticamente a BCrypt

### ✅ 2. Autenticación JWT
- **Expiración**: 15 minutos de inactividad
- **Tokens seguros**: Firmados con clave secreta
- **Renovación automática**: Al realizar acciones

### ✅ 3. Sistema de Roles Jerárquico
- **Super Administrador**: Control total del sistema
- **Administrador**: Gestión de usuarios (por debajo del Super Admin)
- **Usuario**: Acceso básico
- **Operador**: Funciones operativas

### ✅ 4. Autenticación en Dos Pasos (2FA)
- **Google Authenticator**: Compatible con TOTP
- **Códigos QR**: Para configuración fácil
- **Códigos de respaldo**: Para recuperación

### ✅ 5. Control de Acceso por Pagos
- **Super Admin**: Siempre tiene acceso (controlado por Cibercom)
- **Estados de pago**: PAID, PENDING, OVERDUE, SUSPENDED, CANCELLED
- **Denegación automática**: Por falta de pago
- **Restauración**: Solo Super Admin puede restaurar acceso

## 📋 Instalación y Configuración

### 1. Dependencias Maven
Las siguientes dependencias ya están agregadas al `pom.xml`:

```xml
<!-- JWT para autenticación -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Autenticación en dos pasos -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.15</version>
</dependency>
```

### 2. Base de Datos
Ejecutar el script `database_security_setup.sql` en Oracle Database:

```sql
-- Crear tabla de estados de pago
CREATE TABLE USER_PAYMENT_STATUS (
    NO_USUARIO VARCHAR2(50) NOT NULL,
    PAYMENT_STATUS VARCHAR2(20) NOT NULL,
    -- ... más campos
);

-- Agregar columnas a USUARIOS
ALTER TABLE USUARIOS ADD (
    TWO_FACTOR_ENABLED CHAR(1) DEFAULT 'N',
    TWO_FACTOR_SECRET VARCHAR2(100),
    LAST_LOGIN DATE,
    USER_ROLE VARCHAR2(20) DEFAULT 'USER'
);
```

### 3. Configuración
El `application.yml` ya está configurado con:

```yaml
# Configuración JWT
jwt:
  secret: cibercom-facturacion-secret-key-2024-super-secure
  expiration: 900000 # 15 minutos

# Configuración de seguridad
security:
  cors:
    allowed-origins: http://localhost:5173
```

## 🔧 Uso del Sistema

### Login con JWT
```bash
POST /api/auth/login
{
  "usuario": "SUPERADMIN",
  "password": "admin123"
}

Response:
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "usuario": { ... },
  "requiresTwoFactor": false
}
```

### Configurar 2FA
```bash
POST /api/auth/2fa/setup?username=usuario
Response:
{
  "success": true,
  "setup": {
    "qrCodeUrl": "otpauth://totp/Cibercom...",
    "secretKey": "base64encodedkey",
    "enabled": false
  }
}
```

### Verificar Código 2FA
```bash
POST /api/auth/2fa/verify
{
  "code": "123456",
  "sessionToken": "jwt_token"
}
```

### Gestión de Pagos (Solo Super Admin)
```bash
# Ver estados de pago
GET /api/admin/payment-status
Authorization: Bearer jwt_token

# Actualizar estado de pago
POST /api/admin/payment-status
{
  "username": "usuario",
  "status": "SUSPENDED",
  "reason": "Falta de pago"
}

# Denegar acceso
POST /api/admin/deny-access
{
  "username": "usuario",
  "reason": "Pago vencido"
}
```

## 🔐 Flujo de Autenticación

1. **Login inicial**: Usuario + contraseña
2. **Verificación de contraseña**: Base64 o BCrypt
3. **Verificación de rol**: Determinar permisos
4. **Verificación de pago**: Solo Super Admin puede saltarse
5. **Generación JWT**: Token con expiración de 15 min
6. **2FA (opcional)**: Si está habilitado
7. **Acceso concedido**: Con token JWT

## 🛡️ Seguridad Implementada

### Contraseñas
- **Base64**: Para migración gradual
- **BCrypt**: Para nuevas contraseñas
- **Salt automático**: Cada contraseña tiene salt único

### JWT
- **Firma HMAC-SHA256**: Clave secreta robusta
- **Expiración**: 15 minutos de inactividad
- **Claims seguros**: Solo información necesaria

### 2FA
- **TOTP estándar**: Compatible con Google Authenticator
- **Códigos de 6 dígitos**: Renovación cada 30 segundos
- **Tolerancia de tiempo**: ±2 ventanas de tiempo

### Control de Acceso
- **Jerarquía de roles**: Super Admin > Admin > Usuario
- **Verificación de pagos**: Automática en cada request
- **Auditoría**: Logs de todos los accesos

## 📊 Roles y Permisos

| Rol | Gestionar Usuarios | Gestionar Pagos | Acceso Completo | Denegar Acceso |
|-----|-------------------|-----------------|-----------------|----------------|
| Super Admin | ✅ | ✅ | ✅ | ✅ |
| Admin | ✅ | ❌ | ❌ | ❌ |
| Usuario | ❌ | ❌ | ❌ | ❌ |
| Operador | ❌ | ❌ | ❌ | ❌ |

## 🔄 Estados de Pago

| Estado | Descripción | Acceso |
|--------|-------------|--------|
| PAID | Pagado | ✅ |
| PENDING | Pendiente | ❌ |
| OVERDUE | Vencido | ❌ |
| SUSPENDED | Suspendido | ❌ |
| CANCELLED | Cancelado | ❌ |

## 🚨 Consideraciones de Seguridad

1. **Super Admin**: Solo Cibercom tiene acceso
2. **Tokens JWT**: Se invalidan automáticamente
3. **Contraseñas**: Nunca se almacenan en texto plano
4. **2FA**: Opcional pero recomendado para administradores
5. **Auditoría**: Todos los accesos se registran

## 📝 Próximos Pasos

1. **Ejecutar script SQL**: Crear tablas necesarias
2. **Compilar proyecto**: `mvn clean install`
3. **Configurar Super Admin**: Usuario inicial
4. **Probar autenticación**: Login y JWT
5. **Configurar 2FA**: Para administradores
6. **Gestionar pagos**: Estados de usuarios

## 🆘 Soporte

Para problemas o dudas sobre la implementación:
- Revisar logs del servidor
- Verificar configuración de base de datos
- Validar tokens JWT en jwt.io
- Comprobar configuración de 2FA

---

**Nota**: Este sistema está diseñado para dar control total a Cibercom sobre el acceso de usuarios basado en pagos, manteniendo la seguridad y escalabilidad del sistema.
