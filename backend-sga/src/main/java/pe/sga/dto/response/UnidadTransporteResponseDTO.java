package pe.sga.dto.response;

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
public class UnidadTransporteResponseDTO {

    private Integer idUnidad;
    private String placaTracto;
    private String placaCarreta;
    private String marcaModelo;
    private Integer anoFabricacion;
    private TipoVehiculo tipoVehiculo;
    private BigDecimal capacidadCargaKg;
    private LocalDate soatVencimiento;
    private LocalDate citvVencimiento;
    private Integer kilometrajeActual;
    private EstadoOperativoFlota estadoOperativo;
    private Boolean estadoActivo;
    private String observaciones;
}
