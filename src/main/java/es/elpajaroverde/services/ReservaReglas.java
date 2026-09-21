package es.elpajaroverde.services;

/**
 * Constantes de negocio del dominio de reservas.
 * Fuente única de las reglas fijas del producto (RF-12).
 */
public final class ReservaReglas {

    private ReservaReglas() {
    }

    /** Tope de huéspedes por reserva (la casa se alquila completa). */
    public static final int MAX_HUESPEDES = 10;
}