package com.palmergames.bukkit.towny.war.eventwar;

import java.util.UUID;

import com.google.common.base.Charsets;
import com.palmergames.bukkit.towny.object.EconomyAccount;

public class WarSpoils extends EconomyAccount {

	public WarSpoils() {
		super(UUID.nameUUIDFromBytes(("towny-war-chest").getBytes(Charsets.UTF_8)));
	}
}