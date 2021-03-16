package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event called whenever town map color is being calculated
 */
public class TownMapColourCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Town town;
	private String mapColorHexCode;

	public TownMapColourCalculationEvent(Town town, String mapColorHexCode) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.mapColorHexCode = mapColorHexCode;
	}

	public Town getTown() {
		return town;
	}

	public String getMapColorHexCode() {
		return mapColorHexCode;
	}

	private void setMapColorHexCode(String mapColorHexCode) {
		this.mapColorHexCode = mapColorHexCode;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
