package pe.sga.dto.response;

import lombok.*;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.EstadoOperativoConductor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConductorResponseDTO {

    private Integer idConductor;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private String numeroDocumento;
    private CategoriaLicenciaMtc tipoLicencia;
    private String numeroLicencia;
    private LocalDate fechaVencimientoLicencia;
    private String telefono;
    private EstadoOperativoConductor estadoOperativo;
    private Boolean estadoActivo;
    private String observaciones;
}
