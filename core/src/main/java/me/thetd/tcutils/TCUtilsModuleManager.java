package me.thetd.tcutils;


import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.totemcraftmc.bukkitplugin.TCBaseLib.simplemodule.Module;
import org.totemcraftmc.bukkitplugin.TCBaseLib.simplemodule.ModuleManager;
import org.totemcraftmc.bukkitplugin.TCBaseLib.util.FormatUtil;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TCUtilsModuleManager extends ModuleManager<TCUtilsPlugin> {
    private final List<ModuleRegistration> registration = new ArrayList<>();
    private final Set<Class> defaultLoadModules = new HashSet<>();

    TCUtilsModuleManager(TCUtilsPlugin plugin) {
        super(plugin);
    }

    public void loadAll() {
        Server server = Bukkit.getServer();
        for (ModuleRegistration moduleRegistration : registration) {
            List<String> missingDependencies = moduleRegistration.dependencies.stream()
                    .filter(pluginName -> !server.getPluginManager().isPluginEnabled(pluginName))
                    .collect(Collectors.toList());

            if (!missingDependencies.isEmpty()) {
                plugin.getLogger().severe(String.format("module %s requires plugin %s", moduleRegistration.moduleClass.getSimpleName(), FormatUtil.flat(missingDependencies)));
                continue;
            }

            try {
                Constructor<? extends TCUtilsModule> constructor = moduleRegistration.moduleClass.getConstructor();
                add(constructor.newInstance());
                if (moduleRegistration.defaultLoad) {
                    defaultLoadModules.add(moduleRegistration.moduleClass);
                }
            } catch (NoSuchMethodException e) {
                plugin.getLogger().severe("empty constructor not found of module " + moduleRegistration.moduleClass.getSimpleName());
            } catch (Throwable e) {
                plugin.getLogger().severe("failed creating instance of module " + moduleRegistration.moduleClass.getSimpleName() + ": " + e.getMessage());
            }
        }

        for (Module<TCUtilsPlugin> m : list) {
            TCUtilsModule module = (TCUtilsModule) m;
            try {
                if (plugin.getConfig().getBoolean(module.getName() + ".enabled", defaultLoadModules.contains(m.getClass()))) {
                    plugin.getLogger().info("loading module " + module.getClass().getSimpleName());
                    module.load(plugin);
                    module.loaded = true;
                } else {
                    plugin.getLogger().info(String.format("module %s is not enabled", module.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, String.format("exception occurred loading module %s", m.getClass().getSimpleName()), e);
                try {
                    module.unload();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void unloadAll() {
        List<Module<TCUtilsPlugin>> r = new ArrayList<>();
        list.forEach(m -> r.add(0, m));

        for (Module<TCUtilsPlugin> m : r) {
            TCUtilsModule module = (TCUtilsModule) m;
            if (!module.loaded) continue;
            try {
                m.unload();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, String.format("exception occurred unloading %s", ((TCUtilsModule) m).getName()), e);
            }
        }
    }

    public void add(Class<? extends TCUtilsModule> moduleClass, boolean defaultLoad, String... dependencies) {
        Validate.notNull(moduleClass, "moduleClass cannot be null");

        registration.removeIf(r -> r.moduleClass.equals(moduleClass));

        for (ModuleRegistration moduleRegistration : registration) {
            if (moduleRegistration.moduleClass.getSimpleName().equals(moduleClass.getSimpleName())) {
                throw new RuntimeException("simple name of class " + moduleClass + " conflict with " + moduleRegistration.moduleClass);
            }
        }

        registration.add(new ModuleRegistration(moduleClass, defaultLoad, new HashSet<>(Arrays.asList(dependencies))));
    }

    private class ModuleRegistration {
        private final Class<? extends TCUtilsModule> moduleClass;
        private final Set<String> dependencies;
        private final boolean defaultLoad;

        private ModuleRegistration(Class<? extends TCUtilsModule> moduleClass, boolean defaultLoad, Set<String> dependencies) {
            this.moduleClass = moduleClass;
            this.dependencies = dependencies;
            this.defaultLoad = defaultLoad;
        }
    }
}

