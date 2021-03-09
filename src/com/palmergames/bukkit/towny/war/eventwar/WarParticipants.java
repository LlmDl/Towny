package com.palmergames.bukkit.towny.war.eventwar;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;

public class WarParticipants {
	private War war;
	private List<Town> warringTowns = new ArrayList<>();
	private List<Nation> warringNations = new ArrayList<>();
	private List<Resident> warringResidents = new ArrayList<>();
	private List<Player> onlineWarriors = new ArrayList<>();
	private Hashtable<Resident, Integer> residentLives = new Hashtable<>();
	private int totalResidentsAtStart = 0;
	private int totalTownsAtStart = 0;
	private int totalNationsAtStart = 0;
	
	public WarParticipants(War war) {
		this.war = war;
	}
	
	public List<Nation> getNations() {
		return warringNations;
	}
	
	public List<Town> getTowns() {
		return warringTowns;
	}
	
	public List<Resident> getResidents() {
		return warringResidents;
	}
	
	public List<Player> getOnlineWarriors() {
		return onlineWarriors;
	}
	
	public int getNationsAtStart() {
		return totalNationsAtStart;
	}
	
	public int getTownsAtStart() {
		return totalTownsAtStart;
	}
	
	public int getResidentsAtStart() {
		return totalResidentsAtStart;
	}
	
	private void setNationsAtStart(int n) {
		totalNationsAtStart = n;
	}
	
	private void setTownsAtStart(int n) {
		totalTownsAtStart = n;
	}
	
	private void setResidentsAtStart(int n) {
		totalResidentsAtStart = n;
	}
	
	public boolean has(Nation nation) {
		return warringNations.contains(nation);
	}

	public boolean has(Town town) {
		return warringTowns.contains(town);
	}
	
	public boolean has(Resident resident) {
		return warringResidents.contains(resident);
	}
	
	/**
	 * Add a nation to war, and all the towns within it.
	 * @param nation {@link Nation} to incorporate into War.
	 * @return false if conditions are not met.
	 */
	boolean add(Nation nation) {
		if (nation.getEnemies().size() < 1)
			return false;
		int enemies = 0;
		for (Nation enemy : nation.getEnemies()) {
			if (enemy.hasEnemy(nation))
				enemies++;
		}
		if (enemies < 1)
			return false;
		
		int numTowns = 0;
		for (Town town : nation.getTowns()) {
			if (add(town)) {
				warringTowns.add(town);
				war.getScoreManager().getTownScores().put(town, 0);
				numTowns++;
			}
		}
		// The nation capital must be one of the valid towns for a nation to go to war.
		if (numTowns > 0 && warringTowns.contains(nation.getCapital())) {
			TownyMessaging.sendPrefixedNationMessage(nation, "You have joined a war of type: " + war.getWarType().getName());
			warringNations.add(nation);
			return true;
		} else {
			for (Town town : nation.getTowns()) {
				if (warringTowns.contains(town)) {
					warringTowns.remove(town);
					war.getScoreManager().getTownScores().remove(town);
				}
			}
			return false;
		}
	}
	
	/**
	 * Add a town to war. Set the townblocks in the town to the correct health.
	 * Add the residents to the war, give them their lives.
	 * @param town {@link Town} to incorporate into war
	 * @return false if conditions are not met.
	 */
	boolean add(Town town) {
		int numTownBlocks = 0;
		
		/*
		 * With the instanced war system, Towns can only have one on-going war.
		 * TODO: make this a setting which will recover from a crash/shutdown.
		 */
		if (town.hasActiveWar()) {
			TownyMessaging.sendErrorMsg("The town " + town.getName() + " is already involved in a war. They will not take part in the war.");
			return false;
		}

		/*
		 * Homeblocks are absolutely required for a war.
		 */
		if (!town.hasHomeBlock()) {
			TownyMessaging.sendErrorMsg("The town " + town.getName() + " does not have a homeblock. They will not take part in the war.");
			return false;
		}
		
		/*
		 * Limit war to towns in worlds with war allowed.
		 */
		try {
			if (!town.getHomeBlock().getWorld().isWarAllowed()) {
				TownyMessaging.sendErrorMsg("The town " + town.getName() + " exists in a world with war disabled. They will not take part in the war.");
				return false;
			}
		} catch (TownyException ignored) {}
		
		/*
		 * Even if TownBlock HP is not a factor we 
		 * still need a list of warzone plots.
		 */
		for (TownBlock townBlock : town.getTownBlocks()) {
			if (!townBlock.getWorld().isWarAllowed())
				continue;
			numTownBlocks++;
			if (town.isHomeBlock(townBlock))
				war.getWarZoneManager().addWarZone(townBlock.getWorldCoord(), TownySettings.getWarzoneHomeBlockHealth());
			else
				war.getWarZoneManager().addWarZone(townBlock.getWorldCoord(), TownySettings.getWarzoneTownBlockHealth());
		}
		
		/*
		 * This should probably not happen because of the homeblock test above.
		 */
		if (numTownBlocks < 1) {
			TownyMessaging.sendErrorMsg("The town " + town.getName() + " does not have any land to fight over. They will not take part in the war.");
			return false;
		}	

		TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_war_join", town.getName()));
		TownyMessaging.sendPrefixedTownMessage(town, "You have joined a war of type: " + war.getWarType().getName());
		
		warringResidents.addAll(town.getResidents());
		
		/*
		 * Give the players their lives.
		 * TODO: Make mayors/kings have the ability to receive a different amount.
		 */
		for (Resident resident : town.getResidents()) 
			residentLives.put(resident, war.getWarType().lives);

		return true;
	}
	
	public void addOnlineWarrior(Player player) {
		onlineWarriors.add(player);
	}

	public void removeOnlineWarrior(Player player) {
		onlineWarriors.remove(player);
	}

	/**
	 * Method for gathering the nations, towns and residents which will join a war.
	 * 
	 * @param nations - List&lt;Nation&gt; which will be tested and added to war.
	 * @param towns - List&lt;Town&gt; which will be tested and added to war.
	 * @param residents - List&lt;Resident&gt; which will be tested and added to war.
	 * @return true if there are enough participants on opposing sides to have a war.
	 */
	public boolean gatherParticipantsForWar(List<Nation> nations, List<Town> towns, List<Resident> residents) {
		/*
		 * Takes the given lists and add them to War lists, if they 
		 * meet the requires set out in add(Town) and add(Nation),
		 * based on the WarType.
		 */
		switch(war.getWarType()) {
			case WORLDWAR:
			case NATIONWAR:
			case CIVILWAR:
				for (Nation nation : nations) {
					if (!nation.isNeutral() && add(nation)) {
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_war_join_nation", nation.getName()));
					} else if (!TownySettings.isDeclaringNeutral()) {
						nation.setNeutral(false);
						if (add(nation)) {
							TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_war_join_forced", nation.getName()));
						}
					}
				}
				break;
			case TOWNWAR:
			case RIOT:
				for (Town town : towns) {
					// TODO: town neutrality tests here
					if (add(town))
						warringTowns.add(town);
				}
				break;
		}

		/*
		 * Make sure that we have enough people/towns/nations involved
		 * for the give WarType.
		 */
		if (!verifyTwoEnemies()) {
			TownyMessaging.sendGlobalMessage("Failed to get the correct number of teams for war to happen! Good-bye!");
			return false;
		}
		
		setNationsAtStart(warringNations.size());
		setTownsAtStart(warringTowns.size());
		setResidentsAtStart(warringResidents.size());

		return true;
	}
	
	/**
	 * Verifies that for the WarType there are enough residents/towns/nations involved to have at least 2 sides.
	 * @return
	 */
	private boolean verifyTwoEnemies() {
		switch(war.getWarType()) {
		case WORLDWAR:
		case NATIONWAR:
			// Cannot have a war with less than 2 nations.
			if (warringNations.size() < 2) {
				TownyMessaging.sendGlobalMessage(Translation.of("msg_war_not_enough_nations"));
				warringNations.clear();
				warringTowns.clear();
				return false;
			}
			
			// Lets make sure that at least 2 nations consider each other enemies.
			boolean enemy = false; 
			for (Nation nation : warringNations) {
				for (Nation nation2 : warringNations) {
					if (nation.hasEnemy(nation2) && nation2.hasEnemy(nation)) {
						enemy = true;
						break;
					}
				}			
			}
			if (!enemy) {
				TownyMessaging.sendGlobalMessage(Translation.of("msg_war_no_enemies_for_war"));
				return false;
			}
			break;
		case CIVILWAR:
			if (warringNations.size() > 1) {
				TownyMessaging.sendGlobalMessage("Too many nations for a civil war!");
				return false;
			}
			break;
		case TOWNWAR:
			if (warringTowns.size() < 2) {
				TownyMessaging.sendGlobalMessage("Not enough Towns for town vs town war!");
				return false;
			}
			//TODO: add town enemy checking.
			break;
		case RIOT:
			if (warringTowns.size() > 1 ) {
				TownyMessaging.sendGlobalMessage("Too many towns gathered for a riot war!");
			}
			break;
		}
		
		for (Town town : warringTowns) {
			town.setActiveWar(true);
		}
		return true;
	}
	
	/**
	 * Used at war start and in the /towny war participants command.
	 * 
	 * @param warType WarType of the war.
	 * @param name The formal name of the war.  
	 */
	public void outputParticipants(WarType warType, String name) {
		List<String> warParticipants = new ArrayList<>();
		
		switch (warType) {
		case WORLDWAR:
		case NATIONWAR:
		case CIVILWAR:
			Translation.of("msg_war_participants_header");
			for (Nation nation : warringNations) {
				int towns = 0;
				for (Town town : nation.getTowns())
					if (warringTowns.contains(town))
						towns++;
				warParticipants.add(Translation.of("msg_war_participants", nation.getName(), towns));			
			}
			break;
		case TOWNWAR:
			warParticipants.add(Colors.translateColorCodes("&6[War] &eTown Name &f(&bResidents&f)"));
			for (Town town : warringTowns) {
				warParticipants.add(Translation.of("msg_war_participants", town.getName(), town.getResidents().size()));
			}
			break;
		case RIOT:
			warParticipants.add(Colors.translateColorCodes("&6[War] &eResident Name &f(&bLives&f) "));
			for (Resident resident : warringResidents) {
				warParticipants.add(Translation.of("msg_war_participants", resident.getName(), getLives(resident)));
			}
			break;
		}
		for (Nation nation : warringNations) {
			int towns = 0;
			for (Town town : nation.getTowns())
				if (warringTowns.contains(town))
					towns++;
			warParticipants.add(Translation.of("msg_war_participants", nation.getName(), towns));			
		}
		TownyMessaging.sendPlainGlobalMessage(ChatTools.formatTitle(name + " Participants"));

		for (String string : warParticipants)
			TownyMessaging.sendPlainGlobalMessage(string);
		TownyMessaging.sendPlainGlobalMessage(ChatTools.formatTitle("----------------"));
	}
	
	/**
	 * Removes a Nation from the war.
	 * Called when a Nation voluntarily leaves a war.
	 * Called by remove(Town town). 
	 * @param nation Nation being removed from the war.
	 */
	void remove(Nation nation) {

		// remove Nation
		warringNations.remove(nation);		
		
		// Send title message to nation and elimination message globally.
		TownyMessaging.sendTitleMessageToNation(nation, Translation.of("msg_war_nation_removed_from_war_titlemsg"), "");		
		sendEliminateMessage(nation.getFormattedName());
		
		// Cleanup in case they were not already removed from the war.
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				remove(town);
	}

	/**
	 * Removes a Town from the war.
	 * Called when a player is killed and their Town Bank cannot pay the war penalty.
	 * Called when a Town voluntarily leaves a War.
	 * Called by remove(Nation nation).
	 * @param town The Town being removed from the war.
	 */
	void remove(Town town) {

		// remove Town
		warringTowns.remove(town);
				
		
		// remove Residents still in the war.
		for (Resident resident : town.getResidents()) {
			if (warringResidents.contains(resident))
				remove(resident);
		}
		
		// Remove TownBlocks still in the war & count them for the eliminated message.
		int fallenTownBlocks = 0;
		for (TownBlock townBlock : town.getTownBlocks())
			if (war.getWarZoneManager().isWarZone(townBlock.getWorldCoord())){
				fallenTownBlocks++;
				war.getWarZoneManager().remove(townBlock.getWorldCoord());
			}
		
		// Disable activewar flag on Town so they can take part in another war.
		town.setActiveWar(false);
		
		// Send title message to town and elimination message globally.		
		TownyMessaging.sendTitleMessageToTown(town, Translation.of("msg_war_town_removed_from_war_titlemsg"), "");
		sendEliminateMessage(town.getFormattedName() + " (" + fallenTownBlocks + Translation.of("msg_war_append_townblocks_fallen"));
	}
	
	/**
	 * Removes a resident from the war.
	 * 
	 * Called by takeLife(Resident resident)
	 * Called by remove(Town town)
	 * @param resident
	 */
	public void remove(Resident resident) {
		warringResidents.remove(resident);
	}

	public int getLives(Resident resident) {
		return residentLives.get(resident);
	}
	
	/**
	 * Takes a life from the resident, removes them from the war if they have none remaining.
	 * 
	 * @param resident Resident losing a life.
	 */
	public void takeLife(Resident resident) {
		residentLives.put(resident, residentLives.get(resident) - 1);
	}

	private void sendEliminateMessage(String name) {
		TownyMessaging.sendGlobalMessage(Translation.of("msg_war_eliminated", name));
	}

	/*
	 * Voluntary leaving section. (UNUSED AS OF YET)
	 * 
	 * TODO: set up some leave commands because these are unused!
	 */

	@Deprecated
	public void nationLeave(Nation nation) {

		remove(nation);
		TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_FORFEITED", nation.getName()));
		war.checkEnd();
	}

	@Deprecated
	public void townLeave(Town town) {

		remove(town);
		TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_FORFEITED", town.getName()));
		war.checkEnd();
	}


}
