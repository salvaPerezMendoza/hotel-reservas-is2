package ar.fiuba.is2.hotel.dominio.usuario;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Administrador del hotel: configura habitaciones, tarifas y políticas, y ve reportes. */
@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {

    protected Administrador() {
    }

    public Administrador(String nombre, String apellido, String email) {
        super(nombre, apellido, email);
    }

    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }

    /** El administrador también puede hacer todo lo de recepción. */
    @Override
    public boolean puedeOperarRecepcion() {
        return true;
    }

    @Override
    public boolean puedeAdministrar() {
        return true;
    }
}
