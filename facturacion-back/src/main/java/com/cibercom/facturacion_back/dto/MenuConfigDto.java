package com.cibercom.facturacion_back.dto;

/**
 * DTO para la configuración de menús
 */
public class MenuConfigDto {
    private Long idConfig;
    private Integer idPerfil;
    private String menuLabel;
    private String menuPath;
    private Boolean isVisible;
    private Integer orden;
    private String nombrePerfil;

    // Constructor por defecto
    public MenuConfigDto() {
    }

    // Constructor con parámetros
    public MenuConfigDto(Long idConfig, Integer idPerfil, String menuLabel, String menuPath, 
                        Boolean isVisible, Integer orden, String nombrePerfil) {
        this.idConfig = idConfig;
        this.idPerfil = idPerfil;
        this.menuLabel = menuLabel;
        this.menuPath = menuPath;
        this.isVisible = isVisible;
        this.orden = orden;
        this.nombrePerfil = nombrePerfil;
    }

    // Getters y Setters
    public Long getIdConfig() {
        return idConfig;
    }

    public void setIdConfig(Long idConfig) {
        this.idConfig = idConfig;
    }

    public Integer getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }

    public String getMenuLabel() {
        return menuLabel;
    }

    public void setMenuLabel(String menuLabel) {
        this.menuLabel = menuLabel;
    }

    public String getMenuPath() {
        return menuPath;
    }

    public void setMenuPath(String menuPath) {
        this.menuPath = menuPath;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public String getNombrePerfil() {
        return nombrePerfil;
    }

    public void setNombrePerfil(String nombrePerfil) {
        this.nombrePerfil = nombrePerfil;
    }

    @Override
    public String toString() {
        return "MenuConfigDto{" +
                "idConfig=" + idConfig +
                ", idPerfil=" + idPerfil +
                ", menuLabel='" + menuLabel + '\'' +
                ", menuPath='" + menuPath + '\'' +
                ", isVisible=" + isVisible +
                ", orden=" + orden +
                ", nombrePerfil='" + nombrePerfil + '\'' +
                '}';
    }
}
