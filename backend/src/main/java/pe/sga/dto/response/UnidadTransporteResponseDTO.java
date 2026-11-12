package pe.sga.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String observaciones;

    @JsonProperty("anioFabricacion")
    public Integer getAnioFabricacion() {
        return anoFabricacion;
    }

    @JsonProperty("fechaVencimientoSoat")
    public LocalDate getFechaVencimientoSoat() {
        return soatVencimiento;
    }

    @JsonProperty("fechaVencimientoCitv")
    public LocalDate getFechaVencimientoCitv() {
        return citvVencimiento;
    }

    @JsonProperty("placa")
    public String getPlaca() {
        return placaTracto;
    }

    @JsonProperty("tipoUnidad")
    public String getTipoUnidad() {
        return tipoVehiculo;
    }
}
