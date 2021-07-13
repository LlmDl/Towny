package com.palmergames.bukkit.towny.object.gui;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil.SetPermissionType;

import org.bukkit.inventory.Inventory;

public class EditGUI extends PermissionGUI {
	private final Resident editor;
	private final Resident selectedResident;

	public EditGUI(Resident res, Inventory inv, String name, TownBlock townBlock, Resident selectedResident) {
		super(res, inv, name, townBlock, true);
		this.editor = res;
		this.selectedResident = selectedResident;
	}

	/**
	 * Saves updated permissions 
	 */
	public void saveChanges() {
		SetPermissionType[] newTypes = new SetPermissionType[4];
		for (int i = 0; i < 4; i++) {
			switch (getInventory().getItem(PermissionGUIUtil.getWoolSlots()[i]).getType()) {
				case LIME_WOOL:
					newTypes[i] = SetPermissionType.SET;
					break;
				case RED_WOOL:
					newTypes[i] = SetPermissionType.NEGATED;
					break;
				default:
					newTypes[i] = SetPermissionType.UNSET;
			}
		}

		getTownBlock().getPermissionOverrides().put(selectedResident, new PermissionData(newTypes, editor.getName()));
		getTownBlock().save();

		Towny.getPlugin().deleteCache(selectedResident.getName());

		exitScreen();
	}

	public void exitScreen() {
		PermissionGUIUtil.openPermissionGUI(editor, super.getTownBlock());
	}
	
	public void deleteResident() {
		getTownBlock().getPermissionOverrides().remove(selectedResident);
		getTownBlock().save();
		
		Towny.getPlugin().deleteCache(selectedResident.getName());

		exitScreen();
	}
}