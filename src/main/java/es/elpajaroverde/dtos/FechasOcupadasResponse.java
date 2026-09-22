package es.elpajaroverde.dtos;

import java.util.List;

/**
 * Respuesta de la consulta pública de disponibilidad (RF-1).
 */
public class FechasOcupadasResponse {

    private List<FechasOcupadas> ocupadas;

    public FechasOcupadasResponse() {
    }

    public FechasOcupadasResponse(List<FechasOcupadas> ocupadas) {
        this.ocupadas = ocupadas;
    }

    public List<FechasOcupadas> getOcupadas() {
        return ocupadas;
    }

    public void setOcupadas(List<FechasOcupadas> ocupadas) {
        this.ocupadas = ocupadas;
    }
}