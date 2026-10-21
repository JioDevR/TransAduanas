package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SedeCreateDTO {

    @NotBlank(message = "El código de sede es obligatorio")
    @Size(max = 50, message = "El código de sede no puede exceder 50 caracteres")
    private String codigoSede;

    @NotBlank(message = "El nombre de la sede es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @Builder.Default
    private String tipo = "SEDE_CENTRAL";

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;

    @NotBlank(message = "El distrito es obligatorio")
    @Size(max = 100, message = "El distrito no puede exceder 100 caracteres")
    private String distrito;

    @Size(max = 50, message = "El teléfono no puede exceder 50 caracteres")
    private String telefono;

    @Builder.Default
    private String estado = "OPERATIVO";
}
