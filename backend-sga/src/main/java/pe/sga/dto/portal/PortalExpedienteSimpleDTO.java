package pe.sga.dto.portal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalExpedienteSimpleDTO {

    @NotNull(message = "Debe seleccionar la cotización aprobada para generar su expediente")
    private Integer idCotizacion;

    @NotBlank(message = "El nombre del contacto exterior es obligatorio")
    @Size(max = 150, message = "El nombre del contacto exterior no puede superar los 150 caracteres")
    private String contactoExteriorNombre;

    @Size(max = 30, message = "El teléfono del contacto exterior no puede superar los 30 caracteres")
    private String contactoExteriorTelefono;

    @Email(message = "Ingrese un correo válido para el contacto exterior")
    @Size(max = 100, message = "El correo del contacto exterior no puede superar los 100 caracteres")
    private String contactoExteriorCorreo;

    private LocalDate fechaZarpeEtd;

    private LocalDate fechaLlegadaEta;

    @Size(max = 30)
    private String numeroContenedor;

    @Size(max = 100)
    private String naveViaje;

    @Size(max = 1000)
    private String observaciones;
}
