package com.protectium.gui;

/**
 * Identificador de cada tipo de GUI en el plugin.
 * Se usa junto con GuiHolder para detectar clicks.
 */
public enum GuiTipo {
    PRINCIPAL, // Menú principal con estadísticas
    LISTA, // Lista paginada de protecciones
    TIPOS, // Lista de tipos disponibles
    DETALLE, // Detalle de una protección individual
    MENU_PROTECCION, // Menú de gestión de protección específica
    FLAGS, // Configuración de flags
    MIEMBROS, // Gestión de miembros
    CONFIRMAR, // Confirmación de acción
    TIENDA // Menú de compra
}
