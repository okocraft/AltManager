package net.okocraft.altmanager.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerRenameLoginEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final String oldName;

    public PlayerRenameLoginEvent(Player player, String oldName) {
        this.player = player;
        this.oldName = oldName;
    }

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return the oldName
     */
    public String getOldName() {
        return oldName;
    }
    
}