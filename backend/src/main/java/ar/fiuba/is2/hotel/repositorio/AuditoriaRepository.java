package ar.fiuba.is2.hotel.repositorio;

import ar.fiuba.is2.hotel.dominio.auditoria.RegistroAuditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    List<RegistroAuditoria> findAllByOrderByFechaDesc(Pageable pageable);

    List<RegistroAuditoria> findByEntidadAndEntidadIdOrderByFechaAsc(String entidad, String entidadId);
}
