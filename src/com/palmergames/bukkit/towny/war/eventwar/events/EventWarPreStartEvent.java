package com.palmergames.bukkit.towny.war.eventwar.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EventWarPreStartEvent extends Event{
	private static final HandlerList handlers = new HandlerList();
	private double warSpoils;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public EventWarPreStartEvent() {
		super(!Bukkit.getServer().isPrimaryThread());
		this.warSpoils = 0.0;
	}

	/** 
	 * This amount is in addition to the base_spoils amount in the config.
	 * 
	 * @return double - Amount that will be added to the WarSpoils account.
	 */
	public double getWarSpoils() {
		return warSpoils;		
	}
	
	/**
	 * Set an additional amount to the base_spoils amount in the config.
	 * 
	 * @param warSpoils - Amount that will be added to the WarSpoils.
	 */
	public void setWarSpoils(double warSpoils) {
		this.warSpoils = warSpoils;
	}
}
