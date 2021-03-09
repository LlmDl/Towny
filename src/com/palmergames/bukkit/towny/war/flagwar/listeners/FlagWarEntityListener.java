package com.palmergames.bukkit.towny.war.flagwar.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;

public class FlagWarEntityListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent event) {

		for (Block block : event.blockList())
			FlagWar.checkBlock(null, block, event);
	}
	
	@EventHandler
	public void onPlayerDamagePlayer(TownyPlayerDamagePlayerEvent event) {
		if (FlagWarConfig.isAllowingAttacks() && event.hasTownBlock()) {
			/*
			 * Defending player is in a warzone, not an ally, allow the damage.
			 */
			if (event.getTownBlock().getWorld().isFlagWarZone(Coord.parseCoord(event.getEntity())) && !CombatUtil.isAlly(event.getAttackingPlayer().getName(), event.getVictimPlayer().getName()))
				event.setCancelled(false);
		}
	}
}
