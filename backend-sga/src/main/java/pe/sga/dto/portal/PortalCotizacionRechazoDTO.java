package pe.sga.dto.portal;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalCotizacionRechazoDTO {

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;

    // RECHAZAR | OBSERVAR (por defecto RECHAZAR; OBSERVAR devuelve a Comercial para ajuste)
    private String accion;
}
