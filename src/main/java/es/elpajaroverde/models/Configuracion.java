package es.elpajaroverde.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "configuracion")
public class Configuracion {

    @Id
    private Long id = 1L;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioNoche;

    @Column(nullable = false)
    private int estanciaMinima;

    @Column(nullable = false)
    private int estanciaMaxima;

    public Configuracion() {
    }

    public Configuracion(BigDecimal precioNoche, int estanciaMinima, int estanciaMaxima) {
        this.id = 1L;
        this.precioNoche = precioNoche;
        this.estanciaMinima = estanciaMinima;
        this.estanciaMaxima = estanciaMaxima;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public int getEstanciaMinima() {
        return estanciaMinima;
    }

    public void setEstanciaMinima(int estanciaMinima) {
        this.estanciaMinima = estanciaMinima;
    }

    public int getEstanciaMaxima() {
        return estanciaMaxima;
    }

    public void setEstanciaMaxima(int estanciaMaxima) {
        this.estanciaMaxima = estanciaMaxima;
    }
}
