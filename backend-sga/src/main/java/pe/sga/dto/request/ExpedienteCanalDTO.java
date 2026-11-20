package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import pe.sga.entity.enums.CanalSunat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteCanalDTO {

    @NotBlank(message = "El número de DAM es obligatorio")
    @Size(max = 30, message = "El número de DAM no puede exceder 30 caracteres")
    private String numeroDam;

    @NotNull(message = "El canal asignado por SUNAT es obligatorio (VERDE, NARANJA, ROJO)")
    private CanalSunat canalSunat;

    private LocalDateTime fechaAsignacionCanal;

    private String detalleInspeccion;
}
