package com.protectium.listener;

import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import java.util.List;

/**
 * Protección tipo FUEGO: bloquea la propagación de fuego
 * y el flujo de lava dentro de cualquier cubo de tipo FUEGO activo.
 */
public final class ListenerFuego implements Listener {

    private final ProtectionRegistry registry;

    public ListenerFuego(ProtectionRegistry registry) {
        this.registry = registry;
    }

    // ---------------------------------------------------------------
    // Propagación de fuego (BlockSpreadEvent)
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPropagarFuego(BlockSpreadEvent event) {
        if (event.isCancelled())
            return;

        // Solo nos interesan propagaciones de fuego o lava
        Material source = event.getSource().getType();
        if (source != Material.FIRE && source != Material.SOUL_FIRE && source != Material.LAVA) {
            return;
        }

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.FUEGO)
                continue;
            event.setCancelled(true);
            return;
        }
    }

    // ---------------------------------------------------------------
    // Encender fuego (BlockIgniteEvent)
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onEncenderFuego(BlockIgniteEvent event) {
        if (event.isCancelled())
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.FUEGO)
                continue;
            event.setCancelled(true);
            return;
        }
    }
}
