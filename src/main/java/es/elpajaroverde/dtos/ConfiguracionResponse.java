package es.elpajaroverde.dtos;

import java.math.BigDecimal;

/**
 * Configuración vigente devuelta al administrador (RF-34).
 */
public class ConfiguracionResponse {

    private Long id;

    private BigDecimal precioNoche;

    private int estanciaMinima;

    private int estanciaMaxima;

    public ConfiguracionResponse() {
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