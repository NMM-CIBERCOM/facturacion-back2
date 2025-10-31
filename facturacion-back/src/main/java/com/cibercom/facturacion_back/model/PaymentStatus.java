package com.cibercom.facturacion_back.model;

import java.time.LocalDateTime;

/**
 * Enum que define los estados de pago de un usuario
 */
public enum PaymentStatus {
    
    PAID("PAID", "Pagado", true),
    PENDING("PENDING", "Pendiente", false),
    OVERDUE("OVERDUE", "Vencido", false),
    SUSPENDED("SUSPENDED", "Suspendido", false),
    CANCELLED("CANCELLED", "Cancelado", false);
    
    private final String code;
    private final String description;
    private final boolean allowsAccess;
    
    PaymentStatus(String code, String description, boolean allowsAccess) {
        this.code = code;
        this.description = description;
        this.allowsAccess = allowsAccess;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean allowsAccess() {
        return allowsAccess;
    }
    
    /**
     * Obtiene el estado por su código
     */
    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de pago no válido: " + code);
    }
}

/**
 * Clase que representa el estado de pago de un usuario
 */
class UserPaymentStatus {
    
    private String noUsuario;
    private PaymentStatus status;
    private LocalDateTime lastPaymentDate;
    private LocalDateTime nextPaymentDate;
    private Double amount;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Constructores
    public UserPaymentStatus() {}
    
    public UserPaymentStatus(String noUsuario, PaymentStatus status) {
        this.noUsuario = noUsuario;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters y Setters
    public String getNoUsuario() {
        return noUsuario;
    }
    
    public void setNoUsuario(String noUsuario) {
        this.noUsuario = noUsuario;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getLastPaymentDate() {
        return lastPaymentDate;
    }
    
    public void setLastPaymentDate(LocalDateTime lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }
    
    public LocalDateTime getNextPaymentDate() {
        return nextPaymentDate;
    }
    
    public void setNextPaymentDate(LocalDateTime nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    /**
     * Verifica si el usuario tiene acceso basado en su estado de pago
     */
    public boolean hasAccess() {
        return status.allowsAccess();
    }
    
    /**
     * Verifica si el pago está vencido
     */
    public boolean isOverdue() {
        return nextPaymentDate != null && 
               LocalDateTime.now().isAfter(nextPaymentDate) && 
               status != PaymentStatus.PAID;
    }
}
