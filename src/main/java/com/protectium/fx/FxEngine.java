package com.protectium.fx;

import com.protectium.protection.CubeRegion;
import com.protectium.protection.ProtectionRecord;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor visual centralizado. Todas las partículas, sonidos y efectos
 * de las protecciones pasan por aquí. Nada de efectos escrito fuera.
 *
 * Diseño clave: el motor recibe un ProtectionRecord y su FxTheme,
 * y genera los efectos sin conocer ni importarle la lógica de protección.
 */
public final class FxEngine {

    private final ConfigurationSection fxConfig;
    private Plugin plugin;

    // Cache de temas por tipo para no re-parsear cada tick
    private final Map<String, FxTheme> temaCache = new ConcurrentHashMap<>();

    // Visualizaciones activas (Protección -> Tiempo de inicio)
    private final Map<ProtectionRecord, Long> activeVisuals = new ConcurrentHashMap<>();
    private static final long VISUAL_TIMEOUT_MS = 10000;

    // Variables de pulso globales (evoluciona cada tick)
    private int tickPulso = 0;

    // Parámetros parseados de config
    private final int ticksArista;
    private final int explosionEsquinas;
    private final int ambientePorCara;
    private final double pulsoMin;
    private final double pulsoMax;
    private final int pulsoPeriodo;
    private final int explosionRomper;

    public FxEngine(ConfigurationSection config) {
        this.fxConfig = config;
        ConfigurationSection fx = config.getConfigurationSection("fx");

        this.ticksArista = fx.getInt("ticks-arista", 2);
        this.explosionEsquinas = fx.getInt("explosion-esquinas", 28);
        this.ambientePorCara = fx.getInt("ambiente-por-cara", 4);
        this.pulsoMin = fx.getDouble("pulso-min", 0.25);
        this.pulsoMax = fx.getDouble("pulso-max", 1.0);
        this.pulsoPeriodo = fx.getInt("pulso-periodo", 40);
        this.explosionRomper = fx.getInt("explosion-romper", 60);
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // API principal — llamada desde FxTickTask cada tick
    // ---------------------------------------------------------------

    /**
     * Tick principal del motor. Se llama cada tick desde la tarea periódica.
     * Evoluciona el pulso y decide qué renderear este tick.
     */
    /**
     * Tick principal del motor.
     * Solo renderea protecciones marcadas como activas visualmente.
     */
    public void tick() {
        tickPulso = (tickPulso + 1) % pulsoPeriodo;
        long now = System.currentTimeMillis();

        activeVisuals.entrySet().removeIf(entry -> {
            ProtectionRecord rec = entry.getKey();
            long start = entry.getValue();

            // Timeout check
            if (now - start > VISUAL_TIMEOUT_MS) {
                // Time's up, remove from active visuals
                return true;
            }

            // Render
            FxTheme tema = getTema(rec);
            World mundo = rec.getUbicacionBloque().getWorld();
            if (mundo == null)
                return true;

            CubeRegion cubo = rec.getCubo();

            // Aristas
            if (tickPulso % ticksArista == 0) {
                renderAristas(mundo, cubo, tema);
            }

            // Caras (si se quiere)
            renderCara(mundo, cubo, tema);

            return false;
        });
    }

    public void showVisuals(ProtectionRecord rec) {
        activeVisuals.put(rec, System.currentTimeMillis());
    }

    // ---------------------------------------------------------------
    // Efectos de evento (lugar / romper)
    // ---------------------------------------------------------------

    /**
     * Explosión visual cuando se coloca un bloque de protección.
     * Burst en las 8 esquinas + explosión central + sonido.
     */
    public void onColocar(ProtectionRecord rec) {
        World mundo = rec.getUbicacionBloque().getWorld();
        if (mundo == null)
            return;

        FxTheme tema = getTema(rec);
        CubeRegion cubo = rec.getCubo();
        Location centro = rec.getUbicacionBloque().clone().add(0.5, 0.5, 0.5);

        // --- Burst en cada esquina ---
        for (Location esquina : cubo.esquinas(mundo)) {
            mundo.spawnParticle(
                    tema.getParticuEsquina(),
                    esquina,
                    explosionEsquinas,
                    0.15, 0.15, 0.15,
                    0.05,
                    tema.getOpcionesEsquina());
        }

        // --- Explosión central grande ---
        mundo.spawnParticle(
                tema.getParticuArista(),
                centro,
                explosionEsquinas * 3,
                (double) cubo.getRadio() * 0.4,
                (double) cubo.getRadio() * 0.4,
                (double) cubo.getRadio() * 0.4,
                0.02,
                tema.getOpcionesArista());

        // --- Onda de expansión: ring de partículas que sale del centro ---
        if (plugin != null) {
            for (int i = 0; i < 3; i++) {
                final int paso = i;
                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> anilloExpansion(mundo, centro, tema, paso * 0.6 + 0.3),
                        (long) i * 3);
            }
        }

        // --- Sonido ---
        try {
            Sound sonido = Sound.valueOf(tema.getSonidoColocar());
            mundo.playSound(centro, sonido, SoundCategory.BLOCKS, 1.5f, 0.6f);
        } catch (IllegalArgumentException ignored) {
            mundo.playSound(centro, Sound.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 1.5f, 0.6f);
        }

        // Sonido adicional de "activación"
        mundo.playSound(centro, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.8f, 1.8f);
    }

    /**
     * Explosión visual cuando se rompe el bloque de protección.
     * Dispersión masiva + implosión de partículas hacia el centro.
     */
    public void onRomper(ProtectionRecord rec) {
        World mundo = rec.getUbicacionBloque().getWorld();
        if (mundo == null)
            return;

        FxTheme tema = getTema(rec);
        CubeRegion cubo = rec.getCubo();
        Location centro = rec.getUbicacionBloque().clone().add(0.5, 0.5, 0.5);

        // --- Dispersión masiva desde las esquinas ---
        for (Location esquina : cubo.esquinas(mundo)) {
            mundo.spawnParticle(
                    tema.getParticuEsquina(),
                    esquina,
                    explosionRomper / 2,
                    0.3, 0.3, 0.3,
                    0.08,
                    tema.getOpcionesEsquina());
        }

        // --- Explosión central de dispersión ---
        mundo.spawnParticle(
                tema.getParticuArista(),
                centro,
                explosionRomper,
                (double) cubo.getRadio() * 0.5,
                (double) cubo.getRadio() * 0.5,
                (double) cubo.getRadio() * 0.5,
                0.05,
                tema.getOpcionesArista());

        // --- Lluvias de partículas desde cada cara (efecto de derrumbe) ---
        List<Location> caras = cubo.puntosCara(mundo, cubo.getRadio() > 8 ? 3 : 2);
        int sampleo = Math.min(caras.size(), 40); // máximo 40 puntos para no saturar
        for (int i = 0; i < sampleo; i++) {
            int idx = (int) (Math.random() * caras.size());
            Location punto = caras.get(idx);
            mundo.spawnParticle(
                    tema.getParticuCara(),
                    punto,
                    6,
                    0.1, 0.3, 0.1,
                    0.04,
                    tema.getOpcionesCara());
        }

        // --- Sonido ---
        try {
            Sound sonido = Sound.valueOf(tema.getSonidoRomper());
            mundo.playSound(centro, sonido, SoundCategory.BLOCKS, 1.5f, 0.5f);
        } catch (IllegalArgumentException ignored) {
            mundo.playSound(centro, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.5f, 0.5f);
        }

        mundo.playSound(centro, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.8f, 0.6f);
    }

    /**
     * Efecto de rebote visual cuando un jugador intenta romper o colocar
     * un bloque dentro de una zona AREA protegida.
     * Partículas dispersas desde el punto de contacto + crack + sonido rígido.
     */
    public void onReboteBloqueo(Location punto, ProtectionRecord rec) {
        World mundo = punto.getWorld();
        if (mundo == null)
            return;

        FxTheme tema = getTema(rec);

        // Partículas principales del tema en el punto de contacto
        mundo.spawnParticle(
                tema.getParticuArista(),
                punto,
                20,
                0.15, 0.15, 0.15,
                0.04,
                tema.getOpcionesArista());

        // Partículas de crack (efecto de impacto en piedra)
        mundo.spawnParticle(
                Particle.BLOCK_CRACK,
                punto,
                12,
                0.2, 0.2, 0.2,
                0.0,
                org.bukkit.Material.AMETHYST_BLOCK.createBlockData());

        // Partículas CRIT pequeñas para dar sensación de choque
        mundo.spawnParticle(
                Particle.CRIT,
                punto,
                8,
                0.1, 0.1, 0.1,
                0.03);

        // Sonido de impacto: golpe contra algo rígido
        mundo.playSound(punto, Sound.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.5f, 1.6f);
        mundo.playSound(punto, Sound.ITEM_SHIELD_BLOCK, SoundCategory.BLOCKS, 0.7f, 1.0f);
    }

    /**
     * Efecto de rebote visual cuando un jugador intenta entrar a una zona
     * restringida.
     */
    public void onReboteEntrada(Location punto, ProtectionRecord rec) {
        World mundo = punto.getWorld();
        if (mundo == null)
            return;

        FxTheme tema = getTema(rec);
        // punto ya llega con el centro del bloque correcto (offset aplicado por el
        // caller)

        // Partículas de impacto
        mundo.spawnParticle(
                tema.getParticuArista(),
                punto,
                30,
                0.2, 0.2, 0.2,
                0.06,
                tema.getOpcionesArista());

        // Onda de choque pequeña
        mundo.spawnParticle(
                Particle.CRIT,
                punto,
                15,
                0.3, 0.1, 0.3,
                0.05);

        // Sonido de rebote
        mundo.playSound(punto, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.6f, 1.4f);
        mundo.playSound(punto, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8f, 0.9f);
    }

    // ---------------------------------------------------------------
    // Renderizado periódico interno
    // ---------------------------------------------------------------

    /**
     * Dibuja las 12 aristas del cubo con partículas.
     * El brillo pulsa según el ciclo global.
     */
    private void renderAristas(World mundo, CubeRegion cubo, FxTheme tema) {
        // Factor de pulso: oscila entre pulsoMin y pulsoMax
        double factor = pulsoMin + (pulsoMax - pulsoMin)
                * (0.5 + 0.5 * Math.sin(2.0 * Math.PI * tickPulso / pulsoPeriodo));

        List<List<Location>> aristas = cubo.aristas(mundo);

        for (List<Location> arista : aristas) {
            // Para aristas largas, muestreamos cada N puntos para no saturar
            int paso = Math.max(1, arista.size() / 10);

            for (int i = 0; i < arista.size(); i += paso) {
                Location punto = arista.get(i);

                // Decide si este punto se renderea según el pulso (efecto parpadeo)
                if (Math.random() > factor)
                    continue;

                mundo.spawnParticle(
                        tema.getParticuArista(),
                        punto,
                        1,
                        0.0, 0.0, 0.0,
                        0.0,
                        tema.getOpcionesArista());
            }
        }
    }

    /**
     * Partículas ambientes flotando en las caras del cubo.
     * Muy pocas por tick para mantener rendimiento, pero constantes.
     */
    private void renderCara(World mundo, CubeRegion cubo, FxTheme tema) {
        List<Location> caras = cubo.puntosCara(mundo, cubo.getRadio() > 16 ? 4 : 3);
        if (caras.isEmpty())
            return;

        for (int i = 0; i < ambientePorCara; i++) {
            int idx = (int) (Math.random() * caras.size());
            Location punto = caras.get(idx);

            mundo.spawnParticle(
                    tema.getParticuCara(),
                    punto,
                    1,
                    0.0, 0.05, 0.0,
                    0.01,
                    tema.getOpcionesCara());
        }
    }

    /**
     * Anillo de expansión que sale del centro con un radio creciente.
     */
    private void anilloExpansion(World mundo, Location centro, FxTheme tema, double radio) {
        int cantidad = (int) (radio * 12);
        for (int i = 0; i < cantidad; i++) {
            double angulo = 2.0 * Math.PI * i / cantidad;
            double x = centro.getX() + Math.cos(angulo) * radio;
            double z = centro.getZ() + Math.sin(angulo) * radio;
            Location punto = new Location(mundo, x, centro.getY(), z);

            mundo.spawnParticle(
                    tema.getParticuArista(),
                    punto,
                    2,
                    0.0, 0.15, 0.0,
                    0.02,
                    tema.getOpcionesArista());
        }
    }

    // ---------------------------------------------------------------
    // Cache de temas
    // ---------------------------------------------------------------

    private FxTheme getTema(ProtectionRecord rec) {
        String key = rec.getTipo().getConfigKey();
        return temaCache.computeIfAbsent(key, k -> {
            ConfigurationSection sec = fxConfig.getConfigurationSection(
                    "tipos-proteccion." + k);
            return sec != null
                    ? new FxTheme(sec)
                    : new FxTheme(fxConfig.getConfigurationSection("tipos-proteccion.area"));
        });
    }

    /** Limpia el cache de temas (usar al hacer reload). */
    public void clearCache() {
        temaCache.clear();
    }
}
