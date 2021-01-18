package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.Nameable;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Used to facilitate transactions regarding money, 
 * and the storage of funds.
 * 
 * @author Suneet Tipirneni (Siris)
 * @see BankAccount
 * @see EconomyAccount
 */
public abstract class Account implements Nameable {
	private static final AccountObserver GLOBAL_OBSERVER = new GlobalAccountObserver();
	private final List<AccountObserver> observers = new ArrayList<>();
	private AccountAuditor auditor;
	
	protected UUID uuid;
	World world;
	
	public Account(UUID uuid) {
		this.uuid = uuid;
		observers.add(GLOBAL_OBSERVER);
	}
	
	public Account(UUID uuid, World world) {
		this.uuid = uuid;
		this.world = world;
		
		// ALL account transactions will route auditing data through this
		// central auditor.
		observers.add(GLOBAL_OBSERVER);
	}
	
	// Template methods
	protected abstract boolean addMoney(double amount);
	protected abstract boolean subtractMoney(double amount);

	/**
	 * Attempts to add money to the account, 
	 * and notifies account observers of any changes.
	 * 
	 * @param amount The amount to add.
	 * @param reason The reason for adding.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	public boolean deposit(double amount, String reason) throws EconomyException {
		if (addMoney(amount)) {
			notifyObserversDeposit(this, amount, reason);
			if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED))
				return payFromServer(amount, reason);

			return true;
		}
		
		return false;
	}

	/**
	 * Attempts to withdraw money from the account, 
	 * and notifies account observers of any changes.
	 *
	 * @param amount The amount to withdraw.
	 * @param reason The reason for subtracting.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	public boolean withdraw(double amount, String reason) throws EconomyException {
		if (subtractMoney(amount)) {
			notifyObserversWithdraw(this, amount, reason);
			if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED))
				return payToServer(amount, reason);

			return true;
		}
		
		return false;
	}

	/**
	 * Pays another account the specified funds.
	 *
	 * @param amount The amount to pay.
	 * @param collector The account to pay.
	 * @param reason The reason for the pay. 
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	public boolean payTo(double amount, EconomyHandler collector, String reason) throws EconomyException {
		return payTo(amount, collector.getAccount(), reason);
	}

	/**
	 * Pays another account the specified funds.
	 *
	 * @param amount The amount to pay.
	 * @param collector The account to pay.
	 * @param reason The reason for the pay.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	public boolean payTo(double amount, Account collector, String reason) throws EconomyException {
		
		if (amount > getHoldingBalance()) {
			return false;
		}

		return withdraw(amount, reason) && collector.deposit(amount, reason);
	}
	
	protected boolean payToServer(double amount, String reason) throws EconomyException {

		// Put it back into the server.
		return TownyEconomyHandler.addToServer(amount, world);
	}
	
	protected boolean payFromServer(double amount, String reason) throws EconomyException {
		
		// Remove it from the server economy.
		return TownyEconomyHandler.subtractFromServer(amount, world);
	}

	/**
	 * Set balance and log this action
	 *
	 * @param amount currency to transact
	 * @param reason memo regarding transaction
	 * @return true, or pay/collect balance for given reason
	 * @throws EconomyException if transaction fails
	 */
	public boolean setBalance(double amount, String reason) throws EconomyException {
		double balance = getHoldingBalance();
		double diff = amount - balance;
		if (diff > 0) {
			// Adding to
			return deposit(diff, reason);
		} else if (balance > amount) {
			// Subtracting from
			diff = -diff;
			return withdraw(diff, reason);
		} else {
			// Same amount, do nothing.
			return true;
		}
	}

	/**
	 * Gets the current balance of this account.
	 * 
	 * @return The amount in this account.
	 * @throws EconomyException On an economy error.
	 */
	public double getHoldingBalance() throws EconomyException {
		try {
			return TownyEconomyHandler.getBalance(uuid, world);
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error getting holdings for " + uuid);
		}
	}

	/**
	 * Does this object have enough in it's economy account to pay?
	 *
	 * @param amount currency to check for
	 * @return true if there is enough.
	 * @throws EconomyException if failure
	 */
	public boolean canPayFromHoldings(double amount) throws EconomyException {
		return TownyEconomyHandler.hasEnough(uuid, amount, world);
	}

	/**
	 * Used To Get Balance of Players holdings in String format for printing
	 *
	 * @return current account balance formatted in a string.
	 */
	public String getHoldingFormattedBalance() {
		try {
			return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
		} catch (EconomyException e) {
			return "Error Accessing Bank Account";
		}
	}

	/**
	 * Attempt to delete the economy account.
	 */
	public void removeAccount() {
		TownyEconomyHandler.removeAccount(uuid);
	}

	/*
	 * Auditors and Observers
	 */
	
	/**
	 * Gets the observers of this account.
	 * 
	 * @return A list of account observers.
	 */
	public List<AccountObserver> getObservers() {
		return Collections.unmodifiableList(observers);
	}
	
	private void notifyObserversDeposit(Account account, double amount, String reason) {
		for (AccountObserver observer : getObservers()) {
			observer.deposited(account, amount, reason);
		}
	}

	private void notifyObserversWithdraw(Account account, double amount, String reason) {
		for (AccountObserver observer : getObservers()) {
			observer.withdrew(account, amount, reason);
		}
	}

	/**
	 * Adds an account observer that listens to account changes.
	 * 
	 * @param observer The observer to add.
	 */
	public final void addObserver(AccountObserver observer) {
		observers.add(observer);
	}

	/**
	 * Removes an account observer that listens to account changes.
	 *
	 * @param observer The observer to remove.
	 */
	public final void removeObserver(AccountObserver observer) {
		observers.remove(observer);
	}

	/**
	 * Gets the auditor that audits this account.
	 * 
	 * @return The auditor tracking this account.
	 */
	public final AccountAuditor getAuditor() {
		return auditor;
	}

	/**
	 * Sets the auditor that audits this account, and
	 * adds it as an observer.
	 *
	 * @param auditor The auditor to track this account.
	 */
	public final void setAuditor(AccountAuditor auditor) {
		this.auditor = auditor;
		
		// Add the auditor to the observer list.
		addObserver(auditor);
	}
	
	/*
	 *  Legacy Compatibility Methods.
	 */
	
	/**
	 * @deprecated As of 0.96.1.11, use {@link #deposit(double, String)} instead.
	 * 
	 * @param amount The amount to add.
	 * @param reason The reason for adding.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	@Deprecated
	public boolean collect(double amount, String reason) throws EconomyException {
		return deposit(amount, reason);
	}
	
	/**
	 * @deprecated As of 0.96.1.11, use {@link #withdraw(double, String)} instead.
	 *
	 * @param amount The amount to subtract.
	 * @param reason The reason for subcracting.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	@Deprecated
	public boolean pay(double amount, String reason) throws EconomyException {
		return withdraw(amount, reason);
	}
}
