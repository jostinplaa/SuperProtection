package com.protectium.task;

import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Tarea periódica que verifica la ubicación de los jugadores
 * y muestra una BossBar si están dentro de una protección.
 */
public final class BossBarTask extends BukkitRunnable {

    private final ProtectionRegistry registry;
    // Cache de BossBars por jugador para evitar crearlas/destruirlas cada tick
    private final Map<UUID, BossBar> activeBars = new HashMap<>();
    private final Map<UUID, UUID> lastProtectionId = new HashMap<>();

    public BossBarTask(ProtectionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }

        // Limpiar cache de desconectados
        activeBars.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        lastProtectionId.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private void updatePlayer(Player player) {
        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(player.getLocation());

        if (contenedoras.isEmpty()) {
            removeBar(player);
            lastProtectionId.remove(player.getUniqueId());
            return;
        }

        // Tomamos la primera (o la más relevante si hubiera prioridades)
        ProtectionRecord actual = contenedoras.get(0);

        // Si cambió de protección o no tenía barra
        if (!activeBars.containsKey(player.getUniqueId()) ||
                !actual.getId().equals(lastProtectionId.get(player.getUniqueId()))) {

            removeBar(player);
            createBar(player, actual);
            lastProtectionId.put(player.getUniqueId(), actual.getId());
        }
    }

    private void createBar(Player player, ProtectionRecord rec) {
        // Obtenemos el nombre (item custom name) o el tipo
        // Como no tenemos el item a mano, usamos el nombre del tipo por defecto
        // O podríamos guardar el "nombre" en el record (V3 mejora)
        // Por ahora usamos el nombre del tipo

        String nombre = rec.getTipo().getConfigKey();
        try {
            nombre = nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
        } catch (Exception ignored) {
        }

        Component title = Component
                .text("§x§f§f§f§f§f§f🛡 §x§a§a§a§a§f§fZONA PROTEGIDA: §x§0§0§f§f§f§f" + nombre.toUpperCase());

        BossBar bar = BossBar.bossBar(
                title,
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS);

        player.showBossBar(bar);
        activeBars.put(player.getUniqueId(), bar);
    }

    private void removeBar(Player player) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }
}
