package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sga.dto.request.LoginRequestDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.AuthResponseDTO;
import pe.sga.dto.response.UsuarioResponseDTO;
import pe.sga.entity.Usuario;
import pe.sga.service.IUsuarioService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IUsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = usuarioService.autenticar(request);
        return ResponseEntity.ok(ApiResponseDTO.ok("Autenticación exitosa", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> me(@AuthenticationPrincipal Usuario usuario) {
        UsuarioResponseDTO response = usuarioService.buscarPorId(usuario.getIdUsuario());
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuario autenticado obtenido", response));
    }
}
