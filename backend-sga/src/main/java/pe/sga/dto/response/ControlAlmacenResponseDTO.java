package pe.sga.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlAlmacenResponseDTO {

    private Integer idControlAlmacen;

    // Expediente asociado
    private Integer idExpediente;
    private String codigoExpediente;
    private String numeroDam;
    private String canalSunat;
    private String clienteRazonSocial;

    // Depósito y Ubicación
    private String depositoTemporal;
    private String ubicacionBahia;
    private String tipoIngreso;
    private String finalidadAlmacenaje;

    // Fechas y Días Libres
    private LocalDateTime fechaIngreso;
    private Integer diasLibresAlmacen;
    private LocalDate fechaLimiteRetiro;
    private Long diasRestantes;

    // Carga y Pesaje
    private Integer cantidadBultos;
    private BigDecimal pesoBrutoKg;
    private String ticketPesajeVgm;
    private Boolean precintoConforme;

    // Salida
    private Boolean autorizadoMovimiento;
    private LocalDateTime fechaSalida;
    private String estadoAlmacen;
    private String observaciones;
}
