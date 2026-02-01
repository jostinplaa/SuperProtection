package com.protectium.command;

import org.bukkit.command.CommandSender;

/**
 * Interfaz que implementa cada subcomando.
 * El comando raíz hace dispatch según el primer argumento
 * y delega la ejecución completa al SubComando correspondiente.
 */
public interface SubComando {

    /** Nombre del subcomando (lowercase). */
    String nombre();

    /** Descripción corta para la ayuda. */
    String descripcion();

    /** Ejecuta el subcomando. Retorna false si el uso es incorrecto. */
    boolean ejecutar(CommandSender sender, String[] args);

    /** Autocompletar argumentos. */
    default java.util.List<String> autocompletar(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
