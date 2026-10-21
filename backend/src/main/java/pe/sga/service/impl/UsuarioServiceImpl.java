package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.config.JwtTokenProvider;
import pe.sga.dto.request.LoginRequestDTO;
import pe.sga.dto.request.UsuarioCreateDTO;
import pe.sga.dto.request.UsuarioUpdateDTO;
import pe.sga.dto.response.AuthResponseDTO;
import pe.sga.dto.response.UsuarioResponseDTO;
import pe.sga.entity.Rol;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.RolRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IUsuarioService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponseDTO autenticar(LoginRequestDTO loginRequest) {
        Usuario usuario = usuarioRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            throw new BadCredentialsException("El usuario se encuentra inactivo en el sistema");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = tokenProvider.generateToken(usuario);

        return AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .idUsuario(usuario.getIdUsuario())
                .username(usuario.getUsername())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .correoInstitucional(usuario.getCorreoInstitucional())
                .rol(usuario.getRol().getNombreRol().name())
                .idSede(usuario.getSede() != null ? usuario.getSede().getIdSede() : null)
                .codigoSede(usuario.getSede() != null ? usuario.getSede().getCodigoSede() : null)
                .nombreSede(usuario.getSede() != null ? usuario.getSede().getNombre() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarActivos() {
        return usuarioRepository.findByEstadoActivoTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));
        return mapToResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con username: " + username));
        return mapToResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO crear(UsuarioCreateDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new BadRequestException("El nombre de usuario '" + dto.getUsername() + "' ya está registrado");
        }
        if (usuarioRepository.existsByCorreoInstitucional(dto.getCorreoInstitucional())) {
            throw new BadRequestException("El correo institucional '" + dto.getCorreoInstitucional() + "' ya está registrado");
        }

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getIdRol()));

        Sede sede = null;
        if (dto.getIdSede() != null) {
            sede = sedeRepository.findById(dto.getIdSede())
                    .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getIdSede()));
        }

        Usuario usuario = Usuario.builder()
                .rol(rol)
                .sede(sede)
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .correoInstitucional(dto.getCorreoInstitucional())
                .tipoDocumento(dto.getTipoDocumento() != null ? dto.getTipoDocumento() : "DNI")
                .numeroDocumento(dto.getNumeroDocumento())
                .telefono(dto.getTelefono())
                .cargoTitulo(dto.getCargoTitulo())
                .codigoSunat(dto.getCodigoSunat())
                .estadoActivo(true)
                .build();

        return mapToResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizar(Integer idUsuario, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        if (dto.getIdRol() != null && !dto.getIdRol().equals(usuario.getRol().getIdRol())) {
            Rol rol = rolRepository.findById(dto.getIdRol())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getIdRol()));
            usuario.setRol(rol);
        }

        if (dto.getIdSede() != null) {
            Sede sede = sedeRepository.findById(dto.getIdSede())
                    .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getIdSede()));
            usuario.setSede(sede);
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getCorreoInstitucional() != null && !dto.getCorreoInstitucional().equals(usuario.getCorreoInstitucional())) {
            if (usuarioRepository.existsByCorreoInstitucional(dto.getCorreoInstitucional())) {
                throw new BadRequestException("El correo institucional ya está en uso por otro usuario");
            }
            usuario.setCorreoInstitucional(dto.getCorreoInstitucional());
        }

        if (dto.getNombres() != null) usuario.setNombres(dto.getNombres());
        if (dto.getApellidos() != null) usuario.setApellidos(dto.getApellidos());
        if (dto.getTipoDocumento() != null) usuario.setTipoDocumento(dto.getTipoDocumento());
        if (dto.getNumeroDocumento() != null) usuario.setNumeroDocumento(dto.getNumeroDocumento());
        if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
        if (dto.getCargoTitulo() != null) usuario.setCargoTitulo(dto.getCargoTitulo());
        if (dto.getCodigoSunat() != null) usuario.setCodigoSunat(dto.getCodigoSunat());
        if (dto.getEstadoActivo() != null) usuario.setEstadoActivo(dto.getEstadoActivo());

        return mapToResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void eliminarPorEstado(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));
        usuario.setEstadoActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void reactivar(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));
        usuario.setEstadoActivo(true);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponseDTO mapToResponse(Usuario u) {
        return UsuarioResponseDTO.builder()
                .idUsuario(u.getIdUsuario())
                .idRol(u.getRol().getIdRol())
                .nombreRol(u.getRol().getNombreRol().name())
                .idSede(u.getSede() != null ? u.getSede().getIdSede() : null)
                .codigoSede(u.getSede() != null ? u.getSede().getCodigoSede() : null)
                .nombreSede(u.getSede() != null ? u.getSede().getNombre() : null)
                .username(u.getUsername())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .correoInstitucional(u.getCorreoInstitucional())
                .tipoDocumento(u.getTipoDocumento())
                .numeroDocumento(u.getNumeroDocumento())
                .telefono(u.getTelefono())
                .cargoTitulo(u.getCargoTitulo())
                .codigoSunat(u.getCodigoSunat())
                .ultimoAcceso(u.getUltimoAcceso())
                .estadoActivo(u.getEstadoActivo())
                .build();
    }
}
