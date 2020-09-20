package com.palmergames.bukkit.towny.chat.checks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import net.tnemc.tnc.core.common.chat.ChatCheck;
import org.bukkit.entity.Player;

/**
 * @author creatorfromhell
 */
public class KingCheck extends ChatCheck {
	@Override
	public String name() {
		return "isking";
	}

	@Override
	public boolean runCheck(Player player, String checkString) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			if(townyUniverse.getDataSource().getResident(player).hasNation()) {
				return townyUniverse.getDataSource().getResident(player).getTown().getNation().isKing(townyUniverse.getDataSource().getResident(player));
			}
		} catch(NotRegisteredException ignore) {
		}
		return false;
	}
}