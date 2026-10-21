package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConductorRequestDTO {

    private Integer idUsuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombres;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String apellidos;

    @Size(max = 15, message = "El número de documento no puede exceder 15 caracteres")
    private String numeroDocumento;

    @Builder.Default
    @Size(max = 10, message = "El tipo de licencia no puede exceder 10 caracteres")
    private String tipoLicencia = "A-IIIc";

    @NotBlank(message = "El número de licencia es obligatorio")
    @Size(max = 20, message = "El número de licencia no puede exceder 20 caracteres")
    private String numeroLicencia;

    private LocalDate fechaVencimientoLicencia;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Builder.Default
    private String estadoOperativo = "DISPONIBLE";

    @Builder.Default
    private Boolean estadoActivo = true;
}
