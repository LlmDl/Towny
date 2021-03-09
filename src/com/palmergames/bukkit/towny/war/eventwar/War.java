package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.NameGenerator;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarEndEvent;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarPreStartEvent;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarStartEvent;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.KeyValue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;

public class War {
	
	private Towny plugin;
	// War Managers
	private WarZoneManager warZoneManager = new WarZoneManager(this);
	private WarParticipants warParticipants = new WarParticipants(this);
	private ScoreManager scoreManager = new ScoreManager(this);
	private WarTaskManager taskManager = new WarTaskManager(this);
	
	private WarType warType;
	private String warName;	

	double warSpoilsAtStart = 0.0;
	WarSpoils warSpoils = new WarSpoils();

	/**
	 * Creates a new War instance.
	 * 
	 * @param plugin - Towny plugin.
	 * @param startDelay - int startup delay in seconds.
	 * @param nations - List&lt;Nation&gt; which will be tested and added to war.
	 * @param towns - List&lt;Town&gt; which will be tested and added to war.
	 * @param residents - List&lt;Resident&gt; which will be tested and added to war.
	 * @param warType - WarType of the war being started.
	 */
	public War(Towny plugin, int startDelay, List<Nation> nations, List<Town> towns, List<Resident> residents, WarType warType) {

		
		if (!TownySettings.isUsingEconomy()) {
			TownyMessaging.sendGlobalMessage("War Event cannot function while using_economy: false in the config.yml. Economy Required.");
        	return;
		}

		this.plugin = plugin;
		this.warType = warType;
		
		/*
		 * Currently only used to add money to the war spoils.
		 */
		EventWarPreStartEvent preEvent = new EventWarPreStartEvent();
		Bukkit.getServer().getPluginManager().callEvent(preEvent);
		if (preEvent.getWarSpoils() != 0.0)
			warSpoils.deposit(preEvent.getWarSpoils(), "WarSpoils EventWarPreStartEvent Added");


		/*
		 * Attempts to gather the given lists of nations/towns/residents into a War
		 * with at least one pair of opposing teams.
		 */
		if (!warParticipants.gatherParticipantsForWar(nations, towns, residents)) {
			end(false);
			return;
		}	

		/*
		 * Get a name for the war.
		 */
		setWarName();
		
		/*
		 * Seed the war spoils.
		 */
		try {
			warSpoils.deposit(warType.baseSpoils, "Start of " + warName + " - Base Spoils");			
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_seeding_spoils_with", TownySettings.getBaseSpoilsOfWar()));			
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_total_seeding_spoils", warSpoils.getHoldingBalance()));
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_activate_war_hud_tip"));
			
			EventWarStartEvent event = new EventWarStartEvent(warParticipants.getTowns(), warParticipants.getNations(), warSpoilsAtStart);
			Bukkit.getServer().getPluginManager().callEvent(event);
			warSpoilsAtStart = warSpoils.getHoldingBalance();
		} catch (EconomyException e) {
			TownyMessaging.sendErrorMsg("[War] Could not seed spoils of war.");
			end(false);
			return;
		}
		
		/*
		 * If things have gotten this far it is reasonable to think we can start the war.
		 */
		taskManager.setupDelay(startDelay);
	}

	/*
	 * War Start and End Methods.
	 */

	/**
	 * Start the war.
	 * 
	 * Starts the timer taxks.
	 */
	public void start() {
		
		warParticipants.outputParticipants(warType, warName);

		// Start the WarTimerTask if the war type allows for using townblock HP system.
		if (warType.hasTownBlockHP)
			taskManager.scheduleWarTimerTask(this.plugin);
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident != null && warParticipants.has(resident)) {
				player.getInventory().addItem(BookFactory.makeBook(warName, "War Declared", WarBooks.warStartBook(this)));
				warParticipants.addOnlineWarrior(player);
			}
		}
		
		checkEnd();
		TownyUniverse.getInstance().addWar(this);
	}

	/**
	 * Checks if the end has been reached.
	 */
	public void checkEnd() {

		switch(warType) {
		case WORLDWAR:
		case NATIONWAR:
			if (warParticipants.getNations().size() <= 1)
				end(true);
			else if (CombatUtil.areAllAllies(warParticipants.getNations()))
				end(true);
			break;
		case CIVILWAR:
			if (warParticipants.getTowns().size() <= 1)
				end(true);
			break;
		case TOWNWAR:
			if (warParticipants.getTowns().size() <= 1)
				end(true);
			// TODO: Handle town neutrality.
			break;
		case RIOT:
			if (warParticipants.getResidents().size() <= 1)
				end(true);
			else if (CombatUtil.areAllFriends(warParticipants.getResidents()))
				end(true);
			break;
		}
	}

	/**
	 * Ends the war.
	 * 
	 * Send the stats to all the players, toggle all the war HUDS.
	 * @param endedSuccessful - False if something has caused the war to finish before it should have.
	 */
	public void end(boolean endedSuccessful) {
		
		/*
		 * Print out stats to players
		 */
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null)
				TownyMessaging.sendMessage(player, getScoreManager().getStats());
		}

		/*
		 * Kill the war huds.
		 */
		removeWarHuds(this);
		
		/*
		 * Pay out the money.
		 */
		if (endedSuccessful)
			awardSpoils();

		/*
		 * Null this war.
		 */
		TownyUniverse.getInstance().removeWar(this);
	}

	/*
	 * Getters and Setters
	 */

	public Towny getPlugin() {
		return plugin;
	}

	public WarZoneManager getWarZoneManager() {
		return warZoneManager;
	}
	
	public WarType getWarType() {
		return warType;
	}

	public WarParticipants getWarParticipants() {
		return warParticipants;
	}
	
	public ScoreManager getScoreManager() {
		return scoreManager;
	}

	public WarSpoils getWarSpoils() {

		return warSpoils;
	}
	
	public String getWarName() {
		return warName;
	}

	/**
	 * Picks out a name (sometimes a randomly generated one,) for the war.
	 */
	private void setWarName() {
		
		String warName = null;
		switch (warType) {
		case WORLDWAR:
			warName = String.format("World War of %s", NameGenerator.getRandomWarName());
			break;
		case NATIONWAR:			
			warName = String.format("War of %s", NameGenerator.getRandomWarName());
			break;
		case TOWNWAR:
			warName = String.format("%s - %s Skirmish", warParticipants.getTowns().get(0), warParticipants.getTowns().get(1));
			break;
		case CIVILWAR:
			warName = String.format("%s Civil War", warParticipants.getNations().get(0));
			break;
		case RIOT:
			warName = String.format("%s Riot", warParticipants.getTowns().get(0));
			break;
		}
		this.warName = warName;
	}

	/*
	 * Private End of War Methods
	 */
	
	/**
	 * Remove all the war huds.
	 * @param war
	 */
	private void removeWarHuds(War war) {
		new BukkitRunnable() {

			@Override
			public void run() {
				plugin.getHUDManager().toggleAllWarHUD(war);
			}
			
		}.runTask(plugin);		
	}
	
	/**
	 * Pay out the money to the winner(s).
	 */
	private void awardSpoils() {
		double halfWinnings;
		double nationWinnings = 0;
		try {
			
			// Compute war spoils
			halfWinnings = getWarSpoils().getHoldingBalance() / 2.0;
			try {
				nationWinnings = halfWinnings / getWarParticipants().getNations().size(); // Again, might leave residue.
				for (Nation winningNation : getWarParticipants().getNations()) {
					getWarSpoils().payTo(nationWinnings, winningNation, "War - Nation Winnings");
					TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_WINNING_NATION_SPOILS", winningNation.getName(), TownyEconomyHandler.getFormattedBalance(nationWinnings)));
				}
			} catch (ArithmeticException e) {
				TownyMessaging.sendDebugMsg("[War]   War ended with 0 nations.");
			}

			// Pay money to winning town and print message
			try {
				KeyValue<Town, Integer> winningTownScore = getScoreManager().getWinningTownScore();
				getWarSpoils().payTo(halfWinnings, winningTownScore.key, "War - Town Winnings");
				TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_WINNING_TOWN_SPOILS", winningTownScore.key.getName(), TownyEconomyHandler.getFormattedBalance(halfWinnings),  winningTownScore.value));
				
				EventWarEndEvent event = new EventWarEndEvent(getWarParticipants().getTowns(), winningTownScore.key, halfWinnings, getWarParticipants().getNations(), nationWinnings);
				Bukkit.getServer().getPluginManager().callEvent(event);
			} catch (TownyException e) {
			}
		} catch (EconomyException e1) {}
	}

}