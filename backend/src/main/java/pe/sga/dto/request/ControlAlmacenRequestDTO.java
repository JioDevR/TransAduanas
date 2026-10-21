package pe.sga.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlAlmacenRequestDTO {

    @NotNull(message = "El idExpediente es obligatorio")
    private Integer idExpediente;

    @NotBlank(message = "El depósito temporal es obligatorio")
    @Size(max = 100, message = "El depósito temporal no puede exceder 100 caracteres")
    private String depositoTemporal;

    @Size(max = 50, message = "La ubicación de bahía no puede exceder 50 caracteres")
    @Builder.Default
    private String ubicacionBahia = "Patio General";

    @Size(max = 30, message = "El tipo de ingreso no puede exceder 30 caracteres")
    @Builder.Default
    private String tipoIngreso = "DIRECTO_VERDE";

    @Size(max = 30, message = "La finalidad de almacenaje no puede exceder 30 caracteres")
    @Builder.Default
    private String finalidadAlmacenaje = "INGRESO_IMPORTACION";

    @Builder.Default
    private Integer diasLibresAlmacen = 15;

    private LocalDate fechaLimiteRetiro;

    @NotNull(message = "La cantidad de bultos es obligatoria")
    @Builder.Default
    private Integer cantidadBultos = 0;

    @NotNull(message = "El peso bruto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El peso bruto no puede ser negativo")
    @Builder.Default
    private BigDecimal pesoBrutoKg = BigDecimal.ZERO;

    @Size(max = 50, message = "El ticket VGM no puede exceder 50 caracteres")
    private String ticketPesajeVgm;

    @Builder.Default
    private Boolean precintoConforme = true;

    private String observaciones;
}
