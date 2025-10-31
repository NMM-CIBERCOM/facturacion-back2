package com.cibercom.facturacion_back.dto;

public class PantallaConfigDto {
    private Integer idConfig;
    private Integer idPerfil;
    private String menuLabel;
    private String menuPath;
    private Boolean isVisible;
    private Integer orden;
    private String parentLabel;

    // Constructores
    public PantallaConfigDto() {}

    public PantallaConfigDto(Integer idConfig, Integer idPerfil, String menuLabel, String menuPath, 
                           Boolean isVisible, Integer orden, String parentLabel) {
        this.idConfig = idConfig;
        this.idPerfil = idPerfil;
        this.menuLabel = menuLabel;
        this.menuPath = menuPath;
        this.isVisible = isVisible;
        this.orden = orden;
        this.parentLabel = parentLabel;
    }

    // Getters y Setters
    public Integer getIdConfig() {
        return idConfig;
    }

    public void setIdConfig(Integer idConfig) {
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

    public String getParentLabel() {
        return parentLabel;
    }

    public void setParentLabel(String parentLabel) {
        this.parentLabel = parentLabel;
    }

    // Métodos de conveniencia
    public boolean isVisible() {
        return isVisible != null && isVisible;
    }

    @Override
    public String toString() {
        return "PantallaConfigDto{" +
                "idConfig=" + idConfig +
                ", idPerfil=" + idPerfil +
                ", menuLabel='" + menuLabel + '\'' +
                ", menuPath='" + menuPath + '\'' +
                ", isVisible=" + isVisible +
                ", orden=" + orden +
                ", parentLabel='" + parentLabel + '\'' +
                '}';
    }
}
