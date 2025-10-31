package com.cibercom.facturacion_back.dto;

import com.cibercom.facturacion_back.model.UserRole;
import com.cibercom.facturacion_back.model.PaymentStatus;

/**
 * DTO para los datos del usuario en la respuesta de login
 */
public class UsuarioLoginDto {
    
    private String noUsuario;
    private String nombreEmpleado;
    private String nombrePerfil;
    private Integer idPerfil;
    private String estatusUsuario;
    private Integer idDfi;
    private Integer idEstacionamiento;
    private String modificaUbicacion;
    
    // Nuevos campos para seguridad y roles
    private UserRole role;
    private PaymentStatus paymentStatus;
    private boolean twoFactorEnabled;
    private String twoFactorSecret;
    private String lastLogin;
    private String sessionToken;
    private boolean isSuperAdmin;
    
    // Constructores
    public UsuarioLoginDto() {}
    
    public UsuarioLoginDto(String noUsuario, String nombreEmpleado, String nombrePerfil, 
                          Integer idPerfil, String estatusUsuario, Integer idDfi, 
                          Integer idEstacionamiento, String modificaUbicacion) {
        this.noUsuario = noUsuario;
        this.nombreEmpleado = nombreEmpleado;
        this.nombrePerfil = nombrePerfil;
        this.idPerfil = idPerfil;
        this.estatusUsuario = estatusUsuario;
        this.idDfi = idDfi;
        this.idEstacionamiento = idEstacionamiento;
        this.modificaUbicacion = modificaUbicacion;
        this.isSuperAdmin = false;
        this.twoFactorEnabled = false;
    }
    
    // Getters y Setters existentes
    public String getNoUsuario() {
        return noUsuario;
    }
    
    public void setNoUsuario(String noUsuario) {
        this.noUsuario = noUsuario;
    }
    
    public String getNombreEmpleado() {
        return nombreEmpleado;
    }
    
    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }
    
    public String getNombrePerfil() {
        return nombrePerfil;
    }
    
    public void setNombrePerfil(String nombrePerfil) {
        this.nombrePerfil = nombrePerfil;
    }
    
    public Integer getIdPerfil() {
        return idPerfil;
    }
    
    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }
    
    public String getEstatusUsuario() {
        return estatusUsuario;
    }
    
    public void setEstatusUsuario(String estatusUsuario) {
        this.estatusUsuario = estatusUsuario;
    }
    
    public Integer getIdDfi() {
        return idDfi;
    }
    
    public void setIdDfi(Integer idDfi) {
        this.idDfi = idDfi;
    }
    
    public Integer getIdEstacionamiento() {
        return idEstacionamiento;
    }
    
    public void setIdEstacionamiento(Integer idEstacionamiento) {
        this.idEstacionamiento = idEstacionamiento;
    }
    
    public String getModificaUbicacion() {
        return modificaUbicacion;
    }
    
    public void setModificaUbicacion(String modificaUbicacion) {
        this.modificaUbicacion = modificaUbicacion;
    }
    
    // Nuevos getters y setters
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
        this.isSuperAdmin = (role == UserRole.SUPER_ADMINISTRADOR);
    }
    
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }
    
    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
    
    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }
    
    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }
    
    public String getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public boolean isSuperAdmin() {
        return isSuperAdmin;
    }
    
    public void setSuperAdmin(boolean superAdmin) {
        isSuperAdmin = superAdmin;
    }
    
    /**
     * Verifica si el usuario tiene acceso basado en su estado de pago
     */
    public boolean hasAccess() {
        if (isSuperAdmin) {
            return true; // Super admin siempre tiene acceso
        }
        return paymentStatus != null && paymentStatus.allowsAccess();
    }
    
    /**
     * Verifica si el usuario puede gestionar otros usuarios
     */
    public boolean canManageUsers() {
        return role != null && role.canManageUsers();
    }
    
    /**
     * Verifica si el usuario puede gestionar pagos
     */
    public boolean canManagePayments() {
        return role != null && role.canManagePayments();
    }
}