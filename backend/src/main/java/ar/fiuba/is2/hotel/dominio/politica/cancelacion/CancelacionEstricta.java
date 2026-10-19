package ar.fiuba.is2.hotel.dominio.politica.cancelacion;

/** 50% hasta 14 días antes; después, nada. */
public class CancelacionEstricta extends PoliticaDeCancelacion {

    @Override
    protected int porcentajeDeReintegro(long diasDeAnticipacion) {
        return diasDeAnticipacion >= 14 ? 50 : 0;
    }

    @Override
    public String nombre() {
        return "Estricta";
    }

    @Override
    public String descripcion() {
        return "Reintegro del 50% si se cancela hasta 14 días antes del ingreso.";
    }
}
