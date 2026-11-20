package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteCrearDTO {

    @NotNull(message = "El idCotizacion es obligatorio")
    private Integer idCotizacion;

    @NotNull(message = "El idAgenteAduanero asignado es obligatorio")
    private Integer idAgenteAduanero;

    @Size(max = 150, message = "El nombre del contacto exterior no puede exceder 150 caracteres")
    private String contactoExteriorNombre;

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    private String contactoExteriorTelefono;

    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String contactoExteriorCorreo;

    private LocalDate fechaZarpeEtd;

    private LocalDate fechaLlegadaEta;

    @Size(max = 30, message = "El contenedor no puede exceder 30 caracteres")
    private String numeroContenedor;

    @Size(max = 50, message = "El precinto no puede exceder 50 caracteres")
    private String precintoAduanero;

    @Size(max = 100, message = "La nave o viaje no puede exceder 100 caracteres")
    private String naveViaje;

    private String observaciones;
}
