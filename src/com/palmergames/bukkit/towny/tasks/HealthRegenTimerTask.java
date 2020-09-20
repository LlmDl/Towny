package com.palmergames.bukkit.towny.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.CombatUtil;

public class HealthRegenTimerTask extends TownyTimerTask {

	private final Server server;

	public HealthRegenTimerTask(Towny plugin, Server server) {

		super(plugin);
		this.server = server;
	}

	@Override
	public void run() {

		if (TownyAPI.getInstance().isWarTime())
			return;

		for (Player player : server.getOnlinePlayers()) {
			if (player.getHealth() <= 0)
				continue;

			try {
				if (TownyAPI.getInstance().isWilderness(player.getLocation()))
					continue;
				
				TownBlock townBlock = TownyUniverse.getInstance().getTownBlock(new WorldCoord(WorldCoord.parseWorldCoord(player.getLocation())));
				
				if (townBlock != null && CombatUtil.isAlly(townBlock.getTown(), TownyUniverse.getInstance().getDataSource().getResident(player).getTown()))
					if (!townBlock.getType().equals(TownBlockType.ARENA)) // only regen if not in an arena
						incHealth(player);
			} catch (TownyException x) {
			}
		}

		//if (TownySettings.getDebug())
		//	System.out.println("[Towny] Debug: Health Regen");
	}

	public void incHealth(Player player) {

		// Keep saturation above zero while in town.
		float currentSat = player.getSaturation();
		if (currentSat == 0) {
			
			player.setSaturation(1F);
		}
		
		// Heal while in town.
		double currentHP = player.getHealth();
		if (currentHP < player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
			player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), ++currentHP));

			// Raise an event so other plugins can keep in sync.
			EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, currentHP, RegainReason.REGEN);
			Bukkit.getServer().getPluginManager().callEvent(event);

		}
		
	}

}
