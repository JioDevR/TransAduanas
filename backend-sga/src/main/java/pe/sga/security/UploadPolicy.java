package pe.sga.security;

import org.springframework.web.multipart.MultipartFile;
import pe.sga.exception.BadRequestException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Lista de formatos documentales. Las firmas son una comprobación básica, no un antivirus. */
public final class UploadPolicy {
    private UploadPolicy() { }
    private static final Map<String, String> MIME = Map.ofEntries(
            Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"),
            Map.entry("pdf", "application/pdf"), Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("doc", "application/msword"), Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    public static String mime(MultipartFile file) {
        String name = file.getOriginalFilename();
        return MIME.get(name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT));
    }

    public static void validateAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) throw new BadRequestException("Debe adjuntar al menos un archivo");
        for (MultipartFile file : files) validate(file);
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("No se permiten archivos vacíos");
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || name.length() > 200 || name.contains("/") ||
                name.contains("\\") || name.contains("..") || name.contains(":") ||
                name.chars().anyMatch(c -> Character.isISOControl(c) || c == '"')) {
            throw new BadRequestException("Nombre de archivo no permitido");
        }
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String expected = MIME.get(extension);
        if (expected == null) throw new BadRequestException("Formato de archivo no permitido");
        String declared = file.getContentType();
        if (declared == null) declared = "application/octet-stream";
        declared = declared.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean csvAlias = extension.equals("csv") && Set.of("text/plain", "application/vnd.ms-excel").contains(declared);
        if (!declared.equals(expected) && !declared.equals("application/octet-stream") && !csvAlias) {
            throw new BadRequestException("El tipo declarado no corresponde al formato del archivo");
        }
        try (var stream = file.getInputStream()) {
            byte[] prefix = stream.readNBytes(8);
            boolean valid = switch (extension) {
                case "pdf" -> starts(prefix, 0x25, 0x50, 0x44, 0x46, 0x2d);
                case "png" -> starts(prefix, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
                case "jpg", "jpeg" -> starts(prefix, 0xff, 0xd8, 0xff);
                case "doc", "xls" -> starts(prefix, 0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1);
                case "docx", "xlsx" -> starts(prefix, 0x50, 0x4b, 0x03, 0x04);
                default -> true; // TXT/CSV se sirven con un MIME de texto fijo.
            };
            if (!valid) throw new BadRequestException("El contenido no corresponde al formato del archivo");
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el archivo adjunto");
        }
    }

    private static boolean starts(byte[] prefix, int... signature) {
        if (prefix.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if ((prefix[i] & 255) != signature[i]) return false;
        return true;
    }
}
