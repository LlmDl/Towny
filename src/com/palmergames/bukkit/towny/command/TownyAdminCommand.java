package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.BackupTask;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MemMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Send a list of all general townyadmin help commands to player Command:
 * /townyadmin
 */

public class TownyAdminCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> ta_panel = new ArrayList<>();
	
	private static final List<String> adminTabCompletes = Arrays.asList(
		"delete",
		"plot",
		"resident",
		"town",
		"nation",
		"reset",
		"toggle",
		"set",
		"givebonus",
		"reload",
		"backup",
		"checkperm",
		"newday",
		"newhour",
		"unclaim",
		"purge",
		"mysqldump",
		"tpplot",
		"database",
		"depositall",
		"war"
	);

	private static final List<String> adminWarTabCompletes = Arrays.asList(
		"riot",
		"townwar",
		"civilwar",
		"nationwar",
		"worldwar",
		"list"
	);
	
	private static final List<String> adminTownTabCompletes = Arrays.asList(
		"new",
		"add",
		"remove",
		"kick",
		"rename",
		"spawn",
		"tpplot",
		"outpost",
		"delete",
		"rank",
		"toggle",
		"set",
		"meta",
		"deposit",
		"withdraw",
		"bankhistory",
		"outlaw",
		"leavenation",
		"invite"
	);

	private static final List<String> adminNationTabCompletes = Arrays.asList(
		"add",
		"kick",
		"rename",
		"delete",
		"toggle",
		"set",
		"meta",
		"deposit",
		"withdraw",
		"bankhistory",
		"rank"
	);

	private static final List<String> adminToggleTabCompletes = Arrays.asList(
		"war",
		"neutral",
		"npc",
		"debug",
		"devmode",
		"townwithdraw",
		"nationwithdraw"
	);
	
	private static final List<String> adminPlotTabCompletes = Arrays.asList(
		"claim",
		"meta"
	);
	
	private static final List<String> adminMetaTabCompletes = Arrays.asList(
		"set",
		"add",
		"remove"
	);
	
	private static final List<String> adminDatabaseTabCompletes = Arrays.asList(
		"save",
		"load",
		"remove"
	);
	
	private static final List<String> adminResidentTabCompletes = Arrays.asList(
		"rename",
		"friend",
		"unjail"
	);
	
	private static final List<String> adminResidentFriendTabCompletes = Arrays.asList(
		"add",
		"remove",
		"list",
		"clear"
	);
	
	private static final List<String> adminSetCompletes = Arrays.asList(
		"mayor",
		"capital",
		"title",
		"surname",
		"plot"
	);
	

	private boolean isConsole;
	private Player player;
	private CommandSender sender;

	public TownyAdminCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		this.sender = sender;

		if (sender instanceof Player) {
			player = (Player) sender;
			isConsole = false;

		} else {
			isConsole = true;
			this.player = null;
		}

		try {
			return parseTownyAdminCommand(args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase()) {
			case "reload":
				if (args.length > 1)
					return NameUtil.filterByStart(Arrays.asList("database", "db", "config", "perms", "permissions", "language", "lang", "townyperms", "all"), args[1]);
			case "set":
				if (args.length > 1) {
					switch (args[1].toLowerCase()) {
						case "mayor":
							switch (args.length) {
								case 3:
									return getTownyStartingWith(args[2], "t");
								case 4:
									return filterByStartOrGetTownyStartingWith(Collections.singletonList("npc"), args[3], "+r");
							}
						case "capital":
						case "plot":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "t");
						case "title":
						case "surname":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "r");
						default:
							if (args.length == 2)
								return NameUtil.filterByStart(adminSetCompletes, args[1]);
					}
				}
				break;
			case "plot":
				if (args.length == 2) {
					return NameUtil.filterByStart(adminPlotTabCompletes, args[1]);
				} else if (args.length > 2) {
					switch (args[1].toLowerCase()) {
						case "claim":
							return getTownyStartingWith(args[2], "r");
						case "meta":
							if (args.length == 3)
								return NameUtil.filterByStart(adminMetaTabCompletes, args[2]);
					}
				}
				break;
			case "givebonus":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "rt");
				break;
			case "toggle":
				if (args.length == 2) {
					return NameUtil.filterByStart(adminToggleTabCompletes, args[1]);
				} else if (args.length >= 3 && args[1].equalsIgnoreCase("npc")) {
					if (args.length == 3) {
						return getTownyStartingWith(args[2], "r");
					} else if (args.length == 4) {
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[3]);
					}
				} else if (args.length >= 3 && args[1].equalsIgnoreCase("war")) {
					if (args.length == 3) {
						return TownyUniverse.getInstance().getWarNames();
					}
				} else if (args.length == 3) {
					return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
				}
				break;
			case "war":
				if (args.length == 2) {
					return NameUtil.filterByStart(adminWarTabCompletes, args[1]);
				} else if (args.length >= 3) {
					if (args.length == 3) {
						switch (args[1].toLowerCase()) {
							case "civilwar":
								return getTownyStartingWith(args[2], "n");
							case "riot":
								return getTownyStartingWith(args[2], "t");
							case "townwar":
								return getTownyStartingWith(args[2], "t");
							case "nationwar":
								return getTownyStartingWith(args[2], "n");
						}
					} else if (args.length == 4) {
						switch (args[1].toLowerCase()) {
							case "townwar":
								return getTownyStartingWith(args[3], "t");
							case "nationwar":
								return getTownyStartingWith(args[3], "n");
						}
					}
				}
			case "tpplot":
				if (args.length == 2) {
					return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getWorlds()
						.stream()
						.map(TownyWorld::getName)
						.collect(Collectors.toList()), args[1]);
				}
				break;
			case "checkperm":
			case "delete":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "r");
				break;
			case "database":
				if (args.length == 2)
					return NameUtil.filterByStart(adminDatabaseTabCompletes, args[1]);
				if (args.length == 3 && args[1].equalsIgnoreCase("remove"))
					return Collections.singletonList("titles");
				break;
			case "resident":
				switch (args.length) {
					case 2:
						return getTownyStartingWith(args[1], "r");
					case 3:
						return NameUtil.filterByStart(adminResidentTabCompletes, args[2]);
					case 4:
						if (args[2].equalsIgnoreCase("friend"))
							return NameUtil.filterByStart(adminResidentFriendTabCompletes, args[3]);
				}
				break;
			case "town":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+t");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					switch (args[2].toLowerCase()) {
						case "add":
							if (args.length == 4)
								return null;
						case "kick":
							if (args.length == 4)
								return getResidentsOfTownStartingWith(args[1], args[3]);
						case "rank":
							switch (args.length) {
								case 4:
									return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args[3]);
								case 5:
									return getResidentsOfTownStartingWith(args[1], args[4]);
								case 6:
									switch (args[3].toLowerCase()) {
										case "add":
											return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[5]);
										case "remove": {
											Resident res = TownyUniverse.getInstance().getResident(args[4]);
											if (res != null)
												return NameUtil.filterByStart(res.getTownRanks(), args[5]);
											break;
										}
									}
							}
							break;
						case "set": {
							final Town town = TownyUniverse.getInstance().getTown(args[1]);
							if (town != null)
								return TownCommand.townSetTabComplete(town, StringMgmt.remArgs(args, 2));
							break;
						}
						case "toggle":
							if (args.length == 4)
								return NameUtil.filterByStart(TownCommand.townToggleTabCompletes, args[3]);
							else if (args.length == 5 && !args[3].equalsIgnoreCase("jail"))
								return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[4]);
						case "outlaw":
							switch (args.length) {
							case 4:
								return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args[3]);
							case 5:
								switch (args[3].toLowerCase()) {
									case "add":
										return getTownyStartingWith(args[4], "r");
									case "remove": {
										final Town town = TownyUniverse.getInstance().getTown(args[1]);
										
										if (town != null)
											return NameUtil.filterByStart(NameUtil.getNames(town.getOutlaws()), args[4]);
										
										break;
									}
								}
							}
						case "invite":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "r");
						case "meta":
							if (args.length == 4) {
								return NameUtil.filterByStart(adminMetaTabCompletes, args[3]);
							}
							break;
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(adminTownTabCompletes, args[2]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "r");
				}
				break;
			case "nation":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+n");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					switch (args[2].toLowerCase()) {
						case "add":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						case "toggle":
							if (args.length == 4) 
								return NameUtil.filterByStart(NationCommand.nationToggleTabCompletes, args[3]);
							else if (args.length == 5)
								return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[4]);
						case "set": {
							Nation nation = TownyUniverse.getInstance().getNation(args[1]);
							if (nation != null) {
								return NationCommand.nationSetTabComplete(nation, StringMgmt.remArgs(args, 2));
							}
							else {
								return Collections.emptyList();
							}
						}
						case "merge":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "n");
						case "rank":
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("add","remove"), args[3]);
							else if (args.length == 5)
								return getTownyStartingWith(args[4], "r");
							else if (args.length == 6)
								return NameUtil.filterByStart(TownyPerms.getNationRanks(), args[5]);
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(adminNationTabCompletes, args[2]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "t");
				}
				break;
			case "unclaim":
				if (args.length == 2)
					return NameUtil.filterByStart(TownCommand.townUnclaimTabCompletes, args[1]);
			default:
				if (args.length == 1)
					return NameUtil.filterByStart(adminTabCompletes, args[0]);
		}
		
		return Collections.emptyList();
	}

	private CommandSender getSender() {
		return sender;
	}

	public boolean parseTownyAdminCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (getSender()==player && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SCREEN.getNode()))
			throw new TownyException(Translation.of("msg_err_command_disable"));
		if (split.length == 0) {
			buildTAPanel();
			for (String line : ta_panel) {
				sender.sendMessage(line);
			}

		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TA_HELP.send(sender);
		} else {

			if (split[0].equalsIgnoreCase("set")) {

				adminSet(StringMgmt.remFirstArg(split));
				return true;
			} else if (split[0].equalsIgnoreCase("resident")){
				
				parseAdminResidentCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("town")) {

				parseAdminTownCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("nation")) {

				parseAdminNationCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("toggle")) {

				parseToggleCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("plot")) {

				parseAdminPlotCommand(StringMgmt.remFirstArg(split));
				return true;

			}

			if ((!isConsole) && (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode(split[0].toLowerCase()))))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (split[0].equalsIgnoreCase("givebonus") || split[0].equalsIgnoreCase("giveplots")) {

				giveBonus(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("reload")) {
				if (split.length == 2) {
					switch (split[1]) {
						case "db":
						case "database":
							reloadDatabase();
							break;
						case "config":
							reloadConfig(false);
							break;
						case "perms":
						case "townyperms":
						case "permissions":
							reloadPerms();
							break;
						case "language":
						case "lang":
							reloadLangs();
							break;
						case "all":
							reloadConfig(false);
							reloadLangs();
							reloadPerms();
							reloadDatabase();
							break;
						default:
							showReloadHelp();
					}
				} else {
					showReloadHelp();
					return false;
				}
			} else if (split[0].equalsIgnoreCase("reset")) {

				reloadConfig(true);

			} else if (split[0].equalsIgnoreCase("backup")) {

				CompletableFuture.runAsync(new BackupTask())
					.thenRun(()-> TownyMessaging.sendMsg(getSender(), Translation.of("mag_backup_success")));
				
			} else if (split[0].equalsIgnoreCase("database")) {

				parseAdminDatabaseCommand(StringMgmt.remFirstArg(split));
				return true;				
				
			} else if (split[0].equalsIgnoreCase("mysqldump")) {
				if (TownySettings.getSaveDatabase().equalsIgnoreCase("mysql") && TownySettings.getLoadDatabase().equalsIgnoreCase("mysql")) {
					TownyDataSource dataSource = new TownyFlatFileSource(plugin, townyUniverse);
					dataSource.saveAll();
					TownyMessaging.sendMsg(getSender(), Translation.of("msg_mysql_dump_success"));
					return true;
				} else 
					throw new TownyException(Translation.of("msg_err_mysql_not_being_used"));

			} else if (split[0].equalsIgnoreCase("newday")) {

				TownyTimerHandler.newDay();

			} else if (split[0].equalsIgnoreCase("newhour")) {

				TownyTimerHandler.newHour();
				TownyMessaging.sendMsg(getSender(), Translation.of("msg_newhour_success"));

			} else if (split[0].equalsIgnoreCase("purge")) {

				purge(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("delete")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentDelete(player, newSplit);
			} else if (split[0].equalsIgnoreCase("unclaim")) {

				parseAdminUnclaimCommand(StringMgmt.remFirstArg(split));
				/*
				 * else if (split[0].equalsIgnoreCase("seed") &&
				 * TownySettings.getDebug()) seedTowny(); else if
				 * (split[0].equalsIgnoreCase("warseed") &&
				 * TownySettings.getDebug()) warSeed(player);
				 */
				
			} else if (split[0].equalsIgnoreCase("checkperm")) {
				
				parseAdminCheckPermCommand(StringMgmt.remFirstArg(split));
				
			} else if (split[0].equalsIgnoreCase("tpplot")) {
				
				parseAdminTpPlotCommand(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("depositall")) {
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				parseAdminDepositAllCommand(StringMgmt.remFirstArg(split));
				
			} else if (split[0].equalsIgnoreCase("war")) {
				if (!TownySettings.isUsingEconomy())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				parseAdminWarCommand(StringMgmt.remFirstArg(split));
				
			} else {
				TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_sub"));
				return false;
			}
		}

		return true;
	}

	private void parseAdminDatabaseCommand(String[] split) {
	
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin database"));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin database", "save", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin database", "load", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin database", "remove ?", ""));
			return;
		}
		
		if (split[0].equalsIgnoreCase("save")) {
			if (TownyUniverse.getInstance().getDataSource().saveAll())
				TownyMessaging.sendMsg(getSender(), Translation.of("msg_save_success"));
	
		} else if (split[0].equalsIgnoreCase("load")) {
			TownyUniverse.getInstance().clearAllObjects();			
			if (TownyUniverse.getInstance().getDataSource().loadAll()) {
				TownyMessaging.sendMsg(getSender(), Translation.of("msg_load_success"));
				Bukkit.getPluginManager().callEvent(new TownyLoadedDatabaseEvent());
			}
		} else if (split[0].equalsIgnoreCase("remove")) {
			parseAdminDatabaseRemoveCommand(StringMgmt.remFirstArg(split));
		}
	}

	private void parseAdminDatabaseRemoveCommand(String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin database remove"));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin database remove", "titles", "Removes all titles and surnames from every resident."));
			return;
		}
		
		if (split[0].equalsIgnoreCase("titles")) {
			TownyUniverse.getInstance().getResidents().stream()
				.forEach(resident -> {
					resident.setTitle("");
					resident.setSurname("");
					resident.save();
				});
			TownyMessaging.sendMsg(getSender(), Translation.of("msg_ta_removed_all_titles_and_surnames_removed"));
		}
		
	}

	private void parseAdminPlotCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (isConsole) {
			sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
			return;
		}

		if (split.length == 0 || split.length < 1 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin plot"));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin plot claim", "[player]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin plot meta", "", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin plot meta", "set [key] [value]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin plot meta", "[add|remove] [key]", ""));
			return;
		}

		if (split[0].equalsIgnoreCase("meta")) {
			handlePlotMetaCommand(player, split);
			return;
		}
		
		if (split[0].equalsIgnoreCase("claim")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIM.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (split.length == 1) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_ta_plot_claim"));
				return;
			}			
			Optional<Resident> resOpt = townyUniverse.getResidentOpt(split[1]);
			
			if (!resOpt.isPresent()) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_no_player_with_that_name", split[1]));
				return;
			}

			Player player = BukkitTools.getPlayer(sender.getName());
			String world = player.getWorld().getName();
			List<WorldCoord> selection = new ArrayList<>();
			selection.add(new WorldCoord(world, Coord.parseCoord(player)));
			new PlotClaim(plugin, player, resOpt.get(), selection, true, true, false).start();
		}
		
		
	}

	private void parseAdminCheckPermCommand(String[] split) throws TownyException {
		
		if (split.length !=2 ) {
			throw new TownyException(Translation.of("msg_err_invalid_input", "Eg: /ta checkperm {name} {node}"));
		}
		Player player = BukkitTools.getPlayer(split[0]);
		if (player == null) {
			throw new TownyException("Player couldn't be found");
		}
		String node = split[1];
		if (player.hasPermission(node))
			TownyMessaging.sendMessage(sender, "Permission true");
		else
			TownyMessaging.sendErrorMsg(sender, "Permission false");
	}

	private void parseAdminTpPlotCommand(String[] split) throws TownyException {

		if (split.length != 3) {
			throw new TownyException(Translation.of("msg_err_invalid_input", "Eg: /ta tpplot world x z"));
		}
		
		Player player = (Player) sender;
		World world;
		double x;
		double y = 1.0;
		double z;
		Location loc;
		if (Bukkit.getServer().getWorld(split[0]) != null ) {
			world =  Bukkit.getServer().getWorld(split[0]);
			x = Double.parseDouble(split[1]) * TownySettings.getTownBlockSize();
			z = Double.parseDouble(split[2]) * TownySettings.getTownBlockSize();
		} else {
			throw new TownyException(Translation.of("msg_err_invalid_input", "Eg: /ta tpplot world x z"));
		}
		y = Bukkit.getWorld(world.getName()).getHighestBlockYAt(new Location(world, x, y, z));
		loc = new Location(world, x, y, z);
		player.teleport(loc, TeleportCause.PLUGIN);
	}

	private void giveBonus(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Town town;
		boolean isTown = false;

		if (split.length != 2)
			throw new TownyException(Translation.of("msg_err_invalid_input", "Eg: givebonus [town/player] [n]"));

		if ((town = townyUniverse.getTown(split[0])) != null) {
			isTown = true;
		}
		else {
			Resident target = getResidentOrThrow(split[0]);

			if (!target.hasTown()) {
				throw new TownyException(Translation.of("msg_err_resident_doesnt_belong_to_any_town"));
			}

			try {
				town = target.getTown();
			} catch (NotRegisteredException ignore) {
			}
		}
		
		int extraBlocks;
		
		try {
			extraBlocks = Integer.parseInt(split[1].trim());
		} catch (NumberFormatException ex) {
			throw new TownyException(Translation.of("msg_error_must_be_int"));
		}

		town.setBonusBlocks(town.getBonusBlocks() + extraBlocks);
		TownyMessaging.sendMsg(getSender(), Translation.of("msg_give_total", town.getName(), split[1], town.getBonusBlocks()));
		if (!isConsole || isTown)
			TownyMessaging.sendTownMessagePrefixed(town, "You have been given " + extraBlocks + " bonus townblocks.");
		if (isConsole && !isTown) {
			TownyMessaging.sendMessage(town, "You have been given " + extraBlocks + " bonus townblocks.");
			TownyMessaging.sendMessage(town, "If you have paid any real-life money for these townblocks please understand: the creators of Towny do not condone this transaction, the server you play on breaks the Minecraft EULA and, worse, is selling a part of Towny which the developers did not intend to be sold.");
			TownyMessaging.sendMessage(town, "If you did pay real money you should consider playing on a Towny server that respects the wishes of the Towny Team.");
		}
		town.save();

	}

	private void buildTAPanel() {

		ta_panel.clear();
		Runtime run = Runtime.getRuntime();
		ta_panel.add(ChatTools.formatTitle(Translation.of("ta_panel_1")));
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] " + Colors.Green + Translation.of("ta_panel_2") + Colors.LightGreen + TownyAPI.getInstance().isWarTime() + Colors.Gray + " | " + Colors.Green + Translation.of("ta_panel_3") + (TownyTimerHandler.isHealthRegenRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off") + Colors.Gray + " | " + (Colors.Green + Translation.of("ta_panel_5") + (TownyTimerHandler.isDailyTimerRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")));
		/*
		 * ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" +
		 * Colors.Blue + "] " + Colors.Green +
		 * Translation.of("ta_panel_4") +
		 * (TownySettings.isRemovingWorldMobs() ? Colors.LightGreen + "On" :
		 * Colors.Rose + "Off") + Colors.Gray + " | " + Colors.Green +
		 * Translation.of("ta_panel_4_1") +
		 * (TownySettings.isRemovingTownMobs() ? Colors.LightGreen + "On" :
		 * Colors.Rose + "Off"));
		 *
		 * try { TownyEconomyObject.checkEconomy(); ta_panel.add(Colors.Blue +
		 * "[" + Colors.LightBlue + "Economy" + Colors.Blue + "] " +
		 * Colors.Green + Translation.of("ta_panel_6") +
		 * Colors.LightGreen + TownyFormatter.formatMoney(getTotalEconomy()) +
		 * Colors.Gray + " | " + Colors.Green +
		 * Translation.of("ta_panel_7") + Colors.LightGreen +
		 * getNumBankAccounts()); } catch (Exception e) { }
		 */
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + Translation.of("ta_panel_8") + Colors.Blue + "] " + Colors.Green + Translation.of("ta_panel_9") + Colors.LightGreen + MemMgmt.getMemSize(run.totalMemory()) + Colors.Gray + " | " + Colors.Green + Translation.of("ta_panel_10") + Colors.LightGreen + Thread.getAllStackTraces().keySet().size() + Colors.Gray + " | " + Colors.Green + Translation.of("ta_panel_11") + Colors.LightGreen + TownyFormatter.getTime());
		ta_panel.add(Colors.Yellow + MemMgmt.getMemoryBar(50, run));

	}

	public void parseAdminUnclaimCommand(String[] split) {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_UNCLAIM.send((CommandSender) getSender());
		} else {

			if (isConsole) {
				sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
				return;
			}

			try {

				List<WorldCoord> selection;
				selection = AreaSelectionUtil.selectWorldCoordArea(null, new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)), split);
				selection = AreaSelectionUtil.filterOutWildernessBlocks(selection);

				new TownClaim(plugin, player, null, selection, false, false, true).start();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public void parseAdminResidentCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin resident"));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin resident", "[resident]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin resident", "[resident] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin resident", "[resident] friend... [add|remove] [resident]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin resident", "[resident] friend... |list|clear]", ""));
			
			return;
		}

		try	{
			Resident resident = getResidentOrThrow(split[0]);

			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(resident, player));
				return;
			}
						
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT.getNode(split[1].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if(split[1].equalsIgnoreCase("rename"))	{
				
				if (!NameValidation.isBlacklistName(split[2])) {
					townyUniverse.getDataSource().renamePlayer(resident, split[2]);
				} else
					TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_invalid_name"));
				
			} else if(split[1].equalsIgnoreCase("friend"))	{
				
				if (split.length == 2) {
					sender.sendMessage(ChatTools.formatTitle("/townyadmin resident {resident} friend"));
					sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin resident", "[resident] friend... [add|remove] [resident]", ""));
					sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin resident", "[resident] friend... |list|clear]", ""));
					return;
				}
				if (isConsole)
					throw new TownyException("/ta resident {resident} friend cannot be run from console.");

				ResidentCommand.residentFriend(BukkitTools.getPlayer(sender.getName()), StringMgmt.remArgs(split, 2), true, resident);

			} else if(split[1].equalsIgnoreCase("unjail")) {
				
				Player jailedPlayer = TownyAPI.getInstance().getPlayer(resident);
				if (player == null) {
					throw new TownyException(String.format("%s is not online", resident.getName()));
				}

				if(resident.isJailed())	{
					resident.setJailed(false);
					final String jailTownName = resident.getJailTown();
					final int index = resident.getJailSpawn();
					
					final Town jailTown = townyUniverse.getTown(jailTownName);
					
					if (jailTown == null) {
						throw new TownyException(String.format("Invalid jail town of '%s'", jailTownName));
					}
					
					final Location loc = Bukkit.getWorld(jailTown.getHomeblockWorld().getName()).getSpawnLocation();

					// Use teleport warmup
					jailedPlayer.sendMessage(Translation.of("msg_town_spawn_warmup", TownySettings.getTeleportWarmupTime()));
					TownyAPI.getInstance().jailTeleport(jailedPlayer, loc);

					resident.removeJailSpawn();
					resident.setJailTown("");
					TownyMessaging.sendMsg(player, "You have been freed from jail.");
					TownyMessaging.sendPrefixedTownMessage(jailTown, jailedPlayer.getName() + " has been freed from jail number " + index);
				} else {
					throw new TownyException(Translation.of("msg_player_not_jailed_in_your_town"));
				}
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}
	
	public void parseAdminTownCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin town"));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "new [name] [mayor]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] add/kick [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] delete", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] spawn", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] outpost #", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] rank", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] set", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] toggle", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] meta", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] deposit [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] withdraw [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] bankhistory", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] outlaw [add|remove] [name]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] leavenation", ""));
			return;
		}

		try {
			
			if (split[0].equalsIgnoreCase("new")) {
				/*
				 * Moved from TownCommand as of 0.92.0.13
				 */
				if (split.length != 3)
					throw new TownyException(Translation.of("msg_err_not_enough_variables") + "/ta town new [name] [mayor]");

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_NEW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Optional<Resident> resOpt = TownyUniverse.getInstance().getResidentOpt(split[2]);
				
				if (!resOpt.isPresent()) {
					TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_not_registered_1", split[2]));
					return;
				}
				
				TownCommand.newTown(player, split[1], resOpt.get(), true);
				return;
			}
			
			Town town = townyUniverse.getTown(split[0]);
			
			if (town == null) {
				TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_not_registered_1", split[0]));
				return;
			}
			
			
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(town));
				return;
			}

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN.getNode(split[1].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			if (split[1].equalsIgnoreCase("invite")) {
				// Give admins the ability to invite a player to town, invite still requires acceptance.
				TownCommand.townAdd(getSender(), town, StringMgmt.remArgs(split, 2));
				
			} else if (split[1].equalsIgnoreCase("add")) {
				// Force-join command for admins to use to bypass invites system.
				Resident resident = townyUniverse.getResident(split[2]);
				
				if (resident == null) {
					TownyMessaging.sendMessage(sender, Translation.of("msg_error_no_player_with_that_name", split[2]));
					return;
				}
				
				TownCommand.townAddResident(town, resident);
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_join_town", resident.getName()));
				TownyMessaging.sendMessage(sender, Translation.of("msg_join_town", resident.getName()));
				
			} else if (split[1].equalsIgnoreCase("kick")) {

				TownCommand.townKickResidents(getSender(), town.getMayor(), town, ResidentUtil.getValidatedResidents(getSender(), StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {

				Confirmation.runOnAccept(() -> {
					TownyMessaging.sendMessage(sender, Translation.of("town_deleted_by_admin", town.getName()));
					TownyUniverse.getInstance().getDataSource().removeTown(town);
				}).sendTo(sender);

			} else if (split[1].equalsIgnoreCase("rename")) {
				
				TownPreRenameEvent event = new TownPreRenameEvent(town, split[2]);
				Bukkit.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_rename_cancelled"));
					return;
				}

				if (!NameValidation.isBlacklistName(split[2])) {
					townyUniverse.getDataSource().renameTown(town, split[2]);
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_name", ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), town.getName()));
					TownyMessaging.sendMsg(getSender(), Translation.of("msg_town_set_name", ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), town.getName()));
				} else {
					TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_invalid_name"));
				}
				
			} else if (split[1].equalsIgnoreCase("spawn")) {

				SpawnUtil.sendToTownySpawn(player, StringMgmt.remArgs(split, 2), town, "", false, false, SpawnType.TOWN);

			} else if (split[1].equalsIgnoreCase("outpost")) {

				SpawnUtil.sendToTownySpawn(player, StringMgmt.remArgs(split, 2), town, "", true, false, SpawnType.TOWN);

			} else if (split[1].equalsIgnoreCase("rank")) {
				
				parseAdminTownRankCommand(player, town, StringMgmt.remArgs(split, 2));
			} else if (split[1].equalsIgnoreCase("toggle")) {
				
				if (split.length == 2 || split[2].equalsIgnoreCase("?")) {
					sender.sendMessage(ChatTools.formatTitle("/ta town {townname} toggle"));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "pvp", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "forcepvp", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "public", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "explosion", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "fire", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "mobs", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "taxpercent", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "open", ""));					
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "jail [number] [resident]", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "forcepvp", ""));

					return;
				}
				
				Optional<Boolean> choice = Optional.empty();
				if (split.length == 4) {
					choice = BaseCommand.parseToggleChoice(split[3]);
				}
				
				if (split[2].equalsIgnoreCase("forcepvp")) {
					
					town.setAdminEnabledPVP(choice.orElse(!town.isAdminEnabledPVP()));
					
					town.save();
					TownyMessaging.sendMessage(sender, Translation.of("msg_town_forcepvp_setting_set_to", town.getName(), town.isAdminEnabledPVP()));
					
				} else
					TownCommand.townToggle(sender, StringMgmt.remArgs(split, 2), true, town);
				
			} else if (split[1].equalsIgnoreCase("set")) {
				
				TownCommand.townSet(player, StringMgmt.remArgs(split, 2), true, town);
			} else if (split[1].equalsIgnoreCase("meta")) {
				
				handleTownMetaCommand(player, town, split);
			} else if (split[1].equalsIgnoreCase("bankhistory")) {

				int pages = 10;
				if (split.length > 2)
					try {
						pages = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
						return;
					}

				town.generateBankHistoryBook(player, pages);
			} else if (split[1].equalsIgnoreCase("deposit")) {
				
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translation.of("msg_err_invalid_input", "deposit [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					return;
				}
				
				if (town.getAccount().deposit(amount, "Admin Deposit")) {
					// Send notifications
					String depositMessage = Translation.of("msg_xx_deposited_xx", (isConsole ? "Console" : player.getName()), amount,  Translation.of("town_sing"));
					TownyMessaging.sendMessage(sender, depositMessage);
					TownyMessaging.sendPrefixedTownMessage(town, depositMessage);
				} else {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_unable_to_deposit_x", amount));
				}

			} else if (split[1].equalsIgnoreCase("withdraw")) {
				
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				int amount;

				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translation.of("msg_err_invalid_input", "withdraw [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					return;
				}

				if (town.getAccount().withdraw(amount, "Admin Withdraw")) {				
					// Send notifications
					String withdrawMessage = Translation.of("msg_xx_withdrew_xx", (isConsole ? "Console" : player.getName()), amount,  Translation.of("town_sing"));
					TownyMessaging.sendMessage(sender, withdrawMessage);
					TownyMessaging.sendPrefixedTownMessage(town, withdrawMessage);
				} else {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_unable_to_withdraw_x", amount));
				}
			} else if (split[1].equalsIgnoreCase("outlaw")) {
				TownCommand.parseTownOutlawCommand(sender, StringMgmt.remArgs(split, 2), true, town);
			} else if (split[1].equalsIgnoreCase("leavenation")) {
				Nation nation = null;
				if (town.hasNation())
					nation = town.getNation();
				else
					throw new TownyException(Translation.of("That town does not belong to a nation."));
				
				town.removeNation();
				
				plugin.resetCache();

				TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_town_left", StringMgmt.remUnderscore(town.getName())));
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));

			} else {
				sender.sendMessage(ChatTools.formatTitle("/townyadmin town"));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "new [name] [mayor]", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town]", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] add/kick [] .. []", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] rename [newname]", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] delete", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] spawn", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] outpost #", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] rank", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] set", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] toggle", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] meta", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] deposit [amount]", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] withdraw [amount]", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] bankhistory", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] outlaw [add|remove] [name]", ""));
				sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin town", "[town] leavenation", ""));
				
				return;
			}

		} catch (TownyException | EconomyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
		
	}

	private void parseAdminTownRankCommand(Player player, Town town, String[] split) throws TownyException {
		if (split.length < 3) {
			throw new TownyException("Eg: /townyadmin town [townname] rank add/remove [resident] [rank]");
		}

		Resident target = getResidentOrThrow(split[1]);

		if (!target.hasTown()) {
			throw new TownyException(Translation.of("msg_resident_not_your_town"));
		}
		if (target.getTown() != town) {
			throw new TownyException(Translation.of("msg_err_townadmintownrank_wrong_town"));
		}

		/*
		 * Match casing to an existing rank, returns null if Town rank doesn't exist.
		 */
		String rank = TownyPerms.matchTownRank(split[2]);
		if (rank == null)
			throw new TownyException(Translation.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getTownRanks(), ", ")));

		if (split[0].equalsIgnoreCase("add")) {
			try {
				if (target.addTownRank(rank)) {
					TownyMessaging.sendMsg(target, Translation.of("msg_you_have_been_given_rank", "Town", rank));
					TownyMessaging.sendMsg(player, Translation.of("msg_you_have_given_rank", "Town", rank, target.getName()));
				} else {
					// Not in a town or Rank doesn't exist
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_resident_not_your_town"));
					return;
				}
			} catch (AlreadyRegisteredException e) {
				// Must already have this rank
				TownyMessaging.sendMsg(player, Translation.of("msg_resident_already_has_rank", target.getName(), "Town"));
				return;
			}

		} else if (split[0].equalsIgnoreCase("remove")) {
			try {
				if (target.removeTownRank(rank)) {
					TownyMessaging.sendMsg(target, Translation.of("msg_you_have_had_rank_taken", "Town", rank));
					TownyMessaging.sendMsg(player, Translation.of("msg_you_have_taken_rank_from", "Town", rank, target.getName()));
				}
			} catch (NotRegisteredException e) {
				// Must already have this rank
				TownyMessaging.sendMsg(player, Translation.of("msg_resident_doesnt_have_rank", target.getName(), "Town"));
				return;
			}

		} else {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", split[0]));
			return;
		}

		/*
		 * If we got here we have made a change Save the altered resident
		 * data.
		 */
		target.save();
		
	}

	public void parseAdminNationCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			sender.sendMessage(ChatTools.formatTitle("/townyadmin nation"));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "new", "[name] [capital]"));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] add [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] kick [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] delete", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] recheck", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] toggle", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] set", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] deposit [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] withdraw [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[nation] bankhistory", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "[oldnation] merge [newnation]", ""));
			sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation", "rank [add/remove] [resident] [rank]"));

			return;
		}
		try {
			
			if (split[0].equalsIgnoreCase("new")) {
				/*
				 * Moved from TownCommand as of 0.92.0.13
				 */
				if (split.length != 3)
					throw new TownyException(Translation.of("msg_err_not_enough_variables") + "/ta town new [name] [capital]");

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_NEW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				final Town capitalTown = townyUniverse.getTown(split[2]);
				
				if (capitalTown == null)
					throw new TownyException(Translation.of("msg_err_invalid_name", split[2]));

				NationCommand.newNation(player, split[1], capitalTown, true);
				return;
			}
			
			Nation nation = townyUniverse.getNation(split[0]);
			
			if (nation == null) {
				throw new NotRegisteredException(Translation.of("msg_err_no_nation_with_that_name", split[0]));
			}
			
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(nation));
				return;
			}

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION.getNode(split[1].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (split[1].equalsIgnoreCase("add")) {
				/*
				 * if (isConsole) { sender.sendMessage(
				 * "[Towny] InputError: This command was designed for use in game only."
				 * ); return; }
				 */
				NationCommand.nationAdd(nation, townyUniverse.getDataSource().getTowns(StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("kick")) {

				NationCommand.nationKick(sender, nation, townyUniverse.getDataSource().getTowns(StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {
				if (!isConsole) {
					TownyMessaging.sendMessage(sender, Translation.of("nation_deleted_by_admin", nation.getName()));
					TownyMessaging.sendGlobalMessage(Translation.of("msg_del_nation", nation.getName()));
					townyUniverse.getDataSource().removeNation(nation);
				} else {
					Confirmation.runOnAccept(() -> {
						TownyUniverse.getInstance().getDataSource().removeNation(nation);
						TownyMessaging.sendGlobalMessage(Translation.of("MSG_DEL_NATION", nation.getName()));
					})
					.sendTo(sender); // It takes the nation, an admin deleting another town has no confirmation.
				}

			} else if(split[1].equalsIgnoreCase("recheck")) {
				
				nation.recheckTownDistance();
				TownyMessaging.sendMessage(sender, Translation.of("nation_rechecked_by_admin", nation.getName()));

			} else if (split[1].equalsIgnoreCase("rename")) {

				NationPreRenameEvent event = new NationPreRenameEvent(nation, split[2]);
				Bukkit.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_rename_cancelled"));
					return;
				}
				
				if (!NameValidation.isBlacklistName(split[2])) {
					townyUniverse.getDataSource().renameNation(nation, split[2]);
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_set_name", ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), nation.getName()));
				} else
					TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_invalid_name"));

			} else if (split[1].equalsIgnoreCase("merge")) {
				
				Nation remainingNation = townyUniverse.getNation(split[2]);
				
				if (remainingNation == null || remainingNation.equals(nation))
					throw new TownyException(Translation.of("msg_err_invalid_name", split[2]));
				townyUniverse.getDataSource().mergeNation(nation, remainingNation);
				TownyMessaging.sendGlobalMessage(Translation.of("nation1_has_merged_with_nation2", nation, remainingNation));

			} else if(split[1].equalsIgnoreCase("set")) {
				
				NationCommand.nationSet(player, StringMgmt.remArgs(split, 2), true, nation);

			} else if(split[1].equalsIgnoreCase("toggle")) {
				
				NationCommand.nationToggle(sender, StringMgmt.remArgs(split, 2), true, nation);
			} else if (split[1].equalsIgnoreCase("bankhistory")) {

				int pages = 10;
				if (split.length > 2)
					try {
						pages = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
						return;
					}

				nation.generateBankHistoryBook(player, pages);
			} else if (split[1].equalsIgnoreCase("deposit")) {
				
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translation.of("msg_err_invalid_input", "deposit [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					return;
				}

				nation.getAccount().deposit(amount, "Admin Deposit");
				
				// Send notifications
				String depositMessage = Translation.of("msg_xx_deposited_xx", (isConsole ? "Console" : player.getName()), amount,  Translation.of("nation_sing"));
				TownyMessaging.sendMessage(sender, depositMessage);
				TownyMessaging.sendPrefixedNationMessage(nation, depositMessage);
			}
			else if (split[1].equalsIgnoreCase("withdraw")) {
				
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(Translation.of("msg_err_invalid_input", "withdraw [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					return;
				}

				nation.getAccount().withdraw(amount, "Admin Withdraw");
				
				// Send notifications
				String withdrawMessage = Translation.of("msg_xx_withdrew_xx", (isConsole ? "Console" : player.getName()), amount,  Translation.of("nation_sing"));
				TownyMessaging.sendMessage(sender, withdrawMessage);
				TownyMessaging.sendPrefixedNationMessage(nation, withdrawMessage);
			}
			else if (split[1].equalsIgnoreCase("rank")) {
				if (split.length < 5) {
					sender.sendMessage(ChatTools.formatTitle("/townyadmin nation rank"));
					sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation rank add [resident] [rank] ", ""));
					sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation rank remove [resident] [rank] ", ""));
					return;
				}
				Resident target;
				String rank;

				try {
					target = getResidentOrThrow(split[3]);
				} catch (TownyException exception) {
					TownyMessaging.sendMessage(sender, exception.getMessage());
					return;
				}
				rank = TownyPerms.matchNationRank(split[4]);
				if (rank == null) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_unknown_rank_available_ranks", split[4], StringMgmt.join(TownyPerms.getNationRanks(), ", ")));
					return;
				}

				switch(split[2].toLowerCase()) {
					case "add":
						try {
							if (target.addNationRank(rank)) {
								if (BukkitTools.isOnline(target.getName())) {
									TownyMessaging.sendMsg(target, Translation.of("msg_you_have_been_given_rank", "Nation", rank));
									plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
								}
								TownyMessaging.sendMsg(player, Translation.of("msg_you_have_given_rank", "Nation", rank, target.getName()));
								target.save();
							} else {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_resident_not_part_of_any_town"));
								return;
							}
						} catch (AlreadyRegisteredException e) {
							TownyMessaging.sendMsg(player, Translation.of("msg_resident_already_has_rank", target.getName(), "Nation"));
							return;
						}
						return;
					case "remove":
						try {
							if (target.removeNationRank(rank)) {
								if (BukkitTools.isOnline(target.getName())) {
									TownyMessaging.sendMsg(target, Translation.of("msg_you_have_had_rank_taken", "Nation", rank));
									plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
								}
								TownyMessaging.sendMsg(player, Translation.of("msg_you_have_taken_rank_from", "Nation", rank, target.getName()));
								target.save();
							}
						} catch (NotRegisteredException e) {
							TownyMessaging.sendMsg(player, String.format("msg_resident_doesnt_have_rank", target.getName(), "Nation"));
							return;
						}
					default:
						sender.sendMessage(ChatTools.formatTitle("/townyadmin nation rank"));
						sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation rank add [resident] [rank] ", ""));
						sender.sendMessage(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin nation rank remove [resident] [rank] ", ""));
						return;
				}
			}

		} catch (NotRegisteredException | AlreadyRegisteredException | InvalidNameException | EconomyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}

	public void adminSet(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET.getNode()))
			throw new TownyException(Translation.of("msg_err_command_disable"));

		if (split.length == 0) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin set"));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] " + Translation.of("town_help_2"), ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] npc", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "capital [town]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "title [resident] [title]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "surname [resident] [surname]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "plot [town]", ""));

			return;
		}

		if (split[0].equalsIgnoreCase("mayor")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_MAYOR.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			if (split.length < 3) {

				sender.sendMessage(ChatTools.formatTitle("/townyadmin set mayor"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] " + Translation.of("town_help_2"), ""));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] npc", ""));

			} else
				try {
					Resident newMayor;
					Town town = townyUniverse.getTown(split[1]);
					
					if (town == null) {
						TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_not_registered_1", split[1]));
						return;
					}

					if (split[2].equalsIgnoreCase("npc")) {
						String name = nextNpcName();
						final UUID npcUUID = UUID.randomUUID();
						townyUniverse.getDataSource().newResident(name, npcUUID);

						newMayor = townyUniverse.getResident(npcUUID);

						newMayor.setRegistered(System.currentTimeMillis());
						newMayor.setLastOnline(0);
						newMayor.setNPC(true);

						newMayor.save();

						// set for no upkeep as an NPC mayor is assigned
						town.setHasUpkeep(false);

					} else {
						newMayor = getResidentOrThrow(split[2]);
					}

					if (!town.hasResident(newMayor)) {
						TownCommand.townAddResident(town, newMayor);
					}
					// Delete the resident if the old mayor was an NPC.
					Resident oldMayor = town.getMayor();

					town.setMayor(newMayor);

					if (oldMayor.isNPC()) {
						oldMayor.removeTown();
						townyUniverse.getDataSource().removeResident(oldMayor);
						// set upkeep again
						town.setHasUpkeep(true);
					}
					town.save();					
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_new_mayor", newMayor.getName()));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
				}

		} else if (split[0].equalsIgnoreCase("capital")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_CAPITAL.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (split.length < 2) {

				sender.sendMessage(ChatTools.formatTitle("/townyadmin set capital"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set capital", "[town name]", "[nation name]"));

			} else {
				final Town newCapital = townyUniverse.getTown(split[1]);
				
				if (newCapital == null) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[1]));
					return;
				}
				
				try {
					Nation nation = newCapital.getNation();
					NationCommand.nationSet(player, split, true, nation);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

			}
		} else if (split[0].equalsIgnoreCase("title")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_TITLE.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			Resident resident = null;
			// Give the resident a title
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "Eg: /townyadmin set title bilbo Jester");
			else
				resident = getResidentOrThrow(split[1]);

			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_input_too_long"));
				return;
			}

			String title = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setTitle(title + " ");
			resident.save();

			if (resident.hasTitle()) {
				TownyMessaging.sendMessage(sender, Translation.of("msg_set_title", resident.getName(), resident.getTitle()));
				TownyMessaging.sendMessage(resident, Translation.of("msg_set_title", resident.getName(), resident.getTitle()));
			} else {
				TownyMessaging.sendMessage(sender, Translation.of("msg_clear_title_surname", "Title", resident.getName()));
				TownyMessaging.sendMessage(resident, Translation.of("msg_clear_title_surname", "Title", resident.getName()));
			}

		} else if (split[0].equalsIgnoreCase("surname")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_SURNAME.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			Resident resident = null;
			// Give the resident a surname
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "Eg: /townyadmin set surname bilbo Jester");
			else
				resident = getResidentOrThrow(split[1]);

			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_input_too_long"));
				return;
			}

			String surname = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setSurname(surname + " ");
			resident.save();

			if (resident.hasSurname()) {
				TownyMessaging.sendMessage(sender, Translation.of("msg_set_surname", resident.getName(), resident.getSurname()));
				TownyMessaging.sendMessage(resident, Translation.of("msg_set_surname", resident.getName(), resident.getSurname()));
			} else {
				TownyMessaging.sendMessage(sender, Translation.of("msg_clear_title_surname", "Surname", resident.getName()));
				TownyMessaging.sendMessage(resident, Translation.of("msg_clear_title_surname", "Surname", resident.getName()));
			}

		} else if (split[0].equalsIgnoreCase("plot")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_PLOT.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
			if (split.length < 2) {
				sender.sendMessage(ChatTools.formatTitle("/townyadmin set plot"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set plot", "[town name]", Translation.of("msg_admin_set_plot_help_1")));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set plot", "[town name] {rect|circle} {radius}", Translation.of("msg_admin_set_plot_help_2")));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set plot", "[town name] {rect|circle} auto", Translation.of("msg_admin_set_plot_help_2")));
				return;
			}
			if (tb != null) {
				Town newTown = townyUniverse.getTown(split[1]);
				
				if (newTown != null) {
					tb.setResident(null);
					tb.setTown(newTown);
					tb.setType(TownBlockType.RESIDENTIAL);
					tb.setName("");
					TownyMessaging.sendMessage(player, Translation.of("changed_plot_town", newTown.getName()));
				}
				else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[1]));
				}
			} else {
				Town town = townyUniverse.getTown(split[1]);
				
				if (town == null) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[1]));
					return;
				}
				
				TownyWorld world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());
				List<WorldCoord> selection;
				if (split.length == 2)
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), new String[0]);
				else  {
					String[] newSplit = StringMgmt.remFirstArg(split);
					newSplit = StringMgmt.remFirstArg(newSplit);
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), newSplit);
				}
				TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = AreaSelectionUtil.filterOutTownOwnedBlocks(selection);
				TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				
				new TownClaim(plugin, player, town, selection, false, true, false).start();

			}
		} else {
			TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_property", "administrative"));
		}
	}

	public String nextNpcName() throws TownyException {

		String name;
		int i = 0;
		do {
			name = TownySettings.getNPCPrefix() + ++i;
			if (!TownyUniverse.getInstance().hasResident(name))
				return name;
			if (i > 100000)
				throw new TownyException(Translation.of("msg_err_too_many_npc"));
		} while (true);
	}
	
	public void reloadLangs() {
		String rootFolder = TownyUniverse.getInstance().getRootFolder();
		try {
			Translation.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_reload_error"));
			e.printStackTrace();
			return;
		}
		
		TownyMessaging.sendMsg(sender, Translation.of("msg_reloaded_lang"));
	}
	
	public void reloadPerms() {
		String rootFolder = TownyUniverse.getInstance().getRootFolder();
		try {
			TownyPerms.loadPerms(rootFolder + File.separator + "settings", "townyperms.yml");
		} catch (TownyException e) {
			// Place Towny in Safe Mode while the townyperms.yml is unreadable.
			plugin.setError(true);
			TownyMessaging.sendErrorMsg(sender, "Error Loading townyperms.yml!");
			return;
		}
		// If Towny is in Safe Mode (hopefully because of townyperms only) turn off Safe Mode.
		// TODO: Potentially do a full towny reload via the normal TownyUniverse.loadSettings() so that we would know if there would be a reason to have safe mode remain on. 
		if (plugin.isError())
			plugin.setError(false);
		
		// Update everyone who is online with the changes made.
		TownyPerms.updateOnlinePerms();
		TownyMessaging.sendMsg(sender, Translation.of("msg_reloaded_perms"));
		
	}

	/**
	 * Reloads only the config
	 * 
	 * @param reset Whether or not to reset the config.
	 */
	public void reloadConfig(boolean reset) {

		if (reset) {
			TownyUniverse.getInstance().getDataSource().deleteFile(plugin.getConfigPath());
			TownyMessaging.sendMsg(sender, Translation.of("msg_reset_config"));
		}
		
		try {
			String rootFolder = TownyUniverse.getInstance().getRootFolder();
			TownySettings.loadConfig(rootFolder + File.separator + "settings" + File.separator + "config.yml", plugin.getVersion());
			TownySettings.loadTownLevelConfig();   // TownLevel and NationLevels are not loaded in the config,
			TownySettings.loadNationLevelConfig(); // but later so the config-migrator can do it's work on them if needed.
			Translation.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_reload_error"));
			e.printStackTrace();
			return;
		}
		
		TownyMessaging.sendMsg(sender, Translation.of("msg_reloaded_config"));
	}

	/**
	 * Reloads both the database and the config. Used with a database reload command.
	 *
	 */
	public void reloadDatabase() {
		TownyUniverse.getInstance().getDataSource().finishTasks();
		if (plugin.load()) {

			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();

			// Update permissions for all online players
			TownyPerms.updateOnlinePerms();

		}

		TownyMessaging.sendMsg(sender, Translation.of("msg_reloaded_db"));
	}

	/**
	 * Remove residents who havn't logged in for X amount of days.
	 * 
	 * @param split - Current command arguments.
	 */
	public void purge(String[] split) {

		if (split.length == 0) {
			// command was '/townyadmin purge'
			sender.sendMessage(ChatTools.formatTitle("/townyadmin purge"));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin purge", "[number of days] {townless}", ""));
			sender.sendMessage(ChatTools.formatCommand("", "", "Removes offline residents not seen for this duration.", ""));
			sender.sendMessage(ChatTools.formatCommand("", "", "Optional {townless} flag limits purge to only people that have no town.", ""));
			return;
		}
		String days = "";
		if (split.length == 2 && split[1].equalsIgnoreCase("townless")) {
			days += "townless";
		}

		try {
			days += String.valueOf(split[0]);
		} catch (NumberFormatException e) {
			TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_error_must_be_int"));
			return;
		}

		if (!isConsole) {

			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode())) {
				try {
					throw new TownyException(Translation.of("msg_err_admin_only"));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			}

			final String finalDays = days;

			Runnable purgeHandler = () -> {
				Player player = (Player) sender;
				if (player == null) {
					try {
						throw new TownyException("Player could not be found!");
					} catch (TownyException e) {
						e.printStackTrace();
					}
				}

				int numDays;
				boolean townless = false;
				if (finalDays.startsWith("townless")) {
					townless = true;
					numDays = Integer.parseInt(finalDays.substring(8));
				} else {
					numDays = Integer.parseInt(finalDays);
				}

				new ResidentPurge(plugin, player, TimeTools.getMillis(numDays + "d"), townless).start();
			};
			
			if (sender != null) {
				Confirmation.runOnAccept(purgeHandler)
				.sendTo(sender);
			}
		} else { // isConsole
			final String finalDays = days;
			Confirmation.runOnAccept(() -> {
				int numDays;
				boolean townless = false;
				if (finalDays.startsWith("townless")) {
					townless = true;
					numDays = Integer.parseInt(finalDays.substring(8));
				} else {
					numDays = Integer.parseInt(finalDays);
				}

				new ResidentPurge(plugin, null, TimeTools.getMillis(numDays + "d"), townless).start();
			})
			.sendTo(sender);
		}
	}

	/**
	 * Delete a resident and it's data file (if not online) Available Only to
	 * players with the 'towny.admin' permission node.
	 * 
	 * @param player - Player.
	 * @param split - Current command arguments.
	 */
	public void residentDelete(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0)
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
		else
			try {
				if (!townyUniverse.getPermissionSource().isTownyAdmin(player))
					throw new TownyException(Translation.of("msg_err_admin_only_delete"));

				for (String name : split) {
					Resident resident = townyUniverse.getResident(name);
					if (resident != null) {
						if (!resident.isNPC() && !BukkitTools.isOnline(resident.getName())) {
							townyUniverse.getDataSource().removeResident(resident);
							TownyMessaging.sendGlobalMessage(Translation.of("MSG_DEL_RESIDENT", resident.getName()));
						} else
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_online_or_npc", name));
					}
					else {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_name", name));
					}
				}
			} catch (TownyException x) {
				// Admin only escape
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
	}

	public void parseToggleCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2) {
			choice = BaseCommand.parseToggleChoice(split[1]);
		} else if (split.length == 3 && split[1].equalsIgnoreCase("npc")) {
			choice = BaseCommand.parseToggleChoice(split[2]);
		}

		if (split.length == 0) {
			// command was '/townyadmin toggle'
			player.sendMessage(ChatTools.formatTitle("/townyadmin toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "war {warname}", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "peaceful", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "devmode", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "debug", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "townwithdraw/nationwithdraw", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle npc", "[resident]", ""));
			return;

		}

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE.getNode(split[0].toLowerCase())))
			throw new TownyException(Translation.of("msg_err_command_disable"));

		if (split[0].equalsIgnoreCase("war")) {
			if (!choice.orElse(!TownyAPI.getInstance().isWarTime())) {
				List<Nation> nations = new ArrayList<>();
				for (Nation nation : TownyUniverse.getInstance().getNations())
					nations.add(nation);
				new War(plugin,TownySettings.getWarTimeWarningDelay(), nations, null, null, WarType.WORLDWAR);
				TownyMessaging.sendMsg(getSender(), Translation.of("msg_war_started"));
			} else {
				if (split.length == 1) {
					player.sendMessage(ChatTools.formatTitle("/townyadmin toggle war "));
					player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle war", "{warname}", ""));
					return;
				}
				endWar(StringMgmt.remFirstArg(split));
				TownyMessaging.sendMsg(getSender(), Translation.of("msg_war_ended"));
			}	

		} else if (split[0].equalsIgnoreCase("peaceful") || split[0].equalsIgnoreCase("neutral")) {

			try {
				TownySettings.setDeclaringNeutral(choice.orElse(!TownySettings.isDeclaringNeutral()));
				TownyMessaging.sendMsg(getSender(), Translation.of("msg_nation_allow_peaceful", TownySettings.isDeclaringNeutral() ? Translation.of("enabled") : Translation.of("disabled")));

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_choice"));
			}

		} else if (split[0].equalsIgnoreCase("devmode")) {
			try {
				TownySettings.setDevMode(choice.orElse(!TownySettings.isDevMode()));
				TownyMessaging.sendMsg(getSender(), "Dev Mode " + (TownySettings.isDevMode() ? Colors.Green + Translation.of("enabled") : Colors.Red + Translation.of("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("debug")) {
			try {
				TownySettings.setDebug(choice.orElse(!TownySettings.getDebug()));
				TownyLogger.getInstance().refreshDebugLogger();
				TownyMessaging.sendMsg(getSender(), "Debug Mode " + (TownySettings.getDebug() ? Colors.Green + Translation.of("enabled") : Colors.Red + Translation.of("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("townwithdraw")) {
			try {
				TownySettings.SetTownBankAllowWithdrawls(choice.orElse(!TownySettings.getTownBankAllowWithdrawls()));
				TownyMessaging.sendMsg(getSender(), "Town Withdrawls " + (TownySettings.getTownBankAllowWithdrawls() ? Colors.Green + Translation.of("enabled") : Colors.Red + Translation.of("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("nationwithdraw")) {
			try {
				TownySettings.SetNationBankAllowWithdrawls(choice.orElse(!TownySettings.getNationBankAllowWithdrawls()));
				TownyMessaging.sendMsg(getSender(), "Nation Withdrawls " + (TownySettings.getNationBankAllowWithdrawls() ? Colors.Green + Translation.of("enabled") : Colors.Red + Translation.of("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_choice"));
			}
			
		} else if (split[0].equalsIgnoreCase("npc")) {
			
			if (split.length != 2)
				throw new TownyException(Translation.of("msg_err_invalid_input", "Eg: toggle npc [resident]"));
			
			Resident resident = townyUniverse.getResident(split[1]);
			
			if (resident == null) {
				throw new TownyException(Translation.of("msg_err_not_registered_1", split[1]));
			}

			resident.setNPC(!resident.isNPC());

			resident.save();

			TownyMessaging.sendMessage(sender, Translation.of("msg_npc_flag", resident.isNPC(), resident.getName()));
		} else {
			// parameter error message
			// peaceful/war/townmobs/worldmobs
			TownyMessaging.sendErrorMsg(getSender(), Translation.of("msg_err_invalid_choice"));
		}
	}

	private void endWar(String[] args) {
		
		String warName = StringMgmt.join(args);	
		
		War war = TownyUniverse.getInstance().getWarEvent(warName);
		if (war != null) {
			war.end(true);
			TownyMessaging.sendMsg(getSender(), Translation.of("msg_war_ended")); // TODO: New Language String.
		}
	}

	public static void handleTownMetaCommand(Player player, Town town, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_META.getNode()))
			throw new TownyException(Translation.of("msg_err_command_disable"));

		if (split.length == 2) {
			if (town.hasMeta()) {
				player.sendMessage(ChatTools.formatTitle("Custom Meta Data"));
				for (CustomDataField<?> field : town.getMetadata()) {
					player.sendMessage(field.getKey() + " = " + field.getValue());
				}
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_this_town_doesnt_have_any_associated_metadata"));
			}

			return;
		}

		if (split.length < 4) {
			player.sendMessage(ChatTools.formatTitle("/townyadmin town {townname} meta"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "set", "The key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "add", "Add a key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "remove", "Remove a key from the town"));
			return;
		}
		
		final String mdKey = split[3];
		
		if (split[2].equalsIgnoreCase("set")) {
			String val = split.length == 5 ? split[4] : null;
			
			if (town.hasMeta() && town.hasMeta(mdKey)) {
				CustomDataField<?> cdf = town.getMetadata(mdKey);

				// Check if the given value is valid for this field.
				try {
					if (val == null)
						throw new InvalidMetadataTypeException(cdf);

					cdf.isValidType(val);
				} catch (InvalidMetadataTypeException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
					return;
				}

				// Change state
				cdf.setValueFromString(val);

				// Let user know that it was successful.
				TownyMessaging.sendMsg(player, Translation.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

				// Save changes.
				town.save();
			}
			else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_key_x_is_not_part_of_this_town", mdKey));
			}
		} else if (split[2].equalsIgnoreCase("add")) {

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)){
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));
				return;
			}
			
			CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

			if (town.hasMeta() && town.hasMeta(md.getKey())) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_key_x_already_exists", mdKey));
				return;
			}

			TownyMessaging.sendMsg(player, Translation.of("msg_custom_data_was_successfully_added_to_town"));
			
			town.addMetaData(md.clone());
			
		} else if (split[2].equalsIgnoreCase("remove")) {

			if (town.hasMeta() && town.hasMeta(mdKey)) {
				CustomDataField<?> cdf = town.getMetadata(mdKey);
				town.removeMetaData(cdf);
				TownyMessaging.sendMsg(player, Translation.of("msg_data_successfully_deleted"));
				return;
			}
			
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_key_cannot_be_deleted"));
		}
	}
	
	public static boolean handlePlotMetaCommand(Player player, String[] split) throws TownyException {
		
		String world = player.getWorld().getName();
		TownBlock townBlock = null;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
			return false;
		}

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_META.getNode()))
			throw new TownyException(Translation.of("msg_err_command_disable"));
		
		if (split.length == 1) {
			if (townBlock.hasMeta()) {
				player.sendMessage(ChatTools.formatTitle("Custom Meta Data"));
				for (CustomDataField<?> field : townBlock.getMetadata()) {
					player.sendMessage(field.getKey() + " = " + field.getValue());
				}
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_this_plot_doesnt_have_any_associated_metadata"));
			}

			return true;
		}

		if (split.length < 3) {
			player.sendMessage(ChatTools.formatTitle("/townyadmin plot meta"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "set", "The key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "add", "Add a key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "remove", "Remove a key from the town"));
			return false;
		}

		final String mdKey = split[2];
		
		if (split[1].equalsIgnoreCase("set")) {
			String val = split.length == 4 ? split[3] : null;
			
			if (townBlock.hasMeta() && townBlock.hasMeta(mdKey)) {
				CustomDataField<?> cdf = townBlock.getMetadata(mdKey);

				// Change state
				try {
					if (val == null)
						throw new InvalidMetadataTypeException(cdf); 
							
					cdf.isValidType(val);
				} catch (InvalidMetadataTypeException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
					return false;
				}

				cdf.setValueFromString(val);

				// Let user know that it was successful.
				TownyMessaging.sendMsg(player, Translation.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

				// Save changes.
				townBlock.save();
				return true;
			}
			else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_key_x_is_not_part_of_this_plot", mdKey));
				return false;
			}
		} else if (split[1].equalsIgnoreCase("add")) {

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));
				return false;
			}

			CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
			if (townBlock.hasMeta() && townBlock.hasMeta(md.getKey())) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_key_x_already_exists", mdKey));
				return false;
			}

			TownyMessaging.sendMsg(player, Translation.of("msg_custom_data_was_successfully_added_to_townblock"));

			townBlock.addMetaData(md.clone());
			
		} else if (split[1].equalsIgnoreCase("remove")) {

			if (townBlock.hasMeta() && townBlock.hasMeta(mdKey)) {
				CustomDataField<?> cdf = townBlock.getMetadata(mdKey);
				townBlock.removeMetaData(cdf);
				TownyMessaging.sendMsg(player, Translation.of("msg_data_successfully_deleted"));
				return true;
			}

			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_key_cannot_be_deleted"));
			return false;
		}
		
		return true;
	}
	
	private void showReloadHelp() {
		sender.sendMessage(ChatTools.formatTitle("/ta reload"));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "database", "Reloads database"));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "config", "Reloads config"));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "lang", "Reloads language file."));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "perms", "Reloads Towny permissions."));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "all", "Reloads all components of towny."));
	}
	
	private void parseAdminDepositAllCommand(String[] split) {
		if (split.length == 0)
			showDepositAllHelp();
		else if (split.length > 1)
			showDepositAllHelp();
		else if (split.length == 1) {
			String reason = "townyadmin depositall";
			double amount = 0;
			try {
				amount = Double.parseDouble(split[0]);				
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_must_be_num"));
				return;
			}
			
			for (Nation nation : TownyUniverse.getInstance().getNations())
				nation.getAccount().deposit(amount, reason);
			
			for (Town town : TownyUniverse.getInstance().getTowns())
				town.getAccount().deposit(amount, reason);

			TownyMessaging.sendMsg(sender, Translation.of("msg_ta_deposit_all_success", TownyEconomyHandler.getFormattedBalance(amount)));
		}
	}
	
	private void showDepositAllHelp() {
		sender.sendMessage(ChatTools.formatTitle("/townyadmin depositall"));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin depositall", "[amount]", ""));		
	}

	private void parseAdminWarCommand(String[] split) {
		List<Nation> nations = new ArrayList<>();
		List<Town> towns = new ArrayList<>();
		
		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			showWarHelp();
		else if (split.length > 3)
			showWarHelp();
		else if (split.length == 1) {
			if (split[0].equalsIgnoreCase("worldwar")) {
				for (Nation nation : TownyUniverse.getInstance().getNations())
					nations.add(nation);
				new War(plugin,TownySettings.getWarTimeWarningDelay(), nations, null, null, WarType.WORLDWAR);
				return;
			} else if (split[0].equalsIgnoreCase("list")) {
				sender.sendMessage(ChatTools.formatTitle("Ongoing Wars"));
				if (TownyUniverse.getInstance().getWarNames().isEmpty()) {
					sender.sendMessage(ChatTools.formatCommand("None", "", ""));
					return;
				}
				for (War war : TownyUniverse.getInstance().getWars()) {
					sender.sendMessage(ChatTools.formatCommand("War Name: " + war.getWarName(), "Type: " + war.getWarType().getName(), ""));
				}
			} else {
				showWarHelp();
			}
		} else if (split.length == 2) {
			if (split[0].equalsIgnoreCase("riot")) {
				Town town = null;
				try {
					town = TownyUniverse.getInstance().getDataSource().getTown(split[1]);
					towns.add(town);
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
				}
				new War(plugin, 20, null, towns, null, WarType.RIOT);
				
			} else if (split[0].equalsIgnoreCase("civilwar")) {
				Nation nation = null;
				try {
					nation = TownyUniverse.getInstance().getDataSource().getNation(split[1]);
					nations.add(nation);
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
				}
				new War(plugin, 20, nations, null, null, WarType.CIVILWAR);
			} else {
				showWarHelp();
				return;
			}
		} else if (split.length == 3) {
			if (split[0].equalsIgnoreCase("townwar")) {
				Town town = null;
				try {
					town = TownyUniverse.getInstance().getDataSource().getTown(split[1]);
					towns.add(town);
					town = TownyUniverse.getInstance().getDataSource().getTown(split[2]);
					towns.add(town);
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
				}
				new War(plugin, 20, null, towns, null, WarType.TOWNWAR);
			} else if (split[0].equalsIgnoreCase("nationwar")) {
				Nation nation = null;

				try {
					nation = TownyUniverse.getInstance().getDataSource().getNation(split[1]);
					nations.add(nation);
					nation = TownyUniverse.getInstance().getDataSource().getNation(split[2]);
					nations.add(nation);
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
				}
				new War(plugin, 20, nations, null, null, WarType.NATIONWAR);
			} else {
				showWarHelp();
				return;
			}
		}
	}

	private void showWarHelp() {
		sender.sendMessage(ChatTools.formatTitle("/townyadmin war"));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "RIOT [town]", ""));		
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "TOWNWAR [town] [town]", ""));		
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "CIVILWAR [nation]", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "NATIONWAR [nation] [nation]", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "WORLDWAR", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "list", ""));
	}

}
