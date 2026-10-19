package ar.fiuba.is2.hotel.seguridad;

import ar.fiuba.is2.hotel.dominio.usuario.Rol;

/** Lo que el sistema sabe de quien hace el pedido (sale del token JWT). */
public record UsuarioAutenticado(Long id, String email, Rol rol) {
}
