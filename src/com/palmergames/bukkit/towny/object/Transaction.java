package com.palmergames.bukkit.towny.object;

import java.util.UUID;

public class Transaction {
	private final TransactionType type;
	private final UUID uuid;
	private final double amount;
	
	public Transaction(TransactionType type, UUID uuid, double amount) {
		this.type = type;
		this.uuid = uuid;
		this.amount = amount;
	}

	public TransactionType getType() {
		return type;
	}

	public UUID getUUID() {
		return uuid;
	}
	
	public double getAmount() {
		return amount;
	}
}
