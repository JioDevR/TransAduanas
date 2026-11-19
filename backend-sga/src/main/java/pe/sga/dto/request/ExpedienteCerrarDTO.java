package pe.sga.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteCerrarDTO {

    @Size(max = 1000, message = "Las observaciones de cierre no pueden exceder 1000 caracteres")
    private String observacionesCierre;
}
