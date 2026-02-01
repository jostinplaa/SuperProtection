package com.protectium.listener;

import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;

/**
 * Protección tipo SPAWN: bloquea el spawn de mobs hostiles
 * dentro de cualquier cubo de tipo SPAWN activo.
 */
public final class ListenerSpawn implements Listener {

    private final ProtectionRegistry registry;

    // Tipos de mob que se consideran "hostiles"
    private static final java.util.Set<EntityType> HOSTILES = java.util.EnumSet.of(
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.CREEPER,
        EntityType.SPIDER,
        EntityType.ENDERMAN,
        EntityType.BLAZE,
        EntityType.PHANTOM,
        EntityType.SLIME,
        EntityType.MAGMA_CUBE,
        EntityType.WITCH,
        EntityType.GUARDIAN,
        EntityType.ELDER_GUARDIAN,
        EntityType.SILVERFISH,
        EntityType.CAVE_SPIDER,
        EntityType.STRAY,
        EntityType.HUSK,
        EntityType.DROWNED,
        EntityType.VINDICATOR,
        EntityType.EVOKER,
        EntityType.VEX,
        EntityType.RAVAGER,
        EntityType.PILLAGER,
        EntityType.WITHER_SKELETON,
        EntityType.ENDERMITE,
        EntityType.SHULKER
    );

    public ListenerSpawn(ProtectionRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;

        // Solo bloquear spawns naturales (no artificiales del admin)
        if (event.getSpawnReason() != org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL
         && event.getSpawnReason() != org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.PATROL) {
            return;
        }

        Entity entidad = event.getEntity();

        // ¿Es un mob hostile?
        if (!HOSTILES.contains(entidad.getType())) return;

        // Buscar protecciones SPAWN que contengan esta ubicación
        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(entidad.getLocation());

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.SPAWN) continue;
            event.setCancelled(true);
            return;
        }
    }
}
