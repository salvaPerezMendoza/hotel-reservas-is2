package ar.fiuba.is2.hotel.web;

import ar.fiuba.is2.hotel.servicio.AuthService;
import ar.fiuba.is2.hotel.web.dto.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionDto registrar(@Valid @RequestBody RegistroRequest req) {
        return auth.registrar(req);
    }

    @PostMapping("/login")
    public SesionDto login(@Valid @RequestBody LoginRequest req) {
        return auth.iniciarSesion(req);
    }

    @GetMapping("/me")
    public UsuarioDto yo() {
        return auth.yo();
    }

    @PutMapping("/me")
    public UsuarioDto actualizarPerfil(@Valid @RequestBody PerfilRequest req) {
        return auth.actualizarPerfil(req);
    }

    @PostMapping("/recuperar")
    public Map<String, String> recuperar(@Valid @RequestBody RecuperarRequest req) {
        auth.solicitarRecuperacion(req);
        return Map.of("mensaje", "Si el email está registrado, te enviamos un enlace para restablecer la contraseña.");
    }

    @PostMapping("/restablecer")
    public Map<String, String> restablecer(@Valid @RequestBody RestablecerRequest req) {
        auth.restablecer(req);
        return Map.of("mensaje", "Listo, ya podés iniciar sesión con tu nueva contraseña.");
    }
}
