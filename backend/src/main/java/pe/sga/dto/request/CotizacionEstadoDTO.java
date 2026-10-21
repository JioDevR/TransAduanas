package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.sga.entity.enums.EstadoCotizacion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionEstadoDTO {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoCotizacion estado;

    private String motivoObservacion;
}
