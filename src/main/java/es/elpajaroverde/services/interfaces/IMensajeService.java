package es.elpajaroverde.services.interfaces;

import es.elpajaroverde.dtos.MensajeRequest;
import es.elpajaroverde.dtos.MensajeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface IMensajeService {

    MensajeResponse enviar(MensajeRequest request);

    Page<MensajeResponse> listar(Long usuarioId, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);
}