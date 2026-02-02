package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows ActionBar notifications when players enter or leave protected zones.
 * v2.3.1 Feature
 */
public final class ListenerZoneNotify implements Listener {

    private final ProtectionRegistry registry;
    private final Mensajes mensajes;

    // Track if player is in any zone (simple boolean)
    private final Map<UUID, Boolean> playerInZone = new ConcurrentHashMap<>();

    // Cooldown to avoid spam
    private static final long COOLDOWN_MS = 3000;
    private final Map<UUID, Long> lastNotify = new ConcurrentHashMap<>();

    public ListenerZoneNotify(ProtectionRegistry registry, Mensajes mensajes) {
        this.registry = registry;
        this.mensajes = mensajes;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Only check on block change
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if currently in any zone
        List<ProtectionRecord> currentZones = registry.buscarContenedoras(to);
        boolean nowInZone = !currentZones.isEmpty();
        Boolean wasInZone = playerInZone.get(uuid);

        if (wasInZone == null) {
            wasInZone = false;
        }

        // Entering a zone
        if (nowInZone && !wasInZone) {
            if (canNotify(uuid)) {
                String type = currentZones.get(0).getTipo().name();
                sendActionBar(player, mensajes.zoneEntered(type));
            }
        }

        // Leaving a zone
        if (!nowInZone && wasInZone) {
            if (canNotify(uuid)) {
                sendActionBar(player, mensajes.zoneLeft());
            }
        }

        // Update state
        playerInZone.put(uuid, nowInZone);
    }

    private boolean canNotify(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = lastNotify.get(uuid);
        if (last == null || (now - last) >= COOLDOWN_MS) {
            lastNotify.put(uuid, now);
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message));
    }
}
