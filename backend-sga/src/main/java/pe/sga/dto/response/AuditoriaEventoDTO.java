package pe.sga.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEventoDTO {
    private Long idEvento;
    private LocalDateTime fecha;
    private String usuarioActor;
    private String rolActor;
    private String modulo;
    private String accion;
    private String entidad;
    private String idEntidad;
    private String detalle;
    private String ipOrigen;
    private String resultado;
}
