package com.cibercom.facturacion_back.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import java.time.Instant;
import java.time.Duration;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cibercom.facturacion_back.util.LoggingConfigService;

import com.cibercom.facturacion_back.util.LogPreviewService;
import com.cibercom.facturacion_back.dto.StoreStatusDto;

@RestController
@RequestMapping("/api/logs")
public class LogPreviewController {
    private static final Logger log = LoggerFactory.getLogger(LogPreviewController.class);

    @Value("${app.logs.base-dir:}")
    private String baseDir;

    @GetMapping(value = "/config", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> config() {
        String yml = (baseDir == null) ? "<null>" : baseDir;
        String sys = System.getProperty("LOGS_BASE_DIR");
        String env = System.getenv("LOGS_BASE_DIR");
        String effective = resolveBaseDir();
        String body = String.join("\n",
                "yml=" + yml,
                "sysprop=" + (sys == null ? "<null>" : sys),
                "env=" + (env == null ? "<null>" : env),
                "effective=" + (effective == null ? "<null>" : effective)
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/preview", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> preview(
            @RequestParam("file") String file,
            @RequestParam(value = "mode", defaultValue = "tail") String mode,
            @RequestParam(value = "lines", defaultValue = "200") int lines,
            @RequestParam(value = "baseDir", required = false) String baseDirOverride
    ) throws IOException {
        String effectiveBase = resolveBaseDir(baseDirOverride);
        if (effectiveBase == null || effectiveBase.isBlank() || "<null>".equals(effectiveBase)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body("Log preview not configured. Set app.logs.base-dir or LOGS_BASE_DIR env var.");
        }

        // Clamp lines to a safe range
        if (lines < 1) lines = 1;
        if (lines > 1000) lines = 1000;

        try {
            Path base = Paths.get(effectiveBase).toAbsolutePath().normalize();
            Path target = base.resolve(file).normalize();

            // Prevent path traversal
            if (!target.startsWith(base)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid path");
            }

            if (!Files.exists(target) || !Files.isRegularFile(target)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            String text;
            if ("head".equalsIgnoreCase(mode)) {
                text = LogPreviewService.readHeadLines(target, lines);
            } else {
                text = LogPreviewService.readTailLines(target, lines);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(text);
        } catch (java.nio.file.InvalidPathException e) {
            log.error("Invalid path in preview: {}", effectiveBase, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid base directory path: " + effectiveBase);
        }
    }

    @GetMapping(value = "/previewPage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> previewPage(
            @RequestParam("file") String file,
            @RequestParam(value = "lines", defaultValue = "100") int lines,
            @RequestParam(value = "skip", defaultValue = "0") int skip,
            @RequestParam(value = "baseDir", required = false) String baseDirOverride
    ) throws IOException {
        if (lines < 1) lines = 1;
        if (lines > 1000) lines = 1000;
        if (skip < 0) skip = 0;

        String effectiveBase = resolveBaseDir(baseDirOverride);
        if (effectiveBase == null || effectiveBase.isBlank() || "<null>".equals(effectiveBase)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body(Map.of("error", "Log preview not configured"));
        }
        try {
            Path base = Paths.get(effectiveBase).toAbsolutePath().normalize();
            Path target = base.resolve(file).normalize();
            if (!target.startsWith(base)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid path"));
            }
            if (!Files.exists(target) || !Files.isRegularFile(target)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "File not found"));
            }

            Map<String, Object> body = new HashMap<>();
            if (target.getFileName().toString().endsWith(".gz")) {
                if (skip > 0) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .body(Map.of("error", "Pagination not supported for .gz files. Use first page only."));
                }
                List<String> last = LogPreviewService.readTailLinesFromGzip(target, lines);
                body.put("lines", last);
                body.put("hasMore", false);
                body.put("skip", 0);
                body.put("pageSize", lines);
                return ResponseEntity.ok(body);
            } else {
                LogPreviewService.PagedResult res = LogPreviewService.readTailLinesPaged(target, lines, skip);
                body.put("lines", res.lines);
                body.put("hasMore", res.hasMore);
                body.put("skip", skip);
                body.put("pageSize", lines);
                return ResponseEntity.ok(body);
            }
        } catch (java.nio.file.InvalidPathException e) {
            log.error("Invalid path in previewPage: {}", effectiveBase, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid base directory path: " + effectiveBase));
        }
    }

    @Value("${app.logs.allow-request-base:true}")
    private boolean allowRequestBase;

    private String resolveBaseDir(String baseOverride) {
        if (allowRequestBase && baseOverride != null && !baseOverride.isBlank()) {
            // Validar que no sea "<null>" o contenga caracteres inválidos
            if ("<null>".equals(baseOverride) || baseOverride.contains("<") || baseOverride.contains(">")) {
                return null;
            }
            try {
                Path p = Paths.get(baseOverride).toAbsolutePath().normalize();
                if (Files.exists(p) && Files.isDirectory(p)) {
                    return p.toString();
                }
            } catch (Exception e) {
                log.warn("Invalid baseDir override: {}", baseOverride, e);
                return null;
            }
        }
        String val = baseDir;
        // Validar que baseDir no sea "<null>" o contenga caracteres inválidos
        if (val != null && ("<null>".equals(val) || val.contains("<") || val.contains(">"))) {
            val = null;
        }
        if (val == null || val.isBlank()) {
            val = System.getProperty("LOGS_BASE_DIR");
            // Validar system property
            if (val != null && ("<null>".equals(val) || val.contains("<") || val.contains(">"))) {
                val = null;
            }
        }
        if (val == null || val.isBlank()) {
            val = System.getenv("LOGS_BASE_DIR");
            // Validar environment variable
            if (val != null && ("<null>".equals(val) || val.contains("<") || val.contains(">"))) {
                val = null;
            }
        }
        return val;
    }

    // Backwards-compatible no-arg resolver used by /config endpoint
    private String resolveBaseDir() {
        return resolveBaseDir(null);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("file") String file,
                                             @RequestParam(value = "baseDir", required = false) String baseDirOverride) throws IOException {
        String effectiveBase = resolveBaseDir(baseDirOverride);
        if (effectiveBase == null || effectiveBase.isBlank() || "<null>".equals(effectiveBase)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build();
        }

        try {
            Path base = Paths.get(effectiveBase).toAbsolutePath().normalize();
        Path target = base.resolve(file).normalize();
        if (!target.startsWith(base)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Resource resource;
        try {
            resource = new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        String contentType = Files.probeContentType(target);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

            String filename = target.getFileName().toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(Files.size(target))
                    .body(resource);
        } catch (java.nio.file.InvalidPathException e) {
            log.error("Invalid path in download: {}", effectiveBase, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/browse")
    public ResponseEntity<Map<String, Object>> browse(
            @RequestParam(value = "baseDir", required = false) String baseDirOverride,
            @RequestParam(value = "subPath", required = false, defaultValue = "") String subPath
    ) throws IOException {
        String effectiveBase = resolveBaseDir(baseDirOverride);
        if (effectiveBase == null || effectiveBase.isBlank() || "<null>".equals(effectiveBase)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(Map.of("error", "Base dir not configured"));
        }
        try {
            Path base = Paths.get(effectiveBase).toAbsolutePath().normalize();
        Path current = base.resolve(subPath).normalize();
        if (!current.startsWith(base)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid path"));
        }
        if (!Files.exists(current) || !Files.isDirectory(current)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Directory not found"));
        }

        List<Map<String, Object>> dirs = new ArrayList<>();
        try (var stream = Files.list(current)) {
            stream.filter(Files::isDirectory)
                    .sorted()
                    .forEach(p -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", p.getFileName().toString());
                        m.put("relative", base.relativize(p).toString().replace('\\', '/'));
                        dirs.add(m);
                    });
        }
        String rel = base.relativize(current).toString().replace('\\', '/');
        String parentRel = current.equals(base) ? null : base.relativize(current.getParent()).toString().replace('\\', '/');
        Map<String, Object> body = new HashMap<>();
        body.put("base", base.toString());
        body.put("current", current.toString());
        body.put("relative", rel);
            body.put("parent", parentRel);
            body.put("directories", dirs);
            return ResponseEntity.ok(body);
        } catch (java.nio.file.InvalidPathException e) {
            log.error("Invalid path in browse: {}", effectiveBase, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid base directory path: " + effectiveBase));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(value = "baseDir", required = false) String baseDirOverride,
            @RequestParam(value = "subPath", required = false, defaultValue = "") String subPath
    ) throws IOException {
        String effectiveBase = resolveBaseDir(baseDirOverride);
        if (effectiveBase == null || effectiveBase.isBlank() || "<null>".equals(effectiveBase)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(Map.of("error", "Base dir not configured"));
        }
        try {
            Path base = Paths.get(effectiveBase).toAbsolutePath().normalize();
        Path current = base.resolve(subPath).normalize();
        if (!current.startsWith(base)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid path"));
        }
        if (!Files.exists(current) || !Files.isDirectory(current)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Directory not found"));
        }

        List<Map<String, Object>> filesList = new ArrayList<>();
        try (var stream = Files.list(current)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        try {
                            Map<String, Object> m = new HashMap<>();
                            m.put("name", p.getFileName().toString());
                            m.put("relative", base.relativize(p).toString().replace('\\', '/'));
                            m.put("size", Files.size(p));
                            m.put("modified", Files.getLastModifiedTime(p).toMillis());
                            filesList.add(m);
                        } catch (IOException ignore) { }
                    });
        }
        filesList.sort(Comparator.comparingLong(o -> (Long) o.get("modified")));
        // Most recent first
        java.util.Collections.reverse(filesList);

        Map<String, Object> body = new HashMap<>();
        body.put("base", base.toString());
        body.put("current", current.toString());
            body.put("relative", base.relativize(current).toString().replace('\\', '/'));
            body.put("total", filesList.size());
            body.put("files", filesList);
            return ResponseEntity.ok(body);
        } catch (java.nio.file.InvalidPathException e) {
            log.error("Invalid path in list: {}", effectiveBase, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid base directory path: " + effectiveBase));
        }
    }

    @GetMapping("/pick-dir")
    public ResponseEntity<Map<String, String>> pickDir(
            @RequestParam(value = "start", required = false) String start
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Server is headless; UI picker not available"));
        }

        AtomicReference<Path> chosen = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser chooser = new JFileChooser(start != null && !start.isBlank() ? new java.io.File(start) : null);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Seleccionar carpeta de logs");
                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                    chosen.set(chooser.getSelectedFile().toPath().toAbsolutePath().normalize());
                }
            });
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to open native picker"));
        }
        if (chosen.get() == null) {
            return ResponseEntity.noContent().build();
        }
        log.info("Directory chosen via picker: {}", chosen.get());
        return ResponseEntity.ok(Map.of("path", chosen.get().toString()));
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("Test log line from /api/logs/ping");
        return ResponseEntity.ok("ok");
    }

    // --- Monitor tiendas: mock simple de estados para front ---
    private static final List<StoreStatusDto> MOCK_STORES = List.of(
            new StoreStatusDto("S001", "Tienda Centro", true, Instant.now()),
            new StoreStatusDto("S002", "Tienda Norte", true, Instant.now()),
            new StoreStatusDto("S003", "Tienda Sur", false, Instant.now()),
            new StoreStatusDto("S004", "Tienda Este", true, Instant.now()),
            new StoreStatusDto("S005", "Tienda Oeste", false, Instant.now())
    );
    private static final SecureRandom RANDOM = new SecureRandom();

    @GetMapping("/stores/status")
    public ResponseEntity<Map<String, Object>> getStoreStatus(
            @RequestParam(value = "simulate", defaultValue = "true") boolean simulate,
            @RequestParam(value = "thresholdMinutes", defaultValue = "10") long thresholdMinutes
    ) {
        Instant now = Instant.now();
        Duration threshold = Duration.ofMinutes(Math.max(1, Math.min(thresholdMinutes, 60)));

        List<StoreStatusDto> snapshot = new ArrayList<>();
        for (StoreStatusDto store : MOCK_STORES) {
            boolean online = store.isOnline();
            Instant lastSeen = now.minus(Duration.ofMinutes(5));
            if (simulate) {
                // 70% prob de estar en línea y lastSeen en los últimos 15 minutos
                online = RANDOM.nextDouble() > 0.3;
                long minutesAgo = 1 + RANDOM.nextInt(15);
                lastSeen = now.minus(Duration.ofMinutes(minutesAgo));
            }
            boolean withinThreshold = lastSeen.isAfter(now.minus(threshold));
            snapshot.add(new StoreStatusDto(store.getId(), store.getName(), online && withinThreshold, lastSeen));
        }

        List<StoreStatusDto> onlineList = new ArrayList<>();
        List<StoreStatusDto> offlineList = new ArrayList<>();
        for (StoreStatusDto s : snapshot) {
            if (s.isOnline()) {
                onlineList.add(s);
            } else {
                offlineList.add(s);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("online", onlineList);
        body.put("offline", offlineList);
        body.put("counts", Map.of(
                "online", onlineList.size(),
                "offline", offlineList.size(),
                "total", snapshot.size()
        ));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/log-target")
    public ResponseEntity<Map<String, String>> getLogTarget() {
        try {
            String t = LoggingConfigService.getCurrentTarget();
            // Asegurarse de que nunca devolvamos "<null>" como string válido
            if (t == null || t.isBlank() || "<null>".equals(t)) {
                return ResponseEntity.ok(Map.of("target", "<unknown>"));
            }
            return ResponseEntity.ok(Map.of("target", t));
        } catch (Exception e) {
            log.warn("Error al obtener log target: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("target", "<unknown>"));
        }
    }

    @PostMapping(value = "/set-log-dir", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> setLogDir(
            @RequestParam("baseDir") String baseDir,
            @RequestParam(value = "fileName", defaultValue = "server.log") String fileName
    ) {
        try {
            // Validar que baseDir no sea null, vacío, o la cadena literal "<null>"
            if (baseDir == null || baseDir.isBlank() || "<null>".equals(baseDir)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", 
                    "baseDir no está configurado. Configure app.logs.base-dir o la variable de entorno LOGS_BASE_DIR",
                    "details", 
                    "invalid_base_dir"
                ));
            }
            
            // Validar que no contenga caracteres inválidos
            if (baseDir.contains("<") || baseDir.contains(">")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "baseDir contiene caracteres inválidos: " + baseDir,
                    "details",
                    "invalid_chars"
                ));
            }
            
            String path = LoggingConfigService.configureFileAppender(baseDir, fileName);
            log.info("Reconfigured logging to {}", path);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "details", "bad_request"));
        } catch (Exception e) {
            log.error("Error al configurar logging", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to configure logging", "details", e.getClass().getSimpleName()));
        }
    }

    // Dev fallback via GET (útil si algún proxy bloquea POST en tu entorno dev)
    @GetMapping(value = "/set-log-dir", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> setLogDirGet(
            @RequestParam("baseDir") String baseDir,
            @RequestParam(value = "fileName", defaultValue = "server.log") String fileName
    ) {
        return setLogDir(baseDir, fileName);
    }
}
