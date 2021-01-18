package com.palmergames.bukkit.towny;

import com.earth2me.essentials.Essentials;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.metrics.Metrics;
import com.palmergames.bukkit.towny.chat.TNCRegister;
import com.palmergames.bukkit.towny.command.InviteCommand;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.command.ResidentCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyAdminCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.command.TownyWorldCommand;
import com.palmergames.bukkit.towny.command.commandobjects.AcceptCommand;
import com.palmergames.bukkit.towny.command.commandobjects.CancelCommand;
import com.palmergames.bukkit.towny.command.commandobjects.ConfirmCommand;
import com.palmergames.bukkit.towny.command.commandobjects.DenyCommand;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.listeners.TownyBlockListener;
import com.palmergames.bukkit.towny.listeners.TownyCustomListener;
import com.palmergames.bukkit.towny.listeners.TownyEntityListener;
import com.palmergames.bukkit.towny.listeners.TownyEntityMonitorListener;
import com.palmergames.bukkit.towny.listeners.TownyInventoryListener;
import com.palmergames.bukkit.towny.listeners.TownyLoginListener;
import com.palmergames.bukkit.towny.listeners.TownyPlayerListener;
import com.palmergames.bukkit.towny.listeners.TownyServerListener;
import com.palmergames.bukkit.towny.listeners.TownyVehicleListener;
import com.palmergames.bukkit.towny.listeners.TownyWorldListener;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.BukkitPermSource;
import com.palmergames.bukkit.towny.permissions.GroupManagerSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.permissions.VaultPermSource;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.war.common.WarZoneListener;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.listeners.FlagWarBlockListener;
import com.palmergames.bukkit.towny.war.flagwar.listeners.FlagWarCustomListener;
import com.palmergames.bukkit.towny.war.flagwar.listeners.FlagWarEntityListener;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.StringMgmt;

import net.milkbowl.vault.permission.Permission;

import org.apache.commons.lang.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Towny Plugin for Bukkit
 * 
 * Website &amp; Source: https://github.com/TownyAdvanced/Towny
 * 
 * @author Shade, ElgarL, LlmDl
 */
public class Towny extends JavaPlugin {
	private static final Logger LOGGER = LogManager.getLogger(Towny.class);
	private static final Version NETHER_VER = Version.fromString("1.16.1");
	private static final Version CUR_BUKKIT_VER = Version.fromString(Bukkit.getBukkitVersion());
	private final String version = this.getDescription().getVersion();

	private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
	private final TownyVehicleListener vehicleListener = new TownyVehicleListener(this);
	private final TownyBlockListener blockListener = new TownyBlockListener(this);
	private final TownyCustomListener customListener = new TownyCustomListener(this);
	private final TownyEntityListener entityListener = new TownyEntityListener(this);
	private final TownyServerListener serverListener = new TownyServerListener(this);
	private final TownyEntityMonitorListener entityMonitorListener = new TownyEntityMonitorListener(this);
	private final TownyWorldListener worldListener = new TownyWorldListener(this);
	private final TownyInventoryListener inventoryListener = new TownyInventoryListener();
	private final FlagWarBlockListener flagWarBlockListener = new FlagWarBlockListener(this);
	private final FlagWarCustomListener flagWarCustomListener = new FlagWarCustomListener(this);
	private final FlagWarEntityListener flagWarEntityListener = new FlagWarEntityListener();
	private final WarZoneListener warzoneListener = new WarZoneListener(this);
	private final TownyLoginListener loginListener = new TownyLoginListener();
	private final HUDManager HUDManager = new HUDManager(this);

	private TownyUniverse townyUniverse;

	private final Map<String, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<>());

	private Essentials essentials = null;
	private boolean citizens2 = false;
	public static boolean isSpigot = false;

	private boolean error = false;
	
	private static Towny plugin;
	
	public Towny() {
		
		plugin = this;
	}

	@Override
	public void onEnable() {

		System.out.println("====================      Towny      ========================");

		townyUniverse = TownyUniverse.getInstance();
		
		isSpigot = BukkitTools.isSpigot();

		// Setup classes
		BukkitTools.initialize(this);
		TownyTimerHandler.initialize(this);
		TownyEconomyHandler.initialize(this);
		TownyFormatter.initialize();
		PlayerCacheUtil.initialize(this);
		SpawnUtil.initialize(this);
		TownyPerms.initialize(this);
		InviteHandler.initialize(this);

		
		if (load()) {
			// Setup bukkit command interfaces
			registerSpecialCommands();
			registerCommands();

			// Add custom metrics charts.
			addMetricsCharts();

			// Begin FlagWar.
			FlagWar.onEnable();

			if (TownySettings.isTownyUpdating(getVersion())) {
				
				printChangelogToConsole();
				// Update config with new version.
				TownySettings.setLastRunVersion(getVersion());
				// Save database.
				townyUniverse.getDataSource().saveAll();
				// cleanup() updates SQL schema for any changes.
				townyUniverse.getDataSource().cleanup();
			}

			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();
		}

		registerEvents();

		System.out.println("=============================================================");
		if (isError()) {
			System.out.println("[WARNING] - ***** SAFE MODE ***** " + version);
		} else {
			System.out.println("[Towny] Version: " + version + " - Plugin Enabled");
		}
		System.out.println("=============================================================");

		if (!isError()) {
			// Re login anyone online. (In case of plugin reloading)
			for (Player player : BukkitTools.getOnlinePlayers())
				if (player != null) {
					
					// Test and kick any players with invalid names.
					if (player.getName().contains(" ")) {
						player.kickPlayer("Invalid name!");
						return;
					}

					// Perform login code in it's own thread to update Towny data.
					if (BukkitTools.scheduleSyncDelayedTask(new OnPlayerLogin(this, player), 0L) == -1) {
						TownyMessaging.sendErrorMsg("Could not schedule OnLogin.");
					}
				}
		}
	}

	@Override
	public void onDisable() {

		System.out.println("==============================================================");
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (townyUniverse.getDataSource() != null && !error) {
			townyUniverse.getDataSource().saveQueues();
		}

		if (!error) {
			FlagWar.onDisable();
		}

		if (TownyAPI.getInstance().isWarTime()) {
			TownyUniverse.getInstance().getWarEvent().toggleEnd();
		}

		// Turn off timers.		
		toggleTimersOff();

		TownyRegenAPI.cancelProtectionRegenTasks();

		playerCache.clear();
		
		try {
			// Shut down our saving task.
			System.out.println("[Towny] Finishing File IO Tasks...");
			townyUniverse.getDataSource().finishTasks();
			townyUniverse.finishTasks();
		} catch (NullPointerException ignored) {
			// The saving task will not have started if this disable was fired by onEnable failing.			
		}

		this.townyUniverse = null;

		System.out.println("[Towny] Version: " + version + " - Plugin Disabled");
		System.out.println("=============================================================");
	}

	/**
	 * Attempts to load language, config and database files.
	 * Checks for plugins, ends and begins timers, resets player cache. 
	 * 
	 * @return true if things have loaded without error.
	 */
	public boolean load() {

		// Things which have to be done first.
		checkCitizens();
		TownyTimerHandler.toggleGatherResidentUUIDTask(false);
		
		// Load Config, Language Files, Database.
		if (!townyUniverse.loadSettings()) {
			setError(true);
			return false;
		}

		// Check for plugins that we use, we develop.
		checkPlugins();

		// Make sure the timers are stopped for a reset, then started.
		cycleTimers();
		
		// Reset player cache.
		resetCache();

		return true;
	}

	private void checkCitizens() {
		/*
		 * Test for Citizens2 so we can avoid removing their NPC's
		 */
		Plugin test = getServer().getPluginManager().getPlugin("Citizens");
		if (test != null)
			citizens2 = getServer().getPluginManager().getPlugin("Citizens").isEnabled();
	}
	
	private void checkPlugins() {

		System.out.println("[Towny] Searching for third-party plugins...");
		String ecowarn = "";
		List<String> addons = new ArrayList<>();
		Plugin test;

	 	//Check for permission source.
		String output = returnPermissionsProviders();

		/*
		 * Check for economy source.
		 */
		if (TownySettings.isUsingEconomy()) {			
			if (TownyEconomyHandler.setupEconomy()) {
				output += System.lineSeparator() + "  Economy: " + TownyEconomyHandler.getVersion();				
				if (TownyEconomyHandler.getVersion().startsWith("Essentials Economy"))
					ecowarn = "Warning: Essentials Economy has been known to reset town and nation bank accounts to their default amount. The authors of Essentials recommend using another economy plugin until they have fixed this bug.";
				
		        File f = new File(TownyUniverse.getInstance().getRootFolder(), "debtAccountsConverted.txt");                   // For a short time Towny stored debt accounts in the server's
		        if (!f.exists())                                                                                               // economy plugin. This practice had to end, being replaced 
		        	Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> MoneyUtil.convertLegacyDebtAccounts(), 600l); // with the debtBalance which is stored in the Town object.
				
		        f = new File(TownyUniverse.getInstance().getRootFolder(), "legacyAccountsConverted.txt");
		        if (!f.exists())
		        	Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> MoneyUtil.convertLegacyBankAccounts(), 600l);
					
			} else {
				ecowarn = "Warning: No compatible Economy plugins found. Install Vault.jar or Reserve.jar with any of the supported eco systems. If you do not want an economy to be used, set using_economy: false in your Towny config.yml.";
			}
		}
		
		/*
		 * Check add-ons and third-party plugins we use.
		 */
		test = getServer().getPluginManager().getPlugin("TownyChat");
		if (test != null) {
			addons.add(String.format("%s v%s", "TownyChat", test.getDescription().getVersion()));			
		}

		test = getServer().getPluginManager().getPlugin("TownyFlight");
		if (test != null) {
			addons.add(String.format("%s v%s", "TownyFlight", test.getDescription().getVersion()));			
		}
		
		test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null) {
			TownySettings.setUsingEssentials(false);
		} else if (TownySettings.isUsingEssentials()) {
			this.essentials = (Essentials) test;
			addons.add(String.format("%s v%s", "Essentials", test.getDescription().getVersion()));
		}

		test = getServer().getPluginManager().getPlugin("PlaceholderAPI");
		if (test != null) {
            new TownyPlaceholderExpansion(this).register();
            addons.add(String.format("%s v%s", "PlaceholderAPI", test.getDescription().getVersion()));
		}

		//Add our chat handler to TheNewChat via the API.
		if(Bukkit.getPluginManager().isPluginEnabled("TheNewChat")) {
			TNCRegister.initialize();
		}

		/*
		 * Output discovered plugins and warnings.
		 */
		System.out.println("[Towny] Plugins found: " + output);
		if (!addons.isEmpty())
			System.out.println("  Add-ons: " + WordUtils.wrap(StringMgmt.join(addons, ", "), 52, System.lineSeparator() + "           ", true));
		if (!ecowarn.isEmpty())
			System.out.println("[Towny] " + WordUtils.wrap(ecowarn, 55, System.lineSeparator() + "        ", true));

		//Add our chat handler to TheNewChat via the API.
		if(Bukkit.getPluginManager().isPluginEnabled("TheNewChat")) {
			TNCRegister.initialize();
		}

		//Legacy check to see if questioner.jar is still present.
		test = getServer().getPluginManager().getPlugin("Questioner");
		if (test != null) {
			String questioner= "Warning: Questioner.jar present on server, Towny no longer requires Questioner for invites/confirmations. You may safely remove Questioner.jar from your plugins folder.";
			System.out.println("[Towny] " + WordUtils.wrap(questioner, 55, System.lineSeparator() + "        ", true));
		}
	}
	
	private String returnPermissionsProviders() {
		// TownyPerms is always present.
		String output = System.lineSeparator() + "  Permissions: TownyPerms, ";
		
		// Test for GroupManager being present.
		Plugin test = getServer().getPluginManager().getPlugin("GroupManager");
		if (test != null) {
			TownyUniverse.getInstance().setPermissionSource(new GroupManagerSource(this, test));
			output += String.format("%s v%s", "GroupManager", test.getDescription().getVersion());
		// Else test for vault being present.
		} else {
			// Try Vault
			test = getServer().getPluginManager().getPlugin("Vault");
			if (test != null) {
				net.milkbowl.vault.chat.Chat chat = getServer().getServicesManager().load(net.milkbowl.vault.chat.Chat.class);
				if (chat == null) {
					// No Chat implementation
					test = null;
					// Fall back to BukkitPermissions below
				} else {
					TownyUniverse.getInstance().setPermissionSource(new VaultPermSource(this, chat));
					RegisteredServiceProvider<Permission> vaultPermProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
					if (vaultPermProvider != null) {
						output += vaultPermProvider.getPlugin().getName() + " " + vaultPermProvider.getPlugin().getDescription().getVersion() + " via Vault";
					} else {
						output += String.format("%s v%s", "Vault", test.getDescription().getVersion());
					}
				}
			}

			if (test == null) {
				TownyUniverse.getInstance().setPermissionSource(new BukkitPermSource(this));
				output += "BukkitPermissions";
			}
		}
		return output;		
	}
	
	private void cycleTimers() {

		toggleTimersOff();
		TownyTimerHandler.toggleTownyRepeatingTimer(true);
		TownyTimerHandler.toggleDailyTimer(true);
		TownyTimerHandler.toggleHourlyTimer(true);
		TownyTimerHandler.toggleShortTimer(true);
		TownyTimerHandler.toggleMobRemoval(true);
		TownyTimerHandler.toggleHealthRegen(TownySettings.hasHealthRegen());
		TownyTimerHandler.toggleTeleportWarmup(TownySettings.getTeleportWarmupTime() > 0);
		TownyTimerHandler.toggleCooldownTimer(TownySettings.getPVPCoolDownTime() > 0 || TownySettings.getSpawnCooldownTime() > 0);
		TownyTimerHandler.toggleDrawSmokeTask(true);
		if (!TownySettings.getUUIDPercent().equals("100%") && TownySettings.isGatheringResidentUUIDS())
			TownyTimerHandler.toggleGatherResidentUUIDTask(true);
	}
	
	private void toggleTimersOff() {

		TownyTimerHandler.toggleTownyRepeatingTimer(false);
		TownyTimerHandler.toggleDailyTimer(false);
		TownyTimerHandler.toggleHourlyTimer(false);
		TownyTimerHandler.toggleShortTimer(false);
		TownyTimerHandler.toggleMobRemoval(false);
		TownyTimerHandler.toggleHealthRegen(false);
		TownyTimerHandler.toggleTeleportWarmup(false);
		TownyTimerHandler.toggleCooldownTimer(false);
		TownyTimerHandler.toggleDrawSmokeTask(false);
	}

	private void registerEvents() {

		final PluginManager pluginManager = getServer().getPluginManager();

		if (!isError()) {
			// Have War Events get launched before regular events.
			pluginManager.registerEvents(flagWarBlockListener, this);
			pluginManager.registerEvents(flagWarEntityListener, this);
			
			// Huds
			pluginManager.registerEvents(HUDManager, this);

			// Manage player deaths and death payments
			pluginManager.registerEvents(entityMonitorListener, this);
			pluginManager.registerEvents(vehicleListener, this);
			pluginManager.registerEvents(serverListener, this);
			pluginManager.registerEvents(flagWarCustomListener, this);
			pluginManager.registerEvents(customListener, this);
			pluginManager.registerEvents(worldListener, this);
			pluginManager.registerEvents(loginListener, this);
			pluginManager.registerEvents(warzoneListener, this);
		}

		// Always register these events.
		pluginManager.registerEvents(playerListener, this);
		pluginManager.registerEvents(blockListener, this);
		pluginManager.registerEvents(entityListener, this);
		pluginManager.registerEvents(inventoryListener, this);

	}

	private void printChangelogToConsole() {

		try {
			List<String> changeLog = JavaUtil.readTextFromJar("/ChangeLog.txt");
			boolean display = false;
			System.out.println("------------------------------------");
			System.out.println("[Towny] ChangeLog up until v" + getVersion());
			String lastVersion = Version.fromString(TownySettings.getLastRunVersion()).toString(); // Parse out any trailing text after the *.*.*.* version, ie "-for-1.12.2".
			for (String line : changeLog) { // TODO: crawl from the bottom, then
											// past from that index.
				if (line.startsWith(lastVersion)) {
					display = true;
				}
				if (display && line.replaceAll(" ", "").replaceAll("\t", "").length() > 0) {
					System.out.println(line);
				}
			}
			System.out.println("------------------------------------");
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg("Could not read ChangeLog.txt");
		}
	}

	/**
	 * Fetch the TownyUniverse instance.
	 * 
	 * @return TownyUniverse
	 * @deprecated Use {@link com.palmergames.bukkit.towny.TownyUniverse#getInstance()} instead.
	 */
	@Deprecated
	public com.palmergames.bukkit.towny.TownyUniverse getTownyUniverse() {

		return townyUniverse;
	}

	public String getVersion() {

		return version;
	}

	/**
	 * @return the error
	 */
	public boolean isError() {

		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(boolean error) {

		this.error = error;
	}

	// is Essentials active
	public boolean isEssentials() {

		return (TownySettings.isUsingEssentials() && (this.essentials != null));
	}

	// is Citizens2 active
	public boolean isCitizens2() {

		return citizens2;
	}

	/**
	 * @return Essentials object
	 * @throws TownyException - If Towny can't find Essentials.
	 */
	public Essentials getEssentials() throws TownyException {

		if (essentials == null)
			throw new TownyException("Essentials is not installed, or not enabled!");
		else
			return essentials;
	}

	public World getServerWorld(String name) throws NotRegisteredException {
		World world = BukkitTools.getWorld(name);
		
		if (world == null)
			throw new NotRegisteredException(String.format("A world called '$%s' has not been registered.", name));
		
		return world;
	}

	public boolean hasCache(Player player) {

		return playerCache.containsKey(player.getName().toLowerCase());
	}

	public PlayerCache newCache(Player player) {

		try {
			PlayerCache cache = new PlayerCache(TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName()), player);
			playerCache.put(player.getName().toLowerCase(), cache);
			return cache;
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, "Could not create permission cache for this world (" + player.getWorld().getName() + ".");
			return null;
		}

	}

	public void deleteCache(Player player) {

		deleteCache(player.getName());
	}

	public void deleteCache(String name) {

		playerCache.remove(name.toLowerCase());
	}

	/**
	 * Fetch the current players cache
	 * Creates a new one, if one doesn't exist.
	 * 
	 * @param player - Player to get the current cache from.
	 * @return the current (or new) cache for this player.
	 */
	public PlayerCache getCache(Player player) {

		PlayerCache cache = playerCache.get(player.getName().toLowerCase());
		
		if (cache == null) {
			cache = newCache(player);
			
			if (cache != null)
				cache.setLastTownBlock(WorldCoord.parseWorldCoord(player));
		}

		return cache;
	}

	/**
	 * Resets all Online player caches, retaining their location info.
	 */
	public void resetCache() {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				getCache(player).resetAndUpdate(WorldCoord.parseWorldCoord(player)); // Automatically resets permissions.
	}

	/**
	 * Resets all Online player caches if their location equals this one
	 * 
	 * @param worldCoord - the location to check for
	 */
	public void updateCache(WorldCoord worldCoord) {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (Coord.parseCoord(player).equals(worldCoord))
					getCache(player).resetAndUpdate(worldCoord); // Automatically resets permissions.
	}

	/**
	 * Resets all Online player caches if their location has changed
	 */
	public void updateCache() {

		WorldCoord worldCoord = null;

		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				worldCoord = WorldCoord.parseWorldCoord(player);
				PlayerCache cache = getCache(player);
				if (cache.getLastTownBlock() != worldCoord)
					cache.resetAndUpdate(worldCoord);
			}
		}
	}

	/**
	 * Resets a specific players cache if their location has changed
	 * 
	 * @param player - Player, whose cache is to be updated.
	 */
	public void updateCache(Player player) {

		WorldCoord worldCoord = WorldCoord.parseWorldCoord(player);
		PlayerCache cache = getCache(player);

		if (!cache.getLastTownBlock().equals(worldCoord))
			cache.resetAndUpdate(worldCoord);
	}

	/**
	 * Resets a specific players cache
	 * 
	 * @param player - Player, whose cache is to be reset.
	 */
	public void resetCache(Player player) {

		getCache(player).resetAndUpdate(WorldCoord.parseWorldCoord(player));
	}

	public void setPlayerMode(Player player, String[] modes, boolean notify) {

		if (player == null)
			return;

		Resident resident = TownyUniverse.getInstance().getResident(player.getName());
		if (resident != null)
			resident.setModes(modes, notify);
	}

	/**
	 * Remove ALL current modes (and set the defaults)
	 * 
	 * @param player - player, whose modes are to be reset (all removed).
	 */
	public void removePlayerMode(Player player) {

		Resident resident = TownyUniverse.getInstance().getResident(player.getName());
		if (resident != null)
			resident.clearModes();
	}

	/**
	 * Fetch a list of all the players current modes.
	 * 
	 * @param player - player, whose modes are to be listed, taken.
	 * @return list of modes
	 */
	public List<String> getPlayerMode(Player player) {

		return getPlayerMode(player.getName());
	}

	public List<String> getPlayerMode(String name) {

		Resident resident = TownyUniverse.getInstance().getResident(name);
		return resident != null ? resident.getModes() : null;
	}

	/**
	 * Check if the player has a specific mode.
	 * 
	 * @param player - Player to be checked
	 * @param mode - Mode to be checked for within player.
	 * @return true if the mode is present.
	 */
	public boolean hasPlayerMode(Player player, String mode) {

		return hasPlayerMode(player.getName(), mode);
	}

	public boolean hasPlayerMode(String name, String mode) {
		
		Resident resident = TownyUniverse.getInstance().getResident(name);
		return resident != null && resident.hasMode(mode);
	}

	public String getConfigPath() {

		return getDataFolder().getPath() + File.separator + "settings" + File.separator + "config.yml";
	}

	public Object getSetting(String root) {

		return TownySettings.getProperty(root);
	}

	public void log(String msg) {

		if (TownySettings.isLogging()) {
			LOGGER.info(ChatColor.stripColor(msg));
		}
	}

	/**
	 * @return the Towny instance
	 */
	public static Towny getPlugin() {
		return plugin;
	}

	/**
	 * @return the playerListener
	 */
	public TownyPlayerListener getPlayerListener() {
	
		return playerListener;
	}

	
	/**
	 * @return the vehicleListener
	 */
	public TownyVehicleListener getVehicleListener() {
	
		return vehicleListener;
	}

	
	/**
	 * @return the entityListener
	 */
	public TownyEntityListener getEntityListener() {
	
		return entityListener;
	}
	
	/**
	 * @return the entityMonitorListener
	 */
	public TownyEntityMonitorListener getEntityMonitorListener() {
	
		return entityMonitorListener;
	}

	
	/**
	 * @return the worldListener
	 */
	public TownyWorldListener getWorldListener() {
	
		return worldListener;
	}

	
	/**
	 * @return the flagWarBlockListener
	 */
	public FlagWarBlockListener getFlagWarBlockListener() {
	
		return flagWarBlockListener;
	}

	
	/**
	 * @return the flagWarCustomListener
	 */
	public FlagWarCustomListener getFlagWarCustomListener() {
	
		return flagWarCustomListener;
	}

	
	/**
	 * @return the flagWarEntityListener
	 */
	public FlagWarEntityListener getFlagWarEntityListener() {
	
		return flagWarEntityListener;
	}
	
	/**
	 * @return the HUDManager
	 */
	public HUDManager getHUDManager() {
		
		return HUDManager;
	}

	// https://www.spigotmc.org/threads/small-easy-register-command-without-plugin-yml.38036/
	private void registerSpecialCommands() {
		List<Command> commands = new ArrayList<>(4);
		commands.add(new AcceptCommand(TownySettings.getAcceptCommand()));
		commands.add(new DenyCommand(TownySettings.getDenyCommand()));
		commands.add(new ConfirmCommand(TownySettings.getConfirmCommand()));
		commands.add(new CancelCommand(TownySettings.getCancelCommand()));
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			commandMap.registerAll("towny", commands);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private void registerCommands() {
		getCommand("townyadmin").setExecutor(new TownyAdminCommand(this));
		getCommand("townyworld").setExecutor(new TownyWorldCommand(this));
		getCommand("resident").setExecutor(new ResidentCommand(this));
		getCommand("towny").setExecutor(new TownyCommand(this));

		CommandExecutor townCommandExecutor = new TownCommand(this);
		getCommand("town").setExecutor(townCommandExecutor);
		
		// This is needed because the vanilla "/t" tab completer needs to be overridden.
		getCommand("t").setTabCompleter((TabCompleter)townCommandExecutor);
		
		getCommand("nation").setExecutor(new NationCommand(this));
		getCommand("plot").setExecutor(new PlotCommand(this));
		getCommand("invite").setExecutor(new InviteCommand(this));
	}

	private void addMetricsCharts() {
		/*
		 * Register bStats Metrics
		 */
		Metrics metrics = new Metrics(this, 2244);
		
		metrics.addCustomChart(new Metrics.SimplePie("language", () -> TownySettings.getString(ConfigNodes.LANGUAGE)));
		
		metrics.addCustomChart(new Metrics.SimplePie("server_type", () -> {
			if (Bukkit.getServer().getName().equalsIgnoreCase("paper"))
				return "Paper";
			else if (Bukkit.getServer().getName().equalsIgnoreCase("craftbukkit")) {
				if (isSpigot)
					return "Spigot";
				else 
					return "CraftBukkit";
			}
			return "Unknown";
		}));

		metrics.addCustomChart(new Metrics.SimplePie("nation_zones_enabled", () -> TownySettings.getNationZonesEnabled() ? "true" : "false"));
		
		metrics.addCustomChart(new Metrics.SimplePie("database_type", () -> TownySettings.getSaveDatabase().toLowerCase()));
		
		metrics.addCustomChart(new Metrics.SimplePie("town_block_size", () -> String.valueOf(TownySettings.getTownBlockSize())));
		
		metrics.addCustomChart(new Metrics.SimplePie("resident_uuids_stored", () -> TownySettings.getUUIDPercent()));
	}
	
	public static boolean is116Plus() {
		return CUR_BUKKIT_VER.compareTo(NETHER_VER) >= 0;
	}
}
