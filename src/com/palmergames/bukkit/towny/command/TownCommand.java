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
import com.palmergames.bukkit.towny.event.TownPreTransactionEvent;
import com.palmergames.bukkit.towny.event.TownTransactionEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.comparators.GovernmentComparators;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.comparators.TownComparators;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.inviteobjects.PlayerJoinTownInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.naming.InvalidNameException;
import java.io.InvalidObjectException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
		"reslist",
		"say",
		"set",
		"toggle",
		"join",
		"invite",
		"buy",
		"mayor"
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

	static final List<String> townToggleTabCompletes = Arrays.asList(
		"explosion",
		"fire",
		"mobs",
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
								case "remove":
									try {
										return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getResident(player).getTownRanks(), args[3]);
									} catch (TownyException ignored) {}
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
								case "remove":
									try {
										return NameUtil.filterByStart(NameUtil.getNames(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getOutlaws()), args[2]);
									} catch (TownyException ignore) {}
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
						return null;
					break;
				case "kick":
					if (args.length == 2)
						return getTownResidentNamesOfPlayerStartingWith(player, args[1]);
					break;
				case "set":
					try {
						return townSetTabComplete(TownyUniverse.getInstance().getDataSource().getResident(player).getTown(), args);
					} catch (TownyException e) {
						return Collections.emptyList();
					}
				case "invite":
					switch (args.length) {
						case 2:
							List<String> returnValue = NameUtil.filterByStart(townInviteTabCompletes, args[1]);
							if (returnValue.size() > 0) {
								return returnValue;
							} else {
								if (args[1].startsWith("-")) {
									try {
										return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getSentInvites()
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
								case "deny":
									try {
										return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getReceivedInvites()
											// Get the names of all received invites
											.stream()
											.map(Invite::getSender)
											.map(InviteSender::getName)
											.collect(Collectors.toList()), args[2]);
									} catch (TownyException ignore) {}
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
						case 4:
							return getTownResidentNamesOfPlayerStartingWith(player, args[3]);
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
				
			Player player = (Player) sender;
			parseTownCommand(player, args);
		} else
			try {
				parseTownCommandForConsole(sender, args);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}

		return true;
	}

	@SuppressWarnings("static-access")
	private void parseTownCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWN_HELP.send(sender);
		} else if (split[0].equalsIgnoreCase("list")) {

			listTowns(sender, split);

		} else {
			try {
				final Town town = TownyUniverse.getInstance().getDataSource().getTown(split[0]);
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(town)));
			} catch (NotRegisteredException x) {
				throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
			}
		}

	}

	@SuppressWarnings("static-access")
	private void parseTownCommand(final Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {

			if (split.length == 0) {
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
					try {
						Resident resident = townyUniverse.getDataSource().getResident(player);
						Town town = resident.getTown();

						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
					} catch (NotRegisteredException x) {
						try {
							throw new TownyException(Translation.of("msg_err_dont_belong_town"));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage()); // Exceptions written from this runnable, are not reached by the catch at the end.
						}
					}
				});
			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
				HelpMenu.TOWN_HELP.send(player);
				
			} else if (split[0].equalsIgnoreCase("here")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_HERE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				showTownStatusHere(player);

			} else if (split[0].equalsIgnoreCase("list")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				listTowns(player, split);

			} else if (split[0].equalsIgnoreCase("new") || split[0].equalsIgnoreCase("create")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_NEW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				boolean noCharge = TownySettings.getNewTownPrice() == 0.0 || !TownySettings.isUsingEconomy();

				if (split.length == 1) {
					throw new TownyException(Translation.of("msg_specify_name"));
				} else if (split.length >= 2) {
					String[] newSplit = StringMgmt.remFirstArg(split);
					String townName = String.join("_", newSplit);
					
					newTown(player, townName, player.getName(), noCharge);
				}

			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LEAVE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				townLeave(player);

			} else if (split[0].equalsIgnoreCase("withdraw")) {
				
				if (!TownySettings.isUsingEconomy())
					throw new TownyException(Translation.of("msg_err_no_economy"));

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_WITHDRAW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
					Town tbTown = tb.getTown(); 
					Town pTown = townyUniverse.getDataSource().getResident(player).getTown();
					if (tbTown != pTown)
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));						
				}
				
				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_your_town"));
					if (!townyUniverse.getDataSource().getResident(player).getTown().getName().equals(TownyAPI.getInstance().getTownName(player.getLocation()))) 
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_your_town"));
				}

				if (split.length == 2)
					try {
						townWithdraw(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						throw new TownyException(Translation.of("msg_error_must_be_int"));
					}
				else
					throw new TownyException(Translation.of("msg_must_specify_amnt", "/town withdraw"));

			} else if (split[0].equalsIgnoreCase("deposit")) {

				if (!TownySettings.isUsingEconomy())
					throw new TownyException(Translation.of("msg_err_no_economy"));
				
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					}
					TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
					Town tbTown = tb.getTown(); 
					Town pTown = townyUniverse.getDataSource().getResident(player).getTown();
					if (tbTown != pTown)
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
				}
				
				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_your_town"));
					}
					if (!townyUniverse.getDataSource().getResident(player).getTown().equals(TownyAPI.getInstance().getTownBlock(player.getLocation()).getTown()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_your_town"));
				}

				if (split.length == 2)
					try {
						townDeposit(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						throw new TownyException(Translation.of("msg_error_must_be_int"));
					}
				else
					throw new TownyException(Translation.of("msg_must_specify_amnt", "/town deposit"));
			} else if (split[0].equalsIgnoreCase("plots")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Town town = null;
				try {


					if (split.length == 1) {
						town = townyUniverse.getDataSource().getResident(player).getTown();
					} else {
						town = townyUniverse.getDataSource().getTown(split[1]);
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
					return;
				}

				townPlots(player, town);

			} else {
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

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_BUY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townBuy(player, newSplit);

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test performed in method.
					 */
					townToggle(player, newSplit, false, null);

				} else if (split[0].equalsIgnoreCase("mayor")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_MAYOR.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townMayor(player, newSplit);

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
					if (split.length >= 2) {
						if (split[1].equalsIgnoreCase("list")) {
							if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTPOST_LIST.getNode())){
								throw new TownyException(Translation.of("msg_err_command_disable"));
							}
							Resident resident = townyUniverse.getDataSource().getResident(player);
							if (resident.hasTown()){
								Town town = resident.getTown();
								List<Location> outposts = town.getAllOutpostSpawns();
								int page = 1;
								int total = (int) Math.ceil(((double) outposts.size()) / ((double) 10));
								if (split.length == 3){
									try {
										page = Integer.parseInt(split[2]);
										if (page < 0) {
											TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative"));
											return;
										} else if (page == 0) {
											TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
											return;
										}
									} catch (NumberFormatException e) {
										TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
										return;
									}
								}
								if (page > total) {
									TownyMessaging.sendErrorMsg(player, TownySettings.getListNotEnoughPagesMsg(total));
									return;
								}
								int iMax = page * 10;
								if ((page * 10) > outposts.size()) {
									iMax = outposts.size();
								}
								@SuppressWarnings({ "unchecked", "rawtypes" })
								List<String> outputs = new ArrayList();
								for (int i = (page - 1) * 10; i < iMax; i++) {
									Location outpost = outposts.get(i);
									String output;
									TownBlock tb = TownyAPI.getInstance().getTownBlock(outpost);
									if (tb == null)
										continue;
									if (!tb.getName().equalsIgnoreCase("")) {
										output = Colors.Gold + (i + 1) + Colors.Gray + " - " + Colors.LightGreen  + tb.getName() +  Colors.Gray + " - " + Colors.LightBlue + outpost.getWorld().getName() +  Colors.Gray + " - " + Colors.LightBlue + "(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")";
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
												TownySettings.getListPageMsg(page, total)
										));

							} else {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_must_belong_town"));
							}
						} else {
							boolean ignoreWarning = false;

							if (split.length == 2) {
								if (split[1].equals("-ignore")) {
									ignoreWarning = true;
								}
							}
							townSpawn(player, newSplit, true, ignoreWarning);
						}
					} else {
						townSpawn(player, newSplit, true, false);
					}
				} else if (split[0].equalsIgnoreCase("delete")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DELETE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townDelete(player, newSplit);

				} else if (split[0].equalsIgnoreCase("reslist")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RESLIST.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					Town town = null;
					try {
						if (split.length == 1) {
							town = townyUniverse.getDataSource().getResident(player).getTown();
						} else {
							town = townyUniverse.getDataSource().getTown(split[1]);
						}
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
						return;
					}
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedResidents(town));

				} else if (split[0].equalsIgnoreCase("ranklist")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANKLIST.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					try {
						Resident resident = townyUniverse.getDataSource().getResident(player);
						Town town = resident.getTown();
						TownyMessaging.sendMessage(player, TownyFormatter.getRanks(town));
					} catch (NotRegisteredException x) {
						throw new TownyException(Translation.of("msg_err_dont_belong_town"));
					}

				} else if (split[0].equalsIgnoreCase("outlawlist")) {

					Town town;
					try {
						if (split.length == 1)
							town = townyUniverse.getDataSource().getResident(player).getTown();
						else
							town = townyUniverse.getDataSource().getTown(split[1]);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
						return;
					}
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOutlaws(town));

				} else if (split[0].equalsIgnoreCase("join")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseTownJoin(player, newSplit);

				} else if (split[0].equalsIgnoreCase("add")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townAdd(player, null, newSplit);

				} else if (split[0].equalsIgnoreCase("invite") || split[0].equalsIgnoreCase("invites")) {// He does have permission to manage Real invite Permissions. (Mayor or even assisstant)
					parseInviteCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					townKick(player, newSplit);

				} else if (split[0].equalsIgnoreCase("claim")) {

					parseTownClaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseTownUnclaimCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("online")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseTownOnlineCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("say")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SAY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					try {
						Town town = townyUniverse.getDataSource().getResident(player).getTown();
						StringBuilder builder = new StringBuilder();
						for (String s : newSplit) {
							builder.append(s + " ");
						}
						String message = builder.toString();
						TownyMessaging.sendPrefixedTownMessage(town, message);
					} catch (Exception ignored) {
					}
					
				} else if (split[0].equalsIgnoreCase("outlaw")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTLAW.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseTownOutlawCommand(player, newSplit, false, townyUniverse.getDataSource().getResident(player).getTown());
				} else {
					try {
						final Town town = townyUniverse.getDataSource().getTown(split[0]);
						Resident resident = townyUniverse.getDataSource().getResident(player);
						if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_OTHERTOWN.getNode()) && ( (resident.getTown() != town) || (!resident.hasTown()) ) ) {
							throw new TownyException(Translation.of("msg_err_command_disable"));
						}
						Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town)));

					} catch (NotRegisteredException x) {
						throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
					}
				}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

	}

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		// We know he has the main permission to manage this stuff. So Let's continue:

		Resident resident = townyUniverse.getDataSource().getResident(player);

		String received = Translation.of("town_received_invites")
				.replace("%a", Integer.toString(resident.getTown().getReceivedInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident.getTown())));
		String sent = Translation.of("town_sent_invites")
				.replace("%a", Integer.toString(resident.getTown().getSentInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown())));


		if (newSplit.length == 0) { // (/town invite)
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_SEE_HOME.getNode())) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_SENT.getNode())) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_RECEIVED.getNode())) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ACCEPT.getNode())) {
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
					try {
						nation = townyUniverse.getDataSource().getNation(newSplit[1]);
					} catch (NotRegisteredException e) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_DENY.getNode())) {
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
					try {
						nation = townyUniverse.getDataSource().getNation(newSplit[1]);
					} catch (NotRegisteredException e) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode())) {
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

			if (!admin)
				resident = townyUniverse.getDataSource().getResident(sender.getName());
			else
				resident = town.getMayor();				
			
			try {
				target = townyUniverse.getDataSource().getResident(split[1]);
			} catch (NotRegisteredException e2) {
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
					townyUniverse.getDataSource().saveTown(town);
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
					townyUniverse.getDataSource().saveTown(town);
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
			townyUniverse.getDataSource().saveTown(town);

		}

	}

	private void townPlots(Player player, Town town) {

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
		out.add(Colors.Green + "Town Size: " + Colors.LightGreen + town.getTownBlocks().size() + " / " + TownySettings.getMaxTownBlocks(town) + (TownySettings.isSellingBonusBlocks(town) ? Colors.LightBlue + " [Bought: " + town.getPurchasedBlocks() + "/" + TownySettings.getMaxPurchedBlocks(town) + "]" : "") + (town.getBonusBlocks() > 0 ? Colors.LightBlue + " [Bonus: " + town.getBonusBlocks() + "]" : "") + ((TownySettings.getNationBonusBlocks(town) > 0) ? Colors.LightBlue + " [NationBonus: " + TownySettings.getNationBonusBlocks(town) + "]" : ""));
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
		TownyMessaging.sendMessage(player, out);

	}

	private void parseTownOnlineCommand(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length > 0) {
			try {
				Town town = townyUniverse.getDataSource().getTown(split[0]);
				List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, town);
				if (onlineResidents.size() > 0) {
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translation.of("msg_town_online"), town, player));
				} else {
					TownyMessaging.sendMessage(player, Translation.of("default_towny_prefix") + Colors.White + "0 " + Translation.of("res_list") + " " + (Translation.of("msg_town_online") + ": " + town));
				}

			} catch (NotRegisteredException e) {
				throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
			}
		} else {
			try {
				Resident resident = townyUniverse.getDataSource().getResident(player);
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
	 * @throws TownyException - Thrown when player does not have permission nodes.
	 */
	@SuppressWarnings("unchecked")
	public void listTowns(CommandSender sender, String[] split) throws TownyException {
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
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

		List<Town> townsToSort = TownyUniverse.getInstance().getDataSource().getTowns();
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		Comparator<? extends Government> comparator = GovernmentComparators.BY_NUM_RESIDENTS;
		int total = (int) Math.ceil(((double) townsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) {
				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_multiple_comparators"));
					return;
				}
				i++;
				if (i < split.length) {

					if (split[i].equalsIgnoreCase("residents") || split[i].equalsIgnoreCase("resident")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_RESIDENTS.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_NUM_RESIDENTS;
					} else if (split[i].equalsIgnoreCase("balance")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_BALANCE.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_BANK_BALANCE;
					} else if (split[i].equalsIgnoreCase("name")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_NAME.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_NAME;
					} else if (split[i].equalsIgnoreCase("townblocks")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_TOWNBLOCKS.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = TownComparators.BY_TOWNBLOCKS_CLAIMED;
					} else if (split[i].equalsIgnoreCase("online")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_ONLINE.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_NUM_ONLINE;
					} else if (split[i].equalsIgnoreCase("open")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_OPEN.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_OPEN;
					} else {
						TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_invalid_comparator_town"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else {
				if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_RESIDENTS.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (pageSet) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_too_many_pages"));
					return;
				}
				try {
					page = Integer.parseInt(split[1]);
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
			TownyMessaging.sendErrorMsg(sender, TownySettings.getListNotEnoughPagesMsg(total));
			return;
		}
		
		final List<Town> towns = townsToSort;
		final Comparator<Town> comp = (Comparator<Town>) comparator;
		final int pageNumber = page;
		final int totalNumber = total; 
		try {
			if (!TownySettings.isTownListRandom()) {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					towns.sort(comp);
					sendList(sender, towns, pageNumber, totalNumber);
				});
			} else { 
				Collections.shuffle(towns);
				sendList(sender, towns, pageNumber, totalNumber);
			}
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_comparator_failed"));
		}
	}
	
	public void sendList(CommandSender sender, List<Town> towns, int page, int total) {
		
		if (Towny.isSpigot && sender instanceof Player) {
			TownySpigotMessaging.sendSpigotTownList(sender, towns, page, total);
			return;
		}

		int iMax = Math.min(page * 10, towns.size());
		List<String> townsformatted = new ArrayList<>(10);
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Town town = towns.get(i);
			String output = Colors.Blue + StringMgmt.remUnderscore(town.getName()) + 
					(TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + Colors.LightBlue + "(" + town.getNumResidents() + ")");
			if (town.isOpen())
				output += Translation.of("status_title_open");
			townsformatted.add(output);
		}
		
		String[] messages = ChatTools.formatList(Translation.of("town_plu"),
			Colors.Blue + Translation.of("town_name") +
				(TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + Colors.LightBlue + Translation.of("number_of_residents")),
			townsformatted, TownySettings.getListPageMsg(page, total)
		);
		
		sender.sendMessage(messages);
		
	}

	public void townMayor(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			showTownMayorHelp(player);
	}

	/**
	 * Send a the status of the town the player is physically at to him
	 *
	 * @param player - Player.
	 */
	public void showTownStatusHere(Player player) {

		try {
			if (TownyAPI.getInstance().isWilderness(player.getLocation()))
				throw new TownyException(Translation.of("msg_not_claimed", Coord.parseCoord(player.getLocation())));

			Town town = TownyAPI.getInstance().getTownBlock(player.getLocation()).getTown();
			TownyMessaging.sendMessage(player, TownyFormatter.getStatus(town));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void showTownMayorHelp(Player player) {

		player.sendMessage(ChatTools.formatTitle("Town Mayor Help"));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "withdraw [$]", ""));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "claim", "'/town claim ?' " + Translation.of("res_5")));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "unclaim", "'/town " + Translation.of("res_5")));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "[add/kick] " + Translation.of("res_2") + " .. []", Translation.of("res_6")));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "set [] .. []", "'/town set' " + Translation.of("res_5")));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "buy [] .. []", "'/town buy' " + Translation.of("res_5")));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "plots", ""));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "toggle", ""));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "rank add/remove [resident] [rank]", "'/town rank ?' " + Translation.of("res_5")));
		player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "delete", ""));
	}

	public static void townToggle(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			sender.sendMessage(ChatTools.formatTitle("/town toggle"));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "pvp", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "public", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "explosion", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "fire", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "mobs", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "taxpercent", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "open", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/town toggle", "jail [number] [resident]", ""));
		} else {
			Resident resident;

			try {
				
				if (!admin) {
					resident = townyUniverse.getDataSource().getResident(sender.getName());
					town = resident.getTown();
				} else { // Admin actions will be carried out as the mayor of the town for the purposes of some tests.
					resident = town.getMayor();
				}

			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			if (!admin && !townyUniverse.getPermissionSource().testPermission((Player) sender, PermissionNodes.TOWNY_COMMAND_TOWN_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (split[0].equalsIgnoreCase("public")) {

				town.setPublic(!town.isPublic());
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_public", town.isPublic() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_public", town.isPublic() ? Translation.of("enabled") : Translation.of("disabled")));

			} else if (split[0].equalsIgnoreCase("pvp")) {
				// Make sure we are allowed to set these permissions.
				if (!admin) {
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));
				
					// Test to see if the pvp cooldown timer is active for the town.
					if (TownySettings.getPVPCoolDownTime() > 0 && !admin && CooldownTimerTask.hasCooldown(town.getName(), CooldownType.PVP) && !townyUniverse.getPermissionSource().testPermission((Player) sender, PermissionNodes.TOWNY_ADMIN.getNode()))					 
						throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(town.getName(), CooldownType.PVP)));
				}
				boolean outsiderintown = false;
				if (TownySettings.getOutsidersPreventPVPToggle()) {
					for (Player target : Bukkit.getOnlinePlayers()) {
						Resident targetresident = townyUniverse.getDataSource().getResident(target);
						Block block = target.getLocation().getBlock().getRelative(BlockFace.DOWN);
						if (!TownyAPI.getInstance().isWilderness(block.getLocation())) {
							WorldCoord coord = WorldCoord.parseWorldCoord(target.getLocation());
							for (TownBlock tb : town.getTownBlocks()) {
								if (coord.equals(tb.getWorldCoord()) && ((!(targetresident.hasTown())) || (!(targetresident.getTown().equals(town))))) {
									outsiderintown = true;
								}
							}
						}
					}
				}
				if (!outsiderintown) {
					town.setPVP(!town.isPVP());
					// Add a cooldown to PVP toggling.
					if (TownySettings.getPVPCoolDownTime() > 0 && !admin && !townyUniverse.getPermissionSource().testPermission((Player) sender, PermissionNodes.TOWNY_ADMIN.getNode()))
						CooldownTimerTask.addCooldownTimer(town.getName(), CooldownType.PVP);
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_pvp", town.getName(), town.isPVP() ? Translation.of("enabled") : Translation.of("disabled")));
					if (admin)
						TownyMessaging.sendMsg(sender, Translation.of("msg_changed_pvp", town.getName(), town.isPVP() ? Translation.of("enabled") : Translation.of("disabled")));
				} else if (outsiderintown) {
					throw new TownyException(Translation.of("msg_cant_toggle_pvp_outsider_in_town"));
				}
			} else if (split[0].equalsIgnoreCase("explosion")) {
				// Make sure we are allowed to set these permissions.
				if (!admin)
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));
				town.setBANG(!town.isBANG());
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_expl", town.getName(), town.isBANG() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_expl", town.getName(), town.isBANG() ? Translation.of("enabled") : Translation.of("disabled")));

			} else if (split[0].equalsIgnoreCase("fire")) {
				// Make sure we are allowed to set these permissions.
				if (!admin)
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));
				town.setFire(!town.isFire());
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_fire", town.getName(), town.isFire() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_fire", town.getName(), town.isFire() ? Translation.of("enabled") : Translation.of("disabled")));

			} else if (split[0].equalsIgnoreCase("mobs")) {
				// Make sure we are allowed to set these permissions.
				if (!admin)
					toggleTest((Player) sender, town, StringMgmt.join(split, " "));
				town.setHasMobs(!town.hasMobs());
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_mobs", town.getName(), town.hasMobs() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_mobs", town.getName(), town.hasMobs() ? Translation.of("enabled") : Translation.of("disabled")));

			} else if (split[0].equalsIgnoreCase("taxpercent")) {
				town.setTaxPercentage(!town.isTaxPercentage());
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_taxpercent", town.isTaxPercentage() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_taxpercent", town.isTaxPercentage() ? Translation.of("enabled") : Translation.of("disabled")));
			} else if (split[0].equalsIgnoreCase("open")) {

				town.setOpen(!town.isOpen());
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_changed_open", town.isOpen() ? Translation.of("enabled") : Translation.of("disabled")));
				if (admin)
					TownyMessaging.sendMsg(sender, Translation.of("msg_changed_open", town.isOpen() ? Translation.of("enabled") : Translation.of("disabled")));

				// Send a warning when toggling on (a reminder about plot
				// permissions).
				if (town.isOpen())
					throw new TownyException(Translation.of("msg_toggle_open_on_warning"));

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
						Resident jailedresident = townyUniverse.getDataSource().getResident(split[2]);
						if (!admin && !((Player) sender).hasPermission("towny.command.town.toggle.jail")) 
							throw new TownyException(Translation.of("msg_no_permission_to_jail_your_residents"));
						if (!jailedresident.hasTown())
							if (!jailedresident.isJailed())
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
								Town jailTown = townyUniverse.getDataSource().getTown(jailedresident.getJailTown());
								if (jailTown != sendertown) {
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
				throw new TownyException(Translation.of("msg_err_invalid_property", split[0]));
			}

			//Propagate perms to all unchanged, town owned, townblocks

			for (TownBlock townBlock : town.getTownBlocks()) {
				if (!townBlock.hasResident() && !townBlock.isChanged()) {
					townBlock.setType(townBlock.getType());
					townyUniverse.getDataSource().saveTownBlock(townBlock);
				}
			}

			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(town);
			Bukkit.getServer().getPluginManager().callEvent(event);
			
			townyUniverse.getDataSource().saveTown(town);
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

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
				resident = townyUniverse.getDataSource().getResident(player);
				target = townyUniverse.getDataSource().getResident(split[1]);
				town = resident.getTown();

				if (town != target.getTown())
					throw new TownyException(Translation.of("msg_resident_not_your_town"));

			} catch (TownyException x) {
				throw new TownyException(x.getMessage());
			}

			rank = split[2];
			/*
			 * Match correct casing of rank, if that rank exists.
			 */
			for (String ranks : TownyPerms.getTownRanks()) {
				if (ranks.equalsIgnoreCase(rank))
					rank = ranks;
			}			
			/*
			 * Is this a known rank?
			 */			
			if (!TownyPerms.getTownRanks().contains(rank))
				throw new TownyException(Translation.of("msg_unknown_rank_available_ranks", rank, StringMgmt.join(TownyPerms.getTownRanks(), ",")));

			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank.toLowerCase())))
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
			townyUniverse.getDataSource().saveResident(target);

		}

	}

	public static void townSet(Player player, String[] split, boolean admin, Town town) throws TownyException, EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

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
					resident = townyUniverse.getDataSource().getResident(player);
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

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /town set board " + Translation.of("town_help_9"));
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

					if (!NameValidation.isValidString(line)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_string_board_not_set"));
						return;
					}
					// TownyFormatter shouldn't be given any string longer than 159, or it has trouble splitting lines.
					if (line.length() > 159)
						line = line.substring(0, 159);

					town.setBoard(line);
					TownyMessaging.sendTownBoard(player, town);
				}
			} else if (split[0].equalsIgnoreCase("title")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /town set title bilbo Jester ");
				else
					resident = townyUniverse.getDataSource().getResident(split[1]);
				
				if (resident.hasTown()) {
					if (resident.getTown() != townyUniverse.getDataSource().getResident(player).getTown()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_town", resident.getName()));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_town", resident.getName()));
					return;
				}
				split = StringMgmt.remArgs(split, 2);
				if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_input_too_long"));
					return;
				}

				String title = StringMgmt.join(NameValidation.checkAndFilterArray(split));
				resident.setTitle(title);
				townyUniverse.getDataSource().saveResident(resident);

				if (resident.hasTitle())
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_set_title", resident.getName(), resident.getTitle()));
				else
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_clear_title_surname", "Title", resident.getName()));

			} else if (split[0].equalsIgnoreCase("taxpercentcap")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAXPERCENTCAP.getNode()))
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

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /town set surname bilbo the dwarf ");
				else

					resident = townyUniverse.getDataSource().getResident(split[1]);
				if (resident.hasTown()) {
					if (resident.getTown() != townyUniverse.getDataSource().getResident(player).getTown()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_town", resident.getName()));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_town", resident.getName()));
					return;
				}
				split = StringMgmt.remArgs(split, 2);
				if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_input_too_long"));
					return;
				}

				String surname = StringMgmt.join(NameValidation.checkAndFilterArray(split));
				resident.setSurname(surname);
				townyUniverse.getDataSource().saveResident(resident);

				if (resident.hasSurname())
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_set_surname", resident.getName(), resident.getSurname()));
				else
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_clear_title_surname", "Surname", resident.getName()));


			} else {

				/*
				 * Test we have permission to use this command.
				 */
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split[0].equalsIgnoreCase("mayor")) {

					if (split.length < 2) {
						TownyMessaging.sendErrorMsg(player, "Eg: /town set mayor Dumbo");
						return;
					} else
						try {
							if (!resident.isMayor())
								throw new TownyException(Translation.of("msg_not_mayor"));

							String oldMayor = town.getMayor().getName();
							Resident newMayor = townyUniverse.getDataSource().getResident(split[1]);
							if (!town.hasResident(split[1]))
								throw new TownyException(Translation.of("msg_err_mayor_doesnt_belong_to_town"));
							town.setMayor(newMayor);
							TownyPerms.assignPermissions(townyUniverse.getDataSource().getResident(oldMayor), null);
							plugin.deleteCache(oldMayor);
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

                    if(TownySettings.isUsingEconomy() && TownySettings.getTownRenameCost() > 0) {
                		if (!town.getAccount().canPayFromHoldings(TownySettings.getTownRenameCost()))							
							throw new EconomyException(Translation.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())));

                    	final Town finalTown = town;
                    	final String name = split[1];
                    	Confirmation confirmation = Confirmation.runOnAccept(() -> {
							try {
								finalTown.getAccount().withdraw(TownySettings.getTownRenameCost(), String.format("Town renamed to: %s", name));
							} catch (EconomyException ignored) {}

							if (!NameValidation.isBlacklistName(name)) {
								townRename(player, finalTown, name);
							} else {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
							}
						})
							.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())))
							.build();
                    	
                    	ConfirmationHandler.sendConfirmation(player, confirmation);
                    	
                    } else {
						if (!NameValidation.isBlacklistName(split[1]))
							townRename(player, town, split[1]);
						else
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
                    }
				} else if (split[0].equalsIgnoreCase("tag")) {

					if (split.length < 2)
						TownyMessaging.sendErrorMsg(player, "Eg: /town set tag PLTC");
					else if (split[1].equalsIgnoreCase("clear")) {
						try {
							town.setTag(" ");
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_reset_town_tag", player.getName()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_reset_town_tag", player.getName()));
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
						}
					} else
						try {
							if (split[1].length() > 4)
								throw new TownyException(Translation.of("msg_err_tag_too_long"));
							town.setTag(NameValidation.checkAndFilterName(split[1]));
							if (admin)
								TownyMessaging.sendMessage(player, Translation.of("msg_set_town_tag", player.getName(), town.getTag()));
							TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_set_town_tag", player.getName(), town.getTag()));
						} catch (TownyException | InvalidNameException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
						}
					
				} else if (split[0].equalsIgnoreCase("homeblock")) {

					Coord coord = Coord.parseCoord(player);
					TownBlock townBlock;
					TownyWorld world;
					try {

						if (FlagWar.isUnderAttack(town) && TownySettings.isFlaggedInteractionTown()) {
							throw new TownyException(Translation.of("msg_war_flag_deny_town_under_attack"));
						}

						if (System.currentTimeMillis()- FlagWar.lastFlagged(town) < TownySettings.timeToWaitAfterFlag()) {
							throw new TownyException(Translation.of("msg_war_flag_deny_recently_attacked"));
						}
						
						if (TownyAPI.getInstance().isWarTime())
							throw new TownyException(Translation.of("msg_war_cannot_do"));

						world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
						final int minDistanceFromHomeblock = world.getMinDistanceFromOtherTowns(coord, resident.getTown());
						if (minDistanceFromHomeblock < TownySettings.getMinDistanceFromTownHomeblocks())
							throw new TownyException(Translation.of("msg_too_close2", Translation.of("homeblock")));

						if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0)
							if ((minDistanceFromHomeblock > TownySettings.getMaxDistanceBetweenHomeblocks()) && world.hasTowns())
								throw new TownyException(Translation.of("msg_too_far"));

						// Test whether towns will be removed from the nation
						if (nation != null && TownySettings.getNationRequiresProximity() > 0) {
							// Do a dry-run of the proximity test.
							List<Town> removedTowns = nation.recheckTownDistanceDryRun(nation.getTowns(), town);
							
							// Oh no, some the nation will lose at least one town, better make a confirmation.
							if (!removedTowns.isEmpty()) {
								final Town finalTown = town;
								final TownBlock finalTB = TownyAPI.getInstance().getTownBlock(player.getLocation());
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
								townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());
								oldWorld = town.getHomeblockWorld();
								town.setHomeBlock(townBlock);
								town.setSpawn(player.getLocation());		
								TownyMessaging.sendMsg(player, Translation.of("msg_set_town_home", coord.toString()));
							}
						// No nation to check proximity for/proximity isn't tested anyways.
						} else {
							townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());
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
						if (TownyAPI.getInstance().getTownBlock(player.getLocation()).getTown().getName().equals(town.getName())) {
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
			
			townyUniverse.getDataSource().saveTown(town);

			if (nation != null)
				townyUniverse.getDataSource().saveNation(nation);

			// If the town (homeblock) has moved worlds we need to update the
			// world files.
			if (oldWorld != null) {
				townyUniverse.getDataSource().saveWorld(town.getHomeblockWorld());
				townyUniverse.getDataSource().saveWorld(oldWorld);
			}
		}
	}

	public void townBuy(Player player, String[] split) {
		
		if (!TownySettings.isUsingEconomy()) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_no_economy"));
		}

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		Resident resident;
		Town town;
		try {
			resident = townyUniverse.getDataSource().getResident(player);
			town = resident.getTown();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		
		if (!TownySettings.isSellingBonusBlocks(town) && !TownySettings.isBonusBlocksPerTownLevel()) {
			TownyMessaging.sendErrorMsg(player, "Config.yml max_purchased_blocks: '0' ");
			return;
		} else if (TownySettings.isBonusBlocksPerTownLevel() && TownySettings.getMaxBonusBlocks(town) == 0) {
			TownyMessaging.sendErrorMsg(player, "Config.yml town_level townBlockBonusBuyAmount: 0");
			return;
		}
			
		
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town buy"));
			String line = Colors.Yellow + "[Purchased Bonus] " + Colors.Green + "Cost: " + Colors.LightGreen + "%s" + Colors.Gray + " | " + Colors.Green + "Max: " + Colors.LightGreen + "%d";
			player.sendMessage(String.format(line, TownyEconomyHandler.getFormattedBalance(town.getBonusBlockCost()), TownySettings.getMaxPurchedBlocks(town)));
			if (TownySettings.getPurchasedBonusBlocksIncreaseValue() != 1.0)
				player.sendMessage(Colors.Green + "Cost Increase per TownBlock: " + Colors.LightGreen + "+" +  new DecimalFormat("##.##%").format(TownySettings.getPurchasedBonusBlocksIncreaseValue()-1));
			player.sendMessage(ChatTools.formatCommand("", "/town buy", "bonus [n]", ""));
		} else {
			try {
				if (split[0].equalsIgnoreCase("bonus")) {
					if (split.length == 2) {
						try {
							townBuyBonusTownBlocks(town, Integer.parseInt(split[1].trim()), player);
						} catch (EconomyException ignored) {							
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
		if (current + inputN > TownySettings.getMaxPurchedBlocks(town)) {
			n = TownySettings.getMaxPurchedBlocks(town) - current;
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
			try {
				if (!town.getAccount().withdraw(cost, String.format("Town Buy Bonus (%d)", n))) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_funds_to_buy", n, Translation.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost)));
					return;
				}
			} catch (EconomyException ignored) {
			}
			town.addPurchasedBlocks(n);
			TownyMessaging.sendMsg(player, Translation.of("msg_buy", n, Translation.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost)));
			TownyUniverse.getInstance().getDataSource().saveTown(town);
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
	 * @param mayorName - name of mayor
	 * @param noCharge - charging for creation - /ta town new NAME MAYOR has no charge.
	 */
	public static void newTown(Player player, String name, String mayorName, boolean noCharge) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		PreNewTownEvent preEvent = new PreNewTownEvent(player, name);
		Bukkit.getPluginManager().callEvent(preEvent);
		
		if (preEvent.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
			return;
		}

		try {
			if (TownyAPI.getInstance().isWarTime())
				throw new TownyException(Translation.of("msg_war_cannot_do"));

			if (TownySettings.hasTownLimit() && townyUniverse.getDataSource().getTowns().size() >= TownySettings.getTownLimit())
				throw new TownyException(Translation.of("msg_err_universe_limit"));

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if ((filteredName == null) || townyUniverse.getDataSource().hasTown(filteredName))
				throw new TownyException(Translation.of("msg_err_invalid_name", name));

			Resident resident = townyUniverse.getDataSource().getResident(mayorName);
			if (resident.hasTown())
				throw new TownyException(Translation.of("msg_err_already_res", resident.getName()));

			TownyWorld world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());

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
			if (!noCharge && TownySettings.isUsingEconomy()) { 
				// Test if the resident can afford the town.
				if (!resident.getAccount().canPayFromHoldings(TownySettings.getNewTownPrice()))
					throw new TownyException(Translation.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translation.of("msg_you") : resident.getName()), TownySettings.getNewTownPrice()));
				
				Confirmation.runOnAccept(() -> {			
					try {
						// Make the resident pay here.
						if (!resident.getAccount().withdraw(TownySettings.getNewTownPrice(), "New Town Cost")) {
							// Send economy message
							TownyMessaging.sendErrorMsg(player,Translation.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translation.of("msg_you") : resident.getName()), TownySettings.getNewTownPrice()));
							return;
						}
					} catch (EconomyException ignored) {
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
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, "No valid economy found, your server admin might need to install Vault.jar or set using_economy: false in the Towny config.yml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Town newTown(TownyWorld world, String name, Resident resident, Coord key, Location spawn, Player player) throws TownyException {
		TownyDataSource townyDataSource = TownyUniverse.getInstance().getDataSource();

		townyDataSource.newTown(name);
		Town town = townyDataSource.getTown(name);
		resident.setTown(town);
		town.setMayor(resident);
		TownBlock townBlock = new TownBlock(key.getX(), key.getZ(), world);
		townBlock.setTown(town);

		// Set the plot permissions to mirror the towns.
		townBlock.setType(townBlock.getType());
		town.setSpawn(spawn);
		town.setUuid(UUID.randomUUID());
		town.setRegistered(System.currentTimeMillis());

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
		TownyMessaging.sendDebugMsg("Creating new Town account: " + "town-" + name);
		if (TownySettings.isUsingEconomy()) {
			try {
				town.getAccount().setBalance(0, "Setting 0 balance for Town");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
		}
		
		townyDataSource.saveResident(resident);
		townyDataSource.saveTownBlock(townBlock);
		townyDataSource.saveTown(town);
		townyDataSource.saveWorld(world);

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
			town = townyUniverse.getDataSource().getTown(newName);
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_set_name", player.getName(), town.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void townLeave(Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Resident resident;
		Town town;
		try {
			// TODO: Allow leaving town during war.
			if (TownyAPI.getInstance().isWarTime())
				throw new TownyException(Translation.of("msg_war_cannot_do"));

			resident = townyUniverse.getDataSource().getResident(player);
			town = resident.getTown();
			
			if (FlagWar.isUnderAttack(town) && TownySettings.isFlaggedInteractionTown()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_war_flag_deny_town_under_attack"));
				return;
			}

			if (System.currentTimeMillis()- FlagWar.lastFlagged(town) < TownySettings.timeToWaitAfterFlag()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_war_flag_deny_recently_attacked"));
				return;
			}
			
			plugin.deleteCache(resident.getName());

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		if (resident.isMayor()) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getMayorAbondonMsg());
			return;
		}

		if (resident.isJailed()) {
			try {
				if (resident.getJailTown().equals(resident.getTown().getName())) {
					if (TownySettings.JailDeniesTownLeave()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_cannot_abandon_town_while_jailed"));
						return;
					}
					resident.setJailed(false);
					resident.setJailSpawn(0);
					resident.setJailTown("");
					TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_escaped_jail_by_leaving_town", resident.getName()));
				}
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
		}

		try {
			townRemoveResident(town, resident);
		} catch (NotRegisteredException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		
		townyUniverse.getDataSource().saveResident(resident);
		townyUniverse.getDataSource().saveTown(town);

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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if ((split.length == 1 && split[0].equals("-ignore")) || (split.length > 1 && split[1].equals("-ignore"))) {
			ignoreWarning = true;
		}
		
		try {

			Resident resident = townyUniverse.getDataSource().getResident(player);
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
				town = townyUniverse.getDataSource().getTown(split[0]);
				notAffordMSG = Translation.of("msg_err_cant_afford_tp_town", town.getName());
			}
			
			SpawnUtil.sendToTownySpawn(player, split, town, notAffordMSG, outpost, ignoreWarning, SpawnType.TOWN);
		} catch (NotRegisteredException e) {

			throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));

		}

	}

	public void townDelete(Player player, String[] split) {

		final Town town;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			try {
				Resident resident = townyUniverse.getDataSource().getResident(player);
				town = resident.getTown();
				Confirmation.runOnAccept(() -> {
					TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
					TownyUniverse.getInstance().getDataSource().removeTown(town);
				})
					.sendTo(player);
				
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		} else {
			try {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode()))
					throw new TownyException(Translation.of("msg_err_admin_only_delete_town"));

				town = townyUniverse.getDataSource().getTown(split[0]);

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
			TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
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
			resident = TownyUniverse.getInstance().getDataSource().getResident(player);
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
			if (TownyUniverse.getInstance().getPermissionSource().has((Player) sender, PermissionNodes.TOWNY_ADMIN.getNode()))
				admin = true;				
		} else {
			name = "Console";
			admin = true;
		}
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		for (Resident newMember : new ArrayList<>(invited)) {
			try {

				TownPreAddResidentEvent preEvent = new TownPreAddResidentEvent(town, newMember);
				Bukkit.getPluginManager().callEvent(preEvent);

				if (preEvent.isCancelled()) {
					TownyMessaging.sendErrorMsg(sender, preEvent.getCancelMessage());
					return;
				}
				
				// only add players with the right permissions.
				if (BukkitTools.matchPlayer(newMember.getName()).isEmpty()) { // Not
																				// online
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_offline_no_join", newMember.getName()));
					invited.remove(newMember);
				} else if (!townyUniverse.getPermissionSource().has(BukkitTools.getPlayer(newMember.getName()), PermissionNodes.TOWNY_TOWN_RESIDENT.getNode())) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_not_allowed_join", newMember.getName()));
					invited.remove(newMember);
				} else if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsPerTown()){
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsPerTown()));
					invited.remove(newMember);
				} else if (!admin && TownySettings.getTownInviteCooldown() > 0 && ( (System.currentTimeMillis()/1000 - newMember.getRegistered()/1000) < (TownySettings.getTownInviteCooldown()) )) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_resident_doesnt_meet_invite_cooldown", newMember));
					invited.remove(newMember);
				} else if (TownySettings.getMaxNumResidentsWithoutNation() > 0 && !town.hasNation() && town.getResidents().size() == TownySettings.getMaxNumResidentsWithoutNation()) {
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
			if (town.hasOutlaw(newMember)) {
				town.removeOutlaw(newMember);
			}
		}

		if (invited.size() > 0) {
			StringBuilder msg = new StringBuilder();
			for (Resident newMember : invited)
				msg.append(newMember.getName()).append(", ");

			msg = new StringBuilder(msg.substring(0, msg.length() - 2));


			msg = new StringBuilder(Translation.of("msg_invited_join_town", name, msg.toString()));
			TownyMessaging.sendPrefixedTownMessage(town, msg.toString());
			townyUniverse.getDataSource().saveTown(town);
		} else
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
	}

	public static void townAddResident(Town town, Resident resident) throws AlreadyRegisteredException {

		resident.setTown(town);
		plugin.deleteCache(resident.getName());
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		townyUniverse.getDataSource().saveResident(resident);
		townyUniverse.getDataSource().saveTown(town);
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
			if (resident == member) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_you_cannot_kick_yourself"));
				kicking.remove(member);				
			}
			if (member.isMayor() || town.hasResidentWithRank(member, "assistant")) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_you_cannot_kick_this_resident", member));
				kicking.remove(member);
			} else {
				try {
					townRemoveResident(town, member);
				} catch (NotRegisteredException e) {
					kicking.remove(member);
				}
			}
		}
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
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
				if (!(sender instanceof Player) || !townyUniverse.getDataSource().getResident(player).hasTown() || !TownyUniverse.getInstance().getDataSource().getResident(player).getTown().equals(town))
					// For when the an admin uses /ta town {name} kick {residents}
					TownyMessaging.sendMessage(sender, msg.toString());
			} catch (NotRegisteredException e) {
			}
			townyUniverse.getDataSource().saveTown(town);
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
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
			TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(town.getNation()));
			townyUniverse.getDataSource().removeNation(town.getNation());

			if (TownySettings.isUsingEconomy() && TownySettings.isRefundNationDisbandLowResidents()) {
				try {
					town.getAccount().withdraw(TownySettings.getNewNationPrice(), "nation refund");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
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
			
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			resident = townyUniverse.getDataSource().getResident(residentName);
			town = townyUniverse.getDataSource().getTown(townName);

			// Check if resident is currently in a town.
			if (resident.hasTown())
				throw new Exception(Translation.of(exceptionMsg, contextualResidentName));

			if (!console) {
				// Check if town is town is free to join.
				if (!town.isOpen())
					throw new Exception(Translation.of("msg_err_not_open", town.getFormattedName()));
				if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsPerTown())
					throw new Exception(Translation.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsPerTown()));
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
				resident = TownyUniverse.getInstance().getDataSource().getResident(name);
				if (specifiedTown == null)
					town = resident.getTown();
				else
					town = specifiedTown;
			}

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage());
			return;
		}
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
		for (String residents : reslist) {
			if (residents.startsWith("-")) {
				removeinvites.add(residents.substring(1));
				// Add to removing them, remove the "-"
			} else {
				newreslist.add(residents);
				// add to adding them,
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
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
							townyUniverse.getDataSource().saveTownBlock(townBlock);
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
						boolean b = plugin.parseOnOff(split[0]);
						
						perm.change(TownyPermissionChange.Action.ALL_PERMS, b);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_town_set_perm_syntax_error"));
						return;
					}
				}

			} else if (split.length == 2) {
				boolean b;

				try {
					b = plugin.parseOnOff(split[1]);
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
					boolean b = plugin.parseOnOff(split[2]);

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
						townyUniverse.getDataSource().saveTownBlock(townBlock);
					}
				} else if (townBlockOwner instanceof Resident)
					if (!townBlock.isChanged()) {
						townBlock.setType(townBlock.getType());
						townyUniverse.getDataSource().saveTownBlock(townBlock);
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town claim"));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "", Translation.of("msg_block_claim")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "outpost", Translation.of("mayor_help_3")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "[circle/rect] [radius]", Translation.of("mayor_help_4")));
			player.sendMessage(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town claim", "[circle/rect] auto", Translation.of("mayor_help_5")));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (TownyAPI.getInstance().isWarTime()) {
					throw new TownyException(Translation.of("msg_war_cannot_do"));
				}

				resident = townyUniverse.getDataSource().getResident(player);
				town = resident.getTown();
				world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());

				if (!world.isUsingTowny()) {
					throw new TownyException(Translation.of("msg_set_use_towny_off"));
				}

				double blockCost = 0;
				List<WorldCoord> selection;
				boolean attachedToEdge = true, outpost = false;
				boolean isAdmin = townyUniverse.getPermissionSource().isTownyAdmin(player);
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());

				if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {

					if (TownySettings.isAllowingOutposts()) {
						if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						
						// Run various tests required by configuration/permissions through Util.
						OutpostUtil.OutpostTests(town, resident, world, key, isAdmin, false);
						
						if (!TownyAPI.getInstance().isWilderness(plugin.getCache(player).getLastLocation()))
							throw new TownyException(Translation.of("msg_already_claimed_1", key));

						
						selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), new String[0]);
						attachedToEdge = false;
						outpost = true;
					} else
						throw new TownyException(Translation.of("msg_outpost_disable"));
				} else {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), split);
					
					if (selection.size() == 0)
						throw new TownyException(Translation.of("msg_err_not_enough_blocks"));
					
					if ((selection.size() > 1) && (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN_MULTIPLE.getNode())))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (TownySettings.isUsingEconomy())
						blockCost = town.getTownBlockCost();
				}

				if ((world.getMinDistanceFromOtherTownsPlots(key, town) < TownySettings.getMinDistanceFromTownPlotblocks()))
					throw new TownyException(Translation.of("msg_too_close2", Translation.of("townblock")));

				if(world.getMinDistanceFromOtherTowns(key, town) < TownySettings.getMinDistanceFromTownHomeblocks())
					throw new TownyException(Translation.of("msg_too_close2", Translation.of("homeblock")));

				TownyMessaging.sendDebugMsg("townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = AreaSelectionUtil.filterTownOwnedBlocks(selection);
				selection = AreaSelectionUtil.filterInvalidProximityTownBlocks(selection, town);
				
				TownyMessaging.sendDebugMsg("townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				checkIfSelectionIsValid(town, selection, attachedToEdge, blockCost, false);
								
				//Check if other plugins have a problem with claiming this area
				int blockedClaims = 0;

				String cancelMessage = "";
				boolean isHomeblock = town.getTownBlocks().size() == 0;
				for(WorldCoord coord : selection){
					//Use the user's current world
					TownPreClaimEvent preClaimEvent = new TownPreClaimEvent(town, new TownBlock(coord.getX(), coord.getZ(), world), player, outpost, isHomeblock);
					BukkitTools.getPluginManager().callEvent(preClaimEvent);
					if(preClaimEvent.isCancelled()) {
						blockedClaims++;
						cancelMessage = preClaimEvent.getCancelMessage();
					}
				}

				if(blockedClaims > 0){
					throw new TownyException(String.format(cancelMessage, blockedClaims, selection.size()));
				}
				
				if (TownySettings.isUsingEconomy()) {
					try {					
						if (selection.size() == 1 && !outpost)
							blockCost = town.getTownBlockCost();
						else if (selection.size() == 1 && outpost)
							blockCost = TownySettings.getOutpostCost();
						else
							blockCost = town.getTownBlockCostN(selection.size());
	
						double missingAmount = blockCost - town.getAccount().getHoldingBalance();
						if (!town.getAccount().withdraw(blockCost, String.format("Town Claim (%d)", selection.size())))
							throw new TownyException(Translation.of("msg_no_funds_claim2", selection.size(), TownyEconomyHandler.getFormattedBalance(blockCost),  TownyEconomyHandler.getFormattedBalance(missingAmount), new DecimalFormat("#").format(missingAmount)));
					} catch (EconomyException e1) {
						throw new TownyException("Economy Error");
					}
				}
				new TownClaim(plugin, player, town, selection, outpost, true, false).start();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public static void parseTownUnclaimCommand(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
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
				if (TownyAPI.getInstance().isWarTime())
					throw new TownyException(Translation.of("msg_war_cannot_do"));

				resident = townyUniverse.getDataSource().getResident(player);
				town = resident.getTown();
				world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());

				if (FlagWar.isUnderAttack(town) && TownySettings.isFlaggedInteractionTown())
					throw new TownyException(Translation.of("msg_war_flag_deny_town_under_attack"));

				if (System.currentTimeMillis()- FlagWar.lastFlagged(town) < TownySettings.timeToWaitAfterFlag())
					throw new TownyException(Translation.of("msg_war_flag_deny_recently_attacked"));
				
				if (split.length == 1 && split[0].equalsIgnoreCase("all")) {
					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM_ALL.getNode()))
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
					//TownyMessaging.sendDebugMsg("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = True.");
					return true;
				}
			} catch (NotRegisteredException e) {
			}
		//TownyMessaging.sendDebugMsg("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = False.");
		return false;
	}

	public static void checkIfSelectionIsValid(TownBlockOwner owner, List<WorldCoord> selection, boolean attachedToEdge, double blockCost, boolean force) throws TownyException {

		if (force)
			return;
		Town town = (Town) owner;

		if (owner instanceof Town) {
			// Town town = (Town)owner;
			int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
			TownyMessaging.sendDebugMsg("Claim Check Available: " + available);
			TownyMessaging.sendDebugMsg("Claim Selection Size: " + selection.size());
			if (available - selection.size() < 1)
				throw new TownyException(Translation.of("msg_err_not_enough_blocks"));
		}


		if (TownySettings.isUsingEconomy()) {
			try {
				if (selection.size() == 1)
					blockCost = town.getTownBlockCost();
				else
					blockCost = town.getTownBlockCostN(selection.size());
	
				double missingAmount = blockCost - town.getAccount().getHoldingBalance();
				if (TownySettings.isUsingEconomy() && !((Town) owner).getAccount().canPayFromHoldings(blockCost))
					throw new TownyException(Translation.of("msg_err_cant_afford_blocks2", selection.size(), TownyEconomyHandler.getFormattedBalance(blockCost),  TownyEconomyHandler.getFormattedBalance(missingAmount), new DecimalFormat("#").format(missingAmount)));
			} catch (EconomyException e1) {
				throw new TownyException("Economy Error");
			}
		}
		
		if (attachedToEdge && !isEdgeBlock(owner, selection) && !town.getTownBlocks().isEmpty()) {
			if (selection.size() == 0)
				throw new TownyException(Translation.of("msg_already_claimed_2"));
			else
				throw new TownyException(Translation.of("msg_err_not_attached_edge"));
		}
	}

	private void townWithdraw(Player player, int amount) {

		Resident resident;
		Town town;
		try {
			if (!TownySettings.getTownBankAllowWithdrawls())
				throw new TownyException(Translation.of("msg_err_withdraw_disabled"));

			if (amount < 0)
				throw new TownyException(Translation.of("msg_err_negative_money"));

			resident = TownyUniverse.getInstance().getDataSource().getResident(player);
			town = resident.getTown();

			if (System.currentTimeMillis()- FlagWar.lastFlagged(town) < TownySettings.timeToWaitAfterFlag())
				throw new TownyException("You cannot do this! You were attacked too recently!");
			
			Transaction transaction = new Transaction(TransactionType.WITHDRAW, player, amount);
			TownPreTransactionEvent preEvent = new TownPreTransactionEvent(town, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}
			
			town.withdrawFromBank(resident, amount);
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_xx_withdrew_xx", resident.getName(), amount, Translation.of("town_sing")));
			BukkitTools.getPluginManager().callEvent(new TownTransactionEvent(town, transaction));
		} catch (TownyException | EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	private void townDeposit(Player player, int amount) {

		Resident resident;
		Town town;
		try {
			resident = TownyUniverse.getInstance().getDataSource().getResident(player);
			town = resident.getTown();

			double bankcap = TownySettings.getTownBankCap();
			if (bankcap > 0) {
				if (amount + town.getAccount().getHoldingBalance() > bankcap)
					throw new TownyException(Translation.of("msg_err_deposit_capped", bankcap));
			}

			if (amount < 0)
				throw new TownyException(Translation.of("msg_err_negative_money"));

			Transaction transaction = new Transaction(TransactionType.DEPOSIT, player, amount);
			
			TownPreTransactionEvent preEvent = new TownPreTransactionEvent(town, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}
			
			// Deposit into town.
			town.depositToBank(resident, amount);
			
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_xx_deposited_xx", resident.getName(), amount, Translation.of("town_sing")));
			BukkitTools.getPluginManager().callEvent(new TownTransactionEvent(town, transaction));
		} catch (TownyException | EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}
	
	/**
	 * Used to allow nation members to deposit to towns' banks in their nations.
	 * 
	 * @param player the player issuing the command
	 * @param town town with bank the player wants to deposit to
	 * @param amount amount the player wishes to deposit
	 */
	public static void townDeposit(Player player, Town town, int amount) {
		try {
			Resident resident = TownyAPI.getInstance().getDataSource().getResident(player);			
			
			double bankcap = TownySettings.getTownBankCap();
			if (bankcap > 0) {
				if (amount + town.getAccount().getHoldingBalance() > bankcap)
					throw new TownyException(Translation.of("msg_err_deposit_capped", bankcap));			
			}
			
			if (amount < 0)
				throw new TownyException(Translation.of("msg_err_negative_money"));

			Transaction transaction = new Transaction(TransactionType.DEPOSIT, player, amount);

			TownPreTransactionEvent preEvent = new TownPreTransactionEvent(town, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);

			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}
			
			if (!resident.getAccount().payTo(amount, town, "Town Deposit from Nation member"))
				throw new TownyException(Translation.of("msg_insuf_funds"));

			TownyMessaging.sendPrefixedNationMessage(resident.getTown().getNation(), Translation.of("msg_xx_deposited_xx", resident.getName(), amount, town + " town"));
			BukkitTools.getPluginManager().callEvent(new TownTransactionEvent(town, transaction));
		} catch (EconomyException | TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}
	
	public static List<Resident> getValidatedResidentsForInviteRevoke(Object sender, String[] names, Town town) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Resident> toRevoke = new ArrayList<>();
		for (Invite invite : town.getSentInvites()) {
			for (String name : names) {
				if (invite.getReceiver().getName().equalsIgnoreCase(name)) {
					try {
						toRevoke.add(townyUniverse.getDataSource().getResident(name));
					} catch (NotRegisteredException ignored) {
					}
				}
			}
			
		}
		return toRevoke;		
	}

}
