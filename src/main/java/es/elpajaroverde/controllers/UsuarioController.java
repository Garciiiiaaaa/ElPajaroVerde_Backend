package es.elpajaroverde.controllers;

import es.elpajaroverde.dtos.UsuarioRequest;
import es.elpajaroverde.dtos.UsuarioResponse;
import es.elpajaroverde.dtos.UsuarioUpdateRequest;
import es.elpajaroverde.dtos.UsuarioVisibleRequest;
import es.elpajaroverde.services.interfaces.IUsuarioService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private static final int TAMANIO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final IUsuarioService usuarioService;

    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResponse> modificar(@PathVariable Long id,
                                                     @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.modificar(id, request));
    }

    @PatchMapping("/{id}/visible")
    public ResponseEntity<UsuarioResponse> cambiarVisible(@PathVariable Long id,
                                                          @Valid @RequestBody UsuarioVisibleRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarVisible(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int tamanio = Math.max(1, Math.min(size, TAMANIO_PAGINA_MAXIMO));
        return ResponseEntity.ok(usuarioService.listar(nombre, apellido, correo, visible,
                PageRequest.of(Math.max(page, 0), tamanio)));
    }
}