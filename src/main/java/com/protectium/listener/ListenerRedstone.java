package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.Set;

/**
 * Protección tipo REDSTONE: bloquea la activación de:
 * - Palancas (levers)
 * - Botones
 * - Placas de presión (en forma indirecta, via BlockRedstone)
 * - Cambios de señal redstone general
 */
public final class ListenerRedstone implements Listener {

    private final ProtectionRegistry registry;
    private final Mensajes mensajes;

    // Materiales que son "activadores" de redstone
    private static final Set<Material> ACTIVADORES = Set.of(
            Material.LEVER,
            Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON,
            Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON,
            Material.MANGROVE_BUTTON, Material.BAMBOO_BUTTON, Material.CHERRY_BUTTON,
            Material.STONE_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON);

    public ListenerRedstone(ProtectionRegistry registry, Mensajes mensajes) {
        this.registry = registry;
        this.mensajes = mensajes;
    }

    // ---------------------------------------------------------------
    // Interacción con activadores (palancas, botones)
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractir(PlayerInteractEvent event) {
        if (event.isCancelled())
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getClickedBlock() == null)
            return;

        // ¿Es un activador de redstone?
        if (!ACTIVADORES.contains(event.getClickedBlock().getType()))
            return;

        // Bypass
        if (event.getPlayer().hasPermission("protectium.bypass"))
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(
                event.getClickedBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.REDSTONE)
                continue;

            event.setCancelled(true);
            event.getPlayer().sendMessage(mensajes.bloqueoPorProteccion("Zona Sin Redstone"));
            return;
        }
    }

    // ---------------------------------------------------------------
    // Cambio general de señal redstone
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onRedstone(BlockRedstoneEvent event) {
        // BlockRedstoneEvent no tiene isCancelled directo,
        // pero podemos igualar la señal nueva a la antigua para "cancelarlo"
        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.REDSTONE)
                continue;

            // "Cancelar" igualando la señal nueva a la antigua
            event.setNewCurrent(event.getOldCurrent());
            return;
        }
    }
}
