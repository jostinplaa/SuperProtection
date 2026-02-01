package com.protectium.protection;

import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de una protección activa.
 * Se crea el instante en que el bloque es colocado.
 * Se elimina el instante en que ese bloque es roto.
 *
 * Ahora incluye sistema de miembros/trust con roles.
 */
public final class ProtectionRecord {

    private final UUID id;
    private final ProtectionType tipo;
    private final Location ubicacionBloque;
    private final CubeRegion cubo;
    private final UUID colocadoPor;
    private final long marcaTiempo;

    // Sistema de miembros: UUID del jugador → rol
    private final Map<UUID, MemberRole> miembros = new ConcurrentHashMap<>();

    // Flags configurables
    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

    /**
     * Roles de miembros en una protección.
     */
    public enum MemberRole {
        OWNER("Dueño", "§6"),
        MODERATOR("Moderador", "§b"),
        MEMBER("Miembro", "§a"),
        VISITOR("Visitante", "§7");

        private final String displayName;
        private final String color;

        MemberRole(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    // ---------------------------------------------------------------
    // Constructores
    // ---------------------------------------------------------------

    public ProtectionRecord(ProtectionType tipo,
            Location ubicacionBloque,
            int radio,
            UUID colocadoPor) {
        this.id = UUID.randomUUID();
        this.tipo = tipo;
        this.ubicacionBloque = ubicacionBloque.clone();
        this.cubo = new CubeRegion(ubicacionBloque, radio);
        this.colocadoPor = colocadoPor;
        this.marcaTiempo = System.currentTimeMillis();

        // El dueño es automáticamente OWNER
        this.miembros.put(colocadoPor, MemberRole.OWNER);

        // Flags por defecto
        initDefaultFlags();
    }

    /**
     * Constructor para cargar desde persistencia.
     */
    public ProtectionRecord(UUID id, ProtectionType tipo, Location ubicacionBloque,
            int radio, UUID colocadoPor, long marcaTiempo,
            Map<UUID, MemberRole> miembros, Map<String, Boolean> flags) {
        this.id = id;
        this.tipo = tipo;
        this.ubicacionBloque = ubicacionBloque.clone();
        this.cubo = new CubeRegion(ubicacionBloque, radio);
        this.colocadoPor = colocadoPor;
        this.marcaTiempo = marcaTiempo;
        this.miembros.putAll(miembros);
        this.flags.putAll(flags);
    }

    private void initDefaultFlags() {
        flags.put("block-break", true);
        flags.put("block-place", true);
        flags.put("interact", true);
        flags.put("pvp", false);
        flags.put("explosions", true);
        flags.put("fire", true);
        flags.put("mob-spawning", false);
        // New V3 Flags
        flags.put("damage", false); // Entities taking damage
        flags.put("interact-entity", true); // Villagers, item frames, etc.
        flags.put("item-drop", true);
        flags.put("item-pickup", true);
    }

    // ---------------------------------------------------------------
    // Sistema de miembros
    // ---------------------------------------------------------------

    /**
     * Añade un miembro con el rol especificado.
     */
    public void addMember(UUID playerId, MemberRole role) {
        miembros.put(playerId, role);
    }

    /**
     * Remueve un miembro de la protección.
     */
    public boolean removeMember(UUID playerId) {
        // No permitir remover al dueño
        if (playerId.equals(colocadoPor))
            return false;
        return miembros.remove(playerId) != null;
    }

    /**
     * Obtiene el rol de un jugador. Retorna null si no es miembro.
     */
    public MemberRole getMemberRole(UUID playerId) {
        return miembros.get(playerId);
    }

    /**
     * ¿Es este jugador miembro (cualquier rol)?
     */
    public boolean isMember(UUID playerId) {
        return miembros.containsKey(playerId);
    }

    /**
     * ¿Este jugador tiene bypass (puede interactuar)?
     * Dueño y Moderadores tienen bypass.
     */
    public boolean hasInteractPermission(UUID playerId) {
        MemberRole role = miembros.get(playerId);
        if (role == null)
            return false;
        return role == MemberRole.OWNER || role == MemberRole.MODERATOR || role == MemberRole.MEMBER;
    }

    /**
     * ¿Este jugador puede modificar la configuración?
     * Solo dueño y moderadores.
     */
    public boolean canModify(UUID playerId) {
        MemberRole role = miembros.get(playerId);
        if (role == null)
            return false;
        return role == MemberRole.OWNER || role == MemberRole.MODERATOR;
    }

    /**
     * ¿Es el dueño de la protección?
     */
    public boolean isOwner(UUID playerId) {
        return playerId.equals(colocadoPor);
    }

    /**
     * Lista todos los miembros.
     */
    public Map<UUID, MemberRole> getMembers() {
        return new HashMap<>(miembros);
    }

    // ---------------------------------------------------------------
    // Sistema de flags
    // ---------------------------------------------------------------

    public void setFlag(String flag, boolean value) {
        flags.put(flag, value);
    }

    public boolean getFlag(String flag, boolean defaultValue) {
        return flags.getOrDefault(flag, defaultValue);
    }

    public Map<String, Boolean> getFlags() {
        return new HashMap<>(flags);
    }

    // ---------------------------------------------------------------
    // Accessors originales
    // ---------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public ProtectionType getTipo() {
        return tipo;
    }

    public Location getUbicacionBloque() {
        return ubicacionBloque.clone();
    }

    public CubeRegion getCubo() {
        return cubo;
    }

    public UUID getColocadoPor() {
        return colocadoPor;
    }

    public long getMarcaTiempo() {
        return marcaTiempo;
    }

    public int getRadio() {
        return cubo.getRadio();
    }

    // ---------------------------------------------------------------
    // Clave única para indexado rápido por ubicación
    // ---------------------------------------------------------------

    public static String clave(Location loc) {
        return loc.getWorld().getName() + ":"
                + loc.getBlockX() + ":"
                + loc.getBlockY() + ":"
                + loc.getBlockZ();
    }

    public String clave() {
        return clave(ubicacionBloque);
    }

    @Override
    public String toString() {
        return String.format("Protección[%s tipo=%s cubo=%s por=%s miembros=%d]",
                id, tipo, cubo, colocadoPor, miembros.size());
    }
}
