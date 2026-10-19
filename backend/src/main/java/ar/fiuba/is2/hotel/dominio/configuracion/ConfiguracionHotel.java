package ar.fiuba.is2.hotel.dominio.configuracion;

import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.PoliticaDeCancelacion;
import ar.fiuba.is2.hotel.dominio.politica.cancelacion.TipoPoliticaCancelacion;
import ar.fiuba.is2.hotel.dominio.politica.precio.PoliticaDePrecio;
import ar.fiuba.is2.hotel.dominio.politica.precio.TipoPoliticaPrecio;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Parámetros generales del hotel que define el administrador (una sola fila). */
@Entity
@Table(name = "configuracion_hotel")
public class ConfiguracionHotel {

    public static final int ID_UNICO = 1;

    @Id
    private Integer id = ID_UNICO;

    @Column(name = "nombre_hotel", nullable = false)
    private String nombreHotel;

    @Enumerated(EnumType.STRING)
    @Column(name = "politica_cancelacion", nullable = false)
    private TipoPoliticaCancelacion politicaCancelacion;

    @Column(name = "porcentaje_sena", nullable = false)
    private int porcentajeSena;

    @Column(name = "minutos_retencion", nullable = false)
    private int minutosRetencion;

    @Column(name = "hora_check_in", nullable = false)
    private String horaCheckIn;

    @Column(name = "hora_check_out", nullable = false)
    private String horaCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "estrategia_precio", nullable = false)
    private TipoPoliticaPrecio estrategiaPrecio;

    protected ConfiguracionHotel() {
    }

    public void actualizar(String nombreHotel, TipoPoliticaCancelacion politica, int porcentajeSena,
                           int minutosRetencion, String horaCheckIn, String horaCheckOut,
                           TipoPoliticaPrecio estrategiaPrecio) {
        if (porcentajeSena < 0 || porcentajeSena > 100) {
            throw new ReglaDeNegocioException("El porcentaje de seña debe estar entre 0 y 100");
        }
        if (minutosRetencion < 1 || minutosRetencion > 120) {
            throw new ReglaDeNegocioException("La retención debe durar entre 1 y 120 minutos");
        }
        this.nombreHotel = nombreHotel;
        this.politicaCancelacion = politica;
        this.porcentajeSena = porcentajeSena;
        this.minutosRetencion = minutosRetencion;
        this.horaCheckIn = horaCheckIn;
        this.horaCheckOut = horaCheckOut;
        this.estrategiaPrecio = estrategiaPrecio;
    }

    public PoliticaDeCancelacion politicaDeCancelacionVigente() {
        return politicaCancelacion.crear();
    }

    public PoliticaDePrecio politicaDePrecioVigente() {
        return estrategiaPrecio.crear();
    }

    /** Monto mínimo a pagar online para confirmar una reserva. */
    public BigDecimal senaPara(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(porcentajeSena))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public String getNombreHotel() {
        return nombreHotel;
    }

    public TipoPoliticaCancelacion getPoliticaCancelacion() {
        return politicaCancelacion;
    }

    public int getPorcentajeSena() {
        return porcentajeSena;
    }

    public int getMinutosRetencion() {
        return minutosRetencion;
    }

    public String getHoraCheckIn() {
        return horaCheckIn;
    }

    public String getHoraCheckOut() {
        return horaCheckOut;
    }

    public TipoPoliticaPrecio getEstrategiaPrecio() {
        return estrategiaPrecio;
    }
}
