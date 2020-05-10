package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotPreChangeTypeEvent extends Event implements Cancellable {
    public static final HandlerList handlers = new HandlerList();
    private TownBlockType oldType;
    private TownBlockType newType;
    private TownBlock townBlock;
	private String cancelMessage = "Sorry this event was cancelled";
	private boolean isCancelled = false;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
	 * Changes a plot's TownBlockType
	 * 
     * @param oldType- Old Type
     * @param newType - New Type
	 * @param townBlock - Plot to target
     */
    public PlotPreChangeTypeEvent(TownBlockType oldType, TownBlockType newType, TownBlock townBlock) {
    	super(!Bukkit.getServer().isPrimaryThread());
        this.newType = newType;
        this.oldType = oldType;
        this.townBlock = townBlock;
    }

    public TownBlockType getNewType() {
        return newType;
    }

    public TownBlockType getOldType() {
        if (oldType == null) {
            return TownBlockType.WILDS; // Considering the further fact we know null is wilderness if there is no old type, it has to have been wilderness.
        }
        return oldType;
    }

    public TownBlock getTownBlock() {
        return townBlock;
    }

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
