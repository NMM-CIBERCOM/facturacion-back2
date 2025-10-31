package com.cibercom.facturacion_back.dto;

/**
 * DTO para la respuesta de configuración de autenticación en dos pasos
 */
public class TwoFactorSetupDto {
    
    private String qrCodeUrl;
    private String secretKey;
    private String backupCodes;
    private boolean isEnabled;
    private String message;
    
    // Constructores
    public TwoFactorSetupDto() {}
    
    public TwoFactorSetupDto(String qrCodeUrl, String secretKey, boolean isEnabled) {
        this.qrCodeUrl = qrCodeUrl;
        this.secretKey = secretKey;
        this.isEnabled = isEnabled;
    }
    
    // Getters y Setters
    public String getQrCodeUrl() {
        return qrCodeUrl;
    }
    
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getBackupCodes() {
        return backupCodes;
    }
    
    public void setBackupCodes(String backupCodes) {
        this.backupCodes = backupCodes;
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
