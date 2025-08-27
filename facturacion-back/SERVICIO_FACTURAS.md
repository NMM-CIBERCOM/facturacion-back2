# Servicio de Consulta de Facturas

## Descripción
Este servicio permite consultar **TODAS las facturas** almacenadas en la base de datos activa según el perfil configurado en `application.yml`. No aplica ningún filtro - consulta todas las facturas disponibles sin restricciones.

## Configuración de Base de Datos

### Perfil Oracle (Activo por defecto)
```yaml
spring:
  profiles:
    active: oracle
  datasource:
    url: jdbc:oracle:thin:@//192.168.1.109:1521/xepdb1
    username: nick
    password: N1C0LASm
```

### Perfil MongoDB
```yaml
spring:
  profiles:
    active: mongo
  data:
    mongodb:
      uri: mongodb://localhost:27017/facturacion_mongo
```

## Endpoints

### GET /api/factura/consultar-por-empresa
Consulta **TODAS las facturas** sin ningún filtro en la base de datos activa.

#### Parámetros de Query:
**NINGUNO** - No requiere parámetros, consulta todas las facturas.

#### Ejemplo de uso:
```
GET /api/factura/consultar-por-empresa
```

#### Respuesta:
```json
{
  "exitoso": true,
  "mensaje": "Todas las facturas consultadas exitosamente desde ORACLE",
  "facturas": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "codigoFacturacion": "FAC-550e8400",
      "tienda": "T001",
      "fechaFactura": "2024-01-15T10:30:00",
      "terminal": "TERM-001",
      "boleta": "BOL-550e8400",
      "razonSocial": "Empresa Ejemplo S.A. de C.V.",
      "rfc": "EEJ920629TE3",
      "total": 1250.50,
      "estado": "TIMBRADA",
      "medioPago": "Efectivo",
      "formaPago": "Pago en una sola exhibición",
      "subtotal": 1078.02,
      "iva": 172.48,
      "ieps": 0.00
    }
  ],
  "totalFacturas": 1
}
```

### GET /api/factura/descargar-xml/{uuid}
Descarga el archivo XML de una factura específica.

#### Parámetros de Path:
- `uuid`: UUID único de la factura a descargar

#### Ejemplo de uso:
```
GET /api/factura/descargar-xml/550e8400-e29b-41d4-a716-446655440001
```

#### Respuesta:
- **Archivo XML**: Descarga directa del archivo XML con nombre `FACTURA_{codigoFacturacion}.xml`
- **Headers**: 
  - `Content-Type: application/xml`
  - `Content-Disposition: attachment; filename="FACTURA_FAC-550e8400.xml"`

#### Casos de Error:
- **404 Not Found**: Si la factura no existe o no tiene contenido XML
- **500 Internal Server Error**: Si hay un error en el servidor

## Arquitectura

### Servicios
- **FacturaConsultaService**: Servicio principal que consulta la BD activa
- **FacturaService**: Servicio existente para operaciones de facturación

### Repositorios
- **FacturaRepository**: Acceso a base de datos Oracle
- **FacturaMongoRepository**: Acceso a base de datos MongoDB

### DTOs
- **FacturaConsultaResponse**: Respuesta estructurada de la consulta
- **FacturaConsultaDTO**: DTO para cada factura individual

## Características

### Consulta de Base de Datos Única
- **Consulta selectiva**: Solo consulta la base de datos del perfil activo
- **Perfil Oracle**: Consulta tabla `FACTURAS` en Oracle
- **Perfil MongoDB**: Consulta colección `facturas` en MongoDB
- **Sin filtros**: Muestra TODAS las facturas sin restricciones
- **Sin duplicados**: No hay necesidad de eliminar duplicados entre bases

### Consulta Total
- **Sin parámetros**: No requiere RFC, tienda, fechas u otros criterios
- **Todas las facturas**: Consulta completa de la base de datos
- **Respuesta directa**: Sin lógica de filtrado compleja

### Descarga de XML
- **Descarga individual**: Cada factura tiene su propio botón de descarga
- **Formato estándar**: Archivos XML con nombre `FACTURA_{codigoFacturacion}.xml`
- **Headers correctos**: Configuración automática para descarga de archivos
- **Búsqueda inteligente**: Busca en la base de datos activa según el perfil

### Conversión de Datos
- Mapeo automático entre entidades y DTOs
- Formateo de fechas
- Manejo de campos opcionales

## Lógica de Consulta

### 1. Detección de Perfil
```java
private String getActiveProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles.length > 0) {
        return activeProfiles[0];
    }
    return "oracle"; // Perfil por defecto
}
```

### 2. Consulta Selectiva
```java
String activeProfile = getActiveProfile();

if ("oracle".equals(activeProfile)) {
    // Solo consulta Oracle - TODAS las facturas
    List<Factura> facturasOracle = consultarFacturasOracle();
    facturas.addAll(convertirFacturasOracle(facturasOracle));
} else if ("mongo".equals(activeProfile)) {
    // Solo consulta MongoDB - TODAS las facturas
    List<FacturaMongo> facturasMongo = consultarFacturasMongo();
    facturas.addAll(convertirFacturasMongo(facturasMongo));
}
```

### 3. Consulta Total
```java
// Oracle
private List<Factura> consultarFacturasOracle() {
    // Consultar TODAS las facturas sin ningún filtro
    return facturaRepository.findAll();
}

// MongoDB
private List<FacturaMongo> consultarFacturasMongo() {
    // Consultar TODAS las facturas sin ningún filtro
    return facturaMongoRepository.findAll();
}
```

## Uso en Frontend

### React Component
```typescript
const cargarFacturas = async () => {
  // No se envían parámetros - consulta todas las facturas
  const response = await fetch(`/api/factura/consultar-por-empresa`);
  const data = await response.json();
  
  if (data.exitoso) {
    setFacturas(data.facturas);
    console.log(`Facturas consultadas desde: ${data.mensaje}`);
  }
};
```

### Interfaz Simplificada
- Solo un botón "Consultar Facturas"
- Sin campos de filtro
- Tabla directa con todas las facturas
- Resumen de estadísticas
- Botón de descarga XML por cada factura

### Función de Descarga XML
```typescript
const descargarXml = async (uuid: string, codigoFacturacion: string) => {
  try {
    const response = await fetch(`/api/factura/descargar-xml/${uuid}`);
    
    if (response.ok) {
      // Crear blob y descargar
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `FACTURA_${codigoFacturacion}.xml`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      console.log(`✅ XML descargado: FACTURA_${codigoFacturacion}.xml`);
    } else {
      console.error('❌ Error al descargar XML:', response.statusText);
      alert('Error al descargar el XML. Verifica que la factura tenga contenido XML.');
    }
  } catch (error) {
    console.error('❌ Error al descargar XML:', error);
    alert('Error al descargar el XML. Intenta nuevamente.');
  }
};
```

## Manejo de Errores

### Errores de Base de Datos
- Solo consulta la base de datos configurada
- Manejo de errores específico por base de datos
- Respuesta de error estructurada

### Validaciones
- Sin validación de parámetros (no hay parámetros)
- Conversión segura de tipos
- Sin validación de fechas (ya no aplica)

## Logs y Monitoreo

### Logs de Consulta
```
=== CONSULTA DE TODAS LAS FACTURAS ===
Sin filtros - consultando todas las facturas disponibles
✅ Consulta exitosa: 15 facturas encontradas
Mensaje: Todas las facturas consultadas exitosamente desde ORACLE
```

### Logs de Base de Datos
```
Oracle: 15 facturas encontradas
MongoDB: 12 facturas encontradas
```

### Métricas
- Tiempo de respuesta de la base de datos activa
- Cantidad total de facturas por consulta
- Errores específicos de la base de datos

## Consideraciones de Rendimiento

### Optimizaciones
- Consulta única a la base de datos activa
- Sin filtros complejos
- Sin overhead de consultas múltiples
- Respuesta directa con `findAll()`

### Límites
- Máximo 1000 facturas por consulta (configurable)
- Timeout de 30 segundos por consulta
- Sin cache entre bases de datos

## Ventajas de la Implementación

### 1. **Simplicidad Extrema**
- Solo una consulta por request
- Sin lógica de filtrado
- Sin lógica de deduplicación
- Respuesta más rápida

### 2. **Configurabilidad**
- Cambio de base de datos por perfil
- Sin modificación de código
- Configuración centralizada

### 3. **Mantenibilidad**
- Lógica extremadamente simple y clara
- Menos puntos de falla
- Fácil debugging
- Código más limpio

## Cambio de Base de Datos

Para cambiar de Oracle a MongoDB (o viceversa):

### 1. Cambiar perfil en application.yml
```yaml
spring:
  profiles:
    active: mongo  # Cambiar de 'oracle' a 'mongo'
```

### 2. Reiniciar aplicación
El servicio automáticamente detectará el nuevo perfil y consultará la base de datos correspondiente.

### 3. Verificar en logs
```
INFO  - Perfil activo: mongo
INFO  - Consulta MongoDB completada: X facturas encontradas
```

## Resumen

**El servicio ahora es extremadamente simple y directo:**
- ✅ Solo consulta la base de datos configurada en `application.yml`
- ✅ Muestra **TODAS las facturas** sin ningún filtro
- ✅ No hay consultas múltiples ni deduplicación
- ✅ Respuesta más rápida y directa
- ✅ Fácil cambio de base de datos por perfil
- ✅ Interfaz limpia y simple
- ✅ Respeta la lógica de inserción existente
- ✅ Código más mantenible y claro
- ✅ Sin parámetros de entrada
- ✅ Consulta total de la base de datos
