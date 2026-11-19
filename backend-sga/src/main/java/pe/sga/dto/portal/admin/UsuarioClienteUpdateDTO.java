package pe.sga.dto.portal.admin;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioClienteUpdateDTO {

    private String username;

    private String contactoRepresentante;

    private String nombres;

    private String apellidos;

    private String correoElectronico;

    private String telefonoContacto;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean estadoActivo;

    private Boolean debeCambiarPassword;
}
