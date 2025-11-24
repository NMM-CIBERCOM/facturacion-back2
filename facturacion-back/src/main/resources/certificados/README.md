# Instrucciones para Configurar Certificados CSD

## Archivos Necesarios

Para firmar CFDI 4.0 con CSD, necesitas colocar estos archivos en esta carpeta:

1. **Certificado**: `CSD_Sucursal_1_MISC491214B86_20230509_130937` (archivo sin extensión o .cer)
2. **Llave Privada**: `CSD_Sucursal_1_MISC491214B86_20230509_130937.key`

## Pasos para Configurar

### 1. Copiar Archivos

Copia los archivos desde donde los tienes actualmente a esta carpeta:
- `src/main/resources/certificados/`

### 2. Verificar Nombres

Asegúrate de que los archivos se llamen exactamente:
- `CSD_Sucursal_1_MISC491214B86_20230509_130937` (o con extensión `.cer`)
- `CSD_Sucursal_1_MISC491214B86_20230509_130937.key`

**IMPORTANTE**: Si tus archivos tienen nombres diferentes, ajusta la configuración en `application.yml`

### 3. Verificar Configuración

En `application.yml` (raíz del proyecto), verifica estas líneas:

```yaml
facturacion:
  csd:
    enabled: true  # Debe estar en true
    certificado:
      path: classpath:certificados/CSD_Sucursal_1_MISC491214B86_20230509_130937
    llave:
      path: classpath:certificados/CSD_Sucursal_1_MISC491214B86_20230509_130937.key
      password: ""  # Agregar contraseña si tu llave tiene una
```

### 4. Si la Llave Privada Tiene Contraseña

Si tu archivo `.key` tiene contraseña, actualiza en `application.yml`:

```yaml
llave:
  password: "tu_contraseña_aqui"
```

### 5. Formato de Archivos

El sistema soporta:
- **Certificado**: Archivo `.cer` (X.509) en formato PEM o DER
- **Llave Privada**: Archivo `.key` (PKCS#8) en formato PEM

## Verificación

Después de configurar:
1. Reinicia la aplicación
2. Revisa los logs al iniciar - debería mostrar "Certificado cargado: ..."
3. Intenta timbrar un CFDI 4.0

## Solución de Problemas

### Error: "No se encontró el certificado"
- Verifica que los archivos estén en `src/main/resources/certificados/`
- Verifica que los nombres coincidan exactamente con la configuración en `application.yml`
- Reinicia la aplicación después de copiar los archivos

### Error: "No se pudo firmar el XML"
- Verifica que el certificado y la llave privada correspondan al mismo CSD
- Verifica que la contraseña sea correcta (si aplica)
- Revisa los logs para más detalles del error

## Nota de Seguridad

⚠️ **IMPORTANTE**: Estos archivos contienen información sensible. 
- NO los subas a repositorios públicos (Git)
- Considera agregar `certificados/` a tu `.gitignore`
- Mantén backups seguros de estos archivos





