package com.palmergames.bukkit.towny.object.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.palmergames.bukkit.towny.TownyEconomyHandler;

/**
 * For internal use only.
 */
public class WarSpoilsAccount extends Account {
	public WarSpoilsAccount() {
		super(TownyEconomyHandler.getUUIDWarChestAccount());
	}

	@Override
	protected boolean addMoney(double amount) {
		return TownyEconomyHandler.add(uuid, amount, world);
	}

	@Override
	protected boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(uuid, amount, world);
	}

	@Override
	public String getName() {
		return "towny-war-chest";
	}
	
	@SuppressWarnings("deprecation")
	public static OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(TownyEconomyHandler.getUUIDWarChestAccount().toString());
	}
}
