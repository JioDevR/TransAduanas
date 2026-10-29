package pe.sga.dto.portal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalCotizacionSolicitudDTO {

    private Integer idMercancia;
    private Integer idTipoCarga;

    @NotBlank(message = "El régimen aduanero es requerido")
    private String regimenAduanero; // IMPORTACION_DEFINITIVA o EXPORTACION_DEFINITIVA

    private Integer idPuertoEnvio;
    private Integer idPuertoDestino;

    private Boolean servicioTransporte;
    private String origenDepartamento;
    private String origenProvincia;
    private String destinoDepartamento;
    private String destinoProvincia;

    @NotNull(message = "El valor FOB estimado es requerido")
    @DecimalMin(value = "0.01", message = "El valor FOB debe ser mayor a 0")
    private BigDecimal valorFob;

    private String observaciones;
}
