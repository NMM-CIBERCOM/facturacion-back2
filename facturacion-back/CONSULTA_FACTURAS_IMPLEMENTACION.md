# Implementación del Sistema de Consulta de Facturas

## Resumen de la Implementación

Se ha implementado un sistema completo de consulta de facturas que cumple con todas las especificaciones requeridas. El sistema incluye validaciones del lado del cliente y del servidor, conexión a base de datos mediante stored procedures, y una interfaz de usuario moderna y responsiva.

## Arquitectura del Sistema

### Backend (Java Spring Boot)

#### 1. DTOs (Data Transfer Objects)

**ConsultaFacturaRequest.java**
- Contiene todos los campos de búsqueda especificados
- Incluye métodos de validación para campos obligatorios y rango de fechas
- Valida que al menos un campo de búsqueda esté lleno
- Valida que el rango de fechas no exceda 365 días

**ConsultaFacturaResponse.java**
- Estructura de respuesta estandarizada
- Incluye clase interna `FacturaConsultaDTO` para representar cada factura
- Campos: UUID, RFC Emisor/Receptor, Serie, Folio, Fecha, Importe, Estatus, etc.
- Campo `permiteCancelacion` para determinar si se puede cancelar
- Campo `motivoNoCancelacion` para explicar por qué no se puede cancelar

#### 2. Servicio (ConsultaFacturaService.java)

**Funcionalidades principales:**
- Validación de campos obligatorios
- Validación de rango de fechas (máximo 365 días)
- Validación de formato de fechas
- Lógica de negocio para determinar si una factura permite cancelación
- Manejo de errores y excepciones

**Reglas de cancelación implementadas:**
- ❌ Usuario con perfil CONSULTA
- ❌ Perfiles restringidos (RESTRINGIDO, SIN_PERMISOS, BLOQUEADO)
- ❌ Estatus de facturación no permite cancelación
- ❌ Estatus SAT no permite cancelación
- ❌ Sin reglas de periodo configuradas

#### 3. DAO (Data Access Object)

**ConsultaFacturaDAO.java (Interfaz)**
- Define el contrato para acceder a datos

**ConsultaFacturaDAOImpl.java (Implementación)**
- Utiliza el stored procedure `FEE_UTIL_PCK.buscaFacturas`
- Maneja parámetros de entrada según las especificaciones
- Mapea resultados de la base de datos a DTOs
- Construye nombre completo a partir de campos individuales

#### 4. Controlador (ConsultaFacturaController.java)

**Endpoints:**
- `POST /api/consulta-facturas/buscar` - Consulta principal
- `GET /api/consulta-facturas/health` - Verificación de estado

**Características:**
- Validación de entrada con `@Valid`
- Manejo de errores HTTP apropiados
- CORS habilitado para frontend
- Respuestas estandarizadas

### Frontend (React TypeScript)

#### 1. Componente Principal (ConsultasFacturasPage.tsx)

**Funcionalidades implementadas:**
- Formulario de búsqueda con todos los campos requeridos
- Validación del lado del cliente
- Conexión con el backend mediante API REST
- Tabla de resultados con información completa de facturas
- Indicadores visuales de estatus
- Botones de cancelación condicionales

**Validaciones del frontend:**
- Al menos un campo de búsqueda debe estar lleno
- Rango de fechas máximo 365 días
- Fechas válidas (inicio no posterior a fin)
- Manejo de errores de conexión

**Tabla de resultados:**
- UUID, RFC Emisor/Receptor, Serie, Folio
- Fecha de emisión, Importe
- Estatus de facturación y SAT
- Tienda, Almacén
- Botón de cancelación (visible/oculto según permisos)

#### 2. Componente Button.tsx

**Mejoras implementadas:**
- Propiedad `size` para diferentes tamaños (sm, md, lg)
- Mantiene compatibilidad con implementación existente

## Flujo de Funcionamiento

### 1. Usuario llena formulario
- Al menos un campo debe estar lleno
- Fechas opcionales con validación de rango

### 2. Validación del frontend
- Verifica campos obligatorios
- Valida rango de fechas
- Muestra errores de validación

### 3. Envío al backend
- Datos se envían al endpoint `/api/consulta-facturas/buscar`
- Incluye perfil del usuario para validaciones

### 4. Validación del backend
- Valida campos obligatorios
- Valida rango de fechas
- Verifica formato de fechas

### 5. Consulta a base de datos
- Llama al stored procedure `FEE_UTIL_PCK.buscaFacturas`
- Pasa todos los parámetros requeridos
- Obtiene resultados de la base de datos

### 6. Procesamiento de resultados
- Para cada factura, determina si permite cancelación
- Aplica reglas de negocio según perfil del usuario
- Genera mensajes explicativos para facturas no cancelables

### 7. Respuesta al frontend
- Lista de facturas con información completa
- Indicadores de permisos de cancelación
- Mensajes de error si aplican

### 8. Visualización en frontend
- Tabla con todos los campos requeridos
- Botones de cancelación visibles/ocultos
- Indicadores visuales de estatus
- Contador de resultados

## Configuración de Base de Datos

### Stored Procedure Requerido

```sql
FEE_UTIL_PCK.buscaFacturas(
    p_rfc_receptor VARCHAR2,
    p_nombre_apellido VARCHAR2,
    p_razon_social VARCHAR2,
    p_almacen VARCHAR2,
    p_usuario VARCHAR2,
    p_serie VARCHAR2,
    p_folio VARCHAR2,
    p_fecha_inicio DATE,
    p_fecha_fin DATE,
    p_perfil_usuario VARCHAR2,
    p_uuid VARCHAR2
)
```

### Parámetros de Entrada
- **p_rfc_receptor**: RFC del receptor de la factura
- **p_nombre_apellido**: Nombre completo del cliente
- **p_razon_social**: Razón social de la empresa
- **p_almacen**: Código del almacén
- **p_usuario**: Usuario que emitió la factura
- **p_serie**: Serie de la factura
- **p_folio**: Folio de la factura
- **p_fecha_inicio**: Fecha de inicio del rango
- **p_fecha_fin**: Fecha de fin del rango
- **p_perfil_usuario**: Perfil del usuario que consulta
- **p_uuid**: UUID específico de la factura (opcional)

### Estructura de Salida Esperada
El stored procedure debe retornar un cursor con las siguientes columnas:
- UUID
- RFC_EMISOR
- RFC_RECEPTOR
- SERIE
- FOLIO
- FECHA_EMISION
- IMPORTE
- ESTATUS_FACTURACION
- ESTATUS_SAT
- TIENDA
- ALMACEN
- USUARIO

## Reglas de Negocio Implementadas

### Validaciones de Entrada
1. **Campos obligatorios**: Al menos uno debe estar lleno
2. **Rango de fechas**: Máximo 365 días
3. **Formato de fechas**: Validación de formato dd/MM/yy
4. **Fechas válidas**: Fecha inicio no posterior a fecha fin

### Reglas de Cancelación
1. **Perfil de usuario**: CONSULTA no puede cancelar
2. **Perfiles restringidos**: RESTRINGIDO, SIN_PERMISOS, BLOQUEADO
3. **Estatus de facturación**: Solo VIGENTE, ACTIVA, EMITIDA
4. **Estatus SAT**: Solo VIGENTE, ACTIVA, EMITIDA
5. **Reglas de periodo**: Configuradas en base de datos

### Mensajes de Error
- "Es necesario seleccionar RFC receptor o Nombre y Apellido Paterno o Razón Social o Almacén o Usuario o Serie"
- "El rango máximo permitido es de 365 días. Reintente"
- "Formato de fechas inválido. Use formato dd/MM/yy"
- "La fecha de inicio no puede ser posterior a la fecha fin"

## Instalación y Configuración

### Backend
1. Asegurar que el stored procedure `FEE_UTIL_PCK.buscaFacturas` esté disponible
2. Configurar conexión a base de datos Oracle en `application.yml`
3. Compilar y ejecutar la aplicación Spring Boot

### Frontend
1. Instalar dependencias: `npm install`
2. Configurar URL del backend en el componente
3. Ejecutar: `npm run dev`

## Pruebas del Sistema

### Casos de Prueba Implementados

1. **Búsqueda por RFC**: Ingresar RFC válido
2. **Búsqueda por nombre**: Ingresar nombre y apellidos
3. **Búsqueda por fechas**: Rango válido (≤365 días)
4. **Búsqueda por almacén**: Seleccionar almacén específico
5. **Búsqueda por serie**: Ingresar serie de factura
6. **Validación de campos vacíos**: Enviar formulario sin datos
7. **Validación de rango de fechas**: Exceder 365 días
8. **Validación de fechas inválidas**: Fecha inicio > fecha fin

### Verificación de Funcionalidades
- ✅ Validación de campos obligatorios
- ✅ Validación de rango de fechas
- ✅ Conexión con base de datos
- ✅ Mapeo de resultados
- ✅ Lógica de permisos de cancelación
- ✅ Manejo de errores
- ✅ Interfaz responsiva
- ✅ Indicadores visuales

## Consideraciones Técnicas

### Seguridad
- Validación tanto en frontend como backend
- Sanitización de parámetros de entrada
- Manejo seguro de conexiones a base de datos

### Rendimiento
- Uso de stored procedures para consultas complejas
- Transacciones de solo lectura para consultas
- Paginación opcional para grandes volúmenes de datos

### Mantenibilidad
- Código modular y bien estructurado
- Separación clara de responsabilidades
- Documentación completa de la implementación

## Próximos Pasos Recomendados

1. **Implementar paginación** para grandes volúmenes de resultados
2. **Agregar filtros adicionales** según necesidades del negocio
3. **Implementar caché** para consultas frecuentes
4. **Agregar logs** para auditoría de consultas
5. **Implementar exportación** de resultados a Excel/PDF
6. **Agregar métricas** de uso y rendimiento

## Soporte y Mantenimiento

Para cualquier consulta o problema con la implementación, revisar:
1. Logs de la aplicación Spring Boot
2. Logs de la base de datos Oracle
3. Consola del navegador para errores del frontend
4. Network tab del navegador para errores de API

La implementación está diseñada para ser robusta y fácil de mantener, siguiendo las mejores prácticas de desarrollo web moderno.
