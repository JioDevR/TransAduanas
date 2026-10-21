package pe.sga.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioUpdateDTO {

    private Integer idRol;

    private Integer idSede;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres si se modifica")
    private String password;

    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    private String nombres;

    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    private String apellidos;

    @Email(message = "El correo institucional debe tener un formato válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correoInstitucional;

    private String tipoDocumento;

    private String numeroDocumento;

    private String telefono;

    private String cargoTitulo;

    private String codigoSunat;

    private Boolean estadoActivo;
}
