package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.sga.entity.enums.EstadoCotizacion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionRevisionDTO {

    @NotNull(message = "El id del agente revisor es obligatorio")
    private Integer idAgenteRevisor;

    @NotNull(message = "El nuevo estado de revisión es obligatorio (APROBADA, OBSERVADA, RECHAZADA)")
    private EstadoCotizacion estado;

    private String motivoObservacion;

    private String observaciones;
}
