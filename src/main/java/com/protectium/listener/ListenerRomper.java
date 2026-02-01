package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.fx.FxEngine;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * El otro momento crítico: cuando se rompe un bloque.
 * Si ese bloque tenía una protección activa → la protección muere.
 * Es instantáneo, irreversible, y sin excepciones.
 */
public final class ListenerRomper implements Listener {

    private final ProtectionRegistry registry;
    private final FxEngine fxEngine;
    private final Mensajes mensajes;

    public ListenerRomper(ProtectionRegistry registry, FxEngine fxEngine, Mensajes mensajes) {
        this.registry = registry;
        this.fxEngine = fxEngine;
        this.mensajes = mensajes;
    }

    @EventHandler(priority = EventPriority.MONITOR) // MONITOR: se ejecuta después de que el evento ya fue confirmado
    public void onRomper(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        // ¿Tenía una protección este bloque?
        ProtectionRecord eliminado = registry.eliminar(event.getBlock().getLocation());
        if (eliminado == null) return; // No tenía protección, no pasa nada

        // --- Efectos de eliminación ---
        fxEngine.onRomper(eliminado);

        // --- Mensaje ---
        event.getPlayer().sendMessage(
            mensajes.exitoProteccionEliminada(eliminado.getTipo().getConfigKey())
        );

        // El bloque ya fue roto por Bukkit. La protección ya no existe.
        // No hay nada más que hacer.
    }
}
