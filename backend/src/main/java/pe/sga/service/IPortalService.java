package pe.sga.service;

import pe.sga.dto.portal.*;
import pe.sga.dto.request.LoginRequestDTO;
import pe.sga.entity.UsuarioCliente;

import java.util.List;

public interface IPortalService {

    PortalAuthResponseDTO autenticarCliente(LoginRequestDTO loginRequest);

    PortalPerfilDTO obtenerPerfil(UsuarioCliente usuarioCliente);

    UsuarioCliente obtenerUsuarioClientePorUsername(String username);

    List<PortalCotizacionDTO> listarCotizaciones(Integer idCliente);

    PortalCotizacionDTO buscarCotizacionPorId(Integer idCliente, Integer idCotizacion);

    PortalCotizacionDTO solicitarCotizacion(Integer idCliente, PortalCotizacionSolicitudDTO dto, Integer idUsuarioCliente);

    PortalCotizacionDTO aceptarCotizacion(Integer idCliente, Integer idCotizacion);

    List<PortalExpedienteDTO> listarExpedientes(Integer idCliente);

    PortalExpedienteDTO buscarExpedientePorId(Integer idCliente, Integer idExpediente);

    PortalResumenFinancieroDTO obtenerResumenFinanciero(Integer idCliente);

    List<PortalPagoAnticipoDTO> listarAnticipos(Integer idCliente);

    List<PortalLiquidacionDTO> listarLiquidaciones(Integer idCliente);

    PortalPagoAnticipoDTO registrarVoucherPago(Integer idCliente, PortalRegistroVoucherDTO dto, Integer idUsuarioCliente);

    PortalPagoAnticipoDTO corregirPagoAnticipo(Integer idCliente, Integer idPago, PortalCorreccionPagoDTO dto, Integer idUsuarioCliente);

    void cambiarPassword(UsuarioCliente usuarioCliente, PortalCambioPasswordDTO dto);

    List<NotificacionClienteDTO> listarNotificaciones(Integer idCliente);

    long contarNotificacionesNoLeidas(Integer idCliente);

    void marcarNotificacionLeida(Integer idCliente, Integer idNotificacion);

    void marcarTodasNotificacionesLeidas(Integer idCliente);

    void crearNotificacion(Integer idCliente, String titulo, String mensaje, String tipo, String enlace);
}
