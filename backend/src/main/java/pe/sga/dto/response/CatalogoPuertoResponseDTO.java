package pe.sga.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoPuertoResponseDTO {

    private Integer idPuerto;
    private String codigoPuerto;
    private String nombre;
    private String pais;
    private String tipo;
    private String estado;
}
