package com.cibercom.facturacion_back.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoggingConfigService {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoggingConfigService.class);
    private static String currentTarget = null;

    public static String configureFileAppender(String baseDir, String fileName) {
        try {
            Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
            
            // Crear directorio si no existe
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            
            if (!Files.isDirectory(basePath)) {
                throw new IllegalArgumentException("Base directory path is not a directory: " + baseDir);
            }
            
            Path logFile = basePath.resolve(fileName);
            String logFilePath = logFile.toString();
            
            // Configurar Logback programáticamente
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // Crear appender de archivo con rotación
            RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender = new RollingFileAppender<>();
            fileAppender.setContext(loggerContext);
            fileAppender.setName("FILE");
            fileAppender.setFile(logFilePath);
            
            // Configurar política de rotación por fecha y tamaño
            SizeAndTimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
            rollingPolicy.setContext(loggerContext);
            rollingPolicy.setParent(fileAppender);
            rollingPolicy.setFileNamePattern(logFilePath + ".%d{yyyy-MM-dd}.%i");
            rollingPolicy.setMaxFileSize(FileSize.valueOf("10MB"));
            rollingPolicy.setMaxHistory(7);
            rollingPolicy.setTotalSizeCap(FileSize.valueOf("200MB"));
            rollingPolicy.start();
            
            fileAppender.setRollingPolicy(rollingPolicy);
            
            // Configurar encoder
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            encoder.start();
            
            fileAppender.setEncoder(encoder);
            fileAppender.start();
            
            // Agregar appender al logger root
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(fileAppender);
            
            currentTarget = logFilePath;
            log.info("Logging configured to file: {}", logFilePath);
            
            return logFilePath;
            
        } catch (Exception e) {
            log.error("Failed to configure file logging", e);
            throw new RuntimeException("Failed to configure file logging: " + e.getMessage(), e);
        }
    }
    
    public static String getCurrentTarget() {
        return currentTarget;
    }
}
