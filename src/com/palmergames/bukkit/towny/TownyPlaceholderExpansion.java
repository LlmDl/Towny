package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.util.StringMgmt;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * This class will be registered through the register-method in the plugins
 * onEnable-method.
 */
public class TownyPlaceholderExpansion extends PlaceholderExpansion {

	final String nomad = Translation.of("nomad_sing");
	final String res = Translation.of("res_sing");
	final String mayor = Translation.of("mayor_sing");
	final String king = Translation.of("king_sing");
	
	private final Towny plugin;

	/**
	 * Since we register the expansion inside our own plugin, we can simply use this
	 * method here to get an instance of our plugin.
	 *
	 * @param plugin The instance of our plugin.
	 */
	public TownyPlaceholderExpansion(Towny plugin) {
		this.plugin = plugin;
	}

	/**
	 * Because this is an internal class, you must override this method to let
	 * PlaceholderAPI know to not unregister your expansion class when
	 * PlaceholderAPI is reloaded
	 *
	 * @return true to persist through reloads
	 */
	@Override
	public boolean persist() {
		return true;
	}

	/**
	 * Because this is a internal class, this check is not needed and we can simply
	 * return {@code true}
	 *
	 * @return Always true since it's an internal class.
	 */
	@Override
	public boolean canRegister() {
		return true;
	}

	/**
	 * The name of the person who created this expansion should go here. <br>
	 * For convienience do we return the author from the plugin.yml
	 * 
	 * @return The name of the author as a String.
	 */
	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthors().toString();
	}

	/**
	 * The placeholder identifier should go here. <br>
	 * This is what tells PlaceholderAPI to call our onRequest method to obtain a
	 * value if a placeholder starts with our identifier. <br>
	 * This must be unique and can not contain % or _
	 *
	 * @return The identifier in {@code %<identifier>_<value>%} as String.
	 */
	@Override
	public String getIdentifier() {
		return "townyadvanced";
	}

	/**
	 * This is the version of the expansion. <br>
	 * You don't have to use numbers, since it is set as a String.
	 *
	 * For convienience do we return the version from the plugin.yml
	 *
	 * @return The version as a String.
	 */
	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	/**
	 * This is the method called when a placeholder with our identifier is found and
	 * needs a value. <br>
	 * We specify the value identifier in this method. <br>
	 * Since version 2.9.1 can you use OfflinePlayers in your requests.
	 *
	 * @param player     A {@link org.bukkit.entity.Player Player}.
	 * @param identifier A String containing the identifier/value.
	 *
	 * @return possibly-null String of the requested identifier.
	 */
	@Override
	public String onPlaceholderRequest(Player player, String identifier) {

		if (player == null) {
			return "";
		}
		Resident resident;
		try {
			resident = TownyAPI.getInstance().getDataSource().getResident(player);
		} catch (NotRegisteredException e) {
			return null;
		}
		TownBlock townblock = TownyAPI.getInstance().getTownBlock(player.getLocation());
		String town = "";
		String nation = "";
		String balance = "";
		String tag = "";
		String title = "";
		String amount = "";
		String name = "";
		String rank = "";
		Double cost = 0.0;

		switch (identifier) {
		case "town": // %townyadvanced_town%
			try {
				town = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getName());
			} catch (NotRegisteredException ignored) {
			}
			return StringMgmt.remUnderscore(town);
		case "town_formatted": // %townyadvanced_town_formatted%
			try {
				town = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getFormattedName());
			} catch (NotRegisteredException ignored) {
			}
			return StringMgmt.remUnderscore(town);
		case "nation": // %townyadvanced_nation%
			try {
				nation = String.format(TownySettings.getPAPIFormattingNation(),
						resident.getTown().getNation().getName());
			} catch (NotRegisteredException ignored) {
			}
			return StringMgmt.remUnderscore(nation);
		case "nation_formatted": // %townyadvanced_nation_formatted%
			try {
				nation = String.format(TownySettings.getPAPIFormattingNation(),
						resident.getTown().getNation().getFormattedName());
			} catch (NotRegisteredException ignored) {
			}
			return StringMgmt.remUnderscore(nation);
		case "town_balance": // %townyadvanced_town_balance%
			try {
				if (TownySettings.isUsingEconomy())
					balance = resident.getTown().getAccount().getHoldingFormattedBalance();
			} catch (NotRegisteredException ignored) {
			}
			return balance;
		case "nation_balance": // %townyadvanced_nation_balance%
			try {
				if (TownySettings.isUsingEconomy())
					balance = resident.getTown().getNation().getAccount().getHoldingFormattedBalance();
			} catch (NotRegisteredException ignored) {
			}
			return balance;
		case "town_tag": // %townyadvanced_town_tag%
			try {
				tag = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getTag());
			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "town_tag_override": // %townyadvanced_town_tag_override%
			try {
				if (resident.getTown().hasTag())
					tag = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getTag());
				else
					tag = StringMgmt.remUnderscore(String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getName()));
			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "nation_tag": // %townyadvanced_nation_tag%
			try {
				tag = String.format(TownySettings.getPAPIFormattingNation(), resident.getTown().getNation().getTag());
			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "nation_tag_override": // %townyadvanced_nation_tag_override%
			try {
				if (resident.getTown().getNation().hasTag())
					tag = String.format(TownySettings.getPAPIFormattingNation(),
							resident.getTown().getNation().getTag());
				else
					tag = StringMgmt.remUnderscore(String.format(TownySettings.getPAPIFormattingNation(),
							resident.getTown().getNation().getName()));
			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "towny_tag": // %townyadvanced_towny_tag%
			try {
				if (resident.hasTown()) {
					if (resident.getTown().hasTag())
						town = resident.getTown().getTag();
					if (resident.getTown().hasNation())
						if (resident.getTown().getNation().hasTag())
							nation = resident.getTown().getNation().getTag();
				}
				if (!nation.isEmpty())
					tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
				else if (!town.isEmpty())
					tag = String.format(TownySettings.getPAPIFormattingTown(), town);

			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "towny_formatted": // %townyadvanced_towny_formatted%
			try {
				if (resident.hasTown()) {
					town = resident.getTown().getFormattedName();
					if (resident.getTown().hasNation())
						nation = resident.getTown().getNation().getFormattedName();
				}
				if (!nation.isEmpty())
					tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
				else if (!town.isEmpty())
					tag = String.format(TownySettings.getPAPIFormattingTown(), town);

			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "towny_tag_formatted": // %townyadvanced_towny_tag_formatted%
			try {
				if (resident.hasTown()) {
					if (resident.getTown().hasTag())
						town = resident.getTown().getTag();
					else
						town = resident.getTown().getFormattedName();
					if (resident.getTown().hasNation()) {
						if (resident.getTown().getNation().hasTag())
							nation = resident.getTown().getNation().getTag();
						else
							nation = resident.getTown().getNation().getFormattedName();
					}
				}
				if (!nation.isEmpty())
					tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
				else if (!town.isEmpty())
					tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "towny_tag_override": // %townyadvanced_towny_tag_override%
			try {
				if (resident.hasTown()) {
					if (resident.getTown().hasTag())
						town = resident.getTown().getTag();
					else
						town = StringMgmt.remUnderscore(resident.getTown().getName());
					if (resident.getTown().hasNation()) {
						if (resident.getTown().getNation().hasTag())
							nation = resident.getTown().getNation().getTag();
						else
							StringMgmt.remUnderscore(nation = resident.getTown().getNation().getName());
					}
				}
				if (!nation.isEmpty())
					tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
				else if (!town.isEmpty())
					tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "title": // %townyadvanced_title%
			if (resident.hasTitle())
				title = resident.getTitle();
			return title;
		case "surname": // %townyadvanced_surname%
			if (resident.hasSurname())
				title = resident.getSurname();
			return title;
		case "towny_name_prefix": // %townyadvanced_towny_name_prefix%
			if (resident.isMayor())
				title = TownySettings.getMayorPrefix(resident);
			if (resident.isKing())
				title = TownySettings.getKingPrefix(resident);
			return title;
		case "towny_name_postfix": // %townyadvanced_towny_name_postfix%
			if (resident.isMayor())
				title = TownySettings.getMayorPostfix(resident);
			if (resident.isKing())
				title = TownySettings.getKingPostfix(resident);
			return title;
		case "towny_prefix": // %townyadvanced_towny_prefix%
			if (resident.hasTitle())
				title = resident.getTitle() + " ";
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPrefix(resident);
				if (resident.isKing())
					title = TownySettings.getKingPrefix(resident);
			}
			return title;
		case "towny_postfix": // %townyadvanced_towny_postfix%
			if (resident.hasSurname())
				title = " " + resident.getSurname();
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPostfix(resident);
				if (resident.isKing())
					title = TownySettings.getKingPostfix(resident);
			}
			return title;
		case "towny_colour": // %townyadvanced_towny_colour%
			String colour = "";
			if (!resident.hasTown())
				colour = TownySettings.getPAPIFormattingNomad();
			else {
				colour = TownySettings.getPAPIFormattingResident();
				if (resident.isMayor())
					colour = TownySettings.getPAPIFormattingMayor();
				if (resident.isKing())
					colour = TownySettings.getPAPIFormattingKing();
			}
			return colour;
		case "town_residents_amount": // %townyadvanced_town_residents_amount%
			if (resident.hasTown()) {
				try {
					amount = String.valueOf(resident.getTown().getNumResidents());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "town_residents_online": // %townyadvanced_town_residents_online%
			if (resident.hasTown()) {
				try {
					amount = String.valueOf(TownyAPI.getInstance().getOnlinePlayers(resident.getTown()).size());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "town_townblocks_used": // %townyadvanced_town_townblocks_used%
			if (resident.hasTown()) {
				try {
					amount = String.valueOf(resident.getTown().getTownBlocks().size());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "town_townblocks_bought": // %townyadvanced_town_townblocks_bought%
			if (resident.hasTown()) {
				try {
					amount = String.valueOf(resident.getTown().getPurchasedBlocks());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "town_townblocks_bonus": // %townyadvanced_town_townblocks_bonus%
			if (resident.hasTown()) {
				try {
					amount = String.valueOf(resident.getTown().getBonusBlocks());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "town_townblocks_maximum": // %townyadvanced_town_townblocks_maximum%
			if (resident.hasTown()) {
				try {
					amount = String.valueOf(TownySettings.getMaxTownBlocks(resident.getTown()));
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "town_townblocks_natural_maximum": // %townyadvanced_town_townblocks_natural_maximum%
			if (resident.hasTown()) {
				try {
					amount = String.valueOf(TownySettings.getMaxTownBlocks(resident.getTown()) - resident.getTown().getBonusBlocks() - resident.getTown().getPurchasedBlocks());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "town_mayor": // %townyadvanced_town_mayor%
			if (resident.hasTown()) {
				try {
					name = resident.getTown().getMayor().getName();
				} catch (NotRegisteredException ignored) {
				}
			}
			return name;
		case "nation_king": // %townyadvanced_nation_king%
			if (resident.hasTown()) {
				try {
					if (resident.getTown().hasNation())
						name = resident.getTown().getNation().getKing().getName();
				} catch (NotRegisteredException ignored) {
				}
			}
			return name;
		case "resident_friends_amount": // %townyadvanced_resident_friends_amount%
			amount = String.valueOf(resident.getFriends().size());
			return amount;
		case "nation_residents_amount": // %townyadvanced_nation_residents_amount%
			if (resident.hasTown()) {
				try {
					if (resident.getTown().hasNation())
						amount = String.valueOf(resident.getTown().getNation().getNumResidents());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "nation_residents_online": // %townyadvanced_nation_residents_online%
			if (resident.hasTown()) {
				try {
					if (resident.getTown().hasNation())
						amount = String.valueOf(
								TownyAPI.getInstance().getOnlinePlayers(resident.getTown().getNation()).size());
				} catch (NotRegisteredException ignored) {
				}
			}
			return amount;
		case "nation_capital": // %townyadvanced_nation_capital%
			if (resident.hasTown()) {
				try {
					if (resident.getTown().hasNation())
						name = StringMgmt.remUnderscore(resident.getTown().getNation().getCapital().getName());
				} catch (NotRegisteredException ignored) {
				}
			}
			return name;
		case "daily_town_upkeep": // %townyadvanced_daily_town_upkeep%
			if (resident.hasTown()) {
				try {
					cost = TownySettings.getTownUpkeepCost(resident.getTown());
				} catch (NotRegisteredException ignored) {
				}
			}
			return String.valueOf(cost);
		case "daily_nation_upkeep": // %townyadvanced_daily_nation_upkeep%
			if (resident.hasTown()) {
				try {
					if (resident.getTown().hasNation())
						cost = TownySettings.getNationUpkeepCost(resident.getTown().getNation());
				} catch (NotRegisteredException ignored) {
				}
			}
			return String.valueOf(cost);
		case "has_town": // %townyadvanced_has_town%
			return String.valueOf(resident.hasTown());
		case "has_nation": // %townyadvanced_has_nation%
			return String.valueOf(resident.hasNation());
		case "nation_tag_town_formatted": // %townyadvanced_nation_tag_town_formatted%
			try {
				if (resident.hasTown()) {
					town = resident.getTown().getFormattedName();
					if (resident.getTown().hasNation() && resident.getTown().getNation().hasTag())
						nation = resident.getTown().getNation().getTag();
				}
				if (!nation.isEmpty())
					tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
				else if (!town.isEmpty())
					tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			} catch (NotRegisteredException ignored) {
			}
			return tag;
		case "town_ranks": // %townyadvanced_town_ranks%
			if (resident.isMayor())
				rank = Translation.of("mayor_sing");
			else if (!resident.getTownRanks().isEmpty())
				rank = StringMgmt.capitalize(StringMgmt.join(resident.getTownRanks(), ", "));
			return rank;
			
		case "nation_ranks": // %townyadvanced_nation_ranks%
			if (resident.isKing())
				rank = Translation.of("king_sing");
			else if (!resident.getNationRanks().isEmpty())
				rank = StringMgmt.capitalize(StringMgmt.join(resident.getNationRanks(), ", "));
			return rank;
		case "player_status": // %townyadvanced_player_status%
			if (!resident.hasTown())
				tag = nomad;
			else {
				if (resident.isKing())
					tag = king;
				else if (resident.isMayor())
					tag = mayor;
				else
					tag = res;
			}
			return tag;
		case "town_prefix": // %townyadvanced_town_prefix%
			try {
				return resident.hasTown() ? TownySettings.getTownPrefix(resident.getTown()) : "";
			} catch (NotRegisteredException ignored) {
			}
		case "town_postfix": // %townyadvanced_town_postfix%
			try {
				return resident.hasTown() ? TownySettings.getTownPostfix(resident.getTown()) : "";
			} catch (NotRegisteredException ignored) {
			}
		case "nation_prefix": // %townyadvanced_nation_prefix%
			try {
				return resident.hasNation() ? TownySettings.getNationPrefix(resident.getTown().getNation()) : "";
			} catch (NotRegisteredException ignored) {
			}
		case "nation_postfix": // %townyadvanced_nation_postfix%
			try {
				return resident.hasNation() ? TownySettings.getNationPostfix(resident.getTown().getNation()) : "";
			} catch (NotRegisteredException ignored) {
			}
		case "player_jailed": // %townyadvanced_player_jailed%
			return String.valueOf(resident.isJailed());
		case "player_plot_type": // %townyadvanced_player_plot_type%
			return townblock != null ? townblock.getType().toString() : "";
		case "player_plot_owner": // %townyadvanced_player_plot_owner%
			return townblock != null ? String.valueOf(townblock.isOwner(resident)) : "false";
		default:
			return null;
		}
	}
}