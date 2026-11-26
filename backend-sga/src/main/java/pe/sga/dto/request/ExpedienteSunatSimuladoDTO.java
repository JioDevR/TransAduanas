package pe.sga.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteSunatSimuladoDTO {

    @Size(max = 1000)
    private String documentosIncluidos;

    @Size(max = 1000)
    private String observaciones;
}
