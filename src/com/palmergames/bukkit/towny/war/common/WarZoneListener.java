package com.palmergames.bukkit.towny.war.common;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.damage.TownyExplosionDamagesEntityEvent;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;

public class WarZoneListener implements Listener {
	
	private final Towny plugin;
	
	public WarZoneListener(Towny instance) {

		plugin = instance;
	}
	
	@EventHandler
	public void onDestroy(TownyDestroyEvent event) {
		if (event.isInWilderness())
			return;
		
		Player player = event.getPlayer();
		Material mat = event.getMaterial();
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow destroy for Event War if material is an EditableMaterial, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_edit_material", "destroy", mat.toString().toLowerCase()));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onBuild(TownyBuildEvent event) {
		if (event.isInWilderness())
			return;
		
		Player player = event.getPlayer();
		Material mat = event.getMaterial();
		TownBlockStatus status = plugin.getCache(player).getStatus();
		
		// Allow build for Event War if material is an EditableMaterial, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War 
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_edit_material", "build", mat.toString().toLowerCase()));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onItemUse(TownyItemuseEvent event) {
		if (event.isInWilderness())
			return;
		
		Player player = event.getPlayer();
		TownBlockStatus status = plugin.getCache(event.getPlayer()).getStatus();
		
		// Allow item_use for Event War if isAllowingItemUseInWarZone is true, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingItemUseInWarZone()) {				
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_use_item"));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onSwitchUse(TownySwitchEvent event) {
		if (event.isInWilderness())
			return;
		
		Player player = event.getPlayer();
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow switch for Event War if isAllowingSwitchesInWarZone is true, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingSwitchesInWarZone()) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_use_switches"));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onExplosionDamagingBlocks(TownyExplodingBlocksEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;

		List<Block> alreadyAllowed = event.getTownyFilteredBlockList();
		List<Block> toAllow = new ArrayList<Block>();
		
		int count = 0;
		for (Block block : event.getVanillaBlockList()) {
			// Wilderness, skip it.
			if (TownyAPI.getInstance().isWilderness(block))
				continue;
			
			// Non-warzone, skip it.
			if (!TownyUniverse.getInstance().hasWarEvent(TownyAPI.getInstance().getTownBlock(block.getLocation())))
				continue;
			
			// A war that doesn't allow any kind of explosions.
			if (!WarZoneConfig.isAllowingExplosionsInWarZone()) {
				// Remove from the alreadyAllowed list if it exists there.
				if (alreadyAllowed.contains(block))
					alreadyAllowed.remove(block);
				continue;
			}

			// A war that does allow explosions and explosions regenerate.
			if (WarZoneConfig.regenBlocksAfterExplosionInWarZone()) {
				// Skip this block if it is in the ignore list. TODO: with the blockdata nowadays this might not even be necessary.
				if (WarZoneConfig.getExplosionsIgnoreList().contains(block.getType().name()) || WarZoneConfig.getExplosionsIgnoreList().contains(block.getRelative(BlockFace.UP).getType().toString())) {
					// Remove from the alreadyAllowed list if it exists there.
					if (alreadyAllowed.contains(block))
						alreadyAllowed.remove(block);
					continue;
				}
				count++;
				TownyRegenAPI.beginProtectionRegenTask(block, count, TownyAPI.getInstance().getTownyWorld(block.getLocation().getWorld().getName()), event);
			}
			// This is an allowed explosion, so add it to our War-allowed list.
			toAllow.add(block);
		}
		// Add all TownyFilteredBlocks to our list, since our list will be used.
		toAllow.addAll(alreadyAllowed);
		
		// Return the list of allowed blocks for this block explosion event.
		event.setBlockList(toAllow);
	}

	@EventHandler
	public void onExplosionDamagingEntity(TownyExplosionDamagesEntityEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		/*
		 * Handle occasions in the wilderness first.
		 */
		if (event.isInWilderness())
			return;

		/*
		 * Must be inside of a town.
		 */
		
		// Not in a war zone, do not modify the outcome of the event.
		if (!TownyUniverse.getInstance().hasWarEvent(TownyAPI.getInstance().getTownBlock(event.getLocation())))
			return;
			
		/*
		 * Stops any type of exploding damage if wars are not allowing explosions.
		 */
		if (!WarZoneConfig.isAllowingExplosionsInWarZone()) {
			event.setCancelled(true);
			return;
		}

		/*
		 * Explosions must be allowed, so un-cancel the event.
		 */
		event.setCancelled(false);
	}
	
	@EventHandler
	public void onBurn(TownyBurnEvent event) {
		/*
		 * Return early if this is in the wild.
		 */
		if (event.isInWilderness())
			return;

		/*
		 * Is this a Town with a flag war WarZone?
		 */
		boolean inFlagWarTown = TownyAPI.getInstance().getTownyWorld(event.getBlock().getWorld().getName()).isFlagWarZone(Coord.parseCoord(event.getLocation()));

		/*
		 * Is this in a Town with an Event War?
		 */
		boolean inEventWarTown = TownyAPI.getInstance().isWarTime() && TownyUniverse.getInstance().hasWarEvent(event.getTownBlock());

		/*
		 * Event War (inWarringTown) & Flag War (isWarZone(coord)) fire control settings.
		 */
		if (inFlagWarTown || inEventWarTown) {
			if (WarZoneConfig.isAllowingFireInWarZone()) {                         // Allow ignition using normal fire-during-war rule.
				event.setCancelled(false);
			} else if (inEventWarTown && TownySettings.isAllowWarBlockGriefing()) { // Allow ignition using exceptionally-griefy-war rule for Event War.
				event.setCancelled(false);
			} else {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onNationToggleNeutral(NationToggleNeutralEvent event) {
		if (!TownySettings.isDeclaringNeutral() && event.getFutureState()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_err_fight_like_king"));
		}

	}
	
	@EventHandler
	public void onPlayerDamagePlayer(TownyPlayerDamagePlayerEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		try {
			
			Town attackerTown = event.getAttackerTown();
			Town defenderTown = event.getVictimTown();
			
			//Cancel because one of two players has no town and should not be interfering during war.
			if (TownySettings.isWarTimeTownsNeutral() && (event.getAttackerTown() == null || event.getVictimTown() == null)){
				event.setMessage(Translation.of("msg_war_a_player_has_no_town"));
				event.setCancelled(true);
				return;
			}

			//Cancel because one of the two players' town has no nation and should not be interfering during war.  AND towns_are_neutral is true in the config.
			if (TownySettings.isWarTimeTownsNeutral() && (!attackerTown.hasNation() || !defenderTown.hasNation())) {
				event.setMessage(Translation.of("msg_war_a_player_has_no_nation"));
				event.setCancelled(true);
				return;
			}
			
			//Cancel because one of the two player's nations is neutral.
			if ((attackerTown.hasNation() && attackerTown.getNation().isNeutral()) || (defenderTown.hasNation() && defenderTown.getNation().isNeutral())) {
				event.setMessage(Translation.of("msg_war_a_player_has_a_neutral_nation"));
				event.setCancelled(true);
				return;
			}
			
			//Cancel because one of the two players are no longer involved in the war.
			if (!TownyUniverse.getInstance().hasWarEvent(defenderTown) || !TownyUniverse.getInstance().hasWarEvent(attackerTown)) {
				event.setMessage(Translation.of("msg_war_a_player_has_been_removed_from_war"));
				event.setCancelled(true);
				return;
			}
			
			//Cancel because one of the two players considers the other an ally.
			if (CombatUtil.isAlly(attackerTown, defenderTown)){
				event.setMessage(Translation.of("msg_war_a_player_is_an_ally"));
				event.setCancelled(true);
				return;
			}
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}
		
	}
}
