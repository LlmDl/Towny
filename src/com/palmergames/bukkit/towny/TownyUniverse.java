package com.palmergames.bukkit.towny;

import com.palmergames.annotations.Unmodifiable;
import com.palmergames.bukkit.config.migration.ConfigMigrator;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyDatabaseHandler;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.BackupTask;
import com.palmergames.bukkit.towny.tasks.CleanupTask;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.Trie;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Towny's class for internal API Methods
 * If you don't want to change the dataSource, war, permissions or similiar behavior
 * and only for example want to get Resident objects you should use {@link TownyAPI}
 *
 * @author Lukas Mansour (Articdive)
 */
public class TownyUniverse {
    private static TownyUniverse instance;
    private final Towny towny;
    
    private final Map<UUID, Resident> residentUUIDMap = new ConcurrentHashMap<>();
    private final Map<String, Resident> residentNameMap = new ConcurrentHashMap<>();
    private final Trie residentsTrie = new Trie();
    
    private final Map<String, Town> townNameMap = new ConcurrentHashMap<>();
    private final Map<UUID, Town> townUUIDMap = new ConcurrentHashMap<>();
    private final Trie townsTrie = new Trie();
    
    private final Map<String, Nation> nationNameMap = new ConcurrentHashMap<>();
	private final Map<UUID, Nation> nationUUIDMap = new ConcurrentHashMap<>();
    private final Trie nationsTrie = new Trie();
    
    private final Map<String, TownyWorld> worlds = new ConcurrentHashMap<>();
    private final Map<String, CustomDataField<?>> registeredMetadata = new HashMap<>();
	private final Map<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
	private CompletableFuture<Void> backupFuture;
    
    private final List<Resident> jailedResidents = new ArrayList<>();
    private final String rootFolder;
    private TownyDataSource dataSource;
    private TownyPermissionSource permissionSource;
    private List<War> wars = new ArrayList<>();

    private TownyUniverse() {
        towny = Towny.getPlugin();
        rootFolder = towny.getDataFolder().getPath();
    }
    
    public static TownyUniverse getInstance() {
        if (instance == null) {
            instance = new TownyUniverse();
        }
        return instance;
    }

    /**
     * Loads Towny's files/database en masse. Will end up in safemode if things do not go well. 
     * 
     * Loads config/language/townyperms files.
     * Initiates the logger.
     * Flushes object maps.
     * Saves and loads the database.
     * Will migrate the config if needed.
     * Loads the town and nation levels.
     * Legacy outpost test.
     * Schedule cleanup and backup.
     * 
     * @return true if things go well.
     */
    boolean loadSettings() {
        
    	// Load config, language and townyperms files.
    	if (!loadFiles())
    		return false;

    	// Init logger
		TownyLogger.getInstance();

        // Clears the object maps from memory.
        clearAllObjects();
                
        // Try to load and save the database.
        if (!loadAndSaveDatabase(TownySettings.getLoadDatabase(), TownySettings.getSaveDatabase()))
        	return false;
        
        // Schedule metadata to be loaded
		MetadataLoader.getInstance().scheduleDeserialization();

        // Try migrating the config and world files if the version has changed.
        if (!TownySettings.getLastRunVersion().equals(towny.getVersion())) {
			ConfigMigrator migrator = new ConfigMigrator(TownySettings.getConfig(), "config-migration.json");
			migrator.migrate();
		}
        
        // Loads Town and Nation Levels after migration has occured.
        if (!loadTownAndNationLevels())
        	return false;

        File f = new File(rootFolder, "outpostschecked.txt");                                        // Old towny didn't keep as good track of outpost spawn points,
        if (!f.exists()) {                                                                           // some of them ending up outside of claimed plots. If the file 
            for (Town town : dataSource.getTowns()) TownyDatabaseHandler.validateTownOutposts(town); // does not exist we will test all outpostspawns and create the
            towny.saveResource("outpostschecked.txt", false);                                        // file. Sometimes still useful on servers who've manually
        }                                                                                            // altered data manually and want to re-check.

		// Run both the cleanup and backup async.
		performCleanupAndBackup();

		// Things would appear to have gone well.
        return true;
    }
 
    /*
     * loadSettings() functions.
     */

    /**
    * Performs CleanupTask and BackupTask in async,
     */
    public void performCleanupAndBackup() {
		backupFuture = CompletableFuture
			.runAsync(new CleanupTask())
			.thenRunAsync(new BackupTask());
	}
    
    /**
     * Load config, language and townyperms files.
     * 
     * @return true if no exceptions are found.
     */
    private boolean loadFiles() {
        try {
            TownySettings.loadConfig(rootFolder + File.separator + "settings" + File.separator + "config.yml", towny.getVersion());
            Translation.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
            TownyPerms.loadPerms(rootFolder + File.separator + "settings", "townyperms.yml");
        } catch (IOException | TownyException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * Clears the object maps.
     */
    public void clearAllObjects() {
    	worlds.clear();
        nationNameMap.clear();
        nationUUIDMap.clear();
        townNameMap.clear();
        townUUIDMap.clear();
        residentNameMap.clear();
        residentUUIDMap.clear();
        townBlocks.clear();
    }
    
    /**
     * Test loading and saving the database.
     * 
     * @param loadDbType - load setting from the config.
     * @param saveDbType - save setting from the config.
     * @return true when the databse will load and save.
     */
    private boolean loadAndSaveDatabase(String loadDbType, String saveDbType) {
    	System.out.println("[Towny] Database: [Load] " + loadDbType + " [Save] " + saveDbType);
        // Try loading the database.
        long startTime = System.currentTimeMillis();
        if (!loadDatabase(loadDbType)) {
            System.out.println("[Towny] Error: Failed to load!");
            return false;
        } else {
        	Bukkit.getPluginManager().callEvent(new TownyLoadedDatabaseEvent());
        }
        
        long time = System.currentTimeMillis() - startTime;
        System.out.println("[Towny] Database: Loaded in " + time + "ms.");
        System.out.println("[Towny] Database: " + TownySettings.getUUIDPercent() + " of residents have stored UUIDs."); // TODO: remove this when we're using UUIDs directly in the database.

        // Try saving the database.
        if (!saveDatabase(saveDbType)) {
        	System.out.println("[Towny] Error: Unsupported save format!");
        	return false;
        }
		return true;
    }
    
    /**
     * Loads the database into memory.
     *  
     * @param loadDbType - load setting from the config.
     * @return true when the database will load.
     */
    private boolean loadDatabase(String loadDbType) {
        
        switch (loadDbType.toLowerCase()) {
            case "ff":
            case "flatfile": {
                this.dataSource = new TownyFlatFileSource(towny, this);
                break;
            }
            case "mysql": {
                this.dataSource = new TownySQLSource(towny, this);
                break;
            }
            default: {
                return false;
            }
        }
        
        return dataSource.loadAll();
    }
    
    /**
     * Saves the database into memory.
     * 
     * @param saveDbType - save setting from the config.
     * @return true when the database will save.
     */
    private boolean saveDatabase(String saveDbType) {
        try {
            // Set the new class for saving.
            switch (saveDbType.toLowerCase()) {
                case "ff":
                case "flatfile": {
                    this.dataSource = new TownyFlatFileSource(towny, this);
                    break;
                }
                case "mysql": {
                    this.dataSource = new TownySQLSource(towny, this);
                    break;
                }
                default: {}
            }

            if (TownySettings.getLoadDatabase().equalsIgnoreCase(saveDbType)) {
                // Update all Worlds data files
                dataSource.saveAllWorlds();                
            } else {
                //Formats are different so save ALL data.
                dataSource.saveAll();
            }
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * Loads the Town and Nation Levels from the config.yml
     * 
     * @return true if they have the required elements.
     */
    private boolean loadTownAndNationLevels() {
		// Load Nation & Town level data into maps.
		try {
			TownySettings.loadTownLevelConfig();
			TownySettings.loadNationLevelConfig();
			return true;
		} catch (IOException e) {
			return false;
		}
    }

    /**
     * Run during onDisable() to finish cleanup and backup.
     */
    public void finishTasks() {
    	if (backupFuture != null) {
			// Join into main thread for proper termination.
			backupFuture.join();
		}
	}

    /*
     * DataSource, PermissionSource and RootFolder.
     */

    public TownyDataSource getDataSource() {
        return dataSource;
    }
    
    public TownyPermissionSource getPermissionSource() {
        return permissionSource;
    }
    
    public void setPermissionSource(TownyPermissionSource permissionSource) {
        this.permissionSource = permissionSource;
    }
    
    public String getRootFolder() {
        return rootFolder;
    }
    
    /*
     * Maps and Tries
     */

	// =========== Resident Methods ===========

	/**
	 * Check if a resident exists with the passed in name.
	 * Will return true for fake residents and registered NPCs.
	 * 
	 * @param residentName Resident name to check for.
	 * @return whether Towny has a resident matching that name.
	 */
	public boolean hasResident(@NotNull String residentName) {
		Validate.notNull(residentName, "Resident name cannot be null!");
		
		if (residentName.isEmpty())
			return false;
		
		if (TownySettings.isFakeResident(residentName))
			return true;

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(residentName).toLowerCase();
		} catch (InvalidNameException ignored) {
			return false;
		}
		
		return residentNameMap.containsKey(filteredName);
	}

	/**
	 * Check if a resident exists matching the passed in UUID.
	 * 
	 * @param residentUUID UUID of the resident to check.
	 * @return whether the resident matching the UUID exists.
	 */
	public boolean hasResident(@NotNull UUID residentUUID) {
		Validate.notNull(residentUUID, "Resident uuid cannot be null!");
		
		return residentUUIDMap.containsKey(residentUUID);
	}

	/**
	 * Get the resident matching the passed in name.
	 * 
	 * Any fake residents (not registered NPCs) will return a new instance of a resident on method call.
	 * 
	 * @param residentName Name of the resident to fetch.
	 * @return the resident matching the given name or {@code null} if no resident is found.
	 */
	@Nullable
	public Resident getResident(@NotNull String residentName) {
		Validate.notNull(residentName, "Resident name cannot be null!");

		if (residentName.isEmpty())
			return null;

		String filteredName = residentName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(residentName).toLowerCase();
		} catch (InvalidNameException ignored) {
		}
		
		Resident res = residentNameMap.get(filteredName);

		if (res == null && TownySettings.isFakeResident(residentName)) {
			Resident npc = new Resident(residentName);
			npc.setNPC(true);
			return npc;
		}
		
		return res;
	}

	/**
	 * Get an optional instance of the resident matching the passed in name.
	 * 
	 * @param residentName Name of the resident to fetch.
	 * @return Optional object that may contain the resident matching the given name.
	 */
	@NotNull
	public Optional<Resident> getResidentOpt(@NotNull String residentName) {
		return Optional.ofNullable(getResident(residentName));
	}

	/**
	 * Get the resident with the passed-in UUID.
	 *
	 * @param residentUUID UUID of the resident to get.
	 * @return the resident with the passed-in UUID or {@code null} if no resident is found.
	 */
	@Nullable
	public Resident getResident(@NotNull UUID residentUUID) {
		Validate.notNull(residentUUID, "Resident uuid cannot be null!");
		
		return residentUUIDMap.get(residentUUID);
	}
	
	/**
	 * Get an optional object that may contain the resident with the passed-in UUID.
	 *
	 * @param residentUUID UUID of the resident to get.
	 * @return an optional object that may contain the resident with the passed-in UUID. 
	 */
	@NotNull
	public Optional<Resident> getResidentOpt(@NotNull UUID residentUUID) {
		return Optional.ofNullable(getResident(residentUUID));
	}
	
	// Internal Use Only
	public void registerResidentUUID(@NotNull Resident resident) throws AlreadyRegisteredException {
		Validate.notNull(resident, "Resident cannot be null!");
		
		if (resident.getUUID() != null) {
			if (residentUUIDMap.putIfAbsent(resident.getUUID(), resident) != null) {
				throw new AlreadyRegisteredException(
					String.format("UUID '%s' was already registered for resident '%s'!", resident.getUUID().toString(), resident.getName())
				);
			}
		}
	}

	/**
	 * Register a resident into the internal structures.
	 * This will allow the resident to be fetched by name and UUID, as well as autocomplete the resident name.
	 * 
	 * If a resident's name or UUID change, the resident must be re-registered into the maps. 
	 * 
	 * This does not modify the resident internally, nor saves the resident in the database.
	 * 
	 * @param resident Resident to register.
	 * @throws AlreadyRegisteredException if another resident has been registered with the same name or UUID.
	 */
	public void registerResident(@NotNull Resident resident) throws AlreadyRegisteredException {
		Validate.notNull(resident, "Resident cannot be null!");

		if (residentNameMap.putIfAbsent(resident.getName().toLowerCase(), resident) != null) {
			throw new AlreadyRegisteredException(String.format("The resident with name '%s' is already registered!", resident.getName()));
		}

		residentsTrie.addKey(resident.getName());
		registerResidentUUID(resident);
	}

	/**
	 * Unregister a resident from the internal structures.
	 * This does not modify the resident internally, nor performs any database operations using the resident.
	 * 
	 * @param resident Resident to unregister
	 * @throws NotRegisteredException if the resident's name or UUID was not registered.
	 */
	public void unregisterResident(@NotNull Resident resident) throws NotRegisteredException {
		Validate.notNull(resident, "Resident cannot be null!");

		if (residentNameMap.remove(resident.getName().toLowerCase()) == null) {
			throw new NotRegisteredException(String.format("The resident with the name '%s' is not registered!", resident.getName()));
		}

		residentsTrie.removeKey(resident.getName());

		if (resident.getUUID() != null) {
			if (residentUUIDMap.remove(resident.getUUID()) == null) {
				throw new NotRegisteredException(String.format("The resident with the UUID '%s' is not registered!", resident.getUUID().toString()));
			}
		}
	}

	/**
	 *
	 * @return map of string -&gt; resident.
	 * 
	 * @deprecated as of 0.96.6.0. Towny does not recommend directly accessing internal structures.
	 */
	@Deprecated
    public Map<String, Resident> getResidentMap() {
        return residentNameMap;
    }
    
    @Unmodifiable
    public Collection<Resident> getResidents() {
		return Collections.unmodifiableCollection(residentNameMap.values());
	}

	/**
	 * @return number of residents that Towny has.
	 */
	public int getNumResidents() {
		return residentNameMap.size();
	}

	public Trie getResidentsTrie() {
		return residentsTrie;
	}
	
    public List<Resident> getJailedResidentMap() {
        return jailedResidents;
    }

	// =========== Town Methods ===========
	
	public boolean hasTown(@NotNull String townName) {
		Validate.notNull(townName, "Town Name cannot be null!");
		
		// Fast-fail
		if (townName.isEmpty())
			return false;
		
    	String formattedName;
		try {
			formattedName = NameValidation.checkAndFilterName(townName).toLowerCase();
		} catch (InvalidNameException e) {
			return false;
		}
		
		return townNameMap.containsKey(formattedName);
	}
	
	public boolean hasTown(@NotNull UUID townUUID) {
		Validate.notNull(townUUID, "Town UUID cannot be null!");
    	
    	return townUUIDMap.containsKey(townUUID);
	}
	
	@Nullable
	public Town getTown(@NotNull String townName) {
    	Validate.notNull(townName, "Town Name cannot be null!");
    	
    	// Fast-fail empty names
    	if (townName.isEmpty())
    		return null;
    	
		String formattedName;
		try {
			formattedName = NameValidation.checkAndFilterName(townName).toLowerCase();
		} catch (InvalidNameException e) {
			return null;
		}
		
		return townNameMap.get(formattedName);
	}
	
	@Nullable
	public Town getTown(UUID townUUID) {
    	return townUUIDMap.get(townUUID);
	}
	
	@Unmodifiable
	public Collection<Town> getTowns() {
    	return Collections.unmodifiableCollection(townNameMap.values());
	}

	/**
	 * 
	 * @return direct access to the town name map that TownyUniverse uses.
	 * 
	 * @deprecated as of 0.96.4.0. It is not recommended to directly access the map, but rather use
	 * other towny universe methods to ensure safe-access and safe-manipulation.
	 */
	@Deprecated
	public Map<String, Town> getTownsMap() {
        return townNameMap;
    }
    
    public Trie getTownsTrie() {
    	return townsTrie;
	}

	// Internal use only.
	public void newTownInternal(String name) throws AlreadyRegisteredException, com.palmergames.bukkit.towny.exceptions.InvalidNameException {
    	newTown(name, false);
	}

	/**
	 * Create a new town from the string name.
	 *
	 * @param name Town name
	 * @throws AlreadyRegisteredException Town name is already in use.
	 * @throws InvalidNameException Town name is invalid.
	 */
	public void newTown(@NotNull String name) throws AlreadyRegisteredException, InvalidNameException {
		Validate.notNull(name, "Name cannot be null!");
		
		newTown(name, true);
	}

	private void newTown(String name, boolean assignUUID) throws AlreadyRegisteredException, InvalidNameException {
		String filteredName = NameValidation.checkAndFilterName(name);;

		Town town = new Town(filteredName, assignUUID ? UUID.randomUUID() : null);
		registerTown(town);
	}
	
	// This is used internally since UUIDs are assigned after town objects are created.
	public void registerTownUUID(@NotNull Town town) throws AlreadyRegisteredException {
		Validate.notNull(town, "Town cannot be null!");
		
		if (town.getUUID() != null) {
			
			if (townUUIDMap.containsKey(town.getUUID())) {
				throw new AlreadyRegisteredException("UUID of town " + town.getName() + " was already registered!");
			}
			
			townUUIDMap.put(town.getUUID(), town);
		}
	}

	/**
	 * Used to register a town into the TownyUniverse internal maps.
	 * 
	 * This does not create a new town, or save a new town.
	 * 
	 * @param town Town to register.
	 * @throws AlreadyRegisteredException Town is already in the universe maps.
	 */
	public void registerTown(@NotNull Town town) throws AlreadyRegisteredException {
		Validate.notNull(town, "Town cannot be null!");
		
		if (townNameMap.putIfAbsent(town.getName().toLowerCase(), town) != null) {
			throw new AlreadyRegisteredException(String.format("The town with name '%s' is already registered!", town.getName()));
		}
		
		townsTrie.addKey(town.getName());
		registerTownUUID(town);
	}

	/**
	 * Used to unregister a town from the TownyUniverse internal maps.
	 * 
	 * This does not delete a town, nor perform any actions that affect the town internally.
	 * 
	 * @param town Town to unregister
	 * @throws NotRegisteredException Town is not registered in the universe maps.
	 */
	public void unregisterTown(@NotNull Town town) throws NotRegisteredException {
		Validate.notNull(town, "Town cannot be null!");
		
		if (townNameMap.remove(town.getName().toLowerCase()) == null) {
			throw new NotRegisteredException(String.format("The town with the name '%s' is not registered!", town.getName()));
		}
		
		townsTrie.removeKey(town.getName());
		
		if (town.getUUID() != null) {
			if (townUUIDMap.remove(town.getUUID()) == null) {
				throw new NotRegisteredException(String.format("The town with the UUID '%s' is not registered!", town.getUUID().toString()));
			}
		}
	}

	// =========== Nation Methods ===========

	/**
	 * Check if the nation matching the given name exists.
	 *
	 * @param nationName Name of the nation to check.
	 * @return whether the nation matching the name exists.
	 */
	public boolean hasNation(@NotNull String nationName) {
		Validate.notNull(nationName, "Nation Name cannot be null!");

		// Fast-fail if empty
		if (nationName.isEmpty())
			return false;

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(nationName).toLowerCase();
		} catch (InvalidNameException ignored) {
			return false;
		}

		return nationNameMap.containsKey(filteredName);
	}

	/**
	 * Check if the nation matching the given UUID exists.
	 * 
	 * @param nationUUID UUID of the nation to check.
	 * @return whether the nation matching the UUID exists.
	 */
	public boolean hasNation(@NotNull UUID nationUUID) {
		Validate.notNull(nationUUID, "Nation UUID cannot be null!");
		
		return nationUUIDMap.containsKey(nationUUID);
	}

	/**
	 * Get the nation with the passed-in nation name if it exists.
	 * 
	 * @param nationName Name of the nation to fetch.
	 * @return the nation matching the name or {@code null} if it doesn't exist.
	 */
	@Nullable
	public Nation getNation(@NotNull String nationName) {
		Validate.notNull(nationName, "Nation Name cannot be null!");
		
		// Fast-fail if empty
		if (nationName.isEmpty())
			return null;

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(nationName).toLowerCase();
		} catch (InvalidNameException ignored) {
			return null;
		}
		
		return nationNameMap.get(filteredName);
	}

	/**
	 * Get the nation with the given UUID if it exists.
	 * 
	 * @param nationUUID UUID of the nation to get.
	 * @return the nation with the given UUID or {@code null} if it doesn't exist.
	 */
	@Nullable
	public Nation getNation(@NotNull UUID nationUUID) {
		Validate.notNull(nationUUID, "Nation UUID cannot be null!");
		
		return nationUUIDMap.get(nationUUID);
	}
	
	@Unmodifiable
	public Collection<Nation> getNations() {
		return Collections.unmodifiableCollection(nationNameMap.values());
	}
	
	public int getNumNations() {
		return nationNameMap.size();
	}

	// This is used internally since UUIDs are assigned after nation objects are created.
	public void registerNationUUID(@NotNull Nation nation) throws AlreadyRegisteredException {
		Validate.notNull(nation, "Nation cannot be null!");

		if (nation.getUUID() != null) {

			if (nationUUIDMap.containsKey(nation.getUUID())) {
				throw new AlreadyRegisteredException("UUID of nation " + nation.getName() + " was already registered!");
			}

			nationUUIDMap.put(nation.getUUID(), nation);
		}
	}

	/**
	 * Used to register a nation into the TownyUniverse internal maps.
	 *
	 * This does not create a new nation, or save a new nation.
	 *
	 * @param nation Nation to register.
	 * @throws AlreadyRegisteredException Nation is already in the universe maps.
	 */
	public void registerNation(@NotNull Nation nation) throws AlreadyRegisteredException {
		Validate.notNull(nation, "Nation cannot be null!");

		if (nationNameMap.putIfAbsent(nation.getName().toLowerCase(), nation) != null) {
			throw new AlreadyRegisteredException(String.format("The nation with name '%s' is already registered!", nation.getName()));
		}

		nationsTrie.addKey(nation.getName());
		registerNationUUID(nation);
	}

	/**
	 * Used to unregister a nation from the TownyUniverse internal maps.
	 *
	 * This does not delete a nation, nor perform any actions that affect the nation internally.
	 *
	 * @param nation Nation to unregister
	 * @throws NotRegisteredException Nation is not registered in the universe maps.
	 */
	public void unregisterNation(@NotNull Nation nation) throws NotRegisteredException {
		Validate.notNull(nation, "Nation cannot be null!");

		if (nationNameMap.remove(nation.getName().toLowerCase()) == null) {
			throw new NotRegisteredException(String.format("The nation with the name '%s' is not registered!", nation.getName()));
		}

		nationsTrie.removeKey(nation.getName());

		if (nation.getUUID() != null) {
			if (nationUUIDMap.remove(nation.getUUID()) == null) {
				throw new NotRegisteredException(String.format("The nation with the UUID '%s' is not registered!", nation.getUUID().toString()));
			}
		}
	}

	/**
	 * Get direct access to the internal nation map.
	 * 
	 * @return direct access to internal nation map.
	 * 
	 * @deprecated as of 0.96.4.0, It is not safe, nor recommended, to directly access the internal nation map. 
	 */
	@Deprecated
	public Map<String, Nation> getNationsMap() {
		return nationNameMap;
	}

	public Trie getNationsTrie() {
		return nationsTrie;
	}
	
    public Map<String, TownyWorld> getWorldMap() {
        return worlds;
    }
    
    /*
     * Towny Tree command output.
     */

    public List<String> getTreeString(int depth) {
        
        List<String> out = new ArrayList<>();
        out.add(getTreeDepth(depth) + "Universe (1)");
        if (towny != null) {
            out.add(getTreeDepth(depth + 1) + "Server (" + BukkitTools.getServer().getName() + ")");
            out.add(getTreeDepth(depth + 2) + "Version: " + BukkitTools.getServer().getVersion());
            //out.add(getTreeDepth(depth + 2) + "Players: " + BukkitTools.getOnlinePlayers().length + "/" + BukkitTools.getServer().getMaxPlayers());
            out.add(getTreeDepth(depth + 2) + "Worlds (" + BukkitTools.getWorlds().size() + "): " + Arrays.toString(BukkitTools.getWorlds().toArray(new World[0])));
        }
        out.add(getTreeDepth(depth + 1) + "Worlds (" + worlds.size() + "):");
        for (TownyWorld world : worlds.values()) {
            out.addAll(world.getTreeString(depth + 2));
        }
        
        out.add(getTreeDepth(depth + 1) + "Nations (" + nationNameMap.size() + "):");
        for (Nation nation : nationNameMap.values()) {
            out.addAll(nation.getTreeString(depth + 2));
        }
        
        Collection<Town> townsWithoutNation = dataSource.getTownsWithoutNation();
        out.add(getTreeDepth(depth + 1) + "Towns (" + townsWithoutNation.size() + "):");
        for (Town town : townsWithoutNation) {
            out.addAll(town.getTreeString(depth + 2));
        }
        
        Collection<Resident> residentsWithoutTown = dataSource.getResidentsWithoutTown();
        out.add(getTreeDepth(depth + 1) + "Residents (" + residentsWithoutTown.size() + "):");
        for (Resident resident : residentsWithoutTown) {
            out.addAll(resident.getTreeString(depth + 2));
        }
        return out;
    }
    
    private String getTreeDepth(int depth) {
        
        char[] fill = new char[depth * 4];
        Arrays.fill(fill, ' ');
        if (depth > 0) {
            fill[0] = '|';
            int offset = (depth - 1) * 4;
            fill[offset] = '+';
            fill[offset + 1] = '-';
            fill[offset + 2] = '-';
        }
        return new String(fill);
    }

    /*
     * PlotGroup Stuff.
     */

	public PlotGroup newGroup(Town town, String name, UUID id) throws AlreadyRegisteredException {
    	
    	// Create new plot group.
		PlotGroup newGroup = new PlotGroup(id, name, town);
		
		// Check if there is a duplicate
		if (town.hasPlotGroupName(newGroup.getName())) {
			TownyMessaging.sendDebugMsg("group " + town.getName() + ":" + id + " already exists");
			throw new AlreadyRegisteredException();
		}
		
		// Create key and store group globally.
		town.addPlotGroup(newGroup);
		
		return newGroup;
	}

	public UUID generatePlotGroupID() {
		return UUID.randomUUID();
	}

	public void removeGroup(PlotGroup group) {
		group.getTown().removePlotGroup(group);
		
	}

    public boolean hasGroup(String townName, UUID groupID) {
		Town t = townNameMap.get(townName);
		
		if (t != null) {
			return t.getObjectGroupFromID(groupID) != null;
		}
		
		return false;
	}

	public boolean hasGroup(String townName, String groupName) {
		Town t = townNameMap.get(townName);

		if (t != null) {
			return t.hasPlotGroupName(groupName);
		}

		return false;
	}

	/**
	 * Get all the plot object groups from all towns
	 * Returns a collection that does not reflect any group additions/removals
	 * 
	 * @return collection of PlotObjectGroup
	 */
	public Collection<PlotGroup> getGroups() {
    	List<PlotGroup> groups = new ArrayList<>();
    	
		for (Town town : townNameMap.values()) {
			if (town.hasPlotGroups()) {
				groups.addAll(town.getPlotObjectGroups());
			}
		}
		
		return groups;
	}

	/**
	 * Gets the plot group from the town name and the plot group UUID 
	 * 
	 * @param townName Town name
	 * @param groupID UUID of the plot group
	 * @return PlotGroup if found, null if none found.
	 */
	public PlotGroup getGroup(String townName, UUID groupID) {
		Town t = getTown(townName);

		if (t != null) {
			return t.getObjectGroupFromID(groupID);
		}
		
		return null;
	}

	/**
	 * Gets the plot group from the town name and the plot group name
	 * 
	 * @param townName Town Name
	 * @param groupName Plot Group Name
	 * @return the plot group if found, otherwise null
	 */
	public PlotGroup getGroup(String townName, String groupName) {
		Town t = townNameMap.get(townName);

		if (t != null) {
			return t.getPlotObjectGroupFromName(groupName);
		}

		return null;
	}

	/*
	 * Metadata Stuff
	 */

	public void addCustomCustomDataField(CustomDataField<?> cdf) throws KeyAlreadyRegisteredException {
    	
    	if (this.getRegisteredMetadataMap().containsKey(cdf.getKey()))
    		throw new KeyAlreadyRegisteredException();
    	
    	this.getRegisteredMetadataMap().put(cdf.getKey(), cdf);
	}

	public Map<String, CustomDataField<?>> getRegisteredMetadataMap() {
		return getRegisteredMetadata();
	}
	
	public Map<String, CustomDataField<?>> getRegisteredMetadata() {
		return registeredMetadata;
	}

	/*
	 * Townblock Stuff
	 */

	/**
	 * How to get a TownBlock for now.
	 * 
	 * @param worldCoord we are testing for a townblock.
	 * @return townblock if it exists, otherwise null.
	 * @throws NotRegisteredException if there is no homeblock to get.
	 */
	public TownBlock getTownBlock(WorldCoord worldCoord) throws NotRegisteredException {
		if (hasTownBlock(worldCoord))
			return townBlocks.get(worldCoord);
		else 
			throw new NotRegisteredException();
	}

	/**
	 * Get Universe-wide ConcurrentHashMap of WorldCoords and their TownBlocks.
	 * Populated at load time from townblocks folder's files.
	 * 
	 * 
	 * @return townblocks hashmap read from townblock files.
	 */	
	public Map<WorldCoord, TownBlock> getTownBlocks() {
		return townBlocks;
	}
	
	public void addTownBlock(TownBlock townBlock) {
		if (hasTownBlock(townBlock.getWorldCoord()))
			return;
		townBlocks.put(townBlock.getWorldCoord(), townBlock);
	}

	/**
	 * Does this WorldCoord have a TownBlock?
	 * @param worldCoord - the coord for which we want to know if there is a townblock.
	 * @return true if Coord is a townblock
	 */	
	public boolean hasTownBlock(WorldCoord worldCoord) {
		return townBlocks.containsKey(worldCoord);
	}

	/**
	 * Remove one townblock from the TownyUniverse townblock map.
	 * @param townBlock to remove.
	 */
	public void removeTownBlock(TownBlock townBlock) {
		
		if (removeTownBlock(townBlock.getWorldCoord())) {
			try {
				if (townBlock.hasResident())
					townBlock.getResident().removeTownBlock(townBlock);
			} catch (NotRegisteredException e) {
			}
			try {
				if (townBlock.hasTown())
					townBlock.getTown().removeTownBlock(townBlock);
			} catch (NotRegisteredException e) {
			}
		}
	}
	
	/**
	 * Remove a list of TownBlocks from the TownyUniverse townblock map.
	 * @param townBlocks to remove.
	 */
	public void removeTownBlocks(List<TownBlock> townBlocks) {

		for (TownBlock townBlock : new ArrayList<>(townBlocks))
			removeTownBlock(townBlock);
	}

	/** 
	 * Removes a townblock at the given worldCoord from the TownyUniverse townblock map.
	 * @param worldCoord to remove.
	 * @return whether the townblock was successfully removed   
	 */
	private boolean removeTownBlock(WorldCoord worldCoord) {

		return townBlocks.remove(worldCoord) != null;
	}

	/*
	 * War Stuff
	 */

    public War getWarEvent(Player player) {
    	Resident resident = getResident(player.getUniqueId());
		if (resident != null)
	        for (War war : getWars()) {
	        	if (war.getWarParticipants().has(resident))
	        		return war;
	        }
        return null;
    }
    
    public War getWarEvent(TownBlock townBlock) {
    	for (War war : getWars()) {
    		if (war.getWarZoneManager().isWarZone(townBlock.getWorldCoord()))
    			return war;
    	}
    	return null;
    }
    
    public War getWarEvent(String warName) {
    	for (War war : getWars()) {
    		if (war.getWarName().equalsIgnoreCase(warName))
    			return war;
    	}
    	return null;
    }

    public boolean hasWarEvent(TownBlock townBlock) {
    	for (War war : getWars()) {
    		if (war.getWarZoneManager().isWarZone(townBlock.getWorldCoord()))
    			return true;
    	}
    	return false;
    }
    
    public boolean hasWarEvent(Town town) {
    	for (War war : getWars()) {
    		if (war.getWarParticipants().has(town))
    			return true;
    	}
    	return false;
    }
    
    public boolean hasWarEvent(Resident resident) {
     	for (War war : getWars()) {
     		if (war.getWarParticipants().has(resident))
				return true;
     	}
     	return false;
    }

	public boolean isWarTime() {	

		return !wars.isEmpty();
	}
    
    public List<War> getWars() {
    	return wars;
    }
    
    public List<String> getWarNames() {
    	List<String> names = new ArrayList<String>(wars.size());
    	for (War war : getWars())
    		names.add(war.getWarName());
    	
    	return names;
    }
    
    public void addWar(War war) {
    	wars.add(war);
    }
    
    public void removeWar(War war) {
    	war = null;
    	wars.remove(war);
    }

    // Used by FlagWar only.
    public void addFlagWarZone(WorldCoord worldCoord) {
        try {
        	if (worldCoord.getTownyWorld().isWarAllowed())
            	worldCoord.getTownyWorld().addFlagWarZone(worldCoord);
        } catch (NotRegisteredException e) {
            // Not a registered world
        }
        towny.updateCache(worldCoord);
    }

    // Used by FlagWar only.
    public void removeFlagWarZone(WorldCoord worldCoord) {
        try {
            worldCoord.getTownyWorld().removeFlagWarZone(worldCoord);
        } catch (NotRegisteredException e) {
            // Not a registered world
        }
        towny.updateCache(worldCoord);
    }
    
	/**
	 * Retrieves the configuration's output database type.
	 * 
	 * @return Returns the output of {@link TownySettings#getSaveDatabase()}
	 * 
	 * @deprecated as of 0.96.3.0, use {@link TownySettings#getSaveDatabase()} instead.
	 */
	@Deprecated
	public String getSaveDbType() {
		return TownySettings.getSaveDatabase();
	}

	/**
	 * Retrieves the configuration's input database type.
	 * 
	 * @return Returns the output of {@link TownySettings#getLoadDatabase()}
	 * 
	 * @deprecated as of 0.96.3.0, use {@link TownySettings#getLoadDatabase()} instead.
	 */
	@Deprecated
	public String getLoadDbType() {
		return TownySettings.getLoadDatabase();
	}
}
