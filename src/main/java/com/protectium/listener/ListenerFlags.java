package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.List;

/**
 * Listener para las nuevas Flags V3:
 * - damage: Dañar entidades (animales, mobs, etc - NO PVP que va separado)
 * - interact-entity: Interactuar con aldeanos, marcos, armor stands
 * - item-drop: Soltar items
 * - item-pickup: Recoger items
 */
public final class ListenerFlags implements Listener {

    private final ProtectionRegistry registry;
    private final Mensajes mensajes;

    public ListenerFlags(ProtectionRegistry registry, Mensajes mensajes) {
        this.registry = registry;
        this.mensajes = mensajes;
    }

    // -------------------------------------------------------------------------
    // Daño a Entidades (No Jugadores)
    // Para PVP ya existe ListenerPvp (o debería existir lógica pvp aparte)
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;
        if (event.getEntity() instanceof Player)
            return; // PVP se maneja aparte o en otro flag check si se quiere unificar

        if (player.hasPermission("protectium.bypass"))
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getEntity().getLocation());
        for (ProtectionRecord rec : contenedoras) {
            // Si el jugador TIENE PERMISO de interacción en la protección, puede dañar
            // (e.g. matar vacas)
            if (rec.hasInteractPermission(player.getUniqueId()))
                continue;

            // flag 'damage': true = permitido, false = denegado
            if (!rec.getFlag("damage", false)) {
                event.setCancelled(true);
                player.sendMessage(mensajes.bloqueoPorProteccion("Dañar Entidades"));
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Interacción con Entidades (Villagers, Marcos, ArmorStands)
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("protectium.bypass"))
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getRightClicked().getLocation());
        for (ProtectionRecord rec : contenedoras) {
            if (rec.hasInteractPermission(player.getUniqueId()))
                continue;

            // flag 'interact-entity'
            if (!rec.getFlag("interact-entity", true)) {
                event.setCancelled(true);
                player.sendMessage(mensajes.bloqueoPorProteccion("Interactuar Entidad"));
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Soltar Items
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("protectium.bypass"))
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(player.getLocation());
        for (ProtectionRecord rec : contenedoras) {
            // Drops suelen permitirse por defecto, pero si flag item-drop es false, se
            // prohíbe.
            // Aquí asumimos que si NO tienes permiso, dependes del flag.
            // Si TIENES permiso (miembro), puedes dropear siempre.
            if (rec.isMember(player.getUniqueId()))
                continue;

            if (!rec.getFlag("item-drop", true)) {
                event.setCancelled(true);
                player.sendMessage(mensajes.bloqueoPorProteccion("Soltar Items"));
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Recoger Items
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (player.hasPermission("protectium.bypass"))
            return;

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(event.getItem().getLocation());
        for (ProtectionRecord rec : contenedoras) {
            if (rec.isMember(player.getUniqueId()))
                continue;

            if (!rec.getFlag("item-pickup", true)) {
                event.setCancelled(true);
                // No mandamos mensaje en pickup para no spamear
                return;
            }
        }
    }
}
