package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.fx.FxEngine;
import com.protectium.item.ItemAuthority;
import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Protección tipo AREA: bloquea romper, colocar, incendiar bloques
 * dentro de cualquier cubo de tipo AREA activo.
 *
 * RESPETA EL SISTEMA DE MIEMBROS:
 * - Los miembros con permiso de interacción pueden modificar bloques
 * - Los dueños y moderadores siempre pueden modificar
 * - Los jugadores con bypass ignoran protecciones
 *
 * Cuando bloquea un intento, emite efecto visual + sonoro en el punto
 * donde el jugador intentó actuar, así hay feedback inmediato.
 */
public final class ListenerBloques implements Listener {

    private final ProtectionRegistry registry;
    private final Mensajes mensajes;
    private final FxEngine fxEngine;
    private final ItemAuthority itemAuthority;

    public ListenerBloques(ProtectionRegistry registry, Mensajes mensajes,
            FxEngine fxEngine, ItemAuthority itemAuthority) {
        this.registry = registry;
        this.mensajes = mensajes;
        this.fxEngine = fxEngine;
        this.itemAuthority = itemAuthority;
    }

    // ---------------------------------------------------------------
    // Romper bloques dentro de zona AREA
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onRomper(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        Player player = event.getPlayer();

        if (player.hasPermission("protectium.bypass"))
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.AREA)
                continue;

            // No bloquear romper el propio bloque de protección (eso lo maneja
            // ListenerRomper)
            if (rec.getUbicacionBloque().equals(event.getBlock().getLocation()))
                continue;

            // Verificar si el jugador es miembro con permisos
            if (rec.hasInteractPermission(player.getUniqueId()))
                continue;

            // Verificar flag block-break
            if (!rec.getFlag("block-break", true))
                continue;

            // --- Bloquear ---
            event.setCancelled(true);
            player.sendMessage(mensajes.bloqueoPorProteccion("Área Protegida"));

            // --- Efecto visual + sonoro en el punto donde intentó romper ---
            Location punto = event.getBlock().getLocation().clone().add(0.5, 0.5, 0.5);
            fxEngine.onReboteBloqueo(punto, rec);
            return;
        }
    }

    // ---------------------------------------------------------------
    // Colocar bloques dentro de zona AREA
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onColocar(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;
        Player player = event.getPlayer();

        if (player.hasPermission("protectium.bypass"))
            return;

        // Si el ítem en la mano es un ítem de protección autorizado, dejarlo pasar
        ItemStack itemEnMano = event.getItemInHand();
        if (itemAuthority.esItemAutorizado(itemEnMano))
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.AREA)
                continue;

            // Verificar si el jugador es miembro con permisos
            if (rec.hasInteractPermission(player.getUniqueId()))
                continue;

            // Verificar flag block-place
            if (!rec.getFlag("block-place", true))
                continue;

            event.setCancelled(true);
            player.sendMessage(mensajes.bloqueoPorProteccion("Área Protegida"));

            Location punto = event.getBlock().getLocation().clone().add(0.5, 0.5, 0.5);
            fxEngine.onReboteBloqueo(punto, rec);
            return;
        }
    }

    // ---------------------------------------------------------------
    // Incendio de bloques dentro de zona AREA
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onIncendio(BlockIgniteEvent event) {
        if (event.isCancelled())
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.AREA)
                continue;

            // Si tiene flag fire deshabilitado, permitir fuego
            if (!rec.getFlag("fire", true))
                continue;

            event.setCancelled(true);
            return;
        }
    }

    // ---------------------------------------------------------------
    // Quemar bloques dentro de zona AREA
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuemar(BlockBurnEvent event) {
        if (event.isCancelled())
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.AREA)
                continue;
            if (!rec.getFlag("fire", true))
                continue;
            event.setCancelled(true);
            return;
        }
    }

    // ---------------------------------------------------------------
    // Desaparecer bloques dentro de zona AREA
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onDesaparecer(BlockFadeEvent event) {
        if (event.isCancelled())
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getBlock().getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.AREA)
                continue;
            event.setCancelled(true);
            return;
        }
    }

    // ---------------------------------------------------------------
    // Explosiones dentro de zona AREA
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplosion(BlockExplodeEvent event) {
        if (event.isCancelled())
            return;

        // Remover bloques protegidos de la lista de bloques afectados
        event.blockList().removeIf(block -> {
            List<ProtectionRecord> recs = registry.buscarContenedoras(block.getLocation());
            for (ProtectionRecord rec : recs) {
                if (rec.getTipo() == ProtectionType.AREA && rec.getFlag("explosions", true)) {
                    return true;
                }
            }
            return false;
        });
    }

    // ---------------------------------------------------------------
    // Pistones dentro de zona AREA
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.isCancelled())
            return;

        for (org.bukkit.block.Block bloque : event.getBlocks()) {
            List<ProtectionRecord> contenedoras = registry.buscarContenedoras(bloque.getLocation());
            for (ProtectionRecord rec : contenedoras) {
                if (rec.getTipo() != ProtectionType.AREA)
                    continue;
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled())
            return;

        for (org.bukkit.block.Block bloque : event.getBlocks()) {
            List<ProtectionRecord> contenedoras = registry.buscarContenedoras(bloque.getLocation());
            for (ProtectionRecord rec : contenedoras) {
                if (rec.getTipo() != ProtectionType.AREA)
                    continue;
                event.setCancelled(true);
                return;
            }
        }
    }
}
