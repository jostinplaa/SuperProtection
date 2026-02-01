package com.protectium.gui;

import com.protectium.core.Mensajes;
import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor completo de GUIs del plugin.
 * Maneja todos los menús: principal, lista, flags, miembros, etc.
 */
public final class GuiManager {

    private final ProtectionRegistry registry;
    private final Mensajes mensajes;

    // Cache de contexto para GUIs abiertas
    private final Map<UUID, GuiContext> contextos = new ConcurrentHashMap<>();

    public GuiManager(ProtectionRegistry registry, Mensajes mensajes) {
        this.registry = registry;
        this.mensajes = mensajes;
    }

    /**
     * Contexto de GUI abierta para un jugador.
     */
    public static class GuiContext {
        public GuiTipo tipo;
        public ProtectionRecord proteccion;
        public int pagina;

        public GuiContext(GuiTipo tipo) {
            this.tipo = tipo;
            this.pagina = 0;
        }
    }

    // ---------------------------------------------------------------
    // GUI Tienda
    // ---------------------------------------------------------------

    public void abrirTienda(Player jugador, com.protectium.shop.ShopManager shop) {
        // Forzar 6 filas para diseño premium
        int filas = 6;
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.TIENDA),
                filas * 9, "§8🛒 §2§lTIENDA DE PROTECCIONES");

        // Rellenar bordes con cristal gris
        ItemStack borde = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, borde);
            }
        }

        // Botón cerrar
        inv.setItem(49, crearItemConLore(Material.BARRIER, "§c§lCerrar", List.of()));

        java.util.List<com.protectium.shop.ShopManager.ShopItem> items = shop.getItems();

        // Rellenar items en el centro (slots 10-16, 19-25, etc.)
        int[] slotsDisponibles = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < items.size() && i < slotsDisponibles.length; i++) {
            com.protectium.shop.ShopManager.ShopItem shopItem = items.get(i);
            ItemStack display = shopItem.getItem().clone(); // Clone to safety
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add("§8────────────────");
                lore.add("§7 Precio: §a$" + shopItem.getPrecio());
                lore.add("");
                lore.add("§e➤ Click para comprar");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey("protectium", "shop_id"),
                        org.bukkit.persistence.PersistentDataType.STRING, shopItem.getId());
                display.setItemMeta(meta);
            }
            inv.setItem(slotsDisponibles[i], display);
        }

        jugador.openInventory(inv);
        GuiContext ctx = new GuiContext(GuiTipo.TIENDA);
        contextos.put(jugador.getUniqueId(), ctx);
    }

    // ---------------------------------------------------------------
    // GUI Principal de una Protección — Click en bloque de protección
    // ---------------------------------------------------------------

    /**
     * Abre el menú de gestión de una protección específica.
     */
    public void abrirMenuProteccion(Player jugador, ProtectionRecord rec) {
        int filas = 5;
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.MENU_PROTECCION),
                filas * 9, "§b§l⬡ §8Gestión de Protección");

        // Fondo oscuro
        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, fondo);

        // Bordes decorativos
        ItemStack borde = crearItem(Material.BLACK_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, borde);
            inv.setItem(inv.getSize() - 9 + i, borde);
        }
        for (int i = 0; i < filas; i++) {
            inv.setItem(i * 9, borde);
            inv.setItem(i * 9 + 8, borde);
        }

        Location loc = rec.getUbicacionBloque();

        // --- Información de la protección (centro superior) ---
        inv.setItem(4, crearItemConLore(
                getMaterialPorTipo(rec.getTipo()),
                getNombreTipo(rec.getTipo()),
                List.of(
                        "§8├─────────────────────────┤",
                        "§7 Radio: §b" + rec.getRadio() + " bloques",
                        "§7 Ubicación: §b" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                        "§7 Mundo: §b" + loc.getWorld().getName(),
                        "§7 Miembros: §b" + rec.getMembers().size(),
                        "§8└─────────────────────────┘")));

        // --- Botón: Configurar Flags ---
        inv.setItem(20, crearItemConLore(
                Material.REDSTONE_TORCH,
                "§e§l⚙ Configurar Flags",
                List.of(
                        "§7Configura los permisos de la zona.",
                        "",
                        "§8▶ §fClick para abrir")));

        // --- Botón: Gestionar Miembros ---
        inv.setItem(22, crearItemConLore(
                Material.PLAYER_HEAD,
                "§3§l👥 Miembros",
                List.of(
                        "§7Gestiona quién puede entrar o editar.",
                        "",
                        "§8▶ §fClick para abrir")));

        // --- Botón: Teletransportarse ---
        inv.setItem(24, crearItemConLore(
                Material.ENDER_PEARL,
                "§d§l⚛ Teletransportarse",
                List.of(
                        "§7Viaja al centro de la protección.",
                        "",
                        "§8▶ §fClick para viajar")));

        // --- Botón: Eliminar ---
        inv.setItem(40, crearItemConLore(
                Material.TNT,
                "§c§l✖ ELIMINAR PROTECCIÓN",
                List.of(
                        "§7Borra esta protección permanentemente.",
                        "§c¡No se puede deshacer!",
                        "",
                        "§8▶ §fClick para eliminar")));

        // --- Botón: Cerrar ---
        inv.setItem(44, crearItemConLore(
                Material.BARRIER,
                "§c§l✕ Cerrar",
                List.of("§7Cierra este menú.")));

        jugador.openInventory(inv);

        GuiContext ctx = new GuiContext(GuiTipo.MENU_PROTECCION);
        ctx.proteccion = rec;
        contextos.put(jugador.getUniqueId(), ctx);
    }

    // ---------------------------------------------------------------
    // GUI de Flags
    // ---------------------------------------------------------------

    public void abrirMenuFlags(Player jugador, ProtectionRecord rec) {
        int filas = 4;
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.FLAGS),
                filas * 9, "§e§l⚙ §8Configurar Flags");

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, fondo);

        // Definir flags con iconos
        Object[][] flagDefs = {
                { "block-break", Material.IRON_PICKAXE, "Romper Bloques", "Permite romper bloques." },
                { "block-place", Material.GRASS_BLOCK, "Colocar Bloques", "Permite colocar bloques." },
                { "interact", Material.LEVER, "Interactuar", "Permite usar puertas, cofres, etc." },
                { "pvp", Material.DIAMOND_SWORD, "PVP", "Permite combate entre jugadores." },
                { "explosions", Material.TNT, "Explosiones", "Permite daño por explosiones." },
                { "fire", Material.FLINT_AND_STEEL, "Fuego", "Permite propagación de fuego." },
                { "mob-spawning", Material.SPAWNER, "Spawn de Mobs", "Permite aparición de mobs." },
                { "damage", Material.IRON_SWORD, "Daño a Entidades", "Permite dañar animales/mobs." },
                { "interact-entity", Material.VILLAGER_SPAWN_EGG, "Interactuar Entidades",
                        "Permite interactuar con aldeanos/mobs." },
                { "item-drop", Material.DROPPER, "Soltar Items", "Permite soltar items." },
                { "item-pickup", Material.HOPPER, "Recoger Items", "Permite recoger items." }
        };

        int slot = 10;
        for (Object[] def : flagDefs) {
            String key = (String) def[0];
            Material mat = (Material) def[1];
            String nombre = (String) def[2];
            String desc = (String) def[3];

            boolean valor = rec.getFlag(key, false);

            inv.setItem(slot, crearItemConLore(
                    mat,
                    (valor ? "§a" : "§c") + "§l" + nombre,
                    List.of(
                            "§7" + desc,
                            "",
                            "§7Estado: " + (valor ? "§aPermitido" : "§cDenegado"),
                            "§8▶ §fClick para cambiar")));

            slot++;
            if (slot % 9 == 8)
                slot += 2; // Saltar bordes
            if (slot > 25)
                break;
        }

        // Botón volver
        inv.setItem(31, crearItemConLore(Material.ARROW, "§c§l◄ Volver",
                List.of("§7Regresa al menú principal.")));

        GuiContext ctx = new GuiContext(GuiTipo.FLAGS);
        ctx.proteccion = rec;
        contextos.put(jugador.getUniqueId(), ctx);

        jugador.openInventory(inv);
    }

    // ---------------------------------------------------------------
    // GUI de Miembros
    // ---------------------------------------------------------------

    public void abrirMenuMiembros(Player jugador, ProtectionRecord rec) {
        int filas = 5;
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.MIEMBROS),
                filas * 9, "§a§l♦ §8Gestionar Miembros");

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, fondo);

        // Encabezado
        inv.setItem(4, crearItemConLore(Material.PLAYER_HEAD, "§a§lMiembros",
                List.of("§7Total: §a" + rec.getMembers().size())));

        // Listar miembros
        Map<UUID, ProtectionRecord.MemberRole> miembros = rec.getMembers();
        int slot = 10;

        for (Map.Entry<UUID, ProtectionRecord.MemberRole> entry : miembros.entrySet()) {
            UUID uuid = entry.getKey();
            ProtectionRecord.MemberRole role = entry.getValue();

            String nombre = Bukkit.getOfflinePlayer(uuid).getName();
            if (nombre == null)
                nombre = "§7(Desconocido)";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                meta.setDisplayName("§b" + nombre);
                meta.setLore(List.of(
                        "§7Rol: " + role.getColoredName(),
                        "",
                        role == com.protectium.protection.ProtectionRecord.MemberRole.OWNER ? "§e👑 Propietario"
                                : "§c▶ Click derecho para remover"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
            slot++;
            if (slot % 9 == 8)
                slot += 2; // Saltar bordes
            if (slot > 43)
                break;
        }

        // Botón: Añadir miembro
        inv.setItem(39, crearItemConLore(Material.EMERALD, "§a§l+ Añadir Miembro",
                List.of("§7Click para añadir un nuevo miembro.", "§7Escribe el nombre en el chat.")));

        // Botón volver
        inv.setItem(41, crearItemConLore(Material.ARROW, "§c§l◄ Volver",
                List.of("§7Regresa al menú principal.")));

        GuiContext ctx = new GuiContext(GuiTipo.MIEMBROS);
        ctx.proteccion = rec;
        contextos.put(jugador.getUniqueId(), ctx);

        jugador.openInventory(inv);
    }

    // ---------------------------------------------------------------
    // GUI de Lista Global (Admin)
    // ---------------------------------------------------------------

    public void abrirLista(Player jugador) {
        abrirLista(jugador, 0);
    }

    public void abrirLista(Player jugador, int pagina) {
        List<ProtectionRecord> todas = registry.todas();
        int porPagina = 28;
        int totalPaginas = Math.max(1, (int) Math.ceil((double) todas.size() / porPagina));
        pagina = Math.max(0, Math.min(pagina, totalPaginas - 1));

        int filas = 6;
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.LISTA),
                filas * 9, mensajes.guiTituloLista());

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, fondo);

        // Encabezado
        inv.setItem(4, crearItemConLore(Material.AMETHYST_BLOCK, "§b§l⬡ Protecciones",
                List.of("§7Total: §b" + todas.size(), "§7Página: §b" + (pagina + 1) + "/" + totalPaginas)));

        // Contenido paginado
        int inicio = pagina * porPagina;
        int fin = Math.min(inicio + porPagina, todas.size());
        int slot = 10;

        for (int i = inicio; i < fin; i++) {
            ProtectionRecord rec = todas.get(i);
            inv.setItem(slot, crearItemProteccion(rec));
            slot++;
            if (slot % 9 == 8)
                slot += 2;
        }

        // Navegación
        if (pagina > 0) {
            inv.setItem(45, crearItemConLore(Material.ARROW, "§c§l◄ Anterior", List.of()));
        }
        if (pagina < totalPaginas - 1) {
            inv.setItem(53, crearItemConLore(Material.ARROW, "§a§l► Siguiente", List.of()));
        }
        inv.setItem(49, crearItemConLore(Material.BARRIER, "§c§l✕ Cerrar", List.of()));

        GuiContext ctx = new GuiContext(GuiTipo.LISTA);
        ctx.pagina = pagina;
        contextos.put(jugador.getUniqueId(), ctx);

        jugador.openInventory(inv);
    }

    // ---------------------------------------------------------------
    // GUI de Tipos (Información)
    // ---------------------------------------------------------------

    public void abrirTipos(Player jugador) {
        int filas = 3;
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.TIPOS),
                filas * 9, "§d§l⬡ §8Tipos de Protección");

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, fondo);

        inv.setItem(11, crearItemConLore(Material.AMETHYST_BLOCK, "§b§l⬡ Área",
                List.of("§7Bloquea romper y colocar", "§7bloques en la zona.")));
        inv.setItem(12, crearItemConLore(Material.SPAWNER, "§d§l⬡ Spawn",
                List.of("§7Previene spawn de mobs", "§7hostiles.")));
        inv.setItem(13, crearItemConLore(Material.SHIELD, "§c§l⬡ Entrada",
                List.of("§7Impide entrada de", "§7jugadores sin permiso.")));
        inv.setItem(14, crearItemConLore(Material.FIRE_CORAL_BLOCK, "§6§l⬡ Fuego",
                List.of("§7Bloquea propagación", "§7de fuego y lava.")));
        inv.setItem(15, crearItemConLore(Material.REDSTONE_BLOCK, "§5§l⬡ Redstone",
                List.of("§7Deshabilita el", "§7redstone.")));

        GuiContext ctx = new GuiContext(GuiTipo.TIPOS);
        contextos.put(jugador.getUniqueId(), ctx);

        jugador.openInventory(inv);
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    public GuiContext getContexto(UUID playerId) {
        return contextos.get(playerId);
    }

    public void limpiarContexto(UUID playerId) {
        contextos.remove(playerId);
    }

    private static ItemStack crearItem(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nombre);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack crearItemConLore(Material material, String nombre, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nombre);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack crearItemProteccion(ProtectionRecord rec) {
        Location loc = rec.getUbicacionBloque();
        return crearItemConLore(getMaterialPorTipo(rec.getTipo()), getNombreTipo(rec.getTipo()),
                List.of(
                        "§7 Radio: §b" + rec.getRadio(),
                        "§7 Pos: §b" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(),
                        "§7 Mundo: §b" + loc.getWorld().getName(),
                        "§8 Click para gestionar"));
    }

    private static Material getMaterialPorTipo(ProtectionType tipo) {
        return switch (tipo) {
            case AREA -> Material.AMETHYST_BLOCK;
            case SPAWN -> Material.SPAWNER;
            case ENTRADA -> Material.SHIELD;
            case FUEGO -> Material.FIRE_CORAL_BLOCK;
            case REDSTONE -> Material.REDSTONE_BLOCK;
        };
    }

    private static String getNombreTipo(ProtectionType tipo) {
        return switch (tipo) {
            case AREA -> "§b⬡ Área Protegida";
            case SPAWN -> "§d⬡ Zona Sin Spawn";
            case ENTRADA -> "§c⬡ Zona Restringida";
            case FUEGO -> "§6⬡ Zona Ignífuga";
            case REDSTONE -> "§5⬡ Zona Sin Redstone";
        };
    }
}
