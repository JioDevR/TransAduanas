package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteDiligenciaDTO {

    @NotBlank(message = "El detalle de la diligencia o aforo físico es obligatorio")
    @Size(max = 2000, message = "El detalle de la inspección no puede exceder 2000 caracteres")
    private String detalleInspeccion;

    private String observaciones;
}
