package me.thetd.tcutils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class AutoSave extends TCUtilsModule implements Listener {
    private int interval = -1;
    private int notify = -1;

    private int currentTick = 0;
    private BossBar bossBar = null;

    private BukkitTask schedule;

    public AutoSave() {
        super("AutoSave");
    }

    @Override
    protected void load() throws Exception {
        interval = getConfig().getInt("interval", 20 * 60 * 10);
        if (interval <= 0) throw new RuntimeException("invalid interval value: " + interval);
        notify = getConfig().getInt("notify", 20 * 30);
        if (notify <= 0 || notify > interval) throw new RuntimeException("invalid notify value: " + notify);

        Bukkit.getWorlds().forEach(w -> w.setAutoSave(false));

        schedule = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            int countdown = interval - currentTick;
            if (countdown <= notify) {
                // notify needed
                if (bossBar == null) {
                    bossBar = Bukkit.createBossBar(ChatColor.RED + "准备进行自动保存，服务器可能停止响应", BarColor.RED, BarStyle.SOLID);
                    bossBar.setVisible(true);
                    Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
                }

                bossBar.setProgress(countdown / (double) notify);
            }

            if (countdown <= 0) {
                // trigger
                Bukkit.savePlayers();
                Bukkit.getWorlds().forEach(World::save);
                bossBar.removeAll();
                bossBar.setVisible(false);
                bossBar = null;
                currentTick = 0;
            } else {
                currentTick++;
            }
        }, 1, 1);

        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    @Override
    public void unload() {
        if (schedule != null) schedule.cancel();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        if (bossBar != null) {
            bossBar.addPlayer(e.getPlayer());
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        if (bossBar != null) {
            bossBar.removePlayer(e.getPlayer());
        }
    }
}
