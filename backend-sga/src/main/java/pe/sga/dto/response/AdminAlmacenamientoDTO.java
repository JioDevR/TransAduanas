package pe.sga.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAlmacenamientoDTO {

    private String directorioRaiz;
    private boolean disponible;
    private long discoTotalMb;
    private long discoLibreMb;
    private long discoUsadoMb;
    private int discoOcupacionPct;

    private long storageTamanoBytes;
    private long archivosEnDisco;
    private long registrosBdTotales;
    private long registrosBdActivos;
    private long registrosBdInactivos;
    private long bytesRegistradosBdActivos;

    private long archivosSincronizados;
    private long archivosFaltantesEnDisco;
    private long archivosHuerfanosEnDisco;
    private long bytesHuerfanosEnDisco;

    private List<GrupoStorageDTO> porEntidad;
    private List<GrupoStorageDTO> porCategoria;
    private List<GrupoStorageDTO> porExtension;

    private List<ArchivoStorageDTO> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GrupoStorageDTO {
        private String clave;
        private String etiqueta;
        private long cantidad;
        private long tamanoBytes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArchivoStorageDTO {
        private Integer idDocumento;
        private String nombreOriginal;
        private String rutaRelativa;
        private String extension;
        private String formatoMime;
        private String entidadTipo;
        private String categoria;
        private long tamanoBytes;
        private boolean activoBd;
        private String estadoIntegridad;
        private String usuarioCreador;
        private LocalDateTime fechaCreacion;
    }
}
