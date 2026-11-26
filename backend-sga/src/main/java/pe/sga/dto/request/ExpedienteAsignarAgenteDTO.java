package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteAsignarAgenteDTO {

    @NotNull(message = "El agente aduanero es obligatorio")
    private Integer idAgenteAduanero;
}
