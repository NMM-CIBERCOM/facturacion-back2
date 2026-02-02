# Alternativas para Crear Túnel SSH

## Opción 1: Usar autossh (Más Robusto)

### Instalar autossh:

```bash
sudo apt-get install -y autossh
```

### Crear túnel con autossh:

```bash
autossh -M 20000 -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -f
```

**Nota:** autossh puede pedir contraseña. Si es así, usa:

```bash
sshpass -p 'AdminPts@2026' autossh -M 20000 -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N -f
```

## Opción 2: Crear Túnel con nohup (Más Simple)

```bash
nohup sshpass -p 'AdminPts@2026' ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N > /tmp/ssh_tunnel.log 2>&1 &
```

Luego verificar:

```bash
ps aux | grep ssh | grep 8443
netstat -tuln | grep 8443
```

## Opción 3: Usar expect (Si sshpass no funciona bien)

### Instalar expect:

```bash
sudo apt-get install -y expect
```

### Crear script con expect:

```bash
cat > /tmp/create_tunnel.sh << 'EOF'
#!/usr/bin/expect
set timeout 30
spawn ssh -o StrictHostKeyChecking=no -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N
expect "password:"
send "AdminPts@2026\r"
interact
EOF

chmod +x /tmp/create_tunnel.sh
nohup /tmp/create_tunnel.sh > /tmp/tunnel.log 2>&1 &
```

## Opción 4: Modificar Código para Usar Servidor Anterior como Proxy

Si el túnel SSH no funciona, podemos modificar el código para que use el servidor anterior como proxy HTTP. Esto requiere:

1. Configurar un proxy HTTP en el servidor anterior
2. Modificar el código para usar ese proxy

Pero primero intenta las opciones 1, 2 o 3.
