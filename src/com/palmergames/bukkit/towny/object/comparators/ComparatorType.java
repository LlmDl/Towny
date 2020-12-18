package com.palmergames.bukkit.towny.object.comparators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.palmergames.bukkit.towny.object.Government;

public enum ComparatorType {
	RESIDENTS("Number of Residents", GovernmentComparators.BY_NUM_RESIDENTS),
	TOWNBLOCKS("Number of Claimed Townblocks", GovernmentComparators.BY_TOWNBLOCKS_CLAIMED),
	BALANCE("Bank Balance", GovernmentComparators.BY_BANK_BALANCE),
	ONLINE("Online Players", GovernmentComparators.BY_NUM_ONLINE),
	TOWNS("Number of Towns", NationComparators.BY_NUM_TOWNS),
	NAME("Alphabetical Order", GovernmentComparators.BY_NAME),
	OPEN("Open Status", GovernmentComparators.BY_OPEN);

	public static final List<String> TOWN_TYPES = new ArrayList<>(Arrays.asList("RESIDENTS", "TOWNBLOCKS", "BALANCE", "ONLINE", "NAME", "OPEN"));
	public static final List<String> NATION_TYPES = new ArrayList<>(Arrays.asList("RESIDENTS", "TOWNBLOCKS", "BALANCE", "ONLINE", "NAME", "OPEN", "TOWNS"));

	private final String name;
	private final Comparator<? extends Government> comparator;
	ComparatorType(String name, Comparator<? extends Government> comparator) {
		this.name = name;
		this.comparator = comparator;
	}
	public String getName() {
		return name;
	}
	public Comparator<? extends Government> getComparator() {
		return comparator;
	}
	
}
