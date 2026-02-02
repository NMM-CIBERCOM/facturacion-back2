# Solución: 404 - index.html no encontrado (/facturacion-backend/index.html)

## Qué significa el error

- **Estado HTTP 404 - No encontrado**
- Mensaje: *"The requested resource [/facturacion-backend/index.html] is not available"*
- El servidor Tomcat **sí responde** en `/facturacion-backend`, pero **no tiene** el archivo `index.html` (ni el frontend) dentro del WAR desplegado.

---

## Causas habituales

### 1. El WAR se generó sin el frontend (la más común)

El WAR que está en el servidor se construyó **sin** haber copiado antes el build del frontend a `src/main/webapp`. En ese caso el WAR solo lleva la API y no `index.html` ni la carpeta `assets/`.

**Solución:** Volver a generar el WAR incluyendo el frontend y volver a desplegar.

### 2. Context path distinto al que abres en el navegador

Si en Tomcat desplegaste el WAR con el nombre por defecto (`facturacion-backend-0.0.1-SNAPSHOT.war`), el contexto suele ser:

- `https://redcibercom.cloud/facturacion-backend-0.0.1-SNAPSHOT/`

y **no** `https://redcibercom.cloud/facturacion-backend/`. Si abres la segunda URL, puede que otra app o nada responda y veas 404.

**Solución:** Probar la URL con el nombre completo del WAR (sin `.war`), o renombrar el WAR a `facturacion-backend.war` antes de desplegar para usar `/facturacion-backend/`.

---

## Pasos para que vuelva a abrir (incluir frontend en el WAR)

Ejecutar en este orden:

### 1. Compilar el frontend (facturacion-cibercom)

```bash
cd facturacion-cibercom
npm run build:prod
```

Se genera la carpeta `dist/` con `index.html` y `assets/`.

### 2. Copiar el frontend al backend

**En Linux/Mac (Git Bash):**
```bash
cd facturacion-cibercom
./copy-frontend-to-backend.sh
```

**En Windows (PowerShell), manualmente:**
```powershell
# Desde la raíz del repo (ajusta rutas si es necesario)
$frontendDist = "facturacion-cibercom\dist"
$backendWebapp = "facturacion-back\facturacion-back\src\main\webapp"

# Limpiar webapp excepto WEB-INF
Get-ChildItem $backendWebapp -Exclude "WEB-INF" | Remove-Item -Recurse -Force

# Copiar contenido de dist a webapp
Copy-Item -Path "$frontendDist\*" -Destination $backendWebapp -Recurse -Force
```

Comprueba que existan `webapp\index.html` y `webapp\assets\` con los `.js`.

### 3. Generar el WAR (facturacion-back)

```bash
cd facturacion-back/facturacion-back
mvn clean package
```

El WAR resultante está en `target/facturacion-backend-0.0.1-SNAPSHOT.war` (o el nombre que tenga tu `pom.xml`).

### 4. Desplegar en Tomcat

- Sube ese WAR al servidor (p. ej. a `webapps/`).
- Si quieres la URL `https://redcibercom.cloud/facturacion-backend/`, renombra el WAR a `facturacion-backend.war` antes de desplegarlo (o configura el context path en Tomcat).
- Reinicia Tomcat si hace falta.

### 5. Probar en el navegador

- Con WAR renombrado a `facturacion-backend.war`:  
  `https://redcibercom.cloud/facturacion-backend/`  
  (o `.../facturacion-backend/index.html`)
- Con nombre por defecto del WAR:  
  `https://redcibercom.cloud/facturacion-backend-0.0.1-SNAPSHOT/`

---

## Comprobar que el WAR lleva el frontend (antes de subir)

En tu máquina, después de `mvn clean package`:

**Windows (PowerShell):**
```powershell
# El WAR es un ZIP; listar raíz
jar -tf target\facturacion-backend-0.0.1-SNAPSHOT.war | findstr "index.html assets"
```

**Linux/Mac:**
```bash
jar -tf target/facturacion-backend-0.0.1-SNAPSHOT.war | grep -E "index\.html|^assets/"
```

Deberías ver `index.html` y entradas bajo `assets/`. Si no aparecen, el WAR se generó sin el frontend: repite los pasos 1 y 2 y luego el 3.
