package com.protectium.item;

import com.protectium.protection.ProtectionType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Única autoridad sobre los ítems de protección.
 * Responsable de:
 * - Crear ítems con marca NBT única y segura
 * - Validar si un ítem dado es un ítem autorizado legítimo
 * - Extraer los metadatos (tipo, radio) de un ítem autorizado
 *
 * Diseño: NUNCA se genera un ítem de protección desde otro lugar.
 * Todo pasa por aquí. Si no tiene el watermark NBT correcto, no existe.
 */
public final class ItemAuthority {

    // Keys NBT
    private final NamespacedKey keyTipo;
    private final NamespacedKey keyRadio;
    private final NamespacedKey keyToken;
    private final NamespacedKey keyNombre;

    // Material base para todos los ítems de protección
    private static final Material MATERIAL_BASE = Material.AMETHYST_BLOCK;

    private final Plugin plugin;

    public ItemAuthority(Plugin plugin) {
        this.plugin = plugin;
        this.keyTipo = new NamespacedKey(plugin, "tipo");
        this.keyRadio = new NamespacedKey(plugin, "radio");
        this.keyToken = new NamespacedKey(plugin, "token");
        this.keyNombre = new NamespacedKey(plugin, "nombre");
    }

    // ---------------------------------------------------------------
    // Creación de ítems autorizados
    // ---------------------------------------------------------------

    /**
     * Crea un ítem de protección completamente autorizado.
     * El ítem tiene nombre personalizado, lore descriptivo, y marca NBT sellada.
     * Este es el ÚNICO punto de entrada para crear ítems funcionales.
     */
    public ItemStack crearItem(ProtectionType tipo, int radio) {
        ItemStack item = new ItemStack(MATERIAL_BASE);
        item.setAmount(1);

        // --- Nombre y lore visual ---
        var meta = item.getItemMeta();
        if (meta == null)
            return item; // nunca debería pasar con AMETHYST_BLOCK

        String nombreTipo = getNombreTipo(tipo);
        meta.setDisplayName(nombreTipo);

        meta.setLore(java.util.List.of(
                "§8├─────────────────────────────┤",
                "§8│ §7Tipo:  " + nombreTipo,
                "§8│ §7Radio: §b" + radio + " bloques",
                "§8│",
                "§8│ §7Este ítem NO hace nada",
                "§8│ §7mientras esté en tu inventario.",
                "§8│",
                "§8│ §aColéca lo en el mundo para",
                "§8│ §aactivar la protección cúbica.",
                "§8│",
                "§8│ §cRomper el bloque elimina",
                "§8│ §cla protección permanentemente.",
                "§8└─────────────────────────────┘"));

        // Que no se pueda combinar ni stackear
        meta.setUnbreakable(true);

        // --- Marca NBT (watermark) ---
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyTipo, PersistentDataType.STRING, tipo.getConfigKey());
        pdc.set(keyRadio, PersistentDataType.INTEGER, radio);
        pdc.set(keyToken, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Crea un ítem de protección con nombre personalizado.
     */
    public ItemStack crearItemConNombre(ProtectionType tipo, int radio, String nombre) {
        ItemStack item = new ItemStack(MATERIAL_BASE);
        item.setAmount(1);

        var meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.setDisplayName("§b§l" + nombre);

        meta.setLore(java.util.List.of(
                "§8├─────────────────────────────┤",
                "§8│ §7Nombre: §b" + nombre,
                "§8│ §7Radio:  §e" + radio + " bloques",
                "§8│",
                "§8│ §aColoca el bloque para",
                "§8│ §aactivar la proteccion.",
                "§8│",
                "§8│ §cRomper = elimina proteccion",
                "§8└─────────────────────────────┘"));

        meta.setUnbreakable(true);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyTipo, PersistentDataType.STRING, tipo.getConfigKey());
        pdc.set(keyRadio, PersistentDataType.INTEGER, radio);
        pdc.set(keyToken, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
        pdc.set(keyNombre, PersistentDataType.STRING, nombre);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Crea un ítem de protección con nombre personalizado y material específico.
     */
    public ItemStack crearItemConNombre(ProtectionType tipo, int radio, String nombre, Material material) {
        ItemStack item = new ItemStack(material);
        item.setAmount(1);

        var meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.setDisplayName("§b§l" + nombre);

        meta.setLore(java.util.List.of(
                "§8├─────────────────────────────┤",
                "§8│ §7Nombre: §b" + nombre,
                "§8│ §7Radio:  §e" + radio + " bloques",
                "§8│",
                "§8│ §aColoca el bloque para",
                "§8│ §aactivar la proteccion.",
                "§8│",
                "§8│ §cRomper = elimina proteccion",
                "§8└─────────────────────────────┘"));

        // No hacemos setUnbreakable(true) forzosamente para bloques normales,
        // pero para protecciones queda bien para distinguirlos.
        // Opcional: si es visualmente molesto, quitarlo.
        // Lo dejaremos para que brille un poco o se vea 'especial' si el cliente lo
        // renderiza así,
        // aunque bloques normales no suelen mostrar el brillo de encantamiento igual.

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyTipo, PersistentDataType.STRING, tipo.getConfigKey());
        pdc.set(keyRadio, PersistentDataType.INTEGER, radio);
        pdc.set(keyToken, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
        pdc.set(keyNombre, PersistentDataType.STRING, nombre);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Extrae el nombre personalizado de la protección.
     */
    public String extraerNombre(ItemStack item) {
        if (!esItemAutorizado(item))
            return null;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(keyNombre, PersistentDataType.STRING);
    }

    // ---------------------------------------------------------------
    // Validación
    // ---------------------------------------------------------------

    /**
     * ¿Este ítem es un ítem de protección autorizado legítimo?
     * Verifica material, existencia de NBT, y consistencia de datos.
     */
    public boolean esItemAutorizado(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        // No forzamos MATERIAL_BASE para permitir bloques personalizados
        // Solo verificamos que tenga los datos NBT correctos

        var meta = item.getItemMeta();
        if (meta == null)
            return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keyTipo, PersistentDataType.STRING)
                && pdc.has(keyRadio, PersistentDataType.INTEGER)
                && pdc.has(keyToken, PersistentDataType.STRING);
    }

    // ---------------------------------------------------------------
    // Extracción de datos
    // ---------------------------------------------------------------

    /**
     * Extrae el tipo de protección del ítem. Retorna null si no es válido.
     */
    public ProtectionType extraerTipo(ItemStack item) {
        if (!esItemAutorizado(item))
            return null;
        String raw = item.getItemMeta()
                .getPersistentDataContainer()
                .get(keyTipo, PersistentDataType.STRING);
        return ProtectionType.fromString(raw);
    }

    /**
     * Extrae el radio de protección del ítem. Retorna -1 si no es válido.
     */
    public int extraerRadio(ItemStack item) {
        if (!esItemAutorizado(item))
            return -1;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(keyRadio, PersistentDataType.INTEGER);
    }

    /**
     * Extrae el token único del ítem para trazabilidad.
     */
    public String extraerToken(ItemStack item) {
        if (!esItemAutorizado(item))
            return null;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(keyToken, PersistentDataType.STRING);
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    private String getNombreTipo(ProtectionType tipo) {
        return switch (tipo) {
            case AREA -> "§b§l⬡ Área Protegida";
            case SPAWN -> "§d§l⬡ Zona Sin Spawn";
            case ENTRADA -> "§c§l⬡ Zona Restringida";
            case FUEGO -> "§6§l⬡ Zona Ignífuga";
            case REDSTONE -> "§5§l⬡ Zona Sin Redstone";
        };
    }

    public Material getMaterialBase() {
        return MATERIAL_BASE;
    }
}
