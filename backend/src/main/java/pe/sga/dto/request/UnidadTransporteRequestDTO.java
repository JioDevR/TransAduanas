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
public class UnidadTransporteRequestDTO {

    @NotBlank(message = "La placa de tracto es obligatoria")
    @Size(max = 15, message = "La placa no puede exceder 15 caracteres")
    private String placaTracto;

    @Size(max = 15, message = "La placa de carreta no puede exceder 15 caracteres")
    private String placaCarreta;

    @NotBlank(message = "La marca y modelo son obligatorios")
    @Size(max = 100, message = "La marca/modelo no puede exceder 100 caracteres")
    private String marcaModelo;

    private Integer anoFabricacion;

    @Builder.Default
    @Size(max = 50, message = "El tipo de vehículo no puede exceder 50 caracteres")
    private String tipoVehiculo = "TRACTO_PLATAFORMA";

    @NotNull(message = "La capacidad de carga es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La capacidad de carga debe ser mayor a 0")
    @Builder.Default
    private BigDecimal capacidadCargaKg = new BigDecimal("30000.00");

    private LocalDate soatVencimiento;

    private LocalDate citvVencimiento;

    @Builder.Default
    private Integer kilometrajeActual = 0;

    @Builder.Default
    @Size(max = 20, message = "El estado operativo no puede exceder 20 caracteres")
    private String estadoOperativo = "DISPONIBLE";

    @Builder.Default
    private Boolean estadoActivo = true;
}
