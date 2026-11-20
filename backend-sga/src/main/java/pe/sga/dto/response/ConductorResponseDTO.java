package pe.sga.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConductorResponseDTO {

    private Integer idConductor;
    private Integer idUsuario;
    private String username;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private String numeroDocumento;
    private String tipoLicencia;
    private String numeroLicencia;
    private LocalDate fechaVencimientoLicencia;
    private String telefono;
    private String estadoOperativo;
    private Boolean estadoActivo;
    private String observaciones;

    @JsonProperty("categoriaLicencia")
    public String getCategoriaLicencia() {
        return tipoLicencia;
    }

    @JsonProperty("usuarioUsername")
    public String getUsuarioUsername() {
        return username;
    }
}
