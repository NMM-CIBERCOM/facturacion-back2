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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cibercom.facturacion_back.util.LoggingConfigService;

import com.cibercom.facturacion_back.util.LogPreviewService;

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
        if (effectiveBase == null || effectiveBase.isBlank()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body("Log preview not configured. Set app.logs.base-dir or LOGS_BASE_DIR env var.");
        }

        // Clamp lines to a safe range
        if (lines < 1) lines = 1;
        if (lines > 1000) lines = 1000;

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
        if (effectiveBase == null || effectiveBase.isBlank()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body(Map.of("error", "Log preview not configured"));
        }
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
    }

    @Value("${app.logs.allow-request-base:true}")
    private boolean allowRequestBase;

    private String resolveBaseDir(String baseOverride) {
        if (allowRequestBase && baseOverride != null && !baseOverride.isBlank()) {
            Path p = Paths.get(baseOverride).toAbsolutePath().normalize();
            if (Files.exists(p) && Files.isDirectory(p)) {
                return p.toString();
            }
        }
        String val = baseDir;
        if (val == null || val.isBlank()) {
            val = System.getProperty("LOGS_BASE_DIR");
        }
        if (val == null || val.isBlank()) {
            val = System.getenv("LOGS_BASE_DIR");
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
        if (effectiveBase == null || effectiveBase.isBlank()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build();
        }

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
    }

    @GetMapping("/browse")
    public ResponseEntity<Map<String, Object>> browse(
            @RequestParam(value = "baseDir", required = false) String baseDirOverride,
            @RequestParam(value = "subPath", required = false, defaultValue = "") String subPath
    ) throws IOException {
        String effectiveBase = resolveBaseDir(baseDirOverride);
        if (effectiveBase == null || effectiveBase.isBlank()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(Map.of("error", "Base dir not configured"));
        }
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
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(value = "baseDir", required = false) String baseDirOverride,
            @RequestParam(value = "subPath", required = false, defaultValue = "") String subPath
    ) throws IOException {
        String effectiveBase = resolveBaseDir(baseDirOverride);
        if (effectiveBase == null || effectiveBase.isBlank()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(Map.of("error", "Base dir not configured"));
        }
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

    @GetMapping("/log-target")
    public ResponseEntity<Map<String, String>> getLogTarget() {
        String t = LoggingConfigService.getCurrentTarget();
        return ResponseEntity.ok(Map.of("target", t == null ? "<unknown>" : t));
    }

    @PostMapping(value = "/set-log-dir", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> setLogDir(
            @RequestParam("baseDir") String baseDir,
            @RequestParam(value = "fileName", defaultValue = "server.log") String fileName
    ) {
        try {
            String path = LoggingConfigService.configureFileAppender(baseDir, fileName);
            log.info("Reconfigured logging to {}", path);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "details", "bad_request"));
        } catch (Exception e) {
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
