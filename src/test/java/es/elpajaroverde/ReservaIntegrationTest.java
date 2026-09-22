package es.elpajaroverde;

import com.fasterxml.jackson.databind.JsonNode;
import es.elpajaroverde.dtos.ReservaCambioEstadoRequest;
import es.elpajaroverde.dtos.ReservaRequest;
import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.exceptions.SolapamientoReservaException;
import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.services.interfaces.IReservaService;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservaIntegrationTest extends BaseIntegracion {

    @Autowired
    private IReservaService reservaService;

    @Test
    void crearReservaConUsuarioInlineCalculaPrecio() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-12\","
                                + "\"numeroHuespedes\":2,\"usuario\":{\"nombre\":\"Ana\",\"apellido\":\"Lopez\","
                                + "\"correo\":\"resa@x.com\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precio").value(100.00))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.visible").value(true))
                .andExpect(jsonPath("$.usuario.correo").value("resa@x.com"));
    }

    @Test
    void crearReservaSolapadaDevuelve409() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva("2026-10-10", "2026-10-15", "", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva("2026-10-14", "2026-10-17", "", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Las fechas solicitadas solapan con otra reserva"));
    }

    @Test
    void crearReservaUsuarioIdInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-12\","
                                + "\"numeroHuespedes\":2,\"usuarioId\":99999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void modificarCambiaFechasYRecalculaPrecio() throws Exception {
        Long id = crearSolo("", "2026-10-10", "2026-10-12");

        mockMvc.perform(patch("/api/v1/reservas/" + id)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(250.00));
    }

    @Test
    void modificarPermiteSolapeConsigoMismo() throws Exception {
        Long id = crearSolo("", "2026-10-10", "2026-10-12");

        mockMvc.perform(patch("/api/v1/reservas/" + id)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-12\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void modificarDesvinculaUsuario() throws Exception {
        Long id = crearSolo("", "2026-10-10", "2026-10-12");

        mockMvc.perform(patch("/api/v1/reservas/" + id)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":null}"))
                .andExpect(status().isOk());

        assertNull(reservaRepository.findById(id).orElseThrow().getUsuario());
    }

    @Test
    void cambiarEstadoRespetaDuración() throws Exception {
        Long id = crearSolo("", "2026-10-10", "2026-10-12");

        mockMvc.perform(patch("/api/v1/reservas/" + id + "/estado")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mockMvc.perform(patch("/api/v1/reservas/" + id + "/estado")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void reactivarReservaCanceladaConSolapeDevuelve409() throws Exception {
        Long cancelada = crearReserva("2026-10-10", "2026-10-15", "", null);
        cancelar(cancelada);
        crearReserva("2026-10-12", "2026-10-17", "", null);

        mockMvc.perform(patch("/api/v1/reservas/" + cancelada + "/estado")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Las fechas solicitadas solapan con otra reserva"));
    }

    @Test
    void ocultarReservaActivaFuturaDevuelve400() throws Exception {
        Long id = crearSolo("", "2026-10-10", "2026-10-12");

        mockMvc.perform(patch("/api/v1/reservas/" + id + "/visible")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solo puede ocultarse una reserva cancelada o pasada"));
    }

    @Test
    void listarFiltraPorEstadoVisibleYFechas() throws Exception {
        Long visible = crearSolo("", "2026-10-10", "2026-10-12");
        Long ocultable = crearSolo("", "2026-11-01", "2026-11-03");

        mockMvc.perform(patch("/api/v1/reservas/" + ocultable + "/estado")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/reservas/" + ocultable + "/visible")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reservas").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(visible));

        mockMvc.perform(get("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .queryParam("visible", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ocultable));

        mockMvc.perform(get("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .queryParam("estado", "CANCELADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .queryParam("estado", "CANCELADA")
                        .queryParam("visible", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ocultable));

        String porFechas = mockMvc.perform(get("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .queryParam("fechaDesde", "2026-10-01")
                        .queryParam("fechaHasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(porFechas).get("content");
        assertEquals(1, content.size());
        assertEquals(visible, content.get(0).get("id").asLong());
    }

    @Test
    void listarPaginaFueraDeRangoDevuelveVacia() throws Exception {
        crearSolo("", "2026-10-10", "2026-10-12");

        mockMvc.perform(get("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .queryParam("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void crearReservaDatosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-10\","
                                + "\"numeroHuespedes\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dosCreacionesSimultaneasSoloUnaProspera() throws Exception {
        Usuario usuario = usuarioRepository.save(
                new Usuario("Ana", "Lopez", "concur@x.com", null, true));

        ReservaRequest request = new ReservaRequest();
        request.setFechaEntrada(LocalDate.of(2026, 10, 10));
        request.setFechaSalida(LocalDate.of(2026, 10, 12));
        request.setNumeroHuespedes(2);
        request.setUsuarioId(usuario.getId());

        Set<String> resultados = ConcurrentHashMap.newKeySet();
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch lanzar = new CountDownLatch(1);

        Runnable tarea = () -> {
            listos.countDown();
            try {
                lanzar.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                reservaService.crear(request);
                resultados.add("OK");
            } catch (SolapamientoReservaException e) {
                resultados.add("SOLAPE");
            } catch (RuntimeException e) {
                resultados.add("FALLO");
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(tarea);
        pool.submit(tarea);
        listos.await(5, TimeUnit.SECONDS);
        lanzar.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        assertEquals(Set.of("OK", "SOLAPE"), resultados);
        assertEquals(1, reservaRepository.count());
    }

    @Test
    void dosReactivacionesSimultaneasSoloUnaProspera() throws Exception {
        Long a = crearReserva("2026-10-10", "2026-10-15", "", null);
        cancelar(a);
        Long b = crearReserva("2026-10-12", "2026-10-17", "", null);
        cancelar(b);

        ReservaCambioEstadoRequest confirmar = new ReservaCambioEstadoRequest();
        confirmar.setEstado(ReservaEstado.CONFIRMADA);

        Set<String> resultados = ConcurrentHashMap.newKeySet();
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch lanzar = new CountDownLatch(1);

        Runnable tareaA = () -> operacion(resultados, listos, lanzar,
                () -> reservaService.cambiarEstado(a, confirmar));
        Runnable tareaB = () -> operacion(resultados, listos, lanzar,
                () -> reservaService.cambiarEstado(b, confirmar));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(tareaA);
        pool.submit(tareaB);
        listos.await(5, TimeUnit.SECONDS);
        lanzar.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        assertEquals(Set.of("OK", "SOLAPE"), resultados);
        assertEquals(1, reservaRepository.findAll()
                .stream().filter(r -> r.getEstado() == ReservaEstado.CONFIRMADA).count());
    }

    private void operacion(Set<String> resultados, CountDownLatch listos, CountDownLatch lanzar,
                           Operacion op) {
        listos.countDown();
        try {
            lanzar.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            op.ejecutar();
            resultados.add("OK");
        } catch (SolapamientoReservaException e) {
            resultados.add("SOLAPE");
        } catch (RuntimeException e) {
            resultados.add("FALLO");
        }
    }

    @FunctionalInterface
    private interface Operacion {
        void ejecutar();
    }

    private void cancelar(Long id) throws Exception {
        mockMvc.perform(patch("/api/v1/reservas/" + id + "/estado")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());
    }

    private Long crearSolo(String correo, String entrada, String salida) throws Exception {
        return crearReserva(entrada, salida, correo.isEmpty() ? "" : correo, null);
    }

    private Long crearReserva(String entrada, String salida, String correoInline, String usuarioId)
            throws Exception {
        String body = mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva(entrada, salida, correoInline, usuarioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String jsonReserva(String entrada, String salida, String correoInline, String usuarioId) {
        String ext = "";
        if (usuarioId != null) {
            ext = ",\"usuarioId\":" + usuarioId;
        } else if (correoInline != null && !correoInline.isEmpty()) {
            ext = ",\"usuario\":{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"" + correoInline + "\"}";
        }
        return "{\"fechaEntrada\":\"" + entrada + "\",\"fechaSalida\":\"" + salida
                + "\",\"numeroHuespedes\":2" + ext + "}";
    }
}