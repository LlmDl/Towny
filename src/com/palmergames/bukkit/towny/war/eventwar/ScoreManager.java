package com.palmergames.bukkit.towny.war.eventwar;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.eventwar.events.TownScoredEvent;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;

public class ScoreManager {

	private War war;
	private Hashtable<Town, Integer> townScores = new Hashtable<>();
	
	public ScoreManager(War war) {
		this.war = war;
	}

	public Hashtable<Town, Integer> getTownScores() {
		return townScores;
	}
	
	/*
	 * Scoring Methods
	 */
	
	/**
	 * A resident has killed another resident.
	 * @param defender - {@link Resident} dying.
	 * @param attacker - {@link Resident} killing.
	 * @param loc - {@link Location} of the death.
	 */
	public void residentScoredKillPoints(Resident defender,  Resident attacker, Location loc) {
		switch(war.getWarType()) {
		case RIOT:
			// TODO: Handle riot scoring.
			break;
		default:
			townScored(defender, attacker, loc);			
			break;
		}
	}

	/**
	 * A town has scored a kill point.
	 * 
	 * @param defender - {@link Resident} dying.
	 * @param attackerRes - {@link Resident} killing.
	 * @param loc - {@link Location} of the death.
	 */
	private void townScored(Resident defender, Resident attacker, Location loc) {
		int points = war.getWarType().pointsPerKill;
		Town attackerTown = null;
		Town defenderTown = null;
		try {
			attackerTown = attacker.getTown();
			defenderTown = defender.getTown();
		} catch (NotRegisteredException ignored) {}
		
		String pointMessage;
		TownBlock deathLoc = TownyAPI.getInstance().getTownBlock(loc);
		if (deathLoc == null)
			pointMessage = Translation.of("MSG_WAR_SCORE_PLAYER_KILL", attacker.getName(), defender.getName(), points, attackerTown.getName());
		else if (war.getWarZoneManager().isWarZone(deathLoc.getWorldCoord()) && attackerTown.getTownBlocks().contains(deathLoc))
			pointMessage = Translation.of("MSG_WAR_SCORE_PLAYER_KILL_DEFENDING", attacker.getName(), defender.getName(), attacker.getName(), points, attackerTown.getName());
		else if (war.getWarZoneManager().isWarZone(deathLoc.getWorldCoord()) && defenderTown.getTownBlocks().contains(deathLoc))
			pointMessage = Translation.of("MSG_WAR_SCORE_PLAYER_KILL_DEFENDING", attacker.getName(), defender.getName(), defender.getName(), points, attackerTown.getName());
		else
			pointMessage = Translation.of("MSG_WAR_SCORE_PLAYER_KILL", attacker.getName(), defender.getName(), points, attackerTown.getName());

		townScores.put(attackerTown, townScores.get(attackerTown) + points);
		TownyMessaging.sendGlobalMessage(pointMessage);

		TownScoredEvent event = new TownScoredEvent(attackerTown, townScores.get(attackerTown), war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}
	
	/**
	 * A town has scored.
	 * @param town - the scoring town
	 * @param n - the score to be added
	 * @param fallenObject - the {@link Object} that fell
	 * @param townBlocksFallen -  the number of fallen townblocks {@link TownBlock}s ({@link Integer})
	 */
	void townScored(Town town, int n, Object fallenObject, int townBlocksFallen) {

		String pointMessage = "";
		if (fallenObject instanceof Nation)
			pointMessage = Translation.of("MSG_WAR_SCORE_NATION_ELIM", town.getName(), n, ((Nation)fallenObject).getName());
		else if (fallenObject instanceof Town)
			pointMessage = Translation.of("MSG_WAR_SCORE_TOWN_ELIM", town.getName(), n, ((Town)fallenObject).getName(), townBlocksFallen);
		else if (fallenObject instanceof TownBlock){
			String townBlockName = "";
			try {
				townBlockName = "[" + ((TownBlock)fallenObject).getTown().getName() + "](" + ((TownBlock)fallenObject).getCoord().toString() + ")";
			} catch (NotRegisteredException ignored) {}
				pointMessage = Translation.of("MSG_WAR_SCORE_TOWNBLOCK_ELIM", town.getName(), n, townBlockName);
		}

		townScores.put(town, townScores.get(town) + n);
		TownyMessaging.sendGlobalMessage(pointMessage);

		TownScoredEvent event = new TownScoredEvent(town, townScores.get(town), war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	void processScoreOnFallenTown(Town town, Town attacker) {
		/*
		 * Count warzones that fell when the town was felled.
		 */
		int fallenTownBlocks = 0;
		for (TownBlock townBlock : town.getTownBlocks())
			if (war.getWarZoneManager().isWarZone(townBlock.getWorldCoord()))
				fallenTownBlocks++;
		
		// TODO: Another message for bulk townblock points from townblocks that did not fall until now. (Mirrors how nations' falling gives points for the eliminated towns.)
		// TODO: A config option to not pay points for townblocks which were not directly captured preventing bulk points.
		
		/*
		 * Award points for the captured town.
		 */
		townScored(attacker, TownySettings.getWarPointsForTown(), town, fallenTownBlocks);
	}
	

	public void sendScores(Player player) {

		sendScores(player, 10);
	}

	public void sendScores(Player player, int maxListing) {

		for (String line : getScores(maxListing))
			player.sendMessage(line);
	}
	
	/**
	 * Gets the scores of a {@link War}
	 * @param maxListing Maximum lines to return. Value of -1 return all.
	 * @return A list of the current scores per town sorted in descending order.
	 */
	public List<String> getScores(int maxListing) {

		List<String> output = new ArrayList<>();
		output.add(ChatTools.formatTitle("War - Top Scores"));
		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<>(townScores);
		kvTable.sortByValue();
		kvTable.reverse();
		int n = 0;
		for (KeyValue<Town, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			Town town = kv.key;
			int score = kv.value;
			if (score > 0)
				output.add(String.format(Colors.Blue + "%40s " + Colors.Gold + "|" + Colors.LightGray + " %4d", town.getFormattedName(), score));
		}
		return output;
	}

	public String[] getTopThree() {
		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<>(townScores);
		kvTable.sortByValue();
		kvTable.reverse();
		String[] top = new String[3];
		top[0] = kvTable.getKeyValues().size() >= 1 ? kvTable.getKeyValues().get(0).value + "-" + kvTable.getKeyValues().get(0).key : "";
		top[1] = kvTable.getKeyValues().size() >= 2 ? kvTable.getKeyValues().get(1).value + "-" + kvTable.getKeyValues().get(1).key : "";
		top[2] = kvTable.getKeyValues().size() >= 3 ? kvTable.getKeyValues().get(2).value + "-" + kvTable.getKeyValues().get(2).key : "";
		return top;
	}

	public KeyValue<Town, Integer> getWinningTownScore() throws TownyException {

		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<>(townScores);
		kvTable.sortByValue();
		kvTable.reverse();
		if (kvTable.getKeyValues().size() > 0)
			return kvTable.getKeyValues().get(0);
		else
			throw new TownyException();
	}

	/*
	 * Stats
	 */

	public List<String> getStats() {

		List<String> output = new ArrayList<>();
		output.add(ChatTools.formatTitle("War Stats"));
		
		switch (war.getWarType()) {
			case WORLDWAR:
			case NATIONWAR:
				output.add(Colors.Green + Translation.of("war_stats_nations") + Colors.LightGreen + war.getWarParticipants().getNations().size() + " / " + war.getWarParticipants().getNationsAtStart());
			case CIVILWAR:
			case TOWNWAR:
				output.add(Colors.Green + Translation.of("war_stats_towns") + Colors.LightGreen + war.getWarParticipants().getTowns().size() + " / " + war.getWarParticipants().getTownsAtStart());
			case RIOT:
				output.add(Colors.Green + "  Residents: " + Colors.LightGreen + war.getWarParticipants().getResidents().size() + " / " + war.getWarParticipants().getResidentsAtStart());
				break;
		}		
		if (war.getWarType().hasTownBlockHP)
			output.add(Colors.Green + Translation.of("war_stats_warzone") + Colors.LightGreen + war.getWarZoneManager().getWarZone().size() + " Town blocks");
		try {
			output.add(Colors.Green + Translation.of("war_stats_spoils_of_war") + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(war.warSpoils.getHoldingBalance()));
			return output;
		} catch (EconomyException e) {
		}
		return null;
	}
	
	public void sendStats(Player player) {

		for (String line : getStats())
			player.sendMessage(line);
	}

}
