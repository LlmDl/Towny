package com.palmergames.bukkit.towny.command;

import com.google.common.collect.Iterables;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.PlotClearEvent;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.event.PlotPreClearEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Send a list of all general towny plot help commands to player Command: /plot
 */

public class PlotCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	
	private static final List<String> plotTabCompletes = Arrays.asList(
		"claim",
		"unclaim",
		"forsale",
		"fs",
		"notforsale",
		"nfs",
		"evict",
		"perm",
		"set",
		"toggle",
		"clear",
		"group"
	);
	
	private static final List<String> plotGroupTabCompletes = Arrays.asList(
		"add",
		"remove",
		"set",
		"toggle",
		"fs",
		"notforsale",
		"forsale",
		"perm"
	);
	
	private static final List<String> plotSetTabCompletes = Arrays.asList(
		"reset",
		"shop",
		"embassy",
		"arena",
		"wilds",
		"inn",
		"jail",
		"farm",
		"bank",
		"outpost",
		"name",
		"perm"
	);
	
	private static final List<String> plotRectCircleCompletes = Arrays.asList(
		"rect",
		"circle"
	);
	
	private static final List<String> plotToggleTabCompletes = Arrays.asList(
		"fire",
		"pvp",
		"explosion",
		"mobs"
	);

	public PlotCommand(Towny instance) {

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
			try {
				if (!TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName()).isUsingTowny()) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_set_use_towny_off"));
					return false;
				}
			} catch (NotRegisteredException e) {
				// World not registered				
			}

			if (args == null) {
				HelpMenu.PLOT_HELP.send(player);

			} else {
				try {
					return parsePlotCommand(player, args);
				} catch (TownyException x) {
					// No permisisons
					 x.getMessage();
				}
			}

		} else
			// Console
			HelpMenu.PLOT_HELP.send(sender);
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player) {
			switch (args[0].toLowerCase()) {
				case "set":
					if (args.length == 2) {
						return NameUtil.filterByStart(plotSetTabCompletes, args[1]);
					}
					if (args.length > 2 && args[1].equalsIgnoreCase("perm")) {
						return permTabComplete(StringMgmt.remArgs(args, 2));
					}
					break;
				case "toggle":
					if (args.length == 2)
						return NameUtil.filterByStart(plotToggleTabCompletes, args[1]);
					break;
				case "claim":
				case "unclaim":
				case "notforsale":
				case "nfs":
					if (args.length == 2)
						return NameUtil.filterByStart(plotRectCircleCompletes, args[1]);
					break;
				case "forsale":
				case "fs":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(Collections.singletonList("within"), args[1]);
						case 3:
							return NameUtil.filterByStart(plotRectCircleCompletes, args[2]);
					}
					break;
				case "group":
					if (args.length == 2) {
						return NameUtil.filterByStart(plotGroupTabCompletes, args[1]);
					} else if (args.length > 2) {
						return permTabComplete(StringMgmt.remFirstArg(args));
					}
					break;
				case "perm":
					if (args.length == 2)
						return NameUtil.filterByStart(Collections.singletonList("hud"), args[1]);
					break;
				default:
					if (args.length == 1)
						return NameUtil.filterByStart(plotTabCompletes, args[0]);
					break;
			}
		}

		return Collections.emptyList();
	}

	public boolean parsePlotCommand(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_HELP.send(player);
		} else {

			Resident resident;
			String world;

			try {
				resident = townyUniverse.getDataSource().getResident(player);
				world = player.getWorld().getName();
				//resident.getTown();
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return true;
			}

			try {
				if (split[0].equalsIgnoreCase("claim")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLAIM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (TownyAPI.getInstance().isWarTime())
						throw new TownyException(Translation.of("msg_war_cannot_do"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					// selection = TownyUtil.filterUnownedPlots(selection);

					if (selection.size() > 0) {

						double cost = 0;

						// Remove any plots Not for sale (if not the mayor) and
						// tally up costs.
						for (WorldCoord worldCoord : new ArrayList<>(selection)) {
							try {
								TownBlock block = worldCoord.getTownBlock();
								double price = block.getPlotPrice();
								
								if (block.hasPlotObjectGroup()) {
									// This block is part of a group, special tasks need to be done.
									PlotGroup group = block.getPlotObjectGroup();

									// Add the confirmation for claiming a plot group.
									Confirmation.runOnAccept(() -> {
										ArrayList<WorldCoord> coords = new ArrayList<>();

										// Get worldcoords from plot group.
										group.getTownBlocks().forEach((tb) -> coords.add(tb.getWorldCoord()));

										// Execute the plot claim.
										new PlotClaim(Towny.getPlugin(), player, resident, coords, true, false, true).start();
									})
									.setTitle(Translation.of("msg_plot_group_claim_confirmation", group.getTownBlocks().size()) + " " + TownyEconomyHandler.getFormattedBalance(group.getPrice()) + ". " + Translation.of("are_you_sure_you_want_to_continue"))
									.sendTo(player);
									
									return true;
								}
								
								// Check if a plot has a price.
								if (price > -1)
									cost += block.getPlotPrice();
								else {
									if (!block.getTown().isMayor(resident)) // ||
										// worldCoord.getTownBlock().getTown().hasAssistant(resident))
										selection.remove(worldCoord);
								}
							} catch (NotRegisteredException e) {
								selection.remove(worldCoord);
							}
						}

						int maxPlots = TownySettings.getMaxResidentPlots(resident);
						int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);
						
						//Infinite plots
						if (maxPlots != -1) {
							maxPlots = maxPlots + extraPlots;
						}
						
						if (maxPlots >= 0 && resident.getTownBlocks().size() + selection.size() > maxPlots)
							throw new TownyException(Translation.of("msg_max_plot_own", maxPlots));

						if (TownySettings.isUsingEconomy() && (!resident.getAccount().canPayFromHoldings(cost)))
							throw new TownyException(Translation.of("msg_no_funds_claim", selection.size(), TownyEconomyHandler.getFormattedBalance(cost)));

						if (cost != 0) {
							String title = Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost));
							Confirmation.runOnAccept(() ->  {	
								// Start the claim task
								new PlotClaim(plugin, player, resident, selection, true, false, false).start();
							})
							.setTitle(title)
							.sendTo(player);
						} else {
							// Start the claim task
							new PlotClaim(plugin, player, resident, selection, true, false, false).start();
						}
					} else {
						player.sendMessage(Translation.of("msg_err_empty_area_selection"));
					}
				} else if (split[0].equalsIgnoreCase("evict")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_EVICT.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (TownyAPI.getInstance().isWarTime())
						throw new TownyException(Translation.of("msg_war_cannot_do"));
					
					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					Town town = townBlock.getTown();										
					
					if (townBlock.getResident() == null) {
						
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_one_to_evict"));						
					} else {
						
						Resident owner = townBlock.getResident();
						if (!town.equals(resident.getTown())){ 
							
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_part_town"));
							return false;							
						}

						if (townBlock.hasPlotObjectGroup()) {
							for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
								
								owner = tb.getResident();
								tb.setResident(null);
								tb.setPlotPrice(-1);

								// Set the plot permissions to mirror the towns.
								tb.setType(townBlock.getType());

								townyUniverse.getDataSource().saveResident(owner);
								// Update the townBlock data file so it's no longer using custom settings.
								townyUniverse.getDataSource().saveTownBlock(tb);
							}
							
							player.sendMessage(Translation.of("msg_plot_evict_group", townBlock.getPlotObjectGroup().getName()));
							return true;
						}

						townBlock.setResident(null);
						townBlock.setPlotPrice(-1);

						// Set the plot permissions to mirror the towns.
						townBlock.setType(townBlock.getType());
						
						townyUniverse.getDataSource().saveResident(owner);
						// Update the townBlock data file so it's no longer using custom settings.
						townyUniverse.getDataSource().saveTownBlock(townBlock);
						
						player.sendMessage(Translation.of("msg_plot_evict"));
					}

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_UNCLAIM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (TownyAPI.getInstance().isWarTime())
						throw new TownyException(Translation.of("msg_war_cannot_do"));

					if (split.length == 2 && split[1].equalsIgnoreCase("all")) {
						// Start the unclaim task
						new PlotClaim(plugin, player, resident, null, false, false, false).start();

					} else {
						
						List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
						selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection);

						if (selection.size() > 0) {

							for (WorldCoord coord : selection) {
								TownBlock block = coord.getTownBlock();

								if (!block.hasPlotObjectGroup()) {
									// Start the unclaim task
									new PlotClaim(plugin, player, resident, selection, false, false, false).start();
									continue;
								}
								
								// Get all the townblocks part of the group.
								final List<WorldCoord> groupSelection = new ArrayList<>();
								block.getPlotObjectGroup().getTownBlocks().forEach((tb) -> {
									groupSelection.add(tb.getWorldCoord());
								});
								
								// Create confirmation.
								Confirmation.runOnAccept(() -> {
									new PlotClaim(Towny.getPlugin(), player, resident, groupSelection, false, false, false).start();
								})
								.setTitle(Translation.of("msg_plot_group_unclaim_confirmation", block.getPlotObjectGroup().getTownBlocks().size()) + " " + Translation.of("are_you_sure_you_want_to_continue"))
								.sendTo(player);
								
								return true;
							}

						} else {
							player.sendMessage(Translation.of("msg_err_empty_area_selection"));
						}
					}

				} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_NOTFORSALE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					
					if (townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode())) {
						for (WorldCoord worldCoord : selection) {
							if (worldCoord.getTownBlock().hasPlotObjectGroup()) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_plot_nfs", worldCoord));
								return false;
							}
							setPlotForSale(resident, worldCoord, -1);
						}
						return true;
					}
					
					if (!townBlock.getType().equals(TownBlockType.EMBASSY)) 
						selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection);
					else
						selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection);

					for (WorldCoord worldCoord : selection) {
						setPlotForSale(resident, worldCoord, -1);
					}
					
					if (selection.isEmpty()){
						throw new TownyException(Translation.of("msg_area_not_own"));
					}

				} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					WorldCoord pos = new WorldCoord(world, Coord.parseCoord(player));
					double plotPrice = pos.getTownBlock().getTown().getPlotTypePrice(pos.getTownBlock().getType());

					if (split.length > 1) {

						int areaSelectPivot = AreaSelectionUtil.getAreaSelectPivot(split);
						List<WorldCoord> selection;
						if (areaSelectPivot >= 0) {
							selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.subArray(split, areaSelectPivot + 1, split.length));
							selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection);
							if (selection.size() == 0) {
								player.sendMessage(Translation.of("msg_err_empty_area_selection"));
								return true;
							}
						} else {
							selection = new ArrayList<>();
							selection.add(pos);
						}

						// Check that it's not: /plot forsale within rect 3
						if (areaSelectPivot != 1) {
							try {
								// command was 'plot fs $'
								plotPrice = Double.parseDouble(split[1]);
								if (plotPrice < 0) {
									TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
									return true;
								}
							} catch (NumberFormatException e) {
								player.sendMessage(Translation.of("msg_error_must_be_num"));
								return true;
							}
						}

						for (WorldCoord worldCoord : selection) {
							TownBlock townBlock = worldCoord.getTownBlock();
							
							// Check if a group is present in a townblock
							if (townBlock.hasPlotObjectGroup()) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_plot_fs2", worldCoord));
								continue;
							}
							
							// Otherwise continue on normally.
							setPlotForSale(resident, worldCoord, plotPrice);
						}
					} else {
						// basic 'plot fs' command

						if (pos.getTownBlock().hasPlotObjectGroup()) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_plot_fs2", pos));
							return false;
						}
						
						setPlotForSale(resident, pos, plotPrice);
						
						// Update group price if neccessary.
						if (pos.getTownBlock().hasPlotObjectGroup())
							pos.getTownBlock().getPlotObjectGroup().addPlotPrice(plotPrice);
					}

				} else if (split[0].equalsIgnoreCase("perm") || split[0].equalsIgnoreCase("info")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (split.length > 1 && split[1].equalsIgnoreCase("hud")) {
						
						if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_HUD.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						
						plugin.getHUDManager().togglePermHUD(player);
						
					} else {
						if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
							TownyMessaging.sendMessage(player, TownyFormatter.getStatus(TownyUniverse.getInstance().getDataSource().getWorld(player.getLocation().getWorld().getName())));
						} else {
							TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
							TownyMessaging.sendMessage(player, TownyFormatter.getStatus(townBlock));
						}
					}

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test in the plottoggle.
					 */

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					// Test we are allowed to work on this plot
					plotTestOwner(resident, townBlock); // ignore the return as
					// we are only checking
					// for an exception
					
					// Make sure that the player is only operating on a single plot and not a plotgroup.
					if (townBlock.hasPlotObjectGroup()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_toggle"));
						return false;
					}

					plotToggle(player, new WorldCoord(world, Coord.parseCoord(player)).getTownBlock(), StringMgmt.remFirstArg(split));

				} else if (split[0].equalsIgnoreCase("set")) {

					split = StringMgmt.remFirstArg(split);
					
					if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

						player.sendMessage(ChatTools.formatTitle("/... set"));
						player.sendMessage(ChatTools.formatCommand("", "set", "[plottype]", "Ex: Inn, Wilds, Farm, Embassy etc"));
						player.sendMessage(ChatTools.formatCommand("", "set", "outpost", "Costs " + TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost())));
						player.sendMessage(ChatTools.formatCommand("", "set", "reset", "Removes a plot type"));
						player.sendMessage(ChatTools.formatCommand("", "set", "[name]", "Names a plot"));
						player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
						player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
						player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "ally off", ""));
						player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "friend build on", ""));
						player.sendMessage(Translation.of("plot_perms", "'friend'", "'resident'"));
						player.sendMessage(Translation.of("plot_perms_1"));

					} else if (split.length > 0) {

						if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(split[0].toLowerCase())))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						
						TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
						
						// Make sure that the player is only operating on a plot object group if one exists.
						if (townBlock.hasPlotObjectGroup()) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_set"));
							return false;
						}

						if (split[0].equalsIgnoreCase("perm")) {

							// Set plot level permissions (if the plot owner) or
							// Mayor/Assistant of the town.
							
							// Test we are allowed to work on this plot
							TownBlockOwner owner = plotTestOwner(resident, townBlock);

							setTownBlockPermissions(player, owner, townBlock, StringMgmt.remFirstArg(split));

							return true;

						} else if (split[0].equalsIgnoreCase("name")) {
							
							// Test we are allowed to work on this plot
							plotTestOwner(resident, townBlock);
							if (split.length == 1) {
								townBlock.setName("");
								TownyMessaging.sendMsg(player, Translation.of("msg_plot_name_removed"));
								townyUniverse.getDataSource().saveTownBlock(townBlock);
								return true;
							}
							
							// Test if the plot name contains invalid characters.
							if (!NameValidation.isBlacklistName(split[1])) {								
								townBlock.setName(StringMgmt.join(StringMgmt.remFirstArg(split), ""));

								//townBlock.setChanged(true);
								townyUniverse.getDataSource().saveTownBlock(townBlock);

								TownyMessaging.sendMsg(player, Translation.of("msg_plot_name_set_to", townBlock.getName()));

							} else {

								TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));

							}
							return true;
						} else if (split[0].equalsIgnoreCase("outpost")) {

							if (TownySettings.isAllowingOutposts()) {
								if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode()))
									throw new TownyException(Translation.of("msg_err_command_disable"));
								
								// Test we are allowed to work on this plot
								plotTestOwner(resident, townBlock);
								
								Town town = townBlock.getTown();
								TownyWorld townyWorld = townBlock.getWorld();
								boolean isAdmin = townyUniverse.getPermissionSource().isTownyAdmin(player);
								Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());
								
								if (OutpostUtil.OutpostTests(town, resident, townyWorld, key, isAdmin, true)) {
									// Test if they can pay.
									if (TownySettings.isUsingEconomy() && !town.getAccount().canPayFromHoldings(TownySettings.getOutpostCost())) 
										throw new TownyException(Translation.of("msg_err_cannot_afford_to_set_outpost"));
									 
									// Create a confirmation for setting outpost.
									Confirmation.runOnAccept(() -> {
										townBlock.setOutpost(true);
										 
										try {
											town.addOutpostSpawn(player.getLocation());
										} catch (TownyException e) {
											TownyMessaging.sendErrorMsg(e.getMessage());
											return;
										}

										//Make them pay, ignoring exception because we already know they can pay.
										if (TownySettings.isUsingEconomy() && TownySettings.getOutpostCost() > 0 )
											try {
												town.getAccount().withdraw(TownySettings.getOutpostCost(), "Plot Set Outpost");
											} catch (EconomyException ignored) {
											}
										townyUniverse.getDataSource().saveTown(town);
										townyUniverse.getDataSource().saveTownBlock(townBlock);
										TownyMessaging.sendMessage(player, Translation.of("msg_plot_set_cost", TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost()), Translation.of("outpost")));
									})
									.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost())))
									.sendTo(player);
	
								}
								return true;
							}
						}
						
						try {
							String plotTypeName = split[0];
							
							// Handle type being reset
							if (plotTypeName.equalsIgnoreCase("reset"))
								plotTypeName = "default";
							
							TownBlockType townBlockType = TownBlockType.lookup(plotTypeName);

							if (townBlockType == null)
								throw new TownyException(Translation.of("msg_err_not_block_type"));
							
							PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(townBlockType, townBlock, resident);
							BukkitTools.getPluginManager().callEvent(preEvent);

							if (!preEvent.isCancelled()) {
								setPlotType(resident, townBlock.getWorldCoord(), townBlockType);
								player.sendMessage(Translation.of("msg_plot_set_type", plotTypeName));
							} else {
								player.sendMessage(preEvent.getCancelMessage());
							}
						} catch (NotRegisteredException nre) {
							player.sendMessage(Translation.of("msg_err_not_part_town"));
						} catch (TownyException te){
							player.sendMessage(te.getLocalizedMessage());
						}

					} else {

						player.sendMessage(ChatTools.formatCommand("", "/plot set", "name", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "reset", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "shop|embassy|arena|wilds|spleef|inn|jail|farm|bank", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set perm", "?", ""));
					}

				} else if (split[0].equalsIgnoreCase("clear")) {

					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLEAR.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();

					if (townBlock != null) {

						/*
						  Only allow mayors or plot owners to use this command.
						 */
						if (townBlock.hasResident()) {
							if (!townBlock.isOwner(resident)) {
								player.sendMessage(Translation.of("msg_area_not_own"));
								return true;
							}

						} else if (!townBlock.getTown().equals(resident.getTown())) {
							player.sendMessage(Translation.of("msg_area_not_own"));
							return true;
						}

						PlotPreClearEvent preEvent = new PlotPreClearEvent(townBlock);
						BukkitTools.getPluginManager().callEvent(preEvent);
						
						if (preEvent.isCancelled()) {
							player.sendMessage(preEvent.getCancelMessage());
							return false;
						}
							

						for (String material : townyUniverse.getDataSource().getWorld(world).getPlotManagementMayorDelete())
							if (Material.matchMaterial(material) != null) {
								TownyRegenAPI.deleteTownBlockMaterial(townBlock, Material.getMaterial(material));
								player.sendMessage(Translation.of("msg_clear_plot_material", material));
							} else
								throw new TownyException(Translation.of("msg_err_invalid_property", material));

						// Raise an event for the claim
						BukkitTools.getPluginManager().callEvent(new PlotClearEvent(townBlock));

					} else {
						// Shouldn't ever reach here as a null townBlock should
						// be caught already in WorldCoord.
						player.sendMessage(Translation.of("msg_err_empty_area_selection"));
					}

				} else if (split[0].equalsIgnoreCase("group")) {

					return handlePlotGroupCommand(StringMgmt.remFirstArg(split), player);
					
				} else
					throw new TownyException(Translation.of("msg_err_invalid_property", split[0]));

			} catch (TownyException | EconomyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		}

		return true;
	}

	/**
	 * Returns a TownyPermissionChange object representing the change action
	 *
	 * @param player Player initiator
	 * @param townBlockOwner Resident/Town with the targeted permissions change
	 * @param townBlock Targeted town block
	 * @param split Permission arguments
	 * @return a TownyPermissionChange object
	 */
	public static TownyPermissionChange setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownBlock townBlock, String[] split) {
		TownyPermissionChange permChange = null;

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
			player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "friend build on", ""));
			return null;

		} else {

			TownyPermission perm = townBlock.getPermissions();

			if (split.length == 1) {

				if (split[0].equalsIgnoreCase("reset")) {

					// reset this townBlock permissions (by town/resident)
					permChange = new TownyPermissionChange(TownyPermissionChange.Action.RESET, false, townBlock);

					perm.change(permChange);
					townyUniverse.getDataSource().saveTownBlock(townBlock);

					TownyMessaging.sendMsg(player, Translation.of("msg_set_perms_reset_single"));
					// Reset all caches as this can affect everyone.
					plugin.resetCache();

					return permChange;

				} else {

					// Set all perms to On or Off
					// '/plot set perm off'

					try {
						boolean b = plugin.parseOnOff(split[0]);

						permChange = new TownyPermissionChange(TownyPermissionChange.Action.ALL_PERMS, b);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
						return null;
					}

				}

			} else if (split.length == 2) {
				boolean b;

				try {
					b = plugin.parseOnOff(split[1]);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
					return null;
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

					permChange = new TownyPermissionChange(TownyPermissionChange.Action.PERM_LEVEL, b, permLevel);
				}
				catch (IllegalArgumentException permLevelException) {
					// If it is not a perm level, then check if it is a action type
					try {
						TownyPermission.ActionType actionType = TownyPermission.ActionType.valueOf(split[0].toUpperCase());

						permChange = new TownyPermissionChange(TownyPermissionChange.Action.ACTION_TYPE, b, actionType);
					} catch (IllegalArgumentException actionTypeException) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
						return null;
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
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

				try {
					boolean b = plugin.parseOnOff(split[2]);

					permChange = new TownyPermissionChange(TownyPermissionChange.Action.SINGLE_PERM, b, permLevel, actionType);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

			}

			if (permChange != null)
				perm.change(permChange);

			townBlock.setChanged(true);
			townyUniverse.getDataSource().saveTownBlock(townBlock);
			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendMsg(player, Translation.of("msg_set_perms"));
				TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
				TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString2().replace("n", "t") : perm.getColourString2().replace("f", "r"))));
				TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
			}


			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
			Bukkit.getServer().getPluginManager().callEvent(event);

			// Reset all caches as this can affect everyone.
			plugin.resetCache();
			return permChange;
		}
	}

	/**
	 * Set the plot type if we are permitted
	 * 
	 * @param resident - Residen object.
	 * @param worldCoord - worldCoord.
	 * @param type - plot type.
	 * @throws TownyException - Exception.
	 * @throws EconomyException - Exception thrown if error with economy.
	 */
	private void setPlotType(Resident resident, WorldCoord worldCoord, TownBlockType type) throws TownyException, EconomyException {
		TownBlock townBlock = worldCoord.getTownBlock();

		// Test we are allowed to work on this plot
		plotTestOwner(resident, townBlock); // ignore the return as we
		// are only checking for an
		// exception

		townBlock.setType(type, resident);
		Town town = resident.getTown();
		if (townBlock.isJail()) {
			Player p = TownyAPI.getInstance().getPlayer(resident);
			if (p == null) {
				throw new TownyException(Translation.of("msg_err_not_part_town"));
			}
			town.addJailSpawn(p.getLocation());
		}

		
	}

	/**
	 * Set the plot for sale/not for sale if permitted
	 * 
	 * @param resident - Resident Object.
	 * @param worldCoord - WorldCoord.
	 * @param forSale - Price.
	 * @throws TownyException - Exception.
	 */
	public void setPlotForSale(Resident resident, WorldCoord worldCoord, double forSale) throws TownyException {

		if (resident.hasTown()) {
			try {
				TownBlock townBlock = worldCoord.getTownBlock();

				// Test we are allowed to work on this plot
				plotTestOwner(resident, townBlock); // ignore the return as we
				// are only checking for an
				// exception
				if (forSale > TownySettings.getMaxPlotPrice() ) {
					townBlock.setPlotPrice(TownySettings.getMaxPlotPrice());
				} else {
					townBlock.setPlotPrice(forSale);
				}

				if (forSale != -1) {
					TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
					if (townBlock.getTown() != resident.getTown())
						TownyMessaging.sendMessage(resident, TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
				} else {
					Player p = TownyAPI.getInstance().getPlayer(resident);
					if (p == null) {
						throw new TownyException("Player could not be found.");
					}
					p.sendMessage(Translation.of("msg_plot_set_to_nfs"));
				}

				// Save this townblock so the for sale status is remembered.
				TownyUniverse.getInstance().getDataSource().saveTownBlock(townBlock);

			} catch (NotRegisteredException e) {
				throw new TownyException(Translation.of("msg_err_not_part_town"));
			}
		} else
			throw new TownyException(Translation.of("msg_err_must_belong_town"));
	}

	public void setGroupForSale(Resident resident, PlotGroup group, double price) throws TownyException {
		group.setPrice(price);

		if (resident.hasTown()) {
			try {

				// exception
				if (price > TownySettings.getMaxPlotPrice()) {
					group.setPrice(TownySettings.getMaxPlotPrice());
				} else {
					group.setPrice(price);
				}

				if (price != -1) {
					TownyMessaging.sendPrefixedTownMessage(resident.getTown(), Translation.of("msg_plot_group_set_for_sale", group.getName()));
					if (group.getTown() != resident.getTown())
						TownyMessaging.sendMessage(resident, Translation.of("msg_plot_group_set_for_sale", group.getName()));
				} else {
					Player p = TownyAPI.getInstance().getPlayer(resident);
					if (p == null) {
						throw new TownyException("Player could not be found.");
					}
					p.sendMessage(Translation.of("msg_plot_set_to_nfs"));
					
					// Since the groups are stored in towns we need to save the town.
					TownyUniverse.getInstance().getDataSource().saveTown(group.getTown());
				}
			} catch (NotRegisteredException e) {
					throw new TownyException(Translation.of("msg_err_not_part_town"));
			}
		}
	}

	/**
	 * Toggle the plots flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 * 
	 * @param player - Player.
	 * @param townBlock - TownBlock object.
	 * @param split  - Current command arguments.
	 */
	public void plotToggle(Player player, TownBlock townBlock, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/plot toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "mobs", ""));
		} else {

			try {

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				if (split[0].equalsIgnoreCase("pvp")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					
					if (TownySettings.getPVPCoolDownTime() > 0 && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode())) {
						// Test to see if the pvp cooldown timer is active for the town this plot belongs to.
						if (CooldownTimerTask.hasCooldown(townBlock.getTown().getName(), CooldownType.PVP))
							throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(townBlock.getTown().getName(), CooldownType.PVP)));
	
						// Test to see if the pvp cooldown timer is active for this plot.
						if (CooldownTimerTask.hasCooldown(townBlock.getWorldCoord().toString(), CooldownType.PVP))
							throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(townBlock.getWorldCoord().toString(), CooldownType.PVP)));
					}
					
					// Prevent plot pvp from being enabled if admin pvp is disabled
					if (townBlock.getTown().isAdminDisabledPVP() && !townBlock.getPermissions().pvp)
						throw new TownyException(Translation.of("msg_err_admin_controlled_pvp_prevents_you_from_changing_pvp", "adminDisabledPVP", "on"));
					
					// Prevent plot pvp from being disabled if admin pvp is enabled
					if (townBlock.getTown().isAdminEnabledPVP() && townBlock.getPermissions().pvp)
						throw new TownyException(Translation.of("msg_err_admin_controlled_pvp_prevents_you_from_changing_pvp", "adminEnabledPVP", "off"));

					townBlock.getPermissions().pvp = !townBlock.getPermissions().pvp;
					// Add a cooldown timer for this plot.
					if (TownySettings.getPVPCoolDownTime() > 0 && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode()))
						CooldownTimerTask.addCooldownTimer(townBlock.getWorldCoord().toString(), CooldownType.PVP);
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_pvp", "Plot", townBlock.getPermissions().pvp ? Translation.of("enabled") : Translation.of("disabled")));

				} else if (split[0].equalsIgnoreCase("explosion")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().explosion = !townBlock.getPermissions().explosion;
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_expl", "the Plot", townBlock.getPermissions().explosion ? Translation.of("enabled") : Translation.of("disabled")));

				} else if (split[0].equalsIgnoreCase("fire")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().fire = !townBlock.getPermissions().fire;
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_fire", "the Plot", townBlock.getPermissions().fire ? Translation.of("enabled") : Translation.of("disabled")));

				} else if (split[0].equalsIgnoreCase("mobs")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					townBlock.getPermissions().mobs = !townBlock.getPermissions().mobs;
					
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_mobs", "the Plot", townBlock.getPermissions().mobs ? Translation.of("enabled") : Translation.of("disabled")));

				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "plot"));
					return;
				}

				townBlock.setChanged(true);

				//Change settings event
				TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
				Bukkit.getServer().getPluginManager().callEvent(event);

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
			
			townyUniverse.getDataSource().saveTownBlock(townBlock);
		}
	}

	/**
	 * Toggle the plot group flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 *
	 * @param player - Player.
	 * @param plotGroup - PlotObjectGroup object.
	 * @param split  - Current command arguments.
	 */
	public void plotGroupToggle(Player player, PlotGroup plotGroup, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/plot group toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "mobs", ""));
		} else {

			try {
				// We need to keep an ending string to show the message only after the transaction is over,
				// to prevent chat log spam.
				String endingMessage = "";
				
				for (TownBlock groupBlock : plotGroup.getTownBlocks()) {
					if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (split[0].equalsIgnoreCase("pvp")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));

						if (TownySettings.getPVPCoolDownTime() > 0) {
							// Test to see if the pvp cooldown timer is active for the town this plot belongs to.
							if (CooldownTimerTask.hasCooldown(groupBlock.getTown().getName(), CooldownType.PVP))
								throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(groupBlock.getTown().getName(), CooldownType.PVP)));

							// Test to see if the pvp cooldown timer is active for this plot.
							if (CooldownTimerTask.hasCooldown(groupBlock.getWorldCoord().toString(), CooldownType.PVP))
								throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(groupBlock.getWorldCoord().toString(), CooldownType.PVP)));
						}

						groupBlock.getPermissions().pvp = !groupBlock.getPermissions().pvp;
						// Add a cooldown timer for this plot.
						if (TownySettings.getPVPCoolDownTime() > 0)
							CooldownTimerTask.addCooldownTimer(groupBlock.getWorldCoord().toString(), CooldownType.PVP);
						endingMessage = Translation.of("msg_changed_pvp", "Plot Group", groupBlock.getPermissions().pvp ? Translation.of("enabled") : Translation.of("disabled"));

					} else if (split[0].equalsIgnoreCase("explosion")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));
						groupBlock.getPermissions().explosion = !groupBlock.getPermissions().explosion;
						endingMessage = Translation.of("msg_changed_fire", "the Plot Group", groupBlock.getPermissions().fire ? Translation.of("enabled") : Translation.of("disabled"));

					} else if (split[0].equalsIgnoreCase("fire")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));
						groupBlock.getPermissions().fire = !groupBlock.getPermissions().fire;
						endingMessage =  Translation.of("msg_changed_fire", "the Plot Group", groupBlock.getPermissions().fire ? Translation.of("enabled") : Translation.of("disabled"));

					} else if (split[0].equalsIgnoreCase("mobs")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));
						groupBlock.getPermissions().mobs = !groupBlock.getPermissions().mobs;
						endingMessage =  Translation.of("msg_changed_mobs", "the Plot Group", groupBlock.getPermissions().mobs ? Translation.of("enabled") : Translation.of("disabled"));

					} else {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "plot"));
						return;
					}

					groupBlock.setChanged(true);

					//Change settings event
					TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(groupBlock);
					Bukkit.getServer().getPluginManager().callEvent(event);
					
					// Save
					townyUniverse.getDataSource().saveTownBlock(groupBlock);
				}
				
				// Finally send the message.
				TownyMessaging.sendMessage(player, endingMessage);
				

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

			
		}
	}

	/**
	 * Check the world and town settings to see if we are allowed to alter these
	 * settings
	 * 
	 * @param player
	 * @param townBlock
	 * @param split
	 * @throws TownyException if toggle is not permitted
	 */
	private void toggleTest(Player player, TownBlock townBlock, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

		split = split.toLowerCase();

		if (split.contains("mobs")) {
			if (townBlock.getWorld().isForceTownMobs())
				throw new TownyException(Translation.of("msg_world_mobs"));
		}

		if (split.contains("fire")) {
			if (townBlock.getWorld().isForceFire())
				throw new TownyException(Translation.of("msg_world_fire"));
		}

		if (split.contains("explosion")) {
			if (townBlock.getWorld().isForceExpl())
				throw new TownyException(Translation.of("msg_world_expl"));
		}

		if (split.contains("pvp")) {
			if (townBlock.getWorld().isForcePVP())
				throw new TownyException(Translation.of("msg_world_pvp"));
		}
		if ((split.contains("pvp")) || (split.trim().equalsIgnoreCase("off"))) {
			if (townBlock.getType().equals(TownBlockType.ARENA))
				throw new TownyException(Translation.of("msg_plot_pvp"));
		}
	}

	/**
	 * Test the townBlock to ensure we are either the plot owner, or the
	 * mayor/assistant
	 * 
	 * @param resident - Resident Object.
	 * @param townBlock - TownBlock Object.
	 * @return - returns owner of plot.
	 * @throws TownyException - Exception.
	 */
	public TownBlockOwner plotTestOwner(Resident resident, TownBlock townBlock) throws TownyException {

		Player player = BukkitTools.getPlayer(resident.getName());
		boolean isAdmin = TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player);

		if (townBlock.hasResident()) {
			
			Resident owner = townBlock.getResident();
			if ((!owner.hasTown() 
					&& (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())))
					&& (townBlock.getTown() == resident.getTown()))				
					return owner;
					
			boolean isSameTown = (resident.hasTown()) && resident.getTown() == owner.getTown();

			if ((resident == owner)
					|| ((isSameTown) && (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())))
					|| ((townBlock.getTown() == resident.getTown())) && (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
					|| isAdmin) {

				return owner;
			}

			// Not the plot owner or the towns mayor or an admin.
			throw new TownyException(Translation.of("msg_area_not_own"));

		} else {

			Town owner = townBlock.getTown();
			boolean isSameTown = (resident.hasTown()) && resident.getTown() == owner;

			if (isSameTown && !BukkitTools.getPlayer(resident.getName()).hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
				throw new TownyException(Translation.of("msg_not_mayor_ass"));

			if (!isSameTown && !isAdmin)
				throw new TownyException(Translation.of("msg_err_not_part_town"));

			return owner;
		}

	}
	
	private boolean handlePlotGroupCommand(String[] split, Player player) throws TownyException {

		Resident resident;
		String world;

		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		resident = townyUniverse.getDataSource().getResident(player);
		world = player.getWorld().getName();
		
		TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
		Town town = townBlock.getTown();

		// Test we are allowed to work on this plot
		plotTestOwner(resident, townBlock);

		if (split.length <= 0 || split[0].equalsIgnoreCase("?")) {

			player.sendMessage(ChatTools.formatTitle("/plot group"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "add | new | create", "[name]", "Ex: /plot group new ExpensivePlots"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "remove", "", "Removes a plot from the specified group."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "rename", "[newName]", "Renames the group you are standing in."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "set", "...", "Ex: /plot group set perm resident on."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "toggle", "...", "Ex: /plot group toggle [pvp|fire|mobs]"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "forsale | fs", "[price]", "Ex: /plot group forsale 50"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "notforsale | nfs", "", "Ex: /plot group notforsale"));

			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translation.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			
			return true;
		}

		if (split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("new") || split[0].equalsIgnoreCase("create")) {

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_ADD.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// Add the group to the new plot.
			PlotGroup newGroup = null;

			if (townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_already_belongs_to_a_group", townBlock.getPlotObjectGroup().getName()));
				return false;
			}

			if (split.length == 2) {
				// Create a brand new plot group.
				UUID plotGroupID = townyUniverse.generatePlotGroupID();
				String plotGroupName = split[1];

				newGroup = new PlotGroup(plotGroupID, plotGroupName, town);

				// Don't add the group to the town data if it's already there.
				if (town.hasPlotGroupName(newGroup.getName())) {
					newGroup = town.getPlotObjectGroupFromName(newGroup.getName());
				}

				townBlock.setPlotObjectGroup(newGroup);

				// Check if a plot price is available.
				if (!(townBlock.getPlotPrice() < 0)) {
					newGroup.addPlotPrice(townBlock.getPlotPrice());
				}

				// Add the plot group to the town set.
				town.addPlotGroup(newGroup);
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_group_name_required"));
				return false;
			}

			townyUniverse.getDataSource().savePlotGroupList();

			// Save changes.
			townyUniverse.getDataSource().savePlotGroup(newGroup);
			townyUniverse.getDataSource().saveTownBlock(townBlock);
			townyUniverse.getDataSource().saveTown(town);

			TownyMessaging.sendMsg(player, Translation.of("msg_plot_was_put_into_group_x", townBlock.getX(), townBlock.getZ(), newGroup.getName()));

		} else if (split[0].equalsIgnoreCase("remove")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_REMOVE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			String name = townBlock.getPlotObjectGroup().getName();
			// Remove the plot from the group.
			townBlock.getPlotObjectGroup().removeTownBlock(townBlock);

			// Detach group from townblock.
			townBlock.removePlotObjectGroup();

			// Save
			TownyUniverse.getInstance().getDataSource().saveTownBlock(townBlock);
			TownyMessaging.sendMsg(player, Translation.of("msg_plot_was_removed_from_group_x", townBlock.getX(), townBlock.getZ(), name));

		} else if (split[0].equalsIgnoreCase("rename")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_RENAME.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			String newName = split[1];

			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}

			String oldName = townBlock.getPlotObjectGroup().getName();
			// Change name;
			TownyUniverse.getInstance().getDataSource().renameGroup(townBlock.getPlotObjectGroup(), newName);
			TownyMessaging.sendMsg(player, Translation.of("msg_plot_renamed_from_x_to_y", oldName, newName));

		} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_FORSALE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// This means the player wants to fs the plot group they are in.
			PlotGroup group = townBlock.getPlotObjectGroup();
			
			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			
			if (split.length < 2) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_group_specify_price"));
				return false;
			}

			int price = Integer.parseInt(split[1]);

			group.setPrice(price);
			
			// Save
			TownyUniverse.getInstance().getDataSource().savePlotGroup(group);
			TownyUniverse.getInstance().getDataSource().savePlotGroupList();

			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_put_group_up_for_sale", player.getName(), group.getName(), TownyEconomyHandler.getFormattedBalance(group.getPrice())));
			
		} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_NOTFORSALE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// This means the player wants to nfs the plot group they are in.
			PlotGroup group = townBlock.getPlotObjectGroup();
			
			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}

			group.setPrice(-1);

			// Save
			TownyUniverse.getInstance().getDataSource().savePlotGroup(group);
			TownyUniverse.getInstance().getDataSource().savePlotGroupList();

			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));
		} else if (split[0].equalsIgnoreCase("toggle")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TOGGLE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (townBlock.getPlotObjectGroup() == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			
			// Create confirmation.
			PlotGroup plotGroup = townBlock.getPlotObjectGroup();
			String title = Translation.of("msg_plot_group_toggle_confirmation", townBlock.getPlotObjectGroup().getTownBlocks().size()) + " " + Translation.of("are_you_sure_you_want_to_continue");
			Confirmation.runOnAccept(() -> {
				// Perform the toggle.
				new PlotCommand(Towny.getPlugin()).plotGroupToggle(player, plotGroup, StringMgmt.remArgs(split, 1));
			})
			.setTitle(title)
			.sendTo(player);
			
			return true;
		} else if (split[0].equalsIgnoreCase("set")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_SET.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// Check if group is present.
			if (townBlock.getPlotObjectGroup() == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			TownBlockOwner townBlockOwner = plotTestOwner(resident, townBlock);
			
			if (split.length < 2) {
				player.sendMessage(ChatTools.formatTitle("/plot group set"));
				if (townBlockOwner instanceof Town)
					player.sendMessage(ChatTools.formatCommand("Level", "[resident/nation/ally/outsider]", "", ""));
				if (townBlockOwner instanceof Resident)
					player.sendMessage(ChatTools.formatCommand("Level", "[friend/town/ally/outsider]", "", ""));				
				player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "[on/off]", "Toggle all permissions"));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "[level/type] [on/off]", ""));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "[level] [type] [on/off]", ""));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "reset", ""));
				player.sendMessage(ChatTools.formatCommand("Eg", "/plot group set perm", "friend build on", ""));				
				player.sendMessage(ChatTools.formatCommand("/plot group set", "[townblocktype]", "", "Farm, Wilds, Bank, Embassy, etc."));
				return false;
			}

			if (split[1].equalsIgnoreCase("perm")) {
				
				// Set plot level permissions (if the plot owner) or
				// Mayor/Assistant of the town.
				
				PlotGroup plotGroup = townBlock.getPlotObjectGroup();
				
				Runnable permHandler = () -> {

					Iterator<TownBlock> townBlockIterator = plotGroup.getTownBlocks().iterator();
					
					if (!townBlockIterator.hasNext()) {
						return;
					}
					
					// Test the waters
					TownBlock tb = townBlockIterator.next();

					// setTownBlockPermissions returns a towny permission change object
					TownyPermissionChange permChange = PlotCommand.setTownBlockPermissions(player, townBlockOwner, tb, StringMgmt.remArgs(split, 2));
					// If the perm change object is not null
					if (permChange != null) {

						// A simple index loop starting from the second element
						while (townBlockIterator.hasNext()) {
							tb = townBlockIterator.next();

							tb.getPermissions().change(permChange);

							tb.setChanged(true);
							townyUniverse.getDataSource().saveTownBlock(tb);

							// Change settings event
							TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(tb);
							Bukkit.getServer().getPluginManager().callEvent(event);
						}

						plugin.resetCache();
						
						TownyPermission perm = Iterables.getFirst(plotGroup.getTownBlocks(), null).getPermissions();
						TownyMessaging.sendMsg(player, Translation.of("msg_set_perms"));
						TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
						TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString2().replace("n", "t") : perm.getColourString2().replace("f", "r"))));
						TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + (!(CombatUtil.preventPvP(townBlock.getWorld(), townBlock)) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
					}
				};

				String title = Translation.of("msg_plot_group_set_perm_confirmation", townBlock.getPlotObjectGroup().getTownBlocks().size()) + " " + Translation.of("are_you_sure_you_want_to_continue");
				// Create confirmation.
				Confirmation.runOnAccept(permHandler)
					.setTitle(title)
					.sendTo(player);
				
				return true;
			}
			String plotTypeName = split[1];

			// Stop setting plot groups to Jail plot, because that would set a spawn point for each plot in the location of the player.			
			if (plotTypeName.equalsIgnoreCase("jail")) {
				throw new TownyException(Translation.of(Translation.of("msg_err_cannot_set_group_to_jail")));
			}

			// Handle type being reset
			if (plotTypeName.equalsIgnoreCase("reset"))
				plotTypeName = "default";

			TownBlockType townBlockType = TownBlockType.lookup(plotTypeName);

			if (townBlockType == null)
				throw new TownyException(Translation.of("msg_err_not_block_type"));
				
			for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
				try {
					// Allow for PlotPreChangeTypeEvent to trigger
					PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(townBlockType, tb, resident);
					BukkitTools.getPluginManager().callEvent(preEvent);

					if (!preEvent.isCancelled()) {
						setPlotType(resident, tb.getWorldCoord(), townBlockType);
					} else {
						player.sendMessage(preEvent.getCancelMessage());
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_could_not_set_group_type") + e.getMessage());
					return false;
				}
			}

			TownyMessaging.sendMsg(player, Translation.of("msg_set_group_type_to_x", plotTypeName));
			
		} else {

			player.sendMessage(ChatTools.formatTitle("/plot group"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "add | new | create", "[name]", "Ex: /plot group new ExpensivePlots"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "remove", "", "Removes a plot from the specified group."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "rename", "[newName]", "Renames the group you are standing in."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "set", "...", "Ex: /plot group set perm resident on."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "toggle", "...", "Ex: /plot group toggle [pvp|fire|mobs]"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "forsale | fs", "[price]", "Ex: /plot group forsale 50"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "notforsale | nfs", "", "Ex: /plot group notforsale"));

			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translation.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			
			return true;
		}
		
		return false;
	}

}
