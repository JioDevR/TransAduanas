package pe.sga.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.service.IStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageServiceImpl implements IStorageService {

    @Value("${app.storage.upload-dir:./uploads/documentos}")
    private String uploadDir;

    private Path rootLocation;

    @Override
    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            log.info("Directorio de almacenamiento de documentos inicializado en: {}", rootLocation);
        } catch (IOException e) {
            log.error("No se pudo inicializar el directorio de almacenamiento: {}", e.getMessage());
            throw new RuntimeException("No se pudo inicializar el almacenamiento de archivos", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        return store(file, null);
    }

    @Override
    public String store(MultipartFile file, String subpath) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No se puede almacenar un archivo vacío.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento");
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Nombre de archivo inválido: " + originalFilename);
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

        try {
            Path destinationDir = this.rootLocation;
            if (subpath != null && !subpath.isBlank()) {
                String cleanSubpath = StringUtils.cleanPath(subpath)
                        .replaceAll("^[\\\\/]+", "")
                        .replaceAll("[\\\\/]+$", "");
                if (cleanSubpath.contains("..")) {
                    throw new BadRequestException("Ruta de almacenamiento inválida: " + subpath);
                }
                destinationDir = this.rootLocation.resolve(cleanSubpath).normalize();
            }

            if (!destinationDir.startsWith(this.rootLocation)) {
                throw new BadRequestException("No se puede almacenar el archivo fuera del directorio designado.");
            }

            Files.createDirectories(destinationDir);

            Path destinationFile = destinationDir.resolve(storedFilename).normalize().toAbsolutePath();
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new BadRequestException("No se puede almacenar el archivo fuera del directorio designado.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativeStoredPath = this.rootLocation.relativize(destinationFile).toString().replace('\\', '/');
            log.info("Archivo almacenado con éxito: {} -> {}", originalFilename, relativeStoredPath);
            return relativeStoredPath;
        } catch (IOException e) {
            log.error("Error al guardar archivo en disco: {}", e.getMessage());
            throw new RuntimeException("Fallo al almacenar el archivo " + originalFilename, e);
        }
    }

    @Override
    public Resource loadAsResource(String pathOrFilename) {
        if (pathOrFilename == null || pathOrFilename.isBlank()) {
            throw new ResourceNotFoundException("Nombre o ruta de archivo no especificado.");
        }
        try {
            String clean = StringUtils.cleanPath(pathOrFilename).replaceAll("^[\\\\/]+", "");
            if (clean.contains("..")) {
                throw new BadRequestException("Ruta de archivo no permitida.");
            }

            // 1. Intentar resolver ruta relativa estructurada
            Path file = rootLocation.resolve(clean).normalize();
            if (file.startsWith(rootLocation)) {
                Resource resource = new UrlResource(file.toUri());
                if (resource.exists() && resource.isReadable()) {
                    return resource;
                }
            }

            // 2. Fallback de compatibilidad: buscar archivo directamente por nombre base en la raíz
            String baseFilename = Paths.get(clean).getFileName().toString();
            Path flatFile = rootLocation.resolve(baseFilename).normalize();
            if (flatFile.startsWith(rootLocation)) {
                Resource flatResource = new UrlResource(flatFile.toUri());
                if (flatResource.exists() && flatResource.isReadable()) {
                    return flatResource;
                }
            }

            throw new ResourceNotFoundException("No se pudo leer el archivo: " + pathOrFilename);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Ruta de archivo no válida: " + pathOrFilename);
        }
    }

    @Override
    public void delete(String pathOrFilename) {
        if (pathOrFilename == null || pathOrFilename.isBlank()) return;
        try {
            String clean = StringUtils.cleanPath(pathOrFilename).replaceAll("^[\\\\/]+", "");
            if (clean.contains("..")) return;

            Path file = rootLocation.resolve(clean).normalize();
            if (file.startsWith(rootLocation) && Files.deleteIfExists(file)) {
                log.info("Archivo eliminado físicamente: {}", pathOrFilename);
                return;
            }

            String baseFilename = Paths.get(clean).getFileName().toString();
            Path flatFile = rootLocation.resolve(baseFilename).normalize();
            if (flatFile.startsWith(rootLocation) && Files.deleteIfExists(flatFile)) {
                log.info("Archivo eliminado físicamente (fallback plano): {}", baseFilename);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo físico: {}", pathOrFilename);
        }
    }
}
