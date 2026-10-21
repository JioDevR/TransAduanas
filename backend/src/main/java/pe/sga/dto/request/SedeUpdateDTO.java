package pe.sga.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SedeUpdateDTO {

    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    private String tipo;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;

    @Size(max = 100, message = "El distrito no puede exceder 100 caracteres")
    private String distrito;

    @Size(max = 50, message = "El teléfono no puede exceder 50 caracteres")
    private String telefono;

    private String estado;
}
