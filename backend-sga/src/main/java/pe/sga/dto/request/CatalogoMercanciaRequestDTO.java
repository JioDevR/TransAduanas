package pe.sga.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoMercanciaRequestDTO {

    @NotBlank(message = "El código de la mercancía es obligatorio")
    @Size(max = 30, message = "El código no puede exceder 30 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre de la mercancía es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 80, message = "La categoría no puede exceder 80 caracteres")
    private String categoria;

    @Builder.Default
    private Boolean requierePermiso = false;

    @Size(max = 80, message = "La entidad reguladora no puede exceder 80 caracteres")
    private String entidadReguladora;

    @DecimalMin(value = "0.0", inclusive = true, message = "El arancel no puede ser menor a 0%")
    @DecimalMax(value = "100.0", inclusive = true, message = "El arancel no puede ser mayor a 100%")
    @Builder.Default
    private BigDecimal arancelBasePct = BigDecimal.ZERO;

    @Builder.Default
    private String estado = "ACTIVO";
}
