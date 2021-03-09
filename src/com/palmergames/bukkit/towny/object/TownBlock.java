package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.PlotChangeOwnerEvent;
import com.palmergames.bukkit.towny.event.PlotChangeTypeEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TownBlock extends TownyObject {

	private TownyWorld world;
	private Town town = null;
	private Resident resident = null;
	private TownBlockType type = TownBlockType.RESIDENTIAL;
	private int x, z;
	private double plotPrice = -1;
	private boolean locked = false;
	private boolean outpost = false;
	private PlotGroup plotGroup;

	//Plot level permissions
	protected TownyPermission permissions = new TownyPermission();
	protected boolean isChanged = false;
	
	public TownBlock(int x, int z, TownyWorld world) {
		super("");
		this.x = x;
		this.z = z;
		this.setWorld(world);
	}

	public void setTown(Town town) {

		if (hasTown())
			this.town.removeTownBlock(this);
		this.town = town;
		try {
			TownyUniverse.getInstance().addTownBlock(this);
			town.addTownBlock(this);
		} catch (AlreadyRegisteredException | NullPointerException ignored) {}
	}

	public Town getTown() throws NotRegisteredException {

		if (!hasTown())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", world.getName(), x, z));
		return town;
	}

	public boolean hasTown() {

		return town != null;
	}

	public void setResident(Resident resident) {
		boolean successful;
		try {
			if (hasResident())
				this.resident.removeTownBlock(this);
		} catch (NotRegisteredException ignored) {}
		this.resident = resident;
		try {
			resident.addTownBlock(this);
			successful = true;
		} catch (AlreadyRegisteredException | NullPointerException e) {
			successful = false;
		}
		if (successful) { //Should not cause a NPE, is checkingg if resident is null and
			// if "this.resident" returns null (Unclaimed / Wilderness) the PlotChangeOwnerEvent changes it to: "undefined"
			Bukkit.getPluginManager().callEvent(new PlotChangeOwnerEvent(this.resident, resident, this));
		}
		this.resident = resident;
	}

	public Resident getResident() throws NotRegisteredException {

		if (!hasResident())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a resident.", world.getName(), x, z));
		return resident;
	}

	public boolean hasResident() {

		return resident != null;
	}

	public boolean isOwner(TownBlockOwner owner) {

		try {
			if (owner == getTown())
				return true;
		} catch (NotRegisteredException ignored) {}

		try {
			if (owner == getResident())
				return true;
		} catch (NotRegisteredException ignored) {}

		return false;
	}

	public void setPlotPrice(double ForSale) {

		this.plotPrice = ForSale;

	}

	public double getPlotPrice() {

		return plotPrice;
	}

	public boolean isForSale() {

		return getPlotPrice() != -1.0;
	}

	public void setPermissions(String line) {

		//permissions.reset(); not needed, already done in permissions.load()
		permissions.load(line);
	}

	public TownyPermission getPermissions() {

		/*
		 * Return our perms
		 */
		return permissions;
	}

	/**
	 * Have the permissions been manually changed.
	 * 
	 * @return the isChanged
	 */
	public boolean isChanged() {

		return isChanged;
	}

	/**
	 * Flag the permissions as changed.
	 * 
	 * @param isChanged the isChanged to set
	 */
	public void setChanged(boolean isChanged) {

		this.isChanged = isChanged;
	}

	/**
	 * @return the outpost
	 */
	public boolean isOutpost() {

		return outpost;
	}

	/**
	 * @param outpost the outpost to set
	 */
	public void setOutpost(boolean outpost) {

		this.outpost = outpost;
	}

	public TownBlockType getType() {

		return type;
	}
	
	public void setType(TownBlockType type) {
		if (type != this.type)
			this.permissions.reset();

		if (type != null){
			Bukkit.getPluginManager().callEvent(new PlotChangeTypeEvent(this.type, type, this));
		}
		this.type = type;

		// Custom plot settings here
		switch (type) {
		
			case RESIDENTIAL:

			case COMMERCIAL:

			case EMBASSY:

			case BANK:

				if (this.hasResident()) {
					setPermissions(this.resident.getPermissions().toString());
				} else {
					setPermissions(this.town.getPermissions().toString());
				}
			
				break;

			case ARENA:
			
				setPermissions("pvp");
				break;

			case SPLEEF:

			case JAIL:

				setPermissions("denyAll");
				break;

			case INN:
			
				setPermissions("residentSwitch,nationSwitch,allySwitch,outsiderSwitch");
				break;
			
			case FARM:
			
			case WILDS:
				
				setPermissions("residentBuild,residentDestroy");
				break;
		}
			
		
		// Set the changed status.
		this.setChanged(false);
				
	}

	public void setType(int typeId) {

		setType(TownBlockType.lookup(typeId));
	}
	
	public void setType(TownBlockType type, Resident resident) throws TownyException {
		// Attempt to clear a jail spawn in case this was a jail plot until now.
		if (this.isJail())
			this.getTown().removeJailSpawn(this.getCoord());

		if ((getType().equals(TownBlockType.ARENA) || type.equals(TownBlockType.ARENA))
			&& TownySettings.getPVPCoolDownTime() > 0 
			&& !TownyUniverse.getInstance().getPermissionSource().testPermission(resident.getPlayer(), PermissionNodes.TOWNY_ADMIN.getNode())) {
			// Test to see if the pvp cooldown timer is active for this plot.
			if (CooldownTimerTask.hasCooldown(getWorldCoord().toString(), CooldownType.PVP))
				throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(getWorldCoord().toString(), CooldownType.PVP)));
			
			setType(type);
			CooldownTimerTask.addCooldownTimer(getWorldCoord().toString(), CooldownType.PVP);

		} else
			setType(type);

		if (this.isJail()) {
			Player p = TownyAPI.getInstance().getPlayer(resident);
			if (p == null)
				throw new TownyException(Translation.of("msg_err_not_part_town"));
				
			this.getTown().addJailSpawn(p.getLocation());
		}

		this.save();
	}

	public boolean isHomeBlock() {

		try {
			return getTown().isHomeBlock(this);
		} catch (NotRegisteredException e) {
			return false;
		}
	}
	
	@Override
	public void setName(String newName) {
		super.setName(newName.replace("_", " ")); 
	}

	public void setX(int x) {

		this.x = x;
	}

	public int getX() {

		return x;
	}

	public void setZ(int z) {

		this.z = z;
	}

	public int getZ() {

		return z;
	}

	public Coord getCoord() {

		return new Coord(x, z);
	}

	public WorldCoord getWorldCoord() {

		return new WorldCoord(world.getName(), x, z);
	}

	/**
	 * Is the TownBlock locked
	 * 
	 * @return the locked
	 */
	public boolean isLocked() {

		return locked;
	}

	/**
	 * @param locked is the to locked to set
	 */
	public void setLocked(boolean locked) {

		this.locked = locked;
	}

	public void setWorld(TownyWorld world) {

		this.world = world;
	}

	public TownyWorld getWorld() {

		return world;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TownBlock townBlock = (TownBlock) o;
		return x == townBlock.x &&
			z == townBlock.z &&
			world.equals(townBlock.world);
	}

	@Override
	public int hashCode() {
		return Objects.hash(world, x, z);
	}

	public void clear() {

		setTown(null);
		setResident(null);
		setWorld(null);
	}

	@Override
	public String toString() {

		return getWorld().getName() + " (" + getCoord() + ")";
	}

	/*
	 * Flag war only.
	 */
	public boolean isWarZone() {

		return getWorld().isFlagWarZone(getCoord());
	}

	public boolean isJail() {

		return this.getType() == TownBlockType.JAIL;
	}
	
	@Override
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, true);
	}
	
	@Override
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, true);
	}
	
	public boolean hasPlotObjectGroup() { return plotGroup != null; }
	
	public PlotGroup getPlotObjectGroup() {
		return plotGroup;
	}
	
	public void removePlotObjectGroup() {
		this.plotGroup = null;
	}

	public void setPlotObjectGroup(PlotGroup group) {
		this.plotGroup = group;

		try {
			group.addTownBlock(this);
		} catch (NullPointerException e) {
			TownyMessaging.sendErrorMsg("Townblock failed to setPlotObjectGroup(group), group is null. " + group);
		}
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveTownBlock(this);
	}
}