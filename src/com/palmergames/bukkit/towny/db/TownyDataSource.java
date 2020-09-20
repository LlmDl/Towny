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

//import java.util.Hashtable;
//import com.palmergames.bukkit.towny.TownySettings;

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

	public abstract void cleanupBackups();

	public abstract void deleteUnusedResidents();

	public boolean loadAll() {

		return loadWorldList() && loadNationList() && loadTownList() && loadPlotGroupList() && loadResidentList() && loadTownBlockList() && loadWorlds() && loadResidents() && loadTowns() && loadNations() && loadTownBlocks() && loadPlotGroups() && loadRegenList() && loadSnapshotList();
	}

	public boolean saveAll() {

		return saveWorldList() && savePlotGroupList() && saveWorlds() && saveNations() && saveTowns() && saveResidents() && savePlotGroups() && saveAllTownBlocks() && saveRegenList() && saveSnapshotList();
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

	abstract public boolean loadResident(Resident resident);

	abstract public boolean loadTown(Town town);

	abstract public boolean loadNation(Nation nation);

	abstract public boolean loadWorld(TownyWorld world);

	abstract public boolean loadPlotGroupList();

	abstract public boolean loadPlotGroups();

	abstract public boolean savePlotGroupList();

	abstract public boolean saveWorldList();

	abstract public boolean saveRegenList();

	abstract public boolean saveSnapshotList();

	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveTown(Town town);
	
	abstract public boolean savePlotGroup(PlotGroup group);

	abstract public boolean saveNation(Nation nation);

	abstract public boolean saveWorld(TownyWorld world);

	abstract public boolean saveAllTownBlocks();

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

	public boolean cleanup() {

		return true;

	}

	public boolean loadResidents() {

		TownyMessaging.sendDebugMsg("Loading Residents");

		TownySettings.setUUIDCount(0);
		
		for (Resident resident : getResidents()) {
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
		for (Nation nation : getNations())
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

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {

		TownyMessaging.sendDebugMsg("Saving Residents");
		for (Resident resident : getResidents())
			saveResident(resident);
		return true;
	}
	
	public boolean savePlotGroups() {
		TownyMessaging.sendDebugMsg("Saving PlotGroups");
		for (PlotGroup plotGroup : getAllPlotGroups())
			savePlotGroup(plotGroup);
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
		for (Nation nation : getNations())
			saveNation(nation);
		return true;
	}

	public boolean saveWorlds() {

		TownyMessaging.sendDebugMsg("Saving Worlds");
		for (TownyWorld world : getWorlds())
			saveWorld(world);
		return true;
	}

	// Database functions
	abstract public List<Resident> getResidents(Player player, String[] names);

	abstract public List<Resident> getResidents();
	
	abstract public List<PlotGroup> getAllPlotGroups();

	abstract public List<Resident> getResidents(String[] names);

	abstract public Resident getResident(String name) throws NotRegisteredException;

	public Resident getResident(Player player) throws NotRegisteredException {
		return getResident(player.getName());
	}

	abstract public void removeNation(Nation nation);

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

	abstract public TownyWorld getTownWorld(String townName);

	abstract public void removeResident(Resident resident);

	abstract public void removeTownBlock(TownBlock townBlock);

	abstract public void removeTownBlocks(Town town);

	abstract public Collection<TownBlock> getAllTownBlocks();

	abstract public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newWorld(String name) throws AlreadyRegisteredException;

	abstract public void removeTown(Town town);

	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;

	abstract public Set<String> getResidentKeys();

	abstract public Set<String> getTownsKeys();

	abstract public Set<String> getNationsKeys();

	abstract public List<Town> getTownsWithoutNation();

	abstract public List<Resident> getResidentsWithoutTown();

	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;
	
	abstract public void mergeNation(Nation succumbingNation, Nation prevailingNation) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException;
}
