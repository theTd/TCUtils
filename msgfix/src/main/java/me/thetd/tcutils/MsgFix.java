package me.thetd.tcutils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.totemcraftmc.bukkitplugin.TCBaseLib.util.MessageUtil;
import org.totemcraftmc.bukkitplugin.TCBaseLib.util.PluginUtil;

public class MsgFix extends TCUtilsModule implements Listener {
    private boolean disable = false;

    public MsgFix() {
        super("MsgFix");
    }

    @Override
    protected void load() throws Exception {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
        disable = getConfig().getBoolean("disable", false);
    }

    @Override
    public void unload() {
        PluginUtil.unregisterListener(this);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        if (disable) return;
        BaseComponent[] message = new BaseComponent[]{new TextComponent(ChatColor.YELLOW + ""),
                new TranslatableComponent("multiplayer.player.joined", e.getPlayer().getName())};
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            MessageUtil.sendRawMessage(player, message, ChatMessageType.SYSTEM);
        }
    }

    @EventHandler
    private void onLeave(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        if (disable) return;
        BaseComponent[] message = new BaseComponent[]{new TextComponent(ChatColor.YELLOW + ""),
                new TranslatableComponent("multiplayer.player.left", e.getPlayer().getName())};
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            MessageUtil.sendRawMessage(player, message, ChatMessageType.SYSTEM);
        }
    }
}
