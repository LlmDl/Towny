package com.palmergames.bukkit.towny.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.BukkitTools;

/**
 * @author ElgarL
 * 
 *         Manager for Permission provider plugins
 * 
 */
public abstract class TownyPermissionSource {

	protected TownySettings settings;
	protected Towny plugin;

	protected GroupManager groupManager = null;

	abstract public String getPrefixSuffix(Resident resident, String node);

	abstract public int getGroupPermissionIntNode(String playerName, String node);
	
	abstract public int getPlayerPermissionIntNode(String playerName, String node);

	//abstract public boolean hasPermission(Player player, String node);

	abstract public String getPlayerGroup(Player player);

	abstract public String getPlayerPermissionStringNode(String playerName, String node);

	protected int getEffectivePermIntNode(String playerName, String node) {

		/*
		 * Bukkit doesn't support non boolean nodes
		 * so treat the same as bPerms
		 */
		Player player = BukkitTools.getPlayer(playerName);

		int biggest = -1;
		for (PermissionAttachmentInfo test : player.getEffectivePermissions()) {
			if (test.getPermission().startsWith(node + ".")) {
				String[] split = test.getPermission().split("\\.");
				try {
					int i = Integer.parseInt(split[split.length - 1]);
					biggest = Math.max(biggest, i);
				} catch (NumberFormatException e) {
				}
			}
		}
		return biggest;
	}

	/**
	 * Test if the player has a wild override to permit this action.
	 *  
	 * @param world - TownyWorld object.
	 * @param player - Player.
	 * @param material - Material being tested.
	 * @param action - Action type.
	 * @return true if the action is permitted.
	 */
	public boolean hasWildOverride(TownyWorld world, Player player, Material material, TownyPermission.ActionType action) {

		// check for permissions

		String blockPerm = PermissionNodes.TOWNY_WILD_ALL.getNode(action.toString().toLowerCase() + "." + material);

		/*
		 * If the player has the data node permission registered directly
		 *  or
		 * the player has the block permission and the data node isn't registered
		 *  or
		 * no node set but we are using permissions so check world settings
		 */
		return has(player, blockPerm) || unclaimedZoneAction(world, material, action);

	}

	public boolean unclaimedZoneAction(TownyWorld world, Material material, TownyPermission.ActionType action) {
		
		switch (action) {

			case BUILD:
				return world.getUnclaimedZoneBuild() || world.isUnclaimedZoneIgnoreMaterial(material);
			case DESTROY:
				return world.getUnclaimedZoneDestroy() || world.isUnclaimedZoneIgnoreMaterial(material);
			case SWITCH:
				return world.getUnclaimedZoneSwitch() || world.isUnclaimedZoneIgnoreMaterial(material);
			case ITEM_USE:
				return world.getUnclaimedZoneItemUse() || world.isUnclaimedZoneIgnoreMaterial(material);
		}

		return false;
	}

	/**
	 * Test if the player has an own town (or all town) override to permit this action.
	 * 
	 * @param player - Player.
	 * @param material - Material being tested.
	 * @param action - ActionType.
	 * @return true if the action is permitted.
	 */
	public boolean hasOwnTownOverride(Player player, Material material, TownyPermission.ActionType action) {

		//check for permissions
		String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("owntown." + action.toString().toLowerCase() + "." + material);

		/*
		 * If the player has the data node permission registered directly
		 *  or
		 * the player has the block permission and the data node isn't registered
		 *  or
		 * the player has an All town Override
		 */
		return has(player, blockPerm) || hasAllTownOverride(player, material, action);
	}

	/**
	 * Test if the player has a 'town owned', 'Own town' or 'all town' override to permit this action.
	 * 
	 * @param player - Player.
	 * @param material - Material being tested.
	 * @param action - ActionType.
	 * @return - True if action is permitted.
	 */
	public boolean hasTownOwnedOverride(Player player, Material material, TownyPermission.ActionType action) {

		//check for permissions
		String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("townowned." + action.toString().toLowerCase() + "." + material);

		/*
		 * If the player has the data node permission registered directly
		 *  or
		 * the player has the block permission and the data node isn't registered
		 *  or
		 * the player has an Own Town Override
		 *  or
		 * the player has an All town Override
		 */
		return has(player, blockPerm) || hasOwnTownOverride(player, material, action) || hasAllTownOverride(player, material, action);
	}

	/**
	 * Test if the player has an all town override to permit this action.
	 * 
	 * @param player - Player.
	 * @param material - Material being tested.
	 * @param action - ActionType.
	 * @return true if the action is permitted.
	 */
	public boolean hasAllTownOverride(Player player, Material material, TownyPermission.ActionType action) {

		//check for permissions
		String blockPerm = PermissionNodes.TOWNY_CLAIMED_ALL.getNode("alltown." + action.toString().toLowerCase() + "." + material);

		/*
		 * If the player has the data node permission registered directly
		 *  or
		 * the player has the block permission and the data node isn't registered
		 */
		return has(player, blockPerm);
	}	

	public boolean isTownyAdmin(Player player) {

		return (player == null) || player.isOp() || strictHas(player, PermissionNodes.TOWNY_ADMIN.getNode());

	}

	/**
	 * Primary test for a permission node, used throughout Towny.
	 * 
	 * @param player Player to check.
	 * @param perm Permission node to check for.
	 * @return true if the player has the permission node or is considered an admin.
	 */
	public boolean testPermission(Player player, String perm) {
		return isTownyAdmin(player) || strictHas(player, perm);
	}

	/**
	 * All local permission checks should go through here.
	 * 
	 * Return true if a player has a certain permission node or is Op.
	 *
	 * If {@link Player#isOp()} has already been called, {@link #strictHas(Player, String)} should be used instead.
	 * 
	 * @param player Player to check
	 * @param node Permission node to check for
	 * @return true if the player has this permission node or is Op.
	 */
	public boolean has(Player player, String node) {
		return player.isOp() || strictHas(player, node);
	}

	/**
	 * Return true if a player has a certain permission node.
	 *
	 * Should be used in place of {@link #has(Player, String)} if {@link Player#isOp()} has already been called.
	 *
	 * @param player Player to check
	 * @param node Permission node to check for
	 * @return true if the player has this permission node.
	 */
	private boolean strictHas(Player player, String node) {

		/*
		 * Node has been set or negated so return the actual value
		 */
		if (player.isPermissionSet(node))
			return player.hasPermission(node);

		/*
		 * Check for a parent with a wildcard
		 */
		final String[] parts = node.split("\\.");
		final StringBuilder builder = new StringBuilder(node.length());
		for (String part : parts) {
			builder.append('*');
			if (player.hasPermission("-" + builder.toString())) {
				return false;
			}
			if (player.hasPermission(builder.toString())) {
				return true;
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append(part).append('.');
		}

		/*
		 * No parent found so we don't have this node.
		 */
		return false;

	}
}
