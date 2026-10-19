package ar.fiuba.is2.hotel.dominio.politica.cancelacion;

/** Políticas de cancelación disponibles. Cada valor crea su estrategia concreta. */
public enum TipoPoliticaCancelacion {

    FLEXIBLE {
        @Override
        public PoliticaDeCancelacion crear() {
            return new CancelacionFlexible();
        }
    },
    MODERADA {
        @Override
        public PoliticaDeCancelacion crear() {
            return new CancelacionModerada();
        }
    },
    ESTRICTA {
        @Override
        public PoliticaDeCancelacion crear() {
            return new CancelacionEstricta();
        }
    };

    public abstract PoliticaDeCancelacion crear();
}
