package me.thetd.tcutils;

import org.bukkit.plugin.java.JavaPlugin;

public class TCUtilsPlugin extends JavaPlugin {
    private final TCUtilsModuleManager moduleManager = new TCUtilsModuleManager(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            ((ModuleInitializer) Class.forName("me.thetd.TCUtils.Initializer").newInstance())
                    .initializeModules(moduleManager);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        moduleManager.loadAll();
    }

    @Override
    public void onDisable() {
        moduleManager.unloadAll();
    }
}
