package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.ClienteRequestDTO;
import pe.sga.dto.response.ClienteResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ClienteRepository;
import pe.sga.service.IClienteService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements IClienteService {

    private final ClienteRepository clienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarActivos() {
        return clienteRepository.findByEstadoActivo(true).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarPorTipoProspecto(Boolean esProspecto, Boolean soloActivos) {
        if (esProspecto == null) {
            return soloActivos != null && soloActivos ? listarActivos() : listarTodos();
        }
        if (soloActivos != null && soloActivos) {
            return clienteRepository.findByEsProspectoAndEstadoActivo(esProspecto, true).stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return clienteRepository.findByEsProspecto(esProspecto).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarPorTermino(String termino, Boolean soloActivos) {
        if (termino == null || termino.trim().isEmpty()) {
            return soloActivos != null && soloActivos ? listarActivos() : listarTodos();
        }
        if (soloActivos != null && soloActivos) {
            return clienteRepository.findByRazonSocialContainingIgnoreCaseAndEstadoActivo(termino.trim(), true).stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return clienteRepository.findByRazonSocialContainingIgnoreCase(termino.trim()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        return mapToResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorNumeroDocumento(String numeroDocumento) {
        Cliente cliente = clienteRepository.findByNumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con documento: " + numeroDocumento));
        return mapToResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO crear(ClienteRequestDTO dto) {
        if (clienteRepository.existsByNumeroDocumento(dto.getNumeroDocumento())) {
            throw new BadRequestException("Ya existe un cliente registrado con el número de documento: " + dto.getNumeroDocumento());
        }

        Cliente cliente = Cliente.builder()
                .tipoDocumento(dto.getTipoDocumento() != null ? dto.getTipoDocumento().toUpperCase() : "RUC")
                .numeroDocumento(dto.getNumeroDocumento().trim())
                .razonSocial(dto.getRazonSocial().trim())
                .contactoRepresentante(dto.getContactoRepresentante() != null ? dto.getContactoRepresentante().trim() : null)
                .direccionFiscal(dto.getDireccionFiscal() != null ? dto.getDireccionFiscal().trim() : null)
                .correoContacto(dto.getCorreoContacto().trim())
                .telefonoContacto(dto.getTelefonoContacto() != null ? dto.getTelefonoContacto().trim() : null)
                .esProspecto(true) // Regla de negocio: Todo cliente nuevo se registra como Prospecto Comercial
                .estadoActivo(dto.getEstadoActivo() != null ? dto.getEstadoActivo() : true)
                .build();

        return mapToResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Integer id, ClienteRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        if (!cliente.getNumeroDocumento().equalsIgnoreCase(dto.getNumeroDocumento().trim())) {
            if (clienteRepository.existsByNumeroDocumento(dto.getNumeroDocumento().trim())) {
                throw new BadRequestException("Ya existe otro cliente con el número de documento: " + dto.getNumeroDocumento());
            }
        }

        cliente.setTipoDocumento(dto.getTipoDocumento() != null ? dto.getTipoDocumento().toUpperCase() : "RUC");
        cliente.setNumeroDocumento(dto.getNumeroDocumento().trim());
        cliente.setRazonSocial(dto.getRazonSocial().trim());
        cliente.setContactoRepresentante(dto.getContactoRepresentante() != null ? dto.getContactoRepresentante().trim() : null);
        cliente.setDireccionFiscal(dto.getDireccionFiscal() != null ? dto.getDireccionFiscal().trim() : null);
        cliente.setCorreoContacto(dto.getCorreoContacto().trim());
        cliente.setTelefonoContacto(dto.getTelefonoContacto() != null ? dto.getTelefonoContacto().trim() : null);
        // El estado 'esProspecto' es inmutable manualmente: solo se promueve automáticamente al aprobar una cotización
        if (dto.getEstadoActivo() != null) {
            cliente.setEstadoActivo(dto.getEstadoActivo());
        }

        return mapToResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        // Borrado lógico por estado_activo
        cliente.setEstadoActivo(false);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void reactivar(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        cliente.setEstadoActivo(true);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO convertirEnCliente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        cliente.setEsProspecto(false);
        return mapToResponse(clienteRepository.save(cliente));
    }

    private ClienteResponseDTO mapToResponse(Cliente c) {
        return ClienteResponseDTO.builder()
                .idCliente(c.getIdCliente())
                .tipoDocumento(c.getTipoDocumento())
                .numeroDocumento(c.getNumeroDocumento())
                .razonSocial(c.getRazonSocial())
                .contacto(new ClienteResponseDTO.contactoDto(c.getContactoRepresentante(), c.getCorreoContacto(), c.getTelefonoContacto()))
                .contactoRepresentante(c.getContactoRepresentante())
                .direccionFiscal(c.getDireccionFiscal())
                .correoContacto(c.getCorreoContacto())
                .telefonoContacto(c.getTelefonoContacto())
                .esProspecto(c.getEsProspecto())
                .estadoActivo(c.getEstadoActivo())
                .build();
    }
}
