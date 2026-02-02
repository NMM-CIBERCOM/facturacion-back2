# Comandos para probar error de conexión/firewall Finkok y conexión con otros sitios

Ejecutar estos comandos **en el servidor donde corre la aplicación** (p. ej. el Tomcat remoto Linux), para distinguir si el problema es solo con Finkok o con toda la salida a Internet.

---

## 1. Probar conexión a Finkok (demo)

### DNS (resolución del dominio)
```bash
nslookup demo-facturacion.finkok.com
```
O con `dig`:
```bash
dig demo-facturacion.finkok.com +short
```

### Puerto 443 (HTTPS) – bash
```bash
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" 2>/dev/null && echo "✓ Puerto 443 ACCESIBLE" || echo "✗ Puerto 443 NO ACCESIBLE"
```

### Curl al endpoint de timbrado (timeout 10 s)
```bash
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -30
```

### Wget (alternativa)
```bash
wget --spider --timeout=10 https://demo-facturacion.finkok.com/servicios/soap/stamp
```

### Ruta hasta Finkok (dónde se corta)
```bash
traceroute demo-facturacion.finkok.com 2>&1 | head -20
```

---

## 2. Probar conexión a otros sitios (confirmar que Internet funciona)

### Ping a DNS de Google
```bash
ping -c 3 8.8.8.8
```

### Ping a un dominio
```bash
ping -c 3 google.com
```

### Curl a Google
```bash
curl -v --max-time 10 https://www.google.com 2>&1 | head -15
```

### Curl a GitHub
```bash
curl -v --max-time 10 https://www.github.com 2>&1 | head -15
```

### Curl a otro HTTPS cualquiera
```bash
curl -v --max-time 10 https://httpbin.org/get 2>&1 | head -15
```

---

## 3. Script todo-en-uno (Linux)

Puedes subir y ejecutar en el servidor el script que ya tienes:

```bash
bash verificar-finkok.sh
```

O ejecutar este bloque manualmente:

```bash
echo "=== 1. DNS Finkok ==="
nslookup demo-facturacion.finkok.com

echo "=== 2. Puerto 443 Finkok ==="
timeout 5 bash -c "</dev/tcp/demo-facturacion.finkok.com/443" 2>/dev/null && echo "✓ Finkok 443 OK" || echo "✗ Finkok 443 FALLO"

echo "=== 3. Curl Finkok ==="
curl -v --max-time 10 https://demo-facturacion.finkok.com/servicios/soap/stamp 2>&1 | head -25

echo "=== 4. Internet (ping 8.8.8.8) ==="
ping -c 3 8.8.8.8 2>&1 | head -5

echo "=== 5. Curl Google ==="
curl -v --max-time 10 https://www.google.com 2>&1 | head -10

echo "=== 6. Curl GitHub ==="
curl -v --max-time 10 https://www.github.com 2>&1 | head -10
```

---

## 4. Cómo interpretar los resultados

| Resultado | Significado |
|-----------|-------------|
| **Ping 8.8.8.8 OK** y **curl Google/GitHub OK** | El servidor tiene salida a Internet. |
| **Finkok: timeout / Connection timed out** | El bloqueo es hacia Finkok (firewall del proveedor, reglas UFW/iptables, o red intermedia). |
| **Todo falla (ping y curl)** | Problema general de red o firewall saliente en el servidor. |
| **nslookup Finkok falla** | Problema de DNS; probar otro DNS o `dig` con servidor: `dig @8.8.8.8 demo-facturacion.finkok.com +short`. |

---

## 5. En Windows (PowerShell) – si pruebas desde tu PC

Solo para comprobar desde tu máquina que Finkok responde (no sustituye la prueba en el servidor):

```powershell
# Resolución DNS
nslookup demo-facturacion.finkok.com

# Probar HTTPS (PowerShell)
Invoke-WebRequest -Uri "https://demo-facturacion.finkok.com/servicios/soap/stamp" -Method GET -TimeoutSec 10 -UseBasicParsing | Select-Object StatusCode, StatusDescription

# Otros sitios
Invoke-WebRequest -Uri "https://www.google.com" -Method GET -TimeoutSec 10 -UseBasicParsing | Select-Object StatusCode
Invoke-WebRequest -Uri "https://www.github.com" -Method GET -TimeoutSec 10 -UseBasicParsing | Select-Object StatusCode
```

---

## 6. Firewall en el servidor (Linux)

Si Finkok falla y otros sitios no, revisar reglas que afecten salida al puerto 443:

```bash
# UFW
sudo ufw status numbered

# iptables (reglas OUTPUT)
sudo iptables -L OUTPUT -n -v 2>/dev/null | head -30
```

Documentos relacionados en el proyecto: `SOLUCION_FIREWALL_FINKOK.md`, `SOLUCION_FIREWALL_EXTERNO.md`, `VERIFICAR_CONECTIVIDAD_SERVIDOR.sh`.
