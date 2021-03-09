package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownySpigotMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.event.NationAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationInviteTownEvent;
import com.palmergames.bukkit.towny.event.NationPreAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationPreRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.event.nation.NationTownLeaveEvent;
import com.palmergames.bukkit.towny.event.NationRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.NationRequestAllyNationEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.event.nation.NationMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownKickEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.nation.PreNewNationEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleUnknownEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleOpenEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationTogglePublicEvent;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.NationRemoveAllyEvent;
import com.palmergames.bukkit.towny.event.NationDenyAllyRequestEvent;
import com.palmergames.bukkit.towny.event.NationAcceptAllyRequestEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import com.palmergames.bukkit.towny.object.inviteobjects.TownJoinNationInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


public class NationCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> nationTabCompletes = Arrays.asList(
		"list",
		"online",
		"leave",
		"withdraw",
		"deposit",
		"new",
		"add",
		"kick",
		"delete",
		"enemy",
		"rank",
		"say",
		"set",
		"toggle",
		"join",
		"merge",
		"townlist",
		"allylist",
		"enemylist",
		"ally",
		"spawn",
		"king",
		"bankhistory"
	);

	private static final List<String> nationSetTabCompletes = Arrays.asList(
		"king",
		"capital",
		"board",
		"taxes",
		"name",
		"spawn",
		"spawncost",
		"title",
		"surname",
		"tag",
		"mapcolor"
	);
	
	private static final List<String> nationListTabCompletes = Arrays.asList(
		"residents",
		"balance",
		"name",		
		"online",
		"open",
		"public",
		"townblocks",
		"towns"
	);
	
	static final List<String> nationToggleTabCompletes = Arrays.asList(
		"neutral",
		"peaceful",
		"public",
		"open"
	);
	
	private static final List<String> nationEnemyTabCompletes = Arrays.asList(
		"add",
		"remove"
	);
	
	private static final List<String> nationAllyTabCompletes = Arrays.asList(
		"add",
		"remove",
		"sent",
		"received",
		"accept",
		"deny"
	);

	private static final List<String> nationKingTabCompletes = Collections.singletonList("?");
	
	private static final List<String> nationConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;

			switch (args[0].toLowerCase()) {
				case "toggle":
					if (args.length == 2)
						return NameUtil.filterByStart(nationToggleTabCompletes, args[1]);
					else if (args.length == 3)
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
					break;
				case "king":
					if (args.length == 2)
						return NameUtil.filterByStart(nationKingTabCompletes, args[1]);
					break;
				case "townlist":
				case "allylist":
				case "enemylist":
				case "online":
				case "join":
				case "delete":
				case "merge":
					if (args.length == 2)
						return getTownyStartingWith(args[1], "n");
					break;
				case "spawn":
					if (args.length == 2) {
						List<String> nationOrIgnore = getTownyStartingWith(args[1], "n");
						nationOrIgnore.add("-ignore");
						return NameUtil.filterByStart(nationOrIgnore, args[1]);
					}
					if (args.length == 3) {
						List<String> ignore = Collections.singletonList("-ignore");
						return ignore;
					}
				case "add":
					return getTownyStartingWith(args[args.length - 1], "t");
				case "kick":
					try {
						Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
						if (res != null)
							return NameUtil.filterByStart(NameUtil.getNames(res.getTown().getNation().getTowns()), args[args.length - 1]);
					} catch (TownyException ignored) {}
				case "ally":
					if (args.length == 2) {
						return NameUtil.filterByStart(nationAllyTabCompletes, args[1]);
					} else if (args.length > 2){
						switch (args[1].toLowerCase()) {
							case "add":
								if (args[args.length - 1].startsWith("-")) {
									// Return only sent invites to revoked because the nation name starts with a hyphen, e.g. -exampleNationName
									try {
										return NameUtil.filterByStart(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getSentAllyInvites()
											// Get names of sent invites
											.stream()
											.map(Invite::getReceiver)
											.map(InviteReceiver::getName)
											// Collect sent invite names and check with the last arg without the hyphen
											.collect(Collectors.toList()), args[args.length - 1].substring(1))
											// Add the hyphen back to the beginning
											.stream()
											.map(e -> "-" + e)
											.collect(Collectors.toList());
									} catch (TownyException ignored) {}
								} else {
									// Otherwise return possible nations to send invites to
									return getTownyStartingWith(args[args.length - 1], "n");
								}
							case "remove":
								// Return current allies to remove
								try {
									return NameUtil.filterByStart(NameUtil.getNames(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getAllies()), args[args.length - 1]);
								} catch (TownyException ignore) {}
							case "accept":
							case "deny":
								// Return sent ally invites to accept or deny
								try {
									return NameUtil.filterByStart(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getReceivedInvites()
										.stream()
										.map(Invite::getSender)
										.map(InviteSender::getName)
										.collect(Collectors.toList()), args[args.length - 1]);
								} catch (TownyException ignore) {}
						}
					}
					break;
				case "rank":
					if (args.length == 2) {
						return NameUtil.filterByStart(nationEnemyTabCompletes, args[1]);
					} else if (args.length > 2){
						switch (args[1].toLowerCase()) {
							case "add":
							case "remove":
								if (args.length == 3) {
									try {
										return NameUtil.filterByStart(NameUtil.getNames(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getResidents()), args[2]);
									} catch (NotRegisteredException e) {
										return Collections.emptyList();
									}
								} else if (args.length == 4) {
									return NameUtil.filterByStart(TownyPerms.getNationRanks(), args[3]);
								}
						}
					}
					break;
				case "enemy":
					if (args.length == 2) {
						return NameUtil.filterByStart(nationEnemyTabCompletes, args[1]);
					} else if (args.length == 3){
						switch (args[1].toLowerCase()) {
							case "add":
								return getTownyStartingWith(args[2], "n");
							case "remove":
								// Return enemies of nation
								try {
									return NameUtil.filterByStart(NameUtil.getNames(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getEnemies()), args[2]);
								} catch (TownyException ignored) {}
						}
					}
					break;
				case "set":
					try {
						return nationSetTabComplete(getResidentOrThrow(player.getUniqueId()).getTown().getNation(), args);
					} catch (NotRegisteredException e) {
						return Collections.emptyList();
					}
				case "list":
					switch (args.length) {
						case 2:
							return Collections.singletonList("by");
						case 3:
							return NameUtil.filterByStart(nationListTabCompletes, args[2]);
					}
				default:
					if (args.length == 1) {
						List<String> nationNames = NameUtil.filterByStart(nationTabCompletes, args[0]);
						if (nationNames.size() > 0) {
							return nationNames;
						} else {
							return getTownyStartingWith(args[0], "n");
						}
					}
			}
		} else if (args.length == 1) {
			return filterByStartOrGetTownyStartingWith(nationConsoleTabCompletes, args[0], "n");
		}

		return Collections.emptyList();
	}
	
	static List<String> nationSetTabComplete(Nation nation, String[] args) {
		if (args.length == 2) {
			return NameUtil.filterByStart(nationSetTabCompletes, args[1]);
		} else if (args.length == 3){
			switch (args[1].toLowerCase()) {
				case "king":
				case "title":
				case "surname":
					return NameUtil.filterByStart(NameUtil.getNames(nation.getResidents()), args[2]);
				case "capital":
					return NameUtil.filterByStart(NameUtil.getNames(nation.getTowns()), args[2]);
				case "tag":
					if (args.length == 3)
						return NameUtil.filterByStart(Collections.singletonList("clear"), args[2]);
			}
		}
		
		return Collections.emptyList();
	}

	public NationCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			if (plugin.isError()) {
				sender.sendMessage(Colors.Rose + "[Towny Error] Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			if (args == null) {
				HelpMenu.NATION_HELP.send(player);
				parseNationCommand(player, args);
			} else {
				parseNationCommand(player, args);
			}

		} else
			parseNationCommandForConsole(sender, args);

		return true;
	}

	private static Nation getNationOrThrow(String nationName) throws NotRegisteredException {
		Nation nation = TownyUniverse.getInstance().getNation(nationName);

		if (nation == null)
			throw new NotRegisteredException(Translation.of("msg_err_not_registered_1", nationName));

		return nation;
	}
	
	private void parseNationCommandForConsole(final CommandSender sender, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			HelpMenu.NATION_HELP_CONSOLE.send(sender);

		} else if (split[0].equalsIgnoreCase("list")) {

			try {
				listNations(sender, split);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}

		} else {
			final Nation nation = TownyUniverse.getInstance().getNation(split[0]);

			if (nation != null) {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(nation)));
			}
			else {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_not_registered_1", split[0]));
			}
		}


	}

	public void parseNationCommand(final Player player, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		try {

			if (split.length == 0)
				Bukkit.getScheduler().runTaskAsynchronously(Towny.getPlugin(), () -> {
					Resident resident =TownyUniverse.getInstance().getResident(player.getUniqueId());
					
					if (resident == null || !resident.hasNation()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_dont_belong_nation"));
						return;
					}
					
					try {
						Town town = resident.getTown();
						Nation nation = town.getNation();
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation));
					} catch (NotRegisteredException ignore) {
					}
				});

			else if (split[0].equalsIgnoreCase("?"))
					HelpMenu.NATION_HELP.send(player);
			else if (split[0].equalsIgnoreCase("list")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				listNations(player, split);
				
			} else if (split[0].equalsIgnoreCase("townlist")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOWNLIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Nation nation = null;
				try {
					if (split.length == 1) {
						nation = getResidentOrThrow(player.getUniqueId()).getTown().getNation();
					} else {
						nation = getNationOrThrow(split[1]);
					}
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
					return;
				}
				TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getFormattedName()));
				TownyMessaging.sendMessage(player, ChatTools.listArr(TownyFormatter.getFormattedNames(nation.getTowns().toArray(new Town[0])), Translation.of("status_nation_towns", nation.getTowns().size())));

			} else if (split[0].equalsIgnoreCase("allylist")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLYLIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Nation nation = null;
				try {
					if (split.length == 1) {
						nation = getResidentOrThrow(player.getUniqueId()).getTown().getNation();
					} else {
						nation = getNationOrThrow(split[1]);
					}
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
					return;
				}
				
				if (nation.getAllies().isEmpty())
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_nation_has_no_allies")); 
				else {
					TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getFormattedName()));
					TownyMessaging.sendMessage(player, ChatTools.listArr(TownyFormatter.getFormattedNames(nation.getAllies().toArray(new Nation[0])), Translation.of("status_nation_allies", nation.getAllies().size())));
				}

			} else if (split[0].equalsIgnoreCase("enemylist")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ENEMYLIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Nation nation = null;
				try {
					if (split.length == 1) {
						nation = getResidentOrThrow(player.getUniqueId()).getTown().getNation();
					} else {
						nation = getNationOrThrow(split[1]);
					}
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
					return;
				}
				if (nation.getEnemies().isEmpty())
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_nation_has_no_enemies")); 
				else {
					TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getFormattedName()));
					TownyMessaging.sendMessage(player, ChatTools.listArr(TownyFormatter.getFormattedNames(nation.getEnemies().toArray(new Nation[0])), Translation.of("status_nation_enemies", nation.getEnemies().size())));
				}

			} else if (split[0].equalsIgnoreCase("new")) {

				Resident resident = getResidentOrThrow(player.getUniqueId());

				if ((TownySettings.getNumResidentsCreateNation() > 0) && (resident.getTown().getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_enough_residents_new_nation"));
					return;
				}

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_NEW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length == 1)
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_nation_name"));
				else if (split.length >= 2) {

					if (!resident.isMayor() && !resident.getTown().hasResidentWithRank(resident, "assistant"))
						throw new TownyException(Translation.of("msg_peasant_right"));
					
					boolean noCharge = TownySettings.getNewNationPrice() == 0.0 || !TownyEconomyHandler.isActive();
					
					String[] newSplit = StringMgmt.remFirstArg(split);
					String nationName = String.join("_", newSplit);
					newNation(player, nationName, resident.getTown(), noCharge);

				}
			} else if (split[0].equalsIgnoreCase("join")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_JOIN.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				parseNationJoin(player, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("merge")) {
				
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_MERGE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (split.length == 1)
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_nation_name"));
				else if (split.length == 2) {
					Resident resident = getResidentOrThrow(player.getUniqueId());
					if (!resident.isKing())
						throw new TownyException(Translation.of("msg_err_merging_for_kings_only"));
					mergeNation(player, split[1]);
				}
				
			} else if (split[0].equalsIgnoreCase("withdraw")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_WITHDRAW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				nationTransaction(player, split, true);
				

			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LEAVE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				nationLeave(player);

			} else if(split[0].equalsIgnoreCase("spawn")){
			    /*
			        Parse standard nation spawn command.
			     */
				String[] newSplit = StringMgmt.remFirstArg(split);
				boolean ignoreWarning = false;
				
				if ((split.length > 1 && split[1].equals("-ignore")) || (split.length > 2 && split[2].equals("-ignore"))) {
					ignoreWarning = true;
				}
				
				nationSpawn(player, newSplit, ignoreWarning);
            }
			else if (split[0].equalsIgnoreCase("deposit")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				nationTransaction(player, split, false);

			}  else {
				String[] newSplit = StringMgmt.remFirstArg(split);

				if (split[0].equalsIgnoreCase("rank")) {

					/*
					 * Rank perm tests are performed in the nationrank method.
					 */
					nationRank(player, newSplit);

				} else if (split[0].equalsIgnoreCase("king")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_KING.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationKing(player, newSplit);

				} else if (split[0].equalsIgnoreCase("add")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationAdd(player, newSplit);

				} else if (split[0].equalsIgnoreCase("invite") || split[0].equalsIgnoreCase("invites")) {
						parseInviteCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_KICK.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationKick(player, newSplit);

				} else if (split[0].equalsIgnoreCase("set")) {

					/*
					 * perm test performed in method.
					 */
					nationSet(player, newSplit, false, null);

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test performed in method.
					 */
					nationToggle(player, newSplit, false, null);

				} else if (split[0].equalsIgnoreCase("ally")) {

					nationAlly(player, newSplit);

				} else if (split[0].equalsIgnoreCase("enemy")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ENEMY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationEnemy(player, newSplit);

				} else if (split[0].equalsIgnoreCase("delete")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DELETE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationDelete(player, newSplit);

				} else if (split[0].equalsIgnoreCase("online")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseNationOnlineCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("say")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SAY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					Nation nation = getResidentOrThrow(player.getUniqueId()).getTown().getNation();
					String message = StringMgmt.join(newSplit);
					TownyMessaging.sendPrefixedNationMessage(nation, message);

				} else if (split[0].equalsIgnoreCase("bankhistory")) {
					
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					int pages = 10;
					if (newSplit.length > 0)
						try {
							pages = Integer.parseInt(newSplit[0]);
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
							return;
						}

					TownyUniverse.getInstance().getResident(player.getUniqueId()).getTown().getNation().generateBankHistoryBook(player, pages);
				} else {

					final Nation nation = TownyUniverse.getInstance().getNation(split[0]);

					if (nation == null) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[0]));
						return;
					}

					try {
						Resident resident = getResidentOrThrow(player.getUniqueId());
						if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_OTHERNATION.getNode()) && ( (resident.hasTown() && resident.getTown().hasNation() && (resident.getTown().getNation() != nation) )  || !resident.hasTown() )) {
							throw new TownyException(Translation.of("msg_err_command_disable"));
						}
						Bukkit.getScheduler().runTaskAsynchronously(Towny.getPlugin(), () -> TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation)));

					} catch (NotRegisteredException ex) {
						TownyMessaging.sendErrorMsg(player, ex.getMessage());
					}
				}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	private void parseNationJoin(Player player, String[] args) throws TownyException {
		
		try {
			Resident resident;
			Town town;
			Nation nation;
			String nationName;

			if (args.length < 1)
				throw new TownyException(String.format("Usage: /nation join [nation]"));

			nationName = args[0];
			
			resident = getResidentOrThrow(player.getUniqueId());
			town = resident.getTown();
			nation = getNationOrThrow(nationName);

			// Check if town is currently in a nation.
			if (town.hasNation())
				throw new TownyException(Translation.of("msg_err_already_in_a_nation"));

			// Check if town is town is free to join.
			if (!nation.isOpen())
				throw new TownyException(Translation.of("msg_err_nation_not_open", nation.getFormattedName()));
			
			if ((TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation()))
				throw new TownyException(Translation.of("msg_err_not_enough_residents_join_nation", town.getName()));

			if (TownySettings.getMaxTownsPerNation() > 0) 
	        	if (nation.getTowns().size() >= TownySettings.getMaxTownsPerNation())
	        		throw new TownyException(Translation.of("msg_err_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));

			if (TownySettings.getNationRequiresProximity() > 0) {
				Coord capitalCoord = nation.getCapital().getHomeBlock().getCoord();
				Coord townCoord = town.getHomeBlock().getCoord();
				if (!nation.getCapital().getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
					throw new TownyException(Translation.of("msg_err_nation_homeblock_in_another_world"));
				}
				double distance;
				distance = Math.sqrt(Math.pow(capitalCoord.getX() - (double)townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - (double)townCoord.getZ(), 2));
				if (distance > TownySettings.getNationRequiresProximity()) {
					throw new TownyException(Translation.of("msg_err_town_not_close_enough_to_nation", town.getName()));
				}
			}
			
			// Check if the command is not cancelled
			NationPreAddTownEvent preEvent = new NationPreAddTownEvent(nation, town);
			Bukkit.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}
			
			List<Town> towns = new ArrayList<>();
			towns.add(town);
			nationAdd(nation, towns);

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}

		
	}

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		Resident resident = getResidentOrThrow(player.getUniqueId());
		String sent = Translation.of("nation_sent_invites")
				.replace("%a", Integer.toString(resident.getTown().getNation().getSentInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown().getNation())));

		if (newSplit.length == 0) { // (/nation invite)
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_SEE_HOME.getNode())) {
				throw new TownyException(Translation.of("msg_err_command_disable"));
			}
			HelpMenu.NATION_INVITE.send(player);
			TownyMessaging.sendMessage(player, sent);
			return;
		}
		if (newSplit.length >= 1) { // /town invite [something]
			if (newSplit[0].equalsIgnoreCase("help") || newSplit[0].equalsIgnoreCase("?")) {
				HelpMenu.NATION_INVITE.send(player);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("sent")) { //  /invite(remfirstarg) sent args[1]
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_LIST_SENT.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				List<Invite> sentinvites = resident.getTown().getNation().getSentInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException e) {
					}
				}
				InviteCommand.sendInviteList(player, sentinvites, page, true);
				player.sendMessage(sent);
				return;
			} else {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				} else {
					nationAdd(player, newSplit);
				}
				// It's none of those 4 subcommands, so it's a townname, I just expect it to be ok.
				// If it is invalid it is handled in townAdd() so, I'm good
			}
		}
	}

	private void parseNationOnlineCommand(Player player, String[] split) throws TownyException {

		if (split.length > 0) {
			Nation nation = getNationOrThrow(split[0]);
			List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, nation);
			if (onlineResidents.size() > 0 ) {
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translation.of("msg_nation_online"), nation, player));
			} else {
				TownyMessaging.sendMessage(player, Colors.White +  "0 " + Translation.of("res_list") + " " + (Translation.of("msg_nation_online") + ": " + nation));
			}
		} else {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			
			if (resident == null || !resident.hasNation()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_dont_belong_nation"));
				return;
			}
			
			try {
				Town town = resident.getTown();
				Nation nation = town.getNation();
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translation.of("msg_nation_online"), nation, player));
			} catch (NotRegisteredException ignore) {
			}
		}
	}

	public void nationRank(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			// Help output.
			player.sendMessage(ChatTools.formatTitle("/nation rank"));
			player.sendMessage(ChatTools.formatCommand("", "/nation rank", "add/remove [resident] rank", ""));

		} else {

			Resident resident, target;
			Town town = null;
			Town targetTown = null;
			String rank;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 3) {
				TownyMessaging.sendErrorMsg(player, "Eg: /nation rank add/remove [resident] [rank]");
				return;
			}

			try {
				resident = getResidentOrThrow(player.getUniqueId());
				target = getResidentOrThrow(split[1]);
				town = resident.getTown();
				targetTown = target.getTown();

				if (town.getNation() != targetTown.getNation())
					throw new TownyException("This resident is not a member of your Nation!");

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			/*
			 * Match casing to an existing rank, returns null if Nation rank doesn't exist.
			 */
			rank = TownyPerms.matchNationRank(split[2]);
			if (rank == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getNationRanks(), ", ")));
				return;
			}
			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank.toLowerCase()))) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_permission_to_give_rank"));
				return;
			}

			if (split[0].equalsIgnoreCase("add")) {

				NationRankAddEvent nationRankAddEvent = new NationRankAddEvent(town.getNation(), rank, target);
				BukkitTools.getPluginManager().callEvent(nationRankAddEvent);
				
				if (nationRankAddEvent.isCancelled()) {
					TownyMessaging.sendErrorMsg(player, nationRankAddEvent.getCancelMessage());
					return;
				}
				
				try {
					if (target.addNationRank(rank)) {
						if (BukkitTools.isOnline(target.getName())) {
							TownyMessaging.sendMsg(target, Translation.of("msg_you_have_been_given_rank", "Nation", rank));
							plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
						}
						TownyMessaging.sendMsg(player, Translation.of("msg_you_have_given_rank", "Nation", rank, target.getName()));
					} else {
						// Not in a nation or Rank doesn't exist
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_resident_not_part_of_any_town"));
						return;
					}
				} catch (AlreadyRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, Translation.of("msg_resident_already_has_rank", target.getName(), "Nation"));
					return;
				}

			} else if (split[0].equalsIgnoreCase("remove")) {

				NationRankRemoveEvent nationRankRemoveEvent = new NationRankRemoveEvent(town.getNation(), rank, target);
				BukkitTools.getPluginManager().callEvent(nationRankRemoveEvent);

				if (nationRankRemoveEvent.isCancelled()) {
					TownyMessaging.sendErrorMsg(player, nationRankRemoveEvent.getCancelMessage());
					return;
				}
				
				try {
					if (target.removeNationRank(rank)) {
						if (BukkitTools.isOnline(target.getName())) {
							TownyMessaging.sendMsg(target, Translation.of("msg_you_have_had_rank_taken", "Nation", rank));
							plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
						}
						TownyMessaging.sendMsg(player, Translation.of("msg_you_have_taken_rank_from", "Nation", rank, target.getName()));
					}
				} catch (NotRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, String.format("msg_resident_doesnt_have_rank", target.getName(), "Nation"));
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

	}

	/**
	 * Send a list of all nations in the universe to player Command: /nation
	 * list
	 *
	 * @param sender - Sender (player or console.)
	 * @param split  - Current command arguments.
	 * @throws TownyException - Thrown when player does not have permission node.
	 */
	@SuppressWarnings("unchecked")
	public void listNations(CommandSender sender, String[] split) throws TownyException {
		
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		boolean console = true;
		Player player = null;
		
		if ( split.length == 2 && split[1].equals("?")) {
			sender.sendMessage(ChatTools.formatTitle("/nation list"));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #}", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #} by residents", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #} by towns", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #} by open", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #} by balance", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #} by name", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #} by townblocks", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/nation list", "{page #} by online", ""));
			return;
		}
		
		if (sender instanceof Player) {
			console = false;
			player = (Player) sender;
		}

		/*
		 * The default comparator on /n list is by residents, test it before we start anything else.
		 */
		if (split.length < 2 && !console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_RESIDENTS.getNode()))
			throw new TownyException(Translation.of("msg_err_command_disable"));
		
		List<Nation> nationsToSort = new ArrayList<>(TownyUniverse.getInstance().getNations());
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		ComparatorType type = ComparatorType.RESIDENTS;
		int total = (int) Math.ceil(((double) nationsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) { // Is a case of someone using /n list by {comparator}
				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_multiple_comparators_nation"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("resident")) 
						split[i] = "residents";
					
					if (!console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode(split[i])))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					
					if (!nationListTabCompletes.contains(split[i].toLowerCase()))
						throw new TownyException(Translation.of("msg_error_invalid_comparator_nation"));

					type = ComparatorType.valueOf(split[i].toUpperCase());
				} else {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else { // Is a case of someone using /n list, /n list # or /n list by {comparator} #
				if (pageSet) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_too_many_pages"));
					return;
				}
				try {
					page = Integer.parseInt(split[i]);
					if (page < 0) {
						TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_negative"));
						return;
					} else if (page == 0) {
						TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_must_be_int"));
						return;
					}
					pageSet = true;
				} catch (NumberFormatException e) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_must_be_int"));
					return;
				}
			}
		}

	    if (page > total) {
	        TownyMessaging.sendErrorMsg(sender, Translation.of("LIST_ERR_NOT_ENOUGH_PAGES", total));
	        return;
	    }

	    final List<Nation> nations = nationsToSort;
		final Comparator comparator = type.getComparator();
	    final ComparatorType finalType = type;
	    final int pageNumber = page;
		try {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				nations.sort(comparator);
				sendList(sender, nations, finalType, pageNumber, total);
			});
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_comparator_failed"));
		}

	}
	
	public void sendList(CommandSender sender, List<Nation> nations, ComparatorType type, int page, int total) {
		
		if (Towny.isSpigot  && sender instanceof Player) {
			TownySpigotMessaging.sendSpigotNationList(sender, nations, type, page, total);
			return;
		}

		int iMax = Math.min(page * 10, nations.size());
		List<String> nationsordered = new ArrayList<>(10);
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Nation nation = nations.get(i);
			String slug = null;
			switch (type) {
			case BALANCE:
				slug = TownyEconomyHandler.getFormattedBalance(nation.getAccount().getCachedBalance());
				break;
			case ONLINE:
				slug = TownyAPI.getInstance().getOnlinePlayersInNation(nation).size() + "";
				break;
			case TOWNS:
				slug = nation.getTowns().size() + "";
				break;
			default:
				slug = nation.getResidents().size() + "";
				break;			
			}
			String output = Colors.Gold + StringMgmt.remUnderscore(nation.getName()) + Colors.Gray + " - " + Colors.LightBlue + "(" + slug + ")";
			nationsordered.add(output);
		}
		sender.sendMessage(
				ChatTools.formatList(
						Translation.of("nation_plu"),
						Colors.Gold + Translation.of("nation_name") + Colors.Gray + " - " + Colors.LightBlue + Translation.of(type.getName()),
						nationsordered,
						Translation.of("LIST_PAGE", page, total)
				));		
	}

	/**
	 * Create a new nation. Command: /nation new [nation] *[capital]
	 *
	 * @param player - Player creating the new nation.
	 * @param name - Nation name.
	 * @param capitalTown - Capital city town.
	 * @param noCharge - charging for creation - /ta nation new NAME CAPITAL has no charge.
	 */
	public static void newNation(Player player, String name, Town capitalTown, boolean noCharge) {

		try {
			if (capitalTown.hasNation())
				throw new TownyException(Translation.of("msg_err_already_nation"));

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if ((filteredName == null) || TownyUniverse.getInstance().hasNation(filteredName))
				throw new TownyException(Translation.of("msg_err_invalid_name", name));

			PreNewNationEvent preEvent = new PreNewNationEvent(capitalTown, name);
			Bukkit.getPluginManager().callEvent(preEvent);

			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}

			// If it isn't free to make a nation, send a confirmation.
			if (!noCharge && TownyEconomyHandler.isActive()) {
				// Test if they can pay.
				if (!capitalTown.getAccount().canPayFromHoldings(TownySettings.getNewNationPrice()))			
					throw new TownyException(Translation.of("msg_no_funds_new_nation2", TownySettings.getNewNationPrice()));

				Confirmation.runOnAccept(() -> {				
					// Town pays for nation here.
					if (!capitalTown.getAccount().withdraw(TownySettings.getNewNationPrice(), "New Nation Cost")) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_funds_new_nation2", TownySettings.getNewNationPrice()));
						return;
					}
					try {
						// Actually make nation.
						newNation(name, capitalTown);
					} catch (AlreadyRegisteredException | NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
					TownyMessaging.sendGlobalMessage(Translation.of("msg_new_nation", player.getName(), StringMgmt.remUnderscore(name)));

				})
					.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNewNationPrice())))
					.sendTo(player);
				
			// Or, it is free, so just make the nation.
			} else {
				newNation(name, capitalTown);
				TownyMessaging.sendGlobalMessage(Translation.of("msg_new_nation", player.getName(), StringMgmt.remUnderscore(name)));
			}
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	public static Nation newNation(String name, Town town) throws AlreadyRegisteredException, NotRegisteredException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		UUID nationUUID = UUID.randomUUID();
		townyUniverse.getDataSource().newNation(name, nationUUID);
		Nation nation = townyUniverse.getNation(nationUUID);
		
		// Should never happen
		if (nation == null) {
			TownyMessaging.sendErrorMsg(String.format("Error fetching new nation with name %s; it was not properly registered!", name));
			throw new NotRegisteredException(Translation.of("msg_err_not_registered_1", name));
		}
		nation.setRegistered(System.currentTimeMillis());
		nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());
		town.setNation(nation);
		nation.setCapital(town);
		if (TownyEconomyHandler.isActive()) {
			try {
				nation.getAccount().setBalance(0, "New Nation Account");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
		}
		town.save();
		nation.save();

		BukkitTools.getPluginManager().callEvent(new NewNationEvent(nation));

		return nation;
	}

	public void mergeNation(Player player, String name) throws TownyException {
		
		Nation nation = TownyUniverse.getInstance().getNation(name);
		Nation remainingNation;
		
		try {
			remainingNation = getResidentOrThrow(player.getUniqueId()).getTown().getNation();
		} catch (NotRegisteredException e) {
			throw new TownyException(Translation.of("msg_err_invalid_name", name));
		}
		if (nation == null || remainingNation.getName().equalsIgnoreCase(name))
			throw new TownyException(Translation.of("msg_err_invalid_name", name));

		Resident king = nation.getKing();
		if (!BukkitTools.isOnline(king.getName())) {
			throw new TownyException(Translation.of("msg_err_king_of_that_nation_is_not_online", name, king.getName()));
		}

		TownyMessaging.sendMessage(BukkitTools.getPlayer(king.getName()), Translation.of("msg_would_you_merge_your_nation_into_other_nation", nation, remainingNation, remainingNation));
		if (TownySettings.getNationRequiresProximity() > 0) {
			List<Town> towns = nation.getTowns();
			towns.addAll(remainingNation.getTowns());
			List<Town> removedTowns = remainingNation.recheckTownDistanceDryRun(towns, remainingNation.getCapital());
			if (!removedTowns.isEmpty()) {
				TownyMessaging.sendMessage(nation.getKing(), Translation.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")));
				TownyMessaging.sendMessage(remainingNation.getKing(), Translation.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")));
			}
		}
		Confirmation.runOnAccept(() -> {
			NationPreMergeEvent preEvent = new NationPreMergeEvent(nation, remainingNation);
			Bukkit.getPluginManager().callEvent(preEvent);

			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(nation, preEvent.getCancelMessage());
				return;
			}

			try {
				BukkitTools.getPluginManager().callEvent(new NationMergeEvent(nation, remainingNation));
				TownyUniverse.getInstance().getDataSource().mergeNation(nation, remainingNation);
				TownyMessaging.sendGlobalMessage(Translation.of("nation1_has_merged_with_nation2", nation, remainingNation));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
		})
			.sendTo(BukkitTools.getPlayerExact(king.getName()));
	}
	
	public void nationLeave(Player player) {
		Town town = null;
		try {
			Resident resident = getResidentOrThrow(player.getUniqueId());
			town = resident.getTown();

			NationPreTownLeaveEvent event = new NationPreTownLeaveEvent(town.getNation(), town);
			Bukkit.getPluginManager().callEvent(event);
			
			if (event.isCancelled())
				throw new TownyException(event.getCancelMessage());

			final Town finalTown = town;
			final Nation nation = town.getNation();
			Confirmation.runOnAccept(() -> {
				Bukkit.getPluginManager().callEvent(new NationTownLeaveEvent(nation, finalTown));
				finalTown.removeNation();

				plugin.resetCache();

				TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_town_left", StringMgmt.remUnderscore(finalTown.getName())));
				TownyMessaging.sendPrefixedTownMessage(finalTown, Translation.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));

			}).sendTo(player);
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
	}

	public void nationDelete(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0)
			try {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				Nation nation = resident.getTown().getNation();
				Confirmation.runOnAccept(() -> {
					TownyUniverse.getInstance().getDataSource().removeNation(nation);
					TownyMessaging.sendGlobalMessage(Translation.of("MSG_DEL_NATION", nation.getName()));
				})
				.sendTo(player);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		else
			try {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE.getNode()))
					throw new TownyException(Translation.of("msg_err_admin_only_delete_nation"));

				Nation nation = getNationOrThrow(split[0]);
				townyUniverse.getDataSource().removeNation(nation);
				TownyMessaging.sendGlobalMessage(Translation.of("MSG_DEL_NATION", nation.getName()));
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
	}

	public void nationKing(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			HelpMenu.KING_HELP.send(player);
	}

	/**
	 * First stage of adding towns to a nation.
	 * 
	 * Tests here are performed to make sure Nation is allowed to add the towns:
	 * - make sure the nation hasn't already hit the max towns (if that is required in teh config.)
	 * 
	 * @param player - Player using the command.
	 * @param names - Names that will be matched to towns.
	 * @throws TownyException generic
	 */
	public void nationAdd(Player player, String[] names) throws TownyException {
		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation add [names]");
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = getResidentOrThrow(player.getUniqueId());
			nation = resident.getTown().getNation();

			if (TownySettings.getMaxTownsPerNation() > 0) {
	        	if (nation.getTowns().size() >= TownySettings.getMaxTownsPerNation()){
	        	TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));
	        	return;
	        	}	
	        }

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		List<String> townlist = new ArrayList<>(Arrays.asList(names));
		// Our Arraylist is above
		List<String> newtownlist = new ArrayList<>();
		// The list of valid invites is above, there are currently none
		List<String> removeinvites = new ArrayList<>();
		// List of invites to be removed;
		for (String townname : townlist) {
			if (townname.startsWith("-")) {
				// Add to removing them, remove the "-"
				removeinvites.add(townname.substring(1));
			} else {
				if (!nation.hasTown(townname))
					newtownlist.add(townname); // add to adding them,
				else 
					removeinvites.add(townname);
			}
		}
		names = newtownlist.toArray(new String[0]);
		String[] namestoremove = removeinvites.toArray(new String[0]);
		if (namestoremove.length >= 1) {
			nationRevokeInviteTown(player,nation, dataSource.getTowns(namestoremove));
		}

		if (names.length >= 1) {
			nationAdd(player, nation, dataSource.getTowns(names));
		}
	}

	private static void nationRevokeInviteTown(Object sender,Nation nation, List<Town> towns) {

		for (Town town : towns) {
			if (InviteHandler.inviteIsActive(nation, town)) {
				for (Invite invite : town.getReceivedInvites()) {
					if (invite.getSender().equals(nation)) {
						try {
							InviteHandler.declineInvite(invite, true);
							TownyMessaging.sendMessage(sender, Translation.of("nation_revoke_invite_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * Second stage of adding towns to a nation.
	 * 
	 * Tests here are performed to make sure the Towns are allowed to join the Nation:
	 * - make sure the town has enough residents to join a nation (if it is required in the config.)
	 * - make sure the town is close enough to the nation capital (if it is required in the config.)
	 * 
	 * Lastly, invites are sent and if successful, the third stage is called by the invite handler.
	 * 
	 * @param player player sending the request
	 * @param nation Nation sending the request
	 * @param invited the Town(s) being invited to the Nation
	 * @throws TownyException executed when the arraylist (invited) returns empty (no valid town was entered)
	 */
	public static void nationAdd(Player player, Nation nation, List<Town> invited) throws TownyException {

		ArrayList<Town> remove = new ArrayList<>();
		for (Town town : invited) {
			try {
				
		        if ((TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
		        	remove.add(town);
		        	TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_enough_residents_join_nation", town.getName()));
		        	continue;
		        }
		        
				if (TownySettings.getNationRequiresProximity() > 0) {
					Coord capitalCoord = nation.getCapital().getHomeBlock().getCoord();
					Coord townCoord = town.getHomeBlock().getCoord();
					if (!nation.getCapital().getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
						remove.add(town);
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_homeblock_in_another_world"));
						continue;
					}
					
					double distance;
					distance = Math.sqrt(Math.pow(capitalCoord.getX() - (double)townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - (double)townCoord.getZ(), 2));
					if (distance > TownySettings.getNationRequiresProximity()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_town_not_close_enough_to_nation", town.getName()));
						remove.add(town);
						continue;
					}
				}
				
				// Check if the command is not cancelled
				NationPreAddTownEvent preEvent = new NationPreAddTownEvent(nation, town);
				Bukkit.getPluginManager().callEvent(preEvent);
				
				if (preEvent.isCancelled()) {
					TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
					return;
				}
				
				nationInviteTown(player, nation, town);
			} catch (AlreadyRegisteredException e) {
				remove.add(town);
			}
		}

		for (Town town : remove) {
			invited.remove(town);
		}

		if (invited.size() > 0) {
			StringBuilder msg = new StringBuilder();

			for (Town town : invited) {
				msg.append(town.getName()).append(", ");
			}

			msg = new StringBuilder(msg.substring(0, msg.length() - 2));
			msg = new StringBuilder(Translation.of("msg_invited_join_nation", player.getName(), msg.toString()));
			TownyMessaging.sendPrefixedNationMessage(nation, msg.toString());
		} else {
			// This is executed when the arraylist returns empty (no valid town was entered).
			throw new TownyException(Translation.of("msg_invalid_name"));
		}
	}

	private static void nationInviteTown(Player player, Nation nation, Town town) throws TownyException {

		TownJoinNationInvite invite = new TownJoinNationInvite(player, town, nation);
		try {
			if (!InviteHandler.inviteIsActive(invite)) { 
				town.newReceivedInvite(invite);
				nation.newSentInvite(invite);
				InviteHandler.addInvite(invite); 
				Player mayor = TownyAPI.getInstance().getPlayer(town.getMayor());
				if (mayor != null)
					TownyMessaging.sendRequestMessage(mayor,invite);
				Bukkit.getPluginManager().callEvent(new NationInviteTownEvent(invite));
			} else {
				throw new TownyException(Translation.of("msg_err_town_already_invited", town.getName()));
			}
		} catch (TooManyInvitesException e) {
			town.deleteReceivedInvite(invite);
			nation.deleteSentInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	/**
	 * Final stage of adding towns to a nation.
	 * @param nation - Nation being added to.
	 * @param towns - List of Town(s) being added to Nation.
	 * @throws AlreadyRegisteredException - Shouldn't happen but could.
	 */
	public static void nationAdd(Nation nation, List<Town> towns) throws AlreadyRegisteredException {
		for (Town town : towns) {
			if (!town.hasNation()) {
				town.setNation(nation);
				town.save();
				TownyMessaging.sendNationMessagePrefixed(nation, Translation.of("msg_join_nation", town.getName()));
			}

		}
		plugin.resetCache();
		nation.save();

	}

	public void nationKick(Player player, String[] names) {

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation kick [names]");
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = getResidentOrThrow(player.getUniqueId());
			nation = resident.getTown().getNation();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		nationKick(player, nation, TownyUniverse.getInstance().getDataSource().getTowns(names));
	}

	public static void nationKick(CommandSender sender, Nation nation, List<Town> kicking) {

		ArrayList<Town> remove = new ArrayList<>();
		for (Town town : kicking)
			if (town.isCapital() || !nation.hasTown(town))
				remove.add(town);
			else {
				// Fire cancellable event.
				NationPreTownKickEvent event = new NationPreTownKickEvent(nation, town);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(sender, event.getCancelMessage());
					remove.add(town);
					continue;
				}
				
				// Actually remove the nation off the Town.
				town.removeNation();
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_nation_kicked_by", sender.getName()));
			}

		for (Town town : remove)
			kicking.remove(town);

		if (kicking.size() > 0) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_kicked", sender.getName(), StringMgmt.join(kicking, ", ")));
			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
	}

	private void nationAlly(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyPermissionSource permSource = townyUniverse.getPermissionSource();
		if (split.length <= 0) {
			HelpMenu.ALLIES_STRING.send(player);
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = getResidentOrThrow(player.getUniqueId());
			nation = resident.getTown().getNation();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		ArrayList<Nation> list = new ArrayList<>();
		ArrayList<Nation> remlist = new ArrayList<>();
		Nation ally;

		String[] names = StringMgmt.remFirstArg(split);
		if (split[0].equalsIgnoreCase("add")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ADD.getNode())) {
				throw new TownyException(Translation.of("msg_err_command_disable"));
			}
			for (String name : names) {
				ally = townyUniverse.getNation(name);
				if (ally != null) {
					if (nation.equals(ally)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
					} else if (nation.isAlliedWith(ally)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_already_ally", ally));
					} else {
						list.add(ally);
					}
				}
				// So "-Name" isn't a nation, remove the - check if that is a town.
				else {
					if (name.startsWith("-") && TownySettings.isDisallowOneWayAlliance()) {
						ally = townyUniverse.getNation(name.substring(1));

						if (ally != null) {
							if (nation.equals(ally)) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
							} else {
								remlist.add(ally);
							}
						} else {
							// Do nothing here as it doesn't match a Nation
							// Well we don't want to send the commands again so just say invalid name
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_name", name));
						}
					} else {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_name", name));
					}
				}
			}
			if (!list.isEmpty()) {
				if (TownySettings.isDisallowOneWayAlliance()) {
					nationAlly(resident,nation,list,true);
				} else {
					nationlegacyAlly(resident, nation, list, true);
				}
			}
			if (!remlist.isEmpty()) {
				nationRemoveAllyRequest(player,nation, remlist);
			}
			return;
		}
		if (split[0].equalsIgnoreCase("remove")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_REMOVE.getNode())) {
				throw new TownyException(Translation.of("msg_err_command_disable"));
			}
			for (String name : names) {
				ally = townyUniverse.getNation(name);
				if (ally != null) {
					if (nation.equals(ally)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
						return;
					} else {
						list.add(ally);
					}
				}
			}
			if (!list.isEmpty()) {
				if (TownySettings.isDisallowOneWayAlliance()) {
					nationAlly(resident,nation,list,false);
				} else {
					nationlegacyAlly(resident, nation, list, false);
				}
			}
			return;
		} else {
			if (!TownySettings.isDisallowOneWayAlliance()){
				HelpMenu.ALLIES_STRING.send(player);
				return;
			}
		}
		if (TownySettings.isDisallowOneWayAlliance()) {
			String received = Translation.of("nation_received_requests")
					.replace("%a", Integer.toString(resident.getTown().getNation().getReceivedInvites().size())
					)
					.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident.getTown().getNation())));
			String sent = Translation.of("nation_sent_ally_requests")
					.replace("%a", Integer.toString(resident.getTown().getNation().getSentAllyInvites().size())
					)
					.replace("%m", Integer.toString(InviteHandler.getSentAllyRequestsMaxAmount(resident.getTown().getNation())));
			if (split[0].equalsIgnoreCase("sent")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_SENT.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				List<Invite> sentinvites = resident.getTown().getNation().getSentAllyInvites();
				int page = 1;
				if (split.length >= 2) {
					try {
						page = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
					}
				}
				InviteCommand.sendInviteList(player, sentinvites, page, true);
				player.sendMessage(sent);
				return;
			}
			if (split[0].equalsIgnoreCase("received")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_RECEIVED.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				List<Invite> receivedinvites = resident.getTown().getNation().getReceivedInvites();
				int page = 1;
				if (split.length >= 2) {
					try {
						page = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
					}
				}
				InviteCommand.sendInviteList(player, receivedinvites, page, false);
				player.sendMessage(received);
				return;

			}
			if (split[0].equalsIgnoreCase("accept")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ACCEPT.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				Nation sendernation;
				List<Invite> invites = nation.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_no_requests"));
					return;
				}
				if (split.length >= 2) { // /invite deny args[1]
					sendernation = townyUniverse.getNation(split[1]);

					if (sendernation == null) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_specify_invite"));
					InviteCommand.sendInviteList(player, invites,1,false);
					return;
				}
				
				Invite toAccept = null;

				for (Invite invite : InviteHandler.getActiveInvites()) {
					if (invite.getSender().equals(sendernation) && invite.getReceiver().equals(nation)) {
						toAccept = invite;
						break;
					}
				}
				if (toAccept != null) {
					try {
						NationAcceptAllyRequestEvent acceptAllyRequestEvent = new NationAcceptAllyRequestEvent((Nation)toAccept.getSender(), (Nation) toAccept.getReceiver());
						Bukkit.getPluginManager().callEvent(acceptAllyRequestEvent);
						if (acceptAllyRequestEvent.isCancelled()) {
							toAccept.getReceiver().deleteReceivedInvite(toAccept);
							toAccept.getSender().deleteSentInvite(toAccept);
							TownyMessaging.sendErrorMsg(player, acceptAllyRequestEvent.getCancelMessage());
							return;
						}
						InviteHandler.acceptInvite(toAccept);
						return;
					} catch (InvalidObjectException e) {
						e.printStackTrace(); // Shouldn't happen, however like i said a fallback
					}
				}

			}
			if (split[0].equalsIgnoreCase("deny")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_DENY.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				Nation sendernation;
				List<Invite> invites = nation.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_no_requests"));
					return;
				}
				if (split.length >= 2) { // /invite deny args[1]
					sendernation = townyUniverse.getNation(split[1]);

					if (sendernation == null) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}

				Invite toDecline = null;
				
				for (Invite invite : InviteHandler.getActiveInvites()) {
					if (invite.getSender().equals(sendernation) && invite.getReceiver().equals(nation)) {
						toDecline = invite;
						break;
					}
				}
				if (toDecline != null) {
					try {
						NationDenyAllyRequestEvent denyAllyRequestEvent = new NationDenyAllyRequestEvent(nation, sendernation);
						Bukkit.getPluginManager().callEvent(denyAllyRequestEvent);
						if (denyAllyRequestEvent.isCancelled()) {
							sendernation.deleteSentAllyInvite(toDecline);
							nation.deleteReceivedInvite(toDecline);
							TownyMessaging.sendErrorMsg(player, denyAllyRequestEvent.getCancelMessage());
							return;
						}
						InviteHandler.declineInvite(toDecline, false);
						TownyMessaging.sendMessage(player, Translation.of("successful_deny_request"));
					} catch (InvalidObjectException e) {
						e.printStackTrace(); // Shouldn't happen, however like i said a fallback
					}
				}
			} else {
				HelpMenu.ALLIES_STRING.send(player);
				return;
			}
		}

	}

	private void nationRemoveAllyRequest(Object sender,Nation nation, ArrayList<Nation> remlist) {
		for (Nation invited : remlist) {
			if (InviteHandler.inviteIsActive(nation, invited)) {
				for (Invite invite : invited.getReceivedInvites()) {
					if (invite.getSender().equals(nation)) {
						try {
							InviteHandler.declineInvite(invite, true);
							TownyMessaging.sendMessage(sender, Translation.of("town_revoke_invite_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private void nationCreateAllyRequest(CommandSender sender, Nation nation, Nation receiver) throws TownyException {
		NationAllyNationInvite invite = new NationAllyNationInvite(sender, receiver, nation);
		try {
			if (!InviteHandler.inviteIsActive(invite)) {
				receiver.newReceivedInvite(invite);
				nation.newSentAllyInvite(invite);
				InviteHandler.addInvite(invite);
				Player mayor = TownyAPI.getInstance().getPlayer(receiver.getCapital().getMayor());
				if (mayor != null)
					TownyMessaging.sendRequestMessage(mayor,invite);
				Bukkit.getPluginManager().callEvent(new NationRequestAllyNationEvent(invite));
			} else {
				throw new TownyException(Translation.of("msg_err_player_already_invited", receiver.getName()));
			}
		} catch (TooManyInvitesException e) {
			receiver.deleteReceivedInvite(invite);
			nation.deleteSentAllyInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	public void nationlegacyAlly(Resident resident, final Nation nation, List<Nation> allies, boolean add) {

		Player player = BukkitTools.getPlayer(resident.getName());

		ArrayList<Nation> remove = new ArrayList<>();

		for (Nation targetNation : allies)
			try {
				if (add && !nation.getAllies().contains(targetNation)) {
					if (!targetNation.hasEnemy(nation)) {
							try {
								nation.addAlly(targetNation);
							} catch (AlreadyRegisteredException e) {
								e.printStackTrace();
							}

							TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_allied_nations", resident.getName(), targetNation.getName()));
							TownyMessaging.sendPrefixedNationMessage(targetNation, Translation.of("msg_added_ally", nation.getName()));

					} else {
						// We are set as an enemy so can't allY
						remove.add(targetNation);
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_unable_ally_enemy", targetNation.getName()));
					}
				} else if (nation.getAllies().contains(targetNation)) {
					nation.removeAlly(targetNation);

					TownyMessaging.sendPrefixedNationMessage(targetNation, Translation.of("msg_removed_ally", nation.getName()));
					TownyMessaging.sendMessage(player, Translation.of("msg_ally_removed_successfully"));
					// Remove any mirrored allies settings from the target nation
					if (targetNation.hasAlly(nation))
						nationlegacyAlly(resident, targetNation, Arrays.asList(nation), false);
				}

			} catch (NotRegisteredException e) {
				remove.add(targetNation);
			}

		for (Nation newAlly : remove)
			allies.remove(newAlly);

		if (allies.size() > 0) {
			
			TownyUniverse.getInstance().getDataSource().saveNations();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));

	}

	public void nationAlly(Resident resident, final Nation nation, List<Nation> allies, boolean add) throws TownyException {
		// This is where we add /remove those invites for nations to ally other nations.
		Player player = BukkitTools.getPlayer(resident.getName());

		ArrayList<Nation> remove = new ArrayList<>();
		for (Nation targetNation : allies) {
			if (add) { // If we are adding as an ally.
				if (!targetNation.hasEnemy(nation)) {
					if (!targetNation.getCapital().getMayor().isNPC()) {
						for (Nation newAlly : allies) {
							nationCreateAllyRequest(player, nation, targetNation);
							TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_ally_req_sent", newAlly.getName()));
						}
					} else {
						if (TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode())) {
							try {
								targetNation.addAlly(nation);
								nation.addAlly(targetNation);
							} catch (AlreadyRegisteredException e) {
								e.printStackTrace();
							}
							TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_allied_nations", resident.getName(), targetNation.getName()));
							TownyMessaging.sendPrefixedNationMessage(targetNation, Translation.of("msg_added_ally", nation.getName()));
						} else
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_unable_ally_npc", nation.getName()));
					}
				}
			} else { // So we are removing an ally
				if (nation.getAllies().contains(targetNation)) {
					try {
						NationRemoveAllyEvent removeAllyEvent = new NationRemoveAllyEvent(nation, targetNation);
						Bukkit.getPluginManager().callEvent(removeAllyEvent);
						if (removeAllyEvent.isCancelled()) {
							TownyMessaging.sendErrorMsg(player, removeAllyEvent.getCancelMessage());
							return;
						}
						nation.removeAlly(targetNation);
						TownyMessaging.sendPrefixedNationMessage(targetNation, Translation.of("msg_removed_ally", nation.getName()));
						TownyMessaging.sendMessage(player, Translation.of("msg_ally_removed_successfully"));
					} catch (NotRegisteredException e) {
						remove.add(targetNation);
					}
					// Remove any mirrored allies settings from the target nation
					// We know the linked allies are enabled so:
					if (targetNation.hasAlly(nation)) {
						try {
							targetNation.removeAlly(nation);
						} catch (NotRegisteredException e) {
							// This should genuinely not be possible since we "hasAlly it beforehand"
						}
					}
				}

			}
		}
		for (Nation newAlly : remove) {
			allies.remove(newAlly);
		}

		if (allies.size() > 0) {
			
			TownyUniverse.getInstance().getDataSource().saveNations();

			plugin.resetCache();
		} else {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
		}

	}

	public void nationEnemy(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident;
		Nation nation;

		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation enemy [add/remove] [name]");
			return;
		}

		try {
			resident = getResidentOrThrow(player.getUniqueId());
			nation = resident.getTown().getNation();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		ArrayList<Nation> list = new ArrayList<>();
		Nation enemy;
		// test add or remove
		String test = split[0];
		String[] newSplit = StringMgmt.remFirstArg(split);

		if ((test.equalsIgnoreCase("remove") || test.equalsIgnoreCase("add")) && newSplit.length > 0) {
			for (String name : newSplit) {
				enemy = townyUniverse.getNation(name);

				if (enemy == null) {
					throw new TownyException(Translation.of("msg_err_no_nation_with_that_name", name));
				}

				if (nation.equals(enemy))
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
				else
					list.add(enemy);
			}
			if (!list.isEmpty())
				nationEnemy(resident, nation, list, test.equalsIgnoreCase("add"));

		} else {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "[add/remove]"));
		}
	}

	public void nationEnemy(Resident resident, Nation nation, List<Nation> enemies, boolean add) {

		ArrayList<Nation> remove = new ArrayList<>();
		for (Nation targetNation : enemies)
			try {
				if (add && !nation.getEnemies().contains(targetNation)) {
					NationPreAddEnemyEvent npaee = new NationPreAddEnemyEvent(nation, targetNation);
					Bukkit.getPluginManager().callEvent(npaee);
					
					if (!npaee.isCancelled()) {
						nation.addEnemy(targetNation);
						
						NationAddEnemyEvent naee = new NationAddEnemyEvent(nation, targetNation);
						Bukkit.getPluginManager().callEvent(naee);
						
						TownyMessaging.sendPrefixedNationMessage(targetNation, Translation.of("msg_added_enemy", nation.getName()));
						// Remove any ally settings from the target nation
						if (targetNation.hasAlly(nation))
							nationlegacyAlly(resident, targetNation, Arrays.asList(nation), false);
						
					} else {
						TownyMessaging.sendMsg(TownyAPI.getInstance().getPlayer(resident), npaee.getCancelMessage());
						remove.add(targetNation);
					}

				} else if (nation.getEnemies().contains(targetNation)) {
					NationPreRemoveEnemyEvent npree = new NationPreRemoveEnemyEvent(nation, targetNation);
					Bukkit.getPluginManager().callEvent(npree);
					if (!npree.isCancelled()) {
						nation.removeEnemy(targetNation);

						NationRemoveEnemyEvent nree = new NationRemoveEnemyEvent(nation, targetNation);
						Bukkit.getPluginManager().callEvent(nree);
						
						TownyMessaging.sendPrefixedNationMessage(targetNation, Translation.of("msg_removed_enemy", nation.getName()));
					} else {
						TownyMessaging.sendMsg(TownyAPI.getInstance().getPlayer(resident), npree.getCancelMessage());
						remove.add(targetNation);
					}
				}

			} catch (AlreadyRegisteredException | NotRegisteredException e) {
				remove.add(targetNation);
			}
		
		for (Nation newEnemy : remove)
			enemies.remove(newEnemy);

		if (enemies.size() > 0) {
			String msg = "";

			for (Nation newEnemy : enemies)
				msg += newEnemy.getName() + ", ";

			msg = msg.substring(0, msg.length() - 2);
			if (add)
				msg = Translation.of("msg_enemy_nations", resident.getName(), msg);
			else
				msg = Translation.of("msg_enemy_to_neutral", resident.getName(), msg);

			TownyMessaging.sendPrefixedNationMessage(nation, msg);
			TownyUniverse.getInstance().getDataSource().saveNations();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(resident, Translation.of("msg_invalid_name"));
	}

	public static void nationSet(Player player, String[] split, boolean admin, Nation nation) throws TownyException, InvalidNameException, EconomyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/nation set"));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "king " + Translation.of("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "capital [town]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "name [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "title/surname [resident] [text]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "tag [upto 4 letters] or clear", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "board [message ... ]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "spawn", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "spawncost [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "mapcolor [color]", ""));
		} else {
			Resident resident;
			try {
				if (!admin) {
					resident = getResidentOrThrow(player.getUniqueId());
					nation = resident.getTown().getNation();
				} else // treat resident as king for testing purposes.
					resident = nation.getKing();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			if (split[0].equalsIgnoreCase("king")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_KING.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set king Dumbo");
				else
					try {
						Resident newKing = getResidentOrThrow(split[1]);
						Resident oldKing = nation.getKing();

			            if ((TownySettings.getNumResidentsCreateNation() > 0) && (newKing.getTown().getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			              TownyMessaging.sendMessage(player, Translation.of("msg_not_enough_residents_capital", newKing.getTown().getName()));
			              return;
			            }

						nation.setKing(newKing);
						plugin.deleteCache(oldKing.getName());
						plugin.deleteCache(newKing.getName());
						TownyPerms.assignPermissions(oldKing, player); // remove permissions from old King.
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_new_king", newKing.getName(), nation.getName()));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
			} else if (split[0].equalsIgnoreCase("capital")) {
				try {
					Town newCapital = TownyUniverse.getInstance().getTown(split[1]);
					
					if (newCapital == null) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[1]));
						return;
					}

		            if ((TownySettings.getNumResidentsCreateNation() > 0) && (newCapital.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
		              TownyMessaging.sendMessage(player, Translation.of("msg_not_enough_residents_capital", newCapital.getName()));
		              return;
		            }

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_CAPITOL.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (split.length < 2)
						TownyMessaging.sendErrorMsg(player, "Eg: /nation set capital {town name}");
					else {
						// Do proximity tests.
						if (TownySettings.getNationRequiresProximity() > 0 ) {
							List<Town> removedTowns = nation.recheckTownDistanceDryRun(nation.getTowns(), newCapital);
							
							// There are going to be some towns removed from the nation, so we'll do a Confirmation.
							if (!removedTowns.isEmpty()) {
								String title = Translation.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", "));
								final Nation finalNation = nation;
								Confirmation.runOnAccept(() -> {
									
									try {
										finalNation.setCapital(newCapital);										
										finalNation.recheckTownDistance();
										plugin.resetCache();
										TownyMessaging.sendPrefixedNationMessage(finalNation, Translation.of("msg_new_king", newCapital.getMayor().getName(), finalNation.getName()));
										
									} catch (TownyException e) {
										TownyMessaging.sendErrorMsg(player, e.getMessage());
									}
								})
								.setTitle(title)
								.sendTo(player);
								
							// No towns will be removed, skip the Confirmation.
							} else {
								nation.setCapital(newCapital);
								plugin.resetCache();
								TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
								nation.save();
							}
						// Proximity doesn't factor in.
						} else {
							nation.setCapital(newCapital);
							plugin.resetCache();
							TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
							nation.save();
						}
					}
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

			} else if (split[0].equalsIgnoreCase("spawn")){

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				try{
					nation.setSpawn(player.getLocation());
					TownyMessaging.sendMsg(player, Translation.of("msg_set_nation_spawn"));
				} catch (TownyException e){
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			}
			else if (split[0].equalsIgnoreCase("taxes")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAXES.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set taxes 70");
				else {
					int amount = Integer.parseInt(split[1].trim());
					if (amount < 0) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
						return;
					}

					try {
						nation.setTaxes(amount);
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_town_set_nation_tax", player.getName(), split[1]));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					}
				}

			} else if (split[0].equalsIgnoreCase("spawncost")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWNCOST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set spawncost 70");
				else {
					try {
						double amount = Double.parseDouble(split[1]);
						if (amount < 0) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
							return;
						}
						if (TownySettings.getSpawnTravelCost() < amount) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_cannot_set_spawn_cost_more_than", TownySettings.getSpawnTravelCost()));
							return;
						}
						nation.setSpawnCost(amount);
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_spawn_cost_set_to", player.getName(), Translation.of("nation_sing"), split[1]));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
						return;
					}
				}

			} else if (split[0].equalsIgnoreCase("name")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set name Plutoria");				
				else {
					
					if(NameValidation.isBlacklistName(split[1]))
						throw new TownyException(Translation.of("msg_invalid_name"));
					
				    if(TownyEconomyHandler.isActive() && TownySettings.getNationRenameCost() > 0) {
						if (!nation.getAccount().canPayFromHoldings(TownySettings.getNationRenameCost()))
							throw new EconomyException(Translation.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));

						final Nation finalNation = nation;
                    	final String name = split[1];
				    	Confirmation.runOnAccept(() -> {
							//Check if nation can still pay rename costs.
							if (!finalNation.getAccount().canPayFromHoldings(TownySettings.getNationRenameCost())) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));
								return;
							}
							
							finalNation.getAccount().withdraw(TownySettings.getNationRenameCost(), String.format("Nation renamed to: %s", name));
								
							nationRename(player, finalNation, name);
				    	})
				    	.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())))
						.sendTo(player);
				    	
                    } else {
                    	nationRename(player, nation, split[1]);
                    }
				}

			} else if (split[0].equalsIgnoreCase("tag")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAG.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					throw new TownyException("Eg: /nation set tag PLT");
				else if (split[1].equalsIgnoreCase("clear")) {
					nation.setTag(" ");
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_reset_nation_tag", player.getName()));
				} else {
					nation.setTag(NameValidation.checkAndFilterName(split[1]));
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_set_nation_tag", player.getName(), nation.getTag()));
				}
			} else if (split[0].equalsIgnoreCase("title")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set title bilbo Jester ");
				else
					resident = getResidentOrThrow(split[1]);
				
				if (!CombatUtil.isSameNation(getResidentOrThrow(player.getUniqueId()), resident)) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_nation", resident.getName()));
					return;
				}

				String title = StringMgmt.join(NameValidation.checkAndFilterArray(StringMgmt.remArgs(split, 2)));
				if (title.length() > TownySettings.getMaxTitleLength()) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_input_too_long"));
					return;
				}
				
				resident.setTitle(title);
				resident.save();

				if (resident.hasTitle())
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_set_title", resident.getName(), resident.getTitle()));
				else
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_clear_title_surname", "Title", resident.getName()));

			} else if (split[0].equalsIgnoreCase("surname")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set surname bilbo the dwarf ");
				else
					resident = getResidentOrThrow(split[1]);

				if (!CombatUtil.isSameNation(getResidentOrThrow(player.getUniqueId()), resident)) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_nation", resident.getName()));
					return;
				}

				String surname = StringMgmt.join(NameValidation.checkAndFilterArray(StringMgmt.remArgs(split, 2)));
				if (surname.length() > TownySettings.getMaxTitleLength()) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_input_too_long"));
					return;
				}
				
				resident.setSurname(surname);
				resident.save();

				if (resident.hasSurname())
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_set_surname", resident.getName(), resident.getSurname()));
				else
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_clear_title_surname", "Surname", resident.getName()));


			} else if (split[0].equalsIgnoreCase("board")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set board " + Translation.of("town_help_9"));
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

					if (!line.equals("none")) {
						if (!NameValidation.isValidString(line)) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_string_nationboard_not_set"));
							return;
						}
						// TownyFormatter shouldn't be given any string longer than 159, or it has trouble splitting lines.
						if (line.length() > 159)
							line = line.substring(0, 159);
					} else 
						line = "";
					
					nation.setBoard(line);
					TownyMessaging.sendNationBoard(player, nation);
				}
			} else if (split[0].equalsIgnoreCase("mapcolor")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_MAPCOLOR.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set mapcolor brown.");
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

					if (!TownySettings.getNationColorsMap().containsKey(line.toLowerCase())) {
						String allowedColorsListAsString = TownySettings.getNationColorsMap().keySet().toString();
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_nation_map_color", allowedColorsListAsString));
						return;
					}

					nation.setMapColorHexCode(TownySettings.getNationColorsMap().get(line.toLowerCase()));
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_map_color_changed", line.toLowerCase()));
				}
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", split[0]));
				return;
			}
			
			nation.save();
		}
	}

	public static void nationToggle(CommandSender sender, String[] split, boolean admin, Nation nation) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.NATION_TOGGLE_HELP.send(sender);
		} else {
			Resident resident;

			try {
				if (!admin) {
					resident = getResidentOrThrow(((Player) sender ).getUniqueId());
					nation = resident.getTown().getNation();
				} else  // Treat any resident tests as though the king were doing it.
					resident = nation.getKing();
				
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(sender, x.getMessage());
				return;
			}
			
			if (!admin && !TownyUniverse.getInstance().getPermissionSource().testPermission((Player) sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			Optional<Boolean> choice = Optional.empty();
			if (split.length == 2) {
				choice = BaseCommand.parseToggleChoice(split[1]);
			}

			if (split[0].equalsIgnoreCase("peaceful") || split[0].equalsIgnoreCase("neutral")) {

				boolean peacefulState = choice.orElse(!nation.isNeutral());
				double cost = TownySettings.getNationNeutralityCost();
				
				if (nation.isNeutral() && peacefulState) throw new TownyException(Translation.of("msg_nation_already_peaceful"));
				else if (!nation.isNeutral() && !peacefulState) throw new TownyException(Translation.of("msg_nation_already_not_peaceful"));

				if (peacefulState && TownyEconomyHandler.isActive() && !nation.getAccount().canPayFromHoldings(cost))
					throw new TownyException(Translation.of("msg_nation_cant_peaceful"));

				// Fire cancellable event directly before setting the toggle.
				NationToggleNeutralEvent preEvent = new NationToggleNeutralEvent(sender, nation, admin, peacefulState);
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancelMessage());
				
				// If they setting neutral status on send a message confirming they paid something, if they did.
				if (peacefulState && TownyEconomyHandler.isActive() && cost > 0) {
					nation.getAccount().withdraw(cost, "Peaceful Nation Cost");
					TownyMessaging.sendMsg(sender, Translation.of("msg_you_paid", TownyEconomyHandler.getFormattedBalance(cost)));
				}

				nation.setNeutral(peacefulState);

				// Send message feedback to the whole nation.
				TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_peaceful") + (nation.isNeutral
						() ? Colors.Green : Colors.Red + " not") + " peaceful.");

			} else if(split[0].equalsIgnoreCase("public")){

				// Fire cancellable event directly before setting the toggle.
				NationTogglePublicEvent preEvent = new NationTogglePublicEvent(sender, nation, admin, choice.orElse(!nation.isPublic()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancelMessage());
                
				// Set the toggle setting.
                nation.setPublic(preEvent.getFutureState());
                
				// Send message feedback.
                TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_changed_public", nation.isPublic() ? Translation.of("enabled") : Translation.of("disabled")));

            } else if(split[0].equalsIgnoreCase("open")){

            	// Fire cancellable event directly before setting the toggle.
				NationToggleOpenEvent preEvent = new NationToggleOpenEvent(sender, nation, admin, choice.orElse(!nation.isOpen()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancelMessage());
                
				// Set the toggle setting.
                nation.setOpen(preEvent.getFutureState());
                
                // Send message feedback.
                TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_changed_open", nation.isOpen() ? Translation.of("enabled") : Translation.of("disabled")));

            } else {
            	/*
            	 * Fire of an event if we don't recognize the command being used.
            	 * The event is cancelled by default, leaving our standard error message 
            	 * to be shown to the player, unless the user of the event does 
            	 * a) uncancel the event, or b) alters the cancellation message.
            	 */
            	NationToggleUnknownEvent event = new NationToggleUnknownEvent(sender, nation, admin, split);
            	Bukkit.getPluginManager().callEvent(event);
            	if (event.isCancelled()) {
            		TownyMessaging.sendErrorMsg(sender, event.getCancelMessage());
            		return;
            	}
				
			}

			nation.save();
		}
	}

	public static void nationRename(Player player, Nation nation, String newName) {

		NationPreRenameEvent event = new NationPreRenameEvent(nation, newName);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_rename_cancelled"));
			return;
		}
		
		try {
			TownyUniverse.getInstance().getDataSource().renameNation(nation, newName);
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_set_name", player.getName(), nation.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

    /**
     * Wrapper for the nationSpawn() method. All calls should be through here
     * unless bypassing for admins.
     *
     * @param player - Player.
     * @param split  - Current command arguments.
     * @param ignoreWarning - Whether to ignore the cost
     * @throws TownyException - Exception.
     */
    public static void nationSpawn(Player player, String[] split, boolean ignoreWarning) throws TownyException {

        try {

            Resident resident = getResidentOrThrow(player.getUniqueId());
            Nation nation;
            String notAffordMSG;

            // Set target nation and affiliated messages.
            if (split.length == 0) {

                if (!resident.hasTown()) {
                    TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_dont_belong_nation"));
                    return;
                }

                if (!resident.getTown().hasNation()) {
                    TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_dont_belong_nation"));
                    return;
                }

                nation = resident.getTown().getNation();
                notAffordMSG = Translation.of("msg_err_cant_afford_tp");

			} else {
				// split.length > 1
				nation = getNationOrThrow(split[0]);
				notAffordMSG = Translation.of("msg_err_cant_afford_tp_nation", nation.getName());

			}
            
			SpawnUtil.sendToTownySpawn(player, split, nation, notAffordMSG, false, ignoreWarning, SpawnType.NATION);
		} catch (NotRegisteredException e) {

            throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));

        }

    }

    private static void nationTransaction(Player player, String[] args, boolean withdraw) {
		try {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident == null || !resident.hasNation())
				throw new TownyException(Translation.of("msg_err_dont_belong_nation"));

			if (args.length < 2 || args.length > 3)
				throw new TownyException(Translation.of("msg_must_specify_amnt", "/nation" + (withdraw ? " withdraw" : " deposit")));

			int amount;
			try {
				amount = Integer.parseInt(args[1].trim());
			} catch (NumberFormatException ex) {
				throw new TownyException(Translation.of("msg_error_must_be_int"));
			}
			
			if (args.length == 2) {
				if (withdraw)
					MoneyUtil.nationWithdraw(player, resident, resident.getTown().getNation(), amount);
				else 
					MoneyUtil.nationDeposit(player, resident, resident.getTown().getNation(), amount);
				return;
			}
			
			if (withdraw)
				throw new TownyException(Translation.of("msg_must_specify_amnt", "/nation withdraw"));

			if (args.length == 3) {
				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT_OTHER.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				Town town = TownyUniverse.getInstance().getTown(args[2]);
				if (town != null) {
					if (!resident.getTown().getNation().hasTown(town))
						throw new TownyException(Translation.of("msg_err_not_same_nation", town.getName()));

					MoneyUtil.townDeposit(player, resident, town, resident.getTown().getNation(), amount);

				} else {
					throw new NotRegisteredException();
				}
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
    }
}
