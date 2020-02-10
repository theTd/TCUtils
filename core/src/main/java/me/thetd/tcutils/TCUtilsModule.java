package me.thetd.tcutils;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.totemcraftmc.bukkitplugin.TCBaseLib.simplemodule.Module;

public abstract class TCUtilsModule implements Module<TCUtilsPlugin> {
    private final String name;
    private TCUtilsPlugin plugin;
    boolean loaded = false;

    public TCUtilsModule(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public TCUtilsPlugin getPlugin() {
        return plugin;
    }

    @Override
    public final void load(TCUtilsPlugin clayBasicsPlugin) throws Exception {
        this.plugin = clayBasicsPlugin;
        load();
    }

    protected abstract void load() throws Exception;

    protected void initConfig(ConfigurationSection config) {
    }

    public boolean isLoaded() {
        return loaded;
    }

    protected ConfigurationSection getConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(name);
        if (section == null) section = plugin.getConfig().createSection(name);
        ConfigurationSection confSection = section.getConfigurationSection("config");
        if (confSection == null) confSection = section.createSection("config");
        return confSection;
    }
}
