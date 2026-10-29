package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.portal.admin.*;
import pe.sga.entity.Cliente;
import pe.sga.entity.UsuarioCliente;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.service.IUsuarioClienteService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioClienteServiceImpl implements IUsuarioClienteService {

    private final UsuarioClienteRepository usuarioClienteRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioClienteResponseDTO> listar(Integer idCliente, Boolean soloActivos, String buscar) {
        List<UsuarioCliente> list = usuarioClienteRepository.findAllWithCliente();
        Stream<UsuarioCliente> stream = list.stream();

        if (idCliente != null) {
            stream = stream.filter(uc -> uc.getCliente() != null && idCliente.equals(uc.getCliente().getIdCliente()));
        }

        if (Boolean.TRUE.equals(soloActivos)) {
            stream = stream.filter(uc -> Boolean.TRUE.equals(uc.getEstadoActivo()));
        }

        if (buscar != null && !buscar.trim().isEmpty()) {
            String term = buscar.trim().toLowerCase();
            stream = stream.filter(uc -> {
                String u = uc.getUsername() != null ? uc.getUsername().toLowerCase() : "";
                String rep = uc.getCliente() != null && uc.getCliente().getContactoRepresentante() != null
                        ? uc.getCliente().getContactoRepresentante().toLowerCase() : "";
                String c = uc.getCliente() != null && uc.getCliente().getCorreoContacto() != null
                        ? uc.getCliente().getCorreoContacto().toLowerCase() : "";
                String tel = uc.getCliente() != null && uc.getCliente().getTelefonoContacto() != null
                        ? uc.getCliente().getTelefonoContacto().toLowerCase() : "";
                String ruc = uc.getCliente() != null && uc.getCliente().getNumeroDocumento() != null
                        ? uc.getCliente().getNumeroDocumento().toLowerCase() : "";
                String razon = uc.getCliente() != null && uc.getCliente().getRazonSocial() != null
                        ? uc.getCliente().getRazonSocial().toLowerCase() : "";

                return u.contains(term) || rep.contains(term) || c.contains(term) || tel.contains(term)
                        || ruc.contains(term) || razon.contains(term);
            });
        }

        return stream.map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioClienteResponseDTO buscarPorId(Integer id) {
        UsuarioCliente uc = usuarioClienteRepository.findByIdWithCliente(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario del portal no encontrado con ID: " + id));
        return mapToResponse(uc);
    }

    @Override
    @Transactional
    public UsuarioClienteResponseDTO crear(UsuarioClienteCreateDTO dto) {
        String username = dto.getUsername() != null ? dto.getUsername().trim().toLowerCase() : "";

        if (usuarioClienteRepository.existsByUsername(username)) {
            throw new BadRequestException("El nombre de usuario '" + username + "' ya se encuentra registrado en el portal");
        }

        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa cliente no encontrada con ID: " + dto.getIdCliente()));

        if (!Boolean.TRUE.equals(cliente.getEstadoActivo())) {
            throw new BadRequestException("No se pueden habilitar usuarios para una empresa inactiva en el sistema");
        }

        if (usuarioClienteRepository.findByCliente_IdCliente(cliente.getIdCliente()).isPresent()) {
            throw new BadRequestException("La empresa cliente '" + cliente.getRazonSocial() + "' ya cuenta con un usuario del portal asignado");
        }

        // Actualizar datos de contacto en Cliente si fueron suministrados
        boolean clienteModificado = false;
        if (dto.getContactoRepresentante() != null && !dto.getContactoRepresentante().isBlank()) {
            cliente.setContactoRepresentante(dto.getContactoRepresentante().trim());
            clienteModificado = true;
        } else if (dto.getNombres() != null && !dto.getNombres().isBlank()) {
            String full = dto.getNombres().trim() + (dto.getApellidos() != null ? " " + dto.getApellidos().trim() : "");
            cliente.setContactoRepresentante(full.trim());
            clienteModificado = true;
        }

        if (dto.getCorreoElectronico() != null && !dto.getCorreoElectronico().isBlank()) {
            cliente.setCorreoContacto(dto.getCorreoElectronico().trim().toLowerCase());
            clienteModificado = true;
        }

        if (dto.getTelefonoContacto() != null && !dto.getTelefonoContacto().isBlank()) {
            cliente.setTelefonoContacto(dto.getTelefonoContacto().trim());
            clienteModificado = true;
        }

        if (clienteModificado) {
            clienteRepository.save(cliente);
        }

        UsuarioCliente uc = UsuarioCliente.builder()
                .cliente(cliente)
                .username(username)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .estadoActivo(true)
                .debeCambiarPassword(dto.getDebeCambiarPassword() != null ? dto.getDebeCambiarPassword() : true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        UsuarioCliente guardado = usuarioClienteRepository.save(uc);
        log.info("Usuario del portal creado exitosamente para la empresa: id={}, username={}, cliente={}",
                guardado.getIdUsuarioCliente(), guardado.getUsername(), cliente.getRazonSocial());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public UsuarioClienteResponseDTO actualizar(Integer id, UsuarioClienteUpdateDTO dto) {
        UsuarioCliente uc = usuarioClienteRepository.findByIdWithCliente(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario del portal no encontrado con ID: " + id));

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String newUsername = dto.getUsername().trim().toLowerCase();
            if (!newUsername.equalsIgnoreCase(uc.getUsername())) {
                if (usuarioClienteRepository.existsByUsername(newUsername)) {
                    throw new BadRequestException("El nombre de usuario '" + newUsername + "' ya está registrado");
                }
                uc.setUsername(newUsername);
            }
        }

        Cliente cliente = uc.getCliente();
        if (cliente != null) {
            boolean clienteModificado = false;
            if (dto.getContactoRepresentante() != null && !dto.getContactoRepresentante().isBlank()) {
                cliente.setContactoRepresentante(dto.getContactoRepresentante().trim());
                clienteModificado = true;
            } else if (dto.getNombres() != null && !dto.getNombres().isBlank()) {
                String full = dto.getNombres().trim() + (dto.getApellidos() != null ? " " + dto.getApellidos().trim() : "");
                cliente.setContactoRepresentante(full.trim());
                clienteModificado = true;
            }

            if (dto.getCorreoElectronico() != null && !dto.getCorreoElectronico().isBlank()) {
                cliente.setCorreoContacto(dto.getCorreoElectronico().trim().toLowerCase());
                clienteModificado = true;
            }

            if (dto.getTelefonoContacto() != null) {
                cliente.setTelefonoContacto(dto.getTelefonoContacto().trim());
                clienteModificado = true;
            }

            if (clienteModificado) {
                clienteRepository.save(cliente);
            }
        }

        if (dto.getEstadoActivo() != null) {
            uc.setEstadoActivo(dto.getEstadoActivo());
        }

        if (dto.getDebeCambiarPassword() != null) {
            uc.setDebeCambiarPassword(dto.getDebeCambiarPassword());
        }

        uc.setFechaActualizacion(LocalDateTime.now());
        UsuarioCliente actualizado = usuarioClienteRepository.save(uc);
        log.info("Usuario del portal actualizado: id={}, username={}", actualizado.getIdUsuarioCliente(), actualizado.getUsername());
        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminarPorEstado(Integer id) {
        UsuarioCliente uc = usuarioClienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario del portal no encontrado con ID: " + id));

        uc.setEstadoActivo(false);
        uc.setFechaActualizacion(LocalDateTime.now());
        usuarioClienteRepository.save(uc);
        log.info("Baja lógica por estado aplicada a usuario de portal: id={}, username={}", id, uc.getUsername());
    }

    @Override
    @Transactional
    public void reactivar(Integer id) {
        UsuarioCliente uc = usuarioClienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario del portal no encontrado con ID: " + id));

        uc.setEstadoActivo(true);
        uc.setFechaActualizacion(LocalDateTime.now());
        usuarioClienteRepository.save(uc);
        log.info("Usuario de portal reactivado: id={}, username={}", id, uc.getUsername());
    }

    @Override
    @Transactional
    public void resetearPassword(Integer id, UsuarioClienteResetPasswordDTO dto) {
        UsuarioCliente uc = usuarioClienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario del portal no encontrado con ID: " + id));

        uc.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        uc.setDebeCambiarPassword(dto.getForzarCambioPrimerInicio() != null ? dto.getForzarCambioPrimerInicio() : true);
        uc.setFechaActualizacion(LocalDateTime.now());
        usuarioClienteRepository.save(uc);
        log.info("Contraseña restablecida administrativamente para usuario de portal: id={}, username={}", id, uc.getUsername());
    }

    private UsuarioClienteResponseDTO mapToResponse(UsuarioCliente uc) {
        Cliente c = uc.getCliente();
        return UsuarioClienteResponseDTO.builder()
                .idUsuarioCliente(uc.getIdUsuarioCliente())
                .idCliente(c != null ? c.getIdCliente() : null)
                .tipoDocumentoCliente(c != null ? c.getTipoDocumento() : null)
                .rucCliente(c != null ? c.getNumeroDocumento() : null)
                .razonSocialCliente(c != null ? c.getRazonSocial() : null)
                .correoContactoCliente(c != null ? c.getCorreoContacto() : null)
                .direccionFiscalCliente(c != null ? c.getDireccionFiscal() : null)
                .username(uc.getUsername())
                .contactoRepresentante(c != null ? c.getContactoRepresentante() : null)
                .nombres(c != null ? c.getContactoRepresentante() : null)
                .apellidos("")
                .nombreCompleto(uc.getNombreCompleto())
                .correoElectronico(c != null ? c.getCorreoContacto() : null)
                .telefonoContacto(c != null ? c.getTelefonoContacto() : null)
                .estadoActivo(uc.getEstadoActivo())
                .debeCambiarPassword(uc.getDebeCambiarPassword())
                .ultimoAcceso(uc.getUltimoAcceso())
                .fechaCreacion(uc.getFechaCreacion())
                .fechaActualizacion(uc.getFechaActualizacion())
                .build();
    }
}
