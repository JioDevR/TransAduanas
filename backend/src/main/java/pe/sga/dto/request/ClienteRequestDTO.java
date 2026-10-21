package pe.sga.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteRequestDTO {

    @Builder.Default
    @Size(max = 10, message = "El tipo de documento no puede exceder 10 caracteres")
    private String tipoDocumento = "RUC";

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 15, message = "El número de documento no puede exceder 15 caracteres")
    private String numeroDocumento;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
    private String razonSocial;

    @Size(max = 100, message = "El contacto representante no puede exceder 100 caracteres")
    private String contactoRepresentante;

    @Size(max = 200, message = "La dirección fiscal no puede exceder 200 caracteres")
    private String direccionFiscal;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto debe tener un formato válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correoContacto;

    @Size(max = 20, message = "El teléfono de contacto no puede exceder 20 caracteres")
    private String telefonoContacto;

    @Builder.Default
    private Boolean esProspecto = true;

    @Builder.Default
    private Boolean estadoActivo = true;
}
