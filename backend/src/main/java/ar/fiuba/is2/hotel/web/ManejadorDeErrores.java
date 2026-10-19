package ar.fiuba.is2.hotel.web;

import ar.fiuba.is2.hotel.dominio.excepciones.NoEncontradoException;
import ar.fiuba.is2.hotel.dominio.excepciones.ReglaDeNegocioException;
import ar.fiuba.is2.hotel.dominio.excepciones.SinDisponibilidadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce las excepciones a respuestas HTTP con mensajes claros para el usuario. */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    public record Error(Instant fecha, int estado, String mensaje, Map<String, String> campos) {
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Error> reglaDeNegocio(ReglaDeNegocioException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null);
    }

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Error> noEncontrado(NoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(SinDisponibilidadException.class)
    public ResponseEntity<Error> sinDisponibilidad(SinDisponibilidadException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    /**
     * Si dos personas reservan exactamente al mismo tiempo, la restricción de la
     * base de datos rechaza la segunda reserva: acá se convierte en un mensaje claro.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Error> integridad(DataIntegrityViolationException e) {
        String detalle = String.valueOf(e.getMostSpecificCause().getMessage());
        if (detalle.contains("reservas_sin_superposicion")) {
            return respuesta(HttpStatus.CONFLICT,
                    "La habitación acaba de ser reservada por otra persona para esas fechas. Probá de nuevo.", null);
        }
        if (detalle.contains("temporadas_sin_superposicion")) {
            return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "La temporada se superpone con otra existente", null);
        }
        log.warn("Violación de integridad: {}", detalle);
        return respuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes", null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Error> concurrencia(ObjectOptimisticLockingFailureException e) {
        return respuesta(HttpStatus.CONFLICT, "La reserva fue modificada por otra persona. Actualizá la página.", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String mensaje = campos.values().stream().findFirst().orElse("Revisá los datos ingresados");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Error> pedidoInvalido(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen el formato esperado", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Error> accesoDenegado(AccessDeniedException e) {
        return respuesta(HttpStatus.FORBIDDEN, "No tenés permiso para hacer esto", null);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Error> recursoInexistente(Exception e) {
        return respuesta(HttpStatus.NOT_FOUND, "No existe ese recurso", null);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Error> metodoNoSoportado(Exception e) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Operación no soportada", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá de nuevo en unos minutos.", null);
    }

    private ResponseEntity<Error> respuesta(HttpStatus estado, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(estado).body(new Error(Instant.now(), estado.value(), mensaje, campos));
    }
}
