package pe.sga.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioCreateDTO {

    @NotNull(message = "El idRol es obligatorio")
    private Integer idRol;

    private Integer idSede;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El nombre de usuario no puede exceder 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    private String apellidos;

    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "El correo institucional debe tener un formato válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correoInstitucional;

    @Builder.Default
    private String tipoDocumento = "DNI";

    private String numeroDocumento;

    private String telefono;

    private String cargoTitulo;

    private String codigoSunat;
}
