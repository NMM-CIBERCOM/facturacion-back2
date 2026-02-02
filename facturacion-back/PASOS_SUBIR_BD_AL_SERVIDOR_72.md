# Pasos: Subir tu BD o script al servidor 72.62.128.98

Flujo: **instalar Oracle en 72.62.128.98 → crear usuario → ejecutar .sql o importar .dmp**.

---

## Paso 1: Instalar Oracle en el servidor

1. Conéctate por **SSH** al servidor **72.62.128.98** (usuario root o el que uses).
2. Descarga **Oracle Database XE** (gratuito) para Linux desde oracle.com (busca "Oracle Database 21c XE" o "18c XE" → Linux x64).
3. Sube el instalador al servidor (por WinSCP o `scp`) si lo descargaste en tu PC.
4. Ejecuta el instalador en el servidor según la documentación oficial (crear usuario de sistema Oracle, contraseña SYS, etc.).
5. Al terminar, Oracle queda escuchando en **puerto 1521** en ese servidor. Comprueba que el listener esté activo.

Documentación de instalación: oracle.com → "Oracle Database XE Installation Guide for Linux".

---

## Paso 2: Crear el usuario que usa la aplicación

La aplicación se conecta con un usuario y contraseña (en tu proyecto: **nick** / **N1C0LASm** en el perfil dev). Ese usuario debe existir en Oracle.

1. Entra a Oracle en el servidor (por SSH):
   - Con **sqlplus**: `sqlplus sys/tu_password@localhost:1521/XE as sysdba` (o el SID que te haya dado la instalación, ej. XEPDB1).
   - O desde tu PC con **DBeaver** / **SQL Developer** conectando a `72.62.128.98:1521` (y el servicio, ej. XE o XEPDB1).
2. Crea el usuario (ej. **nick**), asígnale tablespace por defecto y permisos (CONNECT, RESOURCE; o CREATE SESSION, CREATE TABLE, etc.).
3. Opcional: crea un tablespace para ese usuario y asígnaselo como DEFAULT.

Ejemplo conceptual (adaptar SID/tablespace a tu instalación):
- `CREATE USER nick IDENTIFIED BY N1C0LASm DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;`
- `GRANT CONNECT, RESOURCE TO nick;`
- Si usas tablas en un esquema concreto, los permisos que necesite ese esquema.

---

## Paso 3a: Si tienes scripts .sql

1. Sube tus archivos **.sql** al servidor (WinSCP, SCP) a una carpeta, por ejemplo `/tmp/scripts`.
2. En el servidor, ejecuta los scripts contra el usuario **nick** (o el que creaste):
   - Con **sqlplus**: `sqlplus nick/N1C0LASm@localhost:1521/XE @/tmp/scripts/tu_script.sql`
   - O abres cada .sql en DBeaver/SQL Developer conectado como **nick** y ejecutas.
3. Comprueba que se crearon las tablas y datos que la aplicación necesita (p. ej. USUARIOS, PERFIL).

---

## Paso 3b: Si tienes un backup .dmp (export de Oracle)

1. Sube el archivo **.dmp** al servidor (WinSCP, SCP), por ejemplo a `/tmp/backup.dmp`.
2. En el servidor, usa **imp** (o **impdp** si el dump es Data Pump) para importar:
   - Ejemplo con `imp`: `imp nick/N1C0LASm@localhost:1521/XE file=/tmp/backup.dmp full=y` (o las opciones que correspondan a tu export).
   - Si el dump es de otro usuario/esquema, puedes importar a **nick** indicando FROMUSER/TOUSER (en imp) o remap_schema (en impdp).
3. Verifica que las tablas y datos quedaron en el usuario **nick**.

---

## Paso 4: Configurar la aplicación para usar Oracle en ese servidor

Como Oracle y Tomcat están en el **mismo** servidor (72.62.128.98), la app debe conectarse a **localhost:1521**.

1. En tu proyecto, el perfil **dev** ya usa algo como:
   - URL: `jdbc:oracle:thin:@//localhost:1521/xepdb1` (o el servicio que tengas, ej. XE).
   - Usuario: **nick**, contraseña: **N1C0LASm**.
2. En el servidor 72.62.128.98, al arrancar Tomcat (o la app), activa el perfil **dev** (o un perfil que apunte a localhost con ese usuario/contraseña). Por ejemplo en `setenv.sh` de Tomcat: `-Dspring.profiles.active=dev,oracle`.
3. Si el **servicio/SID** no es xepdb1 sino XE u otro, ajusta la URL en `application.yml` (perfil dev) para que coincida con tu instalación (ej. `...@//localhost:1521/XE`).

No hace falta cambiar código; solo perfil activo y, si aplica, URL y usuario/contraseña en la configuración.

---

## Paso 5: Comprobar

1. Reinicia Tomcat en 72.62.128.98.
2. Prueba el login de la aplicación; debería conectar a Oracle en localhost y validar usuario/contraseña contra las tablas que creaste o importaste.

---

## Resumen

| Paso | Acción |
|------|--------|
| 1 | Instalar Oracle (XE) en 72.62.128.98, listener en 1521 |
| 2 | Crear usuario **nick** (o el que use la app) y permisos |
| 3a | Subir y ejecutar tus .sql con ese usuario |
| 3b | O subir e importar tu .dmp con ese usuario |
| 4 | Configurar la app para perfil que use localhost:1521 y ese usuario |
| 5 | Reiniciar Tomcat y probar login |

Si en algún paso usas un SID o servicio distinto (por ejemplo XE en lugar de XEPDB1), adapta la URL en `application.yml` y los comandos sqlplus/imp a ese SID/servicio.
