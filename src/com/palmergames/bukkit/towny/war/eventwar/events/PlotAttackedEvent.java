package com.palmergames.bukkit.towny.war.eventwar.events;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.war.eventwar.War;

public class PlotAttackedEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}
	
	private TownBlock townBlock;
	private HashSet<Player> players;
	private int hp;
	private War war;
	
	public PlotAttackedEvent (TownBlock townBlock, HashSet<Player> players, int hp, War war)
	{
		super(!Bukkit.getServer().isPrimaryThread());
		this.townBlock = townBlock;
		this.players = players;
		this.hp = hp;
		this.war = war;
	}
	
	public TownBlock getTownBlock() 
	{
		return townBlock;
	}
	
	public HashSet<Player> getPlayers()
	{
		return players;
	}
	
	public int getHP()
	{
		return hp;
	}
	
	public War getWar() {
		return war;
	}
}