package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.gui.EditGUI;
import com.palmergames.bukkit.towny.object.gui.PermissionGUI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class PermissionGUIUtil {
	private static final String GUI_NAME = Translation.of("permission_gui_header");
	private static final SetPermissionType[] defaultTypes = new SetPermissionType[]{SetPermissionType.UNSET, SetPermissionType.UNSET, SetPermissionType.UNSET, SetPermissionType.UNSET};
	private static final int[] woolSlots = new int[]{21, 23, 30, 32};
	
	public enum SetPermissionType {
		UNSET(Colors.Gray, Material.GRAY_WOOL),
		SET(Colors.Green, Material.LIME_WOOL),
		NEGATED(Colors.Red, Material.RED_WOOL);
		
		private String color;
		private Material woolColour;
		
		SetPermissionType(String color, Material woolColour) {
			this.color = color;
			this.woolColour = woolColour;
		}

		public String getColor() {
			return color;
		}

		public Material getWoolColour() {
			return woolColour;
		}
	}
	
	public static void openPermissionGUI(@NotNull Resident resident, @NotNull TownBlock townBlock) {
		boolean canEdit = true;
		try {
			PlotCommand.plotTestOwner(resident, townBlock);
		} catch (TownyException e) {
			canEdit = false;
		}
		
		Inventory page = ResidentUtil.getBlankPage(GUI_NAME);
		ArrayList<Inventory> pages = new ArrayList<>();
		ArrayList<ItemStack> playerSkulls = new ArrayList<>();

		for (Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet()) {
			ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) skull.getItemMeta();
			
			if (!entry.getKey().hasUUID())
				//noinspection deprecation
				meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey().getName())); 
			else
				meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey().getUUID()));
			
			meta.setDisplayName(Colors.Gold + entry.getKey().getName());

			List<String> lore = new ArrayList<>();
			lore.add(entry.getValue().getPermissionTypes()[ActionType.BUILD.getIndex()].getColor() + "Build" + Colors.Gray + "  | " + entry.getValue().getPermissionTypes()[ActionType.DESTROY.getIndex()].getColor() + "Destroy");
			lore.add(entry.getValue().getPermissionTypes()[ActionType.SWITCH.getIndex()].getColor() + "Switch" + Colors.Gray + " | " + entry.getValue().getPermissionTypes()[ActionType.ITEM_USE.getIndex()].getColor() + "Item");

			if (canEdit) {
				if (entry.getValue().getLastChangedAt() > 0 && !entry.getValue().getLastChangedBy().equals(""))
					lore.add(Translation.of("msg_last_edited", TownyFormatter.lastOnlineFormat.format(entry.getValue().getLastChangedAt()), entry.getValue().getLastChangedBy()));
					
				lore.add(Translation.of("msg_click_to_edit"));
			}

			meta.setLore(lore);
			skull.setItemMeta(meta);
			playerSkulls.add(skull);
			
			if (page.firstEmpty() == 46) {
				pages.add(page);
				page = ResidentUtil.getBlankPage(GUI_NAME);
			}

			page.addItem(skull);
		}
		
		pages.add(page);
		resident.setGUIPages(pages);
		resident.setGUIPageNum(0);
		new PermissionGUI(resident, pages.get(0), GUI_NAME, townBlock, canEdit);
	}
	
	public static void openPermissionEditorGUI(@NotNull Resident resident, @NotNull TownBlock townBlock, @NotNull ItemStack clickedItem) {
		Inventory inventory = Bukkit.createInventory(null, 54, GUI_NAME);
		
		SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
		Resident skullOwner = TownyAPI.getInstance().getResident(Colors.strip(meta.getDisplayName()));
		
		clickedItem.setItemMeta(meta);
		inventory.setItem(4, clickedItem);
		
		SetPermissionType[] setPermissionTypes = townBlock.getPermissionOverrides().get(skullOwner).getPermissionTypes();
		for (ActionType actionType : ActionType.values()) {
			ItemStack wool = new ItemStack(setPermissionTypes[actionType.getIndex()].getWoolColour());
			ItemMeta woolMeta = wool.getItemMeta();
			woolMeta.setDisplayName(setPermissionTypes[actionType.getIndex()].getColor() + ChatColor.BOLD + actionType.getCommonName());
			
			wool.setItemMeta(woolMeta);
			inventory.setItem(woolSlots[actionType.getIndex()], wool);
		}
		
		ItemStack saveButton = new ItemStack(Material.LIME_WOOL);
		ItemMeta saveButtonMeta = saveButton.getItemMeta();
		saveButtonMeta.setDisplayName(Colors.LightGreen + ChatColor.BOLD + "Save");
		saveButton.setItemMeta(saveButtonMeta);
		
		ItemStack backButton = new ItemStack(Material.RED_WOOL);
		ItemMeta backButtonMeta = saveButton.getItemMeta();
		backButtonMeta.setDisplayName(Colors.Red + ChatColor.BOLD + "Back");
		backButton.setItemMeta(backButtonMeta);
		
		ItemStack deleteButton = new ItemStack(Material.RED_WOOL);
		backButtonMeta.setDisplayName(Colors.Red + ChatColor.BOLD + "Delete");
		deleteButton.setItemMeta(backButtonMeta);
		
		inventory.setItem(48, saveButton);
		inventory.setItem(50, backButton);
		inventory.setItem(53, deleteButton);
		
		new EditGUI(resident, inventory, GUI_NAME, townBlock, skullOwner);
	}

	public static SetPermissionType[] getDefaultTypes() {
		return defaultTypes;
	}

	public static int[] getWoolSlots() {
		return woolSlots;
	}
}
