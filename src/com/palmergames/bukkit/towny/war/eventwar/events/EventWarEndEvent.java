package com.palmergames.bukkit.towny.war.eventwar.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public class EventWarEndEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private List<Town> warringTowns = new ArrayList<>();
	private List<Nation> warringNations = new ArrayList<>();
	private final Town winningTown;
	private final double townWinnings;
	private final double nationWinnings;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public EventWarEndEvent(List<Town> warringTowns, Town winningTown, double townWinnings, List<Nation> warringNations, double nationWinnings) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.warringNations = warringNations;
		this.warringTowns = warringTowns;
		this.winningTown = winningTown;
		this.townWinnings = townWinnings;
		this.nationWinnings = nationWinnings;
	}

	/**
	 * List of Nations who outlasted the war. Half of the warspoils is divided amongst them.
	 * 
	 * @return List&lt;Nation&gt; - The list of nations who survived the war event.
	 */
	public List<Nation> getWarringNations() {
		return this.warringNations;
	}
	
	/**
	 * List of Towns who outlasted the war.
	 * 
	 * @return List&lt;Town&gt; - The list of towns who survived the war event.
	 */
	public List<Town> getWarringTowns() {
		return this.warringTowns;
	}
	
	/**
	 * The town with the highest score, who gets half of the warspoils.
	 * 
	 * @return Town - the Town with the highest score.
	 */
	public Town getWinningTown() {
		return this.winningTown;
	}
	
	/**
	 * The Amount won by the highest scoring Town.
	 * 
	 * @return double - Amount won by the highest score.
	 */
	public double getTownWinnings() {
		return this.townWinnings;
	}
	
	/**
	 * The Amount won by the each of the surviving nations.
	 * 
	 * @return double - Amount won by each surviving nation.
	 */
	public double getNationWinnings() {
		return this.nationWinnings;
	}

}
