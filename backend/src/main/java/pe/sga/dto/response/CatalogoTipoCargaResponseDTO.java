package pe.sga.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoTipoCargaResponseDTO {

    private Integer idTipoCarga;
    private String codigo;
    private String nombre;
    private String estado;
}
