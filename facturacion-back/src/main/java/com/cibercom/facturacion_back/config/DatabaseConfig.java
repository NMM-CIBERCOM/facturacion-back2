package com.cibercom.facturacion_back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.cibercom.facturacion_back.repository")
@EnableMongoRepositories(basePackages = "com.cibercom.facturacion_back.repository")
public class DatabaseConfig {
    
    // Esta configuración permite usar tanto JPA (Oracle) como MongoDB
    // El perfil activo determinará qué base de datos se usa por defecto
    
}
