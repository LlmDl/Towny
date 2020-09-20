package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

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
				Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player);
				if (resident.isJailed())
					return true;
				if (resident.hasTown())
					if (!War.isWarringTown(resident.getTown()))
						return true;
			} catch (NotRegisteredException e) {
			}			
		}		
		return false;
	}
}
