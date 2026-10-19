package ar.fiuba.is2.hotel.dominio.politica.precio;

/** Políticas de precio disponibles. Cada valor sabe crear su estrategia concreta. */
public enum TipoPoliticaPrecio {

    POR_TEMPORADA {
        @Override
        public PoliticaDePrecio crear() {
            return new PrecioPorTemporada();
        }
    },
    TEMPORADA_CON_DESCUENTO {
        @Override
        public PoliticaDePrecio crear() {
            return new PrecioConDescuentoPorEstadiaLarga();
        }
    };

    public abstract PoliticaDePrecio crear();
}
