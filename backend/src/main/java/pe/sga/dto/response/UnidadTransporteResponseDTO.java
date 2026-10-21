package pe.sga.dto.response;

import lombok.*;

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
    private String tipoVehiculo;
    private BigDecimal capacidadCargaKg;
    private LocalDate soatVencimiento;
    private LocalDate citvVencimiento;
    private Integer kilometrajeActual;
    private String estadoOperativo;
    private Boolean estadoActivo;
}
