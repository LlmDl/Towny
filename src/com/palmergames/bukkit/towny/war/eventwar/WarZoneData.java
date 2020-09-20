package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class WarZoneData {

	private HashSet<Player> attackers;
	private HashSet<Player> defenders;
	private HashSet<Town> attackerTowns;
	private HashSet<Player> allPlayers;
	
	public WarZoneData () {
		attackers = new HashSet<Player>();
		defenders = new HashSet<Player>();
		attackerTowns = new HashSet<Town>();
		allPlayers = new HashSet<Player>();
	}

	public int getHealthChange () {
		//check config
		return defenders.size() - attackers.size();
	}
	
	public void addAttacker (Player p) throws NotRegisteredException {
		if (!p.isDead()){
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(p);
			attackerTowns.add(resident.getTown());
			allPlayers.add(p);
			attackers.add(p);
		}
	}
	
	public void addDefender (Player p) {
		if (!p.isDead()){
			allPlayers.add(p);
			defenders.add(p);
		}
	}
	
	public boolean hasAttackers() {
		return !attackers.isEmpty();
	}
	
	public boolean hasDefenders() {
		return !defenders.isEmpty();
	}
	
	public HashSet<Player> getAttackers() {
		return attackers;
	}
	
	public HashSet<Player> getDefenders() {
		return defenders;
	}
	
	public HashSet<Town> getAttackerTowns() {
		return attackerTowns;
	}
	
	public Player getRandomAttacker() {
		int index = (int)(Math.random() * attackers.size());
		int curIndex = 0;
		for (Player p : attackers) {
			if (curIndex == index)
				return p;
			curIndex++;
		}
		//No players in the list
		return null;
	}
	
	public Player getRandomDefender() {
		int index = (int)(Math.random() * defenders.size());
		int curIndex = 0;
		for (Player p : defenders) {
			if (curIndex == index)
				return p;
			curIndex++;
		}
		//No players in the list
		return null;
	}
	
	public HashSet<Player> getAllPlayers() {
		return allPlayers;
	}
}