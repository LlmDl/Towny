package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;

import java.util.UUID;

import org.bukkit.World;

/**
 * Economy object which provides an interface with the Economy Handler.
 *
 * @author ElgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 */
public class EconomyAccount extends Account {
	public static final TownyServerAccount SERVER_ACCOUNT = new TownyServerAccount();
	private World world;
	
	protected EconomyAccount(UUID uuid, World world) {
		super(uuid);
		this.world = world;
	}

	@Override
	protected boolean addMoney(double amount) {
		return TownyEconomyHandler.add(uuid, amount, world);
	}

	@Override
	protected boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(uuid, amount, world);
	}

	protected EconomyAccount(UUID uuid) {
		super(uuid);
	}

	public World getWorld() {
		return world;
	}

	@Override
	public String getName() {
		return null;
	}
}
