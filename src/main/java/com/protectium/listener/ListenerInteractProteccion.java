package com.protectium.listener;

import com.protectium.gui.GuiManager;
import com.protectium.item.ItemAuthority;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener que abre el menú de gestión de una protección
 * cuando el jugador hace click derecho en el bloque de protección.
 */
public final class ListenerInteractProteccion implements Listener {

    private final ProtectionRegistry registry;
    private final GuiManager guiManager;
    private final ItemAuthority itemAuthority;

    public ListenerInteractProteccion(ProtectionRegistry registry, GuiManager guiManager,
            ItemAuthority itemAuthority) {
        this.registry = registry;
        this.guiManager = guiManager;
        this.itemAuthority = itemAuthority;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // Solo nos interesa click derecho en bloque
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        // No verificamos material para permitir bloques personalizados
        // if (block.getType() != itemAuthority.getMaterialBase()) return;

        // ¿Existe una protección en esta ubicación?
        ProtectionRecord rec = registry.obtenerEn(block.getLocation());
        if (rec == null)
            return;

        Player player = event.getPlayer();

        // Verificar si el jugador puede gestionar esta protección
        // (es miembro o tiene bypass)
        if (!rec.isMember(player.getUniqueId()) && !player.hasPermission("protectium.bypass")) {
            // No es miembro, denegar silenciosamente
            return;
        }

        // Cancelar el evento para evitar colocar bloques
        event.setCancelled(true);

        // Mostrar visuales - feedback visual de qué protección es
        com.protectium.fx.FxEngine engine = ((com.protectium.core.ProtectiumPlugin) org.bukkit.Bukkit.getPluginManager()
                .getPlugin("Protectium")).getFxEngine();
        if (engine != null)
            engine.showVisuals(rec);

        // Abrir menú de gestión
        guiManager.abrirMenuProteccion(player, rec);
    }
}
