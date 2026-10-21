package pe.sga.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponseDTO {

    private Integer idCliente;
    private String tipoDocumento;
    private String numeroDocumento;
    private String razonSocial;
    private contactoDto contacto;
    private String contactoRepresentante;
    private String direccionFiscal;
    private String correoContacto;
    private String telefonoContacto;
    private Boolean esProspecto;
    private Boolean estadoActivo;

    public record contactoDto(String representante, String correo, String telefono) {}
}
