package com.palmergames.bukkit.towny.object.gui;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyInventory;
import org.bukkit.inventory.Inventory;

public class PermissionGUI extends TownyInventory {
	private final TownBlock townBlock;
	private final boolean canEdit;
	
	public PermissionGUI(Resident res, Inventory inv, String name, TownBlock townBlock, boolean canEdit) {
		super(res, inv, name);
		this.townBlock = townBlock;
		this.canEdit = canEdit;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public boolean canEdit() {
		return canEdit;
	}
}
