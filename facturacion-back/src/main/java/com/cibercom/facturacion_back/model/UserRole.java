package com.cibercom.facturacion_back.model;

/**
 * Enum que define los roles del sistema con sus niveles de autoridad
 */
public enum UserRole {
    
    SUPER_ADMINISTRADOR("SUPER_ADMIN", "Super Administrador", 1, true, true),
    ADMINISTRADOR("ADMIN", "Administrador", 2, true, false),
    USUARIO("USER", "Usuario", 3, false, false),
    OPERADOR("OPERATOR", "Operador", 4, false, false);
    
    private final String code;
    private final String description;
    private final int level;
    private final boolean canManageUsers;
    private final boolean canManagePayments;
    
    UserRole(String code, String description, int level, boolean canManageUsers, boolean canManagePayments) {
        this.code = code;
        this.description = description;
        this.level = level;
        this.canManageUsers = canManageUsers;
        this.canManagePayments = canManagePayments;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean canManageUsers() {
        return canManageUsers;
    }
    
    public boolean canManagePayments() {
        return canManagePayments;
    }
    
    /**
     * Verifica si este rol tiene autoridad sobre otro rol
     */
    public boolean hasAuthorityOver(UserRole otherRole) {
        return this.level < otherRole.level;
    }
    
    /**
     * Verifica si este rol puede denegar acceso por falta de pago
     */
    public boolean canDenyAccessForPayment() {
        return this == SUPER_ADMINISTRADOR;
    }
    
    /**
     * Obtiene el rol por su código
     */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + code);
    }
    
    /**
     * Obtiene el rol por su nivel
     */
    public static UserRole fromLevel(int level) {
        for (UserRole role : values()) {
            if (role.level == level) {
                return role;
            }
        }
        throw new IllegalArgumentException("Nivel de rol no válido: " + level);
    }
}
