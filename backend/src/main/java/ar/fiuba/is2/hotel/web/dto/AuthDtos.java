package ar.fiuba.is2.hotel.web.dto;

import ar.fiuba.is2.hotel.dominio.usuario.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
            @NotBlank(message = "El apellido es obligatorio") @Size(max = 80) String apellido,
            @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") String email,
            @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
            @Size(max = 40) String telefono,
            @Size(max = 30) String documento) {
    }

    public record LoginRequest(
            @NotBlank(message = "El email es obligatorio") String email,
            @NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record RecuperarRequest(@NotBlank @Email(message = "El email no es válido") String email) {
    }

    public record RestablecerRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password) {
    }

    public record PerfilRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @Size(max = 40) String telefono,
            @Size(max = 30) String documento) {
    }

    public record UsuarioDto(Long id, String nombre, String apellido, String email, Rol rol,
                             String telefono, String documento, boolean activo) {
    }

    public record SesionDto(String token, UsuarioDto usuario) {
    }
}
