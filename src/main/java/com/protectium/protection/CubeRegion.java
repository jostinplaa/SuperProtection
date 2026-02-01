package com.protectium.protection;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Representación geométrica pura de un cubo centrado en un bloque.
 * No tiene estado mutable ni referencia al mundo — solo matemáticas.
 * Todo el cálculo espacial del plugin pasa por aquí.
 */
public final class CubeRegion {

    private final int centerX, centerY, centerZ;
    private final int radius;
    // Límites absolutos del cubo (inclusive)
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final String worldName;

    public CubeRegion(Location center, int radius) {
        this.centerX = center.getBlockX();
        this.centerY = center.getBlockY();
        this.centerZ = center.getBlockZ();
        this.radius = radius;
        this.worldName = center.getWorld().getName();

        this.minX = centerX - radius;
        this.minY = centerY - radius;
        this.minZ = centerZ - radius;
        this.maxX = centerX + radius;
        this.maxY = centerY + radius;
        this.maxZ = centerZ + radius;
    }

    // ---------------------------------------------------------------
    // Consultas espaciales
    // ---------------------------------------------------------------

    /** ¿El punto dado cae dentro de la columna infinita (X/Z) del cubo? */
    public boolean contiene(Location punto) {
        if (punto.getWorld() == null)
            return false;
        if (!punto.getWorld().getName().equals(worldName))
            return false;
        int x = punto.getBlockX(), z = punto.getBlockZ();
        // Ignoramos Y para rango vertical infinito
        return x >= minX && x <= maxX
                && z >= minZ && z <= maxZ;
    }

    /** ¿El punto dado cae dentro de la columna infinita (X/Z)? */
    public boolean contiene(String world, int x, int y, int z) {
        if (!world.equals(worldName))
            return false;
        // Ignoramos Y para rango vertical infinito
        return x >= minX && x <= maxX
                && z >= minZ && z <= maxZ;
    }

    /** ¿El punto dado está exactamente sobre una cara del cubo? */
    public boolean estaEnCara(Location punto) {
        if (!contiene(punto))
            return false;
        int x = punto.getBlockX(), y = punto.getBlockY(), z = punto.getBlockZ();
        return x == minX || x == maxX
                || y == minY || y == maxY
                || z == minZ || z == maxZ;
    }

    // ---------------------------------------------------------------
    // Generadores de puntos para efectos visuales
    // ---------------------------------------------------------------

    /**
     * Genera los 12 segmentos de arista del cubo.
     * Cada arista es una lista de ubicaciones desde un extremo al otro.
     * Se usa para renderear las líneas brillantes del cubo.
     */
    public List<List<Location>> aristas(World world) {
        List<List<Location>> aristas = new ArrayList<>(12);

        // 4 aristas paralelas al eje X
        aristas.add(segmento(world, minX, minY, minZ, maxX, minY, minZ));
        aristas.add(segmento(world, minX, maxY, minZ, maxX, maxY, minZ));
        aristas.add(segmento(world, minX, minY, maxZ, maxX, minY, maxZ));
        aristas.add(segmento(world, minX, maxY, maxZ, maxX, maxY, maxZ));

        // 4 aristas paralelas al eje Y
        aristas.add(segmento(world, minX, minY, minZ, minX, maxY, minZ));
        aristas.add(segmento(world, maxX, minY, minZ, maxX, maxY, minZ));
        aristas.add(segmento(world, minX, minY, maxZ, minX, maxY, maxZ));
        aristas.add(segmento(world, maxX, minY, maxZ, maxX, maxY, maxZ));

        // 4 aristas paralelas al eje Z
        aristas.add(segmento(world, minX, minY, minZ, minX, minY, maxZ));
        aristas.add(segmento(world, maxX, minY, minZ, maxX, minY, maxZ));
        aristas.add(segmento(world, minX, maxY, minZ, minX, maxY, maxZ));
        aristas.add(segmento(world, maxX, maxY, minZ, maxX, maxY, maxZ));

        return aristas;
    }

    /**
     * Las 8 esquinas exactas del cubo. Punto de referencia para bursts.
     */
    public List<Location> esquinas(World world) {
        List<Location> esquinas = new ArrayList<>(8);
        esquinas.add(loc(world, minX, minY, minZ));
        esquinas.add(loc(world, maxX, minY, minZ));
        esquinas.add(loc(world, minX, maxY, minZ));
        esquinas.add(loc(world, maxX, maxY, minZ));
        esquinas.add(loc(world, minX, minY, maxZ));
        esquinas.add(loc(world, maxX, minY, maxZ));
        esquinas.add(loc(world, minX, maxY, maxZ));
        esquinas.add(loc(world, maxX, maxY, maxZ));
        return esquinas;
    }

    /**
     * Puntos muestreados sobre las 6 caras del cubo.
     * Se usa para partículas ambientes que van flotando en cada cara.
     * El paso controla la densidad: paso=2 → una partícula cada 2 bloques.
     */
    public List<Location> puntosCara(World world, int paso) {
        List<Location> puntos = new ArrayList<>();
        paso = Math.max(1, paso);

        // Cara X- y X+
        for (int y = minY; y <= maxY; y += paso)
            for (int z = minZ; z <= maxZ; z += paso) {
                puntos.add(loc(world, minX, y, z));
                puntos.add(loc(world, maxX, y, z));
            }
        // Cara Y- y Y+
        for (int x = minX + paso; x < maxX; x += paso)
            for (int z = minZ; z <= maxZ; z += paso) {
                puntos.add(loc(world, x, minY, z));
                puntos.add(loc(world, x, maxY, z));
            }
        // Cara Z- y Z+
        for (int x = minX + paso; x < maxX; x += paso)
            for (int y = minY + paso; y < maxY; y += paso) {
                puntos.add(loc(world, x, y, minZ));
                puntos.add(loc(world, x, y, maxZ));
            }

        return puntos;
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public int getRadio() {
        return radius;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public String getWorldName() {
        return worldName;
    }

    // ---------------------------------------------------------------
    // Utilidades privadas
    // ---------------------------------------------------------------

    private static Location loc(World w, int x, int y, int z) {
        return new Location(w, x + 0.5, y, z + 0.5);
    }

    /**
     * Genera una línea de puntos entre dos extremos (inclusive).
     * Solo una de las tres coordenadas varía a la vez (aristas alineadas a eje).
     */
    private static List<Location> segmento(World w,
            int x1, int y1, int z1,
            int x2, int y2, int z2) {
        List<Location> seg = new ArrayList<>();
        int dx = Integer.signum(x2 - x1);
        int dy = Integer.signum(y2 - y1);
        int dz = Integer.signum(z2 - z1);
        int pasos = Math.max(Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)), Math.abs(z2 - z1));

        for (int i = 0; i <= pasos; i++) {
            seg.add(loc(w, x1 + i * dx, y1 + i * dy, z1 + i * dz));
        }
        return seg;
    }

    @Override
    public String toString() {
        return String.format("Cubo[centro=(%d,%d,%d) radio=%d mundo=%s]",
                centerX, centerY, centerZ, radius, worldName);
    }
}
