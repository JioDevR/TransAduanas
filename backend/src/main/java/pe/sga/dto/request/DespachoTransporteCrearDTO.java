package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespachoTransporteCrearDTO {

    @NotNull(message = "El idExpediente es obligatorio")
    private Integer idExpediente;

    @Builder.Default
    @Size(max = 30, message = "El tipo de traslado no puede exceder 30 caracteres")
    private String tipoTraslado = "ENTREGA_IMPORTACION";

    private Integer idConductor;

    private Integer idUnidad;

    @Size(max = 30, message = "La guía de remisión no puede exceder 30 caracteres")
    private String guiaRemisionTransporte;

    @NotBlank(message = "La dirección de partida es obligatoria")
    @Size(max = 255, message = "La dirección de partida no puede exceder 255 caracteres")
    private String direccionPartida;

    @NotBlank(message = "La dirección de llegada es obligatoria")
    @Size(max = 255, message = "La dirección de llegada no puede exceder 255 caracteres")
    private String direccionLlegada;

    @NotBlank(message = "El distrito de llegada es obligatorio")
    @Size(max = 100, message = "El distrito de llegada no puede exceder 100 caracteres")
    private String distritoLlegada;

    @Size(max = 100, message = "El contacto de recepción no puede exceder 100 caracteres")
    private String contactoRecepcion;

    @NotNull(message = "La fecha programada de traslado es obligatoria")
    private LocalDateTime fechaProgramada;

    private String observaciones;
}
