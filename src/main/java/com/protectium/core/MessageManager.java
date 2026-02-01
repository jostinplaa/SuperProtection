package com.protectium.core;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sistema mejorado de mensajes que carga todo desde messages.yml
 * Soporta:
 * - Placeholders dinámicos {variable}
 * - Códigos de color & y §
 * - Mensajes multilínea
 * - Cache de mensajes parseados
 * - Recarga en caliente
 */
public final class MessageManager {

    private final Plugin plugin;
    private FileConfiguration messages;
    private File messagesFile;
    
    // Cache de mensajes parseados
    private final Map<String, String> cache = new HashMap<>();
    
    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    /**
     * Carga o recarga el archivo messages.yml
     */
    public void loadMessages() {
        cache.clear();
        
        // Crear archivo si no existe
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Cargar configuración
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Cargar defaults desde el JAR
        try (InputStream defStream = plugin.getResource("messages.yml")) {
            if (defStream != null) {
                messages.setDefaults(
                    YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8)
                    )
                );
            }
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo cargar messages.yml por defecto: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene un mensaje del archivo con placeholders reemplazados
     */
    public String get(String path, Map<String, String> placeholders) {
        String msg = getRaw(path);
        if (msg == null) return "§c[Mensaje no encontrado: " + path + "]";
        
        // Reemplazar placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return msg;
    }
    
    /**
     * Obtiene un mensaje sin placeholders
     */
    public String get(String path) {
        return get(path, null);
    }
    
    /**
     * Obtiene mensaje raw desde cache o config
     */
    private String getRaw(String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }
        
        String msg;
        
        // Intentar obtener como lista (para mensajes multilínea)
        if (messages.isList(path)) {
            List<String> lines = messages.getStringList(path);
            msg = String.join("\n", lines);
        } else {
            msg = messages.getString(path);
        }
        
        if (msg != null) {
            msg = colorize(msg);
            cache.put(path, msg);
        }
        
        return msg;
    }
    
    /**
     * Convierte códigos de color & y § a colores
     */
    private String colorize(String text) {
        if (text == null) return null;
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS DE ACCESO RÁPIDO PARA MENSAJES COMUNES
    // ═══════════════════════════════════════════════════════════════════════
    
    public String getPrefix(String type) {
        return get("prefixes." + type);
    }
    
    public String getSeparator(String type) {
        return get("separators." + type);
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // COMANDOS
    // ───────────────────────────────────────────────────────────────────────
    
    public String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(get("commands.help.header")).append("\n");
        
        List<String> cmds = messages.getStringList("commands.help.commands");
        for (String cmd : cmds) {
            sb.append(colorize(cmd)).append("\n");
        }
        
        sb.append(get("commands.help.footer"));
        return sb.toString();
    }
    
    public String getGiveSuccess(String tipo, int radio, String player) {
        Map<String, String> vars = new HashMap<>();
        vars.put("tipo", tipo);
        vars.put("radio", String.valueOf(radio));
        vars.put("player", player);
        return get("commands.give.success", vars);
    }
    
    public String getGiveUsage() {
        return get("commands.give.usage");
    }
    
    public String getInventoryFull(String player) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player);
        return get("commands.give.inventory-full", vars);
    }
    
    public String getCreateSuccess(String name, String tipo, int radio, String material) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("tipo", tipo);
        vars.put("radio", String.valueOf(radio));
        vars.put("material", material);
        return get("commands.create.success", vars);
    }
    
    public String getReloadSuccess(int protections) {
        Map<String, String> vars = new HashMap<>();
        vars.put("protections", String.valueOf(protections));
        return get("commands.reload.success", vars);
    }
    
    public String getTypes() {
        StringBuilder sb = new StringBuilder();
        sb.append(get("commands.types.header")).append("\n");
        sb.append(get("commands.types.area")).append("\n");
        sb.append(get("commands.types.spawn")).append("\n");
        sb.append(get("commands.types.entrada")).append("\n");
        sb.append(get("commands.types.fuego")).append("\n");
        sb.append(get("commands.types.redstone")).append("\n");
        sb.append(get("commands.types.footer"));
        return sb.toString();
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // ERRORES
    // ───────────────────────────────────────────────────────────────────────
    
    public String getErrorInvalidType(String tipo) {
        Map<String, String> vars = new HashMap<>();
        vars.put("tipo", tipo);
        return get("errors.invalid-type", vars);
    }
    
    public String getErrorInvalidRadius(String radius) {
        Map<String, String> vars = new HashMap<>();
        vars.put("radius", radius);
        return get("errors.invalid-radius", vars);
    }
    
    public String getErrorRadiusExceeds(int radius, int max) {
        Map<String, String> vars = new HashMap<>();
        vars.put("radius", String.valueOf(radius));
        vars.put("max", String.valueOf(max));
        return get("errors.radius-exceeds-max", vars);
    }
    
    public String getErrorPlayerNotFound(String player) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player);
        return get("errors.player-not-found", vars);
    }
    
    public String getErrorNoPermission() {
        return get("errors.no-permission");
    }
    
    public String getErrorLimitReached(int limit) {
        Map<String, String> vars = new HashMap<>();
        vars.put("limit", String.valueOf(limit));
        return get("errors.limit-reached", vars);
    }
    
    public String getErrorDatabaseError() {
        return get("errors.database-error");
    }
    
    public String getErrorInternalError(String error) {
        Map<String, String> vars = new HashMap<>();
        vars.put("error", error);
        return get("errors.internal-error", vars);
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // PROTECCIONES
    // ───────────────────────────────────────────────────────────────────────
    
    public String getProtectionActivated(String tipo, int radio, int x, int y, int z, String world) {
        Map<String, String> vars = new HashMap<>();
        vars.put("tipo", tipo);
        vars.put("radio", String.valueOf(radio));
        vars.put("x", String.valueOf(x));
        vars.put("y", String.valueOf(y));
        vars.put("z", String.valueOf(z));
        vars.put("world", world);
        return get("protection.activated.chat", vars);
    }
    
    public String getProtectionRemoved(String tipo) {
        Map<String, String> vars = new HashMap<>();
        vars.put("tipo", tipo);
        return get("protection.removed.chat", vars);
    }
    
    public String getBlockedBreak(String tipo) {
        Map<String, String> vars = new HashMap<>();
        vars.put("tipo", tipo);
        return get("protection.blocked.break", vars);
    }
    
    public String getBlockedPlace(String tipo) {
        Map<String, String> vars = new HashMap<>();
        vars.put("tipo", tipo);
        return get("protection.blocked.place", vars);
    }
    
    public String getBlockedEntry() {
        return get("protection.blocked.entry");
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // GUI
    // ───────────────────────────────────────────────────────────────────────
    
    public String getGuiTitle(String type) {
        return get("gui.titles." + type);
    }
    
    public String getGuiTitleWithPlayer(String type, String player) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player);
        return get("gui.titles." + type, vars);
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // MIEMBROS
    // ───────────────────────────────────────────────────────────────────────
    
    public String getMemberAdded(String player, String role, String protectionId) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player);
        vars.put("role_colored", get("members.roles." + role.toLowerCase()));
        vars.put("protection_id", protectionId);
        return get("members.added", vars);
    }
    
    public String getMemberRemoved(String player, String protectionId) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player);
        vars.put("protection_id", protectionId);
        return get("members.removed", vars);
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // FLAGS
    // ───────────────────────────────────────────────────────────────────────
    
    public String getFlagEnabled(String flag) {
        Map<String, String> vars = new HashMap<>();
        vars.put("flag", getFlagName(flag));
        return get("flags.enabled", vars);
    }
    
    public String getFlagDisabled(String flag) {
        Map<String, String> vars = new HashMap<>();
        vars.put("flag", getFlagName(flag));
        return get("flags.disabled", vars);
    }
    
    public String getFlagName(String flag) {
        String name = messages.getString("flags.names." + flag);
        return name != null ? colorize(name) : flag;
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // TIENDA
    // ───────────────────────────────────────────────────────────────────────
    
    public String getShopPurchaseSuccess(String item, double price, double balance) {
        Map<String, String> vars = new HashMap<>();
        vars.put("item", item);
        vars.put("price", String.format("%.2f", price));
        vars.put("balance", String.format("%.2f", balance));
        return get("shop.purchase-success", vars);
    }
    
    public String getShopInsufficientFunds(double price, double balance) {
        Map<String, String> vars = new HashMap<>();
        vars.put("price", String.format("%.2f", price));
        vars.put("balance", String.format("%.2f", balance));
        vars.put("needed", String.format("%.2f", price - balance));
        return get("shop.insufficient-funds", vars);
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // SISTEMA
    // ───────────────────────────────────────────────────────────────────────
    
    public String getSystemStartupBanner(String version, String paperVersion) {
        Map<String, String> vars = new HashMap<>();
        vars.put("version", version);
        vars.put("paper_version", paperVersion);
        return get("system.startup.banner", vars);
    }
    
    public String getSystemAutosaveSuccess(int count) {
        Map<String, String> vars = new HashMap<>();
        vars.put("count", String.valueOf(count));
        return get("system.autosave.success", vars);
    }
    
    public String getSystemLoadingSuccess(int count) {
        Map<String, String> vars = new HashMap<>();
        vars.put("count", String.valueOf(count));
        return get("system.loading.success", vars);
    }
    
    // ───────────────────────────────────────────────────────────────────────
    // LORE DE ÍTEMS
    // ───────────────────────────────────────────────────────────────────────
    
    public List<String> getItemLore(String type, int radio) {
        List<String> lore = messages.getStringList("items." + type + ".lore");
        
        // Reemplazar placeholders en cada línea
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            line = line.replace("{radio}", String.valueOf(radio));
            line = colorize(line);
            lore.set(i, line);
        }
        
        return lore;
    }
    
    public String getItemName(String type) {
        return get("items." + type + ".name");
    }
    
    public List<String> getCustomItemLore(String customName, int radio) {
        List<String> lore = messages.getStringList("items.custom.lore");
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            line = line.replace("{custom_name}", customName);
            line = line.replace("{radio}", String.valueOf(radio));
            line = colorize(line);
            lore.set(i, line);
        }
        
        return lore;
    }
}
