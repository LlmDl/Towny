package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.tasks.GatherResidentUUIDTask;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * --- : Loading process : ---
 *
 * Load all the names/keys for each world, nation, town, and resident.
 * Load each world, which loads it's town blocks.
 * Load nations, towns, and residents.
 */

/*
 * Loading Towns:
 * Make sure to load TownBlocks, then HomeBlock, then Spawn.
 */

public abstract class TownyDataSource {
	final Lock lock = new ReentrantLock();
	protected final Towny plugin;
	protected final TownyUniverse universe;

	TownyDataSource(Towny plugin, TownyUniverse universe) {
		this.plugin = plugin;
		this.universe = universe;
	}

	public abstract boolean backup() throws IOException;

	public boolean loadAll() {

		return loadWorldList() && loadNationList() && loadTownList() && loadPlotGroupList() && loadJailList() && loadResidentList() && loadTownBlockList() && loadWorlds() && loadResidents() && loadTowns() && loadNations() && loadTownBlocks() && loadPlotGroups() && loadJails() && loadRegenList() && loadSnapshotList();
	}

	public boolean saveAll() {

		return saveWorldList() && savePlotGroupList() && saveWorlds() && saveNations() && saveTowns() && saveResidents() && savePlotGroups() && saveTownBlocks() && saveJails() && saveRegenList() && saveSnapshotList();
	}

	public boolean saveAllWorlds() {

		return saveWorldList() && saveWorlds();
	}

	public boolean saveQueues() {

		return saveRegenList() && saveSnapshotList();
	}

	abstract public void finishTasks();

	abstract public boolean loadTownBlockList();

	abstract public boolean loadResidentList();

	abstract public boolean loadTownList();

	abstract public boolean loadNationList();

	abstract public boolean loadWorldList();

	abstract public boolean loadRegenList();

	abstract public boolean loadSnapshotList();

	abstract public boolean loadTownBlocks();

	abstract public boolean loadJailList();
	
	abstract public boolean loadResident(Resident resident);

	abstract public boolean loadTown(Town town);

	abstract public boolean loadNation(Nation nation);

	abstract public boolean loadWorld(TownyWorld world);
	
	abstract public boolean loadJail(Jail jail);

	abstract public boolean loadPlotGroupList();

	abstract public boolean loadPlotGroups();

	abstract public boolean savePlotGroupList();

	abstract public boolean saveWorldList();

	abstract public boolean saveRegenList();

	abstract public boolean saveSnapshotList();

	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveTown(Town town);
	
	abstract public boolean savePlotGroup(PlotGroup group);
	
	abstract public boolean saveJail(Jail jail);

	abstract public boolean saveNation(Nation nation);

	abstract public boolean saveWorld(TownyWorld world);

	abstract public boolean saveTownBlock(TownBlock townBlock);

	abstract public boolean savePlotData(PlotBlockData plotChunk);

	abstract public PlotBlockData loadPlotData(String worldName, int x, int z);

	abstract public PlotBlockData loadPlotData(TownBlock townBlock);

	abstract public void deletePlotData(PlotBlockData plotChunk);

	abstract public void deleteResident(Resident resident);

	abstract public void deleteTown(Town town);

	abstract public void deleteNation(Nation nation);

	abstract public void deleteWorld(TownyWorld world);

	abstract public void deleteTownBlock(TownBlock townBlock);

	abstract public void deleteFile(String file);
	
	abstract public void deletePlotGroup(PlotGroup group);
	
	abstract public void deleteJail(Jail jail);

	public boolean cleanup() {

		return true;

	}

	public boolean loadResidents() {

		TownyMessaging.sendDebugMsg("Loading Residents");

		TownySettings.setUUIDCount(0);
		
		for (Resident resident : universe.getResidents()) {
			if (!loadResident(resident)) {
				System.out.println("[Towny] Loading Error: Could not read resident data '" + resident.getName() + "'.");
				return false;
			}

			if (resident.hasUUID())
				TownySettings.incrementUUIDCount();
			else
				GatherResidentUUIDTask.addResident(resident);
		}
		return true;
	}

	public boolean loadTowns() {

		TownyMessaging.sendDebugMsg("Loading Towns");
		for (Town town : getTowns())
			if (!loadTown(town)) {
				System.out.println("[Towny] Loading Error: Could not read town data '" + town.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadNations() {

		TownyMessaging.sendDebugMsg("Loading Nations");
		for (Nation nation : universe.getNations())
			if (!loadNation(nation)) {
				System.out.println("[Towny] Loading Error: Could not read nation data '" + nation.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadWorlds() {

		TownyMessaging.sendDebugMsg("Loading Worlds");
		for (TownyWorld world : getWorlds())
			if (!loadWorld(world)) {
				System.out.println("[Towny] Loading Error: Could not read world data '" + world.getName() + "'.");
				return false;
			} else {
				// Push all Towns belonging to this world
			}
		return true;
	}
	
	public boolean loadJails() {
		TownyMessaging.sendDebugMsg("Loading Jails");
		for (Jail jail : getAllJails()) {
			if (!loadJail(jail)) {
				System.out.println("[Towny] Loading Error: Could not read jail data '" + jail.getUUID() + "'.");
				return false;
			}
		}
		return true;
	}

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {

		TownyMessaging.sendDebugMsg("Saving Residents");
		for (Resident resident : universe.getResidents())
			saveResident(resident);
		return true;
	}
	
	public boolean savePlotGroups() {
		TownyMessaging.sendDebugMsg("Saving PlotGroups");
		for (PlotGroup plotGroup : getAllPlotGroups())
			savePlotGroup(plotGroup);
		return true;
	}

	public boolean saveJails() {
		TownyMessaging.sendDebugMsg("Saving Jails");
		for (Jail jail : getAllJails())
			saveJail(jail);
		return true;
	}
	
	public boolean saveTowns() {

		TownyMessaging.sendDebugMsg("Saving Towns");
		for (Town town : getTowns())
			saveTown(town);
		return true;
	}

	public boolean saveNations() {

		TownyMessaging.sendDebugMsg("Saving Nations");
		for (Nation nation : universe.getNations())
			saveNation(nation);
		return true;
	}

	public boolean saveWorlds() {

		TownyMessaging.sendDebugMsg("Saving Worlds");
		for (TownyWorld world : getWorlds())
			saveWorld(world);
		return true;
	}
	
	public boolean saveTownBlocks() {
		TownyMessaging.sendDebugMsg("Saving Townblocks");
		for (Town town : getTowns()) {
			for (TownBlock townBlock : town.getTownBlocks())
				saveTownBlock(townBlock);
		}
		return true;
	}

	// Database functions
	abstract public List<Resident> getResidents(Player player, String[] names);

	abstract public List<Resident> getResidents();
	
	abstract public List<PlotGroup> getAllPlotGroups();
	
	abstract public List<Jail> getAllJails();

	abstract public List<Resident> getResidents(String[] names);
	
	abstract public List<Resident> getResidents(UUID[] uuids);

	/**
	 * @deprecated as of 0.96.6.0. Use {@link TownyUniverse#getResident(String)} instead.
	 * @param name The name of the resident.
	 * @return Resident with the given name.
	 * @throws NotRegisteredException if the Resident does not exist.
	 */
	@Deprecated
	abstract public Resident getResident(String name) throws NotRegisteredException;

	abstract public void removeNation(Nation nation);

	/**
	 * @deprecated as of 0.96.6.0. Use {@link TownyUniverse#hasResident(String)} instead.
	 * @param name The name of the resident.
	 * @return true if the resident exists.
	 */
	@Deprecated
	abstract public boolean hasResident(String name);

	abstract public boolean hasTown(String name);

	abstract public boolean hasNation(String name);

	abstract public List<Town> getTowns(String[] names);

	abstract public List<Town> getTowns();

	abstract public Town getTown(String name) throws NotRegisteredException;

	abstract public Town getTown(UUID uuid) throws NotRegisteredException;

	abstract public List<Nation> getNations(String[] names);

	abstract public List<Nation> getNations();

	abstract public Nation getNation(String name) throws NotRegisteredException;

	abstract public Nation getNation(UUID uiid) throws NotRegisteredException;

	abstract public TownyWorld getWorld(String name) throws NotRegisteredException;

	abstract public List<TownyWorld> getWorlds();

	/**
	 * @deprecated as of 0.96.3.0, Use {@link Town#getHomeblockWorld()} instead.
	 * 
	 * Legacy method to get a world associated with a town.
	 * 
	 * @param townName The name of a town.
	 * 
	 * @return Returns a {@link TownyWorld} associated with the town.
	 */
	@Deprecated // TODO: Scrap worlds holding Towns. Towns' homeblocks should be reliable enough to return a world when needed (if we need it at all anymore.)
	public TownyWorld getTownWorld(String townName) {

		for (TownyWorld world : universe.getWorldMap().values()) {
			if (world.hasTown(townName))
				return world;
		}

		// If this has failed the Town has no land claimed at all but should be given a world regardless.
		return universe.getDataSource().getWorlds().get(0);
	}

	abstract public void removeResident(Resident resident);

	abstract public void removeTownBlock(TownBlock townBlock);

	abstract public void removeTownBlocks(Town town);

	abstract public Collection<TownBlock> getAllTownBlocks();

	abstract public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newResident(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;
	
	abstract public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newNation(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newWorld(String name) throws AlreadyRegisteredException;

	abstract public void removeTown(Town town);

	abstract public void removeTown(Town town, boolean delayFullRemoval);

	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;

	abstract public void removeJail(Jail jail);
	
	/**
	 * @deprecated as of 0.96.4.0, We do not advise messing with the Residents Map.
	 * 
	 * @return Returns a {@link Set} of the Residents Map
	 */
	@Deprecated
	abstract public Set<String> getResidentKeys();

	/**
	 * @deprecated as of 0.96.4.0, We do not advise messing with the Towns Map.
	 * 
	 * @return Returns a {@link Set} of the Towns Map
	 */
	@Deprecated
	abstract public Set<String> getTownsKeys();

	/**
	 * @deprecated as of 0.96.4.0, We do not advise messing with the Nations Map.
	 * 
	 * @return Returns a {@link Set} of the Nations Map
	 */
	@Deprecated
	abstract public Set<String> getNationsKeys();

	abstract public List<Town> getTownsWithoutNation();

	abstract public List<Resident> getResidentsWithoutTown();

	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;
	
	abstract public void mergeNation(Nation succumbingNation, Nation prevailingNation) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void mergeTown(Town mergeInto, Town mergeFrom);

	abstract public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException;
}
