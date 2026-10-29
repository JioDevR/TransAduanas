package pe.sga.dto.portal.admin;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioClienteCreateDTO {

    @NotNull(message = "El cliente empresarial es obligatorio")
    private Integer idCliente;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 100, message = "El usuario debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String username;

    @NotBlank(message = "La contraseña temporal es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    private String contactoRepresentante;

    private String nombres;

    private String apellidos;

    private String correoElectronico;

    private String telefonoContacto;

    @Builder.Default
    private Boolean debeCambiarPassword = true;
}
