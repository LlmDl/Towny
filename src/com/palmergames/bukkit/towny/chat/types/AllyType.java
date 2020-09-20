package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author creatorfromhell
 */
public class AllyType extends ChatType {
	public AllyType() {
		super("ally", "<gray>[<aqua>$nation<gray>]: <white>$message");
	}

	@Override
	public boolean canChat(Player player) {
		try {
			return TownyUniverse.getInstance().getDataSource().getResident(player).hasTown() && TownyUniverse.getInstance().getDataSource().getResident(player).getTown().hasNation();
		} catch(NotRegisteredException ignore) {

		}
		return false;
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			final Nation nation = townyUniverse.getDataSource().getResident(player).getTown().getNation();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				if (!townyUniverse.getDataSource().getResident(p).getTown().getNation().getUuid().equals(nation.getUuid())
						&& !townyUniverse.getDataSource().getResident(p).getTown().getNation().hasAlly(nation)) {
					continue;
				}
				newRecipients.add(p);
			}
			return newRecipients;
		} catch(NotRegisteredException ignore) {
		}
		return recipients;
	}
}