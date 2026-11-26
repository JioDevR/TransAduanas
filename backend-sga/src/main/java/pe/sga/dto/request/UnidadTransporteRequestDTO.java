package pe.sga.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import pe.sga.entity.enums.EstadoOperativoFlota;
import pe.sga.entity.enums.TipoVehiculo;

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

    @Min(value = 1950, message = "El año de fabricación debe ser mayor o igual a 1950")
    @Max(value = 2100, message = "El año de fabricación no es válido")
    private Integer anoFabricacion;

    @Builder.Default
    private TipoVehiculo tipoVehiculo = TipoVehiculo.TRACTO;

    @NotNull(message = "La capacidad de carga es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La capacidad de carga debe ser mayor a 0")
    @Builder.Default
    private BigDecimal capacidadCargaKg = new BigDecimal("30000.00");

    private LocalDate soatVencimiento;

    private LocalDate citvVencimiento;

    @Min(value = 0, message = "El kilometraje actual no puede ser negativo")
    @Builder.Default
    private Integer kilometrajeActual = 0;

    @Builder.Default
    private EstadoOperativoFlota estadoOperativo = EstadoOperativoFlota.DISPONIBLE;

    @Builder.Default
    private Boolean estadoActivo = true;

    private String observaciones;
}
