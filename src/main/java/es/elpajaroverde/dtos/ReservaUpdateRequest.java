package es.elpajaroverde.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import es.elpajaroverde.services.ReservaReglas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Petición de modificación parcial de una reserva (RF-15).
 *
 * <p>En los PATCH todos los campos son opcionales: un campo ausente (o {@code null})
 * no se modifica. La única excepción es {@code usuario}, donde la distinción
 * ausente/`null` es relevante (RF-15): el deserializador de presencia expone
 * {@link #isUsuarioPresente()} para distinguir "no se toca" de "desvincular".</p>
 */
@JsonDeserialize(using = ReservaUpdateRequest.DeserializadorPresencia.class)
public class ReservaUpdateRequest {

    private LocalDate fechaEntrada;

    private LocalDate fechaSalida;

    @Min(1)
    @Max(value = ReservaReglas.MAX_HUESPEDES)
    private Integer numeroHuespedes;

    private Long usuarioId;

    @Valid
    private ReservaRequest.UsuarioReserva usuario;

    private boolean usuarioPresente;

    public ReservaUpdateRequest() {
    }

    /**
     * Deserializador de presencia: distingue el campo {@code usuario} ausente
     * del {@code usuario:null} (desvincula) y del objeto con datos inline.
     */
    public static class DeserializadorPresencia extends JsonDeserializer<ReservaUpdateRequest> {

        @Override
        public ReservaUpdateRequest deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            JsonNode root = parser.getCodec().readTree(parser);

            ReservaUpdateRequest request = new ReservaUpdateRequest();

            if (root.hasNonNull("fechaEntrada")) {
                request.setFechaEntrada(LocalDate.parse(root.get("fechaEntrada").asText()));
            }
            if (root.hasNonNull("fechaSalida")) {
                request.setFechaSalida(LocalDate.parse(root.get("fechaSalida").asText()));
            }
            if (root.hasNonNull("numeroHuespedes")) {
                request.setNumeroHuespedes(root.get("numeroHuespedes").asInt());
            }
            if (root.hasNonNull("usuarioId")) {
                request.setUsuarioId(root.get("usuarioId").asLong());
            }
            if (root.has("usuario")) {
                request.setUsuarioPresente(true);
                if (!root.get("usuario").isNull()) {
                    request.setUsuario(parser.getCodec().treeToValue(root.get("usuario"),
                            ReservaRequest.UsuarioReserva.class));
                }
            }
            return request;
        }
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

    public Integer getNumeroHuespedes() {
        return numeroHuespedes;
    }

    public void setNumeroHuespedes(Integer numeroHuespedes) {
        this.numeroHuespedes = numeroHuespedes;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public ReservaRequest.UsuarioReserva getUsuario() {
        return usuario;
    }

    public void setUsuario(ReservaRequest.UsuarioReserva usuario) {
        this.usuario = usuario;
    }

    /** {@code true} si el JSON contenía la clave {@code usuario} (aunque sea {@code null}). */
    @JsonIgnore
    public boolean isUsuarioPresente() {
        return usuarioPresente;
    }

    public void setUsuarioPresente(boolean usuarioPresente) {
        this.usuarioPresente = usuarioPresente;
    }
}