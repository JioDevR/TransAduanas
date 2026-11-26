package pe.sga.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSistemaEstadoDTO {
    private String appNombre;
    private String appVersion;
    private String perfilActivo;
    private String javaVersion;
    private String osNombre;
    private long uptimeSegundos;
    private LocalDateTime timestamp;

    // Base de datos
    private boolean bdConectada;
    private String bdMotor;
    private String bdVersion;
    private long bdLatenciaMs;

    // Memoria JVM
    private long jvmHeapUsadoMb;
    private long jvmHeapMaximoMb;
    private int jvmHeapPorcentaje;

    // Almacenamiento local (uploads/documentos)
    private String storageDirectorio;
    private boolean storageDisponible;
    private long storageArchivosDisco;
    private long storageTamanoBytes;
    private long storageDocumentosRegistradosBd;
    private long discoEspacioLibreMb;
    private long discoEspacioTotalMb;
}
