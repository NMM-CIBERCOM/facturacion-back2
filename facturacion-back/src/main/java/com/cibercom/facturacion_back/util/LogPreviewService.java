package com.cibercom.facturacion_back.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.zip.GZIPInputStream;

public final class LogPreviewService {

    private LogPreviewService() {}

    public static String readHeadLines(Path file, int lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
                if (++count >= lines) break;
            }
        }
        return sb.toString();
    }

    public static String readTailLines(Path file, int lines) throws IOException {
        List<String> result = new ArrayList<>(Math.max(lines, 16));
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long pointer = raf.length() - 1;
            int found = 0;
            ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
            while (pointer >= 0) {
                raf.seek(pointer--);
                int b = raf.read();
                if (b == -1) break;
                if (b == '\n') {
                    result.add(reverseToString(buf));
                    buf.reset();
                    if (++found >= lines) break;
                } else {
                    buf.write(b);
                }
            }
            if (buf.size() > 0 && found < lines) {
                result.add(reverseToString(buf));
            }
        }
        Collections.reverse(result);
        StringBuilder sb = new StringBuilder();
        for (String s : result) {
            sb.append(s).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static String reverseToString(ByteArrayOutputStream baos) {
        byte[] arr = baos.toByteArray();
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
            byte tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr, StandardCharsets.UTF_8);
    }

    // Read last N lines from gzip (no pagination)
    public static List<String> readTailLinesFromGzip(Path file, int lines) throws IOException {
        Deque<String> deque = new ArrayDeque<>(lines + 1);
        try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(file));
             BufferedReader br = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                deque.addLast(line);
                if (deque.size() > lines) deque.removeFirst();
            }
        }
        return new ArrayList<>(deque);
    }

    // Paged tail reader (from EOF), skipping 'skip' lines then returning 'pageSize'
    public static class PagedResult {
        public final List<String> lines;
        public final boolean hasMore;
        public PagedResult(List<String> lines, boolean hasMore) {
            this.lines = lines;
            this.hasMore = hasMore;
        }
    }

    public static PagedResult readTailLinesPaged(Path file, int pageSize, int skip) throws IOException {
        List<String> result = new ArrayList<>(Math.max(pageSize, 16));
        boolean hasMore = false;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long pointer = raf.length() - 1;
            int found = 0;
            int skipped = 0;
            ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
            while (pointer >= 0) {
                raf.seek(pointer--);
                int b = raf.read();
                if (b == -1) break;
                if (b == '\n') {
                    if (skipped < skip) {
                        skipped++;
                        buf.reset();
                        continue;
                    }
                    if (found < pageSize) {
                        result.add(reverseToString(buf));
                        found++;
                        buf.reset();
                    } else {
                        hasMore = true;
                        break;
                    }
                } else {
                    buf.write(b);
                }
            }
            if (buf.size() > 0 && skipped >= skip && found < pageSize) {
                result.add(reverseToString(buf));
            }
        }
        Collections.reverse(result);
        return new PagedResult(result, hasMore);
    }
}
