package me.thetd.tcutils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.lang.reflect.InvocationTargetException;

public class AutoRespawn extends TCUtilsModule implements Listener {
    private PacketContainer packet;

    public AutoRespawn() {
        super("AutoRespawn");
        packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Client.CLIENT_COMMAND);
        packet.getClientCommands().write(0, EnumWrappers.ClientCommand.PERFORM_RESPAWN);
    }

    @Override
    protected void load() {
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    @Override
    public void unload() {
        PlayerDeathEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    private void handleDeath(PlayerDeathEvent e) {
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
            try {
                ProtocolLibrary.getProtocolManager().recieveClientPacket(e.getEntity(), packet);
            } catch (IllegalAccessException | InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }, 1);
    }
}

