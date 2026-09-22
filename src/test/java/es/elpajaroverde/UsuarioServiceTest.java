package es.elpajaroverde;

import es.elpajaroverde.dtos.UsuarioRequest;
import es.elpajaroverde.dtos.UsuarioResponse;
import es.elpajaroverde.dtos.UsuarioUpdateRequest;
import es.elpajaroverde.dtos.UsuarioVisibleRequest;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.exceptions.CorreoDuplicadoException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.mappers.UsuarioMapper;
import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.repositories.UsuarioRepository;
import es.elpajaroverde.services.UsuarioService;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private IAuditoriaService auditoriaService;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, usuarioMapper, auditoriaService);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Usuario usuario(Long id, String correo) {
        Usuario u = new Usuario("Ana", "Lopez", correo, "600000000", true);
        u.setId(id);
        return u;
    }

    private UsuarioResponse respuestaDe(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(u.getId());
        r.setCorreo(u.getCorreo());
        r.setVisible(u.isVisible());
        return r;
    }

    @Test
    void crear_rechazaCorreoDuplicadoCaseInsensitive() {
        when(usuarioRepository.findByCorreoIgnoreCase("ANA@x.com"))
                .thenReturn(Optional.of(usuario(1L, "ana@x.com")));

        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Otra");
        request.setApellido("Persona");
        request.setCorreo("ANA@x.com");

        assertThrows(CorreoDuplicadoException.class, () -> service.crear(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_exitosoAuditaCrear() {
        Usuario entidad = usuario(7L, "ana@x.com");
        when(usuarioRepository.findByCorreoIgnoreCase("ana@x.com")).thenReturn(Optional.empty());
        when(usuarioMapper.toEntity(any(UsuarioRequest.class))).thenReturn(entidad);
        when(usuarioMapper.toResponse(entidad)).thenReturn(respuestaDe(entidad));

        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Ana");
        request.setApellido("Lopez");
        request.setCorreo("ana@x.com");

        UsuarioResponse respuesta = service.crear(request);

        assertEquals(7L, respuesta.getId());
        verify(auditoriaService).registrar(AuditoriaTipoAccion.CREAR, "Usuario", 7L);
    }

    @Test
    void modificar_rechazaCorreoDeOtroUsuario() {
        Usuario propio = usuario(1L, "ana@x.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(propio));
        when(usuarioRepository.findByCorreoIgnoreCase("otro@x.com"))
                .thenReturn(Optional.of(usuario(9L, "otro@x.com")));

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setCorreo("otro@x.com");

        assertThrows(CorreoDuplicadoException.class, () -> service.modificar(1L, request));
    }

    @Test
    void modificar_permiteCorreoPropio() {
        Usuario propio = usuario(1L, "ana@x.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(propio));
        when(usuarioRepository.findByCorreoIgnoreCase("ana@x.com")).thenReturn(Optional.of(propio));
        when(usuarioMapper.toResponse(propio)).thenReturn(respuestaDe(propio));

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setCorreo("ana@x.com");
        request.setTelefono("611111111");

        service.modificar(1L, request);

        verify(auditoriaService).registrar(AuditoriaTipoAccion.MODIFICAR, "Usuario", 1L);
    }

    @Test
    void modificar_404SiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("X");

        assertThrows(EntidadNoEncontradaException.class, () -> service.modificar(99L, request));
    }

    @Test
    void cambiarVisible_sinRestriccionAudita() {
        Usuario u = usuario(1L, "ana@x.com");
        u.setVisible(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioMapper.toResponse(u)).thenAnswer(i -> respuestaDe(u));

        UsuarioVisibleRequest request = new UsuarioVisibleRequest();
        request.setVisible(false);

        UsuarioResponse respuesta = service.cambiarVisible(1L, request);

        assertFalse(respuesta.isVisible());
        verify(usuarioRepository).save(u);
        verify(auditoriaService).registrar(AuditoriaTipoAccion.ELIMINAR_OCULTAR, "Usuario", 1L);
    }

    @Test
    void listar_ordenaPorIdAscPorDefecto() {
        PageImpl<Usuario> pagina = new PageImpl<>(List.of());
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        service.listar(null, null, null, null, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioRepository).findAll(any(Specification.class), captor.capture());
        assertTrue(captor.getValue().getSort().getOrderFor("id").isAscending());
    }

    @Test
    void listar_respetaPageableConcreto() {
        PageRequest concreto = PageRequest.of(1, 5, Sort.by("id"));
        PageImpl<Usuario> pagina = new PageImpl<>(List.of(), concreto, 0);
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        var resultado = service.listar("nom", null, null, true, concreto);

        assertEquals(0, resultado.getTotalElements());
        assertEquals(1, resultado.getNumber());
    }
}