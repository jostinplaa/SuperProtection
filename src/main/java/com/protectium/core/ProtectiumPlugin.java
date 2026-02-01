package com.protectium.core;

import com.protectium.command.ComandoProtectium;
import com.protectium.command.SubCrear;
import com.protectium.command.SubDar;
import com.protectium.command.SubLista;
import com.protectium.command.SubTipos;
import com.protectium.command.SubRecargar;
import com.protectium.command.SubAddShop;
import com.protectium.command.SubTienda;
import com.protectium.fx.FxEngine;
import com.protectium.gui.GuiManager;
import com.protectium.item.ItemAuthority;
import com.protectium.listener.*;
import com.protectium.registry.ProtectionRegistry;
import com.protectium.storage.PersistenceManager;
import com.protectium.task.ConsistencyTask;
import com.protectium.task.FxTickTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProtectiumPlugin extends JavaPlugin {

    private ProtectionRegistry registry;
    private ItemAuthority itemAuthority;
    private FxEngine fxEngine;
    private GuiManager guiManager;
    private Mensajes mensajes;
    private MessageManager messageManager;
    private PersistenceManager persistenceManager;
    private com.protectium.shop.ShopManager shopManager;
    private LimitManager limitManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mostrarBanner();
        inicializarComponentes();
        cargarDatos();
        registrarComandos();
        registrarListeners();
        iniciarTareas();
        getLogger().info("Protectium cargado exitosamente!");
    }

    @Override
    public void onDisable() {
        guardarDatos();
        Bukkit.getScheduler().cancelTasks(this);
        if (registry != null) {
            registry.limpiar();
        }
        getLogger().info("Protectium desactivado.");
    }

    private void inicializarComponentes() {
        this.registry = new ProtectionRegistry();
        this.itemAuthority = new ItemAuthority(this);
        this.mensajes = new Mensajes(getConfig());
        this.messageManager = new MessageManager(this);
        this.fxEngine = new FxEngine(getConfig());
        this.fxEngine.setPlugin(this);
        this.guiManager = new GuiManager(registry, mensajes);
        this.persistenceManager = new PersistenceManager(this, registry);
        this.shopManager = new com.protectium.shop.ShopManager(this);
        this.limitManager = new LimitManager(this);
    }

    public com.protectium.shop.ShopManager getShopManager() {
        return shopManager;
    }

    private void cargarDatos() {
        try {
            int loaded = persistenceManager.loadAll();
            if (loaded > 0) {
                getLogger().info(messageManager.getSystemLoadingSuccess(loaded));
            }
        } catch (Exception e) {
            getLogger().severe("ERROR al cargar protecciones desde disco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void guardarDatos() {
        if (persistenceManager != null) {
            try {
                persistenceManager.saveAll();
                getLogger().info("Protecciones guardadas exitosamente.");
            } catch (Exception e) {
                getLogger().severe("ERROR CRÍTICO al guardar protecciones en shutdown: " + e.getMessage());
                e.printStackTrace();
                // Intentar crear backup de emergencia
                try {
                    persistenceManager.createEmergencyBackup();
                    getLogger().warning("Backup de emergencia creado.");
                } catch (Exception backupError) {
                    getLogger().severe("No se pudo crear backup de emergencia!");
                }
            }
        }
    }

    private void registrarComandos() {
        ComandoProtectium comando = new ComandoProtectium(mensajes);
        comando.registrar(new SubDar(itemAuthority, mensajes, getConfig()));
        comando.registrar(new SubCrear(itemAuthority, mensajes));
        comando.registrar(new SubLista(guiManager, mensajes));
        comando.registrar(new SubTipos(guiManager, mensajes));
        comando.registrar(new SubRecargar(this, mensajes, fxEngine));
        comando.registrar(new SubAddShop(this, mensajes));
        comando.registrar(new SubTienda(this, mensajes));
        getCommand("prot").setExecutor(comando);
        getCommand("prot").setTabCompleter(comando);
    }

    private void registrarListeners() {
        getServer().getPluginManager().registerEvents(
                new ListenerColocar(itemAuthority, registry, fxEngine, mensajes), this);
        getServer().getPluginManager().registerEvents(
                new ListenerRomper(registry, fxEngine, mensajes), this);
        getServer().getPluginManager().registerEvents(
                new ListenerBloques(registry, mensajes, fxEngine, itemAuthority), this);
        getServer().getPluginManager().registerEvents(
                new ListenerSpawn(registry), this);
        getServer().getPluginManager().registerEvents(
                new ListenerEntrada(registry, fxEngine, mensajes), this);
        getServer().getPluginManager().registerEvents(
                new ListenerFuego(registry), this);
        getServer().getPluginManager().registerEvents(
                new ListenerRedstone(registry, mensajes), this);
        getServer().getPluginManager().registerEvents(
                new ListenerFlags(registry, mensajes), this);
        getServer().getPluginManager().registerEvents(
                new ListenerGui(guiManager, registry, mensajes), this);
        getServer().getPluginManager().registerEvents(
                new ListenerInventario(itemAuthority), this);
        getServer().getPluginManager().registerEvents(
                new ListenerInteractProteccion(registry, guiManager, itemAuthority), this);
    }

    private void iniciarTareas() {
        // FxTickTask - Solo UNA tarea asíncrona optimizada
        new FxTickTask(fxEngine, registry).runTaskTimerAsynchronously(this, 1L, 1L);
        
        // Tarea de consistencia cada 60 segundos
        new ConsistencyTask(registry, itemAuthority, this).runTaskTimer(this, 1200L, 1200L);
        
        // BossBar task cada 0.5 segundos
        new com.protectium.task.BossBarTask(registry).runTaskTimer(this, 10L, 10L);
        
        // Auto-guardado cada 5 minutos (6000 ticks) con manejo de errores
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    persistenceManager.saveAll();
                    getLogger().info(messageManager.getSystemAutosaveSuccess(registry.cantidad()));
                } catch (Exception e) {
                    getLogger().severe("ERROR CRÍTICO al auto-guardar protecciones: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 6000L, 6000L);
    }

    private void mostrarBanner() {
        getLogger().info("========================================");
        getLogger().info("  PROTECTIUM v2.0 - Paper 1.20.4+");
        getLogger().info("  Protecciones cubicas con GUI");
        getLogger().info("========================================");
    }

    public ProtectionRegistry getRegistry() {
        return registry;
    }

    public ItemAuthority getItemAuthority() {
        return itemAuthority;
    }

    public FxEngine getFxEngine() {
        return fxEngine;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public Mensajes getMensajes() {
        return mensajes;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public LimitManager getLimitManager() {
        return limitManager;
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }
}
