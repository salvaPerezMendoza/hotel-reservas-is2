package ar.fiuba.is2.hotel.servicio.notificacion;

import ar.fiuba.is2.hotel.dominio.configuracion.ConfiguracionHotel;
import ar.fiuba.is2.hotel.dominio.pago.Pago;
import ar.fiuba.is2.hotel.dominio.reserva.Reserva;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Arma el HTML de cada mail. */
@Component
public class PlantillasDeCorreo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", new Locale("es", "AR"));

    public String reserva(ConfiguracionHotel hotel, Reserva r, String titulo, String introduccion, String urlReserva) {
        String cuerpo = """
                <p>%s</p>
                <table style="width:100%%;border-collapse:collapse;margin:16px 0;font-size:14px">
                  %s %s %s %s %s %s %s
                </table>
                <p style="margin:24px 0"><a href="%s" style="background:#1F3A44;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none">Ver mi reserva</a></p>
                """.formatted(
                esc(introduccion),
                fila("Código", "<b style=\"font-size:18px;letter-spacing:1px\">" + esc(r.getCodigo()) + "</b>"),
                fila("Habitación", esc(r.getTipo().getNombre())),
                fila("Ingreso", fecha(r.getCheckIn()) + " desde las " + esc(hotel.getHoraCheckIn())),
                fila("Egreso", fecha(r.getCheckOut()) + " hasta las " + esc(hotel.getHoraCheckOut())),
                fila("Huéspedes", String.valueOf(r.getCantidadHuespedes())),
                fila("Total", plata(r.totalACobrar())),
                fila("Saldo pendiente", plata(r.saldoPendiente().max(BigDecimal.ZERO))),
                esc(urlReserva));
        return marco(hotel, titulo, cuerpo);
    }

    public String cancelacion(ConfiguracionHotel hotel, Reserva r) {
        BigDecimal reembolso = r.getReembolso() == null ? BigDecimal.ZERO : r.getReembolso();
        String cuerpo = """
                <p>Cancelamos tu reserva <b>%s</b> (%s, del %s al %s).</p>
                <p>%s</p>
                """.formatted(esc(r.getCodigo()), esc(r.getTipo().getNombre()), fecha(r.getCheckIn()), fecha(r.getCheckOut()),
                reembolso.signum() > 0
                        ? "Te vamos a reintegrar <b>" + plata(reembolso) + "</b> por el mismo medio de pago."
                        : "Según la política de cancelación vigente, no corresponde reintegro.");
        return marco(hotel, "Reserva cancelada", cuerpo);
    }

    public String comprobante(ConfiguracionHotel hotel, Reserva r, Pago p) {
        String cuerpo = """
                <p>Registramos un pago para tu reserva <b>%s</b>.</p>
                <table style="width:100%%;border-collapse:collapse;margin:16px 0;font-size:14px">%s %s %s %s %s</table>
                """.formatted(esc(r.getCodigo()),
                fila("Comprobante N.º", esc(p.getReferencia())),
                fila("Concepto", esc(p.getConcepto().name())),
                fila("Medio", esc(p.getMedio().name())),
                fila("Monto", plata(p.getMonto())),
                fila("Saldo pendiente", plata(r.saldoPendiente().max(BigDecimal.ZERO))));
        return marco(hotel, "Comprobante de pago", cuerpo);
    }

    public String recuperacion(ConfiguracionHotel hotel, String nombre, String url) {
        String cuerpo = """
                <p>Hola %s, recibimos un pedido para restablecer tu contraseña.</p>
                <p style="margin:24px 0"><a href="%s" style="background:#1F3A44;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none">Elegir una contraseña nueva</a></p>
                <p style="color:#5C6B70;font-size:13px">El enlace vence en 1 hora. Si no fuiste vos, ignorá este mail.</p>
                """.formatted(esc(nombre), esc(url));
        return marco(hotel, "Restablecer contraseña", cuerpo);
    }

    private String marco(ConfiguracionHotel hotel, String titulo, String cuerpo) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;background:#EEF2F3;padding:24px">
                  <div style="max-width:560px;margin:auto;background:#fff;border-radius:12px;overflow:hidden">
                    <div style="background:#1F3A44;color:#fff;padding:20px 24px;font-size:20px;font-weight:bold">%s</div>
                    <div style="padding:24px;color:#1C2B33;line-height:1.5">
                      <h2 style="margin-top:0;color:#1F3A44">%s</h2>
                      %s
                    </div>
                  </div>
                </div>
                """.formatted(esc(hotel.getNombreHotel()), esc(titulo), cuerpo);
    }

    private String fila(String etiqueta, String valorHtml) {
        return "<tr><td style=\"padding:6px 0;color:#5C6B70\">" + esc(etiqueta) + "</td><td style=\"padding:6px 0;text-align:right\">" + valorHtml + "</td></tr>";
    }

    private String fecha(LocalDate fecha) {
        return esc(FECHA.format(fecha));
    }

    static String plata(BigDecimal monto) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return formato.format(monto);
    }

    private String esc(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto);
    }
}
