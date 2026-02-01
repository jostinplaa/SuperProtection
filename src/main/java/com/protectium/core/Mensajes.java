package com.protectium.core;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Todos los mensajes del plugin centralizados aquí.
 * Se leen desde la config para permitir personalización,
 * pero tienen valores por defecto en caso de que falten.
 */
public final class Mensajes {

    private final String prefijo;
    private final String prefijoError;
    private final String prefijoExito;
    private final String separador;

    public Mensajes(ConfigurationSection config) {
        ConfigurationSection m = config.getConfigurationSection("mensajes");
        if (m != null) {
            this.prefijo = m.getString("prefijo", "§8⛏ §b§lPROTECTIUM§8 »§r ");
            this.prefijoError = m.getString("error-prefijo", "§8⛏ §c§lPROTECTIUM§8 »§r ");
            this.prefijoExito = m.getString("exito-prefijo", "§8⛏ §a§lPROTECTIUM§8 »§r ");
            this.separador = m.getString("separador", "§8═══════════════════════════════");
        } else {
            this.prefijo = "§8⛏ §b§lPROTECTIUM§8 »§r ";
            this.prefijoError = "§8⛏ §c§lPROTECTIUM§8 »§r ";
            this.prefijoExito = "§8⛏ §a§lPROTECTIUM§8 »§r ";
            this.separador = "§8═══════════════════════════════";
        }
    }

    // ---------------------------------------------------------------
    // Mensajes de comandos
    // ---------------------------------------------------------------

    public String ayuda() {
        return separador + "\n"
                + prefijo + "§7Comandos disponibles:\n"
                + "§8  /prot dar <tipo> <radio> <jugador>  §7— Da un ítem de protección\n"
                + "§8  /prot lista                          §7— Ver protecciones activas\n"
                + "§8  /prot info <jugador>                 §7— Info de protecciones de un jugador\n"
                + "§8  /prot recargar                       §7— Recarga la configuración\n"
                + "§8  /prot tipos                          §7— Lista tipos disponibles\n"
                + separador;
    }

    public String tiposDisponibles(String listaTipos) {
        return prefijo + "§7Tipos disponibles: " + listaTipos;
    }

    public String errorTipoInvalido(String tipo) {
        return prefijoError + "§cTipo inválido: §f" + tipo
                + "\n" + prefijoError + "§cUsa §f/prot tipos§c para ver los disponibles.";
    }

    public String errorRadioInvalido(String radio) {
        return prefijoError + "§cRadio inválido: §f" + radio + "§c. Debe ser un número positivo.";
    }

    public String errorRadioExcede(int radio, int maximo) {
        return prefijoError + "§cEl radio §f" + radio + "§c excede el máximo permitido: §f" + maximo;
    }

    public String errorJugadorNoEncontrando(String nombre) {
        return prefijoError + "§cJugador no encontrado: §f" + nombre;
    }

    public String errorSinPermisos() {
        return prefijoError + "§cNo tienes permisos suficientes.";
    }

    public String errorUsaje(String uso) {
        return prefijoError + "§cUso incorrecto: §f" + uso;
    }

    public String errorInventarioLleno(String jugador) {
        return prefijoError + "§cEl inventario de §f" + jugador + "§c está lleno.";
    }

    public String exitoItemDado(String tipo, int radio, String jugador) {
        return prefijoExito + "§a¡Ítem de §f" + tipo + "§a con radio §f" + radio
                + "§a dado a §f" + jugador + "§a!";
    }

    public String exitoProteccionActiva(String tipo, int radio) {
        return prefijoExito + "§a¡Protección §f" + tipo + "§a activada! Radio: §f" + radio + " bloques.";
    }

    public String exitoProteccionEliminada(String tipo) {
        return prefijoExito + "§a Protección §f" + tipo + "§a eliminada al romper el bloque.";
    }

    public String exitoRecargar() {
        return prefijoExito + "§a¡Configuración recargada exitosamente!";
    }

    // ---------------------------------------------------------------
    // Mensajes de eventos
    // ---------------------------------------------------------------

    public String bloqueoPorProteccion(String tipo) {
        return prefijoError + "§cEsta zona está protegida por: §f" + tipo;
    }

    public String entradaDenegada() {
        return "§c§l╔════════════════════════════╗\n"
                + "§c§l║   ⚠  ZONA PROTEGIDA  ⚠    ║\n"
                + "§c§l║                            ║\n"
                + "§c§l║  Acceso completamente      ║\n"
                + "§c§l║       denegado.            ║\n"
                + "§c§l║                            ║\n"
                + "§c§l╚════════════════════════════╝";
    }

    public String spawneoBloqueado() {
        return prefijoError + "§cEl spawning está bloqueado en esta zona.";
    }

    // ---------------------------------------------------------------
    // Mensajes de GUI
    // ---------------------------------------------------------------

    public String guiTituloPrincipal() {
        return "§8⛏ §b§lPROTECTIUM §8— §7Principal";
    }

    public String guiTituloLista() {
        return "§8⛏ §b§lPROTECTIUM §8— §7Protecciones Activas";
    }

    public String guiTituloInfoJugador(String nombre) {
        return "§8⛏ §b§lPROTECTIUM §8— §7Info: " + nombre;
    }

    public String guiSinProtecciones() {
        return "§7No hay protecciones activas.";
    }

    // ---------------------------------------------------------------
    // Mensajes de consistencia / sistema
    // ---------------------------------------------------------------

    public String consistenciaEliminada(String ubicacion) {
        return prefijoError + "§cProtección huérfana eliminada en: §f" + ubicacion;
    }

    public String exitoProteccionBorrada() {
        return prefijoExito + "§aProtección eliminada y bloque recuperado.";
    }

    public String exitoCompraTienda(String item, double precio) {
        return prefijoExito + "§aHas comprado §f" + item + "§a por §e$" + precio;
    }

    public String exitoAddShop(String precio) {
        return prefijoExito + "§aÍtem agregado a la tienda por §e$" + precio;
    }

    public String errorTiendaVacia() {
        return prefijoError + "§cLa tienda no tiene ítems disponibles.";
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public String getPrefijo() {
        return prefijo;
    }

    public String getPrefijoError() {
        return prefijoError;
    }

    public String getPrefijoExito() {
        return prefijoExito;
    }

    public String getSeparador() {
        return separador;
    }
}
