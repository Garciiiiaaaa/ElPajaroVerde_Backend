package es.elpajaroverde.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/**
 * Petición de modificación parcial de la Configuración (RF-35, RF-36).
 * Todos los campos son opcionales (PATCH parcial); la regla cruzada
 * {@code estanciaMinima <= estanciaMaxima} se valida en el Service.
 */
public class ConfiguracionRequest {

    @DecimalMin("0.01")
    private BigDecimal precioNoche;

    @Min(1)
    private Integer estanciaMinima;

    @Min(1)
    private Integer estanciaMaxima;

    public ConfiguracionRequest() {
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public Integer getEstanciaMinima() {
        return estanciaMinima;
    }

    public void setEstanciaMinima(Integer estanciaMinima) {
        this.estanciaMinima = estanciaMinima;
    }

    public Integer getEstanciaMaxima() {
        return estanciaMaxima;
    }

    public void setEstanciaMaxima(Integer estanciaMaxima) {
        this.estanciaMaxima = estanciaMaxima;
    }
}