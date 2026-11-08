package com.clickobs.controller;

import com.clickobs.dao.UsuarioDao;
import com.clickobs.excepcion.PersistenciaException;
import com.clickobs.model.Usuario;
import java.util.Optional;

/** Controlador de la pantalla de inicio de sesion (CU-01): recibe el nombre que escribio el usuario y consulta al modelo. */
public class LoginController {

    private final UsuarioDao usuarios = new UsuarioDao();

    /** Devuelve el usuario si existe en la base (la contrasena se verificara en la fase de Construccion final). */
    public Optional<Usuario> autenticar(String nombre) throws PersistenciaException {
        return usuarios.buscarPorNombre(nombre);
    }
}
