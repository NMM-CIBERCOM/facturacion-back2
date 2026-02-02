#!/usr/bin/expect -f
set timeout 30
spawn ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -L 8443:demo-facturacion.finkok.com:443 root@174.136.25.157 -N
expect {
    "password:" {
        send "AdminPts@2026\r"
        exp_continue
    }
    "Permission denied" {
        exit 1
    }
    timeout {
        exit 1
    }
}
interact
