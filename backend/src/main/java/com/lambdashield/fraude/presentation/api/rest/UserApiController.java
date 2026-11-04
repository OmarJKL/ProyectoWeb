package com.lambdashield.fraude.presentation.api.rest;

import com.lambdashield.fraude.application.command.UsuarioActualizarCommand;
import com.lambdashield.fraude.application.command.UsuarioCrearCommand;
import com.lambdashield.fraude.application.service.UsuarioAdminService;
import com.lambdashield.fraude.presentation.dto.request.UsuarioActualizarRequest;
import com.lambdashield.fraude.presentation.dto.request.UsuarioClaveRequest;
import com.lambdashield.fraude.presentation.dto.request.UsuarioCrearRequest;
import com.lambdashield.fraude.presentation.dto.response.UsuarioResponse;
import com.lambdashield.fraude.presentation.mapper.UsuarioResponseMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller para /api/v1/user: gestión de usuarios del sistema.
 * Es la única ruta que admite DELETE (baja física de cuentas); las
 * evaluaciones conservan la baja lógica con PATCH.
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserApiController {

    private final UsuarioAdminService usuarioAdminService;
    private final UsuarioResponseMapper mapper;

    public UserApiController(UsuarioAdminService usuarioAdminService, UsuarioResponseMapper mapper) {
        this.usuarioAdminService = usuarioAdminService;
        this.mapper = mapper;
    }

    /** Lista los usuarios del sistema con su rol, estado y cliente asociado. */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> list() {
        return ResponseEntity.ok(mapper.toResponses(usuarioAdminService.listar()));
    }

    /** Devuelve un usuario por su identificador. */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> get(@PathVariable @Positive(message = "El identificador debe ser positivo") Long id) {
        return ResponseEntity.ok(mapper.toResponse(usuarioAdminService.buscar(id)));
    }

    /** Crea un usuario con rol, estado ACTIVO y clave cifrada con BCrypt. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioCrearRequest request) {
        UsuarioResponse creado = mapper.toResponse(usuarioAdminService.crear(new UsuarioCrearCommand(
                request.usuario(), request.nombres(), request.correo(),
                request.clave(), request.rol(), request.codigoCliente())));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** Actualiza nombres, correo, estado, rol y cliente asociado. */
    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResponse> update(
            @PathVariable @Positive(message = "El identificador debe ser positivo") Long id,
            @Valid @RequestBody UsuarioActualizarRequest request) {
        return ResponseEntity.ok(mapper.toResponse(usuarioAdminService.actualizar(id, new UsuarioActualizarCommand(
                request.nombres(), request.correo(), request.estado(), request.rol(), request.codigoCliente()))));
    }

    /** Cambia la clave de un usuario; se almacena cifrada con BCrypt. */
    @PatchMapping("/{id}/clave")
    public ResponseEntity<Void> changePassword(
            @PathVariable @Positive(message = "El identificador debe ser positivo") Long id,
            @Valid @RequestBody UsuarioClaveRequest request) {
        usuarioAdminService.cambiarClave(id, request.clave());
        return ResponseEntity.noContent().build();
    }

    /** Da de baja física un usuario y sus asignaciones de rol. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "El identificador debe ser positivo") Long id,
            @SessionAttribute(value = "usuario", required = false) String usuarioActual) {
        usuarioAdminService.eliminar(id, usuarioActual);
        return ResponseEntity.noContent().build();
    }
}
