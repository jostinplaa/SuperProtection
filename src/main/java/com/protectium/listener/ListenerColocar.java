package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.fx.FxEngine;
import com.protectium.item.ItemAuthority;
import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import com.protectium.core.ProtectiumPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * El momento crítico: cuando un jugador coloca un bloque.
 * Si el ítem es un ítem autorizado → nace la protección.
 * Si no → no pasa nada. El ítem no hace nada en el mundo sin autorización.
 */
public final class ListenerColocar implements Listener {

    private final ItemAuthority itemAuthority;
    private final ProtectionRegistry registry;
    private final FxEngine fxEngine;
    private final Mensajes mensajes;

    public ListenerColocar(ItemAuthority itemAuthority, ProtectionRegistry registry,
            FxEngine fxEngine, Mensajes mensajes) {
        this.itemAuthority = itemAuthority;
        this.registry = registry;
        this.fxEngine = fxEngine;
        this.mensajes = mensajes;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onColocar(BlockPlaceEvent event) {
        // Solo nos interesan los ítems en la mano
        var itemEnMano = event.getItemInHand();

        // ¿Es un ítem autorizado?
        if (!itemAuthority.esItemAutorizado(itemEnMano))
            return;

        // Extraer datos del ítem
        ProtectionType tipo = itemAuthority.extraerTipo(itemEnMano);
        int radio = itemAuthority.extraerRadio(itemEnMano);

        if (tipo == null || radio <= 0)
            return; // datos corruptos, ignorar

        // ¿Ya existe una protección en esta ubicación? No permitir empilamiento
        if (registry.existeEn(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    mensajes.getPrefijoError() + "§c¡Ya existe una protección en esta ubicación!");
            return;
        }

        // --- Crear la protección ---
        ProtectionRecord record = new ProtectionRecord(
                tipo,
                event.getBlock().getLocation(),
                radio,
                event.getPlayer().getUniqueId());

        // Registrar en el registry
        registry.registrar(record);

        // --- Efectos visuales de activación ---
        fxEngine.onColocar(record);
        fxEngine.showVisuals(record);

        // --- Mensaje al jugador ---
        event.getPlayer().sendMessage(mensajes.exitoProteccionActiva(tipo.getConfigKey(), radio));

        // Auto-save async para prevenir pérdida de datos
        try {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                    org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(ListenerColocar.class),
                    () -> {
                        try {
                            ((ProtectiumPlugin) org.bukkit.plugin.java.JavaPlugin
                                    .getProvidingPlugin(ListenerColocar.class))
                                    .getPersistenceManager().saveAll();
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }

        // El bloque se coloca normalmente (no cancelamos el evento)
        // El ítem se consume automáticamente por Bukkit al colocar
    }
}
