# 🔐 Credenciales de Usuarios - Sistema de Facturación

## 👨‍💼 **Administradores**
| Usuario | Contraseña | Perfil | Descripción |
|---------|------------|--------|-------------|
| `admin` | `admin123` | Administrador | Administrador principal |
| `admin001` | `password123` | Administrador | Administrador secundario |

## 👔 **Jefe de Crédito**
| Usuario | Contraseña | Perfil | Descripción |
|---------|------------|--------|-------------|
| `jefe001` | `jefe123` | Jefe de Crédito | Jefe de departamento de crédito |

## 👷 **Operador de Crédito**
| Usuario | Contraseña | Perfil | Descripción |
|---------|------------|--------|-------------|
| `oper001` | `oper123` | Operador de Crédito | Operador de crédito |

---

## 🎯 **Funcionalidades por Perfil**

### 🔧 **Administrador**
- ✅ **Acceso completo** a todas las funcionalidades
- ✅ **Configuración de menús** - Puede habilitar/deshabilitar menús para otros perfiles
- ✅ **Todas las pestañas** visibles por defecto

### 👔 **Jefe de Crédito**
- ✅ **Todas las pestañas** visibles por defecto
- ❌ **Sin acceso** a Configuración de Menús
- ✅ **Puede ser configurado** por el administrador

### 👷 **Operador de Crédito**
- ✅ **Todas las pestañas** visibles por defecto
- ❌ **Sin acceso** a Configuración de Menús
- ✅ **Puede ser configurado** por el administrador

---

## 🚀 **Cómo usar el sistema**

1. **Ejecutar script SQL:** `SETUP_MENU_SYSTEM.sql`
2. **Iniciar sesión** con cualquiera de las credenciales
3. **Como administrador:** Ir a Configuración → Configuración de Menús
4. **Configurar menús** para cada perfil según necesidades

---

## 📋 **Notas importantes**

- **Por defecto:** Todos los perfiles ven todas las pestañas excepto "Configuración"
- **Solo administradores** pueden acceder a la configuración de menús
- **Los cambios** se aplican inmediatamente
- **El sistema** carga automáticamente la configuración al iniciar sesión
