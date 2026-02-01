package com.protectium.storage;

import com.protectium.core.ProtectiumPlugin;
import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Sistema de persistencia YAML para protecciones.
 * Guarda las protecciones activas al apagar el servidor y las restaura al
 * encender.
 * El bloque físico debe seguir existiendo para que la protección se restaure.
 */
public final class PersistenceManager {

    private final ProtectiumPlugin plugin;
    private final ProtectionRegistry registry;
    private final Logger logger;
    private final File dataFile;

    public PersistenceManager(ProtectiumPlugin plugin, ProtectionRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.logger = plugin.getLogger();
        this.dataFile = new File(plugin.getDataFolder(), "protections.yml");
    }

    /**
     * Guarda todas las protecciones activas a YAML.
     */
    public void saveAll() {
        FileConfiguration data = new YamlConfiguration();
        List<ProtectionRecord> protecciones = registry.todas();

        data.set("meta.saved-at", System.currentTimeMillis());
        data.set("meta.count", protecciones.size());

        for (ProtectionRecord rec : protecciones) {
            String path = "protections." + rec.getId().toString();

            // Datos básicos
            data.set(path + ".tipo", rec.getTipo().getConfigKey());
            data.set(path + ".colocado-por", rec.getColocadoPor().toString());
            data.set(path + ".marca-tiempo", rec.getMarcaTiempo());
            data.set(path + ".radio", rec.getRadio());

            // Ubicación
            Location loc = rec.getUbicacionBloque();
            data.set(path + ".ubicacion.mundo", loc.getWorld().getName());
            data.set(path + ".ubicacion.x", loc.getBlockX());
            data.set(path + ".ubicacion.y", loc.getBlockY());
            data.set(path + ".ubicacion.z", loc.getBlockZ());

            // Miembros
            Map<UUID, ProtectionRecord.MemberRole> miembros = rec.getMembers();
            for (Map.Entry<UUID, ProtectionRecord.MemberRole> entry : miembros.entrySet()) {
                data.set(path + ".miembros." + entry.getKey().toString(), entry.getValue().name());
            }

            // Flags
            Map<String, Boolean> flags = rec.getFlags();
            for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
                data.set(path + ".flags." + entry.getKey(), entry.getValue());
            }
        }

        try {
            data.save(dataFile);
            logger.info("§a Guardadas " + protecciones.size() + " protecciones.");
        } catch (IOException e) {
            logger.severe("§c Error guardando protecciones: " + e.getMessage());
        }
    }

    /**
     * Carga protecciones desde YAML.
     * Verifica que el bloque físico siga existiendo.
     */
    public int loadAll() {
        if (!dataFile.exists()) {
            logger.info("§7 No hay archivo de protecciones previo.");
            return 0;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("protections");

        if (section == null) {
            return 0;
        }

        int loaded = 0;
        int skipped = 0;

        for (String key : section.getKeys(false)) {
            try {
                String path = "protections." + key;

                // Parsear datos básicos
                UUID id = UUID.fromString(key);
                ProtectionType tipo = ProtectionType.fromString(data.getString(path + ".tipo"));
                UUID colocadoPor = UUID.fromString(data.getString(path + ".colocado-por"));
                long marcaTiempo = data.getLong(path + ".marca-tiempo");
                int radio = data.getInt(path + ".radio");

                if (tipo == null) {
                    logger.warning("§e Tipo inválido para protección " + key);
                    skipped++;
                    continue;
                }

                // Parsear ubicación
                String worldName = data.getString(path + ".ubicacion.mundo");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    logger.warning("§e Mundo '" + worldName + "' no cargado, saltando protección " + key);
                    skipped++;
                    continue;
                }

                int x = data.getInt(path + ".ubicacion.x");
                int y = data.getInt(path + ".ubicacion.y");
                int z = data.getInt(path + ".ubicacion.z");
                Location ubicacion = new Location(world, x, y, z);

                // Verificar que el bloque físico existe (no es aire)
                if (ubicacion.getBlock().getType().isAir()) {
                    logger.info("§7 Bloque removido para protección " + key + ", no se restaura.");
                    skipped++;
                    continue;
                }

                // Parsear miembros
                Map<UUID, ProtectionRecord.MemberRole> miembros = new HashMap<>();
                ConfigurationSection miembrosSection = data.getConfigurationSection(path + ".miembros");
                if (miembrosSection != null) {
                    for (String memberId : miembrosSection.getKeys(false)) {
                        try {
                            UUID memberUUID = UUID.fromString(memberId);
                            ProtectionRecord.MemberRole role = ProtectionRecord.MemberRole.valueOf(
                                    miembrosSection.getString(memberId));
                            miembros.put(memberUUID, role);
                        } catch (Exception ignored) {
                        }
                    }
                }

                // Parsear flags
                Map<String, Boolean> flags = new HashMap<>();
                ConfigurationSection flagsSection = data.getConfigurationSection(path + ".flags");
                if (flagsSection != null) {
                    for (String flagKey : flagsSection.getKeys(false)) {
                        flags.put(flagKey, flagsSection.getBoolean(flagKey));
                    }
                }

                // Crear y registrar protección
                ProtectionRecord record = new ProtectionRecord(
                        id, tipo, ubicacion, radio, colocadoPor, marcaTiempo, miembros, flags);
                registry.registrar(record);
                loaded++;

            } catch (Exception e) {
                logger.warning("§e Error cargando protección " + key + ": " + e.getMessage());
                skipped++;
            }
        }

        logger.info("§a Cargadas " + loaded + " protecciones. §7(" + skipped + " omitidas)");
        return loaded;
    }

    /**
     * Crea un backup de emergencia cuando falla el guardado normal.
     * Usa timestamp en el nombre del archivo.
     */
    public void createEmergencyBackup() throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        File backupFile = new File(plugin.getDataFolder(), "protections_emergency_" + timestamp + ".yml");
        
        FileConfiguration data = new YamlConfiguration();
        List<ProtectionRecord> protecciones = registry.todas();
        
        data.set("meta.emergency-backup", true);
        data.set("meta.timestamp", timestamp);
        data.set("meta.count", protecciones.size());
        
        for (ProtectionRecord rec : protecciones) {
            String path = "protections." + rec.getId().toString();
            
            data.set(path + ".tipo", rec.getTipo().getConfigKey());
            data.set(path + ".colocado-por", rec.getColocadoPor().toString());
            data.set(path + ".marca-tiempo", rec.getMarcaTiempo());
            data.set(path + ".radio", rec.getRadio());
            
            Location loc = rec.getUbicacionBloque();
            data.set(path + ".ubicacion.mundo", loc.getWorld().getName());
            data.set(path + ".ubicacion.x", loc.getBlockX());
            data.set(path + ".ubicacion.y", loc.getBlockY());
            data.set(path + ".ubicacion.z", loc.getBlockZ());
        }
        
        data.save(backupFile);
        logger.warning("Backup de emergencia creado: " + backupFile.getName());
    }
}
