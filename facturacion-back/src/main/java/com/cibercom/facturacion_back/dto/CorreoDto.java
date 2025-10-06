package com.cibercom.facturacion_back.dto;

import java.util.List;

public class CorreoDto {
    private String from;
    private String to;
    private String subject;
    private String mensaje;
    private List<AdjuntoDto> adjuntos;

    private String smtpHost;
    private String port;
    private String username;
    private String password;

    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
    }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    public String getMensaje() {
        return mensaje;
    }
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    public List<AdjuntoDto> getAdjuntos() {
        return adjuntos;
    }
    public void setAdjuntos(List<AdjuntoDto> adjuntos) {
        this.adjuntos = adjuntos;
    }
    public String getSmtpHost() {
        return smtpHost;
    }
    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }
    public String getPort() {
        return port;
    }
    public void setPort(String port) {
        this.port = port;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public static class AdjuntoDto {
        private String nombre;
        private byte[] contenido;
        private String tipoMime;

        public String getNombre() {
            return nombre;
        }
        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
        public byte[] getContenido() {
            return contenido;
        }
        public void setContenido(byte[] contenido) {
            this.contenido = contenido;
        }
        public String getTipoMime() {
            return tipoMime;
        }
        public void setTipoMime(String tipoMime) {
            this.tipoMime = tipoMime;
        }
    }
}