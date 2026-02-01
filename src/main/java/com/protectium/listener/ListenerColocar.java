package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.core.LimitManager;
import com.protectium.core.MessageManager;
import com.protectium.fx.FxEngine;
import com.protectium.item.ItemAuthority;
import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import com.protectium.core.ProtectiumPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

/**
 * El momento crítico: cuando un jugador coloca un bloque.
 * Si el ítem es un ítem autorizado → nace la protección.
 * Si no → no pasa nada. El ítem no hace nada en el mundo sin autorización.
 * 
 * VERSIÓN MEJORADA CON VALIDACIONES DE SEGURIDAD:
 * - Límites por jugador
 * - Verificación de permisos
 * - Validación de overlapping
 * - Manejo de errores robusto
 */
public final class ListenerColocar implements Listener {

    private final ItemAuthority itemAuthority;
    private final ProtectionRegistry registry;
    private final FxEngine fxEngine;
    private final Mensajes mensajes;
    private final LimitManager limitManager;
    private final MessageManager messageManager;

    public ListenerColocar(ItemAuthority itemAuthority, ProtectionRegistry registry,
            FxEngine fxEngine, Mensajes mensajes) {
        this.itemAuthority = itemAuthority;
        this.registry = registry;
        this.fxEngine = fxEngine;
        this.mensajes = mensajes;
        
        // Obtener managers del plugin
        ProtectiumPlugin plugin = (ProtectiumPlugin) org.bukkit.plugin.java.JavaPlugin
                .getProvidingPlugin(ListenerColocar.class);
        this.limitManager = plugin.getLimitManager();
        this.messageManager = plugin.getMessageManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onColocar(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        var itemEnMano = event.getItemInHand();

        // ¿Es un ítem autorizado?
        if (!itemAuthority.esItemAutorizado(itemEnMano))
            return;

        // Extraer datos del ítem
        ProtectionType tipo = itemAuthority.extraerTipo(itemEnMano);
        int radio = itemAuthority.extraerRadio(itemEnMano);

        if (tipo == null || radio <= 0) {
            // Datos corruptos
            event.setCancelled(true);
            player.sendMessage(messageManager.get("errors.item-damaged"));
            return;
        }

        // ═══════════════════════════════════════════════════════════════════
        // VALIDACIÓN 1: ¿El jugador tiene permiso para colocar aquí?
        // ═══════════════════════════════════════════════════════════════════
        if (!player.hasPermission("protectium.place")) {
            event.setCancelled(true);
            player.sendMessage(messageManager.get("errors.no-permission-place"));
            return;
        }

        // ═══════════════════════════════════════════════════════════════════
        // VALIDACIÓN 2: ¿El radio es permitido para este jugador?
        // ═══════════════════════════════════════════════════════════════════
        if (!limitManager.isRadiusAllowed(player, radio)) {
            event.setCancelled(true);
            int maxRadius = limitManager.getMaxRadius(player);
            player.sendMessage(messageManager.getErrorRadiusExceeds(radio, maxRadius));
            return;
        }

        // ═══════════════════════════════════════════════════════════════════
        // VALIDACIÓN 3: ¿El jugador alcanzó su límite de protecciones?
        // ═══════════════════════════════════════════════════════════════════
        if (!limitManager.canPlaceProtection(player, registry)) {
            event.setCancelled(true);
            int limit = limitManager.getMaxProtections(player);
            player.sendMessage(messageManager.getErrorLimitReached(limit));
            return;
        }

        Location ubicacion = event.getBlock().getLocation();

        // ═══════════════════════════════════════════════════════════════════
        // VALIDACIÓN 4: ¿Ya existe una protección exactamente aquí?
        // ═══════════════════════════════════════════════════════════════════
        if (registry.existeEn(ubicacion)) {
            event.setCancelled(true);
            ProtectionRecord existing = registry.obtenerEn(ubicacion);
            player.sendMessage(messageManager.get("errors.overlap-protection",
                java.util.Map.of(
                    "tipo", existing.getTipo().getConfigKey(),
                    "location", String.format("%d, %d, %d", 
                        ubicacion.getBlockX(), 
                        ubicacion.getBlockY(), 
                        ubicacion.getBlockZ())
                )));
            return;
        }

        // ═══════════════════════════════════════════════════════════════════
        // VALIDACIÓN 5: ¿Hay overlapping con otras protecciones del mismo tipo?
        // (Opcional - configurable)
        // ═══════════════════════════════════════════════════════════════════
        List<ProtectionRecord> nearby = registry.buscarContenedoras(ubicacion);
        for (ProtectionRecord rec : nearby) {
            if (rec.getTipo() == tipo && !rec.getColocadoPor().equals(player.getUniqueId())) {
                // Hay overlap con otra protección del mismo tipo de otro jugador
                if (!player.hasPermission("protectium.bypass")) {
                    event.setCancelled(true);
                    player.sendMessage(mensajes.getPrefijoError() + 
                        "§cNo puedes colocar protecciones superpuestas con otras de este tipo.");
                    return;
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // TODO: VALIDACIÓN FUTURA - WorldGuard/GriefPrevention
        // if (worldGuardHook.isRegionProtected(ubicacion)) { ... }
        // ═══════════════════════════════════════════════════════════════════

        // --- TODAS LAS VALIDACIONES PASADAS: Crear la protección ---
        try {
            ProtectionRecord record = new ProtectionRecord(
                    tipo,
                    ubicacion,
                    radio,
                    player.getUniqueId());

            // Registrar en el registry
            registry.registrar(record);

            // Efectos visuales de activación
            fxEngine.onColocar(record);
            fxEngine.showVisuals(record);

            // Mensaje al jugador usando MessageManager
            player.sendMessage(messageManager.getProtectionActivated(
                tipo.getConfigKey(),
                radio,
                ubicacion.getBlockX(),
                ubicacion.getBlockY(),
                ubicacion.getBlockZ(),
                ubicacion.getWorld().getName()
            ));

            // Auto-save async para prevenir pérdida de datos
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                    org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(ListenerColocar.class),
                    () -> {
                        try {
                            ((ProtectiumPlugin) org.bukkit.plugin.java.JavaPlugin
                                    .getProvidingPlugin(ListenerColocar.class))
                                    .getPersistenceManager().saveAll();
                        } catch (Exception e) {
                            ((ProtectiumPlugin) org.bukkit.plugin.java.JavaPlugin
                                    .getProvidingPlugin(ListenerColocar.class))
                                    .getLogger().warning("Error en auto-save: " + e.getMessage());
                        }
                    });

        } catch (Exception e) {
            // Error crítico al crear protección
            event.setCancelled(true);
            player.sendMessage(messageManager.getErrorInternalError(e.getMessage()));
            
            ProtectiumPlugin plugin = (ProtectiumPlugin) org.bukkit.plugin.java.JavaPlugin
                    .getProvidingPlugin(ListenerColocar.class);
            plugin.getLogger().severe("ERROR al crear protección: " + e.getMessage());
            e.printStackTrace();
        }

        // El bloque se coloca normalmente (no cancelamos el evento si todo salió bien)
        // El ítem se consume automáticamente por Bukkit al colocar
    }
}
