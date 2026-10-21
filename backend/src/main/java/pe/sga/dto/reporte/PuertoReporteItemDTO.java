package pe.sga.dto.reporte;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuertoReporteItemDTO {

    private Integer idPuerto;
    private String codigoPuerto;
    private String nombre;
    private String pais;
    private String tipo;
    private String estado;
}
