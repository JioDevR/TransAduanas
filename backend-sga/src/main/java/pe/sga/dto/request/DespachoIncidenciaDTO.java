package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespachoIncidenciaDTO {

    @NotBlank(message = "El detalle de la incidencia es obligatorio")
    @Size(max = 1000, message = "El detalle de la incidencia no puede exceder 1000 caracteres")
    private String incidencia;
}
