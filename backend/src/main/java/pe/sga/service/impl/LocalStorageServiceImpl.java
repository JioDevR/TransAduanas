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
        if (file.isEmpty()) {
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
            Path destinationFile = this.rootLocation.resolve(storedFilename).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new BadRequestException("No se puede almacenar el archivo fuera del directorio designado.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Archivo almacenado con éxito: {} -> {}", originalFilename, storedFilename);
            return storedFilename;
        } catch (IOException e) {
            log.error("Error al guardar archivo en disco: {}", e.getMessage());
            throw new RuntimeException("Fallo al almacenar el archivo " + originalFilename, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("No se pudo leer el archivo: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Ruta de archivo no válida: " + filename);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Files.deleteIfExists(file);
            log.info("Archivo eliminado físicamente: {}", filename);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo físico: {}", filename);
        }
    }
}
