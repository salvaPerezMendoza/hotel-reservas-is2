package ar.fiuba.is2.hotel.dominio;

import ar.fiuba.is2.hotel.dominio.habitacion.Habitacion;
import ar.fiuba.is2.hotel.dominio.habitacion.TipoHabitacion;
import ar.fiuba.is2.hotel.dominio.usuario.Huesped;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

/** Objetos de dominio listos para usar en tests unitarios (sin base de datos). */
public final class Fixtures {

    private Fixtures() {
    }

    public static TipoHabitacion tipo(long id, String nombre, int capacidad, String precioBase) {
        TipoHabitacion t = new TipoHabitacion(nombre, "", capacidad, new BigDecimal(precioBase), List.of(), null);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    public static Habitacion habitacion(long id, String numero, TipoHabitacion tipo) {
        Habitacion h = new Habitacion(numero, 1, tipo);
        ReflectionTestUtils.setField(h, "id", id);
        return h;
    }

    public static Huesped huesped(long id) {
        Huesped h = new Huesped("Ana", "Fernández", "ana" + id + "@demo.com", null, null);
        ReflectionTestUtils.setField(h, "id", id);
        return h;
    }
}
