package ar.fiuba.is2.hotel.dominio.politica.cancelacion;

/** 100% hasta 5 días antes, 50% hasta 1 día antes, después nada. */
public class CancelacionModerada extends PoliticaDeCancelacion {

    @Override
    protected int porcentajeDeReintegro(long diasDeAnticipacion) {
        if (diasDeAnticipacion >= 5) {
            return 100;
        }
        if (diasDeAnticipacion >= 1) {
            return 50;
        }
        return 0;
    }

    @Override
    public String nombre() {
        return "Moderada";
    }

    @Override
    public String descripcion() {
        return "Reintegro del 100% hasta 5 días antes del ingreso y del 50% hasta 1 día antes.";
    }
}
