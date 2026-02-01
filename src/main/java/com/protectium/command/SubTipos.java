package com.protectium.command;

import com.protectium.core.Mensajes;
import com.protectium.gui.GuiManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /prot tipos
 * Abre el GUI informativo de tipos de protección disponibles.
 */
public final class SubTipos implements SubComando {

    private final GuiManager guiManager;
    private final Mensajes mensajes;

    public SubTipos(GuiManager guiManager, Mensajes mensajes) {
        this.guiManager = guiManager;
        this.mensajes = mensajes;
    }

    @Override
    public String nombre() { return "tipos"; }

    @Override
    public String descripcion() { return "Ver los tipos de protección disponibles (GUI)."; }

    @Override
    public boolean ejecutar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            // Si es consola, manda texto plano
            sender.sendMessage(mensajes.tiposDisponibles(
                String.join(", ", com.protectium.protection.ProtectionType.names())));
            return true;
        }

        guiManager.abrirTipos(jugador);
        return true;
    }

    @Override
    public List<String> autocompletar(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
