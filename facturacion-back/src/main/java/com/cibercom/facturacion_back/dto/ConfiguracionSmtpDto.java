package com.cibercom.facturacion_back.dto;

/**
 * DTO para la configuración de SMTP para el envío de correos
 */
public class ConfiguracionSmtpDto {
    private String from;
    private String smtpHost;
    private String port;
    private String username;
    private String password;

    public ConfiguracionSmtpDto() {
    }

    public ConfiguracionSmtpDto(String from, String smtpHost, String port, String username, String password) {
        this.from = from;
        this.smtpHost = smtpHost;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
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
    
    // Additional setter methods for compatibility
    public void setHost(String host) {
        this.smtpHost = host;
    }
    
    public void setFromEmail(String fromEmail) {
        this.from = fromEmail;
    }
}