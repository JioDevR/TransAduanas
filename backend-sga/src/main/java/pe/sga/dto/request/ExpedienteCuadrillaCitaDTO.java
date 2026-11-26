package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteCuadrillaCitaDTO {

    @NotBlank(message = "El detalle de la cuadrilla o personal de aforo es obligatorio")
    @Size(max = 200)
    private String cuadrillaDetalle;

    @NotNull(message = "La fecha y hora de la cita de inspección física es obligatoria")
    private LocalDateTime fechaCitaInspeccion;

    @Size(max = 50)
    private String codigoCitaInspeccion;

    @Size(max = 1000)
    private String observaciones;
}
