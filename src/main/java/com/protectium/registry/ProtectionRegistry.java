package com.protectium.registry;

import com.protectium.protection.ProtectionRecord;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registro central de todas las protecciones activas en el servidor.
 * Es el único lugar donde se guardan protecciones vivas.
 *
 * Índices optimizados:
 * - Por clave de ubicación (mundo:x:y:z) → búsqueda O(1) al colocar/romper
 * - Por chunk (mundo:chunkX:chunkZ) → búsqueda O(1) para eventos en área
 * - Por mundo → filtrado rápido al iterar por mundo
 *
 * Thread-safe: usa ConcurrentHashMap. Las lecturas son sin bloqueo.
 */
public final class ProtectionRegistry {

    // Índice principal: clave de ubicación → record
    private final ConcurrentHashMap<String, ProtectionRecord> porUbicacion = new ConcurrentHashMap<>();

    // Índice por chunk: "mundo:chunkX:chunkZ" → set de claves de ubicación
    private final ConcurrentHashMap<String, Set<String>> porChunk = new ConcurrentHashMap<>();

    // Índice por mundo: nombre de mundo → set de claves de ubicación
    private final ConcurrentHashMap<String, Set<String>> porMundo = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------
    // Registro / eliminación
    // ---------------------------------------------------------------

    /**
     * Registra una protección activa. Si ya existe en esa ubicación, la reemplaza.
     * Indexa automáticamente en todos los chunks que cubre la protección.
     */
    public void registrar(ProtectionRecord record) {
        String clave = record.clave();
        String mundo = record.getUbicacionBloque().getWorld().getName();

        // Índice principal
        porUbicacion.put(clave, record);

        // Índice por mundo
        porMundo.computeIfAbsent(mundo, k -> ConcurrentHashMap.newKeySet()).add(clave);

        // Índice por chunks (todos los chunks que cubre la protección)
        indexarEnChunks(record, clave);
    }

    /**
     * Elimina la protección en la ubicación dada. Retorna el record eliminado o
     * null.
     */
    public ProtectionRecord eliminar(Location ubicacion) {
        String clave = ProtectionRecord.clave(ubicacion);
        ProtectionRecord eliminado = porUbicacion.remove(clave);

        if (eliminado != null) {
            String mundo = ubicacion.getWorld().getName();

            // Remover de índice por mundo
            Set<String> clavesMundo = porMundo.get(mundo);
            if (clavesMundo != null) {
                clavesMundo.remove(clave);
                if (clavesMundo.isEmpty()) {
                    porMundo.remove(mundo);
                }
            }

            // Remover de índice por chunks
            desindexarDeChunks(eliminado, clave);
        }

        return eliminado;
    }

    // ---------------------------------------------------------------
    // Indexación por chunks
    // ---------------------------------------------------------------

    /**
     * Indexa la protección en todos los chunks que su cubo cubre.
     */
    private void indexarEnChunks(ProtectionRecord record, String clave) {
        String mundo = record.getUbicacionBloque().getWorld().getName();
        var cubo = record.getCubo();

        // Calcular rango de chunks cubiertos
        int minChunkX = cubo.getMinX() >> 4;
        int maxChunkX = cubo.getMaxX() >> 4;
        int minChunkZ = cubo.getMinZ() >> 4;
        int maxChunkZ = cubo.getMaxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                String chunkKey = mundo + ":" + cx + ":" + cz;
                porChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(clave);
            }
        }
    }

    /**
     * Remueve la protección de todos los chunks donde estaba indexada.
     */
    private void desindexarDeChunks(ProtectionRecord record, String clave) {
        String mundo = record.getUbicacionBloque().getWorld().getName();
        var cubo = record.getCubo();

        int minChunkX = cubo.getMinX() >> 4;
        int maxChunkX = cubo.getMaxX() >> 4;
        int minChunkZ = cubo.getMinZ() >> 4;
        int maxChunkZ = cubo.getMaxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                String chunkKey = mundo + ":" + cx + ":" + cz;
                Set<String> claves = porChunk.get(chunkKey);
                if (claves != null) {
                    claves.remove(clave);
                    if (claves.isEmpty()) {
                        porChunk.remove(chunkKey);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    /**
     * ¿Existe una protección exactamente en esta ubicación?
     */
    public boolean existeEn(Location ubicacion) {
        return porUbicacion.containsKey(ProtectionRecord.clave(ubicacion));
    }

    /**
     * Obtiene la protección en esta ubicación exacta. Puede ser null.
     */
    public ProtectionRecord obtenerEn(Location ubicacion) {
        return porUbicacion.get(ProtectionRecord.clave(ubicacion));
    }

    /**
     * Busca TODAS las protecciones cuyo cubo contiene el punto dado.
     * OPTIMIZADO: Solo revisa protecciones indexadas en el chunk del punto.
     * Complejidad: O(k) donde k = protecciones en ese chunk, no todas.
     */
    public List<ProtectionRecord> buscarContenedoras(Location punto) {
        if (punto.getWorld() == null)
            return Collections.emptyList();

        String mundo = punto.getWorld().getName();
        int chunkX = punto.getBlockX() >> 4;
        int chunkZ = punto.getBlockZ() >> 4;
        String chunkKey = mundo + ":" + chunkX + ":" + chunkZ;

        Set<String> clavesEnChunk = porChunk.get(chunkKey);
        if (clavesEnChunk == null || clavesEnChunk.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProtectionRecord> resultado = new ArrayList<>();
        for (String clave : clavesEnChunk) {
            ProtectionRecord rec = porUbicacion.get(clave);
            if (rec != null && rec.getCubo().contiene(punto)) {
                resultado.add(rec);
            }
        }
        return resultado;
    }

    /**
     * Todas las protecciones activas en un mundo específico.
     */
    public List<ProtectionRecord> todosPorMundo(String nombreMundo) {
        Set<String> claves = porMundo.get(nombreMundo);
        if (claves == null)
            return Collections.emptyList();

        return claves.stream()
                .map(porUbicacion::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Todas las protecciones activas en todo el servidor.
     */
    public List<ProtectionRecord> todas() {
        return new ArrayList<>(porUbicacion.values());
    }

    /**
     * Cantidad total de protecciones activas.
     */
    public int cantidad() {
        return porUbicacion.size();
    }

    /**
     * Limpia todo el registro. Solo para uso en shutdown/reload.
     */
    public void limpiar() {
        porUbicacion.clear();
        porChunk.clear();
        porMundo.clear();
    }
}
