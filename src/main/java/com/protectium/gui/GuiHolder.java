package com.protectium.gui;

import com.protectium.protection.ProtectionRecord;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder custom que identifica cada inventario GUI.
 * Porta el tipo de GUI y cualquier dato contextual necesario
 * (como la página actual o el record en detalle).
 */
public class GuiHolder implements InventoryHolder {

    private final GuiTipo tipo;
    private int pagina = 0;
    private ProtectionRecord proteccionDetalle = null;

    public GuiHolder(GuiTipo tipo) {
        this.tipo = tipo;
    }

    public GuiHolder(GuiTipo tipo, int pagina) {
        this.tipo = tipo;
        this.pagina = pagina;
    }

    public GuiTipo getTipo() { return tipo; }
    public int getPagina()   { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public ProtectionRecord getProteccionDetalle() { return proteccionDetalle; }
    public void setProteccionDetalle(ProtectionRecord rec) { this.proteccionDetalle = rec; }

    @Override
    public Inventory getInventory() {
        return null; // No necesitamos retornar un inventario desde aquí
    }
}
