package com.palmergames.bukkit.towny.object.economy.adapter;

import net.tnemc.core.economy.EconomyAPI;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.palmergames.bukkit.towny.object.Government;

import java.math.BigDecimal;
import java.util.UUID;

public class ReserveEconomyAdapter implements EconomyAdapter {
	
	final EconomyAPI economy;
	
	public ReserveEconomyAdapter(EconomyAPI economy) {
		this.economy = economy;
	}

	@Override
	public String getFormattedBalance(double balance) {
		BigDecimal bd = BigDecimal.valueOf(balance);
		return economy.format(bd);
	}

	/*
	 * UUID Account manipulation Methods
	 * @see com.palmergames.bukkit.towny.object.economy.Account
	 */

	@Override
	public boolean add(UUID uuid, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.addHoldingsDetail(uuid, bd, world.getName()).success();
	}

	@Override
	public boolean subtract(UUID uuid, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.removeHoldingsDetail(uuid, bd, world.getName()).success();
	}

	@Override
	public boolean hasAccount(UUID uuid) {
		return economy.hasAccountDetail(uuid).success();
	}

	@Override
	public double getBalance(UUID uuid, World world) {
		return economy.getHoldings(uuid, world.getName()).doubleValue();
	}

	@Override
	public void newAccount(UUID uuid) {
		economy.createAccountDetail(uuid).success();
	}

	@Override
	public void deleteAccount(UUID uuid) {
		economy.deleteAccountDetail(uuid);
	}

	@Override
	public boolean setBalance(UUID uuid, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.setHoldingsDetail(uuid, bd, world.getName()).success();
	}

	/*
	 * Government BankAccount manipulation methods.
	 * @see com.palmergames.bukkit.towny.object.economy.BankAccount
	 */

	@Override
	public boolean add(Government government, double amount, World world) {
		return add(government.getUUID(), amount, world);
	}

	@Override
	public boolean subtract(Government government, double amount, World world) {
		return subtract(government.getUUID(), amount, world);
	}

	@Override
	public boolean hasAccount(Government government) {
		return hasAccount(government.getUUID());
	}

	@Override
	public double getBalance(Government government, World world) {
		return getBalance(government.getUUID(), government.getWorld());
	}

	@Override
	public void newAccount(Government government) {
		newAccount(government.getUUID());
	}

	@Override
	public void deleteAccount(Government government) {
		deleteAccount(government.getUUID());
	}

	@Override
	public boolean setBalance(Government government, double amount, World world) {
		return setBalance(government.getUUID(), amount, world);
	}

	@Override
	public void setBalance(OfflinePlayer offlinePlayer, double balance, World world) {
		setBalance(offlinePlayer, balance, world);
	}
	
	/*
	 * Old accountName methods.
	 */

	@Override
	public boolean add(String accountName, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.addHoldingsDetail(accountName, bd, world.getName()).success();
	}

	@Override
	public boolean subtract(String accountName, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.removeHoldingsDetail(accountName, bd, world.getName()).success();
	}

	@Override
	public boolean hasAccount(String accountName) {
		return economy.hasAccountDetail(accountName).success();
	}

	@Override
	public double getBalance(String accountName, World world) {
		return economy.getHoldings(accountName, world.getName()).doubleValue();
	}

	@Override
	public void newAccount(String accountName) {
		economy.createAccountDetail(accountName).success();
	}

	@Override
	public void deleteAccount(String accountName) {
		economy.deleteAccountDetail(accountName);
	}

	@Override
	public boolean setBalance(String accountName, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.setHoldingsDetail(accountName, bd, world.getName()).success();
	}
}
