package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import pe.sga.entity.enums.EstadoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionRequestDTO {

    @Size(max = 20, message = "El código de cotización no puede exceder 20 caracteres")
    private String codigoCotizacion;

    @NotNull(message = "El idCliente es obligatorio")
    private Integer idCliente;

    @NotNull(message = "El idUsuarioComercial es obligatorio")
    private Integer idUsuarioComercial;

    private Integer idMercancia;

    private Integer idTipoCarga;

    @Builder.Default
    @Size(max = 50, message = "El régimen aduanero no puede exceder 50 caracteres")
    private String regimenAduanero = "IMPORTACION_DEFINITIVA";

    private Integer idPuertoEnvio;

    private Integer idPuertoDestino;

    @Builder.Default
    private Boolean servicioTransporte = false;

    private String origenDepartamento;

    private String origenProvincia;

    private String destinoDepartamento;

    private String destinoProvincia;

    @Builder.Default
    private BigDecimal costoTransporteInterno = BigDecimal.ZERO;

    private LocalDate fechaEmision;

    @Builder.Default
    private Integer validezDias = 15;

    @Builder.Default
    private BigDecimal valorFob = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal montoFleteEstimado = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal montoSeguroEstimado = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal montoHonorariosEstimado = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal tasaAdValorem = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal tasaPercepcion = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal montoTributosEstimado = BigDecimal.ZERO;

    @NotNull(message = "El total cotizado es obligatorio")
    private BigDecimal totalCotizado;

    @Builder.Default
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    private String observaciones;
}
