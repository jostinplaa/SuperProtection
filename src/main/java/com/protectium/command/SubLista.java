package com.protectium.command;

import com.protectium.core.Mensajes;
import com.protectium.gui.GuiManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /prot lista
 * Abre el GUI interactivo de protecciones activas.
 */
public final class SubLista implements SubComando {

    private final GuiManager guiManager;
    private final Mensajes mensajes;

    public SubLista(GuiManager guiManager, Mensajes mensajes) {
        this.guiManager = guiManager;
        this.mensajes = mensajes;
    }

    @Override
    public String nombre() { return "lista"; }

    @Override
    public String descripcion() { return "Ver todas las protecciones activas (GUI)."; }

    @Override
    public boolean ejecutar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage(mensajes.errorUsaje("Este comando solo puede ser usado por jugadores."));
            return false;
        }

        guiManager.abrirLista(jugador);
        return true;
    }

    @Override
    public List<String> autocompletar(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
