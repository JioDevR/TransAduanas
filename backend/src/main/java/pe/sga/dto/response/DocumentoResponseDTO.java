package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.CategoriaDocumento;
import pe.sga.entity.enums.EntidadDocumento;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoResponseDTO {

    private Integer idDocumento;
    private String nombreOriginal;
    private String nombreAlmacenado;
    private String formatoMime;
    private String extension;
    private Long tamanoBytes;
    private CategoriaDocumento categoria;
    private String descripcion;
    private EntidadDocumento entidadTipo;
    private Integer idCotizacion;
    private String codigoCotizacion;
    private Integer idExpediente;
    private String codigoExpediente;
    private Integer idCliente;
    private String clienteRazonSocial;
    private Integer idUsuarioCreador;
    private String usuarioCreadorNombre;
    private LocalDateTime fechaCreacion;
}
