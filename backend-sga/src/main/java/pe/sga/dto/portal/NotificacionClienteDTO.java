package pe.sga.dto.portal;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionClienteDTO {

    private Integer idNotificacion;
    private Integer idCliente;
    private String titulo;
    private String mensaje;
    private String tipo;
    private Boolean leido;
    private String enlace;
    private LocalDateTime fechaCreacion;
}
