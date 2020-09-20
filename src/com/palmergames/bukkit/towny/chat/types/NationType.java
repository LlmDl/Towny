package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author creatorfromhell
 */
public class NationType extends ChatType {
	public NationType() {
		super("nation", "<gray>[<aqua>$town<gray>]$display: <white>$message");
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
			final UUID nation = townyUniverse.getDataSource().getResident(player).getTown().getNation().getUuid();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				if(townyUniverse.getDataSource().getResident(p).getTown().getNation().getUuid().equals(nation)) {
					newRecipients.add(p);
				}
			}
			return newRecipients;
		} catch(NotRegisteredException e) {
			e.printStackTrace();
		}
		return recipients;
	}
}