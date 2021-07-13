package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.gui.EditGUI;
import com.palmergames.bukkit.towny.object.gui.PermissionGUI;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyInventory;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.inventory.meta.ItemMeta;

public class TownyInventoryListener implements Listener {
	
	Sound clickSound = Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.stone_button.click_on"), Sound.Source.PLAYER, 1.0f, 1.0f);

	public TownyInventoryListener() {

	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if (!(event.getInventory().getHolder() instanceof TownyInventory) || event.getCurrentItem() == null)
			return;

		event.setCancelled(true);

		Player player = (Player) event.getWhoClicked();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

		if (resident == null)
			return;

		if (event.getInventory().getHolder() instanceof EditGUI) {
			
			ItemMeta meta;
			switch (event.getCurrentItem().getType()) {
				case LIME_WOOL:
					meta = event.getCurrentItem().getItemMeta();
					if (meta.getDisplayName().equals(Colors.LightGreen + ChatColor.BOLD + "Save")) {
						((EditGUI) event.getInventory().getHolder()).saveChanges();
					} else {
						meta.setDisplayName(Colors.Red + Colors.strip(meta.getDisplayName()));
						event.getCurrentItem().setType(Material.RED_WOOL);
					}
					break;
				case RED_WOOL:
					meta = event.getCurrentItem().getItemMeta();
					if (meta.getDisplayName().equals(Colors.Red + ChatColor.BOLD + "Back")) {
						((EditGUI) event.getInventory().getHolder()).exitScreen();
					} else if (meta.getDisplayName().equals(Colors.Red + ChatColor.BOLD + "Delete")) {
						((EditGUI) event.getInventory().getHolder()).deleteResident();
					} else {
						meta.setDisplayName(Colors.Gray + Colors.strip(meta.getDisplayName()));
						event.getCurrentItem().setType(Material.GRAY_WOOL);
					}
					break;
				case GRAY_WOOL:
					meta = event.getCurrentItem().getItemMeta();
					meta.setDisplayName(Colors.LightGreen + Colors.strip(meta.getDisplayName()));
					event.getCurrentItem().setType(Material.LIME_WOOL);
					break;
				default:
					return;
			}
			
			event.getCurrentItem().setItemMeta(meta);			
			Towny.getAdventure().player(player).playSound(clickSound);

		} else if (event.getInventory().getHolder() instanceof PermissionGUI) {
			PermissionGUI permissionGUI = (PermissionGUI) event.getInventory().getHolder();
			if (event.getCurrentItem().getType() == Material.PLAYER_HEAD && permissionGUI.canEdit()) {
				PermissionGUIUtil.openPermissionEditorGUI(resident, permissionGUI.getTownBlock(), event.getCurrentItem());
				Towny.getAdventure().player(player).playSound(clickSound);
			} else {
				int currentPage = resident.getGUIPageNum();

				try {
					// If the pressed item was a nextpage button
					if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Next")) {
						if (resident.getGUIPageNum() <= resident.getGUIPages().size() - 1) {
							// Next page exists, flip the page
							resident.setGUIPageNum(++currentPage);
							new PermissionGUI(resident, resident.getGUIPage(), event.getView().getTitle(), permissionGUI.getTownBlock(), permissionGUI.canEdit());
							Towny.getAdventure().player(player).playSound(clickSound);
						}
						// if the pressed item was a previous page button
					} else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Back")) {
						// If the page number is more than 0 (So a previous page exists)
						if (resident.getGUIPageNum() > 0) {
							// Flip to previous page
							resident.setGUIPageNum(--currentPage);
							new PermissionGUI(resident, resident.getGUIPage(), event.getView().getTitle(), permissionGUI.getTownBlock(), permissionGUI.canEdit());
							Towny.getAdventure().player(player).playSound(clickSound);
						}
					}
				} catch (Exception ignored) {}
			}
		} else {
			/*
			 * Not a PermissionGUI or EditGUI, use normal pagination
			 */
			int currentPage = resident.getGUIPageNum();

			try {
				// If the pressed item was a nextpage button
				if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Next")) {
					// If there is no next page, don't do anything
					if (resident.getGUIPageNum() >= resident.getGUIPages().size() - 1) {
						return;
					} else {
						// Next page exists, flip the page
						resident.setGUIPageNum(++currentPage);
						new TownyInventory(resident, resident.getGUIPage(), event.getView().getTitle());
						Towny.getAdventure().player(player).playSound(clickSound);
					}
					// if the pressed item was a previous page button
				} else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Back")) {
					// If the page number is more than 0 (So a previous page exists)
					if (resident.getGUIPageNum() > 0) {
						// Flip to previous page
						resident.setGUIPageNum(--currentPage);
						new TownyInventory(resident, resident.getGUIPage(), event.getView().getTitle());
						Towny.getAdventure().player(player).playSound(clickSound);
					}
				}
			} catch (Exception ignored) {}
		}
	}
}