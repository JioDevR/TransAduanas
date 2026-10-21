package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionPagoValidarDTO {

    @NotBlank(message = "La acción es obligatoria (APROBAR, OBSERVADO o RECHAZADO)")
    private String accion;

    private String observacionFinanzas;
}
