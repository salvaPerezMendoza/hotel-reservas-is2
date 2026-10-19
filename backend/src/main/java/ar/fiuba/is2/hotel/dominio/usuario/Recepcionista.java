package ar.fiuba.is2.hotel.dominio.usuario;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Personal de recepción: reservas telefónicas o presenciales, cobros y check-in/out. */
@Entity
@DiscriminatorValue("RECEPCIONISTA")
public class Recepcionista extends Usuario {

    protected Recepcionista() {
    }

    public Recepcionista(String nombre, String apellido, String email) {
        super(nombre, apellido, email);
    }

    @Override
    public Rol getRol() {
        return Rol.RECEPCIONISTA;
    }

    @Override
    public boolean puedeOperarRecepcion() {
        return true;
    }

    @Override
    public boolean puedeAdministrar() {
        return false;
    }
}
