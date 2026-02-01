package com.protectium.command;

import com.protectium.core.Mensajes;
import com.protectium.core.ProtectiumPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class SubTienda implements SubComando {

    private final ProtectiumPlugin plugin;
    private final Mensajes mensajes;

    public SubTienda(ProtectiumPlugin plugin, Mensajes mensajes) {
        this.plugin = plugin;
        this.mensajes = mensajes;
    }

    @Override
    public String nombre() {
        return "tienda";
    }

    @Override
    public String descripcion() {
        return "Abre la tienda de protecciones.";
    }

    @Override
    public boolean ejecutar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mensajes.errorUsaje("Solo jugadores."));
            return false;
        }

        plugin.getGuiManager().abrirTienda(player, plugin.getShopManager());
        return true;
    }

    @Override
    public List<String> autocompletar(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
