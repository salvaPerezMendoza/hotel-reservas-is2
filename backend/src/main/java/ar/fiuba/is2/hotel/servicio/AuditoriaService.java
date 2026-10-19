package ar.fiuba.is2.hotel.servicio;

import ar.fiuba.is2.hotel.dominio.auditoria.RegistroAuditoria;
import ar.fiuba.is2.hotel.repositorio.AuditoriaRepository;
import ar.fiuba.is2.hotel.seguridad.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Deja registro de las operaciones críticas (RNF-15). */
@Service
public class AuditoriaService {

    private final AuditoriaRepository repositorio;
    private final UsuarioActual usuarioActual;
    private final Clock reloj;

    public AuditoriaService(AuditoriaRepository repositorio, UsuarioActual usuarioActual, Clock reloj) {
        this.repositorio = repositorio;
        this.usuarioActual = usuarioActual;
        this.reloj = reloj;
    }

    @Transactional
    public void registrar(String accion, String entidad, Object entidadId, String detalle) {
        repositorio.save(new RegistroAuditoria(reloj.instant(), usuarioActual.emailOSistema(), accion, entidad,
                entidadId == null ? null : String.valueOf(entidadId), detalle));
    }
}
