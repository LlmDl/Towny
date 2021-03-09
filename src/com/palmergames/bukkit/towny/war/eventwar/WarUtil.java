package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.util.BukkitTools;

public class WarUtil {
	
	/** 
	 * Allows War Event to piggy back off of Flag War editable materials, while accounting for neutral nations.
	 * 
	 * @param player - Player who is being tested for neutrality.
	 * @return Whether a player is considered neutral. 
	 */
	public static boolean isPlayerNeutral(Player player) {
		if (TownyAPI.getInstance().isWarTime()) {
			try {
				Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
				if (resident != null) {
					if (resident.isJailed())
						return true;
					if (resident.hasTown())
						if (!TownyUniverse.getInstance().hasWarEvent(resident.getTown()))
							return true;
				}
			} catch (NotRegisteredException e) {
			}			
		}		
		return false;
	}
	
	/**
	 * Launch a {@link Firework} at a given plot
	 * @param townblock - The {@link TownBlock} to fire in
	 * @param atPlayer - The {@link Player} in which the location is grabbed
	 * @param type - The {@link FireworkEffect} type
	 * @param c - The Firework {@link Color}
	 */
	public static void launchFireworkAtPlot(final TownBlock townblock, final Player atPlayer, final FireworkEffect.Type type, final Color c)
	{
		// Check the config. If false, do not launch a firework.
		if (!TownySettings.getPlotsFireworkOnAttacked()) {
			return;
		}
		
		BukkitTools.scheduleSyncDelayedTask(() -> {
			double x = (double)townblock.getX() * Coord.getCellSize() + Coord.getCellSize()/2.0;
			double z = (double)townblock.getZ() * Coord.getCellSize() + Coord.getCellSize()/2.0;
			double y = atPlayer.getLocation().getY() + 20;
			Firework firework = atPlayer.getWorld().spawn(new Location(atPlayer.getWorld(), x, y, z), Firework.class);
			FireworkMeta data = firework.getFireworkMeta();
			data.addEffects(FireworkEffect.builder().withColor(c).with(type).trail(false).build());
			firework.setFireworkMeta(data);
			firework.detonate();
		}, 0);
	}
}
