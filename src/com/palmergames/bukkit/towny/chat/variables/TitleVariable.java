package com.palmergames.bukkit.towny.chat.variables;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import net.tnemc.tnc.core.common.chat.ChatVariable;
import org.bukkit.entity.Player;

/**
 * @author creatorfromhell
 */
public class TitleVariable extends ChatVariable {
	@Override
	public String name() {
		return "$title";
	}

	@Override
	public String parse(Player player, String message) {
		try {
			return TownyUniverse.getInstance().getDataSource().getResident(player).getTitle();
		} catch(NotRegisteredException ignore) {
		}
		return "";
	}
}