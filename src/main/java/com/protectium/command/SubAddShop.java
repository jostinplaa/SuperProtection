package com.protectium.command;

import com.protectium.core.Mensajes;
import com.protectium.core.ProtectiumPlugin;
import com.protectium.shop.ShopManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public final class SubAddShop implements SubComando {

    private final ProtectiumPlugin plugin;
    private final Mensajes mensajes;

    public SubAddShop(ProtectiumPlugin plugin, Mensajes mensajes) {
        this.plugin = plugin;
        this.mensajes = mensajes;
    }

    @Override
    public String nombre() {
        return "addshop";
    }

    @Override
    public String descripcion() {
        return "Agrega el ítem en mano a la tienda.";
    }

    @Override
    public boolean ejecutar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mensajes.errorUsaje("Solo jugadores."));
            return false;
        }

        if (!player.hasPermission("protectium.admin")) {
            player.sendMessage("§cNo tienes permiso.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(mensajes.errorUsaje("/prot addshop <precio>"));
            return false;
        }

        double precio;
        try {
            precio = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cPrecio inválido.");
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cDebes tener un ítem en la mano.");
            return true;
        }

        ShopManager shop = plugin.getShopManager();
        String id = "shop_" + System.currentTimeMillis();
        shop.agregarItem(id, item, precio);

        player.sendMessage(mensajes.exitoAddShop(String.valueOf(precio)));
        return true;
    }

    @Override
    public List<String> autocompletar(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
