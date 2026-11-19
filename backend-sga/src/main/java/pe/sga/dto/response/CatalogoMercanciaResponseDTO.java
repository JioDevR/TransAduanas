package pe.sga.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoMercanciaResponseDTO {

    private Integer idMercancia;
    private String codigo;
    private String nombre;
    private String categoria;
    private Boolean requierePermiso;
    private String entidadReguladora;
    private BigDecimal arancelBasePct;
    private String estado;
}
