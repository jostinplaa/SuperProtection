package com.protectium.shop;

import com.protectium.core.ProtectiumPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Gestiona la tienda de protecciones.
 * Guarda items prototipo en shop.yml con un precio.
 */
public final class ShopManager {

    private final ProtectiumPlugin plugin;
    private final File shopFile;
    private FileConfiguration shopConfig;
    private final List<ShopItem> items = new ArrayList<>();

    public static class ShopItem {
        private final String id;
        private final ItemStack item;
        private final double precio;

        public ShopItem(String id, ItemStack item, double precio) {
            this.id = id;
            this.item = item;
            this.precio = precio;
        }

        public String getId() {
            return id;
        }

        public ItemStack getItem() {
            return item.clone();
        }

        public double getPrecio() {
            return precio;
        }
    }

    public ShopManager(ProtectiumPlugin plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        cargar();
    }

    public void cargar() {
        if (!shopFile.exists()) {
            try {
                shopFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear shop.yml");
            }
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        items.clear();

        ConfigurationSection section = shopConfig.getConfigurationSection("items");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            ItemStack item = section.getItemStack(key + ".item");
            double precio = section.getDouble(key + ".precio");
            if (item != null) {
                items.add(new ShopItem(key, item, precio));
            }
        }
        plugin.getLogger().info("Cargados " + items.size() + " items de tienda.");
    }

    public void agregarItem(String id, ItemStack item, double precio) {
        // Guardar en config
        shopConfig.set("items." + id + ".item", item);
        shopConfig.set("items." + id + ".precio", precio);
        try {
            shopConfig.save(shopFile);
            // Recargar memoria
            cargar();
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando shop.yml: " + e.getMessage());
        }
    }

    public List<ShopItem> getItems() {
        return new ArrayList<>(items);
    }
}
