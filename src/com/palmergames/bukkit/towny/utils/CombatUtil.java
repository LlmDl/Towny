package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.damage.TownBlockPVPTestEvent;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.event.damage.WildernessPVPTestEvent;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

/**
 * 
 * @author ElgarL,Shade,LlmDl
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
	 * @param cause - The DamageCause behind this DamageCall.
	 * @return true if we should cancel.
	 */
	public static boolean preventDamageCall(Towny plugin, Entity attacker, Entity defender, DamageCause cause) {

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

			return preventDamageCall(plugin, world, attacker, defender, a, b, cause);

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
	 * @param cause - The DamageCause behind this DamageCall.
	 * @return true if we should cancel.
	 * @throws NotRegisteredException - Generic NotRegisteredException
	 */
	private static boolean preventDamageCall(Towny plugin, TownyWorld world, Entity attackingEntity, Entity defendingEntity, Player attackingPlayer, Player defendingPlayer, DamageCause cause) throws NotRegisteredException {

		TownBlock defenderTB = TownyAPI.getInstance().getTownBlock(defendingEntity.getLocation());
		TownBlock attackerTB = TownyAPI.getInstance().getTownBlock(attackingEntity.getLocation());
		/*
		 * We have an attacking player
		 */
		if (attackingPlayer != null) {

			boolean cancelled = false;
			
			/*
			 * Defender is a player.
			 */
			if (defendingPlayer != null) {
				
				/*
				 * Both townblocks are not Arena plots.
				 */
				if (!isArenaPlot(attackerTB, defenderTB)) {
					/*
					 * Check if we are preventing friendly fire between allies
					 * Check the attackers TownBlock and it's Town for their PvP status, else the world.
					 * Check the defenders TownBlock and it's Town for their PvP status, else the world.
					 */
					cancelled = preventFriendlyFire(attackingPlayer, defendingPlayer, world) || preventPvP(world, attackerTB) || preventPvP(world, defenderTB);
				}

				/*
				 * A player has attempted to damage a player. Throw a TownPlayerDamagePlayerEvent.
				 */
				TownyPlayerDamagePlayerEvent event = new TownyPlayerDamagePlayerEvent(defendingPlayer.getLocation(), defendingPlayer, cause, defenderTB, cancelled, attackingPlayer);
				BukkitTools.getPluginManager().callEvent(event);

				// A cancelled event should contain a message.
				if (event.isCancelled() && event.getMessage() != null)
					TownyMessaging.sendErrorMsg(attackingPlayer, event.getMessage());
				
				return event.isCancelled();

			/*
			 * Defender is not a player.
			 */
			} else {
				/*
				 * First test protections for Non-Player defenders who are being protected
				 * because they are specifically in Town-Claimed land.
				 */
				if (defenderTB != null) {
					
					/*
					 * Protect tamed dogs in town land which are not owned by the attacking player.
					 */
					if (defendingEntity instanceof Wolf && isNotTheAttackersPetDog((Wolf) defendingEntity, attackingPlayer))
						return true;
					
					/*
					 * Farm Animals - based on whether this is allowed using the PlayerCache and then a cancellable event.
					 */
					if (defenderTB.getType() == TownBlockType.FARM && TownySettings.getFarmAnimals().contains(defendingEntity.getType().toString()))
						return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), Material.WHEAT);

					/*
					 * Config's protected entities: Animals,WaterMob,NPC,Snowman,ArmorStand,Villager
					 */
					if (EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defendingEntity)) 						
						return(!TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), Material.DIRT));
				}
				
				/*
				 * Protect specific entity interactions (faked with Materials).
				 * Requires destroy permissions in either the Wilderness or in Town-Claimed land.
				 */
				Material material = null;

				switch (defendingEntity.getType()) {
					/*
					 * Below are the entities we specifically want to protect with this test.
					 * Any other entity will mean that block is still null and will not be
					 * tested with a destroy test.
					 */
					case ITEM_FRAME:
					case PAINTING:
					case ARMOR_STAND:
					case ENDER_CRYSTAL:
					case MINECART:
					case MINECART_CHEST:
					case MINECART_FURNACE:
					case MINECART_COMMAND:
					case MINECART_HOPPER:
						material = EntityTypeUtil.parseEntityToMaterial(defendingEntity.getType());
						break;
					
					default:
						break;
				}

				if (material != null) {
					//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
					return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), material);
				}
			}

		/*
		 * This is not an attack by a player....
		 */
		} else {

			/*
			 * If Defender is a player, Attacker is not.
			 */
			if (defendingPlayer != null) {

				/*
				 * If attackingEntity is a tamed Wolf and...
				 * Defender is a player and...
				 * Either player or wolf is in a non-PVP area
				 * 
				 * Prevent pvp and remove Wolf targeting.
				 */
				if ( attackingEntity instanceof Wolf && ((Wolf) attackingEntity).isTamed() && (preventPvP(world, attackerTB) || preventPvP(world, defenderTB))) {
					((Wolf) attackingEntity).setTarget(null);
					return true;
				}
				
				if (attackingEntity instanceof LightningStrike 
					&& world.hasTridentStrike(attackingEntity.getEntityId())
					&& preventPvP(world, defenderTB)) {
					return true;
				}
				
			/*
			 * DefendingEntity is not a player.
			 * This is now non-player vs non-player damage.
			 */
			} else {

			    /*
			     * Prevents projectiles fired by non-players harming non-player entities.
			     * Could be a monster or it could be a dispenser.
			     */
				if (attackingEntity instanceof Projectile) {
					return true;	
				}
			}
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

			/*
			 * Check the attackers TownBlock and it's Town for their PvP status.
			 */
			TownBlockPVPTestEvent event = new TownBlockPVPTestEvent(townBlock, isPvP(townBlock));
			Bukkit.getPluginManager().callEvent(event);
			return !event.isPvp();

		} else {

			/*
			 * Attacker isn't in a TownBlock so check the wilderness PvP status.
			 */
			WildernessPVPTestEvent event = new WildernessPVPTestEvent(world, isWorldPvP(world));
			Bukkit.getPluginManager().callEvent(event);
			return !event.isPvp();
		}
	}
	
	private static boolean isPvP(TownBlock townBlock) {
		
		try {
			if (townBlock.getTown().isAdminDisabledPVP())
				return false;

			// Checks PVP perm: 1. Plot PVP, 2. Town PVP, 3. World Force PVP 
			if (!townBlock.getPermissions().pvp && !townBlock.getTown().isPVP() && !townBlock.getWorld().isForcePVP()) 
				return false;
			
			if (townBlock.isHomeBlock() && townBlock.getWorld().isForcePVP() && TownySettings.isForcePvpNotAffectingHomeblocks())
				return false;
		} catch (NotRegisteredException ignored) {}
		
		return true;
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
	 * @deprecated as of 0.96.2.20 use {@link CombatUtil#preventFriendlyFire(Player, Player, TownyWorld) instead}
	 * Should we be preventing friendly fire?
	 * 
	 * @param attacker - Attacking Player
	 * @param defender - Defending Player (receiving damage)
	 * 
	 * @return true if we should cancel damage.
	 */
	@Deprecated
	public static boolean preventFriendlyFire(Player attacker, Player defender) {
		TownyWorld world = null;
		try {
			world = TownyUniverse.getInstance().getDataSource().getWorld(attacker.getLocation().getWorld().getName());
		} catch (NotRegisteredException ignored) {}
		return preventFriendlyFire(attacker, defender, world);
	}
	
	/**
	 * Should we be preventing friendly fire?
	 * 
	 * @param attacker - Attacking Player
	 * @param defender - Defending Player (receiving damage)
	 * @param world - TownyWorld being tested.
	 * @return true if we should cancel damage.
	 */
	public static boolean preventFriendlyFire(Player attacker, Player defender, TownyWorld world) {

		/*
		 * Don't block potion use (self damaging) on ourselves.
		 */
		if (attacker == defender)
			return false;

		if ((attacker != null) && (defender != null))
			if (!world.isFriendlyFireEnabled() && CombatUtil.isAlly(attacker.getName(), defender.getName())) {
				if (isArenaPlot(attacker, defender))
					return false;
				
				TownyMessaging.sendErrorMsg(attacker, Translation.of("msg_err_friendly_fire_disable"));
				return true;
			}		
		return false;
	}

	/**
	 * Returns true if both players are in an arena townblock.
	 * @param attacker Attacking Player
	 * @param defender Defending Player
	 * @return true if both player in an Arena plot.
	 */
	public static boolean isArenaPlot(Player attacker, Player defender) {
		TownBlock attackerTB = TownyAPI.getInstance().getTownBlock(attacker.getLocation());
		TownBlock defenderTB = TownyAPI.getInstance().getTownBlock(defender.getLocation());
		return isArenaPlot(attackerTB, defenderTB);
	}
	
	/**
	 * Return true if both TownBlocks are Arena plots.
	 * 
	 * @param defenderTB TownBlock being tested.
	 * @param attackerTB TownBlock being tested.
	 * @return true if both TownBlocks are Arena plots.
	 */
	public static boolean isArenaPlot(TownBlock attackerTB, TownBlock defenderTB) {

		if (defenderTB != null && attackerTB != null && defenderTB.getType() == TownBlockType.ARENA && attackerTB.getType() == TownBlockType.ARENA)
			return true;
		return false;
	}
	
	/**
	 * @deprecated as of 0.96.7.1. Use {@link CombatUtil#isArenaPlot(Player, Player)}.
	 * Return true if both attacker and defender are in Arena Plots.
	 * 
	 * @param attacker - Attacking Player
	 * @param defender - Defending Player (receiving damage)
	 * @return true if both players in an Arena plot.
	 */
	@Deprecated
	public static boolean isPvPPlot(Player attacker, Player defender) {

		return isArenaPlot(attacker, defender);
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

		Resident residentA = townyUniverse.getResident(attackingResident);
		Resident residentB = townyUniverse.getResident(defendingResident);
		
		// Fast-fail
		if (residentA == null || residentB == null || !residentA.hasTown() || !residentB.hasTown())
			return false;
		
		try {
			if (residentA.getTown().equals(residentB.getTown()))
				return true;
			
			if (residentA.getTown().getNation().equals(residentB.getTown().getNation()))
				return true;
			
			if (residentA.getTown().getNation().hasAlly(residentB.getTown().getNation()))
				return true;
		} catch (NotRegisteredException ignored) {}
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
		} catch (NotRegisteredException ignored) {}
		return false;
	}

	/**
	 * Is town b in a nation with town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are in the same nation.
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
	 * Is resident a in a nation with resident b?
	 * 
	 * @param a - Resident A in comparison.
	 * @param b - Resident B in comparison.
	 * @return true if they are in the same nation.
	 */
	public static boolean isSameNation(Resident a, Resident b) {
		if (!a.hasTown() || !b.hasTown())
			return false;
		
		Town townA = null;
		Town townB = null;
		try {
			townA = a.getTown();
			townB = b.getTown();
		} catch (NotRegisteredException e) {
			return false;
		}
				
		return isSameNation(townA, townB);
	}
	
	
	/**
	 * Is resident a in a town with resident b?
	 * @param a - Resident A in comparison.
	 * @param b - Resident B in comparison.
	 * @return true if they are in the same town.
	 */
	public static boolean isSameTown(Resident a, Resident b) {
		if (!a.hasTown() || !b.hasTown())
			return false;
		
		Town townA = null;
		Town townB = null;
		try {
			townA = a.getTown();
			townB = b.getTown();
		} catch (NotRegisteredException e) {
			return false;
		}
		
		return isSameTown(townA, townB);
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
		Resident residentA = townyUniverse.getResident(a);
		Resident residentB = townyUniverse.getResident(b);
		
		// Fast-fail
		if (residentA == null || residentB == null || !residentA.hasTown() || !residentB.hasTown())
			return false;
		
		try {
			if (residentA.getTown().equals(residentB.getTown()))
				return false;
			if (residentA.getTown().getNation().equals(residentB.getTown().getNation()))
				return false;
			Nation nationA = residentA.getTown().getNation();
			Nation nationB = residentB.getTown().getNation();
			if (nationA.isNeutral() || nationB.isNeutral())
				return false;
			if (nationA.hasEnemy(nationB))
				return true;
		} catch (NotRegisteredException ignored) {}
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
	 * Test if all the listed residents are friends
	 * 
	 * @param possibleFriends - List of Residents (List&lt;Resident&gt;)
	 * @return true if they are all friends
	 */
	public static boolean areAllFriends(List<Resident> possibleFriends) {

		if (possibleFriends.size() <= 1)
			return true;
		else {
			for (int i = 0; i < possibleFriends.size() - 1; i++)
				if (!possibleFriends.get(i).hasFriend(possibleFriends.get(i + 1)))
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
		Resident residentA = townyUniverse.getResident(a);
		Resident residentB = townyUniverse.getResident(b);
		
		if (residentA == null || residentB == null || !residentA.hasNation() || !residentB.hasNation())
			return false;
		
		try {
			if (residentA.getTown().equals(residentB.getTown()))
				return false;
			if (residentA.getTown().getNation().equals(residentB.getTown().getNation()))
				return false;
			if (residentA.getTown().getNation().hasEnemy(residentB.getTown().getNation()))
				return true;
		} catch (NotRegisteredException ignored) {}
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
		} catch (NotRegisteredException ignored) {}
		return false;
	}

	/**
	 * Does this WorldCoord fall within a plot owned by an enemy town?
	 * 
	 * @param player - Player
	 * @param worldCoord - Location
	 * @return true if it is an enemy plot.
	 */
	public static boolean isEnemyTownBlock(Player player, WorldCoord worldCoord) {
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		try {
			if (resident != null && resident.hasTown())
				return CombatUtil.isEnemy(resident.getTown(), worldCoord.getTownBlock().getTown());
		} catch (NotRegisteredException ignored) {}
		return false;
	}
	
	/**
	 * 
	 * @param wolf Wolf being attacked by a player.
	 * @param attackingPlayer Player attacking the wolf.
	 * @return true when a dog who is not owned by the attacker is injured inside of a town's plot.
	 */
	private static boolean isNotTheAttackersPetDog(Wolf wolf, Player attackingPlayer) {
		return wolf.isTamed() && !wolf.getOwner().equals(attackingPlayer);
	}
}
