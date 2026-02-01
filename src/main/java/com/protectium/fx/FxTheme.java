package com.protectium.fx;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Tema visual completo para un tipo de protección.
 * Compatible con API 1.20.4 (compilación) y 1.21+ (ejecución).
 * Usa Particle.REDSTONE en lugar de DUST para compilar con API antigua.
 */
public final class FxTheme {

    private final Particle particuArista;
    private final Particle.DustOptions opcionesArista;

    private final Particle particuEsquina;
    private final Particle.DustOptions opcionesEsquina;

    private final Particle particuCara;
    private final Particle.DustOptions opcionesCara;

    private final String sonidoColocar;
    private final String sonidoRomper;

    public FxTheme(ConfigurationSection sec) {
        // Aristas
        Object[] aristaData = parsear(sec.getString("fx-arista", "DUST 0 180 255"));
        this.particuArista = (Particle) aristaData[0];
        this.opcionesArista = (Particle.DustOptions) aristaData[1];

        // Esquinas
        Object[] esquinaData = parsear(sec.getString("fx-esquina", "DUST 0 255 200"));
        this.particuEsquina = (Particle) esquinaData[0];
        this.opcionesEsquina = (Particle.DustOptions) esquinaData[1];

        // Caras
        Object[] caraData = parsear(sec.getString("fx-cara", "DUST 100 100 255"));
        this.particuCara = (Particle) caraData[0];
        this.opcionesCara = (Particle.DustOptions) caraData[1];

        // Sonidos
        this.sonidoColocar = sec.getString("sonido-colocar", "BLOCK_GLASS_PLACE");
        this.sonidoRomper = sec.getString("sonido-romper", "BLOCK_GLASS_BREAK");
    }

    public Particle getParticuArista() {
        return particuArista;
    }

    public Particle.DustOptions getOpcionesArista() {
        return opcionesArista;
    }

    public Particle getParticuEsquina() {
        return particuEsquina;
    }

    public Particle.DustOptions getOpcionesEsquina() {
        return opcionesEsquina;
    }

    public Particle getParticuCara() {
        return particuCara;
    }

    public Particle.DustOptions getOpcionesCara() {
        return opcionesCara;
    }

    public String getSonidoColocar() {
        return sonidoColocar;
    }

    public String getSonidoRomper() {
        return sonidoRomper;
    }

    /**
     * Parsea strings de configuración.
     * Siempre devuelve REDSTONE (DUST) con opciones para evitar crash por falta de
     * datos.
     */
    private static Object[] parsear(String token) {
        if (token == null || token.isBlank()) {
            return new Object[] {
                    Particle.REDSTONE,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f)
            };
        }

        String[] partes = token.trim().split("\\s+");
        String nombre = partes[0].toUpperCase();

        // Soporte para DUST o REDSTONE
        if ((nombre.equals("DUST") || nombre.equals("REDSTONE")) && partes.length >= 4) {
            try {
                int r = clamp(Integer.parseInt(partes[1]), 0, 255);
                int g = clamp(Integer.parseInt(partes[2]), 0, 255);
                int b = clamp(Integer.parseInt(partes[3]), 0, 255);
                float tamanio = partes.length >= 5 ? Float.parseFloat(partes[4]) : 1.2f;

                return new Object[] {
                        Particle.REDSTONE,
                        new Particle.DustOptions(Color.fromRGB(r, g, b), tamanio)
                };
            } catch (NumberFormatException ignored) {
            }
        }

        // Fallback seguro
        return new Object[] {
                Particle.REDSTONE,
                new Particle.DustOptions(Color.fromRGB(150, 150, 255), 1.0f)
        };
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
