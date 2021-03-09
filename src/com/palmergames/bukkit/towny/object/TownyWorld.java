package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.util.MathUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TownyWorld extends TownyObject {

	private HashMap<String, Town> towns = new HashMap<>();

	private boolean isUsingPlotManagementDelete = TownySettings.isUsingPlotManagementDelete();
	private List<String> plotManagementDeleteIds = null;
	
	private boolean isUsingPlotManagementMayorDelete = TownySettings.isUsingPlotManagementMayorDelete();
	private List<String> plotManagementMayorDelete = null;
	
	private boolean isUsingPlotManagementRevert = TownySettings.isUsingPlotManagementRevert();
	private List<String> plotManagementIgnoreIds = null;

	private boolean isUsingPlotManagementWildEntityRevert = TownySettings.isUsingPlotManagementWildEntityRegen();	
	private long plotManagementWildRevertDelay = TownySettings.getPlotManagementWildRegenDelay();
	private List<String> entityExplosionProtection = null;
	
	private boolean isUsingPlotManagementWildBlockRevert = TownySettings.isUsingPlotManagementWildBlockRegen();
	private List<String> blockExplosionProtection = null;
	
	private List<String> unclaimedZoneIgnoreBlockMaterials = null;
	private Boolean unclaimedZoneBuild = null, unclaimedZoneDestroy = null,
			unclaimedZoneSwitch = null, unclaimedZoneItemUse = null;

	private String unclaimedZoneName = null;
	private List<Coord> warZones = new ArrayList<>();
	
	private boolean isUsingTowny = TownySettings.isUsingTowny();
	private boolean isClaimable = true;
	private boolean isWarAllowed = TownySettings.isWarAllowed();
	private boolean isPVP = TownySettings.isPvP();
	private boolean isForcePVP = TownySettings.isForcingPvP();
	private boolean isFriendlyFire = TownySettings.isFriendlyFireEnabled();
	private boolean isFire = TownySettings.isFire();
	private boolean isForceFire = TownySettings.isForcingFire();
	private boolean hasWorldMobs = TownySettings.isWorldMonstersOn();
	private boolean hasWildernessMonsters = TownySettings.isWildernessMonstersOn();
	private boolean isForceTownMobs = TownySettings.isForcingMonsters();
	private boolean isExplosion = TownySettings.isExplosions();
	private boolean isForceExpl = TownySettings.isForcingExplosions();
	private boolean isEndermanProtect = TownySettings.getEndermanProtect();
	
	private boolean isDisablePlayerTrample = TownySettings.isPlayerTramplingCropsDisabled();
	private boolean isDisableCreatureTrample = TownySettings.isCreatureTramplingCropsDisabled();
	
	public Map<Location, Material> bedMap = new HashMap<>();
	public List<String> tridentStrikeMap = new ArrayList<>();

	public TownyWorld(String name) {
		super(name);
	}

	public HashMap<String, Town> getTowns() {

		return towns;
	}

	public boolean hasTowns() {

		return !towns.isEmpty();
	}

	public boolean hasTown(String name) {

		return towns.containsKey(name);
	}

	public boolean hasTown(Town town) {

		return hasTown(town.getName());
	}

	public void addTown(Town town) throws AlreadyRegisteredException {

		if (hasTown(town))
			throw new AlreadyRegisteredException();
		else {
			towns.put(town.getName(), town);
			town.setWorld(this);
		}
	}

	public TownBlock getTownBlock(Coord coord) throws NotRegisteredException {
		if (!hasTownBlock(coord))
			throw new NotRegisteredException();
		return TownyUniverse.getInstance().getTownBlock(new WorldCoord(this.getName(), coord));
	}

	public boolean hasTownBlock(Coord key) {

		return TownyUniverse.getInstance().hasTownBlock(new WorldCoord(this.getName(), key));
	}

	public TownBlock getTownBlock(int x, int z) throws NotRegisteredException {

		return getTownBlock(new Coord(x, z));
	}

	public List<TownBlock> getTownBlocks(Town town) {

		List<TownBlock> out = new ArrayList<>();
		for (TownBlock townBlock : town.getTownBlocks())
			if (townBlock.getWorld() == this)
				out.add(townBlock);
		return out;
	}

	/*
	 * Used only in the getTreeString() method.
	 */
	public Collection<TownBlock> getTownBlocks() {

		List<TownBlock> townBlocks = new ArrayList<>();
		for (TownBlock townBlock : TownyUniverse.getInstance().getTownBlocks().values())
			if (townBlock.getWorld() == this)
				townBlocks.add(townBlock);
		return townBlocks;
	}

	public void removeTown(Town town) throws NotRegisteredException {

		if (!hasTown(town))
			throw new NotRegisteredException();
		else {
			towns.remove(town.getName());
			/*
			 * try {
			 * town.setWorld(null);
			 * } catch (AlreadyRegisteredException e) {
			 * }
			 */
		}
	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<>();
		out.add(getTreeDepth(depth) + "World (" + getName() + ")");
		out.add(getTreeDepth(depth + 1) + "TownBlocks (" + getTownBlocks().size() + "): " /*
																						 * +
																						 * getTownBlocks
																						 * (
																						 * )
																						 */);
		return out;
	}

	public void setWarAllowed(boolean isWarAllowed) {

		this.isWarAllowed = isWarAllowed;
	}

	public boolean isWarAllowed() {

		return this.isWarAllowed;
	}

	public void setPVP(boolean isPVP) {

		this.isPVP = isPVP;
	}

	public boolean isPVP() {

		return this.isPVP;
	}

	public void setForcePVP(boolean isPVP) {

		this.isForcePVP = isPVP;
	}

	public boolean isForcePVP() {

		return this.isForcePVP;
	}

	public void setExpl(boolean isExpl) {

		this.isExplosion = isExpl;
	}

	public boolean isExpl() {

		return isExplosion;
	}

	public void setForceExpl(boolean isExpl) {

		this.isForceExpl = isExpl;
	}

	public boolean isForceExpl() {

		return isForceExpl;
	}

	public void setFire(boolean isFire) {

		this.isFire = isFire;
	}

	public boolean isFire() {

		return isFire;
	}

	public void setForceFire(boolean isFire) {

		this.isForceFire = isFire;
	}

	public boolean isForceFire() {

		return isForceFire;
	}

	public void setDisablePlayerTrample(boolean isDisablePlayerTrample) {

		this.isDisablePlayerTrample = isDisablePlayerTrample;
	}

	public boolean isDisablePlayerTrample() {

		return isDisablePlayerTrample;
	}

	public void setDisableCreatureTrample(boolean isDisableCreatureTrample) {

		this.isDisableCreatureTrample = isDisableCreatureTrample;
	}

	public boolean isDisableCreatureTrample() {

		return isDisableCreatureTrample;
	}

	public void setWorldMobs(boolean hasMobs) {

		this.hasWorldMobs = hasMobs;
	}

	public boolean hasWorldMobs() {

		return this.hasWorldMobs;
	}
	
	public void setWildernessMobs(boolean hasMonsters) {
		
		this.hasWildernessMonsters = hasMonsters;
	}
	
	public boolean hasWildernessMobs() {
		
		return this.hasWildernessMonsters;
	}

	public void setForceTownMobs(boolean setMobs) {

		this.isForceTownMobs = setMobs;
	}

	public boolean isForceTownMobs() {

		return isForceTownMobs;
	}

	public void setEndermanProtect(boolean setEnder) {

		this.isEndermanProtect = setEnder;
	}

	public boolean isEndermanProtect() {

		return isEndermanProtect;
	}

	public void setClaimable(boolean isClaimable) {

		this.isClaimable = isClaimable;
	}

	public boolean isClaimable() {

		if (!isUsingTowny())
			return false;
		else
			return isClaimable;
	}

	public void setUsingDefault() {

		setUnclaimedZoneBuild(null);
		setUnclaimedZoneDestroy(null);
		setUnclaimedZoneSwitch(null);
		setUnclaimedZoneItemUse(null);
		setUnclaimedZoneIgnore(null);
		setUnclaimedZoneName(null);
	}

	public void setUsingPlotManagementDelete(boolean using) {

		isUsingPlotManagementDelete = using;
	}

	public boolean isUsingPlotManagementDelete() {

		return isUsingPlotManagementDelete;
	}

	public void setUsingPlotManagementMayorDelete(boolean using) {

		isUsingPlotManagementMayorDelete = using;
	}

	public boolean isUsingPlotManagementMayorDelete() {

		return isUsingPlotManagementMayorDelete;
	}

	public void setUsingPlotManagementRevert(boolean using) {

		isUsingPlotManagementRevert = using;
	}

	public boolean isUsingPlotManagementRevert() {

		return isUsingPlotManagementRevert;
	}

	public List<String> getPlotManagementDeleteIds() {

		if (plotManagementDeleteIds == null)
			return TownySettings.getPlotManagementDeleteIds();
		else
			return plotManagementDeleteIds;
	}

	public boolean isPlotManagementDeleteIds(String id) {

		return getPlotManagementDeleteIds().contains(id);
	}

	public void setPlotManagementDeleteIds(List<String> plotManagementDeleteIds) {

		this.plotManagementDeleteIds = plotManagementDeleteIds;
	}

	public List<String> getPlotManagementMayorDelete() {

		if (plotManagementMayorDelete == null)
			return TownySettings.getPlotManagementMayorDelete();
		else
			return plotManagementMayorDelete;
	}

	public boolean isPlotManagementMayorDelete(String material) {

		return getPlotManagementMayorDelete().contains(material.toUpperCase());
	}

	public void setPlotManagementMayorDelete(List<String> plotManagementMayorDelete) {

		this.plotManagementMayorDelete = plotManagementMayorDelete;
	}

	public List<String> getPlotManagementIgnoreIds() {
		
		if (plotManagementIgnoreIds == null)
			return TownySettings.getPlotManagementIgnoreIds();
		else
			return plotManagementIgnoreIds;
	}

	public boolean isPlotManagementIgnoreIds(Material mat) {
		return getPlotManagementIgnoreIds().contains(mat.toString());
	}

	public void setPlotManagementIgnoreIds(List<String> plotManagementIgnoreIds) {

		this.plotManagementIgnoreIds = plotManagementIgnoreIds;
	}

	/**
	 * @return the isUsingPlotManagementWildEntityRevert
	 */
	public boolean isUsingPlotManagementWildEntityRevert() {

		return isUsingPlotManagementWildEntityRevert;
	}
	
	/**
	 * @return the isUsingPlotManagementWildBlockRevert
	 */
	public boolean isUsingPlotManagementWildBlockRevert() {

		return isUsingPlotManagementWildBlockRevert;
	}

	/**
	 * @param isUsingPlotManagementWildEntityRevert the
	 *            isUsingPlotManagementWildRevert to set
	 */
	public void setUsingPlotManagementWildEntityRevert(boolean isUsingPlotManagementWildEntityRevert) {

		this.isUsingPlotManagementWildEntityRevert = isUsingPlotManagementWildEntityRevert;
	}
	
	/**
	 * @param isUsingPlotManagementWildBlockRevert the
	 *            isUsingPlotManagementWildBlockRevert to set
	 */
	public void setUsingPlotManagementWildBlockRevert(boolean isUsingPlotManagementWildBlockRevert) {

		this.isUsingPlotManagementWildBlockRevert = isUsingPlotManagementWildBlockRevert;
	}

	/**
	 * @return the plotManagementWildRevertDelay
	 */
	public long getPlotManagementWildRevertDelay() {

		return plotManagementWildRevertDelay;
	}

	/**
	 * @param plotManagementWildRevertDelay the plotManagementWildRevertDelay to
	 *            set
	 */
	public void setPlotManagementWildRevertDelay(long plotManagementWildRevertDelay) {

		this.plotManagementWildRevertDelay = plotManagementWildRevertDelay;
	}

	public void setPlotManagementWildRevertEntities(List<String> entities) {

		entityExplosionProtection = new ArrayList<>();

		for (String mob : entities)
			if (!mob.equals("")) {
				entityExplosionProtection.add(mob.toLowerCase());
			}

	}

	public List<String> getPlotManagementWildRevertEntities() {

		if (entityExplosionProtection == null)
			setPlotManagementWildRevertEntities(TownySettings.getWildExplosionProtectionEntities());

		return entityExplosionProtection;
	}

	public boolean isProtectingExplosionEntity(Entity entity) {

		if (entityExplosionProtection == null)
			setPlotManagementWildRevertEntities(TownySettings.getWildExplosionProtectionEntities());

		return (entityExplosionProtection.contains(entity.getType().getEntityClass().getSimpleName().toLowerCase()));

	}

	public void setPlotManagementWildRevertMaterials(List<String> mats) {

		blockExplosionProtection = new ArrayList<>();

		for (String mat : mats)
			if (!mat.equals(""))
				blockExplosionProtection.add(mat);

	}

	public List<String> getPlotManagementWildRevertBlocks() {

		if (blockExplosionProtection == null)
			setPlotManagementWildRevertMaterials(TownySettings.getWildExplosionProtectionBlocks());

		return blockExplosionProtection;
	}

	public boolean isProtectingExplosionBlock(Material material) {

		if (blockExplosionProtection == null)
			setPlotManagementWildRevertMaterials(TownySettings.getWildExplosionProtectionBlocks());

		return (blockExplosionProtection.contains(material.toString()));

	}

	public void setUnclaimedZoneIgnore(List<String> unclaimedZoneIgnoreIds) {

		this.unclaimedZoneIgnoreBlockMaterials = unclaimedZoneIgnoreIds;
	}
	
	public List<String> getUnclaimedZoneIgnoreMaterials() {

		if (unclaimedZoneIgnoreBlockMaterials == null)
			return TownySettings.getUnclaimedZoneIgnoreMaterials();
		else
			return unclaimedZoneIgnoreBlockMaterials;
	}

	@SuppressWarnings("unlikely-arg-type")
	public boolean isUnclaimedZoneIgnoreMaterial(Material mat) {

		return getUnclaimedZoneIgnoreMaterials().contains(mat);
	}


	public boolean getUnclaimedZonePerm(ActionType type) {

		switch (type) {
		case BUILD:
			return this.getUnclaimedZoneBuild();
		case DESTROY:
			return this.getUnclaimedZoneDestroy();
		case SWITCH:
			return this.getUnclaimedZoneSwitch();
		case ITEM_USE:
			return this.getUnclaimedZoneItemUse();
		default:
			throw new UnsupportedOperationException();
		}
	}

	public Boolean getUnclaimedZoneBuild() {

		if (unclaimedZoneBuild == null)
			return TownySettings.getUnclaimedZoneBuildRights();
		else
			return unclaimedZoneBuild;
	}

	public void setUnclaimedZoneBuild(Boolean unclaimedZoneBuild) {

		this.unclaimedZoneBuild = unclaimedZoneBuild;
	}

	public Boolean getUnclaimedZoneDestroy() {

		if (unclaimedZoneDestroy == null)
			return TownySettings.getUnclaimedZoneDestroyRights();
		else
			return unclaimedZoneDestroy;
	}

	public void setUnclaimedZoneDestroy(Boolean unclaimedZoneDestroy) {

		this.unclaimedZoneDestroy = unclaimedZoneDestroy;
	}

	public Boolean getUnclaimedZoneSwitch() {

		if (unclaimedZoneSwitch == null)
			return TownySettings.getUnclaimedZoneSwitchRights();
		else
			return unclaimedZoneSwitch;
	}

	public void setUnclaimedZoneSwitch(Boolean unclaimedZoneSwitch) {

		this.unclaimedZoneSwitch = unclaimedZoneSwitch;
	}

	public String getUnclaimedZoneName() {

		if (unclaimedZoneName == null)
			return TownySettings.getUnclaimedZoneName();
		else
			return unclaimedZoneName;
	}

	public void setUnclaimedZoneName(String unclaimedZoneName) {

		this.unclaimedZoneName = unclaimedZoneName;
	}

	public void setUsingTowny(boolean isUsingTowny) {

		this.isUsingTowny = isUsingTowny;
	}

	public boolean isUsingTowny() {

		return isUsingTowny;
	}

	public void setUnclaimedZoneItemUse(Boolean unclaimedZoneItemUse) {

		this.unclaimedZoneItemUse = unclaimedZoneItemUse;
	}

	public Boolean getUnclaimedZoneItemUse() {

		if (unclaimedZoneItemUse == null)
			return TownySettings.getUnclaimedZoneItemUseRights();
		else
			return unclaimedZoneItemUse;
	}

	/**
	 * Checks the distance from the closest homeblock.
	 * 
	 * @param key - Coord to check from.
	 * @return the distance to nearest towns homeblock.
	 */
	public int getMinDistanceFromOtherTowns(Coord key) {

		return getMinDistanceFromOtherTowns(key, null);

	}

	/**
	 * Checks the distance from a another town's homeblock.
	 * 
	 * @param key - Coord to check from.
	 * @param homeTown Players town
	 * @return the closest distance to another towns homeblock.
	 */
	public int getMinDistanceFromOtherTowns(Coord key, Town homeTown) {
		double minSqr = -1;
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		for (Town town : getTowns().values()) {
			try {
				Coord townCoord = town.getHomeBlock().getCoord();
				if (homeTown != null)
					// If the townblock either: the town is the same as homeTown OR
					// both towns are in the same nation (and this is set to ignore distance in the config,) skip over the proximity filter.
					if (homeTown.getUUID().equals(town.getUUID())
						|| (TownySettings.isMinDistanceIgnoringTownsInSameNation() && homeTown.hasNation() && town.hasNation() && town.getNation().equals(homeTown.getNation()))
						|| (TownySettings.isMinDistanceIgnoringTownsInAlliedNation() && homeTown.isAlliedWith(town)))
						continue;
				
				if (!town.getHomeblockWorld().equals(this)) continue;
				
				final double distSqr = MathUtil.distanceSquared((double) townCoord.getX() - keyX, (double) townCoord.getZ() - keyZ);
				if (minSqr == -1 || distSqr < minSqr)
					minSqr = distSqr;
			} catch (TownyException e) {
			}
		}
		return minSqr == -1 ? Integer.MAX_VALUE : (int) Math.ceil(Math.sqrt(minSqr));
	}

	/**
	 * Checks the distance from the closest town block.
	 * 
	 * @param key - Coord to check from.
	 * @return the distance to nearest town's townblock.
	 */
	public int getMinDistanceFromOtherTownsPlots(Coord key) {

		return getMinDistanceFromOtherTownsPlots(key, null);
	}

	/**
	 * Checks the distance from a another town's plots.
	 * 
	 * @param key - Coord to check from.
	 * @param homeTown Players town
	 * @return the closest distance to another towns nearest plot.
	 */
	public int getMinDistanceFromOtherTownsPlots(Coord key, Town homeTown) {
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		double minSqr = -1;
		for (Town town : getTowns().values()) {
			try {
				if (homeTown != null)
					// If the townblock either: the town is the same as homeTown OR 
					// both towns are in the same nation (and this is set to ignore distance in the config,) skip over the proximity filter.
					if (homeTown.getUUID().equals(town.getUUID())
						|| (TownySettings.isMinDistanceIgnoringTownsInSameNation() && homeTown.hasNation() && town.hasNation() && town.getNation().equals(homeTown.getNation()))
						|| (TownySettings.isMinDistanceIgnoringTownsInAlliedNation() && homeTown.isAlliedWith(town)))
						continue;
				for (TownBlock b : town.getTownBlocks()) {
					if (!b.getWorld().equals(this)) continue;

					final int tbX = b.getX();
					final int tbZ = b.getZ();
					
					if (keyX == tbX && keyZ == tbZ)
						continue;
					
					final double distSqr = MathUtil.distanceSquared((double) tbX - keyX, (double) tbZ - keyZ);
					if (minSqr == -1 || distSqr < minSqr)
						minSqr = distSqr;
				}
			} catch (TownyException e) {
			}
		}
		return minSqr == -1 ? Integer.MAX_VALUE : (int) Math.ceil(Math.sqrt(minSqr));
	}
	
	/**
	 * Returns the closest town with a nation from a given coord (key).
	 * 
	 * @param key - Coord.
	 * @param nearestTown - Closest town to given coord.
	 * @return the nearest town belonging to a nation.   
	 */
	public Town getClosestTownWithNationFromCoord(Coord key, Town nearestTown) {
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		double minSqr = -1;
		for (Town town : getTowns().values()) {
			if (!town.hasNation()) continue;
			for (TownBlock b : town.getTownBlocks()) {
				if (!b.getWorld().equals(this)) continue;
				
				final int tbX = b.getX();
				final int tbZ = b.getZ();
				
				double distSqr = MathUtil.distanceSquared((double) tbX - keyX, (double) tbZ - keyZ);
				if (minSqr == -1 || distSqr < minSqr) {
					minSqr = distSqr;
					nearestTown = town;
				}						
			}		
		}		
		return (nearestTown);		
	}

	/**
	 * Get the town block that belongs to the closest town with a nation
	 * from the specified coord.
	 * 
	 * @param key - Coordinate to compare distance to
	 * @return The nearest townblock that belongs to a town or
	 * null if there are no towns in the world.
	 */
	@Nullable
	public TownBlock getClosestTownblockWithNationFromCoord(Coord key) {
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		double minSqr = -1;
		TownBlock tb = null;
		
		for (Town town : getTowns().values()) {
			if (!town.hasNation())
				continue;
			for (TownBlock b : town.getTownBlocks()) {
				if (!b.getWorld().equals(this))
					continue;

				final int tbX = b.getX();
				final int tbZ = b.getZ();
				
				double distSqr = MathUtil.distanceSquared((double) tbX - keyX, (double) tbZ - keyZ);
				if (minSqr == -1 || distSqr < minSqr) {
					minSqr = distSqr;
					tb = b;
				}
			}
		}
		
		return tb;
	}

	/*
	 * Flag War Only.
	 */
	public void addFlagWarZone(Coord coord) {

		if (!isFlagWarZone(coord))
			warZones.add(coord);
	}

	/*
	 * Flag War Only.
	 */
	public void removeFlagWarZone(Coord coord) {

		warZones.remove(coord);
	}

	/*
	 * Flag War Only.
	 */
	public boolean isFlagWarZone(Coord coord) {

		return warZones.contains(coord);
	}

	@Override
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, true);
	}

	@Override
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, true);
	}
	
	/**
	 * Does this world have an exploded bet at the location?
	 * @param location Location to test.
	 * @return true when the bed map contains the location.
	 */
	public boolean hasBedExplosionAtBlock(Location location) {
		return bedMap.containsKey(location);
	}

	/**
	 * Gets the exploded bed material.
	 * @param location Location to get the material.
	 * @return material of the bed or null if the bedMap doesn't contain the location.
	 */
	@Nullable
	public Material getBedExplosionMaterial(Location location) {
		if (hasBedExplosionAtBlock(location))
			return bedMap.get(location);
		return null;
	}
	
	public void addBedExplosionAtBlock(Location location, Material material) {
		bedMap.put(location, material);
	}

	public void removeBedExplosionAtBlock(Location location) {
		if (hasBedExplosionAtBlock(location))
			bedMap.remove(location);
	}
	
	public boolean hasTridentStrike(int entityID) {
		return tridentStrikeMap.contains(String.valueOf(entityID));
	}

	public void addTridentStrike(int entityID) {
		tridentStrikeMap.add(String.valueOf(entityID));
	}
	
	public void removeTridentStrike(int entityID) {
		if (hasTridentStrike(entityID))
			tridentStrikeMap.remove(String.valueOf(entityID));
	}

	public void setFriendlyFire(boolean parseBoolean) {
		isFriendlyFire = parseBoolean;
		
	}
	
	public boolean isFriendlyFireEnabled( ) {
		return isFriendlyFire;
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveWorld(this);
	}
}
