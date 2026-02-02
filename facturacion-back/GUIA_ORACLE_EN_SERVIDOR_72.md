# Guía: Base de datos Oracle en el servidor 72.62.128.98

Tienes dos caminos: **instalar Oracle en ese servidor** (recomendado si el otro servidor cayó) o **usar una BD en otro sitio** y solo abrir conectividad. Esta guía se centra en instalar Oracle en 72.62.128.98 y subir tu script/BD ahí.

---

## Opción recomendada: instalar Oracle en 72.62.128.98

Así la aplicación (Tomcat en ese mismo servidor) se conecta a **localhost:1521** y no dependes de otro servidor.

### 1. Instalar Oracle en el servidor

En el servidor **72.62.128.98** (por SSH, con el usuario que tengas, por ejemplo root):

- **Oracle XE (Express Edition)** es la opción gratuita y suele bastar.
- Descargas el instalador para Linux desde la web de Oracle (Oracle Database XE 21c o 18c para Linux).
- Sigues el instalador: crea la instancia, define la contraseña del usuario **SYS** (y **SYSTEM** si lo pide).
- Al terminar, Oracle queda escuchando en **puerto 1521** en ese servidor.

Documentación oficial de instalación de Oracle XE en Linux está en oracle.com (busca "Oracle Database XE Installation Guide Linux").

### 2. Crear el usuario y esquema que usa la aplicación

La aplicación está configurada para conectarse con un usuario y contraseña (en tu `application.yml` aparece **nick** / **N1C0LASm** en dev). Ese usuario debe existir en Oracle y tener sus tablas.

- Entras a Oracle como **SYSTEM** (o SYS) con **sqlplus** o con una herramienta gráfica (DBeaver, SQL Developer) conectando a `72.62.128.98:1521` (o desde el propio servidor a `localhost:1521`).
- Creas el usuario (por ejemplo **nick**), le asignas tablespace y permisos (CONNECT, RESOURCE, o los que necesites).
- Si tienes **scripts SQL** (archivos .sql con CREATE TABLE, INSERT, etc.), los ejecutas con ese usuario para crear las tablas y datos (USUARIOS, PERFIL, etc.).

### 3. Si tienes un export (backup) de la BD antigua

Si en lugar de scripts tienes un **dump** (archivo .dmp exportado con `exp` o `expdp`):

- Subes el .dmp al servidor 72.62.128.98 (por WinSCP o SCP).
- En el servidor usas **imp** o **impdp** para importar ese dump en la instancia Oracle que instalaste, con el usuario/esquema que use la aplicación.

Así “subes” tu BD al servidor: primero instalas Oracle, luego importas el backup ahí.

### 4. Configurar la aplicación para usar Oracle en ese servidor

- Si **Tomcat y Oracle están en el mismo servidor (72.62.128.98)**:
  - La URL de la BD debe ser **localhost** (o 127.0.0.1), puerto **1521**, y el servicio/SID que te haya dado la instalación (por ejemplo **XE** o **XEPDB1**).
  - En tu proyecto ya tienes un perfil **dev** con algo como `jdbc:oracle:thin:@//localhost:1521/xepdb1`. Puedes usar ese perfil en el servidor, o crear un perfil (por ejemplo **prod-local**) con la misma URL y usuario/contraseña que creaste en el paso 2.
- Asegúrate de que el **usuario y contraseña** en `application.yml` coincidan con el usuario que creaste en Oracle (por ejemplo nick / N1C0LASm, o el que tú definas).

No necesitas cambiar código; solo la configuración (perfil activo y/o URL, usuario, contraseña).

### 5. Resumen de pasos (orden sugerido)

1. Conectarte por SSH al servidor 72.62.128.98.
2. Instalar Oracle XE (o la versión que elijas) y dejar el listener en 1521.
3. Crear el usuario/esquema que usa la app (ej. nick) y ejecutar tus scripts .sql **o** importar tu .dmp.
4. Configurar la aplicación para que use **localhost:1521** y ese usuario (perfil dev o prod-local en ese servidor).
5. Reiniciar Tomcat y probar el login.

---

## Opción alternativa: BD en otro servidor

Si prefieres **no** instalar Oracle en 72.62.128.98 y usar una BD en otro equipo (por ejemplo cuando 174.136.25.157 vuelva a estar arriba, o un Oracle en la nube):

- Instalas Oracle (o usas uno ya existente) en **ese otro** servidor.
- En 72.62.128.98 solo configuras la aplicación con la URL de **ese** servidor (IP/host y puerto 1521).
- Aseguras que entre 72.62.128.98 y el servidor de Oracle haya **conectividad de red** y que el firewall permita el puerto 1521 (de 72.62.128.98 hacia el servidor de Oracle).

En ese caso no “subes” la BD al 72.62.128.98; la BD sigue en el otro servidor y la app se conecta por red.

---

## Qué necesitas tener claro

- **¿Tienes scripts .sql (CREATE TABLE, INSERT, etc.)?** → Instalas Oracle en 72.62.128.98, creas el usuario, ejecutas los scripts contra ese Oracle.
- **¿Tienes un backup .dmp de la BD antigua?** → Instalas Oracle en 72.62.128.98, creas el usuario/esquema, importas el .dmp ahí.
- **¿No tienes ni scripts ni backup?** → Instalas Oracle, creas el usuario y tendrías que recrear las tablas a mano (o desde el código si usas algo como Flyway/Liquibase con scripts en el proyecto).

Si me dices si tienes .sql, .dmp o nada, se puede afinar el siguiente paso (por ejemplo: “solo tengo .sql” o “solo tengo .dmp”).
