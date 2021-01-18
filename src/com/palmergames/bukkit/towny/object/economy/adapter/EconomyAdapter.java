package com.palmergames.bukkit.towny.object.economy.adapter;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.palmergames.bukkit.towny.object.Government;

/**
 * An adapter that is used to adapt multiple
 * economy implementations.
 */
public interface EconomyAdapter {

	/**
	 * Get's the proper formatting for a given balance.
	 * 
	 * @param balance The balance to format.
	 * @return A string with the balance formatted.
	 */
	String getFormattedBalance(double balance);
	
	/**
	 * Attempts to add money to an account.
	 * 
	 * @param uuid The uuid of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean add(UUID uuid, double amount, World world);
	
	/**
	 * Attempts to subtract money from an account.
	 *
	 * @param uuid The uuid of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean subtract(UUID uuid, double amount, World world);

	/**
	 * Checks whether the given account exists.
	 * 
	 * @param uuid The uuid of the account.
	 * @return A boolean indicating success.
	 */
	boolean hasAccount(UUID uuid);

	/**
	 * Gets the balance of the account.
	 * 
	 * @param uuid The uuid of the account.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	double getBalance(UUID uuid, World world);
	
	/**
	 * Attempts to create an account.
	 * 
	 * @param uuid The uuid of the account.
	 */
	void newAccount(UUID uuid);
	
	/**
	 * Removes an account.
	 * 
	 * @param uuid The uuid of the account.
	 */
	void deleteAccount(UUID uuid);
	
	/**
	 * Sets the balance of the account.
	 * 
	 * @param uuid The uuid of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean setBalance(UUID uuid, double amount, World world);
	
	/**
	 * Attempts to add money to an account.
	 * 
	 * @param government The government which owns the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean add(Government government, double amount, World world);
	
	/**
	 * Attempts to subtract money from an account.
	 *
	 * @param government The government which owns the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean subtract(Government government, double amount, World world);

	/**
	 * Checks whether the given account exists.
	 * 
	 * @param government The government which owns the account.
	 * @return A boolean indicating success.
	 */
	boolean hasAccount(Government government);

	/**
	 * Gets the balance of the account.
	 * 
	 * @param government The government which owns the account.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	double getBalance(Government government, World world);
	
	/**
	 * Attempts to create an account.
	 * 
	 * @param government The government which owns the account.
	 */
	void newAccount(Government government);
	
	/**
	 * Removes an account.
	 * 
	 * @param government The government which owns the account.
	 */
	void deleteAccount(Government government);
	
	/**
	 * Sets the balance of the account.
	 * 
	 * @param government The government which owns the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean setBalance(Government government, double amount, World world);
	
	/**
	 * Attempts to add money to an account.
	 * 
	 * @param accountName The name of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean add(String accountName, double amount, World world);

	/**
	 * Attempts to subtract money from an account.
	 *
	 * @param accountName The name of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean subtract(String accountName, double amount, World world);

	/**
	 * Checks whether the given account exists.
	 * 
	 * @param accountName The name of the account.
	 * @return A boolean indicating success.
	 */
	boolean hasAccount(String accountName);

	/**
	 * Gets the balance of the account.
	 * 
	 * @param accountName The name of the account.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	double getBalance(String accountName, World world);

	/**
	 * Attempts to create an account.
	 * 
	 * @param accountName The name of the new account.
	 */
	void newAccount(String accountName);

	/**
	 * Removes an account.
	 * 
	 * @param accountName The name of the account to remove.
	 */
	void deleteAccount(String accountName);

	/**
	 * Sets the balance of the account.
	 * 
	 * @param accountName The name of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean setBalance(String accountName, double amount, World world);

	void setBalance(OfflinePlayer offlinePlayer, double balance, World world);
}
