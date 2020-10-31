package com.palmergames.bukkit.towny.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;

/**
 * Handle events for all Vehicle related events
 * 
 * @author ElgarL
 * 
 */
public class TownyVehicleListener implements Listener {
	
	private final Towny plugin;

	public TownyVehicleListener(Towny instance) {

		plugin = instance;
	}

	
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onVehicleDestroy(VehicleDestroyEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (event.getAttacker() instanceof Player) {
			
			Player player = (Player) event.getAttacker();
			Material vehicle = null;

			/*
			 * Substitute a Material for the Entity so we can run a destroy test against it.
			 * Any entity not in the switch statement will leave vehicle null and no test will occur.
			 */
			switch (event.getVehicle().getType()) {
				case MINECART:
				case MINECART_FURNACE:
				case MINECART_HOPPER:
				case MINECART_CHEST:
				case MINECART_MOB_SPAWNER:
				case MINECART_COMMAND:
				case MINECART_TNT:
					vehicle = EntityTypeUtil.parseEntityToMaterial(event.getVehicle().getType());
					break;
				default:
					break;
			}

			if (vehicle != null) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getVehicle().getLocation(), vehicle));
			}
		}
	}
}
