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
import com.palmergames.bukkit.towny.event.NationAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationInviteTownEvent;
import com.palmergames.bukkit.towny.event.NationPreAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationPreRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.NationRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.NationRequestAllyNationEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.event.NationPreTransactionEvent;
import com.palmergames.bukkit.towny.event.NationTransactionEvent;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
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
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.comparators.GovernmentComparators;
import com.palmergames.bukkit.towny.object.comparators.NationComparators;
import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import com.palmergames.bukkit.towny.object.inviteobjects.TownJoinNationInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
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
import javax.naming.InvalidNameException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
		"rank",
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
		"king"
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
				case "kick":
					return getTownyStartingWith(args[args.length - 1], "t");
				case "ally":
					if (args.length == 2) {
						return NameUtil.filterByStart(nationAllyTabCompletes, args[1]);
					} else if (args.length > 2){
						switch (args[1].toLowerCase()) {
							case "add":
								if (args[args.length - 1].startsWith("-")) {
									// Return only sent invites to revoked because the nation name starts with a hyphen, e.g. -exampleNationName
									try {
										return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getNation().getSentAllyInvites()
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
									return NameUtil.filterByStart(NameUtil.getNames(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getNation().getAllies()), args[args.length - 1]);
								} catch (TownyException ignore) {}
							case "accept":
							case "deny":
								// Return sent ally invites to accept or deny
								try {
									return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getNation().getReceivedInvites()
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
										return NameUtil.filterByStart(NameUtil.getNames(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getNation().getResidents()), args[2]);
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
									return NameUtil.filterByStart(NameUtil.getNames(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getNation().getEnemies()), args[2]);
								} catch (TownyException ignored) {}
						}
					}
					break;
				case "set":
					try {
						return nationSetTabComplete(TownyUniverse.getInstance().getDataSource().getResident(player).getTown().getNation(), args);
					} catch (NotRegisteredException e) {
						return Collections.emptyList();
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
			try {
				parseNationCommandForConsole(sender, args);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage());
			}

		return true;
	}

	@SuppressWarnings("static-access")
	private void parseNationCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			HelpMenu.NATION_HELP.send(sender);

		} else if (split[0].equalsIgnoreCase("list")) {

			listNations(sender, split);

		} else {
			try {
				final Nation nation = TownyUniverse.getInstance().getDataSource().getNation(split[0]);
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(nation)));

			} catch (NotRegisteredException x) {
				throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
			}
		}


	}

	@SuppressWarnings("static-access")
	public void parseNationCommand(final Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		String nationCom = "/nation";

		try {

			if (split.length == 0)
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
					try {
						Resident resident = townyUniverse.getDataSource().getResident(player);
						Town town = resident.getTown();
						Nation nation = town.getNation();
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation));
					} catch (NotRegisteredException x) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_dont_belong_nation"));
					}
				});

			else if (split[0].equalsIgnoreCase("?"))
					HelpMenu.NATION_HELP.send(player);
			else if (split[0].equalsIgnoreCase("list")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				listNations(player, split);
				
			} else if (split[0].equalsIgnoreCase("townlist")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOWNLIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Nation nation = null;
				try {
					if (split.length == 1) {
						nation = townyUniverse.getDataSource().getResident(player).getTown().getNation();
					} else {
						nation = townyUniverse.getDataSource().getNation(split[1]);
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_name"));
					return;
				}
				TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getFormattedName()));
				TownyMessaging.sendMessage(player, ChatTools.listArr(TownyFormatter.getFormattedNames(nation.getTowns().toArray(new Town[0])), Translation.of("status_nation_towns", nation.getTowns().size())));

			} else if (split[0].equalsIgnoreCase("allylist")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLYLIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Nation nation = null;
				try {
					if (split.length == 1) {
						nation = townyUniverse.getDataSource().getResident(player).getTown().getNation();
					} else {
						nation = townyUniverse.getDataSource().getNation(split[1]);
					}
				} catch (Exception e) {
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

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ENEMYLIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				Nation nation = null;
				try {
					if (split.length == 1) {
						nation = townyUniverse.getDataSource().getResident(player).getTown().getNation();
					} else {
						nation = townyUniverse.getDataSource().getNation(split[1]);
					}
				} catch (Exception e) {
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

				Resident resident = townyUniverse.getDataSource().getResident(player);

		        if ((TownySettings.getNumResidentsCreateNation() > 0) && (resident.getTown().getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
		          TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_enough_residents_new_nation"));
		          return;
		        }

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_NEW.getNode()))
					throw new TownyException(TownySettings.getNotPermToNewNationLine());

				if (split.length == 1)
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_nation_name"));
				else if (split.length >= 2) {

					if (!resident.isMayor() && !resident.getTown().hasResidentWithRank(resident, "assistant"))
						throw new TownyException(Translation.of("msg_peasant_right"));
					
					boolean noCharge = TownySettings.getNewNationPrice() == 0.0 || !TownySettings.isUsingEconomy();
					
					String[] newSplit = StringMgmt.remFirstArg(split);
					String nationName = String.join("_", newSplit);
					newNation(player, nationName, resident.getTown().getName(), noCharge);

				}
			} else if (split[0].equalsIgnoreCase("join")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_JOIN.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				parseNationJoin(player, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("merge")) {
				
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_MERGE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				if (split.length == 1)
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_specify_nation_name"));
				else if (split.length == 2) {
					Resident resident = townyUniverse.getDataSource().getResident(player);
					if (!resident.isKing())
						throw new TownyException(Translation.of("msg_err_merging_for_kings_only"));
					mergeNation(player, split[1]);
				}
				
			} else if (split[0].equalsIgnoreCase("withdraw")) {
				if (!TownySettings.isUsingEconomy())
					throw new TownyException(Translation.of("msg_err_no_economy"));

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_WITHDRAW.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
					Nation tbNation = tb.getTown().getNation();
					Nation pNation= townyUniverse.getDataSource().getResident(player).getTown().getNation();
					if ((tbNation != pNation) || (!tb.getTown().isCapital()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
				}

				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_nation_capital"));
					Town town = TownyAPI.getInstance().getTownBlock(player.getLocation()).getTown();
					if (!town.isCapital())
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_nation_capital"));
					Nation nation = town.getNation();
					if (!townyUniverse.getDataSource().getResident(player).getTown().getNation().equals(nation))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_nation_capital"));
				}

				if (split.length == 2)
					try {
						nationWithdraw(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					}
				else
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_must_specify_amnt", nationCom));
			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LEAVE.getNode()))
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
				if (!TownySettings.isUsingEconomy())
					throw new TownyException(Translation.of("msg_err_no_economy"));

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					}
					TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
					Nation tbNation = tb.getTown().getNation();
					Nation pNation= townyUniverse.getDataSource().getResident(player).getTown().getNation();
					if ((tbNation != pNation) || (!tb.getTown().isCapital()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
				}

				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyAPI.getInstance().isWilderness(player.getLocation()))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_nation_capital"));
					Town town = TownyAPI.getInstance().getTownBlock(player.getLocation()).getTown();
					if (!town.isCapital())
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_nation_capital"));
					Nation nation = town.getNation();
					if (!townyUniverse.getDataSource().getResident(player).getTown().getNation().equals(nation))
						throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_nation_capital"));
				}

				if (split.length == 1) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_must_specify_amnt", nationCom + " deposit"));
					return;
				}
				if (split.length == 2)
					try {
						nationDeposit(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
					}
				if (split.length == 3) {
					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT_OTHER.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					
					Town town = TownyAPI.getInstance().getDataSource().getTown(split[2]);
					Nation nation = townyUniverse.getDataSource().getResident(player).getTown().getNation();
					if (town != null) {
						if (!town.hasNation())
							throw new TownyException(Translation.of("msg_err_not_same_nation", town.getName()));
						if (!town.getNation().equals(nation))
							throw new TownyException(Translation.of("msg_err_not_same_nation", town.getName()));
						try {
							TownCommand.townDeposit(player, town, Integer.parseInt(split[1].trim()));
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_error_must_be_int"));
						}
					} else {
						throw new NotRegisteredException();
					}
				}
					

			}  else {
				String[] newSplit = StringMgmt.remFirstArg(split);

				if (split[0].equalsIgnoreCase("rank")) {

					/*
					 * Rank perm tests are performed in the nationrank method.
					 */
					nationRank(player, newSplit);

				} else if (split[0].equalsIgnoreCase("king")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_KING.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationKing(player, newSplit);

				} else if (split[0].equalsIgnoreCase("add")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationAdd(player, newSplit);

				} else if (split[0].equalsIgnoreCase("invite") || split[0].equalsIgnoreCase("invites")) {
						parseInviteCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_KICK.getNode()))
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

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ENEMY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationEnemy(player, newSplit);

				} else if (split[0].equalsIgnoreCase("delete")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DELETE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					nationDelete(player, newSplit);

				} else if (split[0].equalsIgnoreCase("online")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					parseNationOnlineCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("say")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SAY.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					try {
						Nation nation = townyUniverse.getDataSource().getResident(player).getTown().getNation();
						StringBuilder builder = new StringBuilder();
						for (String s : newSplit) {
							builder.append(s + " ");
						}
						String message = builder.toString();
						TownyMessaging.sendPrefixedNationMessage(nation, message);
					} catch (Exception e) {
					}

				} else {

					try {
						final Nation nation = townyUniverse.getDataSource().getNation(split[0]);
						Resident resident = townyUniverse.getDataSource().getResident(player);
						if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_OTHERNATION.getNode()) && ( (resident.hasTown() && resident.getTown().hasNation() && (resident.getTown().getNation() != nation) )  || !resident.hasTown() )) {
							throw new TownyException(Translation.of("msg_err_command_disable"));
						}
						Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation)));

					} catch (NotRegisteredException x) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", split[0]));
					}
				}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	private void parseNationJoin(Player player, String[] args) {
		
		try {
			Resident resident;
			Town town;
			Nation nation;
			String nationName;

			if (args.length < 1)
				throw new Exception(String.format("Usage: /nation join [nation]"));

			nationName = args[0];
			
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			resident = townyUniverse.getDataSource().getResident(player);
			town = resident.getTown();
			nation = townyUniverse.getDataSource().getNation(nationName);

			// Check if town is currently in a nation.
			if (town.hasNation())
				throw new Exception(Translation.of("msg_err_already_in_a_nation"));

			// Check if town is town is free to join.
			if (!nation.isOpen())
				throw new Exception(Translation.of("msg_err_nation_not_open", nation.getFormattedName()));
			
			if ((TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation()))
				throw new Exception(Translation.of("msg_err_not_enough_residents_join_nation", town.getName()));

			if (TownySettings.getMaxTownsPerNation() > 0) 
	        	if (nation.getTowns().size() >= TownySettings.getMaxTownsPerNation())
	        		throw new Exception(Translation.of("msg_err_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));

			if (TownySettings.getNationRequiresProximity() > 0) {
				Coord capitalCoord = nation.getCapital().getHomeBlock().getCoord();
				Coord townCoord = town.getHomeBlock().getCoord();
				if (!nation.getCapital().getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
					throw new Exception(Translation.of("msg_err_nation_homeblock_in_another_world"));
				}
				double distance;
				distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
				if (distance > TownySettings.getNationRequiresProximity()) {
					throw new Exception(Translation.of("msg_err_town_not_close_enough_to_nation", town.getName()));
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident = townyUniverse.getDataSource().getResident(player);
		String sent = Translation.of("nation_sent_invites")
				.replace("%a", Integer.toString(resident.getTown().getNation().getSentInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown().getNation())));

		if (newSplit.length == 0) { // (/nation invite)
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_SEE_HOME.getNode())) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_LIST_SENT.getNode())) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode())) {
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length > 0) {
			try {
				Nation nation = townyUniverse.getDataSource().getNation(split[0]);
				List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, nation);
				if (onlineResidents.size() > 0 ) {
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translation.of("msg_nation_online"), nation, player));
				} else {
					TownyMessaging.sendMessage(player, Colors.White +  "0 " + Translation.of("res_list") + " " + (Translation.of("msg_nation_online") + ": " + nation));
				}

			} catch (NotRegisteredException e) {
				throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));
			}
		} else {
			try {
				Resident resident = townyUniverse.getDataSource().getResident(player);
				Town town = resident.getTown();
				Nation nation = town.getNation();
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translation.of("msg_nation_online"), nation, player));
			} catch (NotRegisteredException x) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_dont_belong_nation"));
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
			TownyUniverse townyUniverse = TownyUniverse.getInstance();

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 3) {
				TownyMessaging.sendErrorMsg(player, "Eg: /town rank add/remove [resident] [rank]");
				return;
			}

			try {
				resident = townyUniverse.getDataSource().getResident(player);
				target = townyUniverse.getDataSource().getResident(split[1]);
				town = resident.getTown();
				targetTown = target.getTown();

				if (town.getNation() != targetTown.getNation())
					throw new TownyException("This resident is not a member of your Town!");

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			rank = split[2];
			/*
			 * Is this a known rank?
			 */
			if (!TownyPerms.getNationRanks().contains(rank)) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_unknown_rank_available_ranks", rank, StringMgmt.join(TownyPerms.getNationRanks(), ",")));
				return;
			}
			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank.toLowerCase()))) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_permission_to_give_rank"));
				return;
			}

			if (split[0].equalsIgnoreCase("add")) {
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
			townyUniverse.getDataSource().saveResident(target);

		}

	}

	private void nationWithdraw(Player player, int amount) {

		Resident resident;
		Nation nation;
		try {
			if (!TownySettings.geNationBankAllowWithdrawls())
				throw new TownyException(Translation.of("msg_err_withdraw_disabled"));

			if (amount < 0)
				throw new TownyException(Translation.of("msg_err_negative_money"));

			resident = TownyUniverse.getInstance().getDataSource().getResident(player);
			nation = resident.getTown().getNation();

			boolean underAttack = false;
			for (Town town : nation.getTowns()) {
				if (FlagWar.isUnderAttack(town) || System.currentTimeMillis()- FlagWar.lastFlagged(town) < TownySettings.timeToWaitAfterFlag()) {
					underAttack = true;
					break;
				}
			}

			if (underAttack && TownySettings.isFlaggedInteractionNation())
				throw new TownyException(Translation.of("msg_war_flag_deny_nation_under_attack"));
			
			Transaction transaction = new Transaction(TransactionType.WITHDRAW, player, amount);
			NationPreTransactionEvent preEvent = new NationPreTransactionEvent(nation, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}
			
			nation.withdrawFromBank(resident, amount);
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_xx_withdrew_xx", resident.getName(), amount, Translation.of("nation_sing")));
			BukkitTools.getPluginManager().callEvent(new NationTransactionEvent(nation, transaction));
		} catch (TownyException | EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	private void nationDeposit(Player player, int amount) {

		Resident resident;
		Nation nation;
		try {
			resident = TownyUniverse.getInstance().getDataSource().getResident(player);
			nation = resident.getTown().getNation();

			double bankcap = TownySettings.getNationBankCap();
			if (bankcap > 0) {
				if (amount + nation.getAccount().getHoldingBalance() > bankcap)
					throw new TownyException(Translation.of("msg_err_deposit_capped", bankcap));
			}

			if (amount < 0)
				throw new TownyException(Translation.of("msg_err_negative_money"));

			Transaction transaction = new Transaction(TransactionType.DEPOSIT, player, amount);

			NationPreTransactionEvent preEvent = new NationPreTransactionEvent(nation, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(preEvent.getCancelMessage());
				return;
			}
			
			// Deposit into bank.
			nation.depositToBank(resident, amount);

			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_xx_deposited_xx", resident.getName(), amount, Translation.of("nation_sing")));
			BukkitTools.getPluginManager().callEvent(new NationTransactionEvent(nation, transaction));
		} catch (TownyException | EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
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
	@SuppressWarnings({"unchecked"})
	public void listNations(CommandSender sender, String[] split) throws TownyException {
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
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
		
		List<Nation> nationsToSort = TownyUniverse.getInstance().getDataSource().getNations();
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		Comparator<? extends Government> comparator = GovernmentComparators.BY_NUM_RESIDENTS;
		int total = (int) Math.ceil(((double) nationsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) {
				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_multiple_comparators_nation"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("residents")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_RESIDENTS.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_NUM_RESIDENTS;
					} else if (split[i].equalsIgnoreCase("balance")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_BALANCE.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_BANK_BALANCE;
					} else if (split[i].equalsIgnoreCase("towns")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_TOWNS.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = NationComparators.BY_NUM_TOWNS;
					} else if (split[i].equalsIgnoreCase("name")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_NAME.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_NAME;						
					} else if (split[i].equalsIgnoreCase("townblocks")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_TOWNBLOCKS.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = NationComparators.BY_TOWNBLOCKS_CLAIMED;
					} else if (split[i].equalsIgnoreCase("online")) {
						if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_ONLINE.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						comparator = GovernmentComparators.BY_NUM_ONLINE;
					} else {
						TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_invalid_comparator_nation"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else {
				if (!console && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_RESIDENTS.getNode()))
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

	    final List<Nation> nations = nationsToSort;
	    final Comparator<Nation> comp = (Comparator<Nation>) comparator;
	    final int pageNumber = page;
		try {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				nations.sort(comp);
				sendList(sender, nations, pageNumber, total);
			});
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_error_comparator_failed"));
		}

	}
	
	public void sendList(CommandSender sender, List<Nation> nations, int page, int total) {
		
		if (Towny.isSpigot  && sender instanceof Player) {
			TownySpigotMessaging.sendSpigotNationList(sender, nations, page, total);
			return;
		}

		int iMax = Math.min(page * 10, nations.size());
		List<String> nationsordered = new ArrayList<>(10);
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Nation nation = nations.get(i);
			String output = Colors.Gold + StringMgmt.remUnderscore(nation.getName()) + Colors.Gray + " - " + Colors.LightBlue + "(" + nation.getNumResidents() + ")" + Colors.Gray + " - " + Colors.LightBlue + "(" + nation.getNumTowns() + ")";
			nationsordered.add(output);
		}
		sender.sendMessage(
				ChatTools.formatList(
						Translation.of("nation_plu"),
						Colors.Gold + Translation.of("nation_name") + Colors.Gray + " - " + Colors.LightBlue + Translation.of("number_of_residents") + Colors.Gray + " - " + Colors.LightBlue + Translation.of("number_of_towns"),
						nationsordered,
						TownySettings.getListPageMsg(page, total)
				));		
	}

	/**
	 * Create a new nation. Command: /nation new [nation] *[capital]
	 *
	 * @param player - Player creating the new nation.
	 * @param name - Nation name.
	 * @param capitalName - Capital city name.
	 * @param noCharge - charging for creation - /ta nation new NAME CAPITAL has no charge.
	 */
	public static void newNation(Player player, String name, String capitalName, boolean noCharge) {

		com.palmergames.bukkit.towny.TownyUniverse universe = com.palmergames.bukkit.towny.TownyUniverse.getInstance();
		try {

			Town town = universe.getDataSource().getTown(capitalName);
			if (town.hasNation())
				throw new TownyException(Translation.of("msg_err_already_nation"));

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if ((filteredName == null) || universe.getDataSource().hasNation(filteredName))
				throw new TownyException(Translation.of("msg_err_invalid_name", name));

			// If it isn't free to make a nation, send a confirmation.
			if (!noCharge && TownySettings.isUsingEconomy()) {
				// Test if they can pay.
				if (!town.getAccount().canPayFromHoldings(TownySettings.getNewNationPrice()))			
					throw new TownyException(Translation.of("msg_no_funds_new_nation2", TownySettings.getNewNationPrice()));

				Confirmation.runOnAccept(() -> {				
					try {
						// Town pays for nation here.
						if (!town.getAccount().withdraw(TownySettings.getNewNationPrice(), "New Nation Cost")) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_funds_new_nation2", TownySettings.getNewNationPrice()));
							return;
						}
					} catch (EconomyException ignored) {
					}
					try {
						// Actually make nation.
						newNation(name, town);
					} catch (AlreadyRegisteredException | NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
					TownyMessaging.sendGlobalMessage(Translation.of("msg_new_nation", player.getName(), StringMgmt.remUnderscore(name)));

				})
					.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNewNationPrice())))
					.sendTo(player);
				
			// Or, it is free, so just make the nation.
			} else {
				newNation(name, town);
				TownyMessaging.sendGlobalMessage(Translation.of("msg_new_nation", player.getName(), StringMgmt.remUnderscore(name)));
			}
		} catch (TownyException | EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	public static Nation newNation(String name, Town town) throws AlreadyRegisteredException, NotRegisteredException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		townyUniverse.getDataSource().newNation(name);
		Nation nation = townyUniverse.getDataSource().getNation(name);
		nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());
		town.setNation(nation);
		nation.setCapital(town);
		nation.setUuid(UUID.randomUUID());
		nation.setRegistered(System.currentTimeMillis());
		if (TownySettings.isUsingEconomy()) {
			try {
				nation.getAccount().setBalance(0, "Deleting Nation");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
		}
		townyUniverse.getDataSource().saveTown(town);
		townyUniverse.getDataSource().saveNation(nation);

		BukkitTools.getPluginManager().callEvent(new NewNationEvent(nation));

		return nation;
	}

	public void mergeNation(Player player, String name) throws TownyException {
		
		com.palmergames.bukkit.towny.TownyUniverse universe = com.palmergames.bukkit.towny.TownyUniverse.getInstance();
		Nation nation;
		Nation remainingNation;
		
		try {
			nation = universe.getDataSource().getNation(name);
			remainingNation = universe.getDataSource().getResident(player).getTown().getNation();
		} catch (NotRegisteredException e) {
			throw new TownyException(Translation.of("msg_err_invalid_name", name));
		}
		if (remainingNation.getName().equalsIgnoreCase(name))
			throw new TownyException(Translation.of("msg_err_invalid_name", name));

		if (nation != null) {
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
				try {
					TownyUniverse.getInstance().getDataSource().mergeNation(nation, remainingNation);
					TownyMessaging.sendGlobalMessage(Translation.of("nation1_has_merged_with_nation2", nation, remainingNation));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			})
			.sendTo(BukkitTools.getPlayerExact(king.getName()));
		}
	}

	public void nationLeave(Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Town town = null;
		Nation nation = null;

		try {
			Resident resident = townyUniverse.getDataSource().getResident(player);
			town = resident.getTown();
			nation = town.getNation();
			
			if (town.isConquered())
				throw new TownyException(Translation.of("msg_err_your_conquered_town_cannot_leave_the_nation_yet"));

			if (FlagWar.isUnderAttack(town) && TownySettings.isFlaggedInteractionTown()) {
				throw new TownyException(Translation.of("msg_war_flag_deny_town_under_attack"));
			}

			if (System.currentTimeMillis() - FlagWar.lastFlagged(town) < TownySettings.timeToWaitAfterFlag()) {
				throw new TownyException(Translation.of("msg_war_flag_deny_recently_attacked"));
			}
			
			town.removeNation();

			plugin.resetCache();

			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_town_left", StringMgmt.remUnderscore(town.getName())));
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
	}

	public void nationDelete(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0)
			try {
				Resident resident = townyUniverse.getDataSource().getResident(player);
				Nation nation = resident.getTown().getNation();
				Confirmation.runOnAccept(() -> {
					TownyUniverse.getInstance().getDataSource().removeNation(nation);
					TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nation));
				})
				.sendTo(player);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		else
			try {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE.getNode()))
					throw new TownyException(Translation.of("msg_err_admin_only_delete_nation"));

				Nation nation = townyUniverse.getDataSource().getNation(split[0]);
				townyUniverse.getDataSource().removeNation(nation);
				TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nation));
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation add [names]");
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = townyUniverse.getDataSource().getResident(player);
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
		List<String> reslist = new ArrayList<>(Arrays.asList(names));
		// Our Arraylist is above
		List<String> newreslist = new ArrayList<>();
		// The list of valid invites is above, there are currently none
		List<String> removeinvites = new ArrayList<>();
		// List of invites to be removed;
		for (String townname : reslist) {
			if (townname.startsWith("-")) {
				// Add to removing them, remove the "-"
				removeinvites.add(townname.substring(1));
			} else {
				// add to adding them,
				newreslist.add(townname);
			}
		}
		names = newreslist.toArray(new String[0]);
		String[] namestoremove = removeinvites.toArray(new String[0]);
		if (namestoremove.length >= 1) {
			nationRevokeInviteTown(player,nation, townyUniverse.getDataSource().getTowns(namestoremove));
		}

		if (names.length >= 1) {
			nationAdd(player, nation, townyUniverse.getDataSource().getTowns(names));
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
					distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		for (Town town : towns) {
			if (!town.hasNation()) {
				town.setNation(nation);
				townyUniverse.getDataSource().saveTown(town);
				TownyMessaging.sendNationMessagePrefixed(nation, Translation.of("msg_join_nation", town.getName()));
			}

		}
		plugin.resetCache();
		townyUniverse.getDataSource().saveNation(nation);

	}

	public void nationKick(Player player, String[] names) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation kick [names]");
			return;
		}

		Resident resident;
		Nation nation;
		try {
			
			if (TownyAPI.getInstance().isWarTime())
				throw new TownyException(Translation.of("msg_war_cannot_do"));
			
			resident = townyUniverse.getDataSource().getResident(player);
			nation = resident.getTown().getNation();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		nationKick(player, nation, townyUniverse.getDataSource().getTowns(names));
	}

	public static void nationKick(CommandSender sender, Nation nation, List<Town> kicking) {

		ArrayList<Town> remove = new ArrayList<>();
		for (Town town : kicking)
			if (town.isCapital())
				remove.add(town);
			else if (!nation.getTowns().contains(town))
				remove.add(town);
			else 
				town.removeNation();

		for (Town town : remove)
			kicking.remove(town);

		if (kicking.size() > 0) {
			StringBuilder msg = new StringBuilder();

			for (Town town : kicking) {
				msg.append(town.getName()).append(", ");

				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_nation_kicked_by", sender.getName()));
			}

			msg = new StringBuilder(msg.substring(0, msg.length() - 2));
			msg = new StringBuilder(Translation.of("msg_nation_kicked", sender.getName(), msg.toString()));
			TownyMessaging.sendPrefixedNationMessage(nation, msg.toString());

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(sender, Translation.of("msg_invalid_name"));
	}

	private void nationAlly(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length <= 0) {
			HelpMenu.ALLIES_STRING.send(player);
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = townyUniverse.getDataSource().getResident(player);
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

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ADD.getNode())) {
				throw new TownyException(Translation.of("msg_err_command_disable"));
			}
			for (String name : names) {
				try {
					ally = townyUniverse.getDataSource().getNation(name);
					if (nation.equals(ally)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
					} else if (nation.isAlliedWith(ally)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_already_ally", ally));
					} else {
						list.add(ally);
					}
					
				} catch (NotRegisteredException e) { // So "-Name" isn't a town, remove the - check if that is a town.
					if (name.startsWith("-") && TownySettings.isDisallowOneWayAlliance()) {
						try {
							ally = townyUniverse.getDataSource().getNation(name.substring(1));
							if (nation.equals(ally)) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
							} else {
								remlist.add(ally);
							}
						} catch (NotRegisteredException x){
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
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_REMOVE.getNode())) {
				throw new TownyException(Translation.of("msg_err_command_disable"));
			}
			for (String name : names) {
				try {
					ally = townyUniverse.getDataSource().getNation(name);
					if (nation.equals(ally)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
						return;
					} else {
						list.add(ally);
					}
				} catch (NotRegisteredException e) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_SENT.getNode())) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_RECEIVED.getNode())) {
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
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ACCEPT.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				Nation sendernation;
				List<Invite> invites = nation.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_no_requests"));
					return;
				}
				if (split.length >= 2) { // /invite deny args[1]
					try {
						sendernation = townyUniverse.getDataSource().getNation(split[1]);
					} catch (NotRegisteredException e) {
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
						InviteHandler.acceptInvite(toAccept);
						return;
					} catch (InvalidObjectException e) {
						e.printStackTrace(); // Shouldn't happen, however like i said a fallback
					}
				}

			}
			if (split[0].equalsIgnoreCase("deny")) {
				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_DENY.getNode())) {
					throw new TownyException(Translation.of("msg_err_command_disable"));
				}
				Nation sendernation;
				List<Invite> invites = nation.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_nation_no_requests"));
					return;
				}
				if (split.length >= 2) { // /invite deny args[1]
					try {
						sendernation = townyUniverse.getDataSource().getNation(split[1]);
					} catch (NotRegisteredException e) {
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

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
						if (townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode())) {
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
			
			townyUniverse.getDataSource().saveNations();

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
			resident = townyUniverse.getDataSource().getResident(player);
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
				try {
					enemy = townyUniverse.getDataSource().getNation(name);
					if (nation.equals(enemy))
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_own_nation_disallow"));
					else
						list.add(enemy);
				} catch (NotRegisteredException e) {
					throw new TownyException(Translation.of("msg_err_no_nation_with_that_name", name));
				}
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
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
					resident = townyUniverse.getDataSource().getResident(player);
					nation = resident.getTown().getNation();
				} else // treat resident as king for testing purposes.
					resident = nation.getKing();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			if (split[0].equalsIgnoreCase("king")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_KING.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set king Dumbo");
				else
					try {
						Resident newKing = townyUniverse.getDataSource().getResident(split[1]);
						String oldKingsName = nation.getCapital().getMayor().getName();

			            if ((TownySettings.getNumResidentsCreateNation() > 0) && (newKing.getTown().getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			              TownyMessaging.sendMessage(player, Translation.of("msg_not_enough_residents_capital", newKing.getTown().getName()));
			              return;
			            }

						nation.setKing(newKing);
						plugin.deleteCache(oldKingsName);
						plugin.deleteCache(newKing.getName());
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_new_king", newKing.getName(), nation.getName()));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
			} else if (split[0].equalsIgnoreCase("capital")) {
				try {
					Town newCapital = townyUniverse.getDataSource().getTown(split[1]);

		            if ((TownySettings.getNumResidentsCreateNation() > 0) && (newCapital.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
		              TownyMessaging.sendMessage(player, Translation.of("msg_not_enough_residents_capital", newCapital.getName()));
		              return;
		            }

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_CAPITOL.getNode()))
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
								TownyUniverse.getInstance().getDataSource().saveNation(nation);
							}
						// Proximity doesn't factor in.
						} else {
							nation.setCapital(newCapital);
							plugin.resetCache();
							TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
							TownyUniverse.getInstance().getDataSource().saveNation(nation);
						}
					}
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

			} else if (split[0].equalsIgnoreCase("spawn")){

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				try{
					nation.setSpawn(player.getLocation());
					TownyMessaging.sendMsg(player, Translation.of("msg_set_nation_spawn"));
				} catch (TownyException e){
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			}
			else if (split[0].equalsIgnoreCase("taxes")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAXES.getNode()))
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

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWNCOST.getNode()))
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

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set name Plutoria");				
				else {
				    if(TownySettings.isUsingEconomy() && TownySettings.getNationRenameCost() > 0) {
						if (!nation.getAccount().canPayFromHoldings(TownySettings.getNationRenameCost()))
							throw new EconomyException(Translation.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));

						final Nation finalNation = nation;
                    	final String name = split[1];
				    	Confirmation.runOnAccept(() -> {
							try {
								finalNation.getAccount().withdraw(TownySettings.getNationRenameCost(), String.format("Nation renamed to: %s", name));
							} catch (EconomyException ignored) {}
								
		                    if (!NameValidation.isBlacklistName(name))
								nationRename(player, finalNation, name);
							else
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
				    	})
				    	.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())))
						.sendTo(player);
				    	
                    } else {
						if (!NameValidation.isBlacklistName(split[1]))
							nationRename(player, nation, split[1]);
						else
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));
                    }
				}

			} else if (split[0].equalsIgnoreCase("tag")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAG.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set tag PLT");
				else if (split[1].equalsIgnoreCase("clear")) {
					try {
						nation.setTag(" ");
						TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_reset_nation_tag", player.getName()));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
				} else
					if (split[1].length() > 4)
						throw new TownyException(Translation.of("msg_err_tag_too_long"));
					nation.setTag(NameValidation.checkAndFilterName(split[1]));
				TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_set_nation_tag", player.getName(), nation.getTag()));

			} else if (split[0].equalsIgnoreCase("title")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set title bilbo Jester ");
				else
					resident = townyUniverse.getDataSource().getResident(split[1]);
				
				if (resident.hasNation()) {
					if (resident.getTown().getNation() != townyUniverse.getDataSource().getResident(player).getTown().getNation()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_nation", resident.getName()));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_nation", resident.getName()));
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
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_set_title", resident.getName(), resident.getTitle()));
				else
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_clear_title_surname", "Title", resident.getName()));

			} else if (split[0].equalsIgnoreCase("surname")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set surname bilbo the dwarf ");
				else

					resident = townyUniverse.getDataSource().getResident(split[1]);
				if (resident.hasNation()) {
					if (resident.getTown().getNation() != townyUniverse.getDataSource().getResident(player).getTown().getNation()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_nation", resident.getName()));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_same_nation", resident.getName()));
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
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_set_surname", resident.getName(), resident.getSurname()));
				else
					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_clear_title_surname", "Surname", resident.getName()));


			} else if (split[0].equalsIgnoreCase("board")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set board " + Translation.of("town_help_9"));
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

					if (!NameValidation.isValidString(line)) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_string_nationboard_not_set"));
						return;
					}
					// TownyFormatter shouldn't be given any string longer than 159, or it has trouble splitting lines.
					if (line.length() > 159)
						line = line.substring(0, 159);

					nation.setBoard(line);
					TownyMessaging.sendNationBoard(player, nation);
				}
			} else if (split[0].equalsIgnoreCase("mapcolor")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_MAPCOLOR.getNode()))
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
			
			townyUniverse.getDataSource().saveNation(nation);
		}
	}

	public static void nationToggle(Player player, String[] split, boolean admin, Nation nation) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/nation toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/nation toggle", "peaceful/neutral", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation toggle", "public", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation toggle", "open", ""));
		} else {
			Resident resident;

			try {
				if (!admin) {
					resident = townyUniverse.getDataSource().getResident(player);
					nation = resident.getTown().getNation();
				} else  // Treat any resident tests as though the king were doing it.
					resident = nation.getKing();
				
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			if (split[0].equalsIgnoreCase("peaceful") || split[0].equalsIgnoreCase("neutral")) {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_NEUTRAL.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				try {
					boolean choice = !nation.isNeutral();
					double cost = TownySettings.getNationNeutralityCost();

					if (choice && TownySettings.isUsingEconomy() && !nation.getAccount().withdraw(cost, "Peaceful Nation Cost"))
						throw new TownyException(Translation.of("msg_nation_cant_peaceful"));

					nation.toggleNeutral(choice);

					// send message depending on if using an economy and charging
					// for peaceful
					if (TownySettings.isUsingEconomy() && cost > 0)
						TownyMessaging.sendMsg(player, Translation.of("msg_you_paid", TownyEconomyHandler.getFormattedBalance(cost)));
					else
						TownyMessaging.sendMsg(player, Translation.of("msg_nation_set_peaceful"));

					TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_peaceful") + (nation.isNeutral
							() ? Colors.Green : Colors.Red + " not") + " peaceful.");
				} catch (TownyException e) {
					nation.setNeutral(false);
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			} else if(split[0].equalsIgnoreCase("public")){
                if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_PUBLIC.getNode()))
                    throw new TownyException(Translation.of("msg_err_command_disable"));

                nation.setPublic(!nation.isPublic());
                TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_changed_public", nation.isPublic() ? Translation.of("enabled") : Translation.of("disabled")));

            } else if(split[0].equalsIgnoreCase("open")){
                if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_PUBLIC.getNode()))
                    throw new TownyException(Translation.of("msg_err_command_disable"));

                nation.setOpen(!nation.isOpen());
                TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_nation_changed_open", nation.isOpen() ? Translation.of("enabled") : Translation.of("disabled")));

            } else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "nation"));
				return;
			}

			townyUniverse.getDataSource().saveNation(nation);
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

        try {

            Resident resident = townyUniverse.getDataSource().getResident(player);
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
                nation = townyUniverse.getDataSource().getNation(split[0]);
                notAffordMSG = Translation.of("msg_err_cant_afford_tp_nation", nation.getName());

			}
            
			SpawnUtil.sendToTownySpawn(player, split, nation, notAffordMSG, false, ignoreWarning, SpawnType.NATION);
		} catch (NotRegisteredException e) {

            throw new TownyException(Translation.of("msg_err_not_registered_1", split[0]));

        }

    }
}
