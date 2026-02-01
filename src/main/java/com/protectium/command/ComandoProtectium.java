package com.protectium.command;

import com.protectium.core.Mensajes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comando raíz /prot. Hace dispatch a SubComandos registrados.
 * Si no hay subcomando, muestra la ayuda automática.
 */
public final class ComandoProtectium implements CommandExecutor, TabCompleter {

    private final Map<String, SubComando> subComandos = new LinkedHashMap<>();
    private final Mensajes mensajes;

    public ComandoProtectium(Mensajes mensajes) {
        this.mensajes = mensajes;
    }

    /** Registra un subcomando por su nombre. */
    public void registrar(SubComando sub) {
        subComandos.put(sub.nombre().toLowerCase(), sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("protectium.admin")) {
            sender.sendMessage(mensajes.errorSinPermisos());
            return true;
        }

        if (args.length == 0) {
            mostrarAyuda(sender);
            return true;
        }

        String nombre = args[0].toLowerCase();
        SubComando sub = subComandos.get(nombre);

        if (sub == null) {
            sender.sendMessage(mensajes.errorUsaje("/prot <subcomando>"));
            mostrarAyuda(sender);
            return true;
        }

        // Pasa el resto de args sin el nombre del subcomando
        String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
        sub.ejecutar(sender, restArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("protectium.admin")) return Collections.emptyList();

        // Primer argumento: nombres de subcomandos
        if (args.length <= 1) {
            String prefijo = args.length == 1 ? args[0].toLowerCase() : "";
            return subComandos.keySet().stream()
                    .filter(n -> n.startsWith(prefijo))
                    .collect(Collectors.toList());
        }

        // Argumentos siguientes: delega al subcomando
        String nombre = args[0].toLowerCase();
        SubComando sub = subComandos.get(nombre);
        if (sub == null) return Collections.emptyList();

        String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.autocompletar(sender, restArgs);
    }

    private void mostrarAyuda(CommandSender sender) {
        sender.sendMessage(mensajes.getSeparador());
        sender.sendMessage(mensajes.getPrefijo() + "§7Subcomandos disponibles:");
        for (SubComando sub : subComandos.values()) {
            sender.sendMessage("§8  /prot " + sub.nombre() + "  §7— " + sub.descripcion());
        }
        sender.sendMessage(mensajes.getSeparador());
    }
}
