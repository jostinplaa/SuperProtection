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
 */
public final class GuiManager {

    private final ProtectionRegistry registry;
    private final Mensajes mensajes;
    private final Map<UUID, GuiContext> contextos = new ConcurrentHashMap<>();

    public GuiManager(ProtectionRegistry registry, Mensajes mensajes) {
        this.registry = registry;
        this.mensajes = mensajes;
    }

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
    // GUI Tienda — Diseño premium
    // ---------------------------------------------------------------
    //
    // Layout (6 filas = 54 slots):
    //
    //  [0] PUR  PUR  PUR  PUR  [TITULO]  PUR  PUR  PUR  PUR
    //  [1] NEG  ——   ——   ——   ——        ——   ——   ——   NEG
    //  [2] NEG  item item item item      item item item NEG
    //  [3] NEG  item item item item      item item item NEG
    //  [4] NEG  item item item item      item item item NEG
    //  [5] NEG  [◄]  NEG  NEG  [X cerrar] NEG  NEG [►]  NEG
    //
    // ---------------------------------------------------------------

    public void abrirTienda(Player jugador, com.protectium.shop.ShopManager shop) {
        List<com.protectium.shop.ShopManager.ShopItem> items = shop.getItems();
        int porPagina = 21; // 3 filas × 7 columnas
        int totalPaginas = Math.max(1, (int) Math.ceil((double) items.size() / porPagina));

        // Obtener página actual del contexto si existe
        GuiContext ctxActual = contextos.get(jugador.getUniqueId());
        int pagina = (ctxActual != null && ctxActual.tipo == GuiTipo.TIENDA) ? ctxActual.pagina : 0;
        pagina = Math.max(0, Math.min(pagina, totalPaginas - 1));

        abrirTiendaPagina(jugador, shop, pagina, totalPaginas);
    }

    public void abrirTiendaPagina(Player jugador, com.protectium.shop.ShopManager shop, int pagina, int totalPaginas) {
        List<com.protectium.shop.ShopManager.ShopItem> items = shop.getItems();

        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.TIENDA), 54,
                "§8⛏ §2§lTIENDA §8» §7Protecciones");

        // ─── Fila 0: borde superior púrpura + título central ───
        ItemStack bordePur = crearItem(Material.PURPLE_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < 9; i++) inv.setItem(i, bordePur);

        // Título central (slot 4)
        inv.setItem(4, crearItemConLore(Material.AMETHYST_BLOCK,
                "§d§l⬡ TIENDA DE PROTECCIONES",
                List.of(
                        "§8├──────────────────────┤",
                        "§7 Compra protecciones",
                        "§7 para proteger tus zonas.",
                        "§8│",
                        "§7 Página: §b" + (pagina + 1) + "§7/§b" + totalPaginas,
                        "§7 Items disponibles: §b" + items.size(),
                        "§8└──────────────────────┘")));

        // ─── Filas 1-4: bordes laterales negros ───
        ItemStack bordeNeg = crearItem(Material.BLACK_STAINED_GLASS_PANE, "§8");
        for (int fila = 1; fila <= 4; fila++) {
            inv.setItem(fila * 9, bordeNeg);      // columna 0
            inv.setItem(fila * 9 + 8, bordeNeg);  // columna 8
        }

        // ─── Fila 5: borde inferior + botones ───
        for (int i = 45; i < 54; i++) inv.setItem(i, bordeNeg);

        // Botón cerrar (centro inferior, slot 49)
        inv.setItem(49, crearItemConLore(Material.BARRIER,
                "§c§l✕ Cerrar",
                List.of("§7Cierra la tienda.")));

        // Botones de navegación
        if (pagina > 0) {
            inv.setItem(46, crearItemConLore(Material.ARROW,
                    "§c§l◄ Anterior",
                    List.of("§7Página anterior")));
        }
        if (pagina < totalPaginas - 1) {
            inv.setItem(52, crearItemConLore(Material.ARROW,
                    "§a§l► Siguiente",
                    List.of("§7Página siguiente")));
        }

        // ─── Items del shop en el grid interior ───
        // Slots disponibles: filas 2-4, columnas 1-7
        int[] slotsDisponibles = {
                19, 20, 21, 22, 23, 24, 25,   // fila 2
                28, 29, 30, 31, 32, 33, 34,   // fila 3
                37, 38, 39, 40, 41, 42, 43    // fila 4
        };

        int inicio = pagina * 21;
        int fin = Math.min(inicio + 21, items.size());

        for (int i = inicio; i < fin; i++) {
            com.protectium.shop.ShopManager.ShopItem shopItem = items.get(i);
            int slotIdx = i - inicio;
            if (slotIdx >= slotsDisponibles.length) break;

            inv.setItem(slotsDisponibles[slotIdx], crearItemTienda(shopItem));
        }

        // Si hay slots vacíos en el grid, rellenar con cristal oscuro para looks
        for (int i = (fin - inicio); i < slotsDisponibles.length; i++) {
            inv.setItem(slotsDisponibles[i], crearItem(Material.DARK_OAK_PLANKS, "§8"));
        }

        // Fila 1 — separador decorativo (cristal gris oscuro)
        ItemStack separa = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int col = 1; col <= 7; col++) inv.setItem(9 + col, separa);

        jugador.openInventory(inv);

        GuiContext ctx = new GuiContext(GuiTipo.TIENDA);
        ctx.pagina = pagina;
        contextos.put(jugador.getUniqueId(), ctx);
    }

    /**
     * Crea el ItemStack de display para un item de la tienda.
     * Diseño de "card": precio destacado en verde, borde visual en lore.
     */
    private static ItemStack crearItemTienda(com.protectium.shop.ShopManager.ShopItem shopItem) {
        ItemStack display = shopItem.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        // Precio formateado
        String precio;
        if (shopItem.getPrecio() == (long) shopItem.getPrecio()) {
            precio = String.valueOf((long) shopItem.getPrecio());
        } else {
            precio = String.format("%.1f", shopItem.getPrecio());
        }

        List<String> lore = new ArrayList<>();
        // Si tenía lore original, separarlo
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
            lore.add("");
        }

        lore.add("§8├──────────────────┤");
        lore.add("§7 Precio: §a§l$" + precio);
        lore.add("§8└──────────────────┘");
        lore.add("");
        lore.add("§e✦ Click para comprar");

        meta.setLore(lore);

        // Marca NBT con el ID del shop para que el ListenerGui lo identifique
        meta.getPersistentDataContainer().set(
                new NamespacedKey("protectium", "shop_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                shopItem.getId());

        display.setItemMeta(meta);
        return display;
    }

    // ---------------------------------------------------------------
    // GUI Menú de una protección específica
    // ---------------------------------------------------------------

    public void abrirMenuProteccion(Player jugador, ProtectionRecord rec) {
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.MENU_PROTECCION),
                45, "§b§l⬡ §8Gestión de Protección");

        // Fondo
        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, fondo);

        // Bordes negros
        ItemStack borde = crearItem(Material.BLACK_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, borde);
            inv.setItem(36 + i, borde);
        }
        for (int fila = 0; fila < 5; fila++) {
            inv.setItem(fila * 9, borde);
            inv.setItem(fila * 9 + 8, borde);
        }

        Location loc = rec.getUbicacionBloque();

        // Info central (slot 4)
        inv.setItem(4, crearItemConLore(
                getMaterialPorTipo(rec.getTipo()),
                getNombreTipo(rec.getTipo()),
                List.of(
                        "§8├─────────────────────────┤",
                        "§7 Radio:     §b" + rec.getRadio() + " bloques",
                        "§7 Ubicación: §b" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                        "§7 Mundo:     §b" + loc.getWorld().getName(),
                        "§7 Miembros:  §b" + rec.getMembers().size(),
                        "§8└─────────────────────────┘")));

        // Botones de acción (fila 2, centrados)
        inv.setItem(20, crearItemConLore(Material.REDSTONE_TORCH,
                "§e§l⚙ Configurar Flags",
                List.of("§7Configura los permisos de la zona.", "", "§8▶ §fClick para abrir")));

        inv.setItem(22, crearItemConLore(Material.PLAYER_HEAD,
                "§3§l👥 Miembros",
                List.of("§7Gestiona quién puede entrar o editar.", "", "§8▶ §fClick para abrir")));

        inv.setItem(24, crearItemConLore(Material.ENDER_PEARL,
                "§d§l⚛ Teletransportarse",
                List.of("§7Viaja al centro de la protección.", "", "§8▶ §fClick para viajar")));

        // Eliminar (abajo izquierda, slot 40)
        inv.setItem(40, crearItemConLore(Material.TNT,
                "§c§l✖ ELIMINAR",
                List.of("§7Borra esta protección permanentemente.",
                        "§c¡No se puede deshacer!", "", "§8▶ §fClick para eliminar")));

        // Cerrar (abajo derecha, slot 44)
        inv.setItem(44, crearItemConLore(Material.BARRIER,
                "§c§l✕ Cerrar",
                List.of("§7Cierra este menú.")));

        jugador.openInventory(inv);

        GuiContext ctx = new GuiContext(GuiTipo.MENU_PROTECCION);
        ctx.proteccion = rec;
        contextos.put(jugador.getUniqueId(), ctx);
    }

    // ---------------------------------------------------------------
    // GUI Flags
    // ---------------------------------------------------------------

    public void abrirMenuFlags(Player jugador, ProtectionRecord rec) {
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.FLAGS),
                36, "§e§l⚙ §8Configurar Flags");

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, fondo);

        Object[][] flagDefs = {
                { "block-break",      Material.IRON_PICKAXE,          "Romper Bloques",      "Permite romper bloques." },
                { "block-place",      Material.GRASS_BLOCK,           "Colocar Bloques",     "Permite colocar bloques." },
                { "interact",         Material.LEVER,                 "Interactuar",         "Permite usar puertas, cofres, etc." },
                { "pvp",              Material.DIAMOND_SWORD,         "PVP",                 "Permite combate entre jugadores." },
                { "explosions",       Material.TNT,                   "Explosiones",         "Permite daño por explosiones." },
                { "fire",             Material.FLINT_AND_STEEL,       "Fuego",               "Permite propagación de fuego." },
                { "mob-spawning",     Material.SPAWNER,               "Spawn de Mobs",       "Permite aparición de mobs." },
                { "damage",           Material.IRON_SWORD,            "Daño Entidades",      "Permite dañar animales/mobs." },
                { "interact-entity", Material.VILLAGER_SPAWN_EGG,    "Interactuar Ent.",    "Permite interactuar con aldeanos." },
                { "item-drop",        Material.DROPPER,               "Soltar Items",        "Permite soltar items." },
                { "item-pickup",      Material.HOPPER,                "Recoger Items",       "Permite recoger items." }
        };

        int slot = 10;
        for (Object[] def : flagDefs) {
            String key    = (String) def[0];
            Material mat  = (Material) def[1];
            String nombre = (String) def[2];
            String desc   = (String) def[3];

            boolean valor = rec.getFlag(key, false);

            // Color según estado: verde si permitido, rojo si denegado
            Material iconMat = valor ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

            inv.setItem(slot, crearItemConLore(mat,
                    (valor ? "§a" : "§c") + "§l" + nombre,
                    List.of(
                            "§7" + desc,
                            "",
                            "§7Estado: " + (valor ? "§a✔ Permitido" : "§c✖ Denegado"),
                            "",
                            "§8▶ §fClick para cambiar")));

            slot++;
            if (slot % 9 == 8) slot += 2; // saltar bordes
            if (slot > 25) break;
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
    // GUI Miembros
    // ---------------------------------------------------------------

    public void abrirMenuMiembros(Player jugador, ProtectionRecord rec) {
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.MIEMBROS),
                45, "§a§l♦ §8Gestionar Miembros");

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, fondo);

        inv.setItem(4, crearItemConLore(Material.PLAYER_HEAD, "§a§lMiembros",
                List.of("§7Total: §a" + rec.getMembers().size())));

        Map<UUID, ProtectionRecord.MemberRole> miembros = rec.getMembers();
        int slot = 10;

        for (Map.Entry<UUID, ProtectionRecord.MemberRole> entry : miembros.entrySet()) {
            UUID uuid = entry.getKey();
            ProtectionRecord.MemberRole role = entry.getValue();

            String nombre = Bukkit.getOfflinePlayer(uuid).getName();
            if (nombre == null) nombre = "(Desconocido)";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                meta.setDisplayName("§b" + nombre);
                meta.setLore(List.of(
                        "§7Rol: " + role.getColoredName(),
                        "",
                        role == ProtectionRecord.MemberRole.OWNER
                                ? "§e👑 Propietario — no se puede remover"
                                : "§c▶ Click derecho para remover"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot > 43) break;
        }

        inv.setItem(39, crearItemConLore(Material.EMERALD, "§a§l+ Añadir Miembro",
                List.of("§7Click para añadir un nuevo miembro.", "§7Escribe el nombre en el chat.")));

        inv.setItem(41, crearItemConLore(Material.ARROW, "§c§l◄ Volver",
                List.of("§7Regresa al menú principal.")));

        GuiContext ctx = new GuiContext(GuiTipo.MIEMBROS);
        ctx.proteccion = rec;
        contextos.put(jugador.getUniqueId(), ctx);

        jugador.openInventory(inv);
    }

    // ---------------------------------------------------------------
    // GUI Lista Global
    // ---------------------------------------------------------------

    public void abrirLista(Player jugador) {
        abrirLista(jugador, 0);
    }

    public void abrirLista(Player jugador, int pagina) {
        List<ProtectionRecord> todas = registry.todas();
        int porPagina = 28;
        int totalPaginas = Math.max(1, (int) Math.ceil((double) todas.size() / porPagina));
        pagina = Math.max(0, Math.min(pagina, totalPaginas - 1));

        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.LISTA), 54,
                mensajes.guiTituloLista());

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, fondo);

        inv.setItem(4, crearItemConLore(Material.AMETHYST_BLOCK, "§b§l⬡ Protecciones",
                List.of("§7Total: §b" + todas.size(), "§7Página: §b" + (pagina + 1) + "/§b" + totalPaginas)));

        int inicio = pagina * porPagina;
        int fin = Math.min(inicio + porPagina, todas.size());
        int slot = 10;

        for (int i = inicio; i < fin; i++) {
            inv.setItem(slot, crearItemProteccion(todas.get(i)));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (pagina > 0)
            inv.setItem(45, crearItemConLore(Material.ARROW, "§c§l◄ Anterior", List.of()));
        if (pagina < totalPaginas - 1)
            inv.setItem(53, crearItemConLore(Material.ARROW, "§a§l► Siguiente", List.of()));
        inv.setItem(49, crearItemConLore(Material.BARRIER, "§c§l✕ Cerrar", List.of()));

        GuiContext ctx = new GuiContext(GuiTipo.LISTA);
        ctx.pagina = pagina;
        contextos.put(jugador.getUniqueId(), ctx);

        jugador.openInventory(inv);
    }

    // ---------------------------------------------------------------
    // GUI Tipos
    // ---------------------------------------------------------------

    public void abrirTipos(Player jugador) {
        Inventory inv = Bukkit.createInventory(new GuiHolder(GuiTipo.TIPOS), 27,
                "§d§l⬡ §8Tipos de Protección");

        ItemStack fondo = crearItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, fondo);

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
                        "§7 Pos:   §b" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                        "§7 Mundo: §b" + loc.getWorld().getName(),
                        "",
                        "§8▶ §fClick para gestionar"));
    }

    static Material getMaterialPorTipo(ProtectionType tipo) {
        return switch (tipo) {
            case AREA      -> Material.AMETHYST_BLOCK;
            case SPAWN     -> Material.SPAWNER;
            case ENTRADA   -> Material.SHIELD;
            case FUEGO     -> Material.FIRE_CORAL_BLOCK;
            case REDSTONE  -> Material.REDSTONE_BLOCK;
        };
    }

    private static String getNombreTipo(ProtectionType tipo) {
        return switch (tipo) {
            case AREA      -> "§b⬡ Área Protegida";
            case SPAWN     -> "§d⬡ Zona Sin Spawn";
            case ENTRADA   -> "§c⬡ Zona Restringida";
            case FUEGO     -> "§6⬡ Zona Ignífuga";
            case REDSTONE  -> "§5⬡ Zona Sin Redstone";
        };
    }
}
