package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.fx.FxEngine;
import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protección tipo ENTRADA: impide que jugadores sin bypass entren al cubo.
 * Usa PlayerMoveEvent con verificación de cambio de bloque para eficiencia.
 * Si detecta entrada, teleporta al jugador fuera y activa efecto de rebote.
 *
 * Usa un cooldown por jugador para no spamear mensajes/efectos.
 */
public final class ListenerEntrada implements Listener {

    private final ProtectionRegistry registry;
    private final FxEngine fxEngine;
    private final Mensajes mensajes;

    // Cooldown de mensaje por jugador (en milisegundos)
    private static final long COOLDOWN_MS = 2000;
    private final Map<UUID, Long> ultimoBloqueo = new ConcurrentHashMap<>();

    public ListenerEntrada(ProtectionRegistry registry, FxEngine fxEngine, Mensajes mensajes) {
        this.registry = registry;
        this.fxEngine = fxEngine;
        this.mensajes = mensajes;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMovimiento(PlayerMoveEvent event) {
        if (event.isCancelled()) return;

        Player jugador = event.getPlayer();
        if (jugador.hasPermission("protectium.bypass")) return;

        Location desde = event.getFrom();
        Location hacia = event.getTo();

        // Optimización: solo verificar si cambió de bloque
        if (desde.getBlockX() == hacia.getBlockX()
         && desde.getBlockY() == hacia.getBlockY()
         && desde.getBlockZ() == hacia.getBlockZ()) {
            return;
        }

        List<ProtectionRecord> contenedoras = registry.buscarContenedoras(hacia);

        for (ProtectionRecord rec : contenedoras) {
            if (rec.getTipo() != ProtectionType.ENTRADA) continue;

            // Si ya estaba dentro del cubo, dejarlo salir sin problema
            if (rec.getCubo().contiene(desde)) continue;

            // --- Bloquear: reescribir destino directamente al origen ---
            // setTo() es el mecanismo correcto en PlayerMoveEvent.
            // Cancelar + teleport genera loops de eventos en la mayoría de servidores.
            event.setTo(desde);

            // --- Efecto visual en el borde que intentó cruzar ---
            // El punto visual es el centro del bloque destino (donde el jugador
            // vería la "pared" invisible). Se usa hacia con offset +0.5 para
            // que las partículas aparezcan en el centro del bloque, no en la esquina.
            Location puntoVisual = hacia.clone();
            puntoVisual.setX(hacia.getBlockX() + 0.5);
            puntoVisual.setY(hacia.getBlockY() + 0.5);
            puntoVisual.setZ(hacia.getBlockZ() + 0.5);
            fxEngine.onReboteEntrada(puntoVisual, rec);

            // --- Mensaje con cooldown para no spamear ---
            UUID uid = jugador.getUniqueId();
            long ahora = System.currentTimeMillis();
            Long ultimo = ultimoBloqueo.get(uid);
            if (ultimo == null || (ahora - ultimo) >= COOLDOWN_MS) {
                jugador.sendMessage(mensajes.entradaDenegada());
                ultimoBloqueo.put(uid, ahora);
            }

            return;
        }
    }
}
