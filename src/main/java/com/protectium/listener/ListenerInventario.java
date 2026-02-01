package com.protectium.listener;

import com.protectium.item.ItemAuthority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Garantiza la regla: "El ítem no hace nada en el inventario."
 * Cancela cualquier interacción de uso (click derecho en el aire)
 * si el jugador sostiene un ítem de protección autorizado.
 * Colocar bloques NO se cancela aquí (se maneja en ListenerColocar).
 */
public final class ListenerInventario implements Listener {

    private final ItemAuthority itemAuthority;

    public ListenerInventario(ItemAuthority itemAuthority) {
        this.itemAuthority = itemAuthority;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractir(PlayerInteractEvent event) {
        if (event.isCancelled())
            return;

        // Solo cancelar clicks en el aire (RIGHT_CLICK_AIR)
        // Los clicks en bloques (RIGHT_CLICK_BLOCK) son necesarios para colocar
        if (event.getAction() != Action.RIGHT_CLICK_AIR)
            return;

        ItemStack itemEnMano = event.getItem();
        if (itemEnMano != null && itemAuthority.esItemAutorizado(itemEnMano)) {
            event.setCancelled(true);
            // El ítem no hace nada. Silencioso, sin mensaje.
        }
    }
}
