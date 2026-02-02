# Solución Final: Crear Túnel SSH

## Paso 1: Subir el Script al Servidor

1. **Sube el archivo `crear-tunel-finkok.sh` al servidor** usando WinSCP (a `/tmp` o `/root`)

## Paso 2: Ejecutar en el Servidor

Ejecuta estos comandos UNO POR UNO en la terminal de WinSCP:

### 1. Instalar expect:

```bash
sudo apt-get install -y expect
```

### 2. Dar permisos al script:

```bash
chmod +x /tmp/crear-tunel-finkok.sh
```

(O la ruta donde lo subiste)

### 3. Ejecutar el script en segundo plano:

```bash
nohup /tmp/crear-tunel-finkok.sh > /tmp/tunnel.log 2>&1 &
```

### 4. Esperar 3 segundos y verificar:

```bash
sleep 3
ps aux | grep ssh | grep 8443
netstat -tuln | grep 8443
```

### 5. Probar el túnel:

```bash
curl -v --connect-timeout 5 -k https://localhost:8443/servicios/soap/stamp 2>&1 | head -15
```

## Si el Túnel Funciona

Si el `curl` funciona, entonces el túnel está activo y modifico el código para usar `https://localhost:8443`.

## Si el Túnel NO Funciona

Si después de todo esto el túnel no funciona, la mejor alternativa es **modificar el código para que use Apache HttpClient** que puede manejar mejor las conexiones, aunque probablemente no solucione el problema de firewall del proveedor.
