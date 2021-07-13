package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PlayerCache {

	private final HashMap<Material, Boolean> buildMatPermission = new HashMap<>();
	private final HashMap<Material, Boolean> destroyMatPermission = new HashMap<>();
	private final HashMap<Material, Boolean> switchMatPermission = new HashMap<>();
	private final HashMap<Material, Boolean> itemUseMatPermission = new HashMap<>();

	private WorldCoord lastWorldCoord;
	private String blockErrMsg;
	private Location lastLocation;

	//TODO: cache last entity attacked

	public PlayerCache(TownyWorld world, Player player) {

		this(new WorldCoord(world.getName(), Coord.parseCoord(player)));
		setLastLocation(player.getLocation());
	}

	public PlayerCache(WorldCoord worldCoord) {

		this.setLastTownBlock(worldCoord);
	}

	/**
	 * Update the cache with new coordinates.
	 * 
	 * @param worldCoord - World Coordinate to set as the lastWorldCoord
	 */
	public void setLastTownBlock(WorldCoord worldCoord) {

		this.lastWorldCoord = worldCoord;
	}
	
	/**
	 * Reset the cache permissions and update the cache with new coordinates.
	 * 
	 * @param worldCoord - World Coordinate to setLastTownBlock
	 */
	public void resetAndUpdate(WorldCoord worldCoord) {
		
		reset();
		setLastTownBlock(worldCoord);
	}

	/**
	 * Retrieve the last cached WorldCoord
	 * 
	 * @return WorldCoord of the last acted upon TownBlock
	 */
	public WorldCoord getLastTownBlock() {

		return lastWorldCoord;
	}
	
	/**
	 * Update the players WorldCoord, resetting all permissions if it has changed.
	 * 
	 * @param pos - WorldCoord to setLastTownBlock
	 * @return true if changed.
	 */
	public boolean updateCoord(WorldCoord pos) {

		if (!getLastTownBlock().equals(pos)) {
			reset();
			setLastTownBlock(pos);
			return true;
		} else
			return false;
	}

	/**
	 * Checks from cache if a certain ActionType can be performed on a given Material
	 * 
	 * @param material - Material to check
	 * @param action - ActionType to check
	 * @return true if permission to perform an ActionType based on the material is granted
	 * @throws NullPointerException if passed an invalid or NULL ActionType
	 */
	public boolean getCachePermission(Material material, ActionType action) throws NullPointerException {

		switch (action) {

		case BUILD: // BUILD
			return getBuildPermission(material);

		case DESTROY: // DESTROY
			return getDestroyPermission(material);

		case SWITCH: // SWITCH
			return getSwitchPermission(material);

		case ITEM_USE: // ITEM_USE
			return getItemUsePermission(material);

		default:
			throw new NullPointerException();

		}

	}

	public void setBuildPermission(Material material, Boolean value) {

		updateMaps(buildMatPermission, material, value);

	}
	public void setDestroyPermission(Material material, Boolean value) {

		updateMaps(destroyMatPermission, material, value);
	}
	public void setSwitchPermission(Material material, Boolean value) {

		updateMaps(switchMatPermission, material, value);

	}
	public void setItemUsePermission(Material material, Boolean value) {

		updateMaps(itemUseMatPermission, material, value);
		
	}
	
	public boolean getBuildPermission(Material material) throws NullPointerException {

		return getBlockPermission(buildMatPermission, material);

	}
	public boolean getDestroyPermission(Material material) throws NullPointerException {

		return getBlockPermission(destroyMatPermission, material);
		
	}
	public boolean getSwitchPermission(Material material) throws NullPointerException {

		return getBlockPermission(switchMatPermission, material);
		
	}
	public Boolean getItemUsePermission(Material material) throws NullPointerException {

		return getBlockPermission(itemUseMatPermission, material);
		
	}
	
	private void updateMaps(HashMap<Material, Boolean> blockMap, Material material, Boolean value) {
		
		if (!blockMap.containsKey(material)) {
			/*
			 * We have no permissions cached for this block.
			 */			
			blockMap.put(material, value);
		} else {
			/*
			 * We have cached permissions for this block type so just push updated data.
			 */
			blockMap.get(material);
		}
	}
	
	private boolean getBlockPermission(HashMap<Material, Boolean> blockMap, Material material) throws NullPointerException {
		
		if (!blockMap.containsKey(material))
			throw new NullPointerException();
		
		return blockMap.get(material);
		
	}

	private void reset() {

		lastWorldCoord = null;
		townBlockStatus = null;
		blockErrMsg = null;
		
		// Clear all maps
		buildMatPermission.clear();
		destroyMatPermission.clear();
		switchMatPermission.clear();
		itemUseMatPermission.clear();
	}

	public enum TownBlockStatus {
		UNKNOWN, NOT_REGISTERED, OFF_WORLD, // In a world untouched by towny.
		ADMIN,
		UNCLAIMED_ZONE,
		NATION_ZONE,
		LOCKED,
		WARZONE,
		OUTSIDER,
		PLOT_OWNER,
		PLOT_FRIEND,
		PLOT_TOWN,
		PLOT_ALLY,
		TOWN_OWNER,
		TOWN_RESIDENT,
		TOWN_ALLY,
		TOWN_NATION,
		ENEMY,
		TOWN_TRUSTED,
		PLOT_TRUSTED,
		@Deprecated UNKOWN,
	}

	private TownBlockStatus townBlockStatus = TownBlockStatus.UNKNOWN;

	public void setStatus(TownBlockStatus townBlockStatus) {

		this.townBlockStatus = townBlockStatus;
	}

	public TownBlockStatus getStatus() throws NullPointerException {

		if (townBlockStatus == null)
			throw new NullPointerException();
		else
			return townBlockStatus;
	}

	public void setBlockErrMsg(String blockErrMsg) {

		this.blockErrMsg = blockErrMsg;
	}

	public String getBlockErrMsg() {

		String temp = blockErrMsg;
		setBlockErrMsg(null); // Delete error msg after reading it.
		return temp;
	}

	public boolean hasBlockErrMsg() {

		return blockErrMsg != null;
	}

	public void setLastLocation(Location lastLocation) {

		this.lastLocation = lastLocation.clone();
	}

	public Location getLastLocation() throws NullPointerException {

		if (lastLocation == null)
			throw new NullPointerException();
		else
			return lastLocation;
	}
}
