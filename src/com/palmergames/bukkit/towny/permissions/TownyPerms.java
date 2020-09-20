package com.palmergames.bukkit.towny.permissions;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * @author ElgarL
 * 
 */
public class TownyPerms {

	protected static LinkedHashMap<String, Permission> registeredPermissions = new LinkedHashMap<>();
	protected static HashMap<String, PermissionAttachment> attachments = new HashMap<>();
	private static CommentedConfiguration perms;
	private static Towny plugin;
	
	public static void initialize(Towny plugin) {
		TownyPerms.plugin = plugin;
	}
	
	private static Field permissions;

	// Setup reflection (Thanks to Codename_B for the reflection source)
	static {
		try {
			permissions = PermissionAttachment.class.getDeclaredField("permissions");
			permissions.setAccessible(true);
		} catch (SecurityException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the townyperms.yml file.
	 * If it doesn't exist create it from the resource file in the jar.
	 * 
	 * @param filepath - Path to townyperms.yml
	 * @param defaultRes - Default townyperms.yml within the jar.
	 * @throws TownyException - When permission file cannot be loaded.
	 */
	public static void loadPerms(String filepath, String defaultRes) throws TownyException {

		String fullPath = filepath + File.separator + defaultRes;

		File file = FileMgmt.unpackResourceFile(fullPath, defaultRes, defaultRes);
		if (file != null) {
			// read the (language).yml into memory
			perms = new CommentedConfiguration(file);
			if (!perms.load())
				throw new TownyException("Could not read Townyperms.yml");
		}
		
		/*
		 * Only do this once as we are really only interested in Towny perms.
		 */
		collectPermissions();
		
	}
	
	/**
	 * Register a specific residents permissions with Bukkit.
	 * 
	 * @param resident - Resident to check if player is valid
	 * @param player - Player to register permission
	 */
	public static void assignPermissions(Resident resident, Player player) {

		PermissionAttachment playersAttachment;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (resident == null) {
			try {
				resident = townyUniverse.getDataSource().getResident(player);
			} catch (NotRegisteredException e) {
				// failed to get resident
				e.printStackTrace();
				return;
			}
		} else {
			player = BukkitTools.getPlayer(resident.getName());
		}

		/*
		 * Find the current attachment or create a new one (if the player is
		 * online)
		 */

		if ((player == null) || !player.isOnline()) {
			attachments.remove(resident.getName());
			return;
		}

		TownyWorld World;

		try {
			World = townyUniverse.getDataSource().getWorld(player.getLocation().getWorld().getName());
		} catch (NotRegisteredException e) {
			// World not registered with Towny.
			e.printStackTrace();
			return;
		}

		if (attachments.containsKey(resident.getName())) {
			playersAttachment = attachments.get(resident.getName());
		} else {
			// DungeonsXL sometimes moves players which aren't online out of dungeon worlds causing an error in the log to appear.
			try {
				playersAttachment = BukkitTools.getPlayer(resident.getName()).addAttachment(plugin);
			} catch (Exception e) {
				return;
			}

		}
		/*
		 * Set all our Towny default permissions using reflection else bukkit
		 * will perform a recalculation of perms for each addition.
		 */

		try {
			synchronized (playersAttachment) {
				@SuppressWarnings("unchecked")
				Map<String, Boolean> orig = (Map<String, Boolean>) permissions.get(playersAttachment);
				/*
				 * Clear the map (faster than removing the attachment and
				 * recalculating)
				 */
				orig.clear();

				if (World.isUsingTowny()) {
					/*
					 * Fill with the fresh perm nodes
					 */
					orig.putAll(TownyPerms.getResidentPerms(resident));

					// System.out.print("Perms set for: " + resident.getName());
				}
				/*
				 * Tell bukkit to update it's permissions
				 */
				playersAttachment.getPermissible().recalculatePermissions();
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		/*
		 * Store the attachment for future reference
		 */
		attachments.put(resident.getName(), playersAttachment);

	}
	
	/**
	 * Should only be called when a player leaves the server.
	 * 
	 * @param name - Player's name to remove attachment of
	 */
	public static void removeAttachment(String name) {
		
		attachments.remove(name);
		
	}
	
	/**
	 * Update the permissions for all online residents
	 * 
	 */
	public static void updateOnlinePerms() {
		
		for (Player player : BukkitTools.getOnlinePlayers()) {
			assignPermissions(null, player);
		}
		
	}
	
	/**
	 * Update the permissions for all residents of a town (if online)
	 * 
	 * @param town - Town to target
	 */
	public static void updateTownPerms(Town town) {
		
		for (Resident resident: town.getResidents()) {
			assignPermissions(resident, null);
		}
		
	}
	
	/**
	 * Update the permissions for all residents of a nation (if online)
	 * 
	 * @param nation - Nation to target
	 */
	public static void updateNationPerms(Nation nation) {
		
		for (Town town: nation.getTowns()) {
			updateTownPerms(town);
		}
		
	}

	/**
	 * Fetch a list of permission nodes
	 * 
	 * @param path - path to permission nodes
	 * @return a List of permission nodes.
	 */
	private static List<String> getList(String path) {

		if (perms.contains(path)) {
			return perms.getStringList(path);
		}
		return null;
	}
	
	/**
	 * Returns a sorted map of this residents current permissions.
	 * 
	 * @param resident - Resident to check
	 * @return a sorted Map of permission nodes
	 */
	public static LinkedHashMap<String, Boolean> getResidentPerms(Resident resident) {
		// Start by adding the default perms everyone gets
		Set<String> permList = new HashSet<>(getDefault());
		
		//Check for town membership
		if (resident.hasTown()) {
			try {
				permList.addAll(getTownDefault(resident.getTown()));
			} catch (NotRegisteredException e) {
				// Not Possible!
			}
			// Is Mayor?
			if (resident.isMayor()) permList.addAll(getTownMayor());
				
			//Add town ranks here
			for (String rank: resident.getTownRanks()) {
				permList.addAll(getTownRank(rank));
			}
			
			//Check for nation membership
			if (resident.hasNation()) {
				permList.addAll(getNationDefault());
				// Is King?
				if (resident.isKing()) permList.addAll(getNationKing());
							
				//Add nation ranks here
				for (String rank: resident.getNationRanks()) {
					permList.addAll(getNationRank(rank));
				}
			}
		}
		
		List<String> playerPermArray = sort(new ArrayList<String>(permList));
		LinkedHashMap<String, Boolean> newPerms = new LinkedHashMap<String, Boolean>();

		Boolean value = false;
		for (String permission : playerPermArray) {			
			if (permission.contains("{townname}")) {
				if (resident.hasTown())
					try {
						String placeholderPerm = permission.replace("{townname}", resident.getTown().getName().toLowerCase());
						newPerms.put(placeholderPerm, true);
					} catch (NotRegisteredException e) {
					}
			} else if (permission.contains("{nationname}")) {
				if (resident.hasNation())
					try {
						String placeholderPerm = permission.replace("{nationname}", resident.getTown().getNation().getName().toLowerCase());
						newPerms.put(placeholderPerm, true);
					} catch (NotRegisteredException e) {
					}
			} else {
				value = (!permission.startsWith("-"));
				newPerms.put((value ? permission : permission.substring(1)), value);
			}
		}
		return newPerms;
		
	}
	
	public static void registerPermissionNodes() {
		
		 plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new
		 Runnable(){

			@Override
			public void run() {

				Permission perm;
				
				/*
				 * Register Town ranks
				 */
				for (String rank : getTownRanks()) {
					perm = new
					Permission(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank),
					"User can grant this town rank to others..",
					PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(), true);
				}
				
				/*
				 * Register Nation ranks
				 */
				for (String rank : getNationRanks()) {
					perm = new
					Permission(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank),
					"User can grant this town rank to others..",
					PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(), true);
				}
			}
			 
		 },1);
	}

	/*
	 * Getter/Setters for TownyPerms
	 */
	
	/**
	 * Default permissions everyone gets
	 * 
	 * @return a List of permissions
	 */
	public static List<String> getDefault() {

		List<String> permsList = getList("nomad");
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/*
	 * Town permission section
	 */

	/**
	 * Fetch a list of all available town ranks
	 * 
	 * @return a list of rank names.
	 */
	public static List<String> getTownRanks() {

		return new ArrayList<String>(((MemorySection) perms.get("towns.ranks")).getKeys(false));
	}

	/**
	 * Default permissions everyone in a town gets
	 * 
	 * @param town - Town to target
	 * @return a list of permissions
	 */
	public static List<String> getTownDefault(Town town) {

		List<String> permsList = getList("towns.default");
		if ((permsList == null)) {
			List<String> emptyPermsList = new ArrayList<String>();
			emptyPermsList.add("towny.town." + town.getName().toLowerCase());
			return emptyPermsList;
		} else {
			permsList.add("towny.town." + town.getName().toLowerCase());
			return permsList;
		}
	}

	/**
	 * A town mayors permissions
	 *
	 * @return a list of permissions
	 */
	public static List<String> getTownMayor() {

		List<String> permsList = getList("towns.mayor");
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/**
	 * Get a specific ranks permissions
	 * 
	 * @param rank - Rank to check permissions for
	 * @return a List of permissions
	 */
	public static List<String> getTownRank(String rank) {

		List<String> permsList = getList("towns.ranks." + rank);//.toLowerCase());
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/*
	 * Nation permission section
	 */

	/**
	 * Fetch a list of all available nation ranks
	 * 
	 * @return a list of rank names.
	 */
	public static List<String> getNationRanks() {

		return new ArrayList<String>(((MemorySection) perms.get("nations.ranks")).getKeys(false));
	}

	/**
	 * Default permissions everyone in a nation gets
	 * 
	 * @return a List of permissions
	 */
	public static List<String> getNationDefault() {

		List<String> permsList = getList("nations.default");
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/**
	 * A nations kings permissions
	 * 
	 * @return a List of permissions
	 */
	public static List<String> getNationKing() {

		List<String> permsList = getList("nations.king");
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/**
	 * Get a specific ranks permissions
	 * 
	 * @param rank - Rank to get permissions of
	 * @return a List of Permissions
	 */
	public static List<String> getNationRank(String rank) {

		List<String> permsList = getList("nations.ranks." + rank);//.toLowerCase());
		return (permsList == null)? new ArrayList<String>() : permsList;
	}
	
	/*
	 * Permission utility functions taken from GroupManager (which I wrote anyway).
	 */
	
	/**
	 * Update the list of permissions registered with bukkit
	 */
	public static void collectPermissions() {

		registeredPermissions.clear();

		for (Permission perm : BukkitTools.getPluginManager().getPermissions()) {
			registeredPermissions.put(perm.getName().toLowerCase(), perm);
		}

	}
	
	/**
	 * Sort a permission node list by parent/child
	 * 
	 * @param permList - List of permissions
	 * @return List sorted for priority
	 */
	private static List<String> sort(List<String> permList) {
		
		List<String> result = new ArrayList<String>();

		for (String key : permList) {
			String a = key.charAt(0) == '-' ? key.substring(1) : key;
			Map<String, Boolean> allchildren = getAllChildren(a, new HashSet<String>());
			if (allchildren != null) {

				ListIterator<String> itr = result.listIterator();

				while (itr.hasNext()) {
					String node = itr.next();
					String b = node.charAt(0) == '-' ? node.substring(1) : node;

					// Insert the parent node before the child
					if (allchildren.containsKey(b)) {
						itr.set(key);
						itr.add(node);
						break;
					}
				}
			}
			if (!result.contains(key))
				result.add(key);
		}

		return result;
	}

	/**
	 * Fetch all permissions which are registered with superperms.
	 * {can include child nodes)
	 * 
	 * @param includeChildren - If child nodes should be included
	 * @return List of all permission nodes
	 */
	public List<String> getAllRegisteredPermissions(boolean includeChildren) {

		List<String> perms = new ArrayList<String>();

		for (String key : registeredPermissions.keySet()) {
			if (!perms.contains(key)) {
				perms.add(key);

				if (includeChildren) {
					Map<String, Boolean> children = getAllChildren(key, new HashSet<String>());
					if (children != null) {
						for (String node : children.keySet())
							if (!perms.contains(node))
								perms.add(node);
					}
				}
			}

		}
		return perms;
	}

	/**
	 * Returns a map of ALL child permissions registered with bukkit
	 * null is empty
	 * 
	 * @param node - Parent node
	 * @param playerPermArray current list of perms to check against for
	 *            negations
	 * @return Map of child permissions
	 */
	public static Map<String, Boolean> getAllChildren(String node, Set<String> playerPermArray) {

		LinkedList<String> stack = new LinkedList<String>();
		Map<String, Boolean> alreadyVisited = new HashMap<String, Boolean>();
		stack.push(node);
		alreadyVisited.put(node, true);

		while (!stack.isEmpty()) {
			String now = stack.pop();

			Map<String, Boolean> children = getChildren(now);

			if ((children != null) && (!playerPermArray.contains("-" + now))) {
				for (String childName : children.keySet()) {
					if (!alreadyVisited.containsKey(childName)) {
						stack.push(childName);
						alreadyVisited.put(childName, children.get(childName));
					}
				}
			}
		}
		alreadyVisited.remove(node);
		if (!alreadyVisited.isEmpty())
			return alreadyVisited;

		return null;
	}
	
	/**
	 * Returns a map of the child permissions (1 node deep) as registered with
	 * Bukkit.
	 * null is empty
	 * 
	 * @param node - Parent node
	 * @return Map of child permissions
	 */
	public static Map<String, Boolean> getChildren(String node) {

		Permission perm = registeredPermissions.get(node.toLowerCase());
		if (perm == null)
			return null;

		return perm.getChildren();

	}

}
