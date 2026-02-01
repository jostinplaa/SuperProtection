package com.protectium.core;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Sistema de mensajes multi-idioma.
 * Carga desde lang/en.yml o lang/es.yml según configuración.
 */
public final class Mensajes {

    private final FileConfiguration lang;
    private final String prefix;
    private final String prefixError;
    private final String prefixSuccess;
    private final String separator;

    public Mensajes(JavaPlugin plugin, String language) {
        // Save default language files
        saveDefaultLang(plugin, "en");
        saveDefaultLang(plugin, "es");

        // Load the configured language
        File langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + language + ".yml, falling back to English");
            langFile = new File(plugin.getDataFolder(), "lang/en.yml");
        }

        this.lang = YamlConfiguration.loadConfiguration(langFile);

        // Also load defaults from jar as fallback
        InputStream defStream = plugin.getResource("lang/en.yml");
        if (defStream != null) {
            YamlConfiguration defLang = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            lang.setDefaults(defLang);
        }

        // Load prefixes
        this.prefix = color(lang.getString("prefix", "&8⛏ &b&lPROTECTIUM&8 »&r "));
        this.prefixError = color(lang.getString("prefix-error", "&8⛏ &c&lPROTECTIUM&8 »&r "));
        this.prefixSuccess = color(lang.getString("prefix-success", "&8⛏ &a&lPROTECTIUM&8 »&r "));
        this.separator = color(lang.getString("separator", "&8═══════════════════════════════"));

        plugin.getLogger().info("Loaded language: " + language);
    }

    private void saveDefaultLang(JavaPlugin plugin, String lang) {
        File file = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
    }

    private String color(String text) {
        if (text == null)
            return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String get(String path) {
        return color(lang.getString(path, "Missing: " + path));
    }

    private String get(String path, String def) {
        return color(lang.getString(path, def));
    }

    // ---------------------------------------------------------------
    // Command Messages
    // ---------------------------------------------------------------

    public String ayuda() {
        StringBuilder sb = new StringBuilder();
        sb.append(get("help.header")).append("\n");
        sb.append(get("help.title")).append("\n");
        sb.append(get("help.separator")).append("\n");

        List<String> commands = lang.getStringList("help.commands");
        for (String cmd : commands) {
            sb.append(color(cmd)).append("\n");
        }

        sb.append(get("help.footer"));
        return sb.toString();
    }

    public String tiposDisponibles(String listaTipos) {
        return prefix + get("types-available").replace("{types}", listaTipos);
    }

    public String errorTipoInvalido(String tipo) {
        return prefixError + get("error.invalid-type").replace("{type}", tipo);
    }

    public String errorRadioInvalido(String radio) {
        return prefixError + get("error.invalid-radius").replace("{radius}", radio);
    }

    public String errorRadioExcede(int radio, int maximo) {
        return prefixError + get("error.radius-exceeds")
                .replace("{radius}", String.valueOf(radio))
                .replace("{max}", String.valueOf(maximo));
    }

    public String errorJugadorNoEncontrando(String nombre) {
        return prefixError + get("error.player-not-found").replace("{player}", nombre);
    }

    public String errorSinPermisos() {
        return prefixError + get("error.no-permission");
    }

    public String errorUsaje(String uso) {
        return prefixError + get("error.usage").replace("{usage}", uso);
    }

    public String errorInventarioLleno(String jugador) {
        return prefixError + get("error.inventory-full").replace("{player}", jugador);
    }

    public String errorTiendaVacia() {
        return prefixError + get("error.shop-empty");
    }

    public String errorFondosInsuficientes(double needed) {
        return prefixError + get("error.insufficient-funds")
                .replace("{needed}", String.format("%.2f", needed));
    }

    public String errorVaultDisabled() {
        return prefixError + get("error.vault-disabled");
    }

    public String exitoItemDado(String tipo, int radio, String jugador) {
        return prefixSuccess + get("success.item-given")
                .replace("{type}", tipo)
                .replace("{radius}", String.valueOf(radio))
                .replace("{player}", jugador);
    }

    public String exitoProteccionActiva(String tipo, int radio) {
        return prefixSuccess + get("success.protection-active")
                .replace("{type}", tipo)
                .replace("{radius}", String.valueOf(radio));
    }

    public String exitoProteccionEliminada(String tipo) {
        return prefixSuccess + get("success.protection-removed").replace("{type}", tipo);
    }

    public String exitoRecargar() {
        return prefixSuccess + get("success.reload");
    }

    public String exitoProteccionBorrada() {
        return prefixSuccess + get("success.protection-deleted");
    }

    public String exitoCompraTienda(String item, double precio) {
        return prefixSuccess + get("success.shop-purchase")
                .replace("{item}", item)
                .replace("{price}", String.format("%.2f", precio))
                .replace("{balance}", "N/A");
    }

    public String exitoCompraTienda(String item, double precio, String balance) {
        return prefixSuccess + get("success.shop-purchase")
                .replace("{item}", item)
                .replace("{price}", String.format("%.2f", precio))
                .replace("{balance}", balance);
    }

    public String exitoAddShop(String precio) {
        return prefixSuccess + get("success.shop-add").replace("{price}", precio);
    }

    // ---------------------------------------------------------------
    // Event Messages
    // ---------------------------------------------------------------

    public String bloqueoPorProteccion(String tipo) {
        return prefixError + get("protection.blocked").replace("{type}", tipo);
    }

    public String entradaDenegada() {
        return get("protection.entry-denied");
    }

    public String spawneoBloqueado() {
        return prefixError + get("protection.spawn-blocked");
    }

    // ---------------------------------------------------------------
    // GUI Messages
    // ---------------------------------------------------------------

    public String guiTituloPrincipal() {
        return get("gui.main-title");
    }

    public String guiTituloLista() {
        return get("gui.list-title");
    }

    public String guiTituloInfoJugador(String nombre) {
        return get("gui.player-info-title").replace("{player}", nombre);
    }

    public String guiTituloTienda() {
        return get("gui.shop-title");
    }

    public String guiTituloFlags() {
        return get("gui.flags-title");
    }

    public String guiTituloMiembros() {
        return get("gui.members-title");
    }

    public String guiSinProtecciones() {
        return get("gui.no-protections");
    }

    // ---------------------------------------------------------------
    // Item Received Messages
    // ---------------------------------------------------------------

    public String itemRecibidoLinea1() {
        return prefix + get("item-received.line1");
    }

    public String itemRecibidoLinea2(String tipo, int radio) {
        return prefix + get("item-received.line2")
                .replace("{type}", tipo)
                .replace("{radius}", String.valueOf(radio));
    }

    public String itemRecibidoLinea3() {
        return prefix + get("item-received.line3");
    }

    // ---------------------------------------------------------------
    // Lore Messages
    // ---------------------------------------------------------------

    public String loreRadio(int radio) {
        return get("lore.radius").replace("{radius}", String.valueOf(radio));
    }

    public String lorePlaceToActivate() {
        return get("lore.place-to-activate");
    }

    public String loreBreakToRemove() {
        return get("lore.break-to-remove");
    }

    public String loreBlocks() {
        return get("lore.blocks");
    }

    public String loreSpawning() {
        return get("lore.spawning");
    }

    public String loreEntry() {
        return get("lore.entry");
    }

    public String loreFire() {
        return get("lore.fire");
    }

    public String loreRedstone() {
        return get("lore.redstone");
    }

    // ---------------------------------------------------------------
    // System Messages
    // ---------------------------------------------------------------

    public String consistenciaEliminada(String ubicacion) {
        return prefixError + "Orphan protection removed at: " + ubicacion;
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public String getPrefijo() {
        return prefix;
    }

    public String getPrefijoError() {
        return prefixError;
    }

    public String getPrefijoExito() {
        return prefixSuccess;
    }

    public String getSeparador() {
        return separator;
    }
}
