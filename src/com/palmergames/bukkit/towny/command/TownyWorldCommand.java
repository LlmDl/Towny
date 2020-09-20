package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Send a list of all general townyworld help commands to player Command:
 * /townyworld
 */

public class TownyWorldCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;

	private static TownyWorld Globalworld;
	
	private static final List<String> townyWorldTabCompletes = Arrays.asList(
		"list",
		"toggle",
		"set",
		"regen",
		"undo"
	);

	private static final List<String> townyWorldToggleTabCompletes = Arrays.asList(
		"claimable",
		"usingtowny",
		"pvp",
		"forcepvp",
		"explosion",
		"forceexplosion",
		"fire",
		"townmobs",
		"worldmobs",
		"revertunclaim",
		"revertexpl",
		"warallowed"
	);
	
	private static List<String> townySetTabCompletes = Arrays.asList(
		"usedefault",
		"wildperm",
		"wildignore",
		"wildregen",
		"wildname"
	);
	
	private boolean isConsole = false;

	public TownyWorldCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			if (plugin.isError()) {
				sender.sendMessage(Colors.Rose + "[Towny Error] Locked in Safe mode!");
				return false;
			}
			parseWorldCommand(sender, args);
		} else {
			isConsole = true;			
			parseWorldFromConsole(sender, args);
		}

		Globalworld = null;
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase()) {
			case "toggle":
				if (args.length == 2)
					return NameUtil.filterByStart(townyWorldToggleTabCompletes, args[1]);
				break;
			case "set":
				if (args.length == 2)
					return NameUtil.filterByStart(townySetTabCompletes, args[1]);
				break;
			default:
				if (args.length == 1)
					return filterByStartOrGetTownyStartingWith(townyWorldTabCompletes, args[0], "+w");
		}
		
		return Collections.emptyList();
	}

	private void parseWorldFromConsole(CommandSender sender, String[] split) {

		Player player = null;

		if ((split.length == 0) || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
			return;
		}
		
		if (split[0].equalsIgnoreCase("list")){
			listWorlds(player, sender);
			return;
		}		

		if (split[0].equalsIgnoreCase("set")) {
			HelpMenu.TOWNYWORLD_SET_CONSOLE.send(sender);
		}
		else if (split[0].equalsIgnoreCase("regen") || split[0].equalsIgnoreCase("undo") || split[0].equalsIgnoreCase("toggle")) {
			HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
		} else {
			try {
				Globalworld = TownyUniverse.getInstance().getDataSource().getWorld(split[0].toLowerCase());
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_area_not_recog"));
				return;
			}
			split = StringMgmt.remFirstArg(split);
			parseWorldCommand(sender, split);
		}
	}

	public void parseWorldCommand(CommandSender sender, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Player player = null;

		if (sender instanceof Player) {
			player = (Player) sender;
			try {
				if (Globalworld == null)
					Globalworld = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_area_not_recog"));
				return;
			}
			
		/*
		 * removed in 0.92.0.10
		 */			
//		} else {
//			if (split.length == 0) {
//				sender.sendMessage(String.format(Translation.of("msg_err_invalid_property"), "world"));
//				return;
//			}
//			if ((!split[0].equalsIgnoreCase("?")) && (!split[0].equalsIgnoreCase("list")))
//				try {
//					if ((split.length >= 1)) {
//						Globalworld = TownyUniverse.getDataSource().getWorld(split[split.length - 1].toLowerCase());
//						split = StringMgmt.remLastArg(split);
//					} else {
//						sender.sendMessage(Translation.of("msg_area_not_recog"));
//						return;
//					}
//
//				} catch (NotRegisteredException e) {
//					sender.sendMessage(String.format(Translation.of("msg_err_invalid_property"), "world"));
//					return;
//				}
		}

		if (split.length == 0) {
			if (player == null) {
				for (String line : TownyFormatter.getStatus(Globalworld)) {
					sender.sendMessage(Colors.strip(line));
				}
			} else {
				TownyMessaging.sendMessage(player, TownyFormatter.getStatus(Globalworld));
			}

			return;
		}

		try {

			if (split[0].equalsIgnoreCase("?")) {
				HelpMenu.TOWNYWORLD_HELP.send(sender);
			} else if (split[0].equalsIgnoreCase("list")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_LIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				listWorlds(player, sender);

			} else if (split[0].equalsIgnoreCase("set")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_SET.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				worldSet(player, sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("toggle")) {

				worldToggle(player, sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("regen")) {
				
				TownyMessaging.sendErrorMsg(player, "This command has been removed for 1.13 compatibility, look for its return in the future.");
//				if (isConsole)
//					throw new TownyException("Command cannot be run from console.");
//
//				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_REGEN.getNode()))
//					throw new TownyException(Translation.of("msg_err_command_disable"));
//
//				if (TownyUniverse.isWarTime()) {
//					TownyMessaging.sendErrorMsg(player, Translation.of("msg_war_cannot_do"));
//					return;
//				}
//
//				if (!TownyUniverse.getPermissionSource().isTownyAdmin(player)) {
//					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_admin_only"));
//					return;
//				}
//
//				if (TownySettings.getTownBlockSize() != 16) {
//					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_regen_wrong_size"));
//					return;
//				}
//
//				// Regen this chunk
//				if (player != null) {
//					TownyRegenAPI.regenChunk(player);
//				}
//
//			} else if (split[0].equalsIgnoreCase("undo")) {
//				if (isConsole)
//					throw new TownyException("Command cannot be run from console.");
//
//				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_UNDO.getNode()))
//					throw new TownyException(Translation.of("msg_err_command_disable"));
//
//				if (player != null)
//					try {
//						TownyUniverse.getDataSource().getResident(player).regenUndo();
//					} catch (NotRegisteredException e) {
//						// Failed to get resident
//					}

			} else {
				/*
				 * try { TownyWorld world =
				 * plugin.getTownyUniverse().getWorld(split[0]);
				 * TownyMessaging.sendMessage(player,
				 * plugin.getTownyUniverse().getStatus(world)); } catch
				 * (NotRegisteredException x) { plugin.sendErrorMsg(player,
				 * String.format(Translation.of
				 * ("msg_err_not_registered_1"), split[0])); }
				 */
			}

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void listWorlds(Player player, CommandSender sender) {

		if (player == null) {
			sender.sendMessage(ChatTools.formatTitle(Translation.of("world_plu")));
		} else
			player.sendMessage(ChatTools.formatTitle(Translation.of("world_plu")));

		ArrayList<String> formatedList = new ArrayList<>();
		HashMap<String, Integer> playersPerWorld = BukkitTools.getPlayersPerWorld();
		for (TownyWorld world : TownyUniverse.getInstance().getDataSource().getWorlds()) {
			int numPlayers = playersPerWorld.getOrDefault(world.getName(), 0);
			formatedList.add(Colors.LightBlue + world.getName() + Colors.Blue + " [" + numPlayers + "]" + Colors.White);
		}

		if (player == null) {
			for (String line : ChatTools.list(formatedList)) {
				sender.sendMessage(line);
			}
		} else {
			for (String line : ChatTools.list(formatedList)) {
				player.sendMessage(line);
			}
		}
	}

	public void worldToggle(Player player, CommandSender sender, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0 ) {
			if (!isConsole) {		
				player.sendMessage(ChatTools.formatTitle("/TownyWorld toggle"));
				player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "claimable", ""));
				player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "usingtowny", ""));
				player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "pvp/forcepvp", ""));
				player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "explosion/forceexplosion", ""));
				player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "fire/forcefire", ""));
				player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "townmobs/worldmobs", ""));
				player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "revertunclaim/revertexpl", ""));
			} else {
				sender.sendMessage(ChatTools.formatTitle("/TownyWorld toggle"));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "claimable", ""));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "usingtowny", ""));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "warallowed", ""));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "pvp/forcepvp", ""));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "explosion/forceexplosion", ""));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "fire/forcefire", ""));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "townmobs/worldmobs", ""));
				sender.sendMessage(ChatTools.formatCommand("", "/TownyWorld {world} toggle", "revertunclaim/revertexpl", ""));
			}
		} else {

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			String msg;

			if (split[0].equalsIgnoreCase("claimable")) {

				Globalworld.setClaimable(!Globalworld.isClaimable());
				msg = Translation.of("msg_set_claim", Globalworld.getName(), Globalworld.isClaimable() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("usingtowny")) {

				Globalworld.setUsingTowny(!Globalworld.isUsingTowny());
				plugin.resetCache();
				msg = String.format(Globalworld.isUsingTowny() ? Translation.of("msg_set_use_towny_on") : Translation.of("msg_set_use_towny_off"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);
			
			} else if (split[0].equalsIgnoreCase("warallowed")) {

				Globalworld.setWarAllowed(!Globalworld.isWarAllowed());
				plugin.resetCache();
				msg = String.format(Globalworld.isWarAllowed() ? Translation.of("msg_set_war_allowed_on") : Translation.of("msg_set_war_allowed_off"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);
				
			} else if (split[0].equalsIgnoreCase("pvp")) {

				Globalworld.setPVP(!Globalworld.isPVP());
				msg = Translation.of("msg_changed_world_setting", "Global PVP", Globalworld.getName(), Globalworld.isPVP() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forcepvp")) {

				Globalworld.setForcePVP(!Globalworld.isForcePVP());
				msg = Translation.of("msg_changed_world_setting", "Force town PVP", Globalworld.getName(), Globalworld.isForcePVP() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("explosion")) {

				Globalworld.setExpl(!Globalworld.isExpl());
				msg = Translation.of("msg_changed_world_setting", "Explosions", Globalworld.getName(), Globalworld.isExpl() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forceexplosion")) {

				Globalworld.setForceExpl(!Globalworld.isForceExpl());
				msg = Translation.of("msg_changed_world_setting", "Force town Explosions", Globalworld.getName(), Globalworld.isForceExpl() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("fire")) {

				Globalworld.setFire(!Globalworld.isFire());
				msg = Translation.of("msg_changed_world_setting", "Fire Spread", Globalworld.getName(), Globalworld.isFire() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forcefire")) {

				Globalworld.setForceFire(!Globalworld.isForceFire());
				msg = Translation.of("msg_changed_world_setting", "Force town Fire Spread", Globalworld.getName(), Globalworld.isForceFire() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("townmobs")) {

				Globalworld.setForceTownMobs(!Globalworld.isForceTownMobs());
				msg = Translation.of("msg_changed_world_setting", "Town Mob spawns", Globalworld.getName(), Globalworld.isForceTownMobs() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("worldmobs")) {

				Globalworld.setWorldMobs(!Globalworld.hasWorldMobs());
				msg = Translation.of("msg_changed_world_setting", "World Mob spawns", Globalworld.getName(), Globalworld.hasWorldMobs() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("revertunclaim")) {

				Globalworld.setUsingPlotManagementRevert(!Globalworld.isUsingPlotManagementRevert());
				msg = Translation.of("msg_changed_world_setting", "Unclaim Revert", Globalworld.getName(), Globalworld.isUsingPlotManagementRevert() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("revertexpl")) {

				Globalworld.setUsingPlotManagementWildRevert(!Globalworld.isUsingPlotManagementWildRevert());
				msg = Translation.of("msg_changed_world_setting", "Wilderness Explosion Revert", Globalworld.getName(), Globalworld.isUsingPlotManagementWildRevert() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else {
				msg = Translation.of("msg_err_invalid_property", "'" + split[0] + "'");
				if (player != null)
					TownyMessaging.sendErrorMsg(player, msg);
				else
					TownyMessaging.sendErrorMsg(msg);
				return;
			}
			
			townyUniverse.getDataSource().saveWorld(Globalworld);
			
			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(Globalworld);
			Bukkit.getServer().getPluginManager().callEvent(event);
		}

	}

	public void worldSet(Player player, CommandSender sender, String[] split) {

		if (split.length == 0) {
			HelpMenu.TOWNYWORLD_SET.send(sender);
		} else {

			if (split[0].equalsIgnoreCase("usedefault")) {

				Globalworld.setUsingDefault();
				plugin.resetCache();
				if (player != null)
					TownyMessaging.sendMsg(player, Translation.of("msg_usedefault", Globalworld.getName()));
				else
					sender.sendMessage(Translation.of("msg_usedefault", Globalworld.getName()));

			} else if (split[0].equalsIgnoreCase("wildperm")) {

				if (split.length < 2) {
					// set default wildperm settings (/tw set wildperm)
					Globalworld.setUsingDefault();
					if (player != null)
						TownyMessaging.sendMsg(player, Translation.of("msg_usedefault", Globalworld.getName()));
					else
						sender.sendMessage(Translation.of("msg_usedefault", Globalworld.getName()));
				} else
					try {
						List<String> perms = Arrays.asList(StringMgmt.remFirstArg(split));
						Globalworld.setUnclaimedZoneBuild(perms.contains("build"));
						Globalworld.setUnclaimedZoneDestroy(perms.contains("destroy"));
						Globalworld.setUnclaimedZoneSwitch(perms.contains("switch"));
						Globalworld.setUnclaimedZoneItemUse(perms.contains("itemuse"));

						plugin.resetCache();
						if (player != null)
							TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_perms", Globalworld.getName(), perms.toString()));
						else
							sender.sendMessage(Translation.of("msg_set_wild_perms", Globalworld.getName(), perms.toString()));
					} catch (Exception e) {
						if (player != null)
							TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildperm build destroy");
						else
							sender.sendMessage("Eg: /townyworld set wildperm build destroy <world>");
					}

			} else if (split[0].equalsIgnoreCase("wildignore")) {

				if (split.length < 2)
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildignore SAPLING,GOLD_ORE,IRON_ORE");
					else
						sender.sendMessage("Eg: /townyworld set wildignore SAPLING,GOLD_ORE,IRON_ORE <world>");
				else
					try {
						List<String> mats = new ArrayList<>();
						for (String s : StringMgmt.remFirstArg(split))
							mats.add(Material.matchMaterial(s.trim().toUpperCase()).name());

						Globalworld.setUnclaimedZoneIgnore(mats);

						plugin.resetCache();
						if (player != null)
							TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_ignore", Globalworld.getName(), Globalworld.getUnclaimedZoneIgnoreMaterials()));
						else
							sender.sendMessage(Translation.of("msg_set_wild_ignore", Globalworld.getName(), Globalworld.getUnclaimedZoneIgnoreMaterials()));

					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_input", " on/off."));
					}

			} else if (split[0].equalsIgnoreCase("wildregen")) {

				if (split.length < 2)
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildregen Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart");
					else
						sender.sendMessage("Eg: /townyworld set wildregen Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart <world>");
				else {

					List<String> entities = new ArrayList<>(Arrays.asList(StringMgmt.remFirstArg(split)));

					Globalworld.setPlotManagementWildRevertEntities(entities);

					if (player != null)
						TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_regen", Globalworld.getName(), Globalworld.getPlotManagementWildRevertEntities()));
					else
						sender.sendMessage(Translation.of("msg_set_wild_regen", Globalworld.getName(), Globalworld.getPlotManagementWildRevertEntities()));

				}

			} else if (split[0].equalsIgnoreCase("wildname")) {

				if (split.length < 2) {
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildname Wildy");
				} else
					try {
						Globalworld.setUnclaimedZoneName(split[1]);

						if (player != null)
							TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_name", Globalworld.getName(), split[1]));
						else
							sender.sendMessage(Translation.of("msg_set_wild_name", Globalworld.getName(), split[1]));
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_input", " on/off."));
					}
			} else {
				if (player != null)
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "world"));
				return;
			}

			TownyUniverse.getInstance().getDataSource().saveWorld(Globalworld);
		}
	}

}
