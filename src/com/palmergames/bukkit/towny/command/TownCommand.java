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
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.PreNewTownEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.event.TownInvitePlayerEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.TownPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreSetHomeBlockEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleUnknownEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleExplosionEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleFireEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleMobsEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleOpenEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownTogglePVPEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownTogglePublicEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleTaxPercentEvent;
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
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.inviteobjects.PlayerJoinTownInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinSettings;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.InvalidObjectException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Send a list of all town help commands to player Command: /town
 */

public class TownCommand extends BaseCommand implements CommandExecutor, TabCompleter {

	private static Towny plugin;

	private static final List<String> townTabCompletes = Arrays.asList(
		"here",
		"leave",
		"list",
		"online",
		"new",
		"plots",
		"add",
		"kick",
		"spawn",
		"claim",
		"unclaim",
		"withdraw",
		"delete",
		"outlawlist",
		"deposit",
		"outlaw",
		"outpost",
		"ranklist",
		"rank",
		"reclaim",
		"reslist",
		"say",
		"set",
		"toggle",
		"join",
		"invite",
		"buy",
		"mayor",
		"bankhistory"
		);
	private static final List<String> townSetTabCompletes = Arrays.asList(
		"board",
		"mayor",
		"homeblock",
		"spawn",
		"spawncost",
		"name",
		"outpost",
		"jail",
		"perm",
		"tag",
		"taxes",
		"plottax",
		"plotprice",
		"shopprice",
		"shoptax",
		"embassyprice",
		"embassytax",
		"title",
		"surname",
		"taxpercentcap"
	);
	private static final List<String> townListTabCompletes = Arrays.asList(
		"residents",
		"balance",
		"bankrupt",
		"name",		
		"online",
		"open",
		"public",
		"ruined",
		"townblocks"
	);
	static final List<String> townToggleTabCompletes = Arrays.asList(
		"explosion",
		"fire",
		"mobs",
		"neutral",
		"peaceful",
		"public",
		"pvp",
		"taxpercent",
		"open",
		"jail"
	);
	
	private static final List<String> townConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);
	
	static final List<String> townAddRemoveTabCompletes = Arrays.asList(
		"add",
		"remove"
	);
	
	private static final List<String> townClaimTabCompletes = Arrays.asList(
		"outpost",
		"auto",
		"circle",
		"rect"
	);
	
	public static final List<String> townUnclaimTabCompletes = Arrays.asList(
		"circle",
		"rect",
		"all"
	);
	
	private static List<String> townInviteTabCompletes = Arrays.asList(
		"sent",
		"received",
		"accept",
		"deny"
	);

	public TownCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player) sender;
			
			switch (args[0].toLowerCase()) {
				case "online":
				case "reslist":
				case "outlawlist":
				case "plots":
				case "delete":
				case "join":
					if (args.length == 2)
						return getTownyStartingWith(args[1], "t");
					break;
				case "spawn":
					if (args.length == 2) {
						List<String> townOrIgnore = getTownyStartingWith(args[1], "t");
						townOrIgnore.add("-ignore");						
						return NameUtil.filterByStart(townOrIgnore, args[1]);
					}
					if (args.length == 3) {
						List<String> ignore = Collections.singletonList("-ignore");
						return ignore;
					}
				case "rank":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(townAddRemoveTabCompletes, args[1]);
						case 3:
							return getTownResidentNamesOfPlayerStartingWith(player, args[2]);
						case 4:
							switch (args[1].toLowerCase()) {
								case "add":
									return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[3]);
								case "remove": {
									Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
									
									if (res != null) {
										return NameUtil.filterByStart(res.getTownRanks(), args[3]);
									}
									break;
								}
							}
					}
					break;
				case "outlaw":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(townAddRemoveTabCompletes, args[1]);
						case 3:
							switch (args[1].toLowerCase()) {
								case "add":
									return getTownyStartingWith(args[2], "r");
								case "remove": {
									Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
									if (resident != null) {
										try {
											return NameUtil.filterByStart(NameUtil.getNames(resident.getTown().getOutlaws()), args[2]);
										} catch (TownyException ignore) {
										}
									}
								}
							}
					}
					break;
				case "claim":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(townClaimTabCompletes, args[1]);
						case 3:
							if (!args[1].equalsIgnoreCase("outpost")) {
								return NameUtil.filterByStart(Collections.singletonList("auto"), args[2]);
							}
					}
					break;
				case "unclaim":
					if (args.length == 2)
						return NameUtil.filterByStart(townUnclaimTabCompletes, args[1]);
					break;
				case "add":
					if (args.length == 2)
						return getResidentsWithoutTownStartingWith(args[1]);
					break;
				case "kick":
					if (args.length == 2)
						return getTownResidentNamesOfPlayerStartingWith(player, args[1]);
					break;
				case "set":
					try {
						Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
						if (res != null)
							return townSetTabComplete(res.getTown(), args);
					} catch (TownyException e) {
					}
					return Collections.emptyList();
				case "invite":
					switch (args.length) {
						case 2:
							List<String> returnValue = NameUtil.filterByStart(townInviteTabCompletes, args[1]);
							if (returnValue.size() > 0) {
								return returnValue;
							} else {
								if (args[1].startsWith("-")) {
									Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
									
									if (res == null)
										return null;
									
									try {
										return NameUtil.filterByStart(res.getTown().getSentInvites()
											// Get all sent invites
											.stream()
											.map(Invite::getReceiver)
											.map(InviteReceiver::getName)
											.collect(Collectors.toList()), args[1].substring(1))
												// Add the hyphen back to the front
												.stream()
												.map(e -> "-"+e)
												.collect(Collectors.toList());
									} catch (TownyException ignore) {}
								} else {
									return null;
								}
							}
						case 3:
							switch (args[1].toLowerCase()) {
								case "accept":
								case "deny": {
									Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
									if (res == null)
										return null;
									
									try {
										return NameUtil.filterByStart(res.getTown().getReceivedInvites()
											// Get the names of all received invites
											.stream()
											.map(Invite::getSender)
											.map(InviteSender::getName)
											.collect(Collectors.toList()),args[2]);
									} catch (TownyException ignore) {
									}
								}
							}
					}
				case "buy":
					if (args.length == 2)
						return NameUtil.filterByStart(Collections.singletonList("bonus"), args[1]);
					break;
				case "toggle":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(townToggleTabCompletes, args[1]);
						case 3:
							if (!args[1].equalsIgnoreCase("jail")) {
								return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
							}
							break;
						case 4:
							return getTownResidentNamesOfPlayerStartingWith(player, args[3]);
					}
				case "list":
					switch (args.length) {
						case 2:
							return Collections.singletonList("by");
						case 3:
							return NameUtil.filterByStart(townListTabCompletes, args[2]);
					}
				default:
					if (args.length == 1)
						return filterByStartOrGetTownyStartingWith(townTabCompletes, args[0], "t");
			}
		} else if (args.length == 1) {
			return filterByStartOrGetTownyStartingWith(townConsoleTabCompletes, args[0], "t");
		}
		
		return Collections.emptyList();
	}
	
	static List<String> townSetTabComplete(Town town, String[] args) {
		if (args.length == 2) {
			return NameUtil.filterByStart(townSetTabCompletes, args[1]);
		} else if (args.length > 2) {
			switch (args[1].toLowerCase()) {
				case "mayor":
					return NameUtil.filterByStart(NameUtil.getNames(town.getResidents()), args[2]);
				case "perm":
					return permTabComplete(StringMgmt.remArgs(args, 2));
				case "tag":
					if (args.length == 3)
						return NameUtil.filterByStart(Collections.singletonList("clear"), args[2]);
				case "title":
				case "surname":
					if (args.length == 3)
						return NameUtil.filterByStart(NameUtil.getNames(town.getResidents()), args[2]);
			}
		}
		
		return Collections.emptyList();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			if (plugin.isError()) {
				sender.sendMessage(Colors.Rose + "[Towny Error] Locked in Safe mode!");
				return false;
			}
				
			parseTownCommand((Player) sender, args);
		} else {
			
			parseTownCommandForConsole(sender, args);
		}
		return true;
	}

	private void parseTownCommandForConsole(final CommandSender sender, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			HelpMenu.TOWN_HELP_CONSOLE.send(sender);
		
		} else if (split[0].equalsIgnoreCase("list")) {

			try {
				listTowns(sender, split);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}

		} else {
			Town town = TownyUniverse.getInstance().getTown(split[0]);
			
			if (town != null)
				townStatusScreen(sender, town);
			else
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_not_registered_1", split[0]));
		}
	}

	private void parseTownCommand(final Player player, String[] split) {

		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		try {

			if (split.length == 0) {

				Resident resident = getResidentOrThrow(player.getUniqueId());
				if (!resident.hasTown())
					throw new TownyException(Translation.of("msg_err_dont_belong_town"));
				
				townStatusScreen(player, resident.getTown());
				
			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

				HelpMenu.TOWN_HELP.send(player);
				
			} else if (split[0].equalsIgnoreCase("mayor")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_MAYOR.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				HelpMenu.TOWN_MAYOR_HELP.send(player);
				
			} else if (split[0].equalsIgnoreCase("here")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_HERE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (TownyAPI.getInstance().isWilderness(player.getLocation()))
					throw new TownyException(Translation.of("msg_not_claimed", Coord.parseCoord(player.getLocation())));
				
				townStatusScreen(player, TownyAPI.getInstance().getTown(player.getLocation()));

			} else if (split[0].equalsIgnoreCase("list")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				listTowns(player, split);

			} else if (split[0].equalsIgnoreCase("new") || split[0].equalsIgnoreCase("create")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_NEW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length == 1) {
					throw new TownyException(Translation.of("msg_specify_name"));
				} else {
					String townName = String.join("_", StringMgmt.remFirstArg(split));
					Resident resident = getResidentOrThrow(player.getUniqueId());
					boolean noCharge = TownySettings.getNewTownPrice() == 0.0 || !TownyEconomyHandler.isActive();
					newTown(player, townName, resident, noCharge);
				}

			} else if (split[0].equalsIgnoreCase("reclaim")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RECLAIM.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if(!TownRuinSettings.getTownRuinsReclaimEnabled())
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				TownRuinUtil.processRuinedTownReclaimRequest(player, plugin);

			} else if (split[0].equalsIgnoreCase("join")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				parseTownJoin(player, StringMgmt.remFirstArg(split));
				
			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LEAVE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				townLeave(player);

			} else if (split[0].equalsIgnoreCase("withdraw")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_WITHDRAW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				townTransaction(player, split, true);

			} else if (split[0].equalsIgnoreCase("deposit")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				townTransaction(player, split, false);
				
			} else if (split[0].equalsIgnoreCase("plots")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				townPlots(player, split);

			} else if (split[0].equalsIgnoreCase("reslist")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RESLIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				townResList(player, split);

			} else if (split[0].equalsIgnoreCase("outlawlist")) {

				townOutlawList(player, split);

			} else {
				/*
				 * The remaining subcommands are completely blocked from use by ruined towns.
				 */

				if (TownRuinUtil.isPlayersTownRuined(player)) {
					
					// Player with ruined towns are unable to do most town commands but we
					// do still want them to be able to look at other towns' status screens.
					Town town = TownyUniverse.getInstance().getTown(split[0]);
					if (town == null)
						throw new TownyException(Translation.of("msg_err_cannot_use_command_because_town_ruined"));

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OTHERTOWN.getNode()) && !town.hasResident(player.getName()))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					
					townStatusScreen(player, town);
					return;
				}

				String[] newSplit = StringMgmt.remFirstArg(split);

				if (split[0].equalsIgnoreCase("rank")) {

					/*
					 * perm tests performed in method.
					 */
					townRank(player, newSplit);

				} else if (split[0].equalsIgnoreCase("set")) {

					/*
					 * perm test performed in method.
					 */
					townSet(player, newSplit, false, null);

				} else if (split[0].equalsIgnoreCase("buy")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_BUY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townBuy(player, newSplit);

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test performed in method.
					 */
					townToggle(player, newSplit, false, null);

				} else if (split[0].equalsIgnoreCase("spawn")) {

					/*
					 * town spawn handles it's own perms.
					 */
					boolean ignoreWarning = false;
					
					if ((split.length > 2 && split[2].equals("-ignore"))) {
						ignoreWarning = true;
					}
					
					townSpawn(player, newSplit, false, ignoreWarning);

				} else if (split[0].equalsIgnoreCase("outpost")) {

					/*
					 * outposts check its own perms. 
					 */
					townOutpost(player, split);

				} else if (split[0].equalsIgnoreCase("delete")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DELETE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townDelete(player, newSplit);

				} else if (split[0].equalsIgnoreCase("ranklist")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANKLIST.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					Resident resident = getResidentOrThrow(player.getUniqueId());
					if (!resident.hasTown())
						throw new TownyException(Translation.of("msg_err_dont_belong_town"));
					TownyMessaging.sendMessage(player, TownyFormatter.getRanks(resident.getTown()));

				} else if (split[0].equalsIgnoreCase("add")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townAdd(player, null, newSplit);

				} else if (split[0].equalsIgnoreCase("invite") || split[0].equalsIgnoreCase("invites")) {// He does have permission to manage Real invite Permissions. (Mayor or even assisstant)

					parseInviteCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townKick(player, newSplit);

				} else if (split[0].equalsIgnoreCase("claim")) {

					parseTownClaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseTownUnclaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("online")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseTownOnlineCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("say")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SAY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					
					Resident resident = getResidentOrThrow(player.getUniqueId());
					if (!resident.hasTown())
						throw new TownyException(Translation.of("msg_err_dont_belong_town"));
					TownyMessaging.sendPrefixedTownMessage(resident.getTown(), StringMgmt.join(newSplit));
					
				} else if (split[0].equalsIgnoreCase("outlaw")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTLAW.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseTownOutlawCommand(player, newSplit, false, getResidentOrThrow(player.getUniqueId()).getTown());
				} else if (split[0].equalsIgnoreCase("bankhistory")) {
					
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					int pages = 10;
					if (newSplit.length > 0)
						try {
							pages = Integer.parseInt(newSplit[0]);
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
							return;
						}

					TownyUniverse.getInstance().getResident(player.getUniqueId()).getTown().generateBankHistoryBook(player, pages);

				} else {
					/*
					 * We've gotten this far without a match, check if the argument is a town name.
					 */
					Town town = TownyUniverse.getInstance().getTown(split[0]);					
					if (town == null)
						throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OTHERTOWN.getNode()) && !town.hasResident(player.getName()))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					
					townStatusScreen(player, town);
				}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

	}

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		// We know he has the main permission to manage this stuff. So Let's continue:

		Resident resident = getResidentOrThrow(player.getUniqueId());

		String received = Translation.of("town_received_invites")
				.replace("%a", Integer.toString(resident.getTown().getReceivedInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident.getTown())));
		String sent = Translation.of("town_sent_invites")
				.replace("%a", Integer.toString(resident.getTown().getSentInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown())));


		if (newSplit.length == 0) { // (/town invite)
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_SEE_HOME.getNode())) {
				throw new TownyException(Translation.of("msg_err_command_disable"));
			}

			HelpMenu.TOWN_INVITE.send(player);
			TownyMessaging.sendMessage(player, sent);
			TownyMessaging.sendMessage(player, received);
			return;
		}
		if (newSplit.length >= 1) { // /town invite [something]
			if (newSplit[0].equalsIgnoreCase("help") || newSplit[0].equalsIgnoreCase("?")) {
				HelpMenu.TOWN_INVITE.send(player);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("sent")) { //  /invite(remfirstarg) sent args[1]
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_SENT.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				List<Invite> sentinvites = resident.getTown().getSentInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException ignored) {
					}
				}
				InviteCommand.sendInviteList(player, sentinvites, page, true);
				player.sendMessage(sent);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("received")) { // /town invite received
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_RECEIVED.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				List<Invite> receivedinvites = resident.getTown().getReceivedInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException ignored) {
					}
				}
				InviteCommand.sendInviteList(player, receivedinvites, page, false);
				player.sendMessage(received);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("accept")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ACCEPT.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				// /town (gone)
				// invite (gone)
				// args[0] = accept = length = 1
				// args[1] = [Nation] = length = 2
				Town town = resident.getTown();
				Nation nation;
				List<Invite> invites = town.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_town_no_invites"));
					return;
				}
				if (newSplit.length >= 2) { // /invite deny args[1]
					nation = TownyUniverse.getInstance().getNation(newSplit[1]);
					
					if (nation == null) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_town_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}

				Invite toAccept = null;
				for (Invite invite : InviteHandler.getActiveInvites()) {
					if (invite.getSender().equals(nation) && invite.getReceiver().equals(town)) {
						toAccept = invite;
						break;
					}
				}
				if (toAccept != null) {
					try {
						InviteHandler.acceptInvite(toAccept);
						return;
					} catch (TownyException | InvalidObjectException e) {
						e.printStackTrace();
					}
				}
			}
			if (newSplit[0].equalsIgnoreCase("deny")) { // /town invite deny
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_DENY.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				Town town = resident.getTown();
				Nation nation;
				List<Invite> invites = town.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_town_no_invites"));
					return;
				}
				if (newSplit.length >= 2) { // /invite deny args[1]
					nation = TownyUniverse.getInstance().getNation(newSplit[1]);
					
					if (nation == null) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_town_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}
				
				Invite toDecline = null;
				
				for (Invite invite : InviteHandler.getActiveInvites()) {
					if (invite.getSender().equals(nation) && invite.getReceiver().equals(town)) {
						toDecline = invite;
						break;
					}
				}
				if (toDecline != null) {
					try {
						InviteHandler.declineInvite(toDecline, false);
						TownyMessaging.sendMessage(player, Translation.of("successful_deny"));
						return;
					} catch (InvalidObjectException e) {
						e.printStackTrace(); // Shouldn't happen, however like i said a fallback
					}
				}
			} else {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				townAdd(player, null, newSplit);
				// It's none of those 4 subcommands, so it's a playername, I just expect it to be ok.
				// If it is invalid it is handled in townAdd() so, I'm good
			}
		}
	}

	public static void parseTownOutlawCommand(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			// Help output.
			if (!admin) {
				sender.sendMessage(ChatTools.formatTitle("/town outlaw"));
				sender.sendMessage(ChatTools.formatCommand("", "/town outlaw", "add/remove [name]", ""));
			} else {
				sender.sendMessage(ChatTools.formatTitle("/ta town [town] outlaw"));
				sender.sendMessage(ChatTools.formatCommand("", "/ta town [town] outlaw", "add/remove [name]", ""));
			}

		} else {

			Resident resident;
			Resident target = null;
			Town targetTown = null;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 2)
				throw new TownyException("Eg: /town outlaw add/remove [name]");

			if (!admin) {
				resident = getResidentOrThrow(sender.getName());
			}
			else
				resident = town.getMayor();				
			
			target = townyUniverse.getResident(split[1]);
			
			if (target == null) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_invalid_name", split[1]));
				return;
			}

			if (target != null && split[0].equalsIgnoreCase("add")) {
				try {
					try {
						targetTown = target.getTown();
					} catch (Exception e1) {
					}
					// Don't allow a resident to outlaw their own mayor.
					if (resident.getTown().getMayor().equals(target))
						return;
					// Kick outlaws from town if they are residents.
					if (targetTown != null)
						if (targetTown == town){
							townRemoveResident(town, target);
							String outlawer = (admin ? Translation.of("admin_sing") : sender.getName());
							TownyMessaging.sendMsg(target, Translation.of("msg_kicked_by", outlawer));
							TownyMessaging.sendPrefixedTownMessage(town,Translation.of("msg_kicked", outlawer, target.getName()));
						}
					town.addOutlaw(target);
					town.save();
					if (target.getPlayer().isOnline())
						TownyMessaging.sendMsg(target, Translation.of("msg_you_have_been_declared_outlaw", town.getName()));
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_you_have_declared_an_outlaw", target.getName(), town.getName()));
					if (admin)
						TownyMessaging.sendMsg(sender, Translation.of("msg_you_have_declared_an_outlaw", target.getName(), town.getName()));
				} catch (AlreadyRegisteredException e) {
					// Must already be an outlaw
					TownyMessaging.sendMsg(sender, Translation.of("msg_err_resident_already_an_outlaw"));
					return;
				}

			} else if (target != null && split[0].equalsIgnoreCase("remove")) {
				if (town.hasOutlaw(target)) {
					town.removeOutlaw(target);
					town.save();
					if (target.getPlayer().isOnline())
						TownyMessaging.sendMsg(target, Translation.of("msg_you_have_been_undeclared_outlaw", town.getName()));
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_you_have_undeclared_an_outlaw", target.getName(), town.getName()));
					if (admin)
						TownyMessaging.sendMsg(sender, Translation.of("msg_you_have_undeclared_an_outlaw", target.getName(), town.getName()));
				} else {
					// Must already not be an outlaw
					TownyMessaging.sendMsg(sender, Translation.of("msg_err_player_not_an_outlaw"));
					return;
				}

			} else {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_invalid_property", split[0]));
				return;
			}

			/*
			 * If we got here we have made a change Save the altered resident
			 * data.
			 */
			town.save();

		}

	}

	private void townPlots(CommandSender sender, String[] args) {
		
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		
		Town town = null;
		try {
			if (args.length == 1 && player != null) {
				if (TownRuinUtil.isPlayersTownRuined(player))
					throw new TownyException(Translation.of("msg_err_cannot_use_command_because_town_ruined"));

				town = getResidentOrThrow(player.getUniqueId()).getTown();
			} else {
				town = TownyUniverse.getInstance().getTown(args[1]);
			}
		} catch (Exception e) {
		}
		
		if (town == null) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_specify_name"));
			return;
		}

		List<String> out = new ArrayList<>();

		int townOwned = 0;
		int resident = 0;
		int residentOwned = 0;
		int residentOwnedFS = 0;
		int embassy = 0;
		int embassyRO = 0;
		int embassyFS = 0;
		int shop = 0;
		int shopRO = 0;
		int shopFS = 0;
		int farm = 0;
		int arena = 0;
		int wilds = 0;
		int jail = 0;
		int inn = 0;
		for (TownBlock townBlock : town.getTownBlocks()) {

			if (townBlock.getType() == TownBlockType.EMBASSY) {
				embassy++;
				if (townBlock.hasResident())
					embassyRO++;
				if (townBlock.isForSale())
					embassyFS++;
			} else if (townBlock.getType() == TownBlockType.COMMERCIAL) {
				shop++;
				if (townBlock.hasResident())
					shopRO++;
				if (townBlock.isForSale())
					shopFS++;
			} else if (townBlock.getType() == TownBlockType.FARM) {
				farm++;
			} else if (townBlock.getType() == TownBlockType.ARENA) {
				arena++;
			} else if (townBlock.getType() == TownBlockType.WILDS) {
				wilds++;
			} else if (townBlock.getType() == TownBlockType.JAIL) {
				jail++;
			} else if (townBlock.getType() == TownBlockType.INN) {
				inn++;
			} else if (townBlock.getType() == TownBlockType.RESIDENTIAL) {
				resident++;
				if (townBlock.hasResident())
					residentOwned++;
				if (townBlock.isForSale())
					residentOwnedFS++;
			}
			if (!townBlock.hasResident()) {
				townOwned++;
			}
		}
		out.add(ChatTools.formatTitle(town + " Town Plots"));
		out.add(Colors.Green + "Town Size: " + Colors.LightGreen + town.getTownBlocks().size() + " / " + TownySettings.getMaxTownBlocks(town) + (TownySettings.isSellingBonusBlocks(town) ? Colors.LightBlue + " [Bought: " + town.getPurchasedBlocks() + "/" + TownySettings.getMaxPurchasedBlocks(town) + "]" : "") + (town.getBonusBlocks() > 0 ? Colors.LightBlue + " [Bonus: " + town.getBonusBlocks() + "]" : "") + ((TownySettings.getNationBonusBlocks(town) > 0) ? Colors.LightBlue + " [NationBonus: " + TownySettings.getNationBonusBlocks(town) + "]" : ""));
		out.add(Colors.Green + "Town Owned Land: " + Colors.LightGreen + townOwned);
		out.add(Colors.Green + "Farms   : " + Colors.LightGreen + farm);
		out.add(Colors.Green + "Arenas : " + Colors.LightGreen + arena);
		out.add(Colors.Green + "Wilds    : " + Colors.LightGreen + wilds);
		out.add(Colors.Green + "Jails    : " + Colors.LightGreen + jail);
		out.add(Colors.Green + "Inns    : " + Colors.LightGreen + inn);
		out.add(Colors.Green + "Type: " + Colors.LightGreen + "Player-Owned / ForSale / Total / Daily Revenue");
		out.add(Colors.Green + "Residential: " + Colors.LightGreen + residentOwned + " / " + residentOwnedFS + " / " + resident + " / " + (residentOwned * town.getPlotTax()));
		out.add(Colors.Green + "Embassies : " + Colors.LightGreen + embassyRO + " / " + embassyFS + " / " + embassy + " / " + (embassyRO * town.getEmbassyPlotTax()));
		out.add(Colors.Green + "Shops      : " + Colors.LightGreen + shopRO + " / " + shopFS + " / " + shop + " / " + (shop * town.getCommercialPlotTax()));
		out.add(Translation.of("msg_town_plots_revenue_disclaimer"));
		TownyMessaging.sendMessage(sender, out);

	}

	private void parseTownOnlineCommand(Player player, String[] split) throws TownyException {

		if (split.length > 0) {
			Town town = TownyUniverse.getInstance().getTown(split[0]);
			
			if (town == null) {
				throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
			}
			
			List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, town);
			if (onlineResidents.size() > 0) {
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translation.of("msg_town_online"), town, player));
			} else {
				TownyMessaging.sendMessage(player, Translation.of("default_towny_prefix") + Colors.White + "0 " + Translation.of("res_list") + " " + (Translation.of("msg_town_online") + ": " + town));
			}
		} else {
			try {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				Town town = resident.getTown();
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translation.of("msg_town_online"), town, player));
			} catch (NotRegisteredException x) {
				TownyMessaging.sendMessage(player, Translation.of("msg_err_dont_belong_town"));
			}
		}
	}

	/**
	 * Send a list of all towns in the universe to player Command: /town list
	 *
	 * @param sender - Sender (player or console.)
	 * @param split  - Current command arguments.
	 * @throws TownyException when a player lacks the permission node.
	 */
	@SuppressWarnings("unchecked")
	public void listTowns(CommandSender sender, String[] split) throws TownyException {

		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		boolean console = true;
		Player player = null;
		
		if (split.length == 2 && split[1].equals("?")) {
			sender.sendMessage(ChatTools.formatTitle("/town list"));
			sender.sendMessage(ChatTools.formatCommand("", "/town list", "{page #}", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town list", "{page #} by residents", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town list", "{page #} by open", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town list", "{page #} by balance", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town list", "{page #} by name", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town list", "{page #} by townblocks", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town list", "{page #} by online", ""));
			return;
		}
		
		if (sender instanceof Player) {
			console = false;
			player = (Player) sender;
		}

		/*
		 * The default comparator on /t list is by residents, test it before we start anything else.
		 */
		if (split.length < 2 && !console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_RESIDENTS.getNode())) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_command_disable"));
			return;
		}
		
		List<Town> townsToSort = TownyUniverse.getInstance().getDataSource().getTowns();
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		ComparatorType type = ComparatorType.RESIDENTS;
		int total = (int) Math.ceil(((double) townsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) { // Is a case of someone using /n list by {comparator}
				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_multiple_comparators"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("resident")) 
						split[i] = "residents";
					
					if (!console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST.getNode(split[i])))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					
					if (!townListTabCompletes.contains(split[i].toLowerCase()))
						throw new TownyException(Translation.of("msg_error_invalid_comparator_town"));

					type = ComparatorType.valueOf(split[i].toUpperCase());

				} else {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else { // Is a case of someone using /t list, /t list # or /t list by {comparator} #
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
		
		final List<Town> towns = townsToSort;
		final Comparator comparator = type.getComparator();
		final int pageNumber = page;
		final int totalNumber = total; 
		final ComparatorType finalType = type;
		try {
			if (!TownySettings.isTownListRandom()) {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					towns.sort(comparator);
					sendList(sender, towns, finalType, pageNumber, totalNumber);
				});
			} else { 
				Collections.shuffle(towns);
				sendList(sender, towns, finalType, pageNumber, totalNumber);
			}
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_comparator_failed"));
		}
	}
	
	public void sendList(CommandSender sender, List<Town> towns, ComparatorType type, int page, int total) {
		
		if (Towny.isSpigot && sender instanceof Player) {
			TownySpigotMessaging.sendSpigotTownList(sender, towns, type, page, total);
			return;
		}

		int iMax = Math.min(page * 10, towns.size());
		List<String> townsformatted = new ArrayList<>(10);
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Town town = towns.get(i);
			String slug = null;
			switch (type) {
			case BALANCE:
				slug = Colors.LightBlue + "(" +TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()) + ")";
				break;
			case TOWNBLOCKS:
				slug = Colors.LightBlue + "(" +town.getTownBlocks().size() + ")";
				break;
			case RUINED:
				slug = town.isRuined() ? Translation.of("msg_ruined"):"";
				break;
			case BANKRUPT:
				slug = town.isBankrupt() ? Translation.of("msg_bankrupt"):"";
				break;
			default:
				slug = Colors.LightBlue + "(" +town.getResidents().size() + ")";
				break;
			}
			
			String output = Colors.Blue + StringMgmt.remUnderscore(town.getName()) + (TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + slug);
			if (town.isOpen())
				output += " " + Translation.of("status_title_open");
			townsformatted.add(output);
		}
		
		String[] messages = ChatTools.formatList(Translation.of("town_plu"),
			Colors.Blue + Translation.of("town_name") +
				(TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + Colors.LightBlue + Translation.of(type.getName())),
			townsformatted, Translation.of("LIST_PAGE", page, total)
		);
		
		sender.sendMessage(messages);
		
	}

	public static void townToggle(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWN_TOGGLE_HELP.send(sender);
		} else {
			
			boolean permChanged = false; // Used to determine if we have to save the town's townblocks later on.
			Resident resident;
			
			if (!admin) {
				resident = getResidentOrThrow(sender.getName());
				
				town = resident.getTown();
			} else { // Admin actions will be carried out as the mayor of the town for the purposes of some tests.
				resident = town.getMayor();
			}

			if (!admin && !permSource.testPermission((Player) sender, PermissionNodes.TOWNY_COMMAND_TOWN_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			Optional<Boolean> choice = Optional.empty();
			if (split.length == 2 && !split[0].equalsIgnoreCase("jail")) { // Exclude jail command from on/off
				choice = BaseCommand.parseToggleChoice(split[1]);
			}

			if (split[0].equalsIgnoreCase("public")) {

				// Fire cancellable event directly before setting the toggle.
				TownTogglePublicEvent preEvent = new TownTogglePublicEvent(sender, town, admin, choice.orElse(!town.isPublic()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setPublic(preEvent.getFutureState());
				
				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_public", town.isPublic() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_public", town.isPublic() ? Translation.of("enabled") : Translation.of("disabled")));

			} else if (split[0].equalsIgnoreCase("pvp")) {
				
				// If we aren't dealing with an admin using /t toggle pvp:
				if (!admin) {
					// Make sure we are allowed to set these permissions.
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));
				
					// Test to see if the pvp cooldown timer is active for the town.
					if (TownySettings.getPVPCoolDownTime() > 0 && !admin && CooldownTimerTask.hasCooldown(town.getName(), CooldownType.PVP) && !permSource.testPermission((Player) sender, PermissionNodes.TOWNY_ADMIN.getNode()))					 
						throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(town.getName(), CooldownType.PVP)));

					// Test to see if an outsider being inside of the Town would prevent toggling PVP.
					if (TownySettings.getOutsidersPreventPVPToggle()) {
						for (Player target : Bukkit.getOnlinePlayers()) {
							if (!TownyAPI.getInstance().isWilderness(target.getLocation()) 
									&& TownyAPI.getInstance().getTown(target.getLocation()).equals(town) 
									&& !town.hasResident(target.getName()))
								throw new TownyException(Translation.of("msg_cant_toggle_pvp_outsider_in_town"));
						}
					}
				}

				// Fire cancellable event directly before setting the toggle.
				TownTogglePVPEvent preEvent = new TownTogglePVPEvent(sender, town, admin, choice.orElse(!town.isPVP()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setPVP(preEvent.getFutureState());
				permChanged = true;

				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_pvp", town.getName(), town.isPVP() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_pvp", town.getName(), town.isPVP() ? Translation.of("enabled") : Translation.of("disabled")));
				
				// Add a cooldown to PVP toggling.
				if (TownySettings.getPVPCoolDownTime() > 0 && !admin && !permSource.testPermission((Player) sender, PermissionNodes.TOWNY_ADMIN.getNode()))
					CooldownTimerTask.addCooldownTimer(town.getName(), CooldownType.PVP);
				
			} else if (split[0].equalsIgnoreCase("explosion")) {

				// Make sure we are allowed to set these permissions.
				if (!admin)
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));
				
				// Fire cancellable event directly before setting the toggle.
				TownToggleExplosionEvent preEvent = new TownToggleExplosionEvent(sender, town, admin, choice.orElse(!town.isBANG()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setBANG(preEvent.getFutureState());
				permChanged = true;

				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_expl", town.getName(), town.isBANG() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_expl", town.getName(), town.isBANG() ? Translation.of("enabled") : Translation.of("disabled")));

			} else if (split[0].equalsIgnoreCase("fire")) {

				// Make sure we are allowed to set these permissions.
				if (!admin)
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));
				
				// Fire cancellable event directly before setting the toggle.
				TownToggleFireEvent preEvent = new TownToggleFireEvent(sender, town, admin, choice.orElse(!town.isFire()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setFire(preEvent.getFutureState());
				permChanged = true;
				
				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_fire", town.getName(), town.isFire() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_fire", town.getName(), town.isFire() ? Translation.of("enabled") : Translation.of("disabled")));
				
			} else if (split[0].equalsIgnoreCase("mobs")) {

				// Make sure we are allowed to set these permissions.
				if (!admin)
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));

				// Fire cancellable event directly before setting the toggle.
				TownToggleMobsEvent preEvent = new TownToggleMobsEvent(sender, town, admin, choice.orElse(!town.hasMobs()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setHasMobs(preEvent.getFutureState());
				permChanged = true;
				
				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_mobs", town.getName(), town.hasMobs() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_mobs", town.getName(), town.hasMobs() ? Translation.of("enabled") : Translation.of("disabled")));
				
			} else if (split[0].equalsIgnoreCase("taxpercent")) {

				// Fire cancellable event directly before setting the toggle.
				TownToggleTaxPercentEvent preEvent = new TownToggleTaxPercentEvent(sender, town, admin, choice.orElse(!town.isTaxPercentage()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setTaxPercentage(preEvent.getFutureState());
				
				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_taxpercent", town.isTaxPercentage() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_taxpercent", town.isTaxPercentage() ? Translation.of("enabled") : Translation.of("disabled")));
				
			} else if (split[0].equalsIgnoreCase("open")) {

				if(town.isBankrupt())
					throw new TownyException(Translation.of("msg_err_bankrupt_town_cannot_toggle_open"));

				// Fire cancellable event directly before setting the toggle.
				TownToggleOpenEvent preEvent = new TownToggleOpenEvent(sender, town, admin, choice.orElse(!town.isOpen()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setOpen(preEvent.getFutureState());
				
				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_open", town.isOpen() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_open", town.isOpen() ? Translation.of("enabled") : Translation.of("disabled")));

				// Send a warning when toggling on (a reminder about plot permissions).
				if (town.isOpen())
					TownyMessaging.sendMsg(sender, Translation.of("msg_toggle_open_on_warning"));
				
			} else if (split[0].equalsIgnoreCase("neutral") || split[0].equalsIgnoreCase("peaceful")) {
				
				// Fire cancellable event directly before setting the toggle.
				TownToggleNeutralEvent preEvent = new TownToggleNeutralEvent(sender, town, admin, choice.orElse(!town.isNeutral()));
				Bukkit.getPluginManager().callEvent(preEvent);
				if (preEvent.isCancelled())
					throw new TownyException(preEvent.getCancellationMsg());

				// Set the toggle setting.
				town.setNeutral(preEvent.getFutureState());

				// Send message feedback.
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_peaceful", town.isNeutral() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_peaceful", town.isNeutral() ? Translation.of("enabled") : Translation.of("disabled")));
				
			} else if (split[0].equalsIgnoreCase("jail")) {
				if (!town.hasJailSpawn())
					throw new TownyException(Translation.of("msg_town_has_no_jails"));

				Integer index, days;
				if (split.length <= 2) {
					sender.sendMessage(ChatTools.formatTitle("/town toggle jail"));
					sender.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident]", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident] [days]", ""));

				} else if (split.length > 2) {
					try {
						Integer.parseInt(split[1]);
						index = Integer.valueOf(split[1]);
						if (split.length == 4) {
							days = Integer.valueOf(split[3]);
							if (days < 1)
								throw new TownyException(Translation.of("msg_err_days_must_be_greater_than_zero"));
						} else
							days = 0;
						if (!admin && !((Player) sender).hasPermission("towny.command.town.toggle.jail")) 
							throw new TownyException(Translation.of("msg_no_permission_to_jail_your_residents"));

						Resident jailedresident = townyUniverse.getResident(split[2]);
						if (jailedresident == null || (!jailedresident.hasTown() && !jailedresident.isJailed()))
							throw new TownyException(Translation.of("msg_resident_not_part_of_any_town"));

						try {

							if (jailedresident.isJailed() && index != jailedresident.getJailSpawn())
								index = jailedresident.getJailSpawn();

							Player jailedPlayer = TownyAPI.getInstance().getPlayer(jailedresident);
							if (jailedPlayer == null) {
								throw new TownyException(Translation.of("msg_player_is_not_online", jailedresident.getName()));
							}
							Town sendertown = resident.getTown();
							if (!admin && jailedPlayer.getUniqueId().equals(((Player) sender).getUniqueId()))
								throw new TownyException(Translation.of("msg_no_self_jailing"));

							if (jailedresident.isJailed()) {
								Town jailTown = townyUniverse.getTown(jailedresident.getJailTown());
								
								if (jailTown == null)
									throw new TownyException(Translation.of("msg_err_not_registered_1", jailedresident.getJailTown()));
								else if (jailTown != sendertown) {
									throw new TownyException(Translation.of("msg_player_not_jailed_in_your_town"));
								} else {
									jailedresident.setJailedByMayor(index, sendertown, days);
									if (admin)
										TownyMessaging.sendMsg(sender, Translation.of("msg_player_has_been_sent_to_jail_number", jailedPlayer.getName(), index));
									return;

								}
							}

							if (jailedresident.getTown() != sendertown)
								throw new TownyException(Translation.of("msg_resident_not_your_town"));

							jailedresident.setJailedByMayor(index, sendertown, days);
							if (admin)
								TownyMessaging.sendMsg(sender, Translation.of("msg_player_has_been_sent_to_jail_number", jailedPlayer.getName(), index));

						} catch (NotRegisteredException x) {
							throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
						}

					} catch (NumberFormatException e) {
						sender.sendMessage(ChatTools.formatTitle("/town toggle jail"));
						sender.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident]", ""));
						sender.sendMessage(ChatTools.formatCommand("", "/town toggle jail", "[number] [resident] [days]", ""));
						return;
					} catch (NullPointerException e) {
						e.printStackTrace();
						return;
					}
				}

			} else {
            	/*
            	 * Fire of an event if we don't recognize the command being used.
            	 * The event is cancelled by default, leaving our standard error message 
            	 * to be shown to the player, unless the user of the event does 
            	 * a) uncancel the event, or b) alters the cancellation message.
            	 */
				TownToggleUnknownEvent event = new TownToggleUnknownEvent(sender, town, admin, split);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled())
					throw new TownyException(event.getCancellationMsg());
			}

			//Propagate perms to all unchanged, town owned, townblocks
			if (permChanged)
				for (TownBlock townBlock : town.getTownBlocks()) {
					if (!townBlock.hasResident() && !townBlock.isChanged()) {
						townBlock.setType(townBlock.getType());
						townBlock.save();
					}
				}

			//Change settings event
			Bukkit.getServer().getPluginManager().callEvent(new TownBlockSettingsChangedEvent(town));

			// Save the Town.
			town.save();
		}
	}

	private static void toggleTest(Player player, Town town, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

		split = split.toLowerCase();

		if (split.contains("mobs")) {
			if (town.getHomeblockWorld().isForceTownMobs())
				throw new TownyException(Translation.of("msg_world_mobs"));
		}

		if (split.contains("fire")) {
			if (town.getHomeblockWorld().isForceFire())
				throw new TownyException(Translation.of("msg_world_fire"));
		}

		if (split.contains("explosion")) {
			if (town.getHomeblockWorld().isForceExpl())
				throw new TownyException(Translation.of("msg_world_expl"));
		}

		if (split.contains("pvp")) {
			if (town.getHomeblockWorld().isForcePVP())
				throw new TownyException(Translation.of("msg_world_pvp"));
		}
	}

	public void townRank(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			// Help output.
			player.sendMessage(ChatTools.formatTitle("/town rank"));
			player.sendMessage(ChatTools.formatCommand("", "/town rank", "add/remove [resident] rank", ""));

		} else {

			Resident resident, target;
			Town town = null;
			String rank;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 3)
				throw new TownyException("Eg: /town rank add/remove [resident] [rank]");

			try {
				resident = getResidentOrThrow(player.getUniqueId());
				target = getResidentOrThrow(split[1]);
				town = resident.getTown();
	
				if (town != target.getTown())
					throw new TownyException(Translation.of("msg_resident_not_your_town"));

			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			/*
			 * Match casing to an existing rank, returns null if Town rank doesn't exist.
			 */
			rank = TownyPerms.matchTownRank(split[2]);
			if (rank == null)
				throw new TownyException(Translation.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getTownRanks(), ", ")));

			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank.toLowerCase())))
				throw new TownyException(Translation.of("msg_no_permission_to_give_rank"));

			if (split[0].equalsIgnoreCase("add")) {
				try {
					if (target.addTownRank(rank)) {
						if (BukkitTools.isOnline(target.getName())) {
							TownyMessaging.sendMsg(target, Translation.of("msg_you_have_been_given_rank", "Town", rank));
							plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
						}
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
						if (BukkitTools.isOnline(target.getName())) {
							TownyMessaging.sendMsg(target, Translation.of("msg_you_have_had_rank_taken", "Town", rank));
							plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
						}
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

	}

	public static void townSet(Player player, String[] split, boolean admin, Town town) throws TownyException, EconomyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town set"));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "board [message ... ]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "mayor " + Translation.of("town_help_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "homeblock", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "spawn/outpost/jail", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "perm ...", "'/town set perm' " + Translation.of("res_5")));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "[plottax/shoptax/embassytax] [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "[plotprice/shopprice/embassyprice] [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "spawncost [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "name [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "tag [upto 4 letters] or clear", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "title/surname [resident] [text]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "taxpercentcap [amount]", ""));
		} else {
			Resident resident;

			Nation nation = null;
			TownyWorld oldWorld = null;

			try {
				if (!admin) {
					resident = getResidentOrThrow(player.getUniqueId());
					town = resident.getTown();
				} else // Have the resident being tested be the mayor.
					resident = town.getMayor();

				if (town.hasNation())
					nation = town.getNation();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			if (split[0].equalsIgnoreCase("board")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /town set board " + Translation.of("town_help_9"));
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");
					
					if (!line.equals("none")) {
						if (!NameValidation.isValidString(line)) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_string_board_not_set"));
							return;
						}
						// TownyFormatter shouldn't be given any string longer than 159, or it has trouble splitting lines.
						if (line.length() > 159)
							line = line.substring(0, 159);
					} else 
						line = "";
					
					town.setBoard(line);
					TownyMessaging.sendTownBoard(player, town);
				}
			} else if (split[0].equalsIgnoreCase("title")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /town set title bilbo Jester ");
				else
					resident = getResidentOrThrow(split[1]);
				
				if (!admin && !CombatUtil.isSameTown(getResidentOrThrow(player.getUniqueId()), resident)) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_town", resident.getName()));
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
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_set_title", resident.getName(), resident.getTitle()));
				else
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_clear_title_surname", "Title", resident.getName()));

			} else if (split[0].equalsIgnoreCase("taxpercentcap")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAXPERCENTCAP.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (!town.isTaxPercentage()) {
					// msg_max_tax_amount_only_for_percent
					throw new TownyException(Translation.of("msg_max_tax_amount_only_for_percent"));
				}
				
				if (split.length < 2) {
					TownyMessaging.sendErrorMsg("Eg. /town set taxMax 10000");
					return;
				}
				
				double amount = Double.parseDouble(split[1]);
				town.setMaxPercentTaxAmount(amount);

				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_tax_max_percent_amount", player.getName(), TownyEconomyHandler.getFormattedBalance(town.getMaxPercentTaxAmount())));
				
			} else if (split[0].equalsIgnoreCase("surname")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /town set surname bilbo the dwarf ");
				else
					resident = getResidentOrThrow(split[1]);

				if (!admin && !CombatUtil.isSameTown(getResidentOrThrow(player.getUniqueId()), resident)) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_town", resident.getName()));
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
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_set_surname", resident.getName(), resident.getSurname()));
				else
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_clear_title_surname", "Surname", resident.getName()));


			} else {

				/*
				 * Test we have permission to use this command.
				 */
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split[0].equalsIgnoreCase("mayor")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set mayor Dumbo");
						return;
					} else
						try {
							if (!resident.isMayor())
								throw new TownyException(Translation.of("msg_not_mayor"));

							Resident oldMayor = town.getMayor();
							Resident newMayor = getResidentOrThrow(split[1]);
							if (!town.hasResident(split[1]))
								throw new TownyException(Translation.of("msg_err_mayor_doesnt_belong_to_town"));
							town.setMayor(newMayor);
							TownyPerms.assignPermissions(oldMayor, null);
							plugin.deleteCache(oldMayor.getName());
							plugin.deleteCache(newMayor.getName());
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_new_mayor", newMayor.getName()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_new_mayor", newMayor.getName()));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
							return;
						}

				} else if (split[0].equalsIgnoreCase("taxes")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set taxes 7");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							if (town.isTaxPercentage() && amount > 100) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_percentage"));
								return;
							}
							if (TownySettings.getTownDefaultTaxMinimumTax() > amount) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_tax_minimum_not_met", TownySettings.getTownDefaultTaxMinimumTax()));
								return;
							}
							town.setTaxes(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_town_set_tax", player.getName(), town.getTaxes()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_tax", player.getName(), town.getTaxes()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("plottax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set plottax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							town.setPlotTax(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_town_set_plottax", player.getName(), town.getPlotTax()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_plottax", player.getName(), town.getPlotTax()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}
				} else if (split[0].equalsIgnoreCase("shoptax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set shoptax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							town.setCommercialPlotTax(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_town_set_alttax", player.getName(), "shop", town.getCommercialPlotTax()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_alttax", player.getName(), "shop", town.getCommercialPlotTax()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("embassytax")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set embassytax 10");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							town.setEmbassyPlotTax(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_town_set_alttax", player.getName(), "embassy", town.getEmbassyPlotTax()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_alttax", player.getName(), "embassy", town.getEmbassyPlotTax()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("plotprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set plotprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							town.setPlotPrice(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_town_set_plotprice", player.getName(), town.getPlotPrice()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_plotprice", player.getName(), town.getPlotPrice()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("shopprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set shopprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							town.setCommercialPlotPrice(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_town_set_altprice", player.getName(), "shop", town.getCommercialPlotPrice()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_altprice", player.getName(), "shop", town.getCommercialPlotPrice()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}
				} else if (split[0].equalsIgnoreCase("embassyprice")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set embassyprice 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							town.setEmbassyPlotPrice(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_town_set_altprice", player.getName(), "embassy", town.getEmbassyPlotPrice()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_altprice", player.getName(), "embassy", town.getEmbassyPlotPrice()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("spawncost")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set spawncost 50");
						return;
					} else {
						try {
							Double amount = Double.parseDouble(split[1]);
							if (amount < 0) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
								return;
							}
							if (TownySettings.getSpawnTravelCost() < amount) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_cannot_set_spawn_cost_more_than", TownySettings.getSpawnTravelCost()));
								return;
							}
							town.setSpawnCost(amount);
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_spawn_cost_set_to", player.getName(), Translation.of("town_sing"), split[1]));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_spawn_cost_set_to", player.getName(), Translation.of("town_sing"), split[1]));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_num"));
							return;
						}
					}

				} else if (split[0].equalsIgnoreCase("name")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set name BillyBobTown");
						return;
					}
					
					if(NameValidation.isBlacklistName(split[1]))
						throw new TownyException(Translation.of("msg_invalid_name"));

                    if(TownyEconomyHandler.isActive() && TownySettings.getTownRenameCost() > 0) {
                		if (!town.getAccount().canPayFromHoldings(TownySettings.getTownRenameCost()))							
							throw new EconomyException(Translation.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())));

                    	final Town finalTown = town;
                    	final String name = split[1];
                    	Confirmation confirmation = Confirmation.runOnAccept(() -> {
							// Check if town can still pay rename cost
							if (!finalTown.getAccount().canPayFromHoldings(TownySettings.getTownRenameCost())) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())));
								return;
							}
							
							finalTown.getAccount().withdraw(TownySettings.getTownRenameCost(), String.format("Town renamed to: %s", name));

							townRename(player, finalTown, name);
						})
						.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())))
						.build();
                    	
                    	ConfirmationHandler.sendConfirmation(player, confirmation);
                    	
                    } else {
                    	townRename(player, town, split[1]);
                    }
				} else if (split[0].equalsIgnoreCase("tag")) {

					if (split.length < 2)
						throw new TownyException("Eg: /town set tag PLTC");
					else if (split[1].equalsIgnoreCase("clear")) {
						town.setTag(" ");
						if (admin)
							TownyMessaging.sendMessage(player, Translation.of("msg_reset_town_tag", player.getName()));
						TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_reset_town_tag", player.getName()));
					} else {
						town.setTag(NameValidation.checkAndFilterName(split[1]));
						if (admin)
							TownyMessaging.sendMessage(player, Translation.of("msg_set_town_tag", player.getName(), town.getTag()));
						TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_set_town_tag", player.getName(), town.getTag()));
					}
					
				} else if (split[0].equalsIgnoreCase("homeblock")) {

					Coord coord = Coord.parseCoord(player);
					TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());
					TownyWorld world;
					try {

						if (townBlock == null || townBlock.getTown() != town)
							throw new TownyException(Translation.of("msg_area_not_own"));

						world = TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName());
						final int minDistanceFromHomeblock = world.getMinDistanceFromOtherTowns(coord, resident.getTown());
						if (minDistanceFromHomeblock < TownySettings.getMinDistanceFromTownHomeblocks())
							throw new TownyException(Translation.of("msg_too_close2", Translation.of("homeblock")));

						if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
							if ((minDistanceFromHomeblock > TownySettings.getMaxDistanceBetweenHomeblocks()) && world.hasTowns())
								throw new TownyException(Translation.of("msg_too_far"));

						TownPreSetHomeBlockEvent preEvent = new TownPreSetHomeBlockEvent(town, townBlock, player);
						Bukkit.getPluginManager().callEvent(preEvent);
						if (preEvent.isCancelled()) 
							throw new TownyException(preEvent.getCancelMessage());
						
						// Test whether towns will be removed from the nation
						if (nation != null && TownySettings.getNationRequiresProximity() > 0) {
							// Do a dry-run of the proximity test.
							List<Town> removedTowns = nation.recheckTownDistanceDryRun(nation.getTowns(), town);
							
							// Oh no, some the nation will lose at least one town, better make a confirmation.
							if (!removedTowns.isEmpty()) {
								final Town finalTown = town;
								final TownBlock finalTB = townBlock;
								final Nation finalNation = nation;
								oldWorld = town.getHomeblockWorld();
								Confirmation confirmation = Confirmation.runOnAccept(() -> {
									try {
										// Set town homeblock and run the recheckTownDistance for real.
										finalTown.setHomeBlock(finalTB);
										finalTown.setSpawn(player.getLocation());
										finalNation.recheckTownDistance();
										TownyMessaging.sendMsg(player, Translation.of("msg_set_town_home", coord.toString()));
									} catch (TownyException e) {
										TownyMessaging.sendErrorMsg(player, e.getMessage());
										return;
									}
								})
									.setTitle(Translation.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")))
									.build();
								ConfirmationHandler.sendConfirmation(player, confirmation);

							// Phew, the nation won't lose any towns, let's do this.
							} else {
								oldWorld = town.getHomeblockWorld();
								town.setHomeBlock(townBlock);
								town.setSpawn(player.getLocation());		
								TownyMessaging.sendMsg(player, Translation.of("msg_set_town_home", coord.toString()));
							}
						// No nation to check proximity for/proximity isn't tested anyways.
						} else {
							oldWorld = town.getHomeblockWorld();
							town.setHomeBlock(townBlock);
							town.setSpawn(player.getLocation());
	
							TownyMessaging.sendMsg(player, Translation.of("msg_set_town_home", coord.toString()));

						}

					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("spawn")) {
					try {
						town.setSpawn(player.getLocation());
						TownyMessaging.sendMsg(player, Translation.of("msg_set_town_spawn"));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("outpost")) {

					try {
						TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());						
						if (townBlock == null || !townBlock.isOutpost())
							throw new TownyException(Translation.of("msg_err_location_is_not_within_an_outpost_plot"));
						
						if (townBlock.getTown().equals(town)) {
							town.addOutpostSpawn(player.getLocation());
							TownyMessaging.sendMsg(player, Translation.of("msg_set_outpost_spawn"));
						} else
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_not_own_area"));

					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("jail")) {

					try {
						town.addJailSpawn(player.getLocation());
						TownyMessaging.sendMsg(player, Translation.of("msg_set_jail_spawn"));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}

				} else if (split[0].equalsIgnoreCase("perm")) {

					// Make sure we are allowed to set these permissions.
					try {
						toggleTest(player, town, StringMgmt.join(split, " "));
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return;
					}
					String[] newSplit = StringMgmt.remFirstArg(split);
					setTownBlockOwnerPermissions(player, town, newSplit);

				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "town"));
					return;
				}
			}
			
			town.save();

			if (nation != null)
				nation.save();

			// If the town (homeblock) has moved worlds we need to update the
			// world files.
			if (oldWorld != null) {
				town.getHomeblockWorld().save();
				oldWorld.save();
			}
		}
	}

	public void townBuy(Player player, String[] split) {
		
		if (!TownyEconomyHandler.isActive()) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_no_economy"));
			return;
		}
		
		Resident resident;
		Town town;
		try {
			resident = getResidentOrThrow(player.getUniqueId());
			town = resident.getTown();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		
		if (!TownySettings.isSellingBonusBlocks(town) && !TownySettings.isBonusBlocksPerTownLevel()) {
			TownyMessaging.sendErrorMsg(player, "Config.yml has bonus blocks diabled at max_purchased_blocks: '0' ");
			return;
		} else if (TownySettings.isBonusBlocksPerTownLevel() && TownySettings.getMaxBonusBlocks(town) == 0) {
			TownyMessaging.sendErrorMsg(player, "Config.yml has bonus blocks disabled at town_level section: townBlockBonusBuyAmount: 0");
			return;
		}
			
		
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town buy"));
			String line = Colors.Yellow + "[Purchased Bonus] " + Colors.Green + "Cost: " + Colors.LightGreen + "%s" + Colors.Gray + " | " + Colors.Green + "Max: " + Colors.LightGreen + "%d";
			player.sendMessage(String.format(line, TownyEconomyHandler.getFormattedBalance(town.getBonusBlockCost()), TownySettings.getMaxPurchasedBlocks(town)));
			if (TownySettings.getPurchasedBonusBlocksIncreaseValue() != 1.0)
				player.sendMessage(Colors.Green + "Cost Increase per TownBlock: " + Colors.LightGreen + "+" +  new DecimalFormat("##.##%").format(TownySettings.getPurchasedBonusBlocksIncreaseValue()-1));
			player.sendMessage(ChatTools.formatCommand("", "/town buy", "bonus [n]", ""));
		} else {
			try {
				if (split[0].equalsIgnoreCase("bonus")) {
					if (split.length == 2) {
						try {
							townBuyBonusTownBlocks(town, Integer.parseInt(split[1].trim()), player);
						} catch (EconomyException e) {
							player.sendMessage(e.getMessage());
						}
					} else {
						throw new TownyException(Translation.of("msg_must_specify_amnt", "/town buy bonus"));
					}
				}
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		}
	}

	/**
	 * Town buys bonus blocks after checking the configured maximum.
	 *
	 * @param town - Towm object.
	 * @param inputN - Number of townblocks being bought.
	 * @param player - Player.
	 * @throws TownyException - Exception.
	 * @throws EconomyException - If the town cannot pay.
	 */
	public static void townBuyBonusTownBlocks(Town town, int inputN, Player player) throws EconomyException, TownyException {

		if (inputN < 0)
			throw new TownyException(Translation.of("msg_err_negative"));

		int current = town.getPurchasedBlocks();

		int n;
		if (current + inputN > TownySettings.getMaxPurchasedBlocks(town)) {
			n = TownySettings.getMaxPurchasedBlocks(town) - current;
		} else {
			n = inputN;
		}

		if (n == 0)
			return;
			double cost = town.getBonusBlockCostN(n);
			// Test if the town can pay and throw economy exception if not.
			if (!town.getAccount().canPayFromHoldings(cost))
				throw new EconomyException(Translation.of("msg_no_funds_to_buy", n, Translation.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost)));
		
		Confirmation confirmation = Confirmation.runOnAccept(() -> {
			if (!town.getAccount().withdraw(cost, String.format("Town Buy Bonus (%d)", n))) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_funds_to_buy", n, Translation.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost)));
				return;
			}
			town.addPurchasedBlocks(n);
			TownyMessaging.sendMsg(player, Translation.of("msg_buy", n, Translation.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost)));
			town.save();
		})
			.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost)))
			.build();
		ConfirmationHandler.sendConfirmation(player, confirmation);
	}

	/**
	 * Create a new town. Command: /town new [town]
	 *
	 * @param player - Player.
	 * @param name - name of town
	 * @param resident - The resident in charge of the town.
	 * @param noCharge - charging for creation - /ta town new NAME MAYOR has no charge.
	 */
	public static void newTown(Player player, String name, Resident resident, boolean noCharge) {
		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();

		PreNewTownEvent preEvent = new PreNewTownEvent(player, name);
		Bukkit.getPluginManager().callEvent(preEvent);
		
		if (preEvent.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
			return;
		}

		try {
			if (TownySettings.hasTownLimit() && TownyUniverse.getInstance().getDataSource().getTowns().size() >= TownySettings.getTownLimit())
				throw new TownyException(Translation.of("msg_err_universe_limit"));

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if ((filteredName == null) || dataSource.hasTown(filteredName))
				throw new TownyException(Translation.of("msg_err_invalid_name", name));
			
			if (resident.hasTown())
				throw new TownyException(Translation.of("msg_err_already_res", resident.getName()));

			TownyWorld world = dataSource.getWorld(player.getWorld().getName());

			if (!world.isUsingTowny())
				throw new TownyException(Translation.of("msg_set_use_towny_off"));

			if (!world.isClaimable())
				throw new TownyException(Translation.of("msg_not_claimable"));

			Coord key = Coord.parseCoord(player);

			if (!TownyAPI.getInstance().isWilderness(player.getLocation()))
				throw new TownyException(Translation.of("msg_already_claimed_1", key));
			
			if ((world.getMinDistanceFromOtherTownsPlots(key) < TownySettings.getMinDistanceFromTownPlotblocks()))
				throw new TownyException(Translation.of("msg_too_close2", Translation.of("townblock")));

			final int minDistFromOtherTowns = world.getMinDistanceFromOtherTowns(key);
			if (minDistFromOtherTowns < TownySettings.getMinDistanceFromTownHomeblocks())
				throw new TownyException(Translation.of("msg_too_close2", Translation.of("homeblock")));

			if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
				if ((minDistFromOtherTowns > TownySettings.getMaxDistanceBetweenHomeblocks()) && world.hasTowns())
					throw new TownyException(Translation.of("msg_too_far"));

			// If the town isn't free to make, send a confirmation.
			if (!noCharge && TownyEconomyHandler.isActive()) { 
				// Test if the resident can afford the town.
				if (!resident.getAccount().canPayFromHoldings(TownySettings.getNewTownPrice()))
					throw new TownyException(Translation.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translation.of("msg_you") : resident.getName()), TownySettings.getNewTownPrice()));
				
				Confirmation.runOnAccept(() -> {			
					// Make the resident pay here.
					if (!resident.getAccount().withdraw(TownySettings.getNewTownPrice(), "New Town Cost")) {
						// Send economy message
						TownyMessaging.sendErrorMsg(player,Translation.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translation.of("msg_you") : resident.getName()), TownySettings.getNewTownPrice()));
						return;
					}
					
					try {
						// Make town.
						newTown(world, name, resident, key, player.getLocation(), player);
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						e.printStackTrace();
					}
					TownyMessaging.sendGlobalMessage(Translation.of("msg_new_town", player.getName(), StringMgmt.remUnderscore(name)));
				})
					.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNewTownPrice())))
					.sendTo(player);

			// Or, if the town doesn't cost money to create, just make the Town.
			} else {
				newTown(world, name, resident, key, player.getLocation(), player);
				TownyMessaging.sendGlobalMessage(Translation.of("msg_new_town", player.getName(), StringMgmt.remUnderscore(name)));
			}
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			// TODO: delete town data that might have been done
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Town newTown(TownyWorld world, String name, Resident resident, Coord key, Location spawn, Player player) throws TownyException {

		TownyUniverse.getInstance().newTown(name);
		Town town = TownyUniverse.getInstance().getTown(name);
		
		// This should never happen
		if (town == null)
			throw new TownyException(String.format("Error fetching new town from name '%s'", name));

		town.setRegistered(System.currentTimeMillis());
		resident.setTown(town);
		town.setMayor(resident);
		TownBlock townBlock = new TownBlock(key.getX(), key.getZ(), world);
		townBlock.setTown(town);

		// Set the plot permissions to mirror the towns.
		townBlock.setType(townBlock.getType());
		town.setSpawn(spawn);

		if (world.isUsingPlotManagementRevert()) {
			PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
			if (plotChunk != null) {

				TownyRegenAPI.deletePlotChunk(plotChunk); // just claimed so stop regeneration.

			} else {

				plotChunk = new PlotBlockData(townBlock); // Not regenerating so create a new snapshot.
				plotChunk.initialize();

			}
			TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save a snapshot.
			plotChunk = null;
		}
		if (TownyEconomyHandler.isActive()) {
			TownyMessaging.sendDebugMsg("Creating new Town account: " + TownySettings.getTownAccountPrefix() + name);
			try {
				town.getAccount().setBalance(0, "Setting 0 balance for Town");
			} catch (EconomyException e) {
				e.printStackTrace();
			} catch (NullPointerException e1) {
				throw new TownyException("The server economy plugin " + TownyEconomyHandler.getVersion() + " could not return the Town account!");
			}
		}
		
		resident.save();
		townBlock.save();
		town.save();
		world.save();

		// Reset cache permissions for anyone in this TownBlock
		plugin.updateCache(townBlock.getWorldCoord());

		BukkitTools.getPluginManager().callEvent(new NewTownEvent(town));

		return town;
	}

	public static void townRename(Player player, Town town, String newName) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		TownPreRenameEvent event = new TownPreRenameEvent(town, newName);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_rename_cancelled"));
			return;
		}

		try {
			townyUniverse.getDataSource().renameTown(town, newName);
			town = townyUniverse.getTown(newName);
			// This should never happen
			if (town == null)
				throw new TownyException("Error renaming town! Cannot fetch town with new name " + newName);
			
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_name", player.getName(), town.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void townLeave(Player player) {

		try {
			Resident resident = getResidentOrThrow(player.getUniqueId());
			
			if (!resident.hasTown())
				throw new TownyException(Translation.of("msg_err_dont_belong_town"));
			
			Town town = resident.getTown();
			
			if (resident.isMayor())
				throw new TownyException(Translation.of("msg_mayor_abandon"));
			
			if (resident.isJailed() && TownySettings.JailDeniesTownLeave() && resident.getJailTown().equals(town.getName()))
				throw new TownyException(Translation.of("msg_cannot_abandon_town_while_jailed"));
			
			TownLeaveEvent event = new TownLeaveEvent(resident, town);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) 
				throw new TownyException(event.getCancelMessage());

			Confirmation.runOnAccept(() -> {
				if (resident.isJailed() && resident.getJailTown().equals(town.getName())) {
					resident.setJailed(false);
					resident.setJailSpawn(0);
					resident.setJailTown("");
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_escaped_jail_by_leaving_town", resident.getName()));
				}

				try {
					townRemoveResident(town, resident);
				} catch (NotRegisteredException ignored) {}

				// Reset everyones cache permissions as this player leaving could affect
				// multiple areas
				plugin.resetCache();

				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_left_town", resident.getName()));
				TownyMessaging.sendMsg(player, Translation.of("msg_left_town", resident.getName()));

				try {
					checkTownResidents(town, resident);
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}
			}).sendTo(player);

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

	}

	/**
	 * Wrapper for the townSpawn() method. All calls should be through here
	 * unless bypassing for admins.
	 *
	 * @param player - Player.
	 * @param split  - Current command arguments.
	 * @param outpost - Whether this in an outpost or not.
	 * @param ignoreWarning - Whether to ignore cost warning and pay automatically.
	 * @throws TownyException - Exception.
	 */
	public static void townSpawn(Player player, String[] split, Boolean outpost, boolean ignoreWarning) throws TownyException{

		if ((split.length == 1 && split[0].equals("-ignore")) || (split.length > 1 && split[1].equals("-ignore"))) {
			ignoreWarning = true;
		}
		

		Resident resident = getResidentOrThrow(player.getUniqueId());
		Town town;
		String notAffordMSG;

		// Set target town and affiliated messages.
		if (split.length == 0 || outpost || split[0].equals("-ignore")) {

			if (!resident.hasTown()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_dont_belong_town"));
				return;
			}

			town = resident.getTown();
			notAffordMSG = Translation.of("msg_err_cant_afford_tp");

		} else {
			// split.length > 1
			town = TownyUniverse.getInstance().getTown(split[0]);
			
			if (town == null)
				throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
			
			notAffordMSG = Translation.of("msg_err_cant_afford_tp_town", town.getName());
		}
			
		SpawnUtil.sendToTownySpawn(player, split, town, notAffordMSG, outpost, ignoreWarning, SpawnType.TOWN);

	}

	public void townDelete(Player player, String[] split) {

		final Town town;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			try {
				Resident resident = getResidentOrThrow(player.getUniqueId());

				if (TownRuinSettings.getTownRuinsEnabled()) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_warning_town_ruined_if_deleted", TownRuinSettings.getTownRuinsMaxDurationHours()));
					if (TownRuinSettings.getTownRuinsReclaimEnabled())
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_warning_town_ruined_if_deleted2", TownRuinSettings.getTownRuinsMinDurationHours()));
				}
				town = resident.getTown();
				Confirmation.runOnAccept(() -> {
					TownyUniverse.getInstance().getDataSource().removeTown(town);
				})
					.sendTo(player);
				
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		} else {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode())) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_admin_only_delete_town"));
				return;
			}
			
			town = townyUniverse.getTown(split[0]);
			
			if (town == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[0]));
				return;
			}

			townyUniverse.getDataSource().removeTown(town);
		}

	}

	/**
	 * Transforms a list of names into a list of residents to be kicked.
	 * Command: /town kick [resident] .. [resident]
	 *
	 * @param player - Player who initiated the kick command.
	 * @param names - List of names to kick.
	 */
	public static void townKick(Player player, String[] names) {

		Resident resident;
		Town town;
		try {
			resident = getResidentOrThrow(player.getUniqueId());
			town = resident.getTown();
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		townKickResidents(player, resident, town, ResidentUtil.getValidatedResidents(player, names));

		// Reset everyones cache permissions as this player leaving can affect
		// multiple areas.
		plugin.resetCache();
	}

	public static void townAddResidents(CommandSender sender, Town town, List<Resident> invited) {
		String name;
		boolean admin = false;
		if (sender instanceof Player) {
			name = ((Player) sender).getName();
			if (TownyUniverse.getInstance().getPermissionSource().isTownyAdmin((Player) sender))
				admin = true;				
		} else {
			name = "Console";
			admin = true;
		}

		for (Resident newMember : new ArrayList<>(invited)) {
			try {

				if (!admin) {
					TownPreAddResidentEvent preEvent = new TownPreAddResidentEvent(town, newMember);
					Bukkit.getPluginManager().callEvent(preEvent);
	
					if (preEvent.isCancelled()) {
						TownyMessaging.sendErrorMsg(sender, preEvent.getCancelMessage());
						return;
					}
				}
				
				// only add players with the right permissions.
				if (BukkitTools.matchPlayer(newMember.getName()).isEmpty()) { // Not
																				// online
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_offline_no_join", newMember.getName()));
					invited.remove(newMember);
				} else if (!TownyUniverse.getInstance().getPermissionSource().testPermission(BukkitTools.getPlayer(newMember.getName()), PermissionNodes.TOWNY_TOWN_RESIDENT.getNode())) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_not_allowed_join", newMember.getName()));
					invited.remove(newMember);
				} else if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsPerTown()){
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsPerTown()));
					invited.remove(newMember);
				} else if (!admin && TownySettings.getTownInviteCooldown() > 0 && ( (System.currentTimeMillis()/1000 - newMember.getRegistered()/1000) < (TownySettings.getTownInviteCooldown()) )) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_resident_doesnt_meet_invite_cooldown", newMember));
					invited.remove(newMember);
				} else if (TownySettings.getMaxNumResidentsWithoutNation() > 0 && !town.hasNation() && town.getResidents().size() >= TownySettings.getMaxNumResidentsWithoutNation()) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_unable_to_add_more_residents_without_nation", TownySettings.getMaxNumResidentsWithoutNation()));
					invited.remove(newMember);
				} else {
					town.addResidentCheck(newMember);
					townInviteResident(sender, town, newMember);
				}
			} catch (TownyException e) {
				invited.remove(newMember);
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}
		}

		if (invited.size() > 0) {
			StringBuilder msg = new StringBuilder();
			for (Resident newMember : invited)
				msg.append(newMember.getName()).append(", ");

			msg = new StringBuilder(msg.substring(0, msg.length() - 2));


			msg = new StringBuilder(Translation.of("msg_invited_join_town", name, msg.toString()));
			TownyMessaging.sendPrefixedTownMessage(town, msg.toString());
			town.save();
		} else
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
	}

	public static void townAddResident(Town town, Resident resident) throws AlreadyRegisteredException {
		// If player is outlawed in target town, remove them from outlaw list.
		if (town.hasOutlaw(resident))
			town.removeOutlaw(resident);

		resident.setTown(town);
		plugin.deleteCache(resident.getName());
		resident.save();
		town.save();
	}

	private static void townInviteResident(CommandSender sender,Town town, Resident newMember) throws TownyException {

		PlayerJoinTownInvite invite = new PlayerJoinTownInvite(sender, newMember, town);
		try {
			if (!InviteHandler.inviteIsActive(invite)) {
				newMember.newReceivedInvite(invite);
				town.newSentInvite(invite);
				InviteHandler.addInvite(invite);
				Player player = TownyAPI.getInstance().getPlayer(newMember);
				if (player != null)
					TownyMessaging.sendRequestMessage(player,invite);
				Bukkit.getPluginManager().callEvent(new TownInvitePlayerEvent(invite));
			} else {
				throw new TownyException(Translation.of("msg_err_player_already_invited", newMember.getName()));
			}
		} catch (TooManyInvitesException e) {
			newMember.deleteReceivedInvite(invite);
			town.deleteSentInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	private static void townRevokeInviteResident(Object sender, Town town, List<Resident> residents) {

		for (Resident invited : residents) {
			if (InviteHandler.inviteIsActive(town, invited)) {
				for (Invite invite : invited.getReceivedInvites()) {
					if (invite.getSender().equals(town)) {
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

	public static void townRemoveResident(Town town, Resident resident) throws NotRegisteredException {

		if (!town.hasResident(resident))
			throw new NotRegisteredException();
		resident.removeTown();

	}
	
	/**
	 * Method for kicking residents from a town.
	 * 
	 * @param sender - CommandSender who initiated the kick.
	 * @param resident - Resident who initiated the kick.
	 * @param town - Town the list of Residents are being kicked from.
	 * @param kicking - List of Residents being kicked from Towny.
	 */
	public static void townKickResidents(Object sender, Resident resident, Town town, List<Resident> kicking) {

		Player player = null;

		if (sender instanceof Player)
			player = (Player) sender;

		for (Resident member : new ArrayList<>(kicking)) {
			if (!town.getResidents().contains(member)) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_resident_not_your_town"));
				kicking.remove(member);
				continue;
			}
			if (resident == member) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_you_cannot_kick_yourself"));
				kicking.remove(member);
				continue;
			}
			if (member.isMayor() || town.hasResidentWithRank(member, "assistant")) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_you_cannot_kick_this_resident", member));
				kicking.remove(member);
				continue;
			} else {
				try {
					townRemoveResident(town, member);
				} catch (NotRegisteredException e) {
					kicking.remove(member);
				}
			}
		}
		
		if (kicking.size() > 0) {
			StringBuilder msg = new StringBuilder();
			for (Resident member : kicking) {
				msg.append(member.getName()).append(", ");
				Player p = BukkitTools.getPlayer(member.getName());
				if (p != null)
					p.sendMessage(Translation.of("msg_kicked_by", (player != null) ? player.getName() : "CONSOLE"));
			}
			msg = new StringBuilder(msg.substring(0, msg.length() - 2));
			msg = new StringBuilder(Translation.of("msg_kicked", (player != null) ? player.getName() : "CONSOLE", msg.toString()));
			TownyMessaging.sendPrefixedTownMessage(town, msg.toString());
			try {
				Resident playerRes = getResidentOrThrow(player.getUniqueId());
				if (!(sender instanceof Player) || !playerRes.hasTown() || !playerRes.getTown().equals(town))
					// For when the an admin uses /ta town {name} kick {residents}
					TownyMessaging.sendMessage(sender, msg.toString());
			} catch (NotRegisteredException e) {
			}
			town.save();
		} else {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
		}

		try {
			checkTownResidents(town, resident);
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}
	}

	public static void checkTownResidents(Town town, Resident removedResident) throws NotRegisteredException {
		if (!town.hasNation())
			return;
		Nation nation = town.getNation();
		if ((town.isCapital()) && (TownySettings.getNumResidentsCreateNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			for (Town newCapital : town.getNation().getTowns())
				if (newCapital.getNumResidents() >= TownySettings.getNumResidentsCreateNation()) {
					town.getNation().setCapital(newCapital);
					if ((TownySettings.getNumResidentsJoinNation() > 0) && (removedResident.getTown().getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
						town.removeNation();
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_capital_not_enough_residents_left_nation", town.getName()));
					}
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_not_enough_residents_no_longer_capital", newCapital.getName()));
					return;
				}
			TownyMessaging.sendPrefixedNationMessage(town.getNation(), Translation.of("msg_nation_disbanded_town_not_enough_residents", town.getName()));
			TownyMessaging.sendGlobalMessage(Translation.of("MSG_DEL_NATION", town.getNation()));
			TownyUniverse.getInstance().getDataSource().removeNation(town.getNation());

			if (TownyEconomyHandler.isActive() && TownySettings.isRefundNationDisbandLowResidents()) {
				town.getAccount().deposit(TownySettings.getNewNationPrice(), "nation refund");
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_not_enough_residents_refunded", TownySettings.getNewNationPrice()));
			}
		} else if ((!town.isCapital()) && (TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_town_not_enough_residents_left_nation", town.getName()));
			town.removeNation();
		}
	}

	/**
	 * If no arguments are given (or error), send usage of command. If sender is
	 * a player: args = [town]. Elsewise: args = [resident] [town]
	 *
	 * @param sender - Sender of command.
	 * @param args - Current command arguments.
	 */
	public static void parseTownJoin(CommandSender sender, String[] args) {

		try {
			Resident resident;
			Town town;
			String residentName, townName, contextualResidentName;
			boolean console = false;
			String exceptionMsg;

			if (sender instanceof Player) {
				// Player
				if (args.length < 1)
					throw new Exception(String.format("Usage: /town join [town]"));

				Player player = (Player) sender;
				residentName = player.getName();
				townName = args[0];
				contextualResidentName = "You";
				exceptionMsg = "msg_err_already_res2";
			} else {
				// Console
				if (args.length < 2)
					throw new Exception(String.format("Usage: town join [resident] [town]"));

				residentName = args[0];
				townName = args[1];
				contextualResidentName = residentName;
				exceptionMsg = "msg_err_already_res";
			}
			
			resident = TownyUniverse.getInstance().getResident(residentName);
			town = TownyUniverse.getInstance().getTown(townName);
			
			if (resident == null || town == null) {
				throw new Exception(Translation.of("msg_err_not_registered_1", resident == null ? residentName : townName));
			}

			// Check if resident is currently in a town.
			if (resident.hasTown())
				throw new Exception(Translation.of(exceptionMsg, contextualResidentName));

			if (!console) {
				// Check if town is town is free to join.
				if (!town.isOpen())
					throw new Exception(Translation.of("msg_err_not_open", town.getFormattedName()));
				if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsPerTown())
					throw new Exception(Translation.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsPerTown()));
				if (TownySettings.getMaxNumResidentsWithoutNation() > 0 && !town.hasNation() && town.getResidents().size() >= TownySettings.getMaxNumResidentsWithoutNation())
					throw new Exception(Translation.of("msg_err_unable_to_add_more_residents_without_nation", TownySettings.getMaxNumResidentsWithoutNation()));
				if (town.hasOutlaw(resident))
					throw new Exception(Translation.of("msg_err_outlaw_in_open_town"));
			}

			TownPreAddResidentEvent preEvent = new TownPreAddResidentEvent(town, resident);
			Bukkit.getPluginManager().callEvent(preEvent);

			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(sender, preEvent.getCancelMessage());
				return;
			}

			// Check if player is already in selected town (Pointless)
			// Then add player to town.
			townAddResident(town, resident);

			// Resident was added successfully.
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_join_town", resident.getName()));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 *
	 * @param sender - Sender.
	 * @param specifiedTown - Town to add to if not null.
	 * @param names - Names to add.
	 * @throws TownyException - General Exception, or if Town's spawn has not been set
	 */
	public static void townAdd(CommandSender sender, Town specifiedTown, String[] names) throws TownyException {

		String name;
		if (sender instanceof Player) {
			name = ((Player) sender).getName();
		} else {
			name = "Console";
		}
		Resident resident;
		Town town;
		try {
			if (name.equalsIgnoreCase("Console")) {
				town = specifiedTown;
			} else {
				resident = getResidentOrThrow(name);
				if (specifiedTown == null)
					town = resident.getTown();
				else
					town = specifiedTown;
			}

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage());
			return;
		}

		if (town.isBankrupt())
			throw new TownyException(Translation.of("msg_err_bankrupt_town_cannot_invite"));

		if (TownySettings.getMaxDistanceFromTownSpawnForInvite() != 0) {

			if (!town.hasSpawn())
				throw new TownyException(Translation.of("msg_err_townspawn_has_not_been_set"));
		
			Location spawnLoc = town.getSpawn();
			ArrayList<String> newNames = new ArrayList<String>();
			for (String nameForDistanceTest : names) {
				
				int maxDistance = TownySettings.getMaxDistanceFromTownSpawnForInvite();
				Player player = BukkitTools.getPlayer(nameForDistanceTest);
				Location playerLoc = player.getLocation();
				Double distance = spawnLoc.distance(playerLoc);
				if (distance <= maxDistance)
					newNames.add(nameForDistanceTest);
				else {
					TownyMessaging.sendMessage(sender, Translation.of("msg_err_player_too_far_from_town_spawn", nameForDistanceTest, maxDistance));
				}
			}
			names = newNames.toArray(new String[0]);
		}
		List<String> reslist = new ArrayList<>(Arrays.asList(names));
		// Our Arraylist is above
		List<String> newreslist = new ArrayList<>();
		// The list of valid invites is above, there are currently none
		List<String> removeinvites = new ArrayList<>();
		// List of invites to be removed;
		for (String resName : reslist) {
			if (resName.startsWith("-")) {
				removeinvites.add(resName.substring(1));
				// Add to removing them, remove the "-"
			} else {
				if (!town.hasResident(resName))
					newreslist.add(resName);// add to adding them,
				else 
					removeinvites.add(resName);
			}
		}
		names = newreslist.toArray(new String[0]);
		String[] namestoremove = removeinvites.toArray(new String[0]);
		if (namestoremove.length != 0) {
			List<Resident> toRevoke = getValidatedResidentsForInviteRevoke(sender, namestoremove, town);
			if (!toRevoke.isEmpty())
				townRevokeInviteResident(sender,town, toRevoke);
		}

		if (names.length != 0) {
			townAddResidents(sender, town, ResidentUtil.getValidatedResidents(sender, names));
		}

		// Reset this players cached permissions
		if (!name.equalsIgnoreCase("Console"))
			plugin.resetCache(BukkitTools.getPlayerExact(name));
	}

	// wrapper function for non friend setting of perms
	public static void setTownBlockOwnerPermissions(Player player, TownBlockOwner townBlockOwner, String[] split) {

		setTownBlockPermissions(player, townBlockOwner, townBlockOwner.getPermissions(), split, false);
	}

	public static void setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownyPermission perm, String[] split, boolean friend) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			if (townBlockOwner instanceof Town)
				player.sendMessage(ChatTools.formatCommand("Level", "[resident/nation/ally/outsider]", "", ""));
			if (townBlockOwner instanceof Resident)
				player.sendMessage(ChatTools.formatCommand("Level", "[friend/town/ally/outsider]", "", ""));
			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
			if (townBlockOwner instanceof Town)
				player.sendMessage(ChatTools.formatCommand("Eg", "/town set perm", "ally off", ""));
			if (townBlockOwner instanceof Resident)
				player.sendMessage(ChatTools.formatCommand("Eg", "/resident set perm", "friend build on", ""));

		} else {

			// reset the friend to resident so the perm settings don't fail
			if (friend && split[0].equalsIgnoreCase("friend"))
				split[0] = "resident";
			// reset the town to nation so the perm settings don't fail
			if (friend && split[0].equalsIgnoreCase("town"))
				split[0] = "nation";

			if (split.length == 1) {

				if (split[0].equalsIgnoreCase("reset")) {

					// reset all townBlock permissions (by town/resident)
					for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {

						if (((townBlockOwner instanceof Town) && (!townBlock.hasResident())) || ((townBlockOwner instanceof Resident) && (townBlock.hasResident()))) {

							// Reset permissions
							townBlock.setType(townBlock.getType());
							townBlock.save();
						}
					}
					if (townBlockOwner instanceof Town)
						TownyMessaging.sendMsg(player, Translation.of("msg_set_perms_reset", "Town owned"));
					else
						TownyMessaging.sendMsg(player, Translation.of("msg_set_perms_reset", "your"));

					// Reset all caches as this can affect everyone.
					plugin.resetCache();
					return;

				} else {
					// Set all perms to On or Off
					// '/town set perm off'

					try {
						boolean b = StringMgmt.parseOnOff(split[0]);
						
						perm.change(TownyPermissionChange.Action.ALL_PERMS, b);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_town_set_perm_syntax_error"));
						return;
					}
				}

			} else if (split.length == 2) {
				boolean b;

				try {
					b = StringMgmt.parseOnOff(split[1]);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_town_set_perm_syntax_error"));
					return;
				}

				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";
				else if (split[0].equalsIgnoreCase("town"))
					split[0] = "nation";
				else if (split[0].equalsIgnoreCase("itemuse"))
					split[0] = "item_use";

				// Check if it is a perm level first
				try {
					TownyPermission.PermLevel permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase());

					perm.change(TownyPermissionChange.Action.PERM_LEVEL, b, permLevel);
				}
				catch (IllegalArgumentException permLevelException) {
					// If it is not a perm level, then check if it is a action type
					try {
						TownyPermission.ActionType actionType = TownyPermission.ActionType.valueOf(split[0].toUpperCase());

						perm.change(TownyPermissionChange.Action.ACTION_TYPE, b, actionType);
					} catch (IllegalArgumentException actionTypeException) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_town_set_perm_syntax_error"));
						return;
					}
				}

			} else if (split.length == 3) {
				// Reset the friend to resident so the perm settings don't fail
				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";

					// reset the town to nation so the perm settings don't fail
				else if (split[0].equalsIgnoreCase("town"))
					split[0] = "nation";

				if (split[1].equalsIgnoreCase("itemuse"))
					split[1] = "item_use";

				TownyPermission.PermLevel permLevel;
				TownyPermission.ActionType actionType;

				try {
					permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase());
					actionType = TownyPermission.ActionType.valueOf(split[1].toUpperCase());
				} catch (IllegalArgumentException ignore) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_town_set_perm_syntax_error"));
					return;
				}
				
				try {
					boolean b = StringMgmt.parseOnOff(split[2]);

					perm.change(TownyPermissionChange.Action.SINGLE_PERM, b, permLevel, actionType);

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_town_set_perm_syntax_error"));
					return;
				}
			}

			// Propagate perms to all unchanged townblocks
			for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {
				if ((townBlockOwner instanceof Town) && (!townBlock.hasResident())) {
					if (!townBlock.isChanged()) {
						townBlock.setType(townBlock.getType());
						townBlock.save();
					}
				} else if (townBlockOwner instanceof Resident)
					if (!townBlock.isChanged()) {
						townBlock.setType(townBlock.getType());
						townBlock.save();
					}
			}

			TownyMessaging.sendMsg(player, Translation.of("msg_set_perms"));
			TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
			TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString2().replace("n", "t") : perm.getColourString2().replace("f", "r"))));
			TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

			// Reset all caches as this can affect everyone.
			plugin.resetCache();
		}
	}

	public static void parseTownClaimCommand(Player player, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town claim"));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "", Translation.of("msg_block_claim")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "outpost", Translation.of("mayor_help_3")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "[auto]", Translation.of("mayor_help_5")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "[circle/rect] [radius]", Translation.of("mayor_help_4")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "[circle/rect] auto", Translation.of("mayor_help_5")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {

				resident = getResidentOrThrow(player.getUniqueId());
				town = resident.getTown();

				if (town.isBankrupt())
					throw new TownyException(Translation.of("msg_err_bankrupt_town_cannot_claim"));

				world = TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName());

				if (!world.isUsingTowny())
					throw new TownyException(Translation.of("msg_set_use_towny_off"));
				
				if (!world.isClaimable())
					throw new TownyException(Translation.of("msg_not_claimable"));

				List<WorldCoord> selection;
				boolean outpost = false;
				boolean isAdmin = permSource.isTownyAdmin(player);
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());

				/*
				 * Make initial selection of WorldCoord(s)
				 */
				if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {

					if (TownySettings.isAllowingOutposts()) {
						if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						
						// Run various tests required by configuration/permissions through Util.
						OutpostUtil.OutpostTests(town, resident, world, key, isAdmin, false);
						
						if (!TownyAPI.getInstance().isWilderness(plugin.getCache(player).getLastLocation()))
							throw new TownyException(Translation.of("msg_already_claimed_1", key));

						// Select a single WorldCoord using the AreaSelectionUtil.
						selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), new String[0]);
						outpost = true;
					} else
						throw new TownyException(Translation.of("msg_outpost_disable"));
				} else {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					// Select the area, can be one or many.
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), split);
					
					if ((selection.size() > 1) && (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN_MULTIPLE.getNode())))
						throw new TownyException(Translation.of("msg_err_command_disable"));
				}

				// Not enough available claims.
				if (selection.size() > TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size())
					throw new TownyException(Translation.of("msg_err_not_enough_blocks"));

				// If this is a single claim and it is already claimed, by someone else.
				if (selection.size() == 1 && selection.get(0).hasTownBlock() && selection.get(0).getTownBlock().hasTown())
					throw new TownyException(Translation.of("msg_already_claimed", selection.get(0).getTownBlock().getTown()));
				
				/*
				 * Filter out any unallowed claims.
				 */
				TownyMessaging.sendDebugMsg("townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				
				// Filter out townblocks already owned.
				selection = AreaSelectionUtil.filterOutTownOwnedBlocks(selection);
				if (selection.isEmpty())
					throw new TownyException(Translation.of("msg_err_empty_area_selection"));

				// Filter out townblocks too close to another Town's homeblock.
				selection = AreaSelectionUtil.filterInvalidProximityToHomeblock(selection, town);
				if (selection.isEmpty())
					throw new TownyException(Translation.of("msg_too_close2", Translation.of("homeblock")));

				// Filter out townblocks too close to other Towns' normal townblocks.
				selection = AreaSelectionUtil.filterInvalidProximityTownBlocks(selection, town);
				if (selection.isEmpty())
					throw new TownyException(Translation.of("msg_too_close2", Translation.of("townblock")));
				
				TownyMessaging.sendDebugMsg("townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				
				// When not claiming an outpost, make sure at least one of the selection is attached to a claimed plot.
				if (!outpost && !isEdgeBlock(town, selection) && !town.getTownBlocks().isEmpty())
					throw new TownyException(Translation.of("msg_err_not_attached_edge"));
								
				/*
				 * Allow other plugins to have a say in whether the claim is allowed.
				 */
				int blockedClaims = 0;

				String cancelMessage = "";
				boolean isHomeblock = town.getTownBlocks().size() == 0;
				for (WorldCoord coord : selection) {
					//Use the user's current world
					TownPreClaimEvent preClaimEvent = new TownPreClaimEvent(town, new TownBlock(coord.getX(), coord.getZ(), world), player, outpost, isHomeblock);
					BukkitTools.getPluginManager().callEvent(preClaimEvent);
					if(preClaimEvent.isCancelled()) {
						blockedClaims++;
						cancelMessage = preClaimEvent.getCancelMessage();
					}
				}

				if (blockedClaims > 0) {
					throw new TownyException(String.format(cancelMessage, blockedClaims, selection.size()));
				}
				
				/*
				 * See if the Town can pay (if required.)
				 */
				if (TownyEconomyHandler.isActive()) {
					double blockCost = 0;
					try {					
						if (outpost)
							blockCost = TownySettings.getOutpostCost();
						else if (selection.size() == 1)
							blockCost = town.getTownBlockCost();
						else
							blockCost = town.getTownBlockCostN(selection.size());
	
						double missingAmount = blockCost - town.getAccount().getHoldingBalance();
						if (!town.getAccount().canPayFromHoldings(blockCost))
							throw new TownyException(Translation.of("msg_no_funds_claim2", selection.size(), TownyEconomyHandler.getFormattedBalance(blockCost),  TownyEconomyHandler.getFormattedBalance(missingAmount), new DecimalFormat("#").format(missingAmount)));
						town.getAccount().withdraw(blockCost, String.format("Town Claim (%d)", selection.size()));
					} catch (EconomyException e1) {
						throw new TownyException("Economy Error");
					} catch (NullPointerException e2) {
						throw new TownyException("The server economy plugin " + TownyEconomyHandler.getVersion() + " could not return the Town account!");
					}
				}
				
				/*
				 * Actually start the claiming process.
				 */
				new TownClaim(plugin, player, town, selection, outpost, true, false).start();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public static void parseTownUnclaimCommand(Player player, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town unclaim"));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town unclaim", "", Translation.of("mayor_help_6")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town unclaim", "[circle/rect] [radius]", Translation.of("mayor_help_7")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town unclaim", "all", Translation.of("mayor_help_8")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				resident = getResidentOrThrow(player.getUniqueId());
				town = resident.getTown();
				world = TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName());

				TownPreUnclaimCmdEvent event = new TownPreUnclaimCmdEvent(town, resident, world);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled())
					throw new TownyException(event.getCancelMessage());
				
				if (split.length == 1 && split[0].equalsIgnoreCase("all")) {
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM_ALL.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					new TownClaim(plugin, player, town, null, false, false, false).start();
					// townUnclaimAll(town);
					// If the unclaim code knows its an outpost or not, doesnt matter its only used once the world deletes the townblock, where it takes the value from the townblock.
					// Which is why in AreaSelectionUtil, since outpost is not parsed in the main claiming of a section, it is parsed in the unclaiming with the circle, rect & all options.
				} else {
					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), Coord.parseCoord(plugin.getCache(player).getLastLocation())), split);
					selection = AreaSelectionUtil.filterOwnedBlocks(town, selection);
					if (selection.isEmpty())
						throw new TownyException(Translation.of("msg_err_empty_area_selection"));

					if (selection.get(0).getTownBlock().isHomeBlock())
						throw new TownyException(Translation.of("msg_err_cannot_unclaim_homeblock"));
					
					if (AreaSelectionUtil.filterHomeBlock(town, selection)) {
						// Do not stop the entire unclaim, just warn that the homeblock cannot be unclaimed
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_cannot_unclaim_homeblock"));
					}
					
					// Set the area to unclaim
					new TownClaim(plugin, player, town, selection, false, false, false).start();

					TownyMessaging.sendMsg(player, Translation.of("msg_abandoned_area", Arrays.toString(selection.toArray(new WorldCoord[0]))));
				}

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, List<WorldCoord> worldCoords) {

		// TODO: Better algorithm that doesn't duplicates checks.

		for (WorldCoord worldCoord : worldCoords)
			if (isEdgeBlock(owner, worldCoord))
				return true;
		return false;
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, WorldCoord worldCoord) {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				if (edgeTownBlock.isOwner(owner)) {
					TownyMessaging.sendDebugMsg("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = True.");
					return true;
				}
			} catch (NotRegisteredException e) {
			}
		TownyMessaging.sendDebugMsg("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = False.");
		return false;
	}

	public static List<Resident> getValidatedResidentsForInviteRevoke(Object sender, String[] names, Town town) {
		List<Resident> toRevoke = new ArrayList<>();
		for (Invite invite : town.getSentInvites()) {
			for (String name : names) {
				if (invite.getReceiver().getName().equalsIgnoreCase(name)) {
					Resident revokeRes = TownyUniverse.getInstance().getResident(name);
					if (revokeRes != null) {
						toRevoke.add(revokeRes);
					}
				}
			}
			
		}
		return toRevoke;		
	}
	
	private static void townTransaction(Player player, String[] args, boolean withdraw) {
		try {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident == null || !resident.hasTown())
				throw new TownyException(Translation.of("msg_err_dont_belong_town"));
			
			if (args.length == 2) {
				int amount;
				try {
					amount = Integer.parseInt(args[1].trim());
				} catch (NumberFormatException ex) {
					throw new TownyException(Translation.of("msg_error_must_be_int"));
				}

				if (withdraw)
					MoneyUtil.townWithdraw(player, resident, resident.getTown(), amount);
				else
					MoneyUtil.townDeposit(player, resident, resident.getTown(), null, amount);
				
			} else {
				String command;
				if (withdraw)
					command = "/town withdraw";
				else 
					command = "/town deposit";
				
				throw new TownyException(Translation.of("msg_must_specify_amnt", command));
			}
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	private static void townOutpost(Player player, String[] args) {
		
		try {
			if (args.length >= 2) {
				if (args[1].equalsIgnoreCase("list")) {
					if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTPOST_LIST.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					Resident resident = getResidentOrThrow(player.getUniqueId());					
					if (!resident.hasTown())
						throw new TownyException(Translation.of("msg_err_must_belong_town"));
					
					Town town = resident.getTown();
					List<Location> outposts = town.getAllOutpostSpawns();
					int page = 1;
					int total = (int) Math.ceil(((double) outposts.size()) / ((double) 10));
					if (args.length == 3) {
						try {
							page = Integer.parseInt(args[2]);
							if (page < 0) {
								throw new TownyException(Translation.of("msg_err_negative"));
							} else if (page == 0) {
								throw new TownyException(Translation.of("msg_error_must_be_int"));
							}
						} catch (NumberFormatException e) {
							throw new TownyException(Translation.of("msg_error_must_be_int"));
						}
					}
					if (page > total)
						throw new TownyException(Translation.of("LIST_ERR_NOT_ENOUGH_PAGES", total));

					int iMax = page * 10;
					if ((page * 10) > outposts.size())
						iMax = outposts.size();
					
					if (Towny.isSpigot) {
						TownySpigotMessaging.sendSpigotOutpostList(player, town, page, total);
						return;
					}
					
					List<String> outputs = new ArrayList<String>();
					for (int i = (page - 1) * 10; i < iMax; i++) {
						Location outpost = outposts.get(i);
						String output;
						TownBlock tb = TownyAPI.getInstance().getTownBlock(outpost);
						if (tb == null)
							continue;
						String name = !tb.hasPlotObjectGroup() ? tb.getName() : tb.getPlotObjectGroup().getName();
						if (!name.equalsIgnoreCase("")) {
							output = Colors.Gold + (i + 1) + Colors.Gray + " - " + Colors.LightGreen  + name +  Colors.Gray + " - " + Colors.LightBlue + outpost.getWorld().getName() +  Colors.Gray + " - " + Colors.LightBlue + "(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")";
						} else {
							output = Colors.Gold + (i + 1) + Colors.Gray + " - " + Colors.LightBlue + outpost.getWorld().getName() + Colors.Gray + " - " + Colors.LightBlue + "(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")";
						}
						outputs.add(output);
					}
					player.sendMessage(
							ChatTools.formatList(
									Translation.of("outpost_plu"),
									Colors.Gold + "#" + Colors.Gray + " - " + Colors.LightGreen + "(Plot Name)" + Colors.Gray + " - " + Colors.LightBlue + "(Outpost World)"+ Colors.Gray + " - " + Colors.LightBlue + "(Outpost Location)",
									outputs,
									Translation.of("LIST_PAGE", page, total)
							));

				} else {
					boolean ignoreWarning = false;

					if (args.length == 2) {
						if (args[1].equals("-ignore")) {
							ignoreWarning = true;
						}
					}
					townSpawn(player, StringMgmt.remFirstArg(args), true, ignoreWarning);
				}
			} else {
				townSpawn(player, StringMgmt.remFirstArg(args), true, false);
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}
	
	private void townStatusScreen(CommandSender sender, Town town) {
		/*
		 * This is run async because it will ping the economy plugin for the town bank value.
		 */
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(town)));
	}

	private void townResList(CommandSender sender, String[] args) {

		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;

		Town town = null;
		try {
			if (args.length == 1 && player != null) {
				if (TownRuinUtil.isPlayersTownRuined(player))
					throw new TownyException(Translation.of("msg_err_cannot_use_command_because_town_ruined"));
				
				town = getResidentOrThrow(player.getUniqueId()).getTown();
			} else {
				town = TownyUniverse.getInstance().getTown(args[1]);
			}
		} catch (TownyException e) {
		}
		
		if (town != null)
			TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedResidents(town));
		else 
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_specify_name"));
	}
	
	private void townOutlawList(CommandSender sender, String[] args) {
		
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		
		Town town = null;
		try {
			if (args.length == 1 && player != null) {
				if (TownRuinUtil.isPlayersTownRuined(player))
					throw new TownyException(Translation.of("msg_err_cannot_use_command_because_town_ruined"));

				town = getResidentOrThrow(player.getUniqueId()).getTown();
			} else {
				town = TownyUniverse.getInstance().getTown(args[1]);
			}
		} catch (TownyException e) {
		}
		
		if (town != null)
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOutlaws(town));
		else 
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
	}
}
