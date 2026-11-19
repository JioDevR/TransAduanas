package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteSalidaOrigenDTO {

    @NotNull(message = "La fecha de zarpe/salida de origen es obligatoria")
    private LocalDate fechaZarpeReal;

    @Size(max = 100, message = "La nave o buque no puede exceder 100 caracteres")
    private String naveViaje;

    @Size(max = 30, message = "El contenedor no puede exceder 30 caracteres")
    private String numeroContenedor;

    @Size(max = 50, message = "El precinto no puede exceder 50 caracteres")
    private String precintoAduanero;

    private LocalDate fechaLlegadaEta;

    private String observaciones;
}
