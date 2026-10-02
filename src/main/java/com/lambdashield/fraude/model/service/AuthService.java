package com.lambdashield.fraude.model.service;

import com.lambdashield.fraude.model.store.UsuarioStore;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UsuarioStore usuarioStore;

    public AuthService(UsuarioStore usuarioStore) {
        this.usuarioStore = usuarioStore;
    }

    public boolean validar(String usuario, String clave) {
        return usuarioStore.validar(usuario, clave).isPresent();
    }

    public Optional<UsuarioStore.UsuarioInfo> obtenerUsuarioInfo(String usuario) {
        return usuarioStore.buscarPorUsuario(usuario);
    }

    public String obtenerNombreUsuario(String usuario) {
        return usuarioStore.buscarPorUsuario(usuario)
                .map(UsuarioStore.UsuarioInfo::nombreCompleto)
                .orElse(usuario);
    }

    public String obtenerRol(String usuario) {
        return usuarioStore.buscarPorUsuario(usuario)
                .map(UsuarioStore.UsuarioInfo::rol)
                .orElse("CLIENTE");
    }
}
