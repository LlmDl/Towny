package com.palmergames.bukkit.towny.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.executors.TownyDestroyEventExecutor;

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

			switch (event.getVehicle().getType()) {

			case MINECART:
				vehicle = Material.MINECART;
				break;
			
			case MINECART_FURNACE:
				vehicle = Material.FURNACE_MINECART;
				break;
			
			case MINECART_HOPPER:
				vehicle = Material.HOPPER_MINECART;
				break;
				
			case MINECART_CHEST:
				vehicle = Material.CHEST_MINECART;
				break;
				
			case MINECART_MOB_SPAWNER:
				vehicle = Material.MINECART;
				break;
			
			case MINECART_COMMAND:
				vehicle = Material.COMMAND_BLOCK_MINECART;
				break;
			
			case MINECART_TNT:
				vehicle = Material.TNT_MINECART;
				break;
				
			default:
				break;

			}
			
			if ((vehicle != null) && (!TownySettings.isItemUseMaterial(vehicle.toString())))
				return;

			if (vehicle != null) {
				//Begin decision on whether this is allowed using the PlayerCache and then a cancellable event.
				TownyDestroyEventExecutor internalEvent = new TownyDestroyEventExecutor(player, event.getVehicle().getLocation(), vehicle);
				event.setCancelled(internalEvent.isCancelled());
			}
		}
	}
}
