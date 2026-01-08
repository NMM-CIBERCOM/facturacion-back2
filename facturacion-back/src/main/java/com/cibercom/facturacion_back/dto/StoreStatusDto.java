package com.cibercom.facturacion_back.dto;

import java.time.Instant;

public class StoreStatusDto {
    private String id;
    private String name;
    private boolean online;
    private Instant lastSeen;

    public StoreStatusDto() {}

    public StoreStatusDto(String id, String name, boolean online, Instant lastSeen) {
        this.id = id;
        this.name = name;
        this.online = online;
        this.lastSeen = lastSeen;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
}
