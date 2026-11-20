package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteRecepcionExteriorDTO {

    @NotBlank(message = "El nombre del consignatario o corresponsal en destino es obligatorio")
    @Size(max = 120, message = "El consignatario no puede exceder 120 caracteres")
    private String consignatarioExterior;

    @Size(max = 50, message = "El documento o BL no puede exceder 50 caracteres")
    private String documentoRecepcion;

    private LocalDateTime fechaRecepcionDestino;

    private String observaciones;
}
