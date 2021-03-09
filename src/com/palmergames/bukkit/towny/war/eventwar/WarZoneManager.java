package com.palmergames.bukkit.towny.war.eventwar;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.war.eventwar.events.PlotAttackedEvent;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.KeyValueTable;

/**
 * This WarZoneManager primarily handles the warZone hashtable, or list of plots involved in the war.
 * Secondarily it handles the logic that precedes a Town or Nation being removed from the war, based
 * on either economic costs or loss-of-homeblock 
 * @author Workstation
 *
 */
public class WarZoneManager {
	private War war;
	private static Hashtable<WorldCoord, Integer> warZone = new Hashtable<>();
	
	public WarZoneManager(War war) {

		this.war = war;
	}
	
	public Hashtable<WorldCoord, Integer> getWarZone() {

		return warZone;
	}
	
	public void addWarZone(WorldCoord coord, int health) {
		
		warZone.put(coord, health);
	}
	
	public boolean isWarZone(WorldCoord worldCoord) {

		return warZone.containsKey(worldCoord);
	}

	/*
	 * WarZone Updating 
	 */

	/**
	 * Update a plot given the WarZoneData on the TownBlock
	 * @param townBlock - {@link TownBlock}
	 * @param wzd - {@link WarZoneData}
	 * @throws NotRegisteredException - Generic
	 */
	public void updateWarZone (TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {
		if (!wzd.hasAttackers()) 
			healPlot(townBlock, wzd);
		else
			attackPlot(townBlock, wzd);
	}

	/**
	 * Heals a plot. Only occurs when the plot has no attackers.
	 * @param townBlock - The {@link TownBlock} to be healed.
	 * @param wzd - {@link WarZoneData}
	 * @throws NotRegisteredException - Generic
	 */
	private void healPlot(TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int healthChange = wzd.getHealthChange();
		int oldHP = warZone.get(worldCoord);
		int hp = getHealth(townBlock, healthChange);
		if (oldHP == hp)
			return;
		warZone.put(worldCoord, hp);
		String healString =  Colors.Gray + "[Heal](" + townBlock.getCoord().toString() + ") HP: " + hp + " (" + Colors.LightGreen + "+" + healthChange + Colors.Gray + ")";
		TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), healString);
		for (Player p : wzd.getDefenders()) {
			if (TownyUniverse.getInstance().getResident(p.getUniqueId()).getTown() != townBlock.getTown())
				TownyMessaging.sendMessage(p, healString);
		}
		WarUtil.launchFireworkAtPlot (townBlock, wzd.getRandomDefender(), Type.BALL, Color.LIME);

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp, war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}
	
	/**
	 * Correctly returns the health of a {@link TownBlock} given the change in the health.
	 * 
	 * @param townBlock - The TownBlock to get health of
	 * @param healthChange - Modifier to the health of the TownBlock ({@link Integer})
	 * @return the health of the TownBlock
	 */
	private int getHealth(TownBlock townBlock, int healthChange) {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int hp = warZone.get(worldCoord) + healthChange;
		boolean isHomeBlock = townBlock.isHomeBlock();
		if (isHomeBlock && hp > TownySettings.getWarzoneHomeBlockHealth())
			return TownySettings.getWarzoneHomeBlockHealth();
		else if (!isHomeBlock && hp > TownySettings.getWarzoneTownBlockHealth())
			return TownySettings.getWarzoneTownBlockHealth();
		return hp;
	}
	
	/**
	 * There are attackers on the plot, update the health.
	 * @param townBlock - The {@link TownBlock} being attacked
	 * @param wzd - {@link WarZoneData}
	 * @throws NotRegisteredException - Generic
	 */
	private void attackPlot(TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {

		Player attackerPlayer = wzd.getRandomAttacker();
		Resident attackerResident = TownyUniverse.getInstance().getResident(attackerPlayer.getUniqueId());
		Town attacker = attackerResident.getTown();

		//Health, messaging, fireworks..
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int healthChange = wzd.getHealthChange();
		int hp = getHealth(townBlock, healthChange);
		Color fwc = healthChange < 0 ? Color.RED : (healthChange > 0 ? Color.LIME : Color.GRAY);
		if (hp > 0) {
			warZone.put(worldCoord, hp);
			String healthChangeStringDef, healthChangeStringAtk;
			if (healthChange > 0) { 
				healthChangeStringDef = "(" + Colors.LightGreen + "+" + healthChange + Colors.Gray + ")";
				healthChangeStringAtk = "(" + Colors.Red + "+" + healthChange + Colors.Gray + ")";
			}
			else if (healthChange < 0) {
				healthChangeStringDef = "(" + Colors.Red + healthChange + Colors.Gray + ")";
				healthChangeStringAtk = "(" + Colors.LightGreen + healthChange + Colors.Gray + ")";
			}
			else {
				healthChangeStringDef = "(+0)";
				healthChangeStringAtk = "(+0)";
			}
			if (!townBlock.isHomeBlock()){
				TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), Colors.Gray + Translation.of("msg_war_town_under_attack") + " (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendPrefixedTownMessage(town, Colors.Gray + Translation.of("msg_war_nation_under_attack") + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendPrefixedNationMessage(nation , Colors.Gray + Translation.of("msg_war_nations_ally_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				}
				else
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendPrefixedTownMessage(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk);
			} else {
				TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), Colors.Gray + Translation.of("msg_war_homeblock_under_attack")+" (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendPrefixedTownMessage(town, Colors.Gray + Translation.of("msg_war_nation_member_homeblock_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendPrefixedNationMessage(nation , Colors.Gray + Translation.of("msg_war_nation_ally_homeblock_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef);
				}
				else
					WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendPrefixedTownMessage(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk);
			}
		} else {
			WarUtil.launchFireworkAtPlot (townBlock, attackerPlayer, Type.CREEPER, fwc);
			// If there's more than one Town involved we want to award it to the town with the most players present.
			if (wzd.getAttackerTowns().size() > 1) {
				Hashtable<Town, Integer> attackerCount = new Hashtable<Town, Integer>();
				for (Town town : wzd.getAttackerTowns()) {
					for (Player player : wzd.getAttackers()) {
						if (town.hasResident(TownyUniverse.getInstance().getResident(player.getUniqueId())))
							attackerCount.put(town, attackerCount.get(town) + 1);
					}
				}
				KeyValueTable<Town, Integer> kvTable = new KeyValueTable<>(attackerCount);
				kvTable.sortByValue();
				kvTable.reverse();
				attacker = kvTable.getKeyValues().get(0).key;
			}
			remove(townBlock, attacker);
		}

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp, war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	/**
	 * Removes a TownBlock attacked by a Town.
	 * 
	 * Can result in removing a Town if the Town cannot pay the costs of losing the townblock,
	 * or if the townblock is the homeblock of the Town.
	 * @param townBlock townBlock which fell.
	 * @param attacker Town which had the most attackers when the townblock was felled.
	 * 
	 * @throws NotRegisteredException - When a Towny Object does not exist.
	 */
	private void remove(TownBlock townBlock, Town attacker) throws NotRegisteredException {

		Town defenderTown = townBlock.getTown();

		/*
		 * Handle bonus townblocks.
		 */
		if (TownySettings.getWarEventCostsTownblocks() || TownySettings.getWarEventWinnerTakesOwnershipOfTownblocks()){		
			defenderTown.addBonusBlocks(-1);
			attacker.addBonusBlocks(1);
		}
		
		/*
		 * Handle take-over of individual TownBlocks in war. (Not used when entire Towns are conquered by Nations) TODO: Handle non-Nation war outcomes.
		 */
		if (!TownySettings.getWarEventWinnerTakesOwnershipOfTown() && TownySettings.getWarEventWinnerTakesOwnershipOfTownblocks()) {
			townBlock.setTown(attacker);
			townBlock.save();
		}		
		
		/*
		 * Handle Money penalties for loser.
		 */
		try {
			// Check for money loss in the defending town
			if (TownySettings.isUsingEconomy() && !defenderTown.getAccount().payTo(TownySettings.getWartimeTownBlockLossPrice(), attacker, "War - TownBlock Loss")) {
				TownyMessaging.sendPrefixedTownMessage(defenderTown, Translation.of("msg_war_town_ran_out_of_money"));
				// Remove the town from the war. If this is a NationWar or WorldWar it will take down the Nation.
				remove(defenderTown, attacker);
				return;
			} else
				TownyMessaging.sendPrefixedTownMessage(defenderTown, Translation.of("msg_war_town_lost_money_townblock", TownyEconomyHandler.getFormattedBalance(TownySettings.getWartimeTownBlockLossPrice())));
		} catch (EconomyException ignored) {}
		
		/*
		 * Handle homeblocks & regular townblocks & regular townblocks with jails on them.
		 */
		if (townBlock.isHomeBlock()) {
			/*
			 * Attacker has taken down a Town.
			 */
			remove(defenderTown, attacker);
		} else {
			// Remove this WorldCoord from the warZone hashtable.
			remove(townBlock.getWorldCoord());
			
			// Free warring players in a jail on this plot.
			if (townBlock.getType().equals(TownBlockType.JAIL))
				freeFromJail(townBlock, defenderTown);
			
			// Update the score. 
			war.getScoreManager().townScored(attacker, TownySettings.getWarPointsForTownBlock(), townBlock, 0);
		}
		
	}

	/**
	 * Removes a Town from the war, attacked by a Town.
	 * 
	 * Can result in removing a Nation if the WarType is NationWar or WorldWar.
	 * @param town Town which is being removed from the war.
	 * @param attacker Town which attacked.
	 * 
	 * @throws NotRegisteredException - When a Towny Object does not exist.
	 */
	public void remove(Town town, Town attacker) throws NotRegisteredException {
 		boolean isCapital = town.isCapital();

 		/*
 		 * Process the scoring of points including townblocks that haven't 
 		 * and the town itself.
 		 */
 		war.getScoreManager().processScoreOnFallenTown(town, attacker);

		/*
		 * Free any players jailed in this Town.
		 */
		freeFromJail(town);
		
		/*
		 * Finally, deal with the various WarTypes' town-falling conditions.
		 */
		switch (war.getWarType()) {
		
		case RIOT:
			break;
		case NATIONWAR:
		case WORLDWAR: 
			/*
			 * If we're dealing with either NationWar or WorldWar, losing a capital city means the whole nation is out.
			 * TODO: Potentially have this end a civil war as well. (Leaning towards no.)
			 */

			/*
			 * Handle conquering.
			 */
			if (TownySettings.getWarEventWinnerTakesOwnershipOfTown()) {
				// It is a capital.
				if (isCapital) {
					List<Town> towns = new ArrayList<>();
					towns = town.getNation().getTowns();
					// Based on config, do not conquer the capital.
					if (TownySettings.getWarEventWinnerTakesOwnershipOfTownsExcludesCapitals()) 
						towns.remove(town.getNation().getCapital());

					for (Town fallenTown : towns) {
				 		/*
				 		 * Process the scoring of points including townblocks that haven't 
				 		 * and the town itself.
				 		 */
				 		war.getScoreManager().processScoreOnFallenTown(fallenTown, attacker);
					}
					
					// Conquer all of the towns (sometimes including the capital.)
					conquer(towns, attacker.getNation());

					// Remove the capital directly, if it wasn't already removed in the conquering.
					if (war.getWarParticipants().getTowns().contains(town))
						war.getWarParticipants().remove(town);
					
					// Remove the rest of the towns.
					remove(town.getNation(), attacker);

					return;

				// Not a capital, so conquer a single town.
				} else {
			 		/*
			 		 * Process the scoring of points including townblocks that haven't 
			 		 * and the town itself.
			 		 */
			 		war.getScoreManager().processScoreOnFallenTown(town, attacker);

					
					// Remove the town directly.
					war.getWarParticipants().remove(town);
					
					// Conquer the single town.
					conquer(town, attacker.getNation());
					
					return;
				}
				
			/*
			 * No Conquering Involved.
			 */
			} else {
				// It is a capital.
				if (isCapital) {
					
			 		/*
			 		 * Process the scoring of points including townblocks that haven't 
			 		 * and the town itself.
			 		 */
			 		war.getScoreManager().processScoreOnFallenTown(town, attacker);

					// Remove the capital directly.
					war.getWarParticipants().remove(town);
					
					// Remove the rest of the towns.
					remove(town.getNation(), attacker);
					
					return;
					
				// Not a capital, so remove a single town.
				} else {
			 		/*
			 		 * Process the scoring of points including townblocks that haven't 
			 		 * and the town itself.
			 		 */
			 		war.getScoreManager().processScoreOnFallenTown(town, attacker);

					// Remove the town directly.
					war.getWarParticipants().remove(town);
					
					return;
				}
			}

		case CIVILWAR:
		case TOWNWAR:
			
			
			break;
		}
		

	}

	/** 
	 * Removes a Nation from the war, attacked by a Town. 
	 * 
	 * Only called when WarType is NationWar or WorldWar.
	 * 
	 * Called from:
	 *    the EventWarListener's onPlayerKillsPlayer(). (In which case no towns have been removed from the WarParticipants yet.
	 *    the remove(attacker, Town) and a capital has fallen. (In which case the capital is already removed from the WarParticipants.
	 * @param nation Nation being removed from the war.
	 * @param attacker Town which attacked the Nation.
	 * 
	 * @throws NotRegisteredException - When a Towny Object does not exist.
	 */
	public void remove(Nation nation, Town attacker) throws NotRegisteredException {

		/*
		 * Award points to the attacking Town for felling a nation.
		 */
		war.getScoreManager().townScored(attacker, TownySettings.getWarPointsForNation(), nation, 0);
		
		/*
		 * Award points for each Town in the Nation which wasn't already removed from the war.
		 * 
		 * In the case of conquering: the towns will already be removed from the nation and war participants.
		 */
		for (Town town : nation.getTowns())
			if (war.getWarParticipants().getTowns().contains(town))
				remove(town, attacker);

		TownyMessaging.sendGlobalMessage(Translation.of("msg_war_eliminated", nation));
		war.getWarParticipants().remove(nation);
		war.checkEnd();
	}
	
	/**
	 * Removes one WorldCoord from the warZone hashtable.
	 * @param worldCoord WorldCoord being removed from the war.
	 */
	public void remove(WorldCoord worldCoord) {	
		warZone.remove(worldCoord);
	}

	/**
	 * Method to fire a jail break if a jail plot falls in a war.
	 * 
	 * @param townBlock TownBlock which is a jail plot.
	 * @param defenderTown Town which has had their jail plot fall.
	 */
	private void freeFromJail(TownBlock townBlock, Town defenderTown) {
		List<Resident> jailedResidents = getResidentsJailedInTown(defenderTown);
		if (jailedResidents.isEmpty())
			return;

		for (Resident resident : jailedResidents) {
			try {
				if (Coord.parseCoord(defenderTown.getJailSpawn(resident.getJailSpawn())) == townBlock.getCoord()){
					jailedResidents.add(resident);
				}
			} catch (TownyException ignored) {}
		}
		
		freeFromJail(jailedResidents);
		TownyMessaging.sendGlobalMessage(Translation.of("msg_war_jailbreak", defenderTown, jailedResidents.size()));
	}
	
	/**
	 * Method to free any jailed warriors in a town.
	 * 
	 * @param town Town which has been removed from the war.
	 */
	private void freeFromJail(Town town) {
		List<Resident> jailedResidents = getResidentsJailedInTown(town);
		if (jailedResidents.isEmpty())
			return;
		freeFromJail(jailedResidents);
		TownyMessaging.sendGlobalMessage(Translation.of("msg_war_jailbreak", town, jailedResidents.size()));
	}
	
	/**
	 * Method to gather any warrior residents jailed in a given town.
	 * 
	 * @param town Town which is being tested for jailed players.
	 * @return List of Residents jailed in the Town who are involved in the war.
	 */
	private List<Resident> getResidentsJailedInTown(Town town) {
		List<Resident> jailedResidents = new ArrayList<>();
		for (Resident resident : TownyUniverse.getInstance().getJailedResidentMap()) {
			if (war.getWarParticipants().getResidents().contains(resident) && resident.isJailed() && resident.getJailTown().equalsIgnoreCase(town.getName())) 
				jailedResidents.add(resident);
		}
		return jailedResidents;
	}
	
	/**
	 * Method which frees a list of residents 
	 * @param residents - List of Residents to be freed.
	 */
	private void freeFromJail(List<Resident> residents) {
		for (Resident resident : residents) {
			resident.setJailed(false);
			resident.save();
		}
	}

	/**
	 * Conquer the given list of towns, putting them into the given nation.
	 * 
	 * @param towns - List of Towns to be conquered.
	 * @param nation - Nation to receive the conquered towns.
	 */
	private void conquer(List<Town> towns, Nation nation) {
		for (Town town : towns) {
			conquer(town, nation);
		}
	}
	
	/**
	 * Conquer a town and put it into the nation.
	 * 
	 * @param town - Town to be conquered.
	 * @param nation - Nation doing the conquering.
	 */
	private void conquer(Town town, Nation nation) {
		town.setConquered(true);
		town.setConqueredDays(TownySettings.getWarEventConquerTime());
		town.removeNation();
		try {
			town.setNation(nation);
		} catch (AlreadyRegisteredException e) {
		}
		town.save();
		TownyMessaging.sendGlobalMessage(Translation.of("msg_war_town_has_been_conquered_by_nation_x_for_x_days", town.getName(), nation.getName(), TownySettings.getWarEventConquerTime()));
		war.getWarParticipants().remove(town);
	}
}
