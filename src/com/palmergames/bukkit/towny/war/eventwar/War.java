package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.EventWarEndEvent;
import com.palmergames.bukkit.towny.event.EventWarPreStartEvent;
import com.palmergames.bukkit.towny.event.EventWarStartEvent;
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
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.ServerBroadCastTimerTask;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

//TODO: Extend a new class called TownyEvent
public class War {
	
	// War Data
	private static Hashtable<WorldCoord, Integer> warZone = new Hashtable<>();
	private Hashtable<Town, Integer> townScores = new Hashtable<>();
	public static List<Town> warringTowns = new ArrayList<>();
	public static List<Nation> warringNations = new ArrayList<>();
	private WarSpoils warSpoils = new WarSpoils();
	
	private Towny plugin;
	private boolean warTime = false;
	private List<Integer> warTaskIds = new ArrayList<>();

	/**
	 * Creates a new War instance.
	 * @param plugin - {@link Towny}
	 * @param startDelay - the delay before war will begin
	 */
	public War(Towny plugin, int startDelay) {

		this.plugin = plugin;
		TownyUniverse.getInstance();
		setupDelay(startDelay);
	}

	////START GETTERS/SETTERS ////
	
	public void addTaskId(int id) {

		warTaskIds.add(id);
	}

	public void clearTaskIds() {

		warTaskIds.clear();
	}

	public void cancelTasks(BukkitScheduler scheduler) {

		for (Integer id : getTaskIds())
			scheduler.cancelTask(id);
		clearTaskIds();
	}
	
	public void setPlugin(Towny plugin) {

		this.plugin = plugin;
	}

	public List<Integer> getTaskIds() {

		return new ArrayList<>(warTaskIds);
	}

	public Towny getPlugin() {

		return plugin;
	}

	public boolean isWarTime() {

		return warTime;
	}
	
	public WarSpoils getWarSpoils() {

		return warSpoils;
	}

	public Hashtable<Town, Integer> getTownScores()
	{
		return townScores;
	}

	public Hashtable<WorldCoord, Integer> getWarZone()
	{
		return warZone;
	}

	public List<Town> getWarringTowns()
	{
		return warringTowns;
	}
	
	public static boolean isWarZone(WorldCoord worldCoord) {

		return warZone.containsKey(worldCoord);
	}

	public boolean isWarringNation(Nation nation) {

		return warringNations.contains(nation);
	}

	public static boolean isWarringTown(Town town) {

		return warringTowns.contains(town);
	}
	
	public void toggleEnd() {
		warTime = false;
	}

	//// END GETTERS/SETTERS ////
	
	/**
	 * Creates a delay before war begins
	 * @param delay - Delay before war begins
	 */
	public void setupDelay(int delay) {

		if (delay <= 0)
			start();
		else {
			// Create a countdown timer
			for (Long t : TimeMgmt.getCountdownDelays(delay, TimeMgmt.defaultCountdownDelays)) {
				int id = BukkitTools.scheduleAsyncDelayedTask(new ServerBroadCastTimerTask(plugin, Translation.of("war_starts_in_x", TimeMgmt.formatCountdownTime(t))), TimeTools.convertToTicks((delay - t)));
				if (id == -1) {
					TownyMessaging.sendErrorMsg("Could not schedule a countdown message for war event.");
					end();
				} else
					addTaskId(id);
			}
			// Schedule set up delay
			int id = BukkitTools.scheduleAsyncDelayedTask(new StartWarTimerTask(plugin), TimeTools.convertToTicks(delay));
			if (id == -1) {
				TownyMessaging.sendErrorMsg("Could not schedule setup delay for war event.");
				end();
			} else {
				addTaskId(id);
				//start();
			}
		}
	}

	/**
	 * Start war.
	 * Seed the spoils, add the nations, start the timer task.
	 */
	public void start() {
		
		EventWarPreStartEvent preEvent = new EventWarPreStartEvent();
		Bukkit.getServer().getPluginManager().callEvent(preEvent);
		if (preEvent.getWarSpoils() != 0.0)
			try {
				warSpoils.deposit(preEvent.getWarSpoils(), "WarSpoils EventWarPreStartEvent Added");
			} catch (EconomyException ignored) {
			}

		//Gather all nations at war
		for (Nation nation : com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getNations()) {
			if (!nation.isNeutral()) {
				add(nation);
				if (warringNations.contains(nation))
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_war_join_nation", nation.getName()));
			} else if (!TownySettings.isDeclaringNeutral()) {
				nation.setNeutral(false);
				add(nation);
				if (warringNations.contains(nation))
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_war_join_forced", nation.getName()));
			}
		}

		// Cannot have a war with less than 2 nations.
		if (warringNations.size() < 2) {
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_not_enough_nations"));
			warringNations.clear();
			warringTowns.clear();
			return;
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
			return;
		}
		
		outputParticipants();

		warTime = true;

		// Seed spoils of war		
		try {
			warSpoils.deposit(TownySettings.getBaseSpoilsOfWar(), "Start of War - Base Spoils");			
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_seeding_spoils_with", TownySettings.getBaseSpoilsOfWar()));			
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_total_seeding_spoils", warSpoils.getHoldingBalance()));
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_activate_war_hud_tip"));
			
			EventWarStartEvent event = new EventWarStartEvent(warringTowns, warringNations, warSpoils.getHoldingBalance());
			Bukkit.getServer().getPluginManager().callEvent(event);
		} catch (EconomyException e) {
			TownyMessaging.sendErrorMsg("[War] Could not seed spoils of war.");
		}
		
		// Start the WarTimerTask
		int id = BukkitTools.scheduleAsyncRepeatingTask(new WarTimerTask(plugin, this), 0, TimeTools.convertToTicks(5));
		if (id == -1) {
			TownyMessaging.sendErrorMsg("Could not schedule war event loop.");
			end();
		} else
			addTaskId(id);
		checkEnd();
	}

	private void outputParticipants() {
		List<String> warParticipants = new ArrayList<>();
		for (Nation nation : warringNations) {
			int towns = 0;
			for (Town town : nation.getTowns())
				if (warringTowns.contains(town))
					towns++;
			warParticipants.add(Translation.of("msg_war_participants", nation.getName(), towns));			
		}
		TownyMessaging.sendPlainGlobalMessage(ChatTools.formatTitle("War Participants"));
		TownyMessaging.sendPlainGlobalMessage(Translation.of("msg_war_participants_header"));
		for (String string : warParticipants)
			TownyMessaging.sendPlainGlobalMessage(string);
		TownyMessaging.sendPlainGlobalMessage(ChatTools.formatTitle("----------------"));
	}

	/**
	 * End war.
	 * Send the stats to all the players, toggle all the war HUDS.
	 */
	public void end() {
		
		// Send stats to the players
		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				sendStats(player);
		
		// Toggle the war huds off for all players (This method is called from an async task so 
		// we create a sync task to use the scoreboard api)
		new BukkitRunnable() {

			@Override
			public void run() {
				plugin.getHUDManager().toggleAllWarHUD();
			}
			
		}.runTask(plugin);
		

		warringNations.clear();
		warringTowns.clear();
		warZone.clear();
		
		double halfWinnings;
		double nationWinnings = 0;
		try {
			
			// Compute war spoils
			halfWinnings = getWarSpoils().getHoldingBalance() / 2.0;
			try {
				nationWinnings = halfWinnings / warringNations.size(); // Again, might leave residue.
				for (Nation winningNation : warringNations) {
					getWarSpoils().payTo(nationWinnings, winningNation, "War - Nation Winnings");
					TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeWinningNationSpoilsMsg(winningNation, TownyEconomyHandler.getFormattedBalance(nationWinnings)));
				}
			} catch (ArithmeticException e) {
				TownyMessaging.sendDebugMsg("[War]   War ended with 0 nations.");
			}

			// Pay money to winning town and print message
			try {
				KeyValue<Town, Integer> winningTownScore = getWinningTownScore();
				getWarSpoils().payTo(halfWinnings, winningTownScore.key, "War - Town Winnings");
				TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeWinningTownSpoilsMsg(winningTownScore.key, TownyEconomyHandler.getFormattedBalance(halfWinnings), winningTownScore.value));
				
				EventWarEndEvent event = new EventWarEndEvent(warringTowns, winningTownScore.key, halfWinnings, warringNations, nationWinnings);
				Bukkit.getServer().getPluginManager().callEvent(event);
			} catch (TownyException e) {
			}
		} catch (EconomyException e1) {}
		
		
		
	}

	/**
	 * Add a nation to war, and all the towns within it.
	 * @param nation {@link Nation} to incorporate into War
	 */
	private void add(Nation nation) {

		int numTowns = 0;
		for (Town town : nation.getTowns()) {
			// A town without a homeblock cannot be won over in war.
			if (!town.hasHomeBlock())
				continue;
			try {
				if (town.getTownBlocks().size() > 0 && town.getHomeBlock().getWorld().isWarAllowed())
					add(town);
			} catch (TownyException ignored) {
			}
			if (warringTowns.contains(town))
				numTowns++;
		}
		// The nation capital must be one of the valid towns for a nation to go to war.
		if (numTowns > 0 && warringTowns.contains(nation.getCapital()))
			warringNations.add(nation);
	}

	/**
	 * Add a town to war. Set the townblocks in the town to the correct health.
	 * @param town {@link Town} to incorporate into war
	 */
	private void add(Town town) {
		int numTownBlocks = 0;
		for (TownBlock townBlock : town.getTownBlocks()) {
			if (!townBlock.getWorld().isWarAllowed())
				continue;
			numTownBlocks++;
			if (town.isHomeBlock(townBlock))
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneHomeBlockHealth());
			else
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneTownBlockHealth());
		}
		if (numTownBlocks > 0) {
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_war_join", town.getName()));
			townScores.put(town, 0);
			warringTowns.add(town);
		}			
	}

	/**
	 * A town has scored.
	 * @param town - the scoring town
	 * @param n - the score to be added
	 * @param fallenObject - the {@link Object} that fell
	 * @param townBlocksFallen -  the number of fallen townblocks {@link TownBlock}s ({@link Integer})
	 */
	public void townScored(Town town, int n, Object fallenObject, int townBlocksFallen) {

		String[] pointMessage = {"error"};
		if (fallenObject instanceof Nation)
			pointMessage = TownySettings.getWarTimeScoreNationEliminatedMsg(town, n, (Nation)fallenObject);
		else if (fallenObject instanceof Town)
			pointMessage = TownySettings.getWarTimeScoreTownEliminatedMsg(town, n, (Town)fallenObject, townBlocksFallen);
		else if (fallenObject instanceof TownBlock){	
			pointMessage = TownySettings.getWarTimeScoreTownBlockEliminatedMsg(town, n, (TownBlock)fallenObject);
		}

		townScores.put(town, townScores.get(town) + n);
		TownyMessaging.sendGlobalMessage(pointMessage);

		TownScoredEvent event = new TownScoredEvent(town, townScores.get(town));
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	/**
	 * A town has scored and there is data on the attacking/defending town
	 * @param defenderTown - {@link Town} defending an attack
	 * @param attackerTown - {@link Town} on the attack
	 * @param defenderPlayer - {@link Player} defending
	 * @param attackerPlayer - {@link Player} attacking
	 * @param n - the score to be added ({@link Integer})
	 */
	public void townScored(Town defenderTown,  Town attackerTown, Player defenderPlayer, Player attackerPlayer, int n) {
		String[] pointMessage;
		TownBlock deathLoc = TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation());
		if (deathLoc == null)
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, n, attackerTown);
		else if (warZone.containsKey(deathLoc.getWorldCoord()) && attackerTown.getTownBlocks().contains(deathLoc))
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, attackerPlayer, n, attackerTown);
		else if (warZone.containsKey(deathLoc.getWorldCoord()) && defenderTown.getTownBlocks().contains(deathLoc))
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, defenderPlayer, n, attackerTown);
		else
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, n, attackerTown);

		townScores.put(attackerTown, townScores.get(attackerTown) + n);
		TownyMessaging.sendGlobalMessage(pointMessage);

		TownScoredEvent event = new TownScoredEvent(attackerTown, townScores.get(attackerTown));
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

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
		TownyMessaging.sendMessageToMode(townBlock.getTown(), healString, "");
		for (Player p : wzd.getDefenders()) {
			if (com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getResident(p).getTown() != townBlock.getTown())
				TownyMessaging.sendMessage(p, healString);
		}
		launchFireworkAtPlot (townBlock, wzd.getRandomDefender(), Type.BALL, Color.LIME);

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	/**
	 * There are attackers on the plot, update the health.
	 * @param townBlock - The {@link TownBlock} being attacked
	 * @param wzd - {@link WarZoneData}
	 * @throws NotRegisteredException - Generic
	 */
	private void attackPlot(TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {

		Player attackerPlayer = wzd.getRandomAttacker();
		Resident attackerResident = com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getResident(attackerPlayer);
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
				TownyMessaging.sendMessageToMode(townBlock.getTown(), Colors.Gray + Translation.of("msg_war_town_under_attack")+ " (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendMessageToMode(town, Colors.Gray + Translation.of("msg_war_nation_under_attack") + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendMessageToMode(nation , Colors.Gray + Translation.of("msg_war_nations_ally_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				}
				else
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendMessageToMode(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk, "");
			} else {
				TownyMessaging.sendMessageToMode(townBlock.getTown(), Colors.Gray + Translation.of("msg_war_homeblock_under_attack")+" (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendMessageToMode(town, Colors.Gray + Translation.of("msg_war_nation_member_homeblock_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendMessageToMode(nation , Colors.Gray + Translation.of("msg_war_nation_ally_homeblock_under_attack", townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				}
				else
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendMessageToMode(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk, "");
			}
		} else {
			launchFireworkAtPlot (townBlock, attackerPlayer, Type.CREEPER, fwc);
			remove(attacker, townBlock);
		}

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp);
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
	 * Launch a {@link Firework} at a given plot
	 * @param townblock - The {@link TownBlock} to fire in
	 * @param atPlayer - The {@link Player} in which the location is grabbed
	 * @param type - The {@link FireworkEffect} type
	 * @param c - The Firework {@link Color}
	 */
	private void launchFireworkAtPlot(final TownBlock townblock, final Player atPlayer, final FireworkEffect.Type type, final Color c)
	{
		// Check the config. If false, do not launch a firework.
		if (!TownySettings.getPlotsFireworkOnAttacked()) {
			return;
		}
		
		BukkitTools.scheduleSyncDelayedTask(() -> {
			double x = (double)townblock.getX() * Coord.getCellSize() + Coord.getCellSize()/2.0;
			double z = (double)townblock.getZ() * Coord.getCellSize() + Coord.getCellSize()/2.0;
			double y = atPlayer.getLocation().getY() + 20;
			Firework firework = atPlayer.getWorld().spawn(new Location(atPlayer.getWorld(), x, y, z), Firework.class);
			FireworkMeta data = firework.getFireworkMeta();
			data.addEffects(FireworkEffect.builder().withColor(c).with(type).trail(false).build());
			firework.setFireworkMeta(data);
			firework.detonate();
		}, 0);
	}

	/**
	 * Removes a TownBlock attacked by a Town.
	 * @param attacker attackPlot method attackerResident.getTown().
	 * @param townBlock townBlock being attacked.
	 * @throws NotRegisteredException - When a Towny Object does not exist.
	 */
	private void remove(Town attacker, TownBlock townBlock) throws NotRegisteredException {
		// Add bonus blocks
		Town defenderTown = townBlock.getTown();
		boolean defenderHomeblock = townBlock.isHomeBlock();
		if (TownySettings.getWarEventCostsTownblocks() || TownySettings.getWarEventWinnerTakesOwnershipOfTownblocks()){		
			defenderTown.addBonusBlocks(-1);
			attacker.addBonusBlocks(1);
		}
		
		// We only change the townblocks over to the winning Town if the WinnerTakesOwnershipOfTown is false and WinnerTakesOwnershipOfTownblocks is true.
		if (!TownySettings.getWarEventWinnerTakesOwnershipOfTown() && TownySettings.getWarEventWinnerTakesOwnershipOfTownblocks()) {
			townBlock.setTown(attacker);
			TownyUniverse.getInstance().getDataSource().saveTownBlock(townBlock);
		}		
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			// Check for money loss in the defending town
			if (TownySettings.isUsingEconomy() && !defenderTown.getAccount().payTo(TownySettings.getWartimeTownBlockLossPrice(), attacker, "War - TownBlock Loss")) {
				TownyMessaging.sendPrefixedTownMessage(defenderTown, Translation.of("msg_war_town_ran_out_of_money"));
				TownyMessaging.sendTitleMessageToTown(defenderTown, Translation.of("msg_war_town_removed_from_war_titlemsg"), "");
				if (defenderTown.isCapital())
					remove(attacker, defenderTown.getNation());
				else
					remove(attacker, defenderTown);
				townyUniverse.getDataSource().saveTown(defenderTown);
				townyUniverse.getDataSource().saveTown(attacker);
				return;
			} else
				TownyMessaging.sendPrefixedTownMessage(defenderTown, Translation.of("msg_war_town_lost_money_townblock", TownyEconomyHandler.getFormattedBalance(TownySettings.getWartimeTownBlockLossPrice())));
		} catch (EconomyException ignored) {}
		
		// Check to see if this is a special TownBlock
		if (defenderHomeblock && defenderTown.isCapital()){
			remove(attacker, defenderTown.getNation());
		} else if (defenderHomeblock){
			remove(attacker, defenderTown);
		} else{
			townScored(attacker, TownySettings.getWarPointsForTownBlock(), townBlock, 0);
			remove(townBlock.getWorldCoord());
			// Free players who are jailed in the jail plot.
			if (townBlock.getType().equals(TownBlockType.JAIL)){
				int count = 0;
				for (Resident resident : townyUniverse.getJailedResidentMap()){
					try {						
						if (resident.isJailed())
							if (resident.getJailTown().equals(defenderTown.toString())) 
								if (Coord.parseCoord(defenderTown.getJailSpawn(resident.getJailSpawn())).toString().equals(townBlock.getCoord().toString())){
									resident.setJailed(false);
									townyUniverse.getDataSource().saveResident(resident);
									count++;
								}
					} catch (TownyException e) {
					}
				}
				if (count>0)
					TownyMessaging.sendGlobalMessage(Translation.of("msg_war_jailbreak", defenderTown, count));
			}				
		}
		townyUniverse.getDataSource().saveTown(defenderTown);
		townyUniverse.getDataSource().saveTown(attacker);
	}

	/** 
	 * Removes a Nation from the war, attacked by a Town. 
	 * @param attacker Town which attacked the Nation.
	 * @param nation Nation being removed from the war.
	 * @throws NotRegisteredException - When a Towny Object does not exist.
	 */
	public void remove(Town attacker, Nation nation) throws NotRegisteredException {

		townScored(attacker, TownySettings.getWarPointsForNation(), nation, 0);
		warringNations.remove(nation);
		TownyMessaging.sendGlobalMessage(Translation.of("msg_war_eliminated", nation));
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				remove(attacker, town);
		checkEnd();
	}

	/**
	 * Removes a Town from the war, attacked by a Town.
	 * @param attacker Town which attacked.
	 * @param town Town which is being removed from the war.
	 * @throws NotRegisteredException - When a Towny Object does not exist.
	 */
	public void remove(Town attacker, Town town) throws NotRegisteredException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Nation losingNation = town.getNation();
		
		int towns = 0;
		for (Town townsToCheck : warringTowns) {
			if (townsToCheck.getNation().equals(losingNation))
				towns++;
		}

		int fallenTownBlocks = 0;
		warringTowns.remove(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord())){
				fallenTownBlocks++;
				remove(townBlock.getWorldCoord());
			}
		townScored(attacker, TownySettings.getWarPointsForTown(), town, fallenTownBlocks);
		
		if (TownySettings.getWarEventWinnerTakesOwnershipOfTown()) {			
			town.setConquered(true);
			town.setConqueredDays(TownySettings.getWarEventConquerTime());

			// if losingNation is not a one-town nation then this.
			town.removeNation();
			try {
				town.setNation(attacker.getNation());
			} catch (AlreadyRegisteredException e) {
			}
			townyUniverse.getDataSource().saveTown(town);
			townyUniverse.getDataSource().saveNation(attacker.getNation());
			townyUniverse.getDataSource().saveNation(losingNation);
			TownyMessaging.sendGlobalMessage(Translation.of("msg_war_town_has_been_conquered_by_nation_x_for_x_days", town.getName(), attacker.getNation(), TownySettings.getWarEventConquerTime()));
		}
		
		if (towns == 1)
			remove(losingNation);
		checkEnd();
	}
	
	/**
	 * Removes a Nation from the war.
	 * Called when a Nation voluntarily leaves a war.
	 * Called by remove(Town town). 
	 * @param nation Nation being removed from the war.
	 */
	private void remove(Nation nation) {

		warringNations.remove(nation);
		sendEliminateMessage(nation.getFormattedName());
		TownyMessaging.sendTitleMessageToNation(nation, Translation.of("msg_war_nation_removed_from_war_titlemsg"), "");
		for (Town town : nation.getTowns())
			remove(town);
		checkEnd();
	}

	/**
	 * Removes a Town from the war.
	 * Called when a player is killed and their Town Bank cannot pay the war penalty.
	 * Called when a Town voluntarily leaves a War.
	 * Called by remove(Nation nation).
	 * @param town The Town being removed from the war.
	 */
	public void remove(Town town) {

		// If a town is removed, is a capital, and the nation has not been removed, call remove(nation) instead.
		try {
			if (town.isCapital() && warringNations.contains(town.getNation())) {
				remove(town.getNation());
				return;
			}
		} catch (NotRegisteredException e) {}
		
		int fallenTownBlocks = 0;
		warringTowns.remove(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord())){
				fallenTownBlocks++;
				remove(townBlock.getWorldCoord());
			}
		sendEliminateMessage(town.getFormattedName() + " (" + fallenTownBlocks + Translation.of("msg_war_append_townblocks_fallen"));
	}

	/**
	 * Removes one WorldCoord from the warZone hashtable.
	 * @param worldCoord WorldCoord being removed from the war.
	 */
	private void remove(WorldCoord worldCoord) {	
		warZone.remove(worldCoord);
	}
	
	private void sendEliminateMessage(String name) {
		TownyMessaging.sendGlobalMessage(Translation.of("msg_war_eliminated", name));
	}
	
	public void nationLeave(Nation nation) {

		remove(nation);
		TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_FORFEITED", nation.getName()));
		checkEnd();
	}

	public void townLeave(Town town) {

		remove(town);
		TownyMessaging.sendGlobalMessage(Translation.of("MSG_WAR_FORFEITED", town.getName()));
		checkEnd();
	}

	public boolean townsLeft(Nation nation) {

		return countActiveTowns(nation) > 0;
	}

	public void checkEnd() {

		if (warringNations.size() <= 1)
			toggleEnd();
		else if (CombatUtil.areAllAllies(warringNations))
			toggleEnd();
	}

	public int countActiveWarBlocks(Town town) {

		int n = 0;
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord()))
				n++;
		return n;
	}

	public int countActiveTowns(Nation nation) {

		int n = 0;
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				n++;
		return n;
	}
	
	//// CALCULATE STATS ////

	public List<String> getStats() {

		List<String> output = new ArrayList<>();
		output.add(ChatTools.formatTitle("War Stats"));
		output.add(Colors.Green + Translation.of("war_stats_nations") + Colors.LightGreen + warringNations.size());
		output.add(Colors.Green + Translation.of("war_stats_towns") + Colors.LightGreen + warringTowns.size() + " / " + townScores.size());
		output.add(Colors.Green + Translation.of("war_stats_warzone") + Colors.LightGreen + warZone.size() + " Town blocks");
		try {
			output.add(Colors.Green + Translation.of("war_stats_spoils_of_war") + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(warSpoils.getHoldingBalance()));
			return output;
		} catch (EconomyException e) {
		}
		return null;
	}
	
	public void sendStats(Player player) {

		for (String line : getStats())
			player.sendMessage(line);
	}

	//// SCORE CALCULATIONS ////
	
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
	
	public void sendScores(Player player) {

		sendScores(player, 10);
	}

	public void sendScores(Player player, int maxListing) {

		for (String line : getScores(maxListing))
			player.sendMessage(line);
	}
	
}