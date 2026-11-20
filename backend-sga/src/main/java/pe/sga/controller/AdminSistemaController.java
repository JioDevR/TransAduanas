package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.RolMatrizDTO;
import pe.sga.service.IAdminSistemaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sistema")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_USUARIOS_READ)
public class AdminSistemaController {

    private final IAdminSistemaService adminSistemaService;

    @GetMapping("/dashboard")
    @PreAuthorize(SecurityRoles.ACCESS_LOGS_READ)
    public ResponseEntity<ApiResponseDTO<AdminDashboardResponseDTO>> obtenerDashboard(
            @RequestParam(defaultValue = "7") int dias,
            @RequestParam(required = false, defaultValue = "TODOS") String modulo
    ) {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Dashboard TI recuperado exitosamente",
                adminSistemaService.obtenerDashboard(dias, modulo)
        ));
    }

    @GetMapping("/estado")
    public ResponseEntity<ApiResponseDTO<AdminSistemaEstadoDTO>> obtenerEstado() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Estado técnico del sistema recuperado exitosamente",
                adminSistemaService.obtenerEstadoSistema()
        ));
    }

    @GetMapping("/roles-matriz")
    public ResponseEntity<ApiResponseDTO<List<RolMatrizDTO>>> obtenerMatrizRoles() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Matriz de roles y accesos recuperada exitosamente",
                adminSistemaService.obtenerMatrizRoles()
        ));
    }
}
