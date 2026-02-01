package com.protectium.task;

import com.protectium.fx.FxEngine;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Tarea periódica que impulsa el renderizado visual de todos los cubos.
 * Se ejecuta cada tick. El FxEngine internamente decide qué renderear
 * según su ciclo de pulso.
 *
 * Diseño: esta tarea es ligera. El trabajo pesado lo hace FxEngine,
 * que ya tiene lógica interna para muestrear y no saturar partículas.
 */
public final class FxTickTask extends BukkitRunnable {

    private final FxEngine fxEngine;
    private final ProtectionRegistry registry;

    public FxTickTask(FxEngine fxEngine, ProtectionRegistry registry) {
        this.fxEngine = fxEngine;
        this.registry = registry;
    }

    @Override
    public void run() {
        fxEngine.tick();
    }
}
