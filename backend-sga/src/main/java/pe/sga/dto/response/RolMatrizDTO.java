package pe.sga.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolMatrizDTO {
    private String codigo;
    private String etiqueta;
    private String descripcion;
    private List<String> modulosPermitidos;
    private List<String> capacidades;
    private boolean conmutaModulos;
    private long usuariosActivos;
    private long usuariosInactivos;
}
