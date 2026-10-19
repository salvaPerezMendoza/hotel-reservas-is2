package ar.fiuba.is2.hotel.dominio.usuario;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Cliente del hotel: busca, reserva, paga, modifica y cancela sus reservas. */
@Entity
@DiscriminatorValue("HUESPED")
public class Huesped extends Usuario {

    protected Huesped() {
    }

    public Huesped(String nombre, String apellido, String email, String telefono, String documento) {
        super(nombre, apellido, email);
        actualizarDatos(nombre, apellido, telefono, documento);
    }

    @Override
    public Rol getRol() {
        return Rol.HUESPED;
    }

    @Override
    public boolean puedeOperarRecepcion() {
        return false;
    }

    @Override
    public boolean puedeAdministrar() {
        return false;
    }
}
