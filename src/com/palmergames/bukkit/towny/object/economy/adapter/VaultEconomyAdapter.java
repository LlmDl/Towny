package com.palmergames.bukkit.towny.object.economy.adapter;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.palmergames.bukkit.towny.object.Government;

@SuppressWarnings("deprecation")
public class VaultEconomyAdapter implements EconomyAdapter {
	
	private final Economy economy;
	
	public VaultEconomyAdapter(Economy economy) {
		this.economy = economy;
	}

	private OfflinePlayer getOP(UUID uuid) {
		return Bukkit.getOfflinePlayer(uuid);
	}

	@Override
	public String getFormattedBalance(double balance) {
		return economy.format(balance);
	}

	/*
	 * UUID Account manipulation Methods
	 * @see com.palmergames.bukkit.towny.object.economy.Account
	 */
	
	@Override
	public boolean add(UUID uuid, double amount, World world) {
		return economy.depositPlayer(getOP(uuid), world.getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}
	
	@Override
	public boolean subtract(UUID uuid, double amount, World world) {
		return economy.withdrawPlayer(getOP(uuid), world.getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}
	
	@Override
	public boolean hasAccount(UUID uuid) {
		return economy.hasAccount(getOP(uuid));
	}
	
	@Override
	public double getBalance(UUID uuid, World world) {
		return economy.getBalance(getOP(uuid));
	}
	
	@Override
	public void newAccount(UUID uuid) {
		economy.createPlayerAccount(getOP(uuid));
	}
	
	@Override
	public void deleteAccount(UUID uuid) {
		if (!economy.hasAccount(getOP(uuid)))
			return;
		
		economy.withdrawPlayer(getOP(uuid), (economy.getBalance(getOP(uuid))));
	}
	
	@Override
	public boolean setBalance(UUID uuid, double amount, World world) {
		double currentBalance = getBalance(uuid, world);
		double diff = Math.abs(amount - currentBalance);
		
		if (amount > currentBalance) {
			return add(uuid, diff, world);
		} else if (amount < currentBalance) {
			return subtract(uuid, diff, world);
		}
		
		// If we get here, the balances are equal.
		return true;
	}
	
	/*
	 * Government BankAccount manipulation methods.
	 * @see com.palmergames.bukkit.towny.object.economy.BankAccount
	 */

	@Override
	public boolean add(Government gov, double amount, World world) {
		return economy.depositPlayer(gov.getOfflinePlayer(), gov.getWorld().getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean subtract(Government gov, double amount, World world) {
		return economy.withdrawPlayer(gov.getOfflinePlayer(), gov.getWorld().getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean hasAccount(Government gov) {
		return economy.hasAccount(gov.getOfflinePlayer());
	}

	@Override
	public double getBalance(Government gov, World world) {
		return economy.getBalance(gov.getOfflinePlayer(), world.getName());
	}

	@Override
	public void newAccount(Government gov) {
		economy.createPlayerAccount(gov.getOfflinePlayer(), gov.getWorld().getName());
	}

	@Override
	public void deleteAccount(Government gov) {
		if (!economy.hasAccount(gov.getOfflinePlayer()))
			return;
		economy.withdrawPlayer(gov.getOfflinePlayer(), economy.getBalance(gov.getOfflinePlayer()));
	}
	
	public boolean setBalance(Government gov, double amount, World world) {
		double currentBalance = getBalance(gov, world);
		double diff = Math.abs(amount - currentBalance);
		
		if (amount > currentBalance) {
			return add(gov, diff, world);
		} else if (amount < currentBalance) {
			return subtract(gov, diff, world);
		}
		
		// If we get here, the balances are equal.
		return true;
	}
	
	@Override
	public void setBalance(OfflinePlayer offlinePlayer, double amount, World world) {
		double currentBalance = economy.getBalance(offlinePlayer);
	
		if (amount > currentBalance) {
			economy.depositPlayer(offlinePlayer, amount);
		} else if (amount < currentBalance) {
			economy.withdrawPlayer(offlinePlayer, amount);
		}
	}
	
	
	/*
	 * Old accountName methods.
	 */
	
	@Override
	public boolean add(String accountName, double amount, World world) {
		return economy.depositPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean subtract(String accountName, double amount, World world) {
		return economy.withdrawPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean hasAccount(String accountName) {
		return economy.hasAccount(accountName);
	}

	@Override
	public double getBalance(String accountName, World world) {
		return economy.getBalance(accountName);
	}

	@Override
	public void newAccount(String accountName) {
		economy.createPlayerAccount(accountName);
	}

	@Override
	public void deleteAccount(String accountName) {
		// Attempt to zero the account as Vault provides no delete method.
		if (!economy.hasAccount(accountName)) {
			return;
		}
		
		economy.withdrawPlayer(accountName, (economy.getBalance(accountName)));
	}

	@Override
	public boolean setBalance(String accountName, double amount, World world) {
		double currentBalance = getBalance(accountName, world);
		double diff = Math.abs(amount - currentBalance);
		
		if (amount > currentBalance) {
			return add(accountName, diff, world);
		}else if (amount < currentBalance) {
			return subtract(accountName, diff, world);
		}
		
		// If we get here, the balances are equal.
		return true;
	}

}
