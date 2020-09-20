package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * @author ElgarL
 * 
 */
public class TownyRegenAPI {

	// table containing snapshot data of active reversions.
	private static Hashtable<String, PlotBlockData> PlotChunks = new Hashtable<>();

	// List of all old plots still to be processed for Block removal
	private static List<WorldCoord> deleteTownBlockIdQueue = new ArrayList<>();

	// A list of worldCoords which are needing snapshots
	private static List<WorldCoord> worldCoords = new ArrayList<>();
	
	// A holder for each protection regen task
	private static  Hashtable<BlockLocation, ProtectionRegenTask> protectionRegenTasks = new Hashtable<>();
	
	// List of protection blocks placed to prevent blockPhysics.
	private static  Set<Block> protectionPlaceholders = new HashSet<>();

	/**
	 * Add a TownBlocks WorldCoord for a snapshot to be taken.
	 * 
	 * @param worldCoord - WorldCoord
	 */
	public static void addWorldCoord(WorldCoord worldCoord) {

		if (!worldCoords.contains(worldCoord))
			worldCoords.add(worldCoord);
	}

	/**
	 * @return true if there are any TownBlocks to be processed.
	 */
	public static boolean hasWorldCoords() {

		return worldCoords.size() != 0;
	}

	/**
	 * Check if this WorldCoord is waiting for a snapshot to be taken.
	 * 
	 * @param worldCoord - WorldCoord to check
	 * @return true if it's in the queue.
	 */
	public static boolean hasWorldCoord(WorldCoord worldCoord) {

		return worldCoords.contains(worldCoord);
	}

	/**
	 * @return First WorldCoord to be processed.
	 */
	public static WorldCoord getWorldCoord() {

		if (!worldCoords.isEmpty()) {
			WorldCoord wc = worldCoords.get(0);
			worldCoords.remove(0);
			return wc;
		}
		return null;
	}

	/**
	 * @return the plotChunks which are being processed
	 */
	public static Hashtable<String, PlotBlockData> getPlotChunks() {

		return PlotChunks;
	}

	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasPlotChunks() {

		return !PlotChunks.isEmpty();
	}

	/**
	 * @param plotChunks the plotChunks to set
	 */
	public static void setPlotChunks(Hashtable<String, PlotBlockData> plotChunks) {

		PlotChunks = plotChunks;
	}

	/**
	 * Removes a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param plotChunk - Chunk to remove (PlotBlockData)
	 */
	public static void deletePlotChunk(PlotBlockData plotChunk) {

		if (PlotChunks.containsKey(getPlotKey(plotChunk))) {
			PlotChunks.remove(getPlotKey(plotChunk));
			TownyUniverse.getInstance().getDataSource().saveRegenList();
		}
	}
	
	/**
	 * Adds a Plot Chunk to the regeneration Hashtable
	 * 
	 * @param plotChunk - Chunk to add (PlotBlockData)
	 * @param save - If Regen List should be saved
	 */
	public static void addPlotChunk(PlotBlockData plotChunk, boolean save) {

		if (!PlotChunks.containsKey(getPlotKey(plotChunk))) {
			//plotChunk.initialize();
			PlotChunks.put(getPlotKey(plotChunk), plotChunk);
			if (save)
				TownyUniverse.getInstance().getDataSource().saveRegenList();
		}
	}

	/**
	 * Saves a Plot Chunk snapshot to the datasource
	 * 
	 * @param plotChunk - Chunk to take Snapshot (PlotBlockData)
	 */
	public static void addPlotChunkSnapshot(PlotBlockData plotChunk) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (townyUniverse.getDataSource().loadPlotData(plotChunk.getWorldName(), plotChunk.getX(), plotChunk.getZ()) == null) {
			townyUniverse.getDataSource().savePlotData(plotChunk);
		}
	}

	/**
	 * Deletes a Plot Chunk snapshot from the datasource
	 * 
	 * @param plotChunk - Chunk to delete snapshot (PlotBlockData)
	 */
	public static void deletePlotChunkSnapshot(PlotBlockData plotChunk) {
		TownyUniverse.getInstance().getDataSource().deletePlotData(plotChunk);
	}

	/**
	 * Loads a Plot Chunk snapshot from the data source
	 * 
	 * @param townBlock - TownBlock to get
	 * @return loads the PlotData for the given townBlock or returns null.   
	 */
	public static PlotBlockData getPlotChunkSnapshot(TownBlock townBlock) {
		PlotBlockData data = TownyUniverse.getInstance().getDataSource().loadPlotData(townBlock);
		if (data != null) 
			return data;
		else
			return null;
	}

	/**
	 * Gets a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param townBlock - TownBlock to get
	 * @return PlotChunks or null   
	 */
	public static PlotBlockData getPlotChunk(TownBlock townBlock) {

		if (PlotChunks.containsKey(getPlotKey(townBlock))) {
			return PlotChunks.get(getPlotKey(townBlock));
		}
		return null;
	}

	private static String getPlotKey(PlotBlockData plotChunk) {

		return "[" + plotChunk.getWorldName() + "|" + plotChunk.getX() + "|" + plotChunk.getZ() + "]";
	}

	public static String getPlotKey(TownBlock townBlock) {

		return "[" + townBlock.getWorld().getName() + "|" + townBlock.getX() + "|" + townBlock.getZ() + "]";
	}

	/**
	 * Regenerate the chunk the player is stood in and store the block data so it can be undone later.
	 * 
	 * @param player
	 */
//	public static void regenChunk(Player player) {
//		
//		try {
//			Coord coord = Coord.parseCoord(player);
//			World world = player.getWorld();
//			Chunk chunk = world.getChunkAt(player.getLocation());
//			int maxHeight = world.getMaxHeight();
//			
//			ChunkSnapshot snapshot = chunk.getChunkSnapshot(true,true,false);
//			
//			Object[][][] snapshot = new Object[16][maxHeight][16];
//			
//			for (int x = 0; x < 16; x++) {
//				for (int z = 0; z < 16; z++) {
//					for (int y = 0; y < maxHeight; y++) {
//						
//						//Current block to save
//						BlockState state = chunk.getBlock(x, y, z).getState();
//						
//						if (state instanceof org.bukkit.block.Sign) {
//							
//							BlockSign sign = new BlockSign(BukkitTools.getTypeId(state), BukkitTools.getDataData(state), ((org.bukkit.block.Sign) state).getLines());
//							sign.setLocation(state.getLocation());
//							snapshot[x][y][z] = sign;
//							
//						} else if (state instanceof CreatureSpawner) {
//							
//							BlockMobSpawner spawner = new BlockMobSpawner(((CreatureSpawner) state).getSpawnedType());
//							spawner.setLocation(state.getLocation());
//							spawner.setDelay(((CreatureSpawner) state).getDelay());
//							snapshot[x][y][z] = spawner;
//							
//						} else if ((state instanceof InventoryHolder) && !(state instanceof Player)) {
//							
//							BlockInventoryHolder holder = new BlockInventoryHolder(BukkitTools.getTypeId(state), BukkitTools.getDataData(state), ((InventoryHolder) state).getInventory().getContents());
//							holder.setLocation(state.getLocation());
//							snapshot[x][y][z] = holder;
//							
//						} else {
//						
//							snapshot[x][y][z] = new BlockObject(BukkitTools.getTypeId(state), BukkitTools.getDataData(state), state.getLocation());
//									
//						}
//						
//					}
//				}
//			}
//			
//			TownyUniverse.getDataSource().getResident(player).addUndo(snapshot);
//
//			Bukkit.getWorld(player.getWorld().getName()).regenerateChunk(coord.getX(), coord.getZ());
//
//		} catch (NotRegisteredException e) {
//			// Failed to get resident
//		}
//	}
//	
//	/**
//	 * Restore the relevant chunk using the snapshot data stored in the resident
//	 * object.
//	 * 
//	 * @param snapshot
//	 * @param resident
//	 */
//	public static void regenUndo(Object[][][] snapshot, Resident resident) {
//
//		BlockObject key = ((BlockObject) snapshot[0][0][0]);
//		World world = key.getLocation().getWorld();
//		Chunk chunk = key.getLocation().getChunk();
//		
//		int maxHeight = world.getMaxHeight();
//		
//		for (int x = 0; x < 16; x++) {
//			for (int z = 0; z < 16; z++) {
//				for (int y = 0; y < maxHeight; y++) {
//					
//					// Snapshot data we need to update the world.
//					Object state = snapshot[x][y][z];
//					
//					// The block we will be updating
//					Block block = chunk.getBlock(x, y, z);
//					
//					if (state instanceof BlockSign) {
//
//						BlockSign signData = (BlockSign)state;
//						BukkitTools.setTypeIdAndData(block, signData.getTypeId(), signData.getData(), false);
//						
//						Sign sign = (Sign) block.getState();
//						int i = 0;
//						for (String line : signData.getLines())
//							sign.setLine(i++, line);
//						
//						sign.update(true);
//						
//					} else if (state instanceof BlockMobSpawner) {
//						
//						BlockMobSpawner spawnerData = (BlockMobSpawner) state;
//						
//						BukkitTools.setTypeIdAndData(block, spawnerData.getTypeId(), spawnerData.getData(), false);
//						((CreatureSpawner) block.getState()).setSpawnedType(spawnerData.getSpawnedType());
//						((CreatureSpawner) block.getState()).setDelay(spawnerData.getDelay());
//						
//					} else if ((state instanceof BlockInventoryHolder) && !(state instanceof Player)) {
//						
//						BlockInventoryHolder containerData = (BlockInventoryHolder) state;
//						BukkitTools.setTypeIdAndData(block, containerData.getTypeId(), containerData.getData(), false);
//						
//						// Container to receive the inventory
//						InventoryHolder container = (InventoryHolder) block.getState();
//						
//						// Contents we are respawning.						
//						if (containerData.getItems().length > 0)
//							container.getInventory().setContents(containerData.getItems());
//						
//					} else {
//						
//						BlockObject blockData = (BlockObject) state;	
//						BukkitTools.setTypeIdAndData(block, blockData.getTypeId(), blockData.getData(), false);
//					}
//					
//					
//					
//
//				}
//			}
//
//		}
//
//		TownyMessaging.sendMessage(BukkitTools.getPlayerExact(resident.getName()), Translation.of("msg_undo_complete"));
//
//	}

	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasDeleteTownBlockIdQueue() {

		return !deleteTownBlockIdQueue.isEmpty();
	}

	public static boolean isDeleteTownBlockIdQueue(WorldCoord plot) {

		return deleteTownBlockIdQueue.contains(plot);
	}

	public static void addDeleteTownBlockIdQueue(WorldCoord plot) {

		if (!deleteTownBlockIdQueue.contains(plot))
			deleteTownBlockIdQueue.add(plot);
	}

	public static WorldCoord getDeleteTownBlockIdQueue() {

		if (!deleteTownBlockIdQueue.isEmpty()) {
			WorldCoord wc = deleteTownBlockIdQueue.get(0);
			deleteTownBlockIdQueue.remove(0);
			return wc;
		}
		return null;
	}

	/**
	 * Deletes all of a specified block type from a TownBlock
	 * 
	 * @param worldCoord - WorldCoord for the Town Block
	 */
	public static void doDeleteTownBlockIds(WorldCoord worldCoord) {

		//Block block = null;
		World world = null;
		int plotSize = TownySettings.getTownBlockSize();

		//TownyMessaging.sendDebugMsg("Processing deleteTownBlockIds");

		world = worldCoord.getBukkitWorld();

		if (world != null) {
			/*
			 * if
			 * (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()),
			 * MinecraftTools.calcChunk(townBlock.getZ())))
			 * return;
			 */
			int height = world.getMaxHeight() - 1;
			int worldx = worldCoord.getX() * plotSize, worldz = worldCoord.getZ() * plotSize;

			for (int z = 0; z < plotSize; z++)
				for (int x = 0; x < plotSize; x++)
					for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
						Block block = world.getBlockAt(worldx + x, y, worldz + z);
						try {
							if (worldCoord.getTownyWorld().isPlotManagementDeleteIds(block.getType().name())) {
								block.setType(Material.AIR);
							}
						} catch (NotRegisteredException e) {
							// Not a registered world
						}
						block = null;
					}
		}

	}

	/**
	 * Deletes all of a specified block type from a TownBlock
	 * 
	 * @param townBlock - TownBlock to delete from
	 * @param material - Material to delete
	 */
	public static void deleteTownBlockMaterial(TownBlock townBlock, Material material) {

		//Block block = null;
		int plotSize = TownySettings.getTownBlockSize();

		TownyMessaging.sendDebugMsg("Processing deleteTownBlockMaterial");

		World world = BukkitTools.getServer().getWorld(townBlock.getWorld().getName());

		if (world != null) {
			/*
			 * if
			 * (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()),
			 * MinecraftTools.calcChunk(townBlock.getZ())))
			 * return;
			 */
			int height = world.getMaxHeight() - 1;
			int worldx = townBlock.getX() * plotSize, worldz = townBlock.getZ() * plotSize;

			for (int z = 0; z < plotSize; z++)
				for (int x = 0; x < plotSize; x++)
					for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
						Block block = world.getBlockAt(worldx + x, y, worldz + z);
						if (block.getType() == material) {
							block.setType(Material.AIR);
						}
						block = null;
					}
		}
	}

	/*
	 * Protection Regen follows
	 */
	
	/**
	 * Called from various explosion listeners.
	 * 
	 * @param block - Block which is being exploded.
	 * @param count - int for setting the delay to do one block at a time.
	 */
	public static void beginProtectionRegenTask(Block block, int count) {
		if (!hasProtectionRegenTask(new BlockLocation(block.getLocation())) && !isBlacklistedBlock(block.getType())) {
			// Piston extensions which are broken by explosions ahead of the base block
			// cause baseblocks to drop as items and no base block to be regenerated.
			if (block.getType().equals(Material.PISTON_HEAD)) {
				org.bukkit.block.data.type.PistonHead blockData = (org.bukkit.block.data.type.PistonHead) block.getBlockData(); 
				Block baseBlock = block.getRelative(blockData.getFacing().getOppositeFace());
				block = baseBlock;
			}
			ProtectionRegenTask task = new ProtectionRegenTask(Towny.getPlugin(), block);
			task.setTaskId(Towny.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), task, (TownySettings.getPlotManagementWildRegenDelay() + count) * 20));
			addProtectionRegenTask(task);
			block.setType(Material.AIR);
		}
	}
	
	private static boolean isBlacklistedBlock(Material type) {		
		return (type == Material.TNT || type == Material.AIR || type == Material.FIRE);
	}

	/**
	 * Does a task for this block already exist?
	 * 
	 * @param blockLocation - Location of the block
	 * @return true if a task exists
	 */
	public static boolean hasProtectionRegenTask(BlockLocation blockLocation) {

		return protectionRegenTasks.containsKey(blockLocation);

	}

	/**
	 * Fetch the relevant regen task for this block
	 * 
	 * @param blockLocation - Location of the block.
	 * @return the stored task, or null if there is none.
	 */
	public static ProtectionRegenTask GetProtectionRegenTask(BlockLocation blockLocation) {

		if (protectionRegenTasks.containsKey(blockLocation))
			return protectionRegenTasks.get(blockLocation);

		return null;
	}

	/**
	 * Add this task to the protection regen queue.
	 * 
	 * @param task - ProtectionRegenTask to add to queue
	 */
	public static void addProtectionRegenTask(ProtectionRegenTask task) {

		protectionRegenTasks.put(task.getBlockLocation(), task);
	}

	/**
	 * Remove this task form the protection regen queue
	 * 
	 * @param task - ProtectionRegenTask to remove from queue
	 */
	public static void removeProtectionRegenTask(ProtectionRegenTask task) {

		protectionRegenTasks.remove(task.getBlockLocation());
		if (protectionRegenTasks.isEmpty())
			protectionPlaceholders.clear();
	}

	/**
	 * Cancel all regenerating tasks and clear all queues.
	 */
	public static void cancelProtectionRegenTasks() {

		for (ProtectionRegenTask task : protectionRegenTasks.values()) {
			BukkitTools.getServer().getScheduler().cancelTask(task.getTaskId());
			task.replaceProtections();
		}
		protectionRegenTasks.clear();
		protectionPlaceholders.clear();
	}

	/**
	 * Is this a placholder block?
	 * 
	 * @param block - Block identifier
	 * @return true if it is a placeholder
	 */
	public static boolean isPlaceholder(Block block) {

		return protectionPlaceholders.contains(block);
	}

	/**
	 * Add this block as a placeholder (will be replaced when it's regeneration task occurs)
	 * 
	 * @param block - Block identifier
	 */
	public static void addPlaceholder(Block block) {

		protectionPlaceholders.add(block);
	}

	/**
	 * Remove this block from being tracked as a placeholder.
	 * 
	 * @param block - Block identifier
	 */
	public static void removePlaceholder(Block block) {

		protectionPlaceholders.remove(block);
	}

}