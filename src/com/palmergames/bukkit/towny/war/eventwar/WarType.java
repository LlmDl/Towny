package com.palmergames.bukkit.towny.war.eventwar;

public enum WarType {
	// TODO: Make all these settings configurable.
	RIOT("Riot", true, false, false, false, 5, 100.0, 1),
	TOWNWAR("Town vs Town War", false, false, false, false, 5, 100.0, 2),
	CIVILWAR("National Civil War", true, false, false, false, 5, 100.0, 3),
	NATIONWAR("Nation vs Nation War", true, true, true, false, 10, 1000.0, 4),
	WORLDWAR("World War", true, true, false, true, -1, 10000.0, 5);
	
	String name;
	boolean hasTownBlockHP;
	public boolean hasMonarchDeath;
	boolean hasTownBlocksSwitchTowns;
	boolean hasTownConquering;
	public int lives;
	public int pointsPerKill;
	double baseSpoils;
	
	/**
	 * 
	 * @param name - Base name used.
	 * @param hasTownBlockHP - Whether townblocks have HP and are fought over for points.
	 * @param hasMonarchDeath - Whether killing the mayor or king will remove the town/nation from the war.
	 * @param lives - How many lives each player gets before being removed from the war.
	 * @param baseSpoils - How much money is added to the war.
	 */
	WarType(String name, boolean hasTownBlockHP, boolean hasMonarchDeath, boolean hasTownBlocksSwitchTowns, boolean hasTownConquering, int lives, double baseSpoils, int pointsPerKill) {
		this.name = name;
		this.hasTownBlockHP = hasTownBlockHP;
		this.hasMonarchDeath = hasMonarchDeath;
		this.hasTownBlocksSwitchTowns = hasTownBlocksSwitchTowns;
		this.hasTownConquering = hasTownConquering;
		this.lives = lives;
		this.baseSpoils = baseSpoils;
		this.pointsPerKill = pointsPerKill;
	}
	
	public String getName() {
		return name;
	}
}
