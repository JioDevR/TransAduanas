package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEmbarqueExportacionDTO {

    @NotBlank(message = "El nombre de la nave y número de viaje es obligatorio")
    @Size(max = 100, message = "La nave no puede exceder 100 caracteres")
    private String naveViaje;

    @Size(max = 30, message = "El tipo de transporte internacional no puede exceder 30 caracteres")
    private String tipoTransporteInternacional;

    @Size(max = 60, message = "El número de vuelo no puede exceder 60 caracteres")
    private String numeroVuelo;

    private LocalDate fechaZarpeEtd;

    private LocalDate fechaEstimadaLlegadaExterior;

    @Size(max = 100, message = "El puerto de destino exterior no puede exceder 100 caracteres")
    private String puertoDestinoExterior;

    private String observaciones;
}
