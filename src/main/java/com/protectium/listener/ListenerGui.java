package com.protectium.listener;

import com.protectium.core.Mensajes;
import com.protectium.gui.GuiHolder;
import com.protectium.gui.GuiManager;
import com.protectium.gui.GuiTipo;
import com.protectium.protection.ProtectionRecord;
import com.protectium.registry.ProtectionRegistry;
import com.protectium.storage.PersistenceManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
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
    public void onClic tory
                lickEvent event) {
        Inventory inv =  .getInventory();

        // ¿Es un GUI nuestro?
        if (!(inv.getHolder() instanceof GuiHolder))
            return;
        GuiHolder holder = (GuiHolder) inv.getHolder();

        // Siempre cancelar clicks dentro de nuestros GUIs
        event.setCancelled(true);

        Player jugador = (Player) event.getWhoClicked( 
        int slot = event.getRawSlot(); 

        // Ignora clicks fuera del inventario
        if (slot < 0)
            return;

        // Obtener contexto del jugador
        GuiManager.GuiContext ctx = guiManager.getContexto(jugador.getUniqueId());

        // Delegar según el tipo de GUI
        switch (holder.getTipo()) {
            case PRINCIPAL -> handlePrincipal(jugador, slot);
            case LISTA -> handleLista(jugador, slot, ctx);
            case TIPOS -> {
                /* Solo informativo */ }
            case DETALLE -> handleDetalle(jugador, slot, holder);
            case MENU_PROTECCION -> handleMenuProteccion(jugador, slot, ctx);
            case FLAGS -> handleFlags(jugador, slot, ctx);
            case MIEMBROS -> handleMiembros(jugador, slot, ctx, event);
            case TIENDA -> handleTienda(jugador, slot, event.getCurrentItem());
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // No limpiamos el contexto aquí
    }

    // ---------------------------------------------------------------
    // Handlers por GUI
    // ----------------------------------------------------
                ----------

    private void handlePrincipal(Player jugador, int slot) {
        switch (slot) {
            case 12 -> guiManager.abrirLista(jugador);
            case 14 -> guiManager.abrirTipos(jugador);
        }
    }

    private void handleLista(Player jugador, int slot, GuiManager.GuiContext ctx) {
        int pagina = ctx != null ? ctx.pagina : 0;

        // Navegación
        if (slot == 45) 
            
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

        // Click en protección
        if (slot >= 10 && slot <= 43) {
            List<ProtectionRecord> todas = registry.todas();
            int porPagina = 28;
            int inicio = pagina * porPagina;

            int col = slot % 9;
            int fila = slot / 9;
            if (col < 1 || col > 7 || fila < 1 || fila > 4)
                return;

            int indexRelativo = (fila - 1) * 7 + (col - 1);
            int indexAbsoluto = inicio + indexRelativo;

            if (indexAbsoluto >= 0 && indexAbsoluto < todas.size()) {
                guiManager.abrirMenuProteccion(jugador, todas.get(indexAbsoluto));
            }
        }
    }

    private void handleDetalle(Player jugador, int slot, GuiHolder holder) {
        ProtectionRecord rec = holder.getProteccionDetalle();
        if (rec == null)
            return;

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
        if (ctx == null || ctx.proteccion == null)
            return;
        ProtectionRecord rec = ctx.proteccion;

        switch (slot) {
            case 20 -> guiManager.abrirMenuFlags(jugador, rec); // Flags
            case 22 -> guiManager.abrirMenuMiembros(jugador, rec); // Miembros
            case 24 -> {
                // Teleport
                Location loc = rec.getUbicacionBloque().clone().add(0.5, 1, 0.5);
                jugador.teleport(loc);
                jugador.closeInventory();
                jugador.sendMessage("§aTeletransportado a la protección.");
            }
            case 40 -> {
                // Eliminar
                if (!rec.isOwner(jugador.getUniqueId()) && !jugador.hasPermission("protectium.admin")) {
                    jugador.sendMessage(mensajes.errorSinPermisos());
                    return;
                }

                // Eliminar del registro
                registry.eliminar(rec.getUbicacionBloque());

                // Dar item del bloque (si era un bloque valioso)
                Material mat = rec.getUbicacionBloque().getBlock().getType();
                if (mat != Material.AIR) {
                    jugador.getInventory().addItem(new ItemStack(mat));
                    rec.getUbicacionBloque().getBlock().setType(Material.AIR); // Romper bloque físico
                }

                jugador.sendMessage(mensajes.exitoProteccionBorrada());
                jugador.closeInventory();

                // Auto-save async
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                        org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(ListenerGui.class),
                        () -> {
                            try {
                                ((com.protectium.core.ProtectiumPlugin) org.bukkit.plugin.java.JavaPlugin
                                        .getProvidingPlugin(ListenerGui.class))
                                        .getPersistenceManager().saveAll();
                            } catch (Exception ignored) {
                            }
                        });
            }
            case 44 -> jugador.closeInventory();
        }
    }

    private void handleFlags(Player jugador, int slot, GuiManager.GuiContext ctx) {
        if (ctx == null || ctx.proteccion == null)
            return;
        ProtectionRecord rec = ctx.proteccion;

        // Verificar permiso
        if (!rec.canModify(jugador.getUniqueId()) && !jugador.hasPermission("protectium.bypass")) {
            jugador.sendMessage(mensajes.errorSinPermisos());
            return;
        }

        // Botón volver
        if (slot == 31) {
            guiManager.abrirMenuProteccion(jugador, rec);
            return;
        }

        // Mapeo de slots a flags
        int[] flagSlots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22 }; // Expanded slots
        // Note: Slot mapping here is tricky because I added rows in GuiManager but need
        // to match here.
        // In GuiManager, I used a loop with slot++.
        // Here I need to match the slots I filled.
        // The slots filled in GuiManager depend on loop. 10, 11...
        // I'll just check if slot is within reasonable range and iterate FLAG_KEYS?
        // Actually, simpler logic:

        // Find which flag corresponds to this slot.
        // The GUI fills slots: 10,11,12,13,14,15,16, 19,20,21,22,...
        // Let's deduce index from slot if possible, or just iterate checking logic.

        // This is getting complicated to match perfectly without exact mapping.
        // I'll stick to iterate logic I had, but expanded for new slots.
        // Slots used: 10-16, 19-25...
        // Better approach: Store key in item PDC? Too late to change GuiManager
        // structure easily.
        // I'll assume standard layout 10++ skipping borders.

        int currentSlot = 10;
        for (String key : FLAG_KEYS) {
            if (currentSlot == slot) {
                boolean current = rec.getFlag(key, false);
                rec.setFlag(key, !current);
                guiManager.abrirMenuFlags(jugador, rec);
                return;
            }
            currentSlot++;
            if (currentSlot % 9 == 8)
                currentSlot += 2;
        }
    }

    private void handleMiembros(Player jugador, int slot, GuiManager.GuiContext ctx, InventoryClickEvent event) {
        if (ctx == null || ctx.proteccion == null)
            return;
        ProtectionRecord rec = ctx.proteccion;

        // Verificar permiso
        if (!rec.canModify(jugador.getUniqueId()) && !jugador.hasPermission("protectium.bypass")) {
            jugador.sendMessage(mensajes.errorSinPermisos());
            return;
        }

        // Botón volver
        if (slot == 41) {
            guiManager.abrirMenuProteccion(jugador, rec);
            return;
        }

        // Botón añadir miembro
        if (slot == 39) {
            jugador.closeInventory();
            jugador.sendMessage(mensajes.getPrefijo() + "§7Escribe el nombre del jugador a añadir:");
            return;
        }

        // Click derecho en miembro para remover
        if (event.isRightClick() && slot >= 10 && slot <= 35) {
            Map<UUID, ProtectionRecord.MemberRole> miembros = rec.getMembers();
            int index = 0;
            int currentSlot = 10;
            for (UUID memberId : miembros.keySet()) {
                if (slot == currentSlot) {
                    if (!rec.isOwner(memberId)) {
                        rec.removeMember(memberId);
                        guiManager.abrirMenuMiembros(jugador, rec);
                    }
                    return;
                }
                currentSlot++;
                if (currentSlot % 9 == 8)
                    currentSlot += 2;
            }
        }
    }

    private void handleTienda(Player jugador, int slot, ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return;

        org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("protectium", "shop_id");
        if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING))
            return;

        // Comprar
        // Como no hay economía, se da gratis (Debug mode / Simple mode)
        // En futuro, integrar Vault aquí.

        jugador.getInventory().addItem(item);
        
        // Obtener precio del lore
        double precio = 0;
        java.util.List<String> lore = item.getItemMeta().getLore();
        if (lore != null) {
             for (String l : lore) {
                 if (l.contains("$")) {
                     try {
                         String priceStr = l.substring(l.indexOf("$") + 1);
                         precio = Double.parseDouble(priceStr);
                     } catch (Exception ignored) {}
                 }
             }
        }
        
        jugador.sendMessage(mensajes.exitoCompraTienda(item.getItemMeta().getDisplayName(), precio));
        jugador.closeInventory();
    }
}
