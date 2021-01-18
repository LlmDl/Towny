package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Nation extends Government {

	private static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getNationAccountPrefix();

	private final List<Town> towns = new ArrayList<>();
	private List<Nation> allies = new ArrayList<>();
	private List<Nation> enemies = new ArrayList<>();
	private Town capital;
	private String mapColorHexCode = "";
	private Location nationSpawn;
	private final transient List<Invite> sentAllyInvites = new ArrayList<>();

	public Nation(String name) {
		super(name);
		
		// Set defaults
		setBoard(TownySettings.getNationDefaultBoard());
		setOpen(TownySettings.getNationDefaultOpen());
	}

	public void addAlly(Nation nation) throws AlreadyRegisteredException {

		if (hasAlly(nation))
			throw new AlreadyRegisteredException();
		else {
			try {
				removeEnemy(nation);
			} catch (NotRegisteredException ignored) {}
			getAllies().add(nation);
		}
	}

	public boolean removeAlly(Nation nation) throws NotRegisteredException {

		if (!hasAlly(nation))
			throw new NotRegisteredException();
		else
			return getAllies().remove(nation);
	}

	public boolean removeAllAllies() {

		for (Nation ally : new ArrayList<>(getAllies()))
			try {
				removeAlly(ally);
				ally.removeAlly(this);
			} catch (NotRegisteredException ignored) {}
		return getAllies().size() == 0;
	}

	public boolean hasAlly(Nation nation) {

		return getAllies().contains(nation);
	}

	public boolean hasMutualAlly(Nation nation) {
		
		return getAllies().contains(nation) && nation.getAllies().contains(this);
	}

	public boolean IsAlliedWith(Nation nation) {

		return getAllies().contains(nation);
	}

	public void addEnemy(Nation nation) throws AlreadyRegisteredException {

		if (hasEnemy(nation))
			throw new AlreadyRegisteredException();
		else {
			try {
				removeAlly(nation);
			} catch (NotRegisteredException ignored) {}
			getEnemies().add(nation);
		}

	}

	public boolean removeEnemy(Nation nation) throws NotRegisteredException {

		if (!hasEnemy(nation))
			throw new NotRegisteredException();
		else
			return getEnemies().remove(nation);
	}

	public boolean removeAllEnemies() {

		for (Nation enemy : new ArrayList<>(getEnemies()))
			try {
				removeEnemy(enemy);
				enemy.removeEnemy(this);
			} catch (NotRegisteredException ignored) {}
		return getAllies().size() == 0;
	}

	public boolean hasEnemy(Nation nation) {

		return getEnemies().contains(nation);
	}

	public List<Town> getTowns() {
		return Collections.unmodifiableList(towns);
	}

	public boolean isKing(Resident resident) {

		return hasCapital() && getCapital().isMayor(resident);
	}

	public boolean hasCapital() {

		return getCapital() != null;
	}

	public boolean hasAssistant(Resident resident) {

		return getAssistants().contains(resident);
	}

	public boolean isCapital(Town town) {

		return town == getCapital();
	}

	public boolean hasTown(String name) {

		for (Town town : towns)
			if (town.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}

	public boolean hasTown(Town town) {

		return towns.contains(town);
	}

	public void addTown(Town town) {
		towns.add(town);
	}

	/**
	 * Only to be called from the loading methods.
	 * 
	 * @param capital - Town to make capital.
	 * @throws EmptyNationException Thrown when no capital can be set.
	 */
	public void forceSetCapital(Town capital) throws EmptyNationException {

		if (towns.isEmpty())
			throw new EmptyNationException(this);
		
		try {
			if (capital.getNation().equals(this)) {
				setCapital(capital);
				return;
			}
		} catch (NotRegisteredException e) {
		}
		if (!findNewCapital())
			throw new EmptyNationException(this);
	}
	
	public void setCapital(Town capital) {

		TownyMessaging.sendDebugMsg("Nation " + this.getName() + " has set a capital city of " + capital.getName());
		this.capital = capital;
		try {
			TownyPerms.assignPermissions(capital.getMayor(), null);
		} catch (Exception e) {
			// Dummy catch to prevent errors on startup when setting nation.
		}
	}

	public Town getCapital() {

		return capital;
	}
	
	/**
	 * Finds the town in the nation with the most residents and makes it the capital.
	 * 
	 * @return whether it successfully set a capital.
	 */
	public boolean findNewCapital() {
		
		int numResidents = 0;
		Town tempCapital = null;
		for (Town newCapital : getTowns()) {
			if (newCapital.getNumResidents() > numResidents) {
				tempCapital = newCapital;
				numResidents = newCapital.getNumResidents();
			}
		}

		if (tempCapital != null) {
			setCapital(tempCapital);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Location getSpawn() throws TownyException {
		if(nationSpawn == null){
			throw new TownyException(Translation.of("msg_err_nation_has_not_set_a_spawn_location"));
		}

		return nationSpawn;
	}

	@Override
	public void setSpawn(Location spawn) throws TownyException {
		if (TownyAPI.getInstance().isWilderness(spawn))
			throw new TownyException(Translation.of("msg_cache_block_error_wild", "set spawn"));

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(spawn);

		if(TownySettings.getBoolean(ConfigNodes.GNATION_SETTINGS_CAPITAL_SPAWN)){
			if(this.capital == null){
				throw new TownyException(Translation.of("msg_err_spawn_not_within_capital"));
			}
			if(!townBlock.hasTown()){
				throw new TownyException(Translation.of("msg_err_spawn_not_within_capital"));
			}
			if(townBlock.getTown() != this.getCapital()){
				throw new TownyException(Translation.of("msg_err_spawn_not_within_capital"));
			}
		} else {
			if(!townBlock.hasTown()){
				throw new TownyException(Translation.of("msg_err_spawn_not_within_nationtowns"));
			}

			if(!towns.contains(townBlock.getTown())){
				throw new TownyException(Translation.of("msg_err_spawn_not_within_nationtowns"));
			}
		}

		this.nationSpawn = spawn;
	}

	/**
	 * Only to be called from the Loading methods.
	 *
	 * @param nationSpawn - Location to set as Nation Spawn
	 */
	public void forceSetNationSpawn(Location nationSpawn){
		this.nationSpawn = nationSpawn;
	}

	public List<Resident> getAssistants() {

		List<Resident> assistants = new ArrayList<>();
		
		for (Town town: towns)
		for (Resident assistant: town.getResidents()) {
			if (assistant.hasNationRank("assistant"))
				assistants.add(assistant);
		}
		return assistants;
	}

	public void setEnemies(List<Nation> enemies) {

		this.enemies = enemies;
	}

	public List<Nation> getEnemies() {

		return enemies;
	}

	public void setAllies(List<Nation> allies) {

		this.allies = allies;
	}

	public List<Nation> getAllies() {

		return allies;
	}

	public List<Nation> getMutualAllies() {
		List<Nation> result = new ArrayList<>();
		for(Nation ally: getAllies()) {
			if(ally.hasAlly(this))
				result.add(ally);
		}
		return result;
	}

	public int getNumTowns() {

		return towns.size();
	}

	public int getNumResidents() {

		int numResidents = 0;
		for (Town town : getTowns())
			numResidents += town.getNumResidents();
		return numResidents;
	}

	/**
	 * Should only be called by Town.removeNation();
	 * Removes town from {@link #towns} list and will choose a
	 * new Capital if necessary.
	 * 
	 * @param town - Town to remove from nation.
	 * @throws EmptyNationException - Thrown when last town is being removed.
	 */
	protected void removeTown(Town town) throws EmptyNationException {

		boolean isCapital = town.isCapital();
		remove(town);

		if (getNumTowns() == 0) {
			throw new EmptyNationException(this);
		} else if (isCapital) {
			findNewCapital();
		}
		this.save();
	}

	private void remove(Town town) {

		towns.remove(town);
	}

	private void removeAllTowns() {

		towns.clear();
	}

	public void setTaxes(double taxes) {
		this.taxes = Math.min(taxes, TownySettings.getMaxNationTax());
	}

	public void clear() {

		removeAllAllies();
		removeAllEnemies();
		removeAllTowns();
		capital = null;
	}

	/**
	 * Method for rechecking town distances to a new nation capital/moved 
	 * nation capital homeblock. Results in towns whose homeblocks are no 
	 * longer close enough to the capital homeblock being removed from 
	 * the nation.
	 * 
	 * @throws TownyException - Generic TownyException
	 */
	public void recheckTownDistance() throws TownyException {
		if(capital != null) {
			if (TownySettings.getNationRequiresProximity() > 0) {
				final Coord capitalCoord = capital.getHomeBlock().getCoord();
				Iterator<Town> it = towns.iterator();
				while(it.hasNext()) {
					Town town = it.next();
					Coord townCoord = town.getHomeBlock().getCoord();
					if (!capital.getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
						it.remove();
						continue;
					}

					final double distance = Math.sqrt(Math.pow(capitalCoord.getX() - (double)townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - (double)townCoord.getZ(), 2));
					if (distance > TownySettings.getNationRequiresProximity()) {
						TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_left_nation", this.getName()));
						TownyMessaging.sendPrefixedNationMessage(this, Translation.of("msg_nation_town_left", town.getName()));
						this.remove(town);
						it.remove();
					}
				}
			}
		}
	}
	
	/**
	 * A dry-run method for rechecking town distances to a new nation capital/
	 * moved nation capital homeblock.
	 * 
	 * @param towns - The list of towns to check.
	 * @param newCapital - The capital city from which to check the distance.
	 * @throws TownyException - Generic TownyException
	 * @return removedTowns - A list of Towns which would be removed under a real recheckTownDistance().
	 */
	public List<Town> recheckTownDistanceDryRun(List<Town> towns, Town newCapital) throws TownyException {
		List<Town> removedTowns = new ArrayList<>();
		if(newCapital != null) {
			if (TownySettings.getNationRequiresProximity() > 0) {
				final Coord capitalCoord = newCapital.getHomeBlock().getCoord();
				
				for (Town town : towns) {
					Coord townCoord = town.getHomeBlock().getCoord();
					if (!newCapital.getHomeblockWorld().equals(town.getHomeblockWorld())) {
						continue;
					}
					final double distance = Math.sqrt(Math.pow(capitalCoord.getX() - (double)townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - (double)townCoord.getZ(), 2));
					if (distance > TownySettings.getNationRequiresProximity()) {
						removedTowns.add(town);
					}
				}
			}
		}
		return removedTowns;
	}	

	public void setKing(Resident king) throws TownyException {

		if (!hasResident(king))
			throw new TownyException(Translation.of("msg_err_king_not_in_nation"));
		if (!king.isMayor())
			throw new TownyException(Translation.of("msg_err_new_king_notmayor"));
		setCapital(king.getTown());
		this.save();
	}

	public boolean hasResident(Resident resident) {

		for (Town town : getTowns())
			if (town.hasResident(resident))
				return true;
		return false;
	}
	
	public void collect(double amount) throws EconomyException {
		
		if (TownyEconomyHandler.isActive()) {
			double bankcap = TownySettings.getNationBankCap();
			if (bankcap > 0) {
				if (amount + this.getAccount().getHoldingBalance() > bankcap) {
					TownyMessaging.sendPrefixedNationMessage(this, Translation.of("msg_err_deposit_capped", bankcap));
					return;
				}
			}
			
			this.getAccount().deposit(amount, null);
		}

	}

	@Override
	public List<Resident> getResidents() {

		List<Resident> out = new ArrayList<>();
		for (Town town : getTowns())
			out.addAll(town.getResidents());
		return out;
	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<>();
		out.add(getTreeDepth(depth) + "Nation (" + getName() + ")");
		out.add(getTreeDepth(depth + 1) + "Capital: " + getCapital().getName());
		
		List<Resident> assistants = getAssistants();
		
		if (assistants.size() > 0)
			out.add(getTreeDepth(depth + 1) + "Assistants (" + assistants.size() + "): " + Arrays.toString(assistants.toArray(new Resident[0])));
		if (getAllies().size() > 0)
			out.add(getTreeDepth(depth + 1) + "Allies (" + getAllies().size() + "): " + Arrays.toString(getAllies().toArray(new Nation[0])));
		if (getEnemies().size() > 0)
			out.add(getTreeDepth(depth + 1) + "Enemies (" + getEnemies().size() + "): " + Arrays.toString(getEnemies().toArray(new Nation[0])));
		out.add(getTreeDepth(depth + 1) + "Towns (" + getTowns().size() + "):");
		for (Town town : getTowns())
			out.addAll(town.getTreeString(depth + 2));
		return out;
	}

	@Override
	public boolean hasResident(String name) {

		for (Town town : getTowns())
			if (town.hasResident(name))
				return true;
		return false;
	}

	@Override
	public Collection<Resident> getOutlaws() {
		List<Resident> out = new ArrayList<>();
		for (Town town : getTowns())
			out.addAll(town.getOutlaws());
		return Collections.unmodifiableList(out);
	}

	/**
	 * Gets the nation's UUID.
	 * @return nation UUID
	 * 
	 * @deprecated Use {@link Government#getUUID()} instead.
	 */
	@Deprecated
	public UUID getUuid() {
		return getUUID();
	}

	/**
	 * Set the nation's UUID. This should only be used internally! 
	 * 
	 * @param uuid UUID to set.
	 *             
	 * @deprecated Use {@link Government#setUUID(UUID)} instead.
	 */
	@Deprecated
	public void setUuid(UUID uuid) {
		setUUID(uuid);
	}

	public boolean hasValidUUID() {
		return uuid != null;
	}
	
	public void newSentAllyInvite(Invite invite) throws TooManyInvitesException {
		if (sentAllyInvites.size() <= InviteHandler.getSentAllyRequestsMaxAmount(this) -1) {
			sentAllyInvites.add(invite);
		} else {
			throw new TooManyInvitesException(Translation.of("msg_err_nation_sent_too_many_requests"));
		}
	}
	
	public void deleteSentAllyInvite(Invite invite) {
		sentAllyInvites.remove(invite);
	}
	
	public List<Invite> getSentAllyInvites() {
		return Collections.unmodifiableList(sentAllyInvites);
	}
	
	public Collection<TownBlock> getTownBlocks() {
		List<TownBlock> townBlocks = new ArrayList<>();
		for (Town town : this.getTowns())
			townBlocks.addAll(town.getTownBlocks());
		
		return Collections.unmodifiableCollection(townBlocks);
	}
	
	public int getNumTownblocks() {
		return getTownBlocks().size();
	}
	
	public Resident getKing() {
		return capital.getMayor();
	}

	@Override
	public String getFormattedName() {
		return TownySettings.getNationPrefix(this) + this.getName().replaceAll("_", " ")
			+ TownySettings.getNationPostfix(this);
	}
	
	@Override
	public void addMetaData(CustomDataField<?> md) {
		super.addMetaData(md);

		this.save();
	}

	@Override
	public void removeMetaData(CustomDataField<?> md) {
		super.removeMetaData(md);

		this.save();
	}

	@Override
	public World getWorld() {
		if (hasCapital() && getCapital().hasWorld()) {
			return BukkitTools.getWorld(getCapital().getHomeblockWorld().getName());
		} else {
			return BukkitTools.getWorlds().get(0);
		}
	}

	@Override
	public String getBankAccountPrefix() {
		return ECONOMY_ACCOUNT_PREFIX;
	}

	@Override
	public double getBankCap() {
		return TownySettings.getNationBankCap();
	}

	/**
	 * Shows if the nation is allied with the specified nation.
	 * 
	 * @param nation The nation that is allied.
	 * @return true if it is allied, false otherwise.
	 */
	public boolean isAlliedWith(Nation nation) {
		return allies.contains(nation);
	}

	public String getMapColorHexCode() {
		return mapColorHexCode;
	}

	public void setMapColorHexCode(String mapColorHexCode) {
		this.mapColorHexCode = mapColorHexCode;
	}
	
	/**
	 * @deprecated As of 0.96.2.0, please use {@link #getSpawn()} instead.
	 * 
	 * @return getSpawn()
	 * @throws TownyException When a nation spawn isn't available
	 */
	@Deprecated
	public Location getNationSpawn() throws TownyException {
		return getSpawn();
	}
	
	/**
	 * @deprecated As of 0.96.2.0, please use {@link #getBoard()} instead.
	 *  
	 * @return getBoard()
	 */
	@Deprecated
	public String getNationBoard() {
		return getBoard();
	}

	/**
	 * @deprecated As of 0.96.5.0, please use {@link Government#setNeutral(boolean)} instead.
	 * 
	 * @param neutral The value which will be used to set Neutrality true or false.
	 */
	@Deprecated
	public void toggleNeutral(boolean neutral) {
		setNeutral(neutral);
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveNation(this);
	}
}
