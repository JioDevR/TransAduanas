package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespachoAsignarRecursosDTO {

    @NotNull(message = "El idConductor es obligatorio")
    private Integer idConductor;

    @NotNull(message = "El idUnidad es obligatorio")
    private Integer idUnidad;

    @Size(max = 30, message = "La guía de remisión no puede exceder 30 caracteres")
    private String guiaRemisionTransporte;

    private java.time.LocalDateTime fechaProgramada;

    private String observaciones;
}
