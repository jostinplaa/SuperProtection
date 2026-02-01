package com.protectium.core;

import com.protectium.protection.ProtectionRecord;
import com.protectium.protection.ProtectionType;
import com.protectium.registry.ProtectionRegistry;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Gestiona límites de protecciones por jugador basados en permisos.
 * 
 * Sistema de límites:
 * - Límite de protecciones totales por jugador
 * - Límite de radio máximo según rango
 * - Verificación antes de colocar protecciones
 */
public final class LimitManager {
    
    private final Plugin plugin;
    
    public LimitManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * ¿Puede el jugador colocar una nueva protección?
     * Verifica contra su límite basado en permisos.
     */
    public boolean canPlaceProtection(Player player, ProtectionRegistry registry) {
        if (player.hasPermission("protectium.bypass") || player.hasPermission("protectium.unlimited")) {
            return true;
        }
        
        int max = getMaxProtections(player);
        int current = countPlayerProtections(player, registry);
        
        return current < max;
    }
    
    /**
     * ¿Es el radio permitido para este jugador?
     */
    public boolean isRadiusAllowed(Player player, int radius) {
        if (player.hasPermission("protectium.bypass")) {
            return true;
        }
        
        int maxRadius = getMaxRadius(player);
        return radius <= maxRadius;
    }
    
    /**
     * Obtiene el máximo de protecciones que puede tener el jugador.
     * Busca por permisos en orden descendente.
     */
    public int getMaxProtections(Player player) {
        // Orden: mvp+ > mvp > vip+ > vip > default
        if (player.hasPermission("protectium.limit.mvpplus")) return 50;
        if (player.hasPermission("protectium.limit.mvp")) return 30;
        if (player.hasPermission("protectium.limit.vipplus")) return 20;
        if (player.hasPermission("protectium.limit.vip")) return 15;
        
        // Default
        return plugin.getConfig().getInt("limits.default-protections", 5);
    }
    
    /**
     * Obtiene el radio máximo permitido para el jugador.
     */
    public int getMaxRadius(Player player) {
        if (player.hasPermission("protectium.bypass")) {
            return 256; // Sin límite práctico
        }
        
        // Orden: mvp+ > mvp > vip+ > vip > default
        if (player.hasPermission("protectium.limit.mvpplus")) return 128;
        if (player.hasPermission("protectium.limit.mvp")) return 96;
        if (player.hasPermission("protectium.limit.vipplus")) return 64;
        if (player.hasPermission("protectium.limit.vip")) return 48;
        
        // Default
        return plugin.getConfig().getInt("limits.default-radius", 32);
    }
    
    /**
     * Cuenta cuántas protecciones tiene el jugador actualmente.
     */
    public int countPlayerProtections(Player player, ProtectionRegistry registry) {
        return (int) registry.todas().stream()
            .filter(rec -> rec.getColocadoPor().equals(player.getUniqueId()))
            .count();
    }
    
    /**
     * Obtiene información de límites para mostrar al jugador.
     */
    public String getLimitInfo(Player player, ProtectionRegistry registry) {
        int current = countPlayerProtections(player, registry);
        int max = getMaxProtections(player);
        int maxRadius = getMaxRadius(player);
        
        return String.format(
            "§7Protecciones: §b%d§7/§e%d §8| §7Radio máximo: §e%d bloques",
            current, max, maxRadius
        );
    }
}
