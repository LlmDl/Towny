package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;

public class NationPreTransactionEvent extends Event implements Cancellable {
	private Nation nation;
	private static final HandlerList handlers = new HandlerList();
	private Transaction transaction;
	private String cancelMessage = "Sorry this event was cancelled.";
	private boolean isCancelled = false;

	public NationPreTransactionEvent(Nation nation, Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.transaction = transaction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public Nation getNation() {
		return nation;
	}

	public Transaction getTransaction() {
		return transaction;
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

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
