package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.event.townblockstatus.NationZoneTownBlockStatusEvent;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Towny's class for external API Methods
 * For more dynamic/controlled changing of Towny's behavior, for example Database, War, Permissions
 * The {@link TownyUniverse} class should be used. It contains the map of all objects
 * aswell as serving as an internal API, that Towny uses.
 * @author Lukas Mansour (Articdive)
 */
public class TownyAPI {
    private static TownyAPI instance;
    private final Towny towny;
    private final TownyUniverse townyUniverse;
    
    private TownyAPI() {
        towny = Towny.getPlugin();
        townyUniverse = TownyUniverse.getInstance();
    }
    
    /**
     * Gets the town spawn {@link Location} of a {@link Player}.
     *
     * @param player {@link Player} of which you want the town spawn.
     * @return {@link Location} of the town spawn or if it is not obtainable null.
     */
    public Location getTownSpawnLocation(Player player) {
    	Resident resident = townyUniverse.getResident(player.getUniqueId());
    	
    	if (resident == null)
    		return null;
    	
        try {
            if (resident.hasTown()) {
				Town town = resident.getTown();
				return town.getSpawn();
			}
        } catch (TownyException ignore) {
        }

		return null;
    }
    
    /**
     * Gets the nation spawn {@link Location} of a {@link Player}.
     *
     * @param player {@link Player} of which you want the nation spawn.
     * @return {@link Location} of the nation spawn or if it is not obtainable null.
     */
    public Location getNationSpawnLocation(Player player) {
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		if (resident == null)
			return null;
		
        try {
            if (resident.hasTown()) {
            	Town t = resident.getTown();
            	if (t.hasNation()) {
					Nation nation = t.getNation();
					return nation.getSpawn();
				}
			}
        } catch (TownyException ignore) {
        }

		return null;
    }
 
    /**
     * Gets the resident's town if they have one.
     * 
     * @param resident Resident to get the town from.
     * @return The resident's Town or null if they have none.
     */
    @Nullable
    public Town getResidentTownOrNull(Resident resident) {
    	if (resident.hasTown())
	    	try {
				return resident.getTown();
			} catch (NotRegisteredException ignored) {}

    	return null;
    }
    
    /**
     * Gets the resident's nation if they have one.
     * 
     * @param resident Resident to get the nation from.
     * @return The resident's Nation or null if they have none.
     */
    @Nullable
    public Nation getResidentNationOrNull(Resident resident) {
    	if (resident.hasNation())
			try {
				return resident.getTown().getNation();
			} catch (NotRegisteredException ignored) {}
    	
    	return null;
    }
    
    @Nullable
    public Nation getTownNationOrNull(Town town) {
    	if (town.hasNation())
			try {
				return town.getNation();
			} catch (NotRegisteredException ignored) {}
    	
    	return null;
    }
    
    /**
     * Gets the nation from the given UUID.
     * @param uuid UUID of the nation.
     * @return nation or null if it doesn't exist.
     */
    @Nullable
    public Nation getNation(UUID uuid) {
    	return TownyUniverse.getInstance().getNation(uuid);
    }
    
    /**
     * Gets the town from the given UUID.
     * @param uuid UUID name of the town.
     * @return town or null if it doesn't exist.
     */
    @Nullable
    public Town getTown(UUID uuid) {
    	return TownyUniverse.getInstance().getTown(uuid);
    }
    
    /**
     * Gets the resident from the given UUID.
     * @param uuid UUID name of the resident.
     * @return resident or null if it doesn't exist.
     */
    @Nullable
    public Resident getResident(UUID uuid) {
    	return TownyUniverse.getInstance().getResident(uuid);
    }  

    /**
     * Gets the nation from the given name.
     * @param name String name of the nation.
     * @return nation or null if it doesn't exist.
     */
    @Nullable
    public Nation getNation(String name) {
    	return TownyUniverse.getInstance().getNation(name);
    }
    
    /**
     * Gets the town from the given name.
     * @param name String name of the town.
     * @return town or null if it doesn't exist.
     */
    @Nullable
    public Town getTown(String name) {
    	return TownyUniverse.getInstance().getTown(name);
    }
    
    /**
     * Gets the resident from the given name.
     * @param name String name of the resident.
     * @return resident or null if it doesn't exist.
     */
    @Nullable
    public Resident getResident(String name) {
    	return TownyUniverse.getInstance().getResident(name);
    }    
    
    /**
     * Find the the matching {@link Player} of the specified {@link Resident}.
     *
     * @param resident {@link Resident} of which you want the matching {@link Player}.
     * @return an online {@link Player} or if it's not obtainable.
     */
    public Player getPlayer(Resident resident) {
    	// NPCs are not players
    	if (resident.isNPC())
    		return null;
    	
    	Player player = null;
    	
    	if (resident.hasUUID())
    		player = BukkitTools.getPlayer(resident.getUUID());
    	
    	// Some servers use cross-platform proxies / offline mode where UUIDs may not be accurate. 
    	if (player == null)
    		player = BukkitTools.getPlayerExact(resident.getName());
    	
        return player;
    }
    
    /**
     * Find the {@link UUID} for the matching {@link Player} of the specified {@link Resident}.
     *
     * @param resident {@link Resident} of which you want the {@link UUID}.
     * @return an online {@link Player}'s {@link UUID} or null if it's not obtainable.
     */
    public UUID getPlayerUUID(Resident resident) {
    	// NPCs are not players
    	if (resident.isNPC())
    		return null;
    	
    	// Use stored UUID if it exists
    	if (resident.hasUUID())
    		return resident.getUUID();
    	
    	Player player = BukkitTools.getPlayerExact(resident.getName());
    	
    	if (player != null)
    		return player.getUniqueId();
        
        return null;
    }
    
    /**
     * Gets all online {@link Player}s for a specific {@link ResidentList}.
     *
     * @param owner {@link ResidentList} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link ResidentList}.
     */
    public List<Player> getOnlinePlayers(ResidentList owner) {
        ArrayList<Player> players = new ArrayList<>();
        
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null) {
                if (owner.hasResident(player.getName())) {
                    players.add(player);
                }
            }
        }
        return players;
    }
    
    /**
     * Gets all online {@link Player}s for a specific {@link Town}.
     * 
     * @param town {@link Town} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Town}.
     */
    public List<Player> getOnlinePlayersInTown(Town town){
    	return getOnlinePlayers(town);
    }

    /**
     * Gets all online {@link Player}s for a specific {@link Nation}.
     * 
     * @param nation {@link Nation} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Nation}.
     */
    public List<Player> getOnlinePlayersInNation(Nation nation){
    	return getOnlinePlayers(nation);
    }
    
    /** 
     * Gets all online {@link Player}s for a specific {@link Nation}s alliance.
     * 
     * @param nation {@link Nation} of which you want all the online allied {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Nation}s allies.
     */
    public List<Player> getOnlinePlayersAlliance(Nation nation) {
		ArrayList<Player> players = new ArrayList<>(getOnlinePlayers(nation));
        if (!nation.getAllies().isEmpty()) {
			for (Nation nations : nation.getAllies()) {
				players.addAll(getOnlinePlayers(nations));
			}
        }
        return players;
    }
    
    /**
     * Check if the specified {@link Block} is in the wilderness.
     *
     * @param block {@link Block} to test for.
     * @return true if the {@link Block} is in the wilderness, false otherwise.
     */
    public boolean isWilderness(Block block) {
        return isWilderness(block.getLocation());
    }
    
    /**
     * Check if the specified {@link Location} is in the wilderness.
     *
     * @param location {@link Location} to test widlerness for.
     * @return true if the {@link Location} is in the wilderness, false otherwise.
     */
    public boolean isWilderness(Location location) {
        return isWilderness(WorldCoord.parseWorldCoord(location));
    }
    
    /**
     * Check if the specified {@link WorldCoord} is in the wilderness.
     *
     * @param worldCoord {@link WorldCoord} to test widlerness for.
     * @return true if the {@link WorldCoord} is in the wilderness, false otherwise.
     */
    public boolean isWilderness(WorldCoord worldCoord) {
        
        try {
        	// Do not throw an exception to reduce object creation
        	if (worldCoord.hasTownBlock() && worldCoord.getTownBlock().hasTown())
        		return false;
        } catch (NotRegisteredException ignore) {
        }

		// Must be wilderness
		return true;
    }    
    
    /**
     * Returns value of usingTowny for the given world.
     * 
     * @param world - the world to check
     * @return true or false
     */
    public boolean isTownyWorld(World world) {
    	try {
			return townyUniverse.getDataSource().getWorld(world.getName()).isUsingTowny();
		} catch (NotRegisteredException e) {
			return false;
		}
    }
    
    /**
     * Returns {@link TownyWorld} unless it is null.
     * 
     * @param worldName - the name of the world to get.
     * @return TownyWorld or {@code null}.
     */
    public TownyWorld getTownyWorld(String worldName) {
    	try {
    		TownyWorld townyWorld = townyUniverse.getDataSource().getWorld(worldName);
    		return townyWorld;
    	} catch (NotRegisteredException e) {
			return null;
		}
    }
    
    
    /**
     * Get the {@link Town} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} for.
     * @return {@link Town} at this location, or {@code null} for none.
     */
    public Town getTown(Location location) {
        try {
            WorldCoord worldCoord = WorldCoord.parseWorldCoord(location);
            if (worldCoord.hasTownBlock()) {
            	TownBlock tb = worldCoord.getTownBlock();
            	if (tb.hasTown())
            		return tb.getTown();
			}
        } catch (NotRegisteredException ignore) {
        }

		// No data so return null
		return null;
    }
    
    /**
     * Get the name of a {@link Town} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} name for.
     * @return {@link String} containg the name of the {@link Town} at this location, or {@code null} for none.
     */
    public String getTownName(Location location) {
    	Town town = getTown(location);
    	return town != null ? town.getName() : null;
    }
    
    
    /**
     * Get the {@link UUID} of a {@link Town} at the specified {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} {@link UUID} for.
     * @return {@link UUID} of any {@link Town} at this {@link Location}, or {@code null} for none.
     */
    public UUID getTownUUID(Location location) {
    	Town town = getTown(location);
    	return town != null ? town.getUUID() : null;
    }
    
    /**
     * Get the {@link TownBlock} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link TownBlock} of.
     * @return {@link TownBlock} at this {@link Location}, or {@code null} for none.
     */
    public TownBlock getTownBlock(Location location) {
        try {
            WorldCoord worldCoord = WorldCoord.parseWorldCoord(location);
            if (worldCoord.hasTownBlock())
            	return worldCoord.getTownBlock();
        } catch (NotRegisteredException ignore) {
        }

		// No data so return null
		return null;
    }
    
    /**
     * Get a list of active {@link Resident}s.
     *
     * @return {@link List} of active {@link Resident}s.
     */
    public List<Resident> getActiveResidents() {
        List<Resident> activeResidents = new ArrayList<>();
        for (Resident resident : townyUniverse.getResidents()) {
            if (isActiveResident(resident)) {
                activeResidents.add(resident);
            }
        }
        return activeResidents;
    }
    
    /**
     * Check if the specified {@link Resident} is an active Resident.
     *
     * @param resident {@link Resident} to test for activity.
     * @return true if the player is active, false otherwise.
     */
    public boolean isActiveResident(Resident resident) {
        return ((System.currentTimeMillis() - resident.getLastOnline() < (20 * TownySettings.getInactiveAfter())) || (BukkitTools.isOnline(resident.getName())));
    }
    
    /**
     * Gets Towny's saving Database
     *
     * @return the {@link TownyDataSource}
     */
    public TownyDataSource getDataSource() {
        return townyUniverse.getDataSource();
    }
    
    /**
     * Checks if server is currently in war-time.
     *
     * @return true if the server is in war-time.
     */
    public boolean isWarTime() {
        return townyUniverse.isWarTime();
    }
    
    /**
     * Check which {@link Resident}s are online in a {@link ResidentList}
     *
     * @param owner {@link ResidentList} to check for online {@link Resident}s.
     * @return {@link List} of {@link Resident}s that are online.
     */
    public List<Resident> getOnlineResidents(ResidentList owner) {
        
        List<Resident> onlineResidents = new ArrayList<>();
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null)
                for (Resident resident : owner.getResidents()) {
                    if (resident.getName().equalsIgnoreCase(player.getName()))
                        onlineResidents.add(resident);
                }
        }
        return onlineResidents;
    }
    
    /**
     * Teleports the Player to the specified jail {@link Location}.
     *
     * @param player   {@link Player} to be teleported to jail.
     * @param location {@link Location} of the jail to be teleported to.
     */
    public void jailTeleport(final Player player, final Location location) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(towny, () -> player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN),
			(long) TownySettings.getTeleportWarmupTime() * 20);
    }
    
    public void requestTeleport(Player player, Location spawnLoc) {
    	Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
    	
    	if (resident != null) {
			TeleportWarmupTimerTask.requestTeleport(resident, spawnLoc);
		}
    }
    
    public void abortTeleportRequest(Resident resident) {
        
        TeleportWarmupTimerTask.abortTeleportRequest(resident);
    }
    
    public void registerCustomDataField(CustomDataField<?> field) throws KeyAlreadyRegisteredException {
    	townyUniverse.addCustomCustomDataField(field);
	}

    /**
     * Method to figure out if a location is in a NationZone.
     * 
     * @param location - Location to test.
     * @return true if the location is in a NationZone.
     */
    public boolean isNationZone(Location location) {
    	if (!isWilderness(location))
    		return false;
    	TownBlockStatus status = hasNationZone(location);
    	if (status.equals(TownBlockStatus.NATION_ZONE))
    		return true;
    	
    	return false;
    }
    /**
     * Method to figure out if a location in the wilderness is normal wilderness of nation zone.
     * Recommended to use {@link TownyAPI#isWilderness(Location)} prior to using this, to confirm the location is not in a town.  
     * 
     * @param location - Location to test whether it is a nation zone or normal wilderness.
     * @return returns either UNCLAIMED_ZONE or NATION_ZONE
     */
    public TownBlockStatus hasNationZone(Location location) {
    	
    	return hasNationZone(WorldCoord.parseWorldCoord(location));
    }

    /**
     * Method to figure out if a worldcoord in the wilderness is normal wilderness of nation zone.
     * Recommended to use {@link TownyAPI#isWilderness(WorldCoord)} prior to using this, to confirm the location is not in a town.  
     * 
     * @param worldCoord - WorldCoord to test whether it is a nation zone or normal wilderness.
     * @return returns either UNCLAIMED_ZONE or NATION_ZONE
     */
    public TownBlockStatus hasNationZone(WorldCoord worldCoord) {
    	
		Town nearestTown = null;
		int distance;
		try {
			final TownBlock nearestTownblock = worldCoord.getTownyWorld().getClosestTownblockWithNationFromCoord(worldCoord);
			
			if (nearestTownblock == null) {
				return TownBlockStatus.UNCLAIMED_ZONE;
			}
			
			nearestTown = nearestTownblock.getTown();
			
			// Safety validation, both these cases should never occur.
			if (nearestTown == null || !nearestTown.hasNation()) {
				return TownBlockStatus.UNCLAIMED_ZONE;
			}
			
			distance = (int) MathUtil.distance(worldCoord.getX(), nearestTownblock.getX(), worldCoord.getZ(), nearestTownblock.getZ());
		} catch (NotRegisteredException e1) {
			// There will almost always be a town in any world where towny is enabled. 
			// If there isn't then we fall back on normal unclaimed zone status.
			return TownBlockStatus.UNCLAIMED_ZONE;
		}

		// It is possible to only have nation zones surrounding nation capitals. If this is true, we treat this like a normal wilderness.
		if (!nearestTown.isCapital() && TownySettings.getNationZonesCapitalsOnly()) {
			return TownBlockStatus.UNCLAIMED_ZONE;
		}

		try {
			int nationZoneRadius = Integer.parseInt(TownySettings.getNationLevel(nearestTown.getNation()).get(TownySettings.NationLevel.NATIONZONES_SIZE).toString());
			
			if (nearestTown.isCapital()) {
				nationZoneRadius += TownySettings.getNationZonesCapitalBonusSize();
			}

			if (distance <= nationZoneRadius) {
				NationZoneTownBlockStatusEvent event = new NationZoneTownBlockStatusEvent(nearestTown);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled())
					return TownBlockStatus.UNCLAIMED_ZONE;
				
				return TownBlockStatus.NATION_ZONE;
			}
		} catch (NumberFormatException | NotRegisteredException ignored) {
		}
		
		return TownBlockStatus.UNCLAIMED_ZONE;
    }
    
    public static TownyAPI getInstance() {
        if (instance == null) {
            instance = new TownyAPI();
        }
        return instance;
    }
}
