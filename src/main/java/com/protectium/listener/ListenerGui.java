package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.gui.GuiHolder;
import com.protectium.gui.GuiManager;
import com.protectium.gui.GuiTipo;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maneja todos los clicks dentro de los GUIs del plugin.
 * Identifica el GUI por su GuiHolder y delega la acción correspondiente.
 */
public final class ListenerGui implements Listener {

    private final GuiManager guiManager;
    private final ProtectionRegistry registry;
    private final Mensajes mensajes;

    private static final String[] FLAG_KEYS = {
            "block-break", "block-place", "interact", "pvp", "explosions", "fire", "mob-spawning",
            "damage", "interact-entity", "item-drop", "item-pickup"
    };

    public ListenerGui(GuiManager guiManager, ProtectionRegistry registry, Mensajes mensajes) {
        this.guiManager = guiManager;
        this.registry = registry;
        this.mensajes = mensajes;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();

        if (!(inv.getHolder() instanceof GuiHolder)) return;
        GuiHolder holder = (GuiHolder) inv.getHolder();

        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= inv.getSize()) return;

        GuiManager.GuiContext ctx = guiManager.getContexto(jugador.getUniqueId());

        switch (holder.getTipo()) {
            case PRINCIPAL        -> handlePrincipal(jugador, slot);
            case LISTA            -> handleLista(jugador, slot, ctx);
            case TIPOS            -> { }
            case DETALLE          -> handleDetalle(jugador, slot, holder);
            case MENU_PROTECCION  -> handleMenuProteccion(jugador, slot, ctx);
            case FLAGS            -> handleFlags(jugador, slot, ctx);
            case MIEMBROS         -> handleMiembros(jugador, slot, ctx, event);
            case TIENDA           -> handleTienda(jugador, slot, event.getCurrentItem());
            default               -> { }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) { }

    // ---------------------------------------------------------------

    private void handlePrincipal(Player jugador, int slot) {
        switch (slot) {
            case 12 -> guiManager.abrirLista(jugador);
            case 14 -> guiManager.abrirTipos(jugador);
        }
    }

    private void handleLista(Player jugador, int slot, GuiManager.GuiContext ctx) {
        int pagina = ctx != null ? ctx.pagina : 0;

        if (slot == 45) {
            guiManager.abrirLista(jugador, pagina - 1);
            return;
        }
        if (slot == 53) {
            guiManager.abrirLista(jugador, pagina + 1);
            return;
        }
        if (slot == 49) {
            jugador.closeInventory();
            return;
        }

        if (slot >= 10 && slot <= 43) {
            int col = slot % 9;
            int fila = slot / 9;
            if (col < 1 || col > 7 || fila < 1 || fila > 4) return;

            List<ProtectionRecord> todas = registry.todas();
            int porPagina = 28;
            int inicio = pagina * porPagina;
            int indexRelativo = (fila - 1) * 7 + (col - 1);
            int indexAbsoluto = inicio + indexRelativo;

            if (indexAbsoluto >= 0 && indexAbsoluto < todas.size()) {
                guiManager.abrirMenuProteccion(jugador, todas.get(indexAbsoluto));
            }
        }
    }

    private void handleDetalle(Player jugador, int slot, GuiHolder holder) {
        ProtectionRecord rec = holder.getProteccionDetalle();
        if (rec == null) return;

        switch (slot) {
            case 20 -> {
                Location loc = rec.getUbicacionBloque().clone();
                loc.setY(loc.getY() + rec.getRadio() + 2);
                loc.setPitch(90);
                jugador.teleport(loc);
                jugador.closeInventory();
            }
            case 22 -> guiManager.abrirLista(jugador);
        }
    }

    private void handleMenuProteccion(Player jugador, int slot, GuiManager.GuiContext ctx) {
        if (ctx == null || ctx.proteccion == null) return;
        ProtectionRecord rec = ctx.proteccion;

        switch (slot) {
            case 20 -> guiManager.abrirMenuFlags(jugador, rec);
            case 22 -> guiManager.abrirMenuMiembros(jugador, rec);
            case 24 -> {
                Location loc = rec.getUbicacionBloque().clone().add(0.5, 1, 0.5);
                jugador.teleport(loc);
                jugador.closeInventory();
                jugador.sendMessage(mensajes.getPrefijo() + "§aTeletransportado a la protección.");
            }
            case 40 -> {
                if (!rec.isOwner(jugador.getUniqueId()) && !jugador.hasPermission("protectium.admin")) {
                    jugador.sendMessage(mensajes.errorSinPermisos());
                    return;
                }

                Location blockLoc = rec.getUbicacionBloque();
                Material mat = blockLoc.getBlock().getType();

                registry.eliminar(blockLoc);

                if (mat != Material.AIR) {
                    blockLoc.getBlock().setType(Material.AIR);
                    jugador.getInventory().addItem(new ItemStack(mat));
                }

                jugador.sendMessage(mensajes.exitoProteccionBorrada());
                jugador.closeInventory();

                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                        org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(ListenerGui.class),
                        () -> {
                            try {
                                ((com.protectium.core.ProtectiumPlugin) org.bukkit.plugin.java.JavaPlugin
                                        .getProvidingPlugin(ListenerGui.class))
                                        .getPersistenceManager().saveAll();
                            } catch (Exception ignored) { }
                        });
            }
            case 44 -> jugador.closeInventory();
        }
    }

    private void handleFlags(Player jugador, int slot, GuiManager.GuiContext ctx) {
        if (ctx == null || ctx.proteccion == null) return;
        ProtectionRecord rec = ctx.proteccion;

        if (!rec.canModify(jugador.getUniqueId()) && !jugador.hasPermission("protectium.bypass")) {
            jugador.sendMessage(mensajes.errorSinPermisos());
            return;
        }

        if (slot == 31) {
            guiManager.abrirMenuProteccion(jugador, rec);
            return;
        }

        // Reconstruir mapeo slot→flag exactamente como GuiManager lo coloca
        int currentSlot = 10;
        for (String key : FLAG_KEYS) {
            if (currentSlot == slot) {
                boolean actual = rec.getFlag(key, false);
                rec.setFlag(key, !actual);
                guiManager.abrirMenuFlags(jugador, rec);
                return;
            }
            currentSlot++;
            if (currentSlot % 9 == 8) currentSlot += 2;
        }
    }

    private void handleMiembros(Player jugador, int slot, GuiManager.GuiContext ctx, InventoryClickEvent event) {
        if (ctx == null || ctx.proteccion == null) return;
        ProtectionRecord rec = ctx.proteccion;

        if (!rec.canModify(jugador.getUniqueId()) && !jugador.hasPermission("protectium.bypass")) {
            jugador.sendMessage(mensajes.errorSinPermisos());
            return;
        }

        if (slot == 41) {
            guiManager.abrirMenuProteccion(jugador, rec);
            return;
        }
        if (slot == 39) {
            jugador.closeInventory();
            jugador.sendMessage(mensajes.getPrefijo() + "§7Escribe el nombre del jugador a añadir:");
            return;
        }

        if (event.isRightClick() && slot >= 10 && slot <= 35) {
            Map<UUID, ProtectionRecord.MemberRole> miembros = rec.getMembers();
            int currentSlot = 10;
            for (UUID memberId : miembros.keySet()) {
                if (slot == currentSlot) {
                    if (!rec.isOwner(memberId)) {
                        rec.removeMember(memberId);
                        guiManager.abrirMenuMiembros(jugador, rec);
                    } else {
                        jugador.sendMessage(mensajes.getPrefijoError() + "§cNo se puede remover al dueño.");
                    }
                    return;
                }
                currentSlot++;
                if (currentSlot % 9 == 8) currentSlot += 2;
            }
        }
    }

    private void handleTienda(Player jugador, int slot, ItemStack item) {
        com.protectium.shop.ShopManager shop = ((com.protectium.core.ProtectiumPlugin)
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(ListenerGui.class)).getShopManager();
        int totalPaginas = Math.max(1, (int) Math.ceil(shop.getItems().size() / 21.0));

        // Cerrar
        if (slot == 49) {
            jugador.closeInventory();
            return;
        }

        // Navegación de páginas
        GuiManager.GuiContext ctx = guiManager.getContexto(jugador.getUniqueId());
        int pagina = ctx != null ? ctx.pagina : 0;

        if (slot == 46) {
            guiManager.abrirTiendaPagina(jugador, shop, pagina - 1, totalPaginas);
            return;
        }
        if (slot == 52) {
            guiManager.abrirTiendaPagina(jugador, shop, pagina + 1, totalPaginas);
            return;
        }

        // Click en un ítem de compra
        if (item == null || !item.hasItemMeta()) return;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey("protectium", "shop_id");
        if (!pdc.has(key, PersistentDataType.STRING)) return;

        String shopId = pdc.get(key, PersistentDataType.STRING);

        // Buscar en ShopManager por ID (fuente de verdad, no parsear lore)
        com.protectium.shop.ShopManager.ShopItem shopItem = null;
        for (com.protectium.shop.ShopManager.ShopItem si : shop.getItems()) {
            if (si.getId().equals(shopId)) {
                shopItem = si;
                break;
            }
        }

        if (shopItem == null) {
            jugador.sendMessage(mensajes.getPrefijoError() + "§cÍtem no encontrado en la tienda.");
            return;
        }

        // Dar ítem original limpio (sin lore de precio ni PDC de shop_id)
        jugador.getInventory().addItem(shopItem.getItem());

        String nombre = shopItem.getItem().getItemMeta() != null && shopItem.getItem().getItemMeta().hasDisplayName()
                ? shopItem.getItem().getItemMeta().getDisplayName() : "Ítem";
        jugador.sendMessage(mensajes.exitoCompraTienda(nombre, shopItem.getPrecio()));
        jugador.closeInventory();
    }
}
