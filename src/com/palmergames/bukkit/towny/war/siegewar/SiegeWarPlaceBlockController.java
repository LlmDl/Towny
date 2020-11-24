package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.playeractions.*;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This class intercepts 'place block' events coming from the towny block listener class
 *
 * The class evaluates the event, and determines if it is siege related e.g.:
 * 1. A siege attack request  (place coloured banner outside town)
 * 2. A siege abandon request  (place white banner near attack banner)
 * 3. A town surrender request  (place white banner in town)
 * 4. A town invasion request (place chest near attack banner)
 * 5. A town plunder request (place coloured banner near attack banner)
 * 6. A siege-forbidden block
 * 7. None of the above
 * 
 * If the place block event is determined to be a siege action,
 * this class then calls an appropriate class/method in the 'playeractions' package
 *
 * @author Goosius
 */
public class SiegeWarPlaceBlockController {
	
	/**
	 * Evaluates a block placement request.
	 * If the block is a standing banner or chest, this method calls an appropriate private method.
	 *
	 * @param player The player placing the block
	 * @param block The block about to be placed
	 * @param event The event object related to the block placement    	
	 * @param plugin The Towny object
	 * @return true if subsequent perm checks for the event should be skipped
	 */
	public static boolean evaluateSiegeWarPlaceBlockRequest(Player player, Block block, BlockPlaceEvent event, Towny plugin) {
		
		Material mat = block.getType();
		//Banner placement
		if (Tag.BANNERS.isTagged(mat))
			return evaluatePlaceBanner(player, block, event, plugin);

		//Chest placement
		if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST)
			return evaluatePlaceChest(player, block, event);

		//Check for forbidden block placement
		if(TownySettings.isWarSiegeZoneBlockPlacementRestrictionsEnabled() && TownyAPI.getInstance().isWilderness(block) && SiegeWarDistanceUtil.isLocationInActiveSiegeZone(block.getLocation())) {
			if(TownySettings.getWarSiegeZoneBlockPlacementRestrictionsMaterials().contains(mat)) {
				event.setCancelled(true);
				event.setBuild(false);
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_war_siege_zone_block_placement_forbidden"));
				return true;
			}
		}

		//Block placement unaffected
		return false;

	}

	/**
 	 * Evaluates a banner placement request.
     * Determines which type of banner this is, and where it is being placed.
	 * Then calls an appropriate private method.
 	*/
	private static boolean evaluatePlaceBanner(Player player, Block block, BlockPlaceEvent event, Towny plugin) {

		if(TownyAPI.getInstance().isWilderness(block)) {
			//Wilderness found
			if (isSurrenderBanner(block)) {
				return evaluatePlaceWhiteBannerInWilderness(block, player, event);
			} else {
				return evaluatePlaceColouredBannerInWilderness(block, player, event, plugin);
			}
		} else {
			Town town = TownyAPI.getInstance().getTown(block.getLocation());
			//Town block found 
			if (town.hasSiege() && isSurrenderBanner(block)) {
				return evaluatePlaceWhiteBannerInTown(player, town, event);
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Evaluates placing a white banner in the wilderness.
	 * Determines if the event will be considered as an abandon request.
	 */
	private static boolean evaluatePlaceWhiteBannerInWilderness(Block block, Player player, BlockPlaceEvent event) {
		if (!TownySettings.getWarSiegeAbandonEnabled())
			return false;

		//Find the nearest siege zone to the player
		Siege nearestSiege = SiegeWarDistanceUtil.findNearestSiege(block);
		
		//If there are no nearby siege zones,then regular block request
		if(nearestSiege == null)
			return false;
		
		AbandonAttack.processAbandonSiegeRequest(player,
			nearestSiege,
			event);

		return true;
	}

	/**
	 * Evaluates placing a coloured banner in the wilderness.
	 * Determines if the event will be considered as an attack or invade request.
	 */
	private static boolean evaluatePlaceColouredBannerInWilderness(Block block, Player player, BlockPlaceEvent event, Towny plugin) {
		try {
			// Fail early if this is not a siege-enabled world.
			if(!SiegeWarDistanceUtil.isSiegeWarEnabledInWorld(block.getWorld()))
				throw new TownyException(Translation.of("msg_err_siege_war_not_enabled_in_world"));
			
			Resident resident = null;
			try {
				resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
			} catch (NotRegisteredException ignored) {}
			
			// Fail early if this Resident has no Town.
			if(!resident.hasTown())
				throw new TownyException(Translation.of("msg_err_siege_war_action_not_a_town_member"));
	
			// Fail early if this Resident's Town has no Nation.
			if(!resident.getTown().hasNation())
				throw new TownyException(Translation.of("msg_err_siege_war_action_not_a_nation_member"));
			
			List<TownBlock> nearbyCardinalTownBlocks = SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(player, block);
	
			//If no townblocks are nearby, do normal block placement
			if (nearbyCardinalTownBlocks.size() == 0)
				return false;
	
			//Ensure that only one of the cardinal points has a townblock
			if(nearbyCardinalTownBlocks.size() > 1) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_too_many_adjacent_cardinal_town_blocks"));
				event.setBuild(false);
				event.setCancelled(true);
				return true;
			}
	
			//Get nearby town
			Town town;
			try {
				town = nearbyCardinalTownBlocks.get(0).getTown();
			} catch (NotRegisteredException e) {
				return false;
			}
	
			//Ensure that there is only one town adjacent
			List<TownBlock> adjacentTownBlocks = new ArrayList<>();
			adjacentTownBlocks.addAll(nearbyCardinalTownBlocks);
			adjacentTownBlocks.addAll(SiegeWarBlockUtil.getNonCardinalAdjacentTownBlocks(player, block));
			for(TownBlock adjacentTownBlock: adjacentTownBlocks) {
				try {
					if (adjacentTownBlock.getTown() != town) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_too_many_adjacent_towns"));
						event.setBuild(false);
						event.setCancelled(true);
						return true;
					}
				} catch (NotRegisteredException nre) {}
			}

			//If the town has a siege where the player's nation is already attacking, 
			//attempt invasion, otherwise attempt attack
			Nation nationOfResident = resident.getTown().getNation();
			if(town.hasSiege() && town.getSiege().getAttackingNation() == nationOfResident) {

				if (!TownySettings.getWarSiegeInvadeEnabled())
					return false;

				InvadeTown.processInvadeTownRequest(
					plugin,
					player,
					town,
					event);

			} else {

				if (!TownySettings.getWarSiegeAttackEnabled())
					return false;

				if(SiegeWarBlockUtil.isSupportBlockUnstable(block)) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_banner_support_block_not_stable"));
					event.setBuild(false);
					event.setCancelled(true);
					return true;
				}

				AttackTown.processAttackTownRequest(
					player,
					block,
					nearbyCardinalTownBlocks.get(0),
					town,
					event);
			}

		} catch (TownyException x) {
			event.setBuild(false);
			event.setCancelled(true);
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return true;
		}

		return true;
	}

	/**
	 * Evaluates placing a white banner inside a town.
	 * Determines if the event will be considered as a surrender request.
	 */
    private static boolean evaluatePlaceWhiteBannerInTown(Player player, Town town, BlockPlaceEvent event) {
		if (!TownySettings.getWarSiegeSurrenderEnabled())
			return false;

		//If there is no siege, do normal block placement
		if (!town.hasSiege())
			return false;
		
		SurrenderTown.processTownSurrenderRequest(
			player,
			town,
			event);
		return true;
	}
	
	/**
	 * Evaluates placing a chest.
	 * Determines if the event will be considered as a plunder request.
	 */
	private static boolean evaluatePlaceChest(Player player, Block block, BlockPlaceEvent event) {
		if (!TownySettings.getWarSiegePlunderEnabled() || !TownyAPI.getInstance().isWilderness(block))
			return false;

		List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(player, block);
		if (nearbyTownBlocks.size() == 0)
			return false;   //No town blocks are nearby. Normal block placement

		if (nearbyTownBlocks.size() > 1) {
			//More than one town block nearby. Error
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_siege_war_too_many_town_blocks_nearby"));
			event.setBuild(false);
			event.setCancelled(true);
			return true;
		}

		//Get nearby town
		Town town = null;
		try {
			town = nearbyTownBlocks.get(0).getTown();
		} catch (NotRegisteredException ignored) {}

		//If there is no siege, do normal block placement
		if(!town.hasSiege())
			return false;

		//Attempt plunder.
		PlunderTown.processPlunderTownRequest(player, town, event);
		return true;

	}
	
	private static boolean isSurrenderBanner(Block block) {
		return block.getType() == Material.WHITE_BANNER  && ((Banner) block.getState()).getPatterns().size() == 0;
	}
}

