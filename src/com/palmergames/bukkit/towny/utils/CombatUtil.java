package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.DisallowedPVPEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;

import java.util.List;

/**
 * 
 * @author ElgarL,Shade
 * 
 */
public class CombatUtil {

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
	 * 
	 * @param plugin - Reference to Towny
	 * @param attacker - Entity attacking the Defender
	 * @param defender - Entity defending from the Attacker
	 * @return true if we should cancel.
	 */
	public static boolean preventDamageCall(Towny plugin, Entity attacker, Entity defender) {

		try {
			TownyWorld world = TownyUniverse.getInstance().getDataSource().getWorld(defender.getWorld().getName());

			// World using Towny
			if (!world.isUsingTowny())
				return false;

			Player a = null;
			Player b = null;

			/*
			 * Find the shooter if this is a projectile.
			 */
			if (attacker instanceof Projectile) {
				
				Projectile projectile = (Projectile) attacker;
				Object source = projectile.getShooter();
				
				if (source instanceof Entity) {
					attacker = (Entity) source;
				} else {
					return false;	// TODO: prevent damage from dispensers
				}

			}

			if (attacker instanceof Player)
				a = (Player) attacker;
			if (defender instanceof Player)
				b = (Player) defender;

			// Allow players to injure themselves
			if (a == b)
				return false;

			return preventDamageCall(plugin, world, attacker, defender, a, b);

		} catch (Exception e) {
			// Failed to fetch world
		}

		return false;

	}

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
	 * 
	 * @param plugin - Reference to Towny
	 * @param world - World in which DamageCall was issued
	 * @param attackingEntity - Entity attacking
	 * @param defendingEntity - Entity defending
	 * @param attackingPlayer - Player attacking
	 * @param defendingPlayer - Player defending
	 * @return true if we should cancel.
	 * @throws NotRegisteredException - Generic NotRegisteredException
	 */
	public static boolean preventDamageCall(Towny plugin, TownyWorld world, Entity attackingEntity, Entity defendingEntity, Player attackingPlayer, Player defendingPlayer) throws NotRegisteredException {

		// World using Towny
		if (!world.isUsingTowny())
			return false;

		Coord coord = Coord.parseCoord(defendingEntity);
		TownBlock defenderTB = TownyAPI.getInstance().getTownBlock(defendingEntity.getLocation());
		TownBlock attackerTB = TownyAPI.getInstance().getTownBlock(attackingEntity.getLocation());
		/*
		 * We have an attacking player
		 */
		if (attackingPlayer != null) {

			/*
			 * If another player is the target
			 * or
			 * The target is in a TownBlock and...
			 * the target is a tame wolf and we are not it's owner
			 */
			if ((defendingPlayer != null) || ((defenderTB != null) && ((defendingEntity instanceof Wolf) && ((Wolf) defendingEntity).isTamed() && !((Wolf) defendingEntity).getOwner().equals(attackingEntity)))) {

				/*
				 * Defending player is in a warzone
				 */
				if (world.isWarZone(coord) && !preventFriendlyFire(attackingPlayer, defendingPlayer))
					return false;

				/*
				 * Check for special pvp plots (arena)
				 */
				if (isPvPPlot(attackingPlayer, defendingPlayer))
					return false;

				/*
				 * Check if we are preventing friendly fire between allies
				 * Check the attackers TownBlock and it's Town for their PvP
				 * status, else the world.
				 * Check the defenders TownBlock and it's Town for their PvP
				 * status, else the world.
				 */
				if (preventFriendlyFire(attackingPlayer, defendingPlayer) || preventPvP(world, attackerTB) || preventPvP(world, defenderTB)) {

					DisallowedPVPEvent event = new DisallowedPVPEvent(attackingPlayer, defendingPlayer);
					plugin.getServer().getPluginManager().callEvent(event);

					return !event.isCancelled();
				}

			} else {
				/*
				 * Remove animal killing prevention start
				 */
				
				
				/*
				 * Defender is not a player so check for PvM
				 */
				if (defenderTB != null) {
					if(defenderTB.getType() == TownBlockType.FARM && TownySettings.getFarmAnimals().contains(defendingEntity.getType().toString())) {
						if (PlayerCacheUtil.getCachePermission(attackingPlayer, attackingPlayer.getLocation(), Material.WHEAT, ActionType.DESTROY))
							return false;
					}
					List<Class<?>> prots = EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getEntityTypes(), "TownMobPVM:");
					if (EntityTypeUtil.isInstanceOfAny(prots, defendingEntity)) {
						
						/*
						 * Only allow the player to kill protected entities etc,
						 * if they are from the same town
						 * and have destroy permissions (dirt) in the defending
						 * TownBlock
						 */
						if (!PlayerCacheUtil.getCachePermission(attackingPlayer, attackingPlayer.getLocation(), Material.DIRT, ActionType.DESTROY))
							return true;
					}
				}

				/*
				 * Remove prevention end
				 */

				/*
				 * Protect specific entity interactions (faked with Materials).
				 */
				Material block = null;

				switch (defendingEntity.getType()) {
	
					case ITEM_FRAME:
						block = Material.ITEM_FRAME;
						break;
	
					case PAINTING:
						block = Material.PAINTING;
						break;
	
					case MINECART:
						block = Material.MINECART;
						break;
						
					case MINECART_CHEST:
						block = Material.CHEST_MINECART;
						break;
					
					case MINECART_FURNACE:
						block = Material.FURNACE_MINECART;
						break;
	
					case MINECART_COMMAND:
						block = Material.COMMAND_BLOCK_MINECART;
						break;
	
					case MINECART_HOPPER:
						block = Material.HOPPER_MINECART;
						break;
					
					default:
						break;
				}

				if (block != null) {
					// Get permissions (updates if none exist)
					boolean bDestroy = PlayerCacheUtil.getCachePermission(attackingPlayer, defendingEntity.getLocation(), block, TownyPermission.ActionType.DESTROY);

					if (!bDestroy) {

						/*
						 * Fetch the players cache
						 */
						PlayerCache cache = plugin.getCache(attackingPlayer);

						if (cache.hasBlockErrMsg())
							TownyMessaging.sendErrorMsg(attackingPlayer, cache.getBlockErrMsg());

						return true;
					}

				}

			}
		}
		
		/*
		 * If attackingEntity is a tamed Wolf and...
		 * Defender is a player and...
		 * Either player or wolf is in a non-PVP area
		 * 
		 * Prevent pvp and remove Wolf targeting.
		 */
		if ( attackingEntity instanceof Wolf && ((Wolf) attackingEntity).isTamed() && defendingPlayer != null && (preventPvP(world, attackerTB) || preventPvP(world, defenderTB)) ) {
			((Wolf) attackingEntity).setTarget(null);
			return true;
		}
		

		return false;
	}

	/**
	 * Is PvP disabled in this TownBlock?
	 * 
	 * @param townBlock - TownBlock to check
	 * @param world - World to check if TownBlock is NULL
	 * @return true if PvP is disallowed
	 */
	public static boolean preventPvP(TownyWorld world, TownBlock townBlock) {

		if (townBlock != null) {
			try {

				/*
				 * Check the attackers TownBlock and it's Town for their PvP
				 * status. This will throw a NotRegisteredException right away 
				 * if it is in the wilderness. 
				 */
				if (townBlock.getTown().isAdminDisabledPVP())
					return true;

				// Checks PVP perm: 1. Plot PVP, 2. Town PVP, 3. World Force PVP 
				if (!townBlock.getPermissions().pvp && !townBlock.getTown().isPVP() && !world.isForcePVP())
					return true;
				
				if (townBlock.isHomeBlock() && world.isForcePVP() && TownySettings.isForcePvpNotAffectingHomeblocks())
					return true;

			} catch (NotRegisteredException ex) {
				/*
				 * Failed to fetch the town data
				 * so check world PvP
				 */
				if (!isWorldPvP(world))
					return true;
			}

		} else {

			/*
			 * Attacker isn't in a TownBlock so check the world PvP
			 */
			if (!isWorldPvP(world))
				return true;
		}
		return false;
	}

	/**
	 * Is PvP enabled in this world?
	 * 
	 * @param world - World to check
	 * @return true if the world is PvP
	 */
	public static boolean isWorldPvP(TownyWorld world) {

		// Universe is only PvP
		if (world.isForcePVP() || world.isPVP())
			return true;

		return false;
	}

	/**
	 * Should we be preventing friendly fire?
	 * 
	 * @param attacker - Attacking Player
	 * @param defender - Defending Player (receiving damage)
	 * @return true if we should cancel damage.
	 */
	public static boolean preventFriendlyFire(Player attacker, Player defender) {

		/*
		 * Don't block potion use (self damaging) on ourselves.
		 */
		if (attacker == defender)
			return false;

		if ((attacker != null) && (defender != null))
			if (!TownySettings.getFriendlyFire() && CombatUtil.isAlly(attacker.getName(), defender.getName())) {
				try {
					TownBlock townBlock = new WorldCoord(defender.getWorld().getName(), Coord.parseCoord(defender)).getTownBlock();
					if (!townBlock.getType().equals(TownBlockType.ARENA))
						attacker.sendMessage(Translation.of("msg_err_friendly_fire_disable"));
						return true;
				} catch (TownyException x) {
					// World or TownBlock failure
					// But we are configured to prevent friendly fire in the
					// wilderness too.					
					attacker.sendMessage(Translation.of("msg_err_friendly_fire_disable"));
					return true;
				}
			}		
		return false;
	}

	/**
	 * Return true if both attacker and defender are in Arena Plots.
	 * 
	 * @param attacker - Attacking Player
	 * @param defender - Defending Player (receiving damage)
	 * @return true if both players in an Arena plot.
	 */
	public static boolean isPvPPlot(Player attacker, Player defender) {

		if ((attacker != null) && (defender != null)) {
			TownBlock attackerTB, defenderTB;
			try {
				attackerTB = new WorldCoord(attacker.getWorld().getName(), Coord.parseCoord(attacker)).getTownBlock();
				defenderTB = new WorldCoord(defender.getWorld().getName(), Coord.parseCoord(defender)).getTownBlock();

				if (defenderTB.getType().equals(TownBlockType.ARENA) && attackerTB.getType().equals(TownBlockType.ARENA))
					return true;

			} catch (NotRegisteredException e) {
				// Not a Town owned Plot
			}
		}
		return false;
	}

	/**
	 * Is the defending resident an ally of the attacking resident?
	 * 
	 * @param attackingResident - Attacking Resident (String)
	 * @param defendingResident - Defending Resident (Receiving Damage; String)
	 * @return true if the defender is an ally of the attacker.
	 */
	public static boolean isAlly(String attackingResident, String defendingResident) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			Resident residentA = townyUniverse.getDataSource().getResident(attackingResident);
			Resident residentB = townyUniverse.getDataSource().getResident(defendingResident);
			if (residentA.getTown() == residentB.getTown())
				return true;
			if (residentA.getTown().getNation() == residentB.getTown().getNation())
				return true;
			if (residentA.getTown().getNation().hasAlly(residentB.getTown().getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Is town b an ally of town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are allies.
	 */
	public static boolean isAlly(Town a, Town b) {

		try {
			if (a == b)
				return true;
			if (a.getNation() == b.getNation())
				return true;
			if (a.getNation().hasAlly(b.getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Is town b in a nation with town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are allies.
	 */
	public static boolean isSameNation(Town a, Town b) {

		try {
			if (a == b)
				return true;
			if (a.getNation() == b.getNation())
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Is town b in a nation with town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are allies.
	 */
	public static boolean isSameTown(Town a, Town b) {

		if (a == b)
			return true;
		return false;
	}

	
	/**
	 * Can resident a attack resident b?
	 * 
	 * @param a - Resident A in comparison
	 * @param b - Resident B in comparison
	 * @return true if they can attack.
	 */
	public static boolean canAttackEnemy(String a, String b) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			Resident residentA = townyUniverse.getDataSource().getResident(a);
			Resident residentB = townyUniverse.getDataSource().getResident(b);
			if (residentA.getTown() == residentB.getTown())
				return false;
			if (residentA.getTown().getNation() == residentB.getTown().getNation())
				return false;
			Nation nationA = residentA.getTown().getNation();
			Nation nationB = residentB.getTown().getNation();
			if (nationA.isNeutral() || nationB.isNeutral())
				return false;
			if (nationA.hasEnemy(nationB))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Test if all the listed nations are allies
	 * 
	 * @param possibleAllies - List of Nations (List&lt;Nation&gt;)
	 * @return true if they are all allies
	 */
	public static boolean areAllAllies(List<Nation> possibleAllies) {

		if (possibleAllies.size() <= 1)
			return true;
		else {
			for (int i = 0; i < possibleAllies.size() - 1; i++)
				if (!possibleAllies.get(i).hasAlly(possibleAllies.get(i + 1)))
					return false;
			return true;
		}
	}

	/**
	 * Is resident b an enemy of resident a?
	 * 
	 * @param a - Resident A in comparison (String)
	 * @param b - Resident B in comparison (String)
	 * @return true if b is an enemy.
	 */
	public static boolean isEnemy(String a, String b) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			Resident residentA = townyUniverse.getDataSource().getResident(a);
			Resident residentB = townyUniverse.getDataSource().getResident(b);
			if (residentA.getTown() == residentB.getTown())
				return false;
			if (residentA.getTown().getNation() == residentB.getTown().getNation())
				return false;
			if (residentA.getTown().getNation().hasEnemy(residentB.getTown().getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Is town b an enemy of town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if b is an enemy.
	 */
	public static boolean isEnemy(Town a, Town b) {

		try {
			if (a == b)
				return false;
			if (a.getNation() == b.getNation())
				return false;
			if (a.getNation().hasEnemy(b.getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Does this WorldCoord fall within a plot owned by an enemy town?
	 * 
	 * @param player - Player
	 * @param worldCoord - Location
	 * @return true if it is an enemy plot.
	 */
	public boolean isEnemyTownBlock(Player player, WorldCoord worldCoord) {

		try {
			return CombatUtil.isEnemy(TownyUniverse.getInstance().getDataSource().getResident(player).getTown(), worldCoord.getTownBlock().getTown());
		} catch (NotRegisteredException e) {
			return false;
		}
	}
}
