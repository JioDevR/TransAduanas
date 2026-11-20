package pe.sga.dto.portal.admin;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioClienteResponseDTO {

    private Integer idUsuarioCliente;

    // Datos de la empresa cliente
    private Integer idCliente;
    private String tipoDocumentoCliente;
    private String rucCliente;
    private String razonSocialCliente;
    private String correoContactoCliente;
    private String direccionFiscalCliente;

    // Datos de la cuenta de acceso y contacto empresarial
    private String username;
    private String contactoRepresentante;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private String correoElectronico;
    private String telefonoContacto;

    // Estado y seguridad
    private Boolean estadoActivo;
    private Boolean debeCambiarPassword;
    private LocalDateTime ultimoAcceso;

    // Auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
