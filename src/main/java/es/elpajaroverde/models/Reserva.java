package es.elpajaroverde.models;

import es.elpajaroverde.enums.ReservaEstado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaReserva;

    @Column(nullable = false)
    private LocalDate fechaEntrada;

    @Column(nullable = false)
    private LocalDate fechaSalida;

    @Column(nullable = false)
    private int numeroHuespedes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservaEstado estado = ReservaEstado.CONFIRMADA;

    @Column(nullable = false)
    private boolean visible = true;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Reserva() {
    }

    public Reserva(LocalDate fechaReserva, LocalDate fechaEntrada, LocalDate fechaSalida,
                   int numeroHuespedes, BigDecimal precio, ReservaEstado estado,
                   boolean visible, Usuario usuario) {
        this.fechaReserva = fechaReserva;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.numeroHuespedes = numeroHuespedes;
        this.precio = precio;
        this.estado = estado;
        this.visible = visible;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(LocalDate fechaReserva) {
        this.fechaReserva = fechaReserva;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public int getNumeroHuespedes() {
        return numeroHuespedes;
    }

    public void setNumeroHuespedes(int numeroHuespedes) {
        this.numeroHuespedes = numeroHuespedes;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public ReservaEstado getEstado() {
        return estado;
    }

    public void setEstado(ReservaEstado estado) {
        this.estado = estado;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
