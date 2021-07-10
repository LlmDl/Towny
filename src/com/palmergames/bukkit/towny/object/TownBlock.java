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
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.JailUtil;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private long claimedAt;
	private Jail jail;
	private final Map<Resident, PermissionData> permissionOverrides = new HashMap<>();
	private List<Resident> trustedResidents = new ArrayList<>();

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
		setTown(town, true);
	}

	public void setTown(Town town, boolean updateClaimedAt) {

		if (hasTown())
			this.town.removeTownBlock(this);
		this.town = town;
		try {
			TownyUniverse.getInstance().addTownBlock(this);
			town.addTownBlock(this);

			if (updateClaimedAt)
				setClaimedAt(System.currentTimeMillis());
			
			permissionOverrides.clear();
		} catch (AlreadyRegisteredException | NullPointerException ignored) {}
	}

	public Town getTown() throws NotRegisteredException {

		if (!hasTown())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", world.getName(), x, z));
		return town;
	}
	
	/**
	 * Relatively safe to use after confirming {@link #hasTown()} is true
	 * or {@link TownyAPI#isWilderness(org.bukkit.Location)} is false.
	 * 
	 * @return Town who owns the TownBlock or null.
	 */
	@Nullable 
	public Town getTownOrNull() {
		return town;
	}

	public boolean hasTown() {

		return town != null;
	}

	public void setResident(Resident resident) {
		boolean successful;
		if (hasResident())
			this.resident.removeTownBlock(this);
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
		permissionOverrides.clear();
	}

	public Resident getResident() throws NotRegisteredException {

		if (!hasResident())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a resident.", world.getName(), x, z));
		return resident;
	}

	/**
	 * Relatively safe to use after testing {@link #hasResident()}.
	 * 
	 * @return Resident who personally owns the TownBlock or null.
	 */
	@Nullable
	public Resident getResidentOrNull() {
		return resident;
	}
	
	public boolean hasResident() {

		return resident != null;
	}

	public boolean isOwner(TownBlockOwner owner) {

		if (hasTown() && owner == getTownOrNull())
			return true;

		if (hasResident() && owner == getResidentOrNull())
			return true;

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

			case INN:

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

		// Delete a jail if this is no longer going to be a jail.
		if (this.isJail() && !type.equals(TownBlockType.JAIL)) {			
			TownyUniverse.getInstance().getDataSource().removeJail(getJail());
			setJail(null);
		}

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

		if (this.isJail() && resident.getPlayer() != null)
			JailUtil.createJailPlot(this, getTown(), resident.getPlayer().getLocation());

		this.save();
	}

	public boolean isHomeBlock() {
		return hasTown() && getTownOrNull().isHomeBlock(this);
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

	public boolean isWarZone() {

		return getWorld().isWarZone(getCoord());
	}

	public boolean isJail() {

		return this.getType() == TownBlockType.JAIL;
	}
	
	public Jail getJail() {
		return jail;
	}

	public void setJail(Jail jail) {
		this.jail = jail;
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

	public long getClaimedAt() {
		return claimedAt;
	}

	public void setClaimedAt(long claimedAt) {
		this.claimedAt = claimedAt;
	}

	public Map<Resident, PermissionData> getPermissionOverrides() {
		return permissionOverrides;
	}

	public List<Resident> getTrustedResidents() {
		return trustedResidents;
	}
	
	public boolean hasTrustedResident(Resident resident) {
		return trustedResidents.contains(resident);
	}
	
	public void addTrustedResident(Resident resident) throws AlreadyRegisteredException {
		if (trustedResidents.contains(resident))
			throw new AlreadyRegisteredException();
		
		trustedResidents.add(resident);
	}
	
	public void removeTrustedResident(Resident resident) {
		trustedResidents.remove(resident);
	}
	
	public boolean hasResident(Resident resident) {
		if (this.resident == null || resident == null)
			return false;
		
		return resident.equals(this.resident);
	}
	
	//TODO: Figure out how we're going to handle permissionOverrides/Trusted residents on the PlotGroup level.
}