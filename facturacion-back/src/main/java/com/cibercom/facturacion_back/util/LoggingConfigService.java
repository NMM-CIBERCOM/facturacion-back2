package com.cibercom.facturacion_back.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoggingConfigService {
    private static final Logger log = LoggerFactory.getLogger(LoggingConfigService.class);

    /**
     * Obtiene el target actual del logging (ruta del archivo de log)
     */
    public static String getCurrentTarget() {
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            
            // Buscar un FileAppender en el root logger
            var appender = rootLogger.getAppender("FILE");
            if (appender instanceof FileAppender) {
                FileAppender<?> fileAppender = (FileAppender<?>) appender;
                return fileAppender.getFile();
            }
            
            // Buscar en todos los appenders
            var iterator = rootLogger.iteratorForAppenders();
            while (iterator.hasNext()) {
                var app = iterator.next();
                if (app instanceof FileAppender) {
                    FileAppender<?> fileAppender = (FileAppender<?>) app;
                    return fileAppender.getFile();
                }
            }
            
            return null;
        } catch (Exception e) {
            log.warn("No se pudo obtener el target actual del logging: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Configura un FileAppender para escribir logs en el directorio y archivo especificados
     */
    public static String configureFileAppender(String baseDir, String fileName) {
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalArgumentException("baseDir no puede estar vacío");
        }
        // Validar que no sea la cadena literal "<null>"
        if ("<null>".equals(baseDir)) {
            throw new IllegalArgumentException("baseDir no está configurado. Configure app.logs.base-dir o la variable de entorno LOGS_BASE_DIR");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName no puede estar vacío");
        }

        try {
            // Validar que el path no contenga caracteres inválidos
            if (baseDir.contains("<") || baseDir.contains(">")) {
                throw new IllegalArgumentException("baseDir contiene caracteres inválidos: " + baseDir);
            }
            Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            if (!Files.isDirectory(basePath)) {
                throw new IllegalArgumentException("baseDir debe ser un directorio: " + baseDir);
            }

            Path logFile = basePath.resolve(fileName).normalize();
            String logFilePath = logFile.toString();

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

            // Remover el appender FILE existente si existe
            var existingAppender = rootLogger.getAppender("FILE");
            if (existingAppender != null) {
                rootLogger.detachAppender(existingAppender);
            }

            // Crear nuevo RollingFileAppender
            RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender = new RollingFileAppender<>();
            fileAppender.setContext(context);
            fileAppender.setName("FILE");
            fileAppender.setFile(logFilePath);

            // Configurar encoder
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            encoder.start();
            fileAppender.setEncoder(encoder);

            // Configurar política de rotación
            TimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setContext(context);
            rollingPolicy.setParent(fileAppender);
            rollingPolicy.setFileNamePattern(logFilePath + ".%d{yyyy-MM-dd}.%i.gz");
            rollingPolicy.setMaxHistory(30); // Mantener 30 días
            rollingPolicy.setTotalSizeCap(FileSize.valueOf("10GB")); // Máximo 10GB
            rollingPolicy.start();
            fileAppender.setRollingPolicy(rollingPolicy);

            fileAppender.start();

            // Agregar al root logger
            rootLogger.addAppender(fileAppender);
            rootLogger.setLevel(ch.qos.logback.classic.Level.INFO);

            log.info("Logging configurado para escribir en: {}", logFilePath);
            return logFilePath;
        } catch (Exception e) {
            log.error("Error al configurar FileAppender: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo configurar el logging: " + e.getMessage(), e);
        }
    }
}
