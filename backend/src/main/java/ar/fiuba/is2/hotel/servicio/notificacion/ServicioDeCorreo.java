package ar.fiuba.is2.hotel.servicio.notificacion;

import ar.fiuba.is2.hotel.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envía mails por SMTP. Si el envío está deshabilitado (o falla), el mail se
 * escribe en el log: nunca se interrumpe una reserva porque falle el correo.
 */
@Service
public class ServicioDeCorreo {

    private static final Logger log = LoggerFactory.getLogger(ServicioDeCorreo.class);

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public ServicioDeCorreo(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void enviar(String destinatario, String asunto, String html) {
        if (!props.mail().habilitado()) {
            log.info("[MAIL simulado] Para: {} | Asunto: {}", destinatario, asunto);
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(props.mail().remitente());
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mensaje);
            log.info("Mail enviado a {}: {}", destinatario, asunto);
        } catch (Exception e) {
            log.warn("No se pudo enviar el mail a {} ({}): {}", destinatario, asunto, e.getMessage());
        }
    }
}
