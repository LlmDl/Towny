package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyPreTransactionEvent extends Event implements Cancellable {
	private final Transaction transaction;
	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;
	private final String cancelMessage = "Sorry this event was cancelled.";

	public TownyPreTransactionEvent(Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.transaction = transaction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public Transaction getTransaction() {
		return transaction;
	}
	
	public int getNewBalance() {
		switch (transaction.getType()) {
			case ADD:
				return (int) (TownyEconomyHandler.getBalance(transaction.getUUID(),
					Bukkit.getPlayer(transaction.getUUID()).getWorld())
					+ transaction.getAmount());
			case SUBTRACT:
				return (int) (TownyEconomyHandler.getBalance(transaction.getUUID(),
					Bukkit.getPlayer(transaction.getUUID()).getWorld())
					- transaction.getAmount());
			default:
				break;
		}
		
		return 0;
	}
}
