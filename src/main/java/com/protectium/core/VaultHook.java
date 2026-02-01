package com.protectium.core;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Hook de integración con Vault para el sistema de economía.
 * Permite verificar saldo y descontar dinero al comprar en la tienda.
 */
public final class VaultHook {

    private static Economy economy = null;
    private static boolean enabled = false;

    /**
     * Intenta conectar con Vault al iniciar el plugin.
     * 
     * @return true si Vault está disponible y configurado
     */
    public static boolean setup() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        enabled = (economy != null);
        return enabled;
    }

    /**
     * ¿Está Vault habilitado y funcionando?
     */
    public static boolean isEnabled() {
        return enabled && economy != null;
    }

    /**
     * Obtiene el saldo del jugador.
     */
    public static double getBalance(Player player) {
        if (!isEnabled())
            return 0;
        return economy.getBalance(player);
    }

    /**
     * ¿Tiene el jugador suficiente dinero?
     */
    public static boolean hasEnough(Player player, double amount) {
        if (!isEnabled())
            return true; // Si no hay Vault, permitir gratis
        return economy.has(player, amount);
    }

    /**
     * Descuenta dinero del jugador.
     * 
     * @return true si la transacción fue exitosa
     */
    public static boolean withdraw(Player player, double amount) {
        if (!isEnabled())
            return true; // Sin Vault = gratis
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Formatea un monto de dinero según la configuración de Vault.
     */
    public static String format(double amount) {
        if (!isEnabled())
            return "$" + amount;
        return economy.format(amount);
    }
}
