package com.protectium.command;

import com.protectium.core.Mensajes;
import com.protectium.fx.FxEngine;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * /prot recargar
 * Recarga la configuración del plugin sin reiniciar el servidor.
 * Las protecciones activas siguen activas (existen mientras el bloque esté).
 */
public final class SubRecargar implements SubComando {

    private final Plugin plugin;
    private final Mensajes mensajes;
    private final FxEngine fxEngine;

    public SubRecargar(Plugin plugin, Mensajes mensajes, FxEngine fxEngine) {
        this.plugin = plugin;
        this.mensajes = mensajes;
        this.fxEngine = fxEngine;
    }

    @Override
    public String nombre() { return "recargar"; }

    @Override
    public String descripcion() { return "Recarga la configuración del plugin."; }

    @Override
    public boolean ejecutar(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        fxEngine.clearCache();
        sender.sendMessage(mensajes.exitoRecargar());
        return true;
    }

    @Override
    public List<String> autocompletar(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
