package com.protectium.command;

import com.protectium.core.Mensajes;
import com.protectium.item.ItemAuthority;
import com.protectium.protection.ProtectionType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * /prot crear <radio> <nombre>
 * Convierte CUALQUIER BLOQUE en la mano del jugador en un ítem de protección.
 */
public final class SubCrear implements SubComando {

    private final ItemAuthority itemAuthority;
    private final Mensajes mensajes;

    public SubCrear(ItemAuthority itemAuthority, Mensajes mensajes) {
        this.itemAuthority = itemAuthority;
        this.mensajes = mensajes;
    }

    @Override
    public String nombre() {
        return "crear";
    }

    @Override
    public String descripcion() {
        return "Convierte el bloque en mano en una proteccion con nombre";
    }

    @Override
    public boolean ejecutar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage(mensajes.getPrefijoError() + "Solo jugadores pueden usar este comando.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(mensajes.getPrefijoError() + "Uso: /prot crear <radio> <nombre>");
            return false;
        }

        // Parsear radio
        int radio;
        try {
            radio = Integer.parseInt(args[0]);
            if (radio < 1 || radio > 50) {
                jugador.sendMessage(mensajes.getPrefijoError() + "El radio debe ser entre 1 y 50.");
                return false;
            }
        } catch (NumberFormatException e) {
            jugador.sendMessage(mensajes.getPrefijoError() + "Radio invalido: " + args[0]);
            return false;
        }

        // Construir nombre
        StringBuilder nombreBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1)
                nombreBuilder.append(" ");
            nombreBuilder.append(args[i]);
        }
        String nombreProteccion = nombreBuilder.toString().trim();

        if (nombreProteccion.isEmpty()) {
            jugador.sendMessage(mensajes.getPrefijoError() + "Debes especificar un nombre para la proteccion.");
            return false;
        }

        if (nombreProteccion.length() > 32) {
            jugador.sendMessage(mensajes.getPrefijoError() + "El nombre no puede tener mas de 32 caracteres.");
            return false;
        }

        // Verificar que tiene un ítem en la mano
        ItemStack itemEnMano = jugador.getInventory().getItemInMainHand();
        if (itemEnMano.getType() == Material.AIR) {
            jugador.sendMessage(mensajes.getPrefijoError() + "Debes tener un bloque en la mano!");
            return false;
        }

        // Verificar que es un BLOQUE (no comida, herramientas, etc.)
        if (!itemEnMano.getType().isBlock()) {
            jugador.sendMessage(mensajes.getPrefijoError() + "Solo puedes usar BLOQUES, no items!");
            return false;
        }

        // Crear el ítem de protección usando el material que tiene en mano
        Material materialOriginal = itemEnMano.getType();
        ItemStack itemProteccion = itemAuthority.crearItemConNombre(ProtectionType.AREA, radio, nombreProteccion,
                materialOriginal);
        jugador.getInventory().setItemInMainHand(itemProteccion);

        jugador.sendMessage(
                mensajes.getPrefijo() + "Proteccion creada: " + nombreProteccion + " (radio: " + radio + ")");
        jugador.sendMessage(mensajes.getPrefijo() + "Coloca el bloque para activar la proteccion.");
        return true;
    }

    @Override
    public List<String> autocompletar(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("5", "10", "15", "20");
        }
        if (args.length == 2) {
            return List.of("MiCasa", "Granja", "Base", "Tienda");
        }
        return Collections.emptyList();
    }
}
