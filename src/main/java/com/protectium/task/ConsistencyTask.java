package com.protectium.task;

import com.protectium.core.ProtectiumPlugin;
import com.protectium.item.ItemAuthority;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Tarea de consistencia que verifica periódicamente que todos los
 * bloques de protección siguen existiendo en el mundo.
 *
 * Si algún bloque fue eliminado por medio externo (plugins, NBT editing,
 * mundo corrupto, etc.), esta tarea limpia la protección huérfana.
 *
 * Se ejecuta cada 5 segundos (100 ticks) por defecto.
 * Es un safety net, no el mecanismo principal (ese es ListenerRomper).
 */
public final class ConsistencyTask extends BukkitRunnable {

    private final ProtectionRegistry registry;
    private final ItemAuthority itemAuthority;
    private final Logger logger;

    public ConsistencyTask(ProtectionRegistry registry, ItemAuthority itemAuthority,
            ProtectiumPlugin plugin) {
        this.registry = registry;
        this.itemAuthority = itemAuthority;
        this.logger = plugin.getLogger();
    }

    @Override
    public void run() {
        List<ProtectionRecord> todas = registry.todas();
        if (todas.isEmpty())
            return;

        List<Location> huerfanas = new ArrayList<>();

        for (ProtectionRecord rec : todas) {
            Location loc = rec.getUbicacionBloque();

            // Si el mundo no está cargado, no podemos verificar
            if (loc.getWorld() == null)
                continue;

            // Verificar simplemente que el bloque exista (no sea aire)
            Material materialActual = loc.getBlock().getType();
            if (materialActual == Material.AIR || materialActual == Material.CAVE_AIR
                    || materialActual == Material.VOID_AIR) {
                huerfanas.add(loc);
            }
        }

        // Eliminar protecciones huérfanas
        for (Location loc : huerfanas) {
            ProtectionRecord eliminado = registry.eliminar(loc);
            if (eliminado != null) {
                String pos = String.format("%s:%d:%d:%d",
                        loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                logger.warning("Protección huérfana eliminada: " + pos);
            }
        }
    }
}
