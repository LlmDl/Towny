package com.palmergames.bukkit.towny.chat.checks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import net.tnemc.tnc.core.common.chat.ChatCheck;
import org.bukkit.entity.Player;

/**
 * @author creatorfromhell
 */
public class MayorCheck extends ChatCheck {
	@Override
	public String name() {
		return "ismayor";
	}

	@Override
	public boolean runCheck(Player player, String checkString) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			if(townyUniverse.getDataSource().getResident(player).hasTown()) {
				return townyUniverse.getDataSource().getResident(player).getTown().isMayor(townyUniverse.getDataSource().getResident(player));
			}
		} catch(NotRegisteredException ignore) {
		}
		return false;
	}
}