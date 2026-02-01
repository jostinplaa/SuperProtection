package com.protectium.command;

import com.protectium.core.Mensajes;
import com.protectium.item.ItemAuthority;
import com.protectium.protection.ProtectionType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /prot dar <tipo> <radio> <jugador>
 * El ÚNICO punto de entrada para crear ítems de protección.
 * Valida todo antes de crear el ítem.
 */
public final class SubDar implements SubComando {

    private final ItemAuthority itemAuthority;
    private final Mensajes mensajes;
    private final org.bukkit.configuration.ConfigurationSection config;

    public SubDar(ItemAuthority itemAuthority, Mensajes mensajes,
                  org.bukkit.configuration.ConfigurationSection config) {
        this.itemAuthority = itemAuthority;
        this.mensajes = mensajes;
        this.config = config;
    }

    @Override
    public String nombre() { return "dar"; }

    @Override
    public String descripcion() { return "Da un ítem de protección a un jugador."; }

    @Override
    public boolean ejecutar(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(mensajes.errorUsaje("/prot dar <tipo> <radio> <jugador>"));
            return false;
        }

        // --- Validar tipo ---
        ProtectionType tipo = ProtectionType.fromString(args[0]);
        if (tipo == null) {
            sender.sendMessage(mensajes.errorTipoInvalido(args[0]));
            return false;
        }

        // Verificar que el tipo está habilitado en config
        var secTipo = config.getConfigurationSection("tipos-proteccion." + tipo.getConfigKey());
        if (secTipo == null || !secTipo.getBoolean("habilitada", false)) {
            sender.sendMessage(mensajes.errorTipoInvalido(args[0] + " (no habilitado)"));
            return false;
        }

        // --- Validar radio ---
        int radio;
        try {
            radio = Integer.parseInt(args[1]);
            if (radio <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(mensajes.errorRadioInvalido(args[1]));
            return false;
        }

        int radioMaximo = secTipo.getInt("radio-maximo", 64);
        if (radio > radioMaximo) {
            sender.sendMessage(mensajes.errorRadioExcede(radio, radioMaximo));
            return false;
        }

        // --- Validar jugador ---
        Player jugador = Bukkit.getPlayer(args[2]);
        if (jugador == null) {
            sender.sendMessage(mensajes.errorJugadorNoEncontrando(args[2]));
            return false;
        }

        // --- Verificar espacio en inventario ---
        ItemStack item = itemAuthority.crearItem(tipo, radio);
        if (jugador.getInventory().firstEmpty() == -1) {
            sender.sendMessage(mensajes.errorInventarioLleno(jugador.getName()));
            return false;
        }

        // --- Dar el ítem ---
        jugador.getInventory().addItem(item);

        // Mensajes de confirmación
        sender.sendMessage(mensajes.exitoItemDado(tipo.getConfigKey(), radio, jugador.getName()));
        jugador.sendMessage(mensajes.getPrefijo() + "§7¡Recibiste un ítem de protección!");
        jugador.sendMessage(mensajes.getPrefijo() + "§7Tipo: §b" + tipo.getConfigKey() + "§7 | Radio: §b" + radio);
        jugador.sendMessage(mensajes.getPrefijo() + "§7Colócalo en el mundo para activar la protección.");

        // Efecto visual en el jugador
        jugador.getWorld().spawnParticle(
            org.bukkit.Particle.SPELL,
            jugador.getLocation(),
            30, 0.3, 0.5, 0.3, 0.05
        );

        return true;
    }

    @Override
    public List<String> autocompletar(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Autocompletar tipos
            String prefijo = args[0].toLowerCase();
            return Arrays.stream(ProtectionType.names())
                    .filter(n -> n.startsWith(prefijo))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            // Sugerir radios comunes
            return List.of("4", "8", "12", "16", "24", "32");
        }
        if (args.length == 3) {
            // Autocompletar jugadores online
            String prefijo = args[2].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefijo))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
