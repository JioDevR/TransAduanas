package pe.sga.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEventoDTO {
    private Integer idEvento;
    private String codigoEstado;
    private String titulo;
    private String descripcion;
    private String regimen;
    private String usuarioNombre;
    private LocalDateTime fechaEvento;
}
