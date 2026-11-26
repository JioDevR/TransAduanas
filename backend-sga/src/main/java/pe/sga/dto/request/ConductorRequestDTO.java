package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.EstadoOperativoConductor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConductorRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombres;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String apellidos;

    @Size(min = 8, max = 15, message = "El número de documento debe tener entre 8 y 15 caracteres")
    private String numeroDocumento;

    @Builder.Default
    private CategoriaLicenciaMtc tipoLicencia = CategoriaLicenciaMtc.A_IIIC;

    @NotBlank(message = "El número de licencia es obligatorio")
    @Size(max = 20, message = "El número de licencia no puede exceder 20 caracteres")
    private String numeroLicencia;

    private LocalDate fechaVencimientoLicencia;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Builder.Default
    private EstadoOperativoConductor estadoOperativo = EstadoOperativoConductor.DISPONIBLE;

    @Builder.Default
    private Boolean estadoActivo = true;

    private String observaciones;
}
