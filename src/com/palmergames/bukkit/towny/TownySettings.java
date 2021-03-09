package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.event.NationBonusCalculationEvent;
import com.palmergames.bukkit.towny.event.NationUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepPenalityCalculationEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.NationSpawnLevel.NSpawnLevel;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownSpawnLevel.SpawnLevel;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyPermission.PermLevel;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.towny.war.common.WarZoneConfig;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.ItemLists;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class TownySettings {

	// Town Level
	public enum TownLevel {
		NAME_PREFIX, NAME_POSTFIX, MAYOR_PREFIX, MAYOR_POSTFIX, TOWN_BLOCK_LIMIT, UPKEEP_MULTIPLIER, OUTPOST_LIMIT, TOWN_BLOCK_BUY_BONUS_LIMIT, DEBT_CAP_MODIFIER
	}

	// Nation Level
	public enum NationLevel {
		NAME_PREFIX, NAME_POSTFIX, CAPITAL_PREFIX, CAPITAL_POSTFIX, KING_PREFIX, KING_POSTFIX, TOWN_BLOCK_LIMIT_BONUS, UPKEEP_MULTIPLIER, NATION_TOWN_UPKEEP_MULTIPLIER, NATIONZONES_SIZE, NATION_BONUS_OUTPOST_LIMIT
	}

	// private static Pattern namePattern = null;
	private static CommentedConfiguration config, newConfig, playermap;
	private static int uuidCount;

	private static final SortedMap<Integer, Map<TownySettings.TownLevel, Object>> configTownLevel = Collections.synchronizedSortedMap(new TreeMap<Integer, Map<TownySettings.TownLevel, Object>>(Collections.reverseOrder()));
	private static final SortedMap<Integer, Map<TownySettings.NationLevel, Object>> configNationLevel = Collections.synchronizedSortedMap(new TreeMap<Integer, Map<TownySettings.NationLevel, Object>>(Collections.reverseOrder()));
	
	private static final List<String> ItemUseMaterials = new ArrayList<>();
	private static final List<String> SwitchUseMaterials = new ArrayList<>();
	private static final List<Class<?>> protectedMobs = new ArrayList<>();
	
	public static void newTownLevel(int numResidents, String namePrefix, String namePostfix, String mayorPrefix, String mayorPostfix, int townBlockLimit, double townUpkeepMultiplier, int townOutpostLimit, int townBlockBuyBonusLimit, double debtCapModifier) {

		ConcurrentHashMap<TownySettings.TownLevel, Object> m = new ConcurrentHashMap<TownySettings.TownLevel, Object>();
		m.put(TownySettings.TownLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.TownLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.TownLevel.MAYOR_PREFIX, mayorPrefix);
		m.put(TownySettings.TownLevel.MAYOR_POSTFIX, mayorPostfix);
		m.put(TownySettings.TownLevel.TOWN_BLOCK_LIMIT, townBlockLimit);
		m.put(TownySettings.TownLevel.UPKEEP_MULTIPLIER, townUpkeepMultiplier);
		m.put(TownySettings.TownLevel.OUTPOST_LIMIT, townOutpostLimit);
		m.put(TownySettings.TownLevel.TOWN_BLOCK_BUY_BONUS_LIMIT, townBlockBuyBonusLimit);
		m.put(TownySettings.TownLevel.DEBT_CAP_MODIFIER, debtCapModifier);
		configTownLevel.put(numResidents, m);
	}

	public static void newNationLevel(int numResidents, String namePrefix, String namePostfix, String capitalPrefix, String capitalPostfix, String kingPrefix, String kingPostfix, int townBlockLimitBonus, double nationUpkeepMultiplier, double nationTownUpkeepMultiplier, int nationZonesSize, int nationBonusOutpostLimit) {

		ConcurrentHashMap<TownySettings.NationLevel, Object> m = new ConcurrentHashMap<TownySettings.NationLevel, Object>();
		m.put(TownySettings.NationLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.NationLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.NationLevel.CAPITAL_PREFIX, capitalPrefix);
		m.put(TownySettings.NationLevel.CAPITAL_POSTFIX, capitalPostfix);
		m.put(TownySettings.NationLevel.KING_PREFIX, kingPrefix);
		m.put(TownySettings.NationLevel.KING_POSTFIX, kingPostfix);
		m.put(TownySettings.NationLevel.TOWN_BLOCK_LIMIT_BONUS, townBlockLimitBonus);
		m.put(TownySettings.NationLevel.UPKEEP_MULTIPLIER, nationUpkeepMultiplier);
		m.put(TownySettings.NationLevel.NATION_TOWN_UPKEEP_MULTIPLIER, nationTownUpkeepMultiplier);
		m.put(TownySettings.NationLevel.NATIONZONES_SIZE, nationZonesSize);
		m.put(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT, nationBonusOutpostLimit);
		configNationLevel.put(numResidents, m);
	}

	/**
	 * Loads town levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 *
	 * numResidents:namePrefix:namePostfix:mayorPrefix:mayorPostfix:
	 * townBlockLimit
	 *
	 * townBlockLimit is a required field even if using a calculated ratio.
	 *
	 * @throws IOException if unable to load the Town Levels
	 */
	public static void loadTownLevelConfig() throws IOException {

		List<Map<?, ?>> levels = config.getMapList("levels.town_level");
		for (Map<?, ?> level : levels) {

			try {
				/*
				 * We parse everything as if it were a string because of the config-migrator, 
				 * which will always write any double or integer as a string (ex: debtCaptModifier: '2.0')
				 * Until the migrator is revamped to handle different types of primitives, or,
				 * the nation/town levels are changed this might be least painful alternative.
				 */
				newTownLevel(
						Integer.parseInt(level.get("numResidents").toString()),
						String.valueOf(level.get("namePrefix")),
						String.valueOf(level.get("namePostfix")),
						String.valueOf(level.get("mayorPrefix")),
						String.valueOf(level.get("mayorPostfix")),
						Integer.parseInt(level.get("townBlockLimit").toString()),
						Double.parseDouble(level.get("upkeepModifier").toString()),
						Integer.parseInt(level.get("townOutpostLimit").toString()),
						Integer.parseInt(level.get("townBlockBuyBonusLimit").toString()),
						Double.parseDouble(level.get("debtCapModifier").toString())
						);
			} catch (NullPointerException e) {
				System.out.println("Your Towny config.yml's town_level section is out of date.");
				System.out.println("This can be fixed automatically by deleting the town_level section and letting Towny remake it on the next startup.");
				throw new IOException("Config.yml town_levels incomplete.");
			}

		}
	}

	/**
	 * Loads nation levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 *
	 * numResidents:namePrefix:namePostfix:capitalPrefix:capitalPostfix:
	 * kingPrefix:kingPostfix
	 *
	 * @throws IOException if Nation Levels cannot be loaded from config
	 */

	public static void loadNationLevelConfig() throws IOException {

		List<Map<?, ?>> levels = config.getMapList("levels.nation_level");
		for (Map<?, ?> level : levels) {

			try {
				/*
				 * We parse everything as if it were a string because of the config-migrator, 
				 * which will always write any double or integer as a string (ex: debtCaptModifier: '2.0')
				 * Until the migrator is revamped to handle different types of primitives, or,
				 * the nation/town levels are changed this might be least painful alternative.
				 */
				newNationLevel( 
						Integer.parseInt(level.get("numResidents").toString()), 
						String.valueOf(level.get("namePrefix")),
						String.valueOf(level.get("namePostfix")),
						String.valueOf(level.get("capitalPrefix")),
						String.valueOf(level.get("capitalPostfix")),
						String.valueOf(level.get("kingPrefix")),
						String.valueOf(level.get("kingPostfix")),
						Integer.parseInt(level.get("townBlockLimitBonus").toString()),
						Double.parseDouble(level.get("upkeepModifier").toString()),
						Double.parseDouble(level.get("nationTownUpkeepModifier").toString()),
						Integer.parseInt(level.get("nationZonesSize").toString()),
						Integer.parseInt(level.get("nationBonusOutpostLimit").toString())
						);
			} catch (Exception e) {
				System.out.println("Your Towny config.yml's nation_level section is out of date.");
				System.out.println("This can be fixed automatically by deleting the nation_level section and letting Towny remake it on the next startup.");
				throw new IOException("Config.yml nation_levels incomplete.");
			}

		}
	}

	public static Map<TownySettings.TownLevel, Object> getTownLevel(int numResidents) {

		return configTownLevel.get(numResidents);
	}

	public static Map<TownySettings.NationLevel, Object> getNationLevel(int numResidents) {

		return configNationLevel.get(numResidents);
	}

	public static Map<TownySettings.TownLevel, Object> getTownLevel(Town town) {

		return getTownLevel(calcTownLevel(town));
	}

	public static Map<TownySettings.NationLevel, Object> getNationLevel(Nation nation) {

		return getNationLevel(calcNationLevel(nation));
	}

	public static CommentedConfiguration getConfig() {
		return config;
	}

	public static int calcTownLevel(Town town) {
//Creatorfromhell's PR for replacing SortedMap town and nation levels.
//		Integer level = configTownLevel.floorKey(town.getNumResidents());
//
//		if (level != null) return level;
//		return 0;
		if(town.isRuined())
			return 0;
		int n = town.getNumResidents();
		for (Integer level : configTownLevel.keySet())
			if (n >= level)
				return level;
		return 0;
	}

	/**
	 * This method returns the id of the town level
	 *
	 * e.g.
	 * ruins = 0
	 * hamlet = 1
	 * village = 2
	 *
	 * @param town Town to test for.
	 * @return id
	 */
	public static int calcTownLevelId(Town town) {
		if(town.isRuined())
			return 0;

		int townLevelId = -1;
		int numResidents = town.getNumResidents();
		for (Integer level : configTownLevel.keySet()) {
			if (level <= numResidents)
				townLevelId ++;
		}
		return townLevelId;
	}

	public static int calcNationLevel(Nation nation) {
//Creatorfromhell's PR for replacing SortedMap town and nation levels.
//		Integer level = configNationLevel.floorKey(nation.getNumResidents());
//
//		if (level != null) return level;
//		return 0;
		int n = nation.getNumResidents();
		for (Integer level : configNationLevel.keySet())
			if (n >= level)
				return level;
		return 0;
	}

	public static void loadConfig(String filepath, String version) throws IOException {
		if (FileMgmt.checkOrCreateFile(filepath)) {
			File file = new File(filepath);

			// read the config.yml into memory
			config = new CommentedConfiguration(file);
			if (!config.load()) {
				System.out.print("Failed to load Config!");
			}

			setDefaults(version, file);

			config.save();
			
			loadWarMaterialsLists(); // TODO: move this to be with the other war stuff.
			loadSwitchAndItemUseMaterialsLists();
			loadProtectedMobsList();
			ChunkNotification.loadFormatStrings();
		}
	}
	
	private static void loadProtectedMobsList() {
		protectedMobs.clear();
		protectedMobs.addAll(EntityTypeUtil.parseLivingEntityClassNames(getStrArr(ConfigNodes.PROT_MOB_TYPES), "TownMobPVM:"));
	}

	private static void loadSwitchAndItemUseMaterialsLists() {

		SwitchUseMaterials.clear();
		ItemUseMaterials.clear();
		
		/*
		 * Load switches from config value.
		 * Scan over them and replace any grouping with the contents of the group.
		 * Add single item or grouping to SwitchUseMaterials.
		 */
		List<String> switches = getStrArr(ConfigNodes.PROT_SWITCH_MAT);
		for (String matName : switches) {
			if (ItemLists.GROUPS.contains(matName)) {
				List<String> group = ItemLists.getGrouping(matName);
				SwitchUseMaterials.addAll(group);
			} else {
				SwitchUseMaterials.add(matName);
			}
		}

		/*
		 * Load items from config value.
		 * Scan over them and replace any grouping with the contents of the group.
		 * Add single item or grouping to ItemUseMaterials.
		 */
		List<String> items = getStrArr(ConfigNodes.PROT_ITEM_USE_MAT);
		for (String matName : items) {
			if (ItemLists.GROUPS.contains(matName)) {
				List<String> group = ItemLists.getGrouping(matName);
				ItemUseMaterials.addAll(group);
			} else {
				ItemUseMaterials.add(matName);
			}
		}
	}

	public static void loadPlayerMap(String filepath) {
		if (FileMgmt.checkOrCreateFile(filepath)) {
			File file = new File(filepath);
			
			playermap = new CommentedConfiguration(file);
			if (!playermap.load()) {
				System.out.println("Failed to load playermap!");
			}
		}
	}
	
	private static void loadWarMaterialsLists() {
		// Cell War material types.
		FlagWarConfig.setFlagBaseMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_FLAG_BASE_BLOCK)));
		FlagWarConfig.setFlagLightMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_FLAG_LIGHT_BLOCK)));
		FlagWarConfig.setBeaconWireFrameMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_BEACON_WIREFRAME_BLOCK)));

		// Load allowed blocks in warzone.
		WarZoneConfig.setEditableMaterialsInWarZone(getAllowedMaterials(ConfigNodes.WAR_WARZONE_EDITABLE_MATERIALS));
	}

	public static void sendError(String msg) {

		System.out.println("[Towny] Error could not read " + msg);
	}
	
	public static SpawnLevel getSpawnLevel(ConfigNodes node)
	{
		SpawnLevel level = SpawnLevel.valueOf(config.getString(node.getRoot()).toUpperCase());
		if(level == null) {
			level = SpawnLevel.valueOf(node.getDefault().toUpperCase());
		}
		return level;
	}
	
	public static NSpawnLevel getNSpawnLevel(ConfigNodes node)
	{
		NSpawnLevel level = NSpawnLevel.valueOf(config.getString(node.getRoot()).toUpperCase());
		if(level == null) {
			level = NSpawnLevel.valueOf(node.getDefault().toUpperCase());
		}
		return level;
	}

	public static boolean getBoolean(ConfigNodes node) {

		return Boolean.parseBoolean(config.getString(node.getRoot().toLowerCase(), node.getDefault()));
	}

	public static double getDouble(ConfigNodes node) {

		try {
			return Double.parseDouble(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase() + " from config.yml");
			return 0.0;
		}
	}

	public static int getInt(ConfigNodes node) {

		try {
			return Integer.parseInt(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase() + " from config.yml");
			return 0;
		}
	}

	public static String getString(ConfigNodes node) {

		return config.getString(node.getRoot().toLowerCase(), node.getDefault());
	}

	public static String getString(String root, String def) {

		String data = config.getString(root.toLowerCase(), def);
		if (data == null) {
			sendError(root.toLowerCase() + " from config.yml");
			return "";
		}
		return data;
	}

	/**
	 * Gets the lang string from the key.
	 * 
	 * @param root The key for the language string.
	 * @return The translated lang string.
	 * 
	 * @deprecated As of 0.96.2.5+ use {@link Translation#of(String)} instead.
	 */
	@Deprecated
	public static String getLangString(String root) {
		String data = Translation.of(root);

		if (data == null) {
			sendError(root.toLowerCase() + " from " + config.getString("language"));
			return "";
		}
		return StringMgmt.translateHexColors(Colors.translateColorCodes(data));
	}

	public static List<Integer> getIntArr(ConfigNodes node) {

		String[] strArray = getString(node.getRoot(), node.getDefault()).split(",");
		List<Integer> list = new ArrayList<>();
		if (strArray != null) {
			for (String aStrArray : strArray)
				if (aStrArray != null) {
					try {
						list.add(Integer.parseInt(aStrArray.trim()));
					} catch (NumberFormatException e) {
						sendError(node.getRoot().toLowerCase() + " from config.yml");
					}
				}
		}
		return list;
	}

	public static List<String> getStrArr(ConfigNodes node) {

		String[] strArray = getString(node.getRoot().toLowerCase(), node.getDefault()).split(",");
		List<String> list = new ArrayList<>();
		if (strArray != null) {
			for (String aStrArray : strArray)
				if (aStrArray != null)
					list.add(aStrArray.trim());
		}
		return list;
	}

	public static long getSeconds(ConfigNodes node) {

		try {
			return TimeTools.getSeconds(getString(node));
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase() + " from config.yml");
			return 1;
		}
	}

	public static Set<Material> getAllowedMaterials(ConfigNodes node) {

		Set<Material> allowedMaterials = new HashSet<Material>();
		for (String material : getStrArr(node)) {
			if (material.equals("*")) {
				allowedMaterials.addAll(Arrays.asList(Material.values()));
			} else if (material.startsWith("-")) {
				allowedMaterials.remove(Material.matchMaterial(material));
			} else {
				allowedMaterials.add(Material.matchMaterial(material));
			}
		}
		return allowedMaterials;
	}

	public static void addComment(String root, String... comments) {

		newConfig.addComment(root.toLowerCase(), comments);
	}

	/**
	 * Builds a new config reading old config data.
	 */
	private static void setDefaults(String version, File file) {

		newConfig = new CommentedConfiguration(file);
		newConfig.load();

		for (ConfigNodes root : ConfigNodes.values()) {
			if (root.getComments().length > 0)
				addComment(root.getRoot(), root.getComments());

			if (root.getRoot().equals(ConfigNodes.LEVELS.getRoot())) {
				
				setDefaultLevels();
				
			} else if ( (root.getRoot().equals(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()))
				|| (root.getRoot().equals(ConfigNodes.LEVELS_NATION_LEVEL.getRoot())) ){
				
				// Do nothing here as setDefaultLevels configured town and
				// nation levels.
				
			} else if (root.getRoot().equals(ConfigNodes.VERSION.getRoot())) {
				setNewProperty(root.getRoot(), version);
			} else if (root.getRoot().equals(ConfigNodes.LAST_RUN_VERSION.getRoot())) {
				setNewProperty(root.getRoot(), getLastRunVersion(version));
			} else
				setNewProperty(root.getRoot(), (config.get(root.getRoot().toLowerCase()) != null) ? config.get(root.getRoot().toLowerCase()) : root.getDefault());

		}

		config = newConfig;
		newConfig = null;
	}

	private static void setDefaultLevels() {

		addComment(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), "# default Town levels.");
		if (!config.contains(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot())) {
			// List<Map<String, Object>> townLevels =
			// config.getMapList(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot());

			// if (townLevels == null || townLevels.isEmpty() ||
			// townLevels.size() == 0) {
			List<Map<String, Object>> levels = new ArrayList<>();
			Map<String, Object> level = new HashMap<>();
			level.put("numResidents", 0);
			level.put("namePrefix", "");
			level.put("namePostfix", " Ruins");
			level.put("mayorPrefix", "Spirit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 1);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 0);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 1);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Settlement)");
			level.put("mayorPrefix", "Hermit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 16);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 0);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 2);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Hamlet)");
			level.put("mayorPrefix", "Chief ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 32);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 1);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 6);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Village)");
			level.put("mayorPrefix", "Baron Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 96);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 1);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 10);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Town)");
			level.put("mayorPrefix", "Viscount ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 160);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 2);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 14);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large Town)");
			level.put("mayorPrefix", "Count Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 224);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 2);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 20);
			level.put("namePrefix", "");
			level.put("namePostfix", " (City)");
			level.put("mayorPrefix", "Earl ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 320);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 3);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 24);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large City)");
			level.put("mayorPrefix", "Duke ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 384);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 3);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 28);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Metropolis)");
			level.put("mayorPrefix", "Lord ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 448);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 4);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			levels.add(new HashMap<>(level));
			level.clear();
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), levels);
		} else {
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), config.get(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()));
		}

		addComment(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), "# default Nation levels.");

		if (!config.contains(ConfigNodes.LEVELS_NATION_LEVEL.getRoot())) {
			// List<Map<String, Object>> nationLevels =
			// config.getMapList(ConfigNodes.LEVELS_NATION_LEVEL.getRoot());

			// if (nationLevels == null || nationLevels.isEmpty() ||
			// nationLevels.size() == 0) {
			List<Map<String, Object>> levels = new ArrayList<>();
			Map<String, Object> level = new HashMap<>();
			level.put("numResidents", 0);
			level.put("namePrefix", "Land of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Leader ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 10);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 10);
			level.put("namePrefix", "Federation of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Count ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 20);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 1);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 20);
			level.put("namePrefix", "Dominion of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Duke ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 40);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 2);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 30);
			level.put("namePrefix", "Kingdom of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "King ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 60);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			level.put("nationZonesSize", 2);
			level.put("nationBonusOutpostLimit", 3);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 40);
			level.put("namePrefix", "The ");
			level.put("namePostfix", " Empire");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Emperor ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 100);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			level.put("nationZonesSize", 2);
			level.put("nationBonusOutpostLimit", 4);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 60);
			level.put("namePrefix", "The ");
			level.put("namePostfix", " Realm");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "God Emperor ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 140);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			level.put("nationZonesSize", 3);
			level.put("nationBonusOutpostLimit", 5);
			levels.add(new HashMap<>(level));
			level.clear();
			newConfig.set(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), levels);
		} else
			newConfig.set(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), config.get(ConfigNodes.LEVELS_NATION_LEVEL.getRoot()));
	}

	public static String getKingPrefix(Resident resident) {

		try {
			return (String) getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_PREFIX);
		} catch (NotRegisteredException e) {
			sendError("getKingPrefix.");
			return "";
		}
	}

	public static String getMayorPrefix(Resident resident) {

		try {
			return (String) getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_PREFIX);
		} catch (NotRegisteredException e) {
			sendError("getMayorPrefix.");
			return "";
		}
	}

	public static String getCapitalPostfix(Town town) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_POSTFIX));
		} catch (NotRegisteredException e) {
			sendError("getCapitalPostfix.");
			return "";
		}
	}

	public static String getTownPostfix(Town town) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getTownLevel(town).get(TownySettings.TownLevel.NAME_POSTFIX));
		} catch (Exception e) {
			sendError("getTownPostfix.");
			return "";
		}
	}

	public static String getNationPostfix(Nation nation) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(nation).get(TownySettings.NationLevel.NAME_POSTFIX));
		} catch (Exception e) {
			sendError("getNationPostfix.");
			return "";
		}
	}

	public static String getNationPrefix(Nation nation) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(nation).get(TownySettings.NationLevel.NAME_PREFIX));
		} catch (Exception e) {
			sendError("getNationPrefix.");
			return "";
		}
	}

	public static String getTownPrefix(Town town) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getTownLevel(town).get(TownySettings.TownLevel.NAME_PREFIX));
		} catch (Exception e) {
			sendError("getTownPrefix.");
			return "";
		}
	}

	public static String getCapitalPrefix(Town town) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_PREFIX));
		} catch (NotRegisteredException e) {
			sendError("getCapitalPrefix.");
			return "";
		}
	}

	public static String getKingPostfix(Resident resident) {

		try {
			return (String) getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_POSTFIX);
		} catch (NotRegisteredException e) {
			sendError("getKingPostfix.");
			return "";
		}
	}

	public static String getMayorPostfix(Resident resident) {

		try {
			return (String) getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_POSTFIX);
		} catch (NotRegisteredException e) {
			sendError("getMayorPostfix.");
			return "";
		}
	}

	public static String getNPCPrefix() {

		return getString(ConfigNodes.FILTERS_NPC_PREFIX.getRoot(), ConfigNodes.FILTERS_NPC_PREFIX.getDefault());
	}

	public static long getInactiveAfter() {

		return getSeconds(ConfigNodes.RES_SETTING_INACTIVE_AFTER_TIME);
	}

	public static boolean getBedUse() {

		return getBoolean(ConfigNodes.RES_SETTING_DENY_BED_USE);
	}

	public static String getLoadDatabase() {

		return getString(ConfigNodes.PLUGIN_DATABASE_LOAD);
	}

	public static String getSaveDatabase() {

		return getString(ConfigNodes.PLUGIN_DATABASE_SAVE);
	}

	public static boolean isGatheringResidentUUIDS() {
		
		return getBoolean(ConfigNodes.PLUGIN_DATABASE_GATHER_RESIDENT_UUIDS);
	}

	// SQL
	public static String getSQLHostName() {

		return getString(ConfigNodes.PLUGIN_DATABASE_HOSTNAME);
	}

	public static String getSQLPort() {

		return getString(ConfigNodes.PLUGIN_DATABASE_PORT);
	}

	public static String getSQLDBName() {

		return getString(ConfigNodes.PLUGIN_DATABASE_DBNAME);
	}

	public static String getSQLTablePrefix() {

		return getString(ConfigNodes.PLUGIN_DATABASE_TABLEPREFIX);
	}

	public static String getSQLUsername() {

		return getString(ConfigNodes.PLUGIN_DATABASE_USERNAME);
	}

	public static String getSQLPassword() {

		return getString(ConfigNodes.PLUGIN_DATABASE_PASSWORD);
	}
	
	public static String getSQLFlags() {
		
		return getString(ConfigNodes.PLUGIN_DATABASE_FLAGS);
	}

	public static int getMaxPoolSize() {
		return getInt(ConfigNodes.PLUGIN_DATABASE_POOLING_MAX_POOL_SIZE);
	}

	public static int getMaxLifetime() {
		return getInt(ConfigNodes.PLUGIN_DATABASE_POOLING_MAX_LIFETIME);
	}

	public static int getConnectionTimeout() {
		return getInt(ConfigNodes.PLUGIN_DATABASE_POOLING_CONNECTION_TIMEOUT);
	}

	public static int getMaxTownBlocks(Town town) {

		int ratio = getTownBlockRatio();
		int n = town.getBonusBlocks() + town.getPurchasedBlocks();

		if (ratio == 0) {
			n += (Integer) getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_LIMIT);

		} else
			n += town.getNumResidents() * ratio;

		n += getNationBonusBlocks(town);

		return n;
	}
	
	public static int getMaxOutposts(Town town) {
		
		int townOutposts = (Integer) getTownLevel(town).get(TownySettings.TownLevel.OUTPOST_LIMIT);
		int nationOutposts = 0;
		if (town.hasNation())
			try {
				nationOutposts = (Integer) getNationLevel(town.getNation()).get(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT);
			} catch (NotRegisteredException e) {
			}
		int n = townOutposts + nationOutposts;
		return n;
	}
	
	public static int getMaxBonusBlocks(Town town) {
		
		return (Integer) getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_BUY_BONUS_LIMIT);
	}

	public static int getNationBonusBlocks(Nation nation) {
		int bonusBlocks = (Integer) getNationLevel(nation).get(TownySettings.NationLevel.TOWN_BLOCK_LIMIT_BONUS);
		NationBonusCalculationEvent calculationEvent = new NationBonusCalculationEvent(nation, bonusBlocks);
		Bukkit.getPluginManager().callEvent(calculationEvent);
		return calculationEvent.getBonusBlocks();
	}

	public static int getNationBonusBlocks(Town town) {

		if (town.hasNation())
			try {
				return getNationBonusBlocks(town.getNation());
			} catch (NotRegisteredException e) {
			}

		return 0;
	}

	public static int getTownBlockRatio() {

		return getInt(ConfigNodes.TOWN_TOWN_BLOCK_RATIO);
	}

	public static int getTownBlockSize() {

		return getInt(ConfigNodes.TOWN_TOWN_BLOCK_SIZE);
	}

	public static boolean isFriendlyFireEnabled() {

		return getBoolean(ConfigNodes.NWS_FRIENDLY_FIRE_ENABLED);
	}
	
	public static boolean isUsingEconomy() {

		return getBoolean(ConfigNodes.PLUGIN_USING_ECONOMY);
	}

	public static boolean isFakeResident(String name) {

		return StringMgmt.containsIgnoreCase(getStrArr(ConfigNodes.PLUGIN_MODS_FAKE_RESIDENTS), name);
	}

	public static boolean isUsingEssentials() {

		return getBoolean(ConfigNodes.PLUGIN_USING_ESSENTIALS);
	}

	public static void setUsingEssentials(boolean newSetting) {

		setProperty(ConfigNodes.PLUGIN_USING_ESSENTIALS.getRoot(), newSetting);
	}

	public static double getNewTownPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_NEW_TOWN);
	}

	public static double getNewNationPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_NEW_NATION);
	}

	public static boolean getUnclaimedZoneBuildRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_BUILD);
	}

	public static boolean getUnclaimedZoneDestroyRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_DESTROY);
	}

	public static boolean getUnclaimedZoneItemUseRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_ITEM_USE);
	}

	public static boolean getDebug() {

		return getBoolean(ConfigNodes.PLUGIN_DEBUG_MODE);
	}
	
	public static String getTool() {

		return getString(ConfigNodes.PLUGIN_INFO_TOOL);
	}

	public static void setDebug(boolean b) {

		setProperty(ConfigNodes.PLUGIN_DEBUG_MODE.getRoot(), b);
	}

	public static boolean getShowTownNotifications() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_SHOW_TOWN_NOTIFICATIONS);
	}
	
	public static boolean isNotificationOwnerShowingNationTitles() {
		
		return getBoolean(ConfigNodes.NOTIFICATION_OWNER_SHOWS_NATION_TITLE);
	}
	
	public static boolean isNotificationsAppearingInActionBar() {
		return getBoolean(ConfigNodes.NOTIFICATION_NOTIFICATIONS_APPEAR_IN_ACTION_BAR);
	}

	public static boolean getShowTownBoardOnLogin() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DISPLAY_TOWNBOARD_ONLOGIN);
	}
	
	public static boolean getShowNationBoardOnLogin() {
		
		return getBoolean(ConfigNodes.GNATION_SETTINGS_DISPLAY_NATIONBOARD_ONLOGIN);
	}

	public static String getUnclaimedZoneName() {

		return Translation.of("UNCLAIMED_ZONE_NAME");
	}

	public static int getMaxTitleLength() {

		return getInt(ConfigNodes.FILTERS_MODIFY_CHAT_MAX_LGTH);
	}

	public static int getMaxNameLength() {

		return getInt(ConfigNodes.FILTERS_MAX_NAME_LGTH);
	}

	public static long getDeleteTime() {

		return getSeconds(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_TIME);
	}

	public static boolean isDeleteEcoAccount() {

		return getBoolean(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_ECO);
	}

	public static boolean isDeleteTownlessOnly() {
		
		return getBoolean(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_TOWNLESS_ONLY);
	}
	
	public static boolean isDeletingOldResidents() {

		return getBoolean(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_ENABLE);
	}

	public static int getWarTimeWarningDelay() {

		return getInt(ConfigNodes.WAR_EVENT_WARNING_DELAY);
	}

	public static boolean isWarTimeTownsNeutral() {

		return getBoolean(ConfigNodes.WAR_EVENT_TOWNS_NEUTRAL);
	}

	public static boolean isAllowWarBlockGriefing() {

		return getBoolean(ConfigNodes.WAR_EVENT_BLOCK_GRIEFING);
	}

	public static int getWarzoneTownBlockHealth() {

		return getInt(ConfigNodes.WAR_EVENT_TOWN_BLOCK_HP);
	}

	public static int getWarzoneHomeBlockHealth() {

		return getInt(ConfigNodes.WAR_EVENT_HOME_BLOCK_HP);
	}

	public static String getDefaultTownName() {

		return getString(ConfigNodes.RES_SETTING_DEFAULT_TOWN_NAME);
	}

	public static int getWarPointsForTownBlock() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_TOWNBLOCK);
	}

	public static int getWarPointsForTown() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_TOWN);
	}

	public static int getWarPointsForNation() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_NATION);
	}

	public static int getWarPointsForKill() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_KILL);
	}

	public static int getMinWarHeight() {

		return getInt(ConfigNodes.WAR_EVENT_MIN_HEIGHT);
	}

	public static List<String> getWorldMobRemovalEntities() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading World Mob removal entities. ");
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_WORLD);
	}

	public static List<String> getWildernessMobRemovalEntities() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading World Mob removal entities. ");
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_WILDERNESS);
	}

	public static List<String> getTownMobRemovalEntities() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading Town Mob removal entities. ");
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_TOWN);
	}

	public static boolean isEconomyAsync() {

		return getBoolean(ConfigNodes.ECO_USE_ASYNC);
	}
	
	public static long getCachedBankTimeout() {
		return getSeconds(ConfigNodes.ECO_BANK_CACHE_TIMEOUT) * 1000;
	}

	public static boolean isRemovingVillagerBabiesWorld() {

		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_VILLAGER_BABIES_WORLD);
	}

	public static boolean isCreatureTriggeringPressurePlateDisabled() {

		return getBoolean(ConfigNodes.PROT_MOB_DISABLE_TRIGGER_PRESSURE_PLATE_STONE);
	}

	public static boolean isRemovingVillagerBabiesTown() {

		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_VILLAGER_BABIES_TOWN);
	}

	public static List<String> getWildExplosionProtectionEntities() {

		if (getDebug())
			System.out.println("[Towny] Debug: Wilderness explosion protection entities. ");
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_ENTITY_REVERT_LIST);
	}
	
	public static List<String> getWildExplosionProtectionBlocks() {

		if (getDebug())
			System.out.println("[Towny] Debug: Wilderness explosion protection blocks. ");
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_BLOCK_REVERT_LIST);
	}

	public static long getMobRemovalSpeed() {

		return getSeconds(ConfigNodes.PROT_MOB_REMOVE_SPEED);
	}

	public static long getHealthRegenSpeed() {

		return getSeconds(ConfigNodes.GTOWN_SETTINGS_REGEN_SPEED);
	}

	public static boolean hasHealthRegen() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_REGEN_ENABLE);
	}

	public static boolean getTownDefaultPublic() {

		return getBoolean(ConfigNodes.TOWN_DEF_PUBLIC);
	}

	public static boolean getTownDefaultOpen() {

		return getBoolean(ConfigNodes.TOWN_DEF_OPEN);
	}
	
	public static boolean getTownDefaultNeutral() {

		return getBoolean(ConfigNodes.TOWN_DEF_NEUTRAL); 
	}

	public static String getTownDefaultBoard() {

		return getString(ConfigNodes.TOWN_DEF_BOARD);
	}

	public static boolean getNationDefaultOpen() {

		return getBoolean(ConfigNodes.NATION_DEF_OPEN);
	}

	public static double getTownDefaultTax() {

		return getDouble(ConfigNodes.TOWN_DEF_TAXES_TAX);
	}

	public static double getTownDefaultShopTax() {

		return getDouble(ConfigNodes.TOWN_DEF_TAXES_SHOP_TAX);
	}
	
	public static double getTownDefaultEmbassyTax() {

		return getDouble(ConfigNodes.TOWN_DEF_TAXES_EMBASSY_TAX);
	}
	
	public static double getTownDefaultPlotTax() {
		
		return getDouble(ConfigNodes.TOWN_DEF_TAXES_PLOT_TAX);
	}

	public static boolean getTownDefaultTaxPercentage() {

		return getBoolean(ConfigNodes.TOWN_DEF_TAXES_TAXPERCENTAGE);
	}
	
	public static double getTownDefaultTaxMinimumTax() {
		
		return getDouble(ConfigNodes.TOWN_DEF_TAXES_MINIMUMTAX);
	}
	
	public static boolean hasTownLimit() {

		return getTownLimit() != 0;
	}

	public static int getTownLimit() {

		return getInt(ConfigNodes.TOWN_LIMIT);
	}

	public static int getMaxPurchasedBlocks(Town town) {

		if (isBonusBlocksPerTownLevel())
			return getMaxBonusBlocks(town);
		else
			return getInt(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS);
	}
	
	/**
	 * @deprecated as of 0.96.6.0, use {@link #getMaxPurchasedBlocks(Town town)}
	 * @param town Town to get the maximum number of blocks they can buy.
	 * @return {@link #getMaxPurchasedBlocks(Town)}
	 */
	@Deprecated
	public static int getMaxPurchedBlocks(Town town) {
		
		return getMaxPurchasedBlocks(town);
	}
	
	public static int getMaxPurchasedBlocksNode() {
		
			return getInt(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS);
	}
	
	public static int getMaxClaimRadiusValue() {
		
		return getInt(ConfigNodes.TOWN_MAX_CLAIM_RADIUS_VALUE);
	}

	public static boolean isSellingBonusBlocks(Town town) {

		return getMaxPurchasedBlocks(town) != 0;
	}
	
	public static boolean isBonusBlocksPerTownLevel() { 
		
		return getBoolean(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS_USES_TOWN_LEVELS);
	}

	public static double getPurchasedBonusBlocksCost() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK);
	}

	public static double getPurchasedBonusBlocksIncreaseValue() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE);
	}
	
	public static double getPurchasedBonusBlocksMaxPrice() {
		
		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCKS_MAXIMUM);
	}
	
	public static boolean isTownSpawnPaidToTown() {
		
		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_SPAWN_PAID_TO_TOWN);
	}

	public static double getNationNeutralityCost() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_NEUTRALITY);
	}

	public static boolean isAllowingOutposts() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTPOSTS);
	}
	
	public static boolean isOutpostsLimitedByLevels() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_LIMIT_OUTPOST_USING_LEVELS);
	}
	
	public static boolean isOutpostLimitStoppingTeleports() {
		
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_OVER_OUTPOST_LIMIT_STOP_TELEPORT);
	}

	public static double getOutpostCost() {

		return getDouble(ConfigNodes.ECO_PRICE_OUTPOST);
	}

	public static List<String> getSwitchMaterials() {

		return SwitchUseMaterials;
	}
	
	public static List<String> getItemUseMaterials() {

		return ItemUseMaterials;
	}
	
	public static boolean isSwitchMaterial(String mat) {

		return SwitchUseMaterials.contains(mat);
	}

	public static boolean isItemUseMaterial(String mat) {

		return ItemUseMaterials.contains(mat);
	}
	
	public static List<String> getFireSpreadBypassMaterials() {
		
		return getStrArr(ConfigNodes.PROT_FIRE_SPREAD_BYPASS);
	}
	
	public static boolean isFireSpreadBypassMaterial(String mat) {
		
		return getFireSpreadBypassMaterials().contains(mat);
	}
	
	public static List<String> getUnclaimedZoneIgnoreMaterials() {

		return getStrArr(ConfigNodes.UNCLAIMED_ZONE_IGNORE);
	}
	
	public static List<Class<?>> getProtectedEntityTypes() {
		return protectedMobs;
	}
	
	public static List<String> getPotionTypes() {

		return getStrArr(ConfigNodes.PROT_POTION_TYPES);
	}

	private static void setProperty(String root, Object value) {

		config.set(root.toLowerCase(), value.toString());
	}

	private static void setNewProperty(String root, Object value) {

		if (value == null) {
			// System.out.print("value is null for " + root.toLowerCase());
			value = "";
		}
		newConfig.set(root.toLowerCase(), value.toString());
	}

	public static Object getProperty(String root) {

		return config.get(root.toLowerCase());
	}

	public static double getClaimPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK);
	}
	
	public static double getClaimPriceIncreaseValue() {
		
		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK_INCREASE);
	}
	
	public static double getMaxClaimPrice() {
		
		return getDouble(ConfigNodes.ECO_MAX_PRICE_CLAIM_TOWNBLOCK);
	}
	
	public static double getClaimRefundPrice() {
		
		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK_REFUND);
	}

	public static boolean getUnclaimedZoneSwitchRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_SWITCH);
	}

	public static boolean getEndermanProtect() {

		return getBoolean(ConfigNodes.NWS_WORLD_ENDERMAN);
	}

	public static String getUnclaimedPlotName() {

		return Translation.of("UNCLAIMED_PLOT_NAME");
	}

	public static long getDayInterval() {

		// return TimeTools.secondsFromDhms("24h");
		return getSeconds(ConfigNodes.PLUGIN_DAY_INTERVAL);
	}

	public static long getNewDayTime() {

		long time = getSeconds(ConfigNodes.PLUGIN_NEWDAY_TIME);
		long day = getDayInterval();
		if (time > day) {
			setProperty(ConfigNodes.PLUGIN_NEWDAY_TIME.getRoot(), day);
			return day;
		}
		return time;
	}
	
	public static boolean isNewDayDeleting0PlotTowns() {
		return getBoolean(ConfigNodes.PLUGIN_NEWDAY_DELETE_0_PLOT_TOWNS);
	}

	public static long getHourInterval() {
		return getSeconds(ConfigNodes.PLUGIN_HOUR_INTERVAL);
	}

	public static long getShortInterval() {
		return getSeconds(ConfigNodes.PLUGIN_SHORT_INTERVAL);
	}

	public static long getNewHourTime() {
		long time = getSeconds(ConfigNodes.PLUGIN_NEWHOUR_TIME);
		long hour = getHourInterval();
		if (time > hour) {
			setProperty(ConfigNodes.PLUGIN_NEWHOUR_TIME.getRoot(), hour);
			return hour;
		}
		return time;
	}

	public static SpawnLevel isAllowingTownSpawn() {

		return getSpawnLevel(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN);
	}

	public static SpawnLevel isAllowingPublicTownSpawnTravel() {

		return getSpawnLevel(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL);
	}
	
	public static NSpawnLevel isAllowingNationSpawn() {

		return getNSpawnLevel(ConfigNodes.GNATION_SETTINGS_ALLOW_NATION_SPAWN);
	}

	public static NSpawnLevel isAllowingPublicNationSpawnTravel() {

		return getNSpawnLevel(ConfigNodes.GNATION_SETTINGS_ALLOW_NATION_SPAWN_TRAVEL);
	}

	public static List<String> getDisallowedTownSpawnZones() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading disallowed town spawn zones. ");
		return getStrArr(ConfigNodes.GTOWN_SETTINGS_PREVENT_TOWN_SPAWN_IN);
	}
	
	public static boolean isSpawnWarnConfirmationUsed() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_SPAWN_WARNINGS);
	}

	public static boolean isTaxingDaily() {

		return getBoolean(ConfigNodes.ECO_DAILY_TAXES_ENABLED);
	}

	public static double getMaxPlotTax() {
		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_PLOT_TAX);
	}

	public static double getMaxTownTax() {
		return getDouble(ConfigNodes.ECO_DAILY_TOWN_TAXES_MAX);
	}

	public static double getMaxNationTax() {
		return getDouble(ConfigNodes.ECO_DAILY_NATION_TAXES_MAX);
	}
	
	public static double getMaxPlotPrice() {
		
		return getDouble(ConfigNodes.GTOWN_MAX_PLOT_PRICE_COST);
	}
	
	public static double getMaxTownTaxPercent() {
		
		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_TOWN_TAX_PERCENT);
	}
	
	public static double getMaxTownTaxPercentAmount() { 
		
		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_TOWN_TAX_PERCENT_AMOUNT); 
	}
	
	public static boolean isBackingUpDaily() {

		return getBoolean(ConfigNodes.PLUGIN_DAILY_BACKUPS);
	}

	public static double getBaseSpoilsOfWar() {

		return getDouble(ConfigNodes.WAR_EVENT_BASE_SPOILS);
	}
	
	public static boolean getOnlyAttackEdgesInWar() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_ENEMY_ONLY_ATTACK_BORDER);
	}
	
	public static boolean getPlotsHealableInWar() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_PLOTS_HEALABLE);
	}
	
	public static boolean getPlotsFireworkOnAttacked() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_PLOTS_FIREWORK_ON_ATTACKED);
	}

	public static double getWartimeDeathPrice() {

		return getDouble(ConfigNodes.WAR_EVENT_PRICE_DEATH);
	}
	
	public static boolean getWarEventCostsTownblocks() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_COSTS_TOWNBLOCKS);
	}

	public static boolean getWarEventWinnerTakesOwnershipOfTownblocks() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWNBLOCKS);
	}
	
	public static boolean getWarEventWinnerTakesOwnershipOfTown() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWN);
	}
	
	public static boolean getWarEventWinnerTakesOwnershipOfTownsExcludesCapitals() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWNS_EXCLUDES_CAPITALS);
	}
	
	public static int getWarEventConquerTime() {
		
		return getInt(ConfigNodes.WAR_EVENT_CONQUER_TIME);
	}
	
	public static boolean isChargingDeath() {
		
		return (getDeathPrice()>0 || getDeathPriceTown()>0 || getDeathPriceNation()>0 );
	}
	
	public static boolean isDeathPriceType() {

		return getString(ConfigNodes.ECO_PRICE_DEATH_TYPE).equalsIgnoreCase("fixed");
	}
	
	public static double getDeathPricePercentageCap() {
		
		return getDouble(ConfigNodes.ECO_PRICE_DEATH_PERCENTAGE_CAP);
	}
	
	public static boolean isDeathPricePercentageCapped() {
		
		return (getDeathPricePercentageCap()>0);
	}

	public static boolean isDeathPricePVPOnly() {

		return getBoolean(ConfigNodes.ECO_PRICE_DEATH_PVP_ONLY);
		
	}
	
	public static double getDeathPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_DEATH);
	}

	public static double getDeathPriceTown() {

		return getDouble(ConfigNodes.ECO_PRICE_DEATH_TOWN);
	}

	public static double getDeathPriceNation() {

		return getDouble(ConfigNodes.ECO_PRICE_DEATH_NATION);
	}
	
	public static boolean isEcoClosedEconomyEnabled() {
		
		return getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED);
	}
	
	public static boolean isJailingAttackingEnemies() {
		
		return getBoolean(ConfigNodes.JAIL_IS_JAILING_ATTACKING_ENEMIES);
	}
	
	public static boolean isJailingAttackingOutlaws() {
		
		return getBoolean(ConfigNodes.JAIL_IS_JAILING_ATTACKING_OUTLAWS);
	}
	
	public static int getJailedOutlawJailDays() {
		
		return getInt(ConfigNodes.JAIL_OUTLAW_JAIL_DAYS);
	}

	public static boolean JailAllowsEnderPearls() {
		
		return getBoolean(ConfigNodes.JAIL_JAIL_ALLOWS_ENDER_PEARLS);
	}
	
	public static boolean JailDeniesTownLeave() {
		
		return getBoolean(ConfigNodes.JAIL_JAIL_DENIES_TOWN_LEAVE);
	}

	public static boolean isAllowingBail() {
		
		return getBoolean(ConfigNodes.JAIL_BAIL_IS_ALLOWING_BAIL);
	}
	
	public static double getBailAmount() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT);
	}
	
	public static double getBailAmountMayor() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT_MAYOR);
	}
	
	public static double getBailAmountKing() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT_KING);
	}

	public static double getWartimeTownBlockLossPrice() {

		return getDouble(ConfigNodes.WAR_EVENT_TOWN_BLOCK_LOSS_PRICE);
	}

	public static boolean isDevMode() {

		return getBoolean(ConfigNodes.PLUGIN_DEV_MODE_ENABLE);
	}

	public static void setDevMode(boolean choice) {

		setProperty(ConfigNodes.PLUGIN_DEV_MODE_ENABLE.getRoot(), choice);
	}

	public static String getDevName() {

		return getString(ConfigNodes.PLUGIN_DEV_MODE_DEV_NAME);
	}

	public static boolean isDeclaringNeutral() {

		return getBoolean(ConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL);
	}

	public static void setDeclaringNeutral(boolean choice) {

		setProperty(ConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL.getRoot(), choice);
	}

	public static boolean isRemovingOnMonarchDeath() {

		return getBoolean(ConfigNodes.WAR_EVENT_REMOVE_ON_MONARCH_DEATH);
	}

	public static double getTownUpkeepCost(Town town) {
		TownUpkeepCalculationEvent event = new TownUpkeepCalculationEvent(town,getTownUpkeepCostRaw(town));
		Bukkit.getPluginManager().callEvent(event);
		return event.getUpkeep();
	}

	private static double getTownUpkeepCostRaw(Town town) {
		double multiplier = 1.0;

		if (town != null) {
			if (isUpkeepByPlot()) {
				multiplier = town.getTownBlocks().size();
			} else {
				multiplier = Double.parseDouble(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString());
			}
		}
		
		if (town.hasNation()) {
			double nationMultiplier = 1.0;
			try {
				nationMultiplier = Double.parseDouble(getNationLevel(town.getNation()).get(TownySettings.NationLevel.NATION_TOWN_UPKEEP_MULTIPLIER).toString());
			} catch (NumberFormatException|NotRegisteredException e) {
				e.printStackTrace();
			}
			if (isUpkeepByPlot()) {
				double amount;
				if (isTownLevelModifiersAffectingPlotBasedUpkeep())
					amount = (((getTownUpkeep() * multiplier) * Double.parseDouble(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString())) * nationMultiplier);
				else
					amount = (getTownUpkeep() * multiplier) * nationMultiplier;
				if (TownySettings.getPlotBasedUpkeepMinimumAmount() > 0.0 && amount < TownySettings.getPlotBasedUpkeepMinimumAmount())
					amount = TownySettings.getPlotBasedUpkeepMinimumAmount();
				return amount;
			}
			return (getTownUpkeep() * multiplier) * nationMultiplier;
		} else {
			if (isUpkeepByPlot()) {
				double amount;
				if (isTownLevelModifiersAffectingPlotBasedUpkeep())
					amount = (getTownUpkeep() * multiplier) * Double.parseDouble(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString());
				else
					amount = getTownUpkeep() * multiplier;
				if (TownySettings.getPlotBasedUpkeepMinimumAmount() > 0.0 && amount < TownySettings.getPlotBasedUpkeepMinimumAmount())
					amount = TownySettings.getPlotBasedUpkeepMinimumAmount();
				return amount;
			}
			return getTownUpkeep() * multiplier;
		}
	}

	public static double getTownUpkeep() {

		return getDouble(ConfigNodes.ECO_PRICE_TOWN_UPKEEP);
	}

	public static boolean isUpkeepByPlot() {

		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED);
	}
	
	public static double getPlotBasedUpkeepMinimumAmount () {
		
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED_MINIMUM_AMOUNT);
		
	}
	public static boolean isTownLevelModifiersAffectingPlotBasedUpkeep() {
		
		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED_TOWNLEVEL_MODIFIER);
	
	}

	public static boolean isUpkeepPayingPlots() {

		return getBoolean(ConfigNodes.ECO_UPKEEP_PLOTPAYMENTS);
	}

	public static double getTownPenaltyUpkeepCost(Town town) {
		TownUpkeepPenalityCalculationEvent event = new TownUpkeepPenalityCalculationEvent(town, getTownPenaltyUpkeepCostRaw(town));
		Bukkit.getPluginManager().callEvent(event);
		return event.getUpkeep();
	}

	private static double getTownPenaltyUpkeepCostRaw(Town town) {

		if (getUpkeepPenalty() > 0) {
			
			int overClaimed = town.getTownBlocks().size() - getMaxTownBlocks(town);
			
			if (!town.isOverClaimed())
				return 0;
			if (isUpkeepPenaltyByPlot())
				return getUpkeepPenalty() * overClaimed;
			return getUpkeepPenalty();
		}
		return 0;
	}

    public static double getUpkeepPenalty() {
    	
    	return getDouble(ConfigNodes.ECO_PRICE_TOWN_OVERCLAIMED_UPKEEP_PENALTY);
    }
    public static boolean isUpkeepPenaltyByPlot() {
    	
    	return getBoolean(ConfigNodes.ECO_PRICE_TOWN_OVERCLAIMED_UPKEEP_PENALTY_PLOTBASED);
    }

	public static double getNationUpkeep() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_UPKEEP);
	}

	public static double getNationUpkeepCost(Nation nation) {
		NationUpkeepCalculationEvent event = new NationUpkeepCalculationEvent(nation, getNationUpkeepCostRaw(nation));
		Bukkit.getPluginManager().callEvent(event);
		return event.getUpkeep();
	}

	private static double getNationUpkeepCostRaw(Nation nation) {

		double multiplier = 1.0;

		if (nation != null) {
			if (isNationUpkeepPerTown()) {
				if (isNationLevelModifierAffectingNationUpkeepPerTown())
					return (getNationUpkeep() * nation.getTowns().size()) * Double.parseDouble(getNationLevel(nation).get(TownySettings.NationLevel.UPKEEP_MULTIPLIER).toString());
				else
					return (getNationUpkeep() * nation.getTowns().size());
			} else {
				multiplier = Double.parseDouble(getNationLevel(nation).get(TownySettings.NationLevel.UPKEEP_MULTIPLIER).toString());
			}
		}
		return getNationUpkeep() * multiplier;
	}

	private static boolean isNationLevelModifierAffectingNationUpkeepPerTown() {

		return getBoolean(ConfigNodes.ECO_PRICE_NATION_UPKEEP_PERTOWN_NATIONLEVEL_MODIFIER);
	}

	private static boolean isNationUpkeepPerTown() {

		return getBoolean(ConfigNodes.ECO_PRICE_NATION_UPKEEP_PERTOWN);
	}

	public static boolean getNationDefaultPublic(){

		return getBoolean(ConfigNodes.NATION_DEF_PUBLIC);
	}

	public static String getNationDefaultBoard(){

		return getString(ConfigNodes.NATION_DEF_BOARD);
	}

	public static String getFlatFileBackupType() {

		return getString(ConfigNodes.PLUGIN_FLATFILE_BACKUP);
	}

	public static long getBackupLifeLength() {

		long t = TimeTools.getMillis(TownySettings.getString(ConfigNodes.PLUGIN_BACKUPS_ARE_DELETED_AFTER));
		long minT = TimeTools.getMillis("1d");
		if (t >= 0 && t < minT)
			t = minT;
		return t;
	}

	public static boolean isUsingTowny() {

		return getBoolean(ConfigNodes.NWS_WORLD_USING_TOWNY);
	}

	public static boolean isPvP() {

		return getBoolean(ConfigNodes.NWS_WORLD_PVP);
	}

	public static boolean isForcingPvP() {

		return getBoolean(ConfigNodes.NWS_FORCE_PVP_ON);
	}

	public static boolean isPlayerTramplingCropsDisabled() {

		return getBoolean(ConfigNodes.NWS_DISABLE_PLAYER_CROP_TRAMPLING);
	}

	public static boolean isCreatureTramplingCropsDisabled() {

		return getBoolean(ConfigNodes.NWS_DISABLE_CREATURE_CROP_TRAMPLING);
	}

	public static boolean isWorldMonstersOn() {

		return getBoolean(ConfigNodes.NWS_WORLD_MONSTERS_ON);
	}
	
	public static boolean isWildernessMonstersOn() {
		return getBoolean(ConfigNodes.NWS_WILDERNESS_MONSTERS_ON);
	}

	public static boolean isExplosions() {

		return getBoolean(ConfigNodes.NWS_WORLD_EXPLOSION);
	}

	public static boolean isForcingExplosions() {

		return getBoolean(ConfigNodes.NWS_FORCE_EXPLOSIONS_ON);
	}

	public static boolean isForcingMonsters() {

		return getBoolean(ConfigNodes.NWS_FORCE_TOWN_MONSTERS_ON);
	}

	public static boolean isFire() {

		return getBoolean(ConfigNodes.NWS_WORLD_FIRE);
	}

	public static boolean isForcingFire() {

		return getBoolean(ConfigNodes.NWS_FORCE_FIRE_ON);
	}

	public static boolean isUsingPlotManagementDelete() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE_ENABLE);
	}

	public static List<String> getPlotManagementDeleteIds() {

		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE);
	}

	public static boolean isUsingPlotManagementMayorDelete() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_MAYOR_DELETE_ENABLE);
	}

	public static List<String> getPlotManagementMayorDelete() {

		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_MAYOR_DELETE);
	}

	public static boolean isUsingPlotManagementRevert() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_ENABLE);
	}

	public static long getPlotManagementSpeed() {

		return getSeconds(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_TIME);
	}

	public static boolean isUsingPlotManagementWildEntityRegen() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_MOB_REVERT_ENABLE);
	}

	public static long getPlotManagementWildRegenDelay() {

		return getSeconds(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_MOB_REVERT_TIME);
	}
	
	public static boolean isUsingPlotManagementWildBlockRegen() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_BLOCK_REVERT_ENABLE);
	}

	public static List<String> getPlotManagementIgnoreIds() {

		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_IGNORE);
	}

	public static boolean isTownRespawning() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_TOWN_RESPAWN);
	}

	public static boolean isTownRespawningInOtherWorlds() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_TOWN_RESPAWN_SAME_WORLD_ONLY);
	}
	
	public static boolean isRespawnAnchorHigherPrecedence() {
		return getBoolean(ConfigNodes.GTOWN_RESPAWN_ANCHOR_HIGHER_PRECEDENCE);
	}
	
	public static int getMaxResidentsPerTown() {
		
		return getInt(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN);
	}

	public static boolean isTownyUpdating(String currentVersion) {
		// Assume
		return !isTownyUpToDate(currentVersion);
	}

	public static boolean isTownyUpToDate(String currentVersion) {

		return currentVersion.equals(getLastRunVersion(currentVersion));
	}

	public static String getLastRunVersion(String currentVersion) {

		return getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
	}
	
	public static String getLastRunVersion() {
		return getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), "0.0.0.0");
	}

	public static void setLastRunVersion(String currentVersion) {

		setProperty(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
		config.save();
	}

	public static int getMinDistanceFromTownHomeblocks() {

		return getInt(ConfigNodes.TOWN_MIN_DISTANCE_FROM_TOWN_HOMEBLOCK);
	}
	
	public static int getMinDistanceForOutpostsFromPlot() {
		
		return getInt(ConfigNodes.TOWN_MIN_DISTANCE_FOR_OUTPOST_FROM_PLOT);
	}

	public static int getMinDistanceFromTownPlotblocks() {

		return getInt(ConfigNodes.TOWN_MIN_PLOT_DISTANCE_FROM_TOWN_PLOT);
	}
	
	public static boolean isMinDistanceIgnoringTownsInSameNation() {

		return getBoolean(ConfigNodes.TOWN_MIN_DISTANCE_IGNORED_FOR_NATIONS);
	}

	public static boolean isMinDistanceIgnoringTownsInAlliedNation() {
		return getBoolean(ConfigNodes.TOWN_MIN_DISTANCE_IGNORED_FOR_ALLIES);
	}

	public static int getMaxDistanceBetweenHomeblocks() {

		return getInt(ConfigNodes.TOWN_MAX_DISTANCE_BETWEEN_HOMEBLOCKS);
	}

	public static int getMaxResidentPlots(Resident resident) {

		int maxPlots = TownyUniverse.getInstance().getPermissionSource().getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_PLOTS.getNode());
		if (maxPlots == -1)
			maxPlots = getInt(ConfigNodes.TOWN_MAX_PLOTS_PER_RESIDENT);
		return maxPlots;
	}
	
	public static int getMaxResidentExtraPlots(Resident resident) {

		int extraPlots = TownyUniverse.getInstance().getPermissionSource().getPlayerPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_EXTRA_PLOTS.getNode());
		if (extraPlots == -1)
			extraPlots = 0;
		return extraPlots;
	}

	public static int getMaxResidentOutposts(Resident resident) {

		int maxOutposts = TownyUniverse.getInstance().getPermissionSource().getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_OUTPOSTS.getNode());
		return maxOutposts;
	}

	public static boolean getPermFlag_Resident_Friend_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_BUILD);
	}

	public static boolean getPermFlag_Resident_Friend_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_DESTROY);
	}

	public static boolean getPermFlag_Resident_Friend_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Friend_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_SWITCH);
	}

	public static boolean getPermFlag_Resident_Town_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_BUILD);
	}

	public static boolean getPermFlag_Resident_Town_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_DESTROY);
	}

	public static boolean getPermFlag_Resident_Town_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Town_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_SWITCH);
	}
	public static boolean getPermFlag_Resident_Ally_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_BUILD);
	}

	public static boolean getPermFlag_Resident_Ally_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_DESTROY);
	}

	public static boolean getPermFlag_Resident_Ally_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Ally_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_SWITCH);
	}

	public static boolean getPermFlag_Resident_Outsider_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_BUILD);
	}

	public static boolean getPermFlag_Resident_Outsider_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_DESTROY);
	}

	public static boolean getPermFlag_Resident_Outsider_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Outsider_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_SWITCH);
	}

	public static boolean getPermFlag_Town_Default_PVP() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_PVP);
	}

	public static boolean getPermFlag_Town_Default_FIRE() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_FIRE);
	}

	public static boolean getPermFlag_Town_Default_Explosion() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_EXPLOSION);
	}

	public static boolean getPermFlag_Town_Default_Mobs() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_MOBS);
	}

	public static boolean getPermFlag_Town_Resident_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_BUILD);
	}

	public static boolean getPermFlag_Town_Resident_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_DESTROY);
	}

	public static boolean getPermFlag_Town_Resident_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Resident_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_SWITCH);
	}

	public static boolean getPermFlag_Town_Nation_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_BUILD);
	}

	public static boolean getPermFlag_Town_Nation_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_DESTROY);
	}

	public static boolean getPermFlag_Town_Nation_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Nation_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_SWITCH);
	}
	
	public static boolean getPermFlag_Town_Ally_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_BUILD);
	}

	public static boolean getPermFlag_Town_Ally_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_DESTROY);
	}

	public static boolean getPermFlag_Town_Ally_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Ally_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_SWITCH);
	}

	public static boolean getPermFlag_Town_Outsider_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_BUILD);
	}

	public static boolean getPermFlag_Town_Outsider_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_DESTROY);
	}

	public static boolean getPermFlag_Town_Outsider_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Outsider_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_SWITCH);
	}

	public static boolean getDefaultResidentPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Friend_Build();
			case DESTROY:
				return getPermFlag_Resident_Friend_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Friend_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Friend_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Resident_Build();
			case DESTROY:
				return getPermFlag_Town_Resident_Destroy();
			case SWITCH:
				return getPermFlag_Town_Resident_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Resident_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultNationPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Town_Build();
			case DESTROY:
				return getPermFlag_Resident_Town_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Town_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Town_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Nation_Build();
			case DESTROY:
				return getPermFlag_Town_Nation_Destroy();
			case SWITCH:
				return getPermFlag_Town_Nation_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Nation_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}
	
	public static boolean getDefaultAllyPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Ally_Build();
			case DESTROY:
				return getPermFlag_Resident_Ally_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Ally_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Ally_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Ally_Build();
			case DESTROY:
				return getPermFlag_Town_Ally_Destroy();
			case SWITCH:
				return getPermFlag_Town_Ally_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Ally_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultOutsiderPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Outsider_Build();
			case DESTROY:
				return getPermFlag_Resident_Outsider_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Outsider_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Outsider_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Outsider_Build();
			case DESTROY:
				return getPermFlag_Town_Outsider_Destroy();
			case SWITCH:
				return getPermFlag_Town_Outsider_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Outsider_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultPermission(TownBlockOwner owner, PermLevel level, ActionType type) {

		switch (level) {
		case RESIDENT:
			return getDefaultResidentPermission(owner, type);
		case NATION:
			return getDefaultNationPermission(owner, type);
		case ALLY:
			return getDefaultAllyPermission(owner, type);
		case OUTSIDER:
			return getDefaultOutsiderPermission(owner, type);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static boolean isLogging() {

		return getBoolean(ConfigNodes.PLUGIN_LOGGING);
	}

	//public static boolean isUsingQuestioner() {
	//
	//	return getBoolean(ConfigNodes.PLUGIN_USING_QUESTIONER_ENABLE);
	//}

	public static String getAcceptCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_ACCEPT_COMMAND);
	}

	public static String getDenyCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_DENY_COMMAND);
	}

	public static String getConfirmCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_CONFIRM_COMMAND);
	}

	public static String getCancelCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_CANCEL_COMMAND);
	}

	public static boolean getOutsidersPreventPVPToggle() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_OUTSIDERS_PREVENT_PVP_TOGGLE);
	}
	
	public static boolean isForcePvpNotAffectingHomeblocks() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_HOMEBLOCKS_PREVENT_FORCEPVP);
	}

	public static int getConfirmationTimeoutSeconds() {
		return getInt(ConfigNodes.INVITE_SYSTEM_CONFIRMATION_TIMEOUT);
	}
	
	public static long getTownInviteCooldown() {

		return getSeconds(ConfigNodes.INVITE_SYSTEM_COOLDOWN_TIME);
	}
	
	public static long getInviteExpirationTime() {
		
		return getSeconds(ConfigNodes.INVITE_SYSTEM_EXPIRATION_TIME);
	}

	public static boolean isAppendingToLog() {

		return !getBoolean(ConfigNodes.PLUGIN_RESET_LOG_ON_BOOT);
	}
	
	public static int getTownyTopSize() {
		return getInt(ConfigNodes.PLUGIN_TOWNY_TOP_SIZE);
	}

	public static String getNameFilterRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_NAME_FILTER_REGEX);
	}

	public static String getNameCheckRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_NAME_CHECK_REGEX);
	}

	public static String getStringCheckRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_STRING_CHECK_REGEX);
	}

	public static String getNameRemoveRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_NAME_REMOVE_REGEX);
	}

	public static int getTeleportWarmupTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_SPAWN_TIMER);
	}
	
	public static boolean isMovementCancellingSpawnWarmup() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_MOVEMENT_CANCELS_SPAWN_WARMUP);
	}
	
	public static boolean isDamageCancellingSpawnWarmup() {
		
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DAMAGE_CANCELS_SPAWN_WARMUP);
	}
	
	public static int getSpawnCooldownTime() {
		
		return getInt(ConfigNodes.GTOWN_SETTINGS_SPAWN_COOLDOWN_TIMER);
	}
	
	public static int getPVPCoolDownTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_PVP_COOLDOWN_TIMER);
	}
	
	public static String getTownAccountPrefix() {

		return getString(ConfigNodes.ECO_TOWN_PREFIX);
	}

	public static String getNationAccountPrefix() {

		return getString(ConfigNodes.ECO_NATION_PREFIX);
	}

	public static double getTownBankCap() {

		return getDouble(ConfigNodes.ECO_BANK_CAP_TOWN);
	}

	public static double getNationBankCap() {

		return getDouble(ConfigNodes.ECO_BANK_CAP_NATION);
	}

	public static boolean getTownBankAllowWithdrawls() {

		return getBoolean(ConfigNodes.ECO_BANK_TOWN_ALLOW_WITHDRAWALS);
	}

	public static void SetTownBankAllowWithdrawls(boolean newSetting) {

		setProperty(ConfigNodes.ECO_BANK_TOWN_ALLOW_WITHDRAWALS.getRoot(), newSetting);
	}

	public static boolean getNationBankAllowWithdrawls() {

		return getBoolean(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWALS);
	}
	
	public static boolean isBankActionDisallowedOutsideTown() {
		
		return getBoolean(ConfigNodes.ECO_BANK_DISALLOW_BANK_ACTIONS_OUTSIDE_TOWN);
	}
	
	public static boolean isBankActionLimitedToBankPlots() {
		
		return getBoolean(ConfigNodes.BANK_IS_LIMTED_TO_BANK_PLOTS);
	}

	public static void SetNationBankAllowWithdrawls(boolean newSetting) {

		setProperty(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWALS.getRoot(), newSetting);
	}

	public static boolean isDisallowOneWayAlliance() {
		
		return getBoolean(ConfigNodes.WAR_DISALLOW_ONE_WAY_ALLIANCE);
	}
	
	public static int getMaxNumResidentsWithoutNation() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_MAX_NUMBER_RESIDENTS_WITHOUT_NATION);
	}
	
	public static int getNumResidentsJoinNation() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_JOIN_NATION);
	}
	
	public static int getNumResidentsCreateNation() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_CREATE_NATION);
	}
	
	public static boolean isRefundNationDisbandLowResidents() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_REFUND_DISBAND_LOW_RESIDENTS);
	}
	
	public static double getNationRequiresProximity() {
		return getDouble(ConfigNodes.GTOWN_SETTINGS_NATION_REQUIRES_PROXIMITY);
	}
	
	public static List<String> getFarmPlotBlocks() {
		return getStrArr(ConfigNodes.GTOWN_FARM_PLOT_ALLOW_BLOCKS);
	}
	
	public static List<String> getFarmAnimals() {
		return getStrArr(ConfigNodes.GTOWN_FARM_ANIMALS);
	}

	public static boolean getKeepInventoryInTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_TOWN);
	}
	
	public static boolean getKeepInventoryInOwnTown() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_OWN_TOWN);
	}
	
	public static boolean getKeepInventoryInAlliedTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_ALLIED_TOWN);
	}
	
	public static boolean getKeepInventoryInArenas() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_ARENA);
	}

	public static boolean getKeepExperienceInTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_EXPERIENCE_ON_DEATH_IN_TOWN);
	}

	public static boolean isNotificationUsingTitles() {
		return getBoolean(ConfigNodes.NOTIFICATION_USING_TITLES);
	}

	public static int getAmountOfResidentsForOutpost() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_MINIMUM_AMOUNT_RESIDENTS_FOR_OUTPOSTS);
	}

	public static int getMaximumInvitesSentTown() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_SENT_TOWN);
	}
	public static int getMaximumInvitesSentNation() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_SENT_NATION);
	}
	public static int getMaximumRequestsSentNation() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_REQUESTS_SENT_NATION);
	}

	public static int getMaximumInvitesReceivedResident() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_RECEIVED_PLAYER);
	}
	public static int getMaximumInvitesReceivedTown() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_RECEIVED_TOWN);
	}
	public static int getMaximumRequestsReceivedNation() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_REQUESTS_RECEIVED_NATION);
	}
	
	public static boolean getNationZonesEnabled() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_ENABLE);
	}
	
	public static boolean getNationZonesCapitalsOnly() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_ONLY_CAPITALS);
	}
	
	public static boolean getNationZonesWarDisables() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_WAR_DISABLES);
	}
	
	public static boolean getNationZonesShowNotifications() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_SHOW_NOTIFICATIONS);
	}
	
	public static int getNationZonesCapitalBonusSize() {
		return getInt(ConfigNodes.GNATION_SETTINGS_NATIONZONE_CAPITAL_BONUS_SIZE);
	}

	public static boolean isShowingRegistrationMessage() {
		return getBoolean(ConfigNodes.RES_SETTING_IS_SHOWING_WELCOME_MESSAGE);
	}
	
	public static int getMaxTownsPerNation() {
		return getInt(ConfigNodes.GNATION_SETTINGS_MAX_TOWNS_PER_NATION);
	}
	
	public static double getSpawnTravelCost() {
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC);
	}
	
	public static boolean isAllySpawningRequiringPublicStatus() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_IS_ALLY_SPAWNING_REQUIRING_PUBLIC_STATUS);
	}
	
	public static String getNotificationTitlesTownTitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_TOWN_TITLE);
	}
	
	public static String getNotificationTitlesTownSubtitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_TOWN_SUBTITLE);
	}
	
	public static String getNotificationTitlesWildTitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_WILDERNESS_TITLE);
	}
	
	public static String getNotificationTitlesWildSubtitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_WILDERNESS_SUBTITLE);
	}

	public static double getTownRenameCost() {
		return getDouble(ConfigNodes.ECO_TOWN_RENAME_COST);
	}

	public static double getNationRenameCost() {
		return getDouble(ConfigNodes.ECO_NATION_RENAME_COST);
	}

	public static boolean isRemovingKillerBunny() {		
		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_TOWN_KILLER_BUNNY);
	}
	
	public static boolean isSkippingRemovalOfNamedMobs() {
		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_SKIP_NAMED_MOBS);
	}
	public static List<String> getJailBlacklistedCommands() {
		return getStrArr(ConfigNodes.JAIL_BLACKLISTED_COMMANDS);
	}
	
	public static String getPAPIFormattingBoth() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_BOTH);
	}

	public static String getPAPIFormattingTown() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_TOWN);
	}

	public static String getPAPIFormattingNation() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_NATION);
	}
	
	public static String getPAPIFormattingNomad() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_NOMAD);
	}

	public static String getPAPIFormattingResident() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_RESIDENT);
	}

	public static String getPAPIFormattingMayor() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_MAYOR);
	}

	public static String getPAPIFormattingKing() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_KING);
	}
	
	public static String getPAPIRelationNone() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_NONE);
	}
	
	public static String getPAPIRelationSameTown() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_SAME_TOWN);
	}
	
	public static String getPAPIRelationSameNation() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_SAME_NATION);
	}
	
	public static String getPAPIRelationAlly() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_ALLY);
	}
	
	public static String getPAPIRelationEnemy() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_ENEMY);
	}

	public static double getPlotSetCommercialCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_COMMERCIAL);
	}
	
	public static double getPlotSetArenaCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_ARENA);
	}
	
	public static double getPlotSetEmbassyCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_EMBASSY);
	}
	
	public static double getPlotSetWildsCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_WILDS);
	}
	
	public static double getPlotSetInnCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_INN);
	}
	
	public static double getPlotSetJailCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_JAIL);
	}
	
	public static double getPlotSetFarmCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_FARM);
	}
	
	public static double getPlotSetBankCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_BANK);
	}
	
	public static int getMaxDistanceFromTownSpawnForInvite() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAX_DISTANCE_FROM_TOWN_SPAWN);
	}
	
	public static boolean getTownDisplaysXYZ() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DISPLAY_XYZ_INSTEAD_OF_TOWNY_COORDS);
	}
	
	public static boolean isTownListRandom() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DISPLAY_TOWN_LIST_RANDOMLY);
	}

	public static List<String> getOrderOfMayoralSuccession() {
		return getStrArr(ConfigNodes.GTOWN_ORDER_OF_MAYORAL_SUCCESSION);
	}

	public static boolean isWarAllowed() {
		return getBoolean(ConfigNodes.NWS_WAR_ALLOWED);
	}
	
	public static int timeToWaitAfterFlag() {
		return getInt(ConfigNodes.WAR_ENEMY_TIME_TO_WAIT_AFTER_FLAGGED);
	}
	
	public static boolean isFlaggedInteractionTown() {
		return getBoolean(ConfigNodes.WAR_ENEMY_PREVENT_INTERACTION_WHILE_FLAGGED);
	}
	
	public static boolean isFlaggedInteractionNation() {
		return getBoolean(ConfigNodes.WAR_ENEMY_PREVENT_NATION_INTERACTION_WHILE_FLAGGED);
	}
	
	public static boolean isNotificationsTownNamesVerbose() {
		return getBoolean(ConfigNodes.NOTIFICATION_TOWN_NAMES_ARE_VERBOSE);
	}

	public static Map<String,String> getNationColorsMap() {
		List<String> nationColorsList = getStrArr(ConfigNodes.GNATION_SETTINGS_ALLOWED_NATION_COLORS);
		Map<String,String> nationColorsMap = new HashMap<>();
		String[] keyValuePair;
		for(String nationColor: nationColorsList) {
			keyValuePair = nationColor.trim().split(":");
			nationColorsMap.put(keyValuePair[0], keyValuePair[1]);
		}
		return nationColorsMap;
	}

	public static String getUUIDPercent() {
		double fraction = (double) uuidCount / TownyUniverse.getInstance().getNumResidents();
		
		if (fraction == 1.00)
			return "100%";
		if (fraction > 0.89)
			return "90%+";
		if (fraction > 0.79)
			return "80%+";
		if (fraction > 0.69)
			return "70%+";
		if (fraction > 0.59)
			return "60%+";	
		if (fraction > 0.49)
			return "50%+";
		
		return "<50%";
	}

	public static int getUUIDCount() {
		return uuidCount;
	}
	
	public static void setUUIDCount(int hasUUID) {
		uuidCount = hasUUID;
	}

	public static void incrementUUIDCount() {
		uuidCount++;
	}
	
	public static boolean isTownBankruptcyEnabled() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_ENABLED);
	}

	public static double getDebtCapMaximum() {
		return getDouble(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_MAXIMUM);
	}
	
	public static double getDebtCapOverride() {
		return getDouble(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_OVERRIDE);
	}
	
	public static boolean isDebtCapDeterminedByTownLevel() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_USES_TOWN_LEVELS);
	}
	
	public static boolean isUpkeepDeletingTownsThatReachDebtCap() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_UPKEEP_DELETE_TOWNS_THAT_REACH_DEBT_CAP);
	}
	
	public static boolean isNationTaxKickingTownsThatReachDebtCap() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_NATION_KICKS_TOWNS_THAT_REACH_DEBT_CAP);
	}
	
	public static boolean doesNationTaxDeleteConqueredTownsWhichCannotPay() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_DOES_NATION_TAX_DELETE_CONQUERED_TOWNS);
	}

	public static boolean doBankruptTownsPayNationTax() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_DO_BANKRUPT_TOWNS_PAY_NATION_TAX);
	}

	public static boolean canOutlawsEnterTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTLAWS_TO_ENTER_TOWN);
	}
	
	public static int getOutlawTeleportWarmup() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_OUTLAW_TELEPORT_WARMUP);
	}
	
	public static String getOutlawTeleportWorld() { 
		return getString(ConfigNodes.GTOWN_SETTINGS_OUTLAW_TELEPORT_WORLD); 
	}
	public static boolean doTownsGetWarnedOnOutlaw() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_WARN_TOWN_ON_OUTLAW);
	}
}

