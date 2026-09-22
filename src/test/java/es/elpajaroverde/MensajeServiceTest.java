package es.elpajaroverde;

import es.elpajaroverde.dtos.MensajeRequest;
import es.elpajaroverde.dtos.MensajeResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.enums.MensajeRemitente;
import es.elpajaroverde.mappers.MensajeMapper;
import es.elpajaroverde.models.Mensaje;
import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.repositories.MensajeRepository;
import es.elpajaroverde.repositories.UsuarioRepository;
import es.elpajaroverde.services.MensajeService;
import es.elpajaroverde.services.interfaces.IAuditoriaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MensajeServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private MensajeMapper mensajeMapper;

    @Mock
    private IAuditoriaService auditoriaService;

    private MensajeService service;

    private final Usuario usuario = nuevoUsuario(1L, "ana@x.com");

    @BeforeEach
    void setUp() {
        service = new MensajeService(usuarioRepository, mensajeRepository, mensajeMapper, auditoriaService);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Usuario nuevoUsuario(Long id, String correo) {
        Usuario u = new Usuario("Ana", "Lopez", correo, "600000000", true);
        u.setId(id);
        return u;
    }

    private MensajeRequest request(String nombre, String apellido, String correo, String asunto) {
        MensajeRequest r = new MensajeRequest();
        r.setNombre(nombre);
        r.setApellido(apellido);
        r.setCorreo(correo);
        r.setMensaje("Hola, quiero información");
        r.setAsunto(asunto);
        return r;
    }

    private MensajeResponse respuesta() {
        MensajeResponse r = new MensajeResponse();
        r.setId(10L);
        r.setAsunto("Consulta");
        return r;
    }

    @Test
    void enviar_asuntoPorDefectoConsulta() {
        when(usuarioRepository.findByCorreoIgnoreCase("ana@x.com")).thenReturn(Optional.of(usuario));
        when(mensajeMapper.toResponse(any())).thenReturn(respuesta());

        MensajeResponse respuesta = service.enviar(request("Ana", "Lopez", "ana@x.com", null));

        ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(captor.capture());
        assertEquals("Consulta", captor.getValue().getAsunto());
        assertEquals(MensajeRemitente.USUARIO, captor.getValue().getRemitente());
        assertNotNull(respuesta);
    }

    @Test
    void enviar_asuntoEnviadoSeUsa() {
        when(usuarioRepository.findByCorreoIgnoreCase("ana@x.com")).thenReturn(Optional.of(usuario));
        when(mensajeMapper.toResponse(any())).thenReturn(respuesta());

        service.enviar(request("Ana", "Lopez", "ana@x.com", "Oferta de temporada"));

        ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(captor.capture());
        assertEquals("Oferta de temporada", captor.getValue().getAsunto());
    }

    @Test
    void enviar_dedupeCaseInsensitiveReutilizaUsuarioOculto() {
        when(usuarioRepository.findByCorreoIgnoreCase("Ana@x.com")).thenReturn(Optional.of(usuario));
        when(mensajeMapper.toResponse(any())).thenReturn(respuesta());

        service.enviar(request(null, null, "Ana@x.com", null));

        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(AuditoriaTipoAccion.class), anyString(), anyLong());
        ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(captor.capture());
        assertEquals(usuario, captor.getValue().getUsuario());
    }

    @Test
    void enviar_creaUsuarioConPlaceholdersYAuda() {
        when(usuarioRepository.findByCorreoIgnoreCase("nuevo@x.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(9L);
            return u;
        });
        when(mensajeMapper.toResponse(any())).thenReturn(respuesta());

        service.enviar(request("", null, "nuevo@x.com", null));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("Sin nombre", captor.getValue().getNombre());
        assertEquals("Sin apellido", captor.getValue().getApellido());
        assertTrue(captor.getValue().isVisible());
        verify(auditoriaService).registrar(AuditoriaTipoAccion.CREAR, "Usuario", 9L);
    }

    @Test
    void listar_ordenaPorFechaMensajeDescPorDefecto() {
        PageImpl<Mensaje> pagina = new PageImpl<>(List.of());
        when(mensajeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        service.listar(1L, null, null, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(mensajeRepository).findAll(any(Specification.class), captor.capture());
        assertTrue(captor.getValue().getSort().getOrderFor("fechaMensaje").isDescending());
    }
}