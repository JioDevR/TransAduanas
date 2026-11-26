package pe.sga.dto.portal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalExpedienteDatosClienteDTO {

    @NotBlank(message = "El nombre del contacto exterior es obligatorio")
    @Size(max = 150)
    private String contactoExteriorNombre;

    @Size(max = 30)
    private String contactoExteriorTelefono;

    @Email(message = "Ingrese un correo válido para el contacto exterior")
    @Size(max = 100)
    private String contactoExteriorCorreo;

    @Size(max = 30)
    private String numeroContenedor;

    @Size(max = 100)
    private String naveViaje;

    @Size(max = 1000)
    private String observaciones;
}
