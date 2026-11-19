package pe.sga.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SedeResponseDTO {

    private Integer idSede;
    private String codigoSede;
    private String nombre;
    private String tipo;
    private String direccion;
    private String distrito;
    private String telefono;
    private String estado;
}
