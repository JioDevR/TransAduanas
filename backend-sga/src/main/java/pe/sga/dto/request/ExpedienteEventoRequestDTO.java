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
public class ExpedienteEventoRequestDTO {

    @NotBlank(message = "El código de estado o hito es obligatorio")
    @Size(max = 60)
    private String codigoEstado;

    @NotBlank(message = "El título del evento es obligatorio")
    @Size(max = 150)
    private String titulo;

    @Size(max = 1000)
    private String descripcion;

    private LocalDateTime fechaEvento;
}
