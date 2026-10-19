package ar.fiuba.is2.hotel.dominio.politica.cancelacion;

/** Reintegro total hasta 1 día antes del ingreso; después, nada. */
public class CancelacionFlexible extends PoliticaDeCancelacion {

    @Override
    protected int porcentajeDeReintegro(long diasDeAnticipacion) {
        return diasDeAnticipacion >= 1 ? 100 : 0;
    }

    @Override
    public String nombre() {
        return "Flexible";
    }

    @Override
    public String descripcion() {
        return "Reintegro del 100% si se cancela hasta 1 día antes del ingreso.";
    }
}
