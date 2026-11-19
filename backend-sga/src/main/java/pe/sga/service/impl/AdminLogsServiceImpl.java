package pe.sga.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.service.IAdminLogsService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class AdminLogsServiceImpl implements IAdminLogsService {

    @Value("${logging.file.path:./logs}")
    private String logPathConfig;

    private static final String APP_NAME = "sga-backend";

    private Path resolveLogPath(String tipo) {
        String logDir = (logPathConfig != null && !logPathConfig.isBlank()) ? logPathConfig : "./logs";
        String fileName = "error".equalsIgnoreCase(tipo) ? (APP_NAME + "-error.log") : (APP_NAME + ".log");
        return Paths.get(logDir).resolve(fileName).normalize();
    }

    @Override
    public Map<String, Object> getLogs(String tipo, int maxLines) {
        Path path = resolveLogPath(tipo);
        int limit = (maxLines <= 0) ? 200 : Math.min(maxLines, 2000);

        Map<String, Object> response = new HashMap<>();
        response.put("tipo", tipo);
        response.put("fileName", path.getFileName().toString());
        response.put("path", path.toAbsolutePath().toString());
        response.put("requestedLines", limit);
        response.put("timestamp", Instant.now().toString());

        if (!Files.exists(path)) {
            response.put("exists", false);
            response.put("sizeBytes", 0);
            response.put("totalLines", 0);
            response.put("lines", Collections.emptyList());
            return response;
        }

        try {
            long sizeBytes = Files.size(path);
            response.put("exists", true);
            response.put("sizeBytes", sizeBytes);

            // Leer las últimas 'limit' líneas eficientemente utilizando una cola circular (ArrayDeque)
            ArrayDeque<String> circularBuffer = new ArrayDeque<>(limit);
            long count = 0;
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    count++;
                    if (circularBuffer.size() == limit) {
                        circularBuffer.removeFirst();
                    }
                    circularBuffer.addLast(line);
                }
            }

            response.put("totalLines", count);
            response.put("returnedLines", circularBuffer.size());
            response.put("lines", new ArrayList<>(circularBuffer));
            return response;
        } catch (IOException e) {
            log.error("Error al leer archivo de logs {}: {}", path, e.getMessage());
            response.put("error", "No se pudo leer el archivo de log: " + e.getMessage());
            response.put("lines", Collections.emptyList());
            return response;
        }
    }

    @Override
    public Resource getLogFileResource(String tipo) {
        Path path = resolveLogPath(tipo);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("El archivo de log solicitado no existe en el servidor: " + path.getFileName());
        }
        return new FileSystemResource(path);
    }

    @Override
    public void clearLogFile(String tipo) {
        Path path = resolveLogPath(tipo);
        if (Files.exists(path)) {
            try {
                Files.write(path, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                log.info("Archivo de log vaciado correctamente: {}", path);
            } catch (IOException e) {
                log.error("Error al vaciar archivo de log {}: {}", path, e.getMessage());
                throw new RuntimeException("Error al vaciar archivo de log: " + e.getMessage());
            }
        }
    }
}
