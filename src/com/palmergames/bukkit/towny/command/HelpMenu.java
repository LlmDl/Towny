package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.command.CommandSender;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum HelpMenu {
	
	GENERAL_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder()
				.addTitle(Translation.of("help_0"))
				.add(Translation.of("help_1"))
				.add(ChatTools.formatCommand("/resident", "?", "") + ", "
					+ ChatTools.formatCommand("/town", "?", "") + ", "
					+ ChatTools.formatCommand("/nation", "?", "") + ", "
					+ ChatTools.formatCommand("/plot", "?", "") + ", "
					+ ChatTools.formatCommand("/towny", "?", ""))
				.add(ChatTools.formatCommand("/tc", "[msg]", Translation.of("help_2")) + ", "
					+ ChatTools.formatCommand("/nc", "[msg]", Translation.of("help_3")).trim())
				.add(Translation.of("admin_sing"), "/townyadmin", "?", "");
		}
	},

	// Towny Help
	HELP {
		@Override
		public MenuBuilder load() {
			return new MenuBuilder("towny", "General help for Towny")
				.add("map", "Displays a map of the nearby townblocks")
				.add("prices", "Display the prices used with Economy")
				.add("top", "Display highscores")
				.add("time", "Display time until a new day")
				.add("universe", "Displays stats")
				.add("v", "Displays the version of Towny")
				.add("war", "'/towny war' for more info");
		}
	},
	
	TA_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin", Translation.of("admin_panel_1"))
				.add("set [] .. []", "")
				.add("unclaim [radius]", "")
				.add("plot", "")
				.add("givebonus [town/player] [num]", "")
				.add("toggle peaceful/war/debug/devmode", "")
				.add("resident/town/nation", "")
				.add("tpplot {world} {x} {z}", "")
				.add("checkperm {name} {node}", "")
				.add("reload", Translation.of("admin_panel_2"))
				.add("reset", "")
				.add("backup", "")
				.add("mysqldump", "")
				.add("database [save/load]", "")
				.add("newday", Translation.of("admin_panel_3"))
				.add("purge [number of days]", "")
				.add("delete [] .. []", "delete a residents data files.");
		}
	},
	
	TA_TOWN {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin town")
				.add("new [name] [mayor]", "")
				.add("[town]", "")
				.add("[town] add/kick [] .. []", "")
				.add("[town] rename [newname]", "")
				.add("[town] delete", "")
				.add("[town] spawn", "")
				.add("[town] outpost #", "")
				.add("[town] rank", "")
				.add("[town] set", "")
				.add("[town] toggle", "")
				.add("[town] meta", "")
				.add("[town] deposit [amount]", "")
				.add("[town] withdraw [amount]", "")
				.add("[town] bankhistory", "")
				.add("[town] outlaw [add|remove] [name]", "")
				.add("[town] leavenation", "");
		}
	},

	TA_TOWN_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("ta town {townname} toggle")
				.add("pvp", "")
				.add("forcepvp", "")
				.add("public", "")
				.add("explosion", "")
				.add("fire", "")
				.add("mobs", "")
				.add("taxpercent", "")
				.add("open", "");
		}
	},

	
	TA_NATION {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin nation")
				.add("new [name] [capital]", "")
				.add("[nation]", "")
				.add("[nation] add [] .. []", "")
				.add("[nation] kick [] .. []", "")
				.add("[nation] rename [newname]", "")
				.add("[nation] delete", "")
				.add("[nation] recheck", "")
				.add("[nation] toggle", "")
				.add("[nation] set", "")
				.add("[nation] deposit [amount]", "")
				.add("[nation] withdraw [amount]", "")
				.add("[nation] bankhistory", "")
				.add("[oldnation] merge [newnation]", "")
				.add("rank [add/remove] [resident] [rank]", "");
		}
	},

	TA_UNCLAIM {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin unclaim", Translation.of("admin_sing"),
				Translation.of("townyadmin_help_1"))
				.add("[radius]", Translation.of("townyadmin_help_2"));
		}
	},
	
	TA_DATABASE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin database")
				.add("save", "")
				.add("load", "")
				.add("remove ?", "");
		}
	},
	
	TA_PLOT {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin plot")
				.add("claim [player]", "")
				.add("meta", "")
				.add("meta set [key] [value]", "")
				.add("meta [add|remove] [key]", "");
		}
	},
	
	TA_RESIDENT {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin resident")
				.add("[resident]", "")
				.add("[resident] rename [newname]", "")
				.add("[resident] friend... [add|remove] [resident]", "")
				.add("[resident] friend... |list|clear]", "");
		}
	},

	TA_RESIDENT_FRIEND {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin resident [resident] friend")
				.add("[add|remove] [resident]", "")
				.add("list", "")
				.add("clear", "");
		}
	},
	
	TA_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin toggle")
				.add("wildernessuse", "")
				.add("regenerations", "")
				.add("war", "")
				.add("peaceful", "")
				.add("devmode", "")
				.add("debug", "")
				.add("townwithdraw/nationwithdraw", "")
				.add("[resident]", "");
		}
	},
	
	TA_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set")
				.add("mayor [town] " + Translation.of("town_help_2"), "")
				.add("mayor [town] npc", "")
				.add("capital [town] [nation]", "")
				.add("title [resident] [title]", "")
				.add("surname [resident] [surname]", "")
				.add("plot [town]", "");
		}
	},
	
	TA_SET_MAYOR {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set mayor")
				.add("[town] " + Translation.of("town_help_2"), "")
				.add("[town] npc", "");
		}
	},

	TA_SET_CAPITAL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set capital")
				.add("capital [town] [nation]", "");
		}
	},

	TA_SET_PLOT {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set plot")
				.add("[town]",  Translation.of("msg_admin_set_plot_help_1"))
				.add("[town name] {rect|circle} {radius}", Translation.of("msg_admin_set_plot_help_2"))
				.add("[town name] {rect|circle} auto", Translation.of("msg_admin_set_plot_help_2"));
		}
	},
	
	TA_PURGE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin purge")
				.add("[number of days] {townless}", "")
				.add("", "Removes offline residents not seen for this duration.")
				.add("", "Optional {townless} flag limits purge to only people that have no town.");
		}
	},
	
	TA_TOWN_META {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin town [townname] meta")
				.add("", "View the metadata stored in a town.")
				.add("set [key] [value]", "Set the key to the value.")
				.add("add [key]", "Add a key of a registered data field.")
				.add("remove [key]", "Remove a key from the town.");
		}
	},

	TA_PLOT_META {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin plot meta")
				.add("", "View the metadata stored in the plot you are stood in.")
				.add("set [key] [value]", "Set the key to the value.")
				.add("add [key]", "Add a key of a registered data field.")
				.add("remove [key]", "Remove a key from the plot.");
		}
	},
	
	TA_RELOAD {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin reload")
				.add("database", "Reloads database")
				.add("config", "Reloads config")
				.add("lang", "Reloads language file.")
				.add("perms", "Reloads Towny permissions.")
				.add("all", "Reloads all components of towny.");
		}
	},
	
	TA_DEPOSITALL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin depositall")
				.add("[amount]", "Deposit the given amount into all town and nation banks.");
		}
	},
	
	TOWNYWORLD_HELP {
		@Override
		protected MenuBuilder load(MenuBuilder builder) {
			return builder
				.add(Translation.of("world_help_2"), Translation.of("world_help_3"))
				.add("list", Translation.of("world_help_4"))
				.add("toggle", "")
				.add(Translation.of("admin_sing"), "set [] .. []", "")
				.add(Translation.of("admin_sing"), "regen", Translation.of("world_help_5"));
		}

		@Override
		protected MenuBuilder load() {
			return load(new MenuBuilder("townyworld", Translation.of("world_help_1")));
		}
	},

	TOWNYWORLD_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return TOWNYWORLD_HELP.load(new MenuBuilder("townyworld {world}", Translation.of("world_help_1")));
		}
	},

	TOWNYWORLD_SET {
		@Override
		protected MenuBuilder load(MenuBuilder builder) {
			return builder.add("wildname [name]", "");
		}

		@Override
		protected MenuBuilder load() {
			return load(new MenuBuilder("townyworld set"));
		}
	},

	TOWNYWORLD_SET_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return TOWNYWORLD_SET.load(new MenuBuilder("townyworld set {world}"));
		}
	},
	
	TOWNYWORLD_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyworld toggle")
				.add("claimable", "")
				.add("usingtowny", "")
				.add("warallowed", "")
				.add("pvp/forcepvp", "")
				.add("friendlyfire", "")
				.add("explosion/forceexplosion", "")
				.add("fire/forcefire", "")
				.add("townmobs/wildernessmobs/worldmobs", "")
				.add("revertunclaim", "")
				.add("revertentityexpl/revertblockexpl", "")
				.add("plotcleardelete", "")
				.add("unclaimblockdelete", "");
		}
	},

	TOWNYWORLD_TOGGLE_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyworld toggle {worldname}")
				.add("claimable", "")
				.add("usingtowny", "")
				.add("warallowed", "")
				.add("pvp/forcepvp", "")
				.add("friendlyfire", "")
				.add("explosion/forceexplosion", "")
				.add("fire/forcefire", "")
				.add("townmobs/wildernessmobs/worldmobs", "")
				.add("revertunclaim", "")
				.add("revertentityexpl/revertblockexpl", "")
				.add("plotcleardelete", "")
				.add("unclaimblockdelete", "");
		}
	},
	
	TOWN_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town", Translation.of("town_help_1"))
				.add("[town]", Translation.of("town_help_3"))
				.add("new [name]", Translation.of("town_help_11"))
				.add("here", Translation.of("town_help_4"))
				.add("list", "")
				.add("online", Translation.of("town_help_10"))
				.add("leave", "")
				.add("reslist", "")
				.add("ranklist", "")
				.add("outlawlist", "")
				.add("plots", "")
				.add("outlaw add/remove [name]", "")
				.add("say", "[message]")
				.add("spawn", Translation.of("town_help_5"))
				.add(Translation.of("res_sing"), "deposit [$]", "")
				.add(Translation.of("res_sing"), "rank add/remove [resident] [rank]", "")
				.add(Translation.of("mayor_sing"), "mayor ?", Translation.of("town_help_8"))
				.add(Translation.of("admin_sing"), "delete [town]", "");
		}
	},
	
	TOWN_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town", Translation.of("town_help_1"))
				.add("[town]", Translation.of("town_help_3"))
				.add("list", "");
		}
	},
	
	TOWN_LIST {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town list")
				.add("{page #}", "")
				.add("{page #} by residents", "")
				.add("{page #} by open", "")
				.add("{page #} by balance", "")
				.add("{page #} by name", "")
				.add("{page #} by townblocks", "")
				.add("{page #} by online", "");
		}
	},
	
	TOWN_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town set")
				.add("board [message ... ]", "")
				.add("mayor " + Translation.of("town_help_2"), "")
				.add("homeblock", "")
				.add("spawn/outpost/jail", "")
				.add("perm ...", "'/town set perm' " + Translation.of("res_5"))
				.add("taxes [$]", "")
				.add("[plottax/shoptax/embassytax] [$]", "")
				.add("[plotprice/shopprice/embassyprice] [$]", "")
				.add("spawncost [$]", "")
				.add("name [name]", "")
				.add("tag [upto 4 letters] or clear", "")
				.add("title/surname [resident] [text]", "")
				.add("taxpercentcap [amount]", "");
		}
	},
	
	TOWN_TOGGLE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town toggle")
				.add("pvp", "")
				.add("public", "")
				.add("explosion", "")
				.add("fire", "")
				.add("mobs", "")
				.add("taxpercent", "")
				.add("open", "");
		}
	},
	
	TOWN_CLAIM {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town claim")
				.add("", Translation.of("msg_block_claim"))
				.add("outpost", Translation.of("mayor_help_3"))
				.add("[auto]", Translation.of("mayor_help_5"))
				.add("[circle/rect] [radius]", Translation.of("mayor_help_4"))
				.add("[circle/rect] auto", Translation.of("mayor_help_5"));
		}
	},
	
	TOWN_UNCLAIM {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town unclaim")
				.add("", Translation.of("mayor_help_6"))
				.add("[circle/rect] [radius]", Translation.of("mayor_help_7"))
				.add("all", Translation.of("mayor_help_8"));
		}
	},
	
	TOWN_JAIL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town jail")
				.add("list", "")
				.add("[resident]", "")
				.add("[resident] [hours]", "")
				.add("[resident] [hours] [jail]", "")
				.add("[resident] [hours] [jail] [cell]", "");
		}
	},
	
	TOWN_UNJAIL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town unjail")
				.add("[resident]", "");
		}
	},
	
	TOWN_PURGE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town purge")
				.add("[days]", "");
		}
	},
	

	TOWN_INVITE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town invite")
				.add("[player]", Translation.of("town_invite_help_1"))
				.add("-[player]", Translation.of("town_invite_help_2"))
				.add("sent", Translation.of("town_invite_help_3"))
				.add("received", Translation.of("town_invite_help_4"))
				.add("accept [nation]", Translation.of("town_invite_help_5"))
				.add("deny [nation]", Translation.of("town_invite_help_6"));
		}
	},

	RESIDENT_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident", Translation.of("res_1"))
				.add(Translation.of("res_2"), Translation.of("res_3"))
				.add("list", Translation.of("res_4"))
				.add("tax", "")
				.add("jail", "")
				.add("toggle", "[mode]...[mode]")
				.add("set [] .. []", "'/resident set' " + Translation.of("res_5"))
				.add("friend [add/remove] " + Translation.of("res_2"), Translation.of("res_6"))
				.add("friend [add+/remove+] " + Translation.of("res_2") + " ", Translation.of("res_7"))
				.add("spawn", "");
		}
	},
	
	RESIDENT_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident", Translation.of("res_1"))
				.add(Translation.of("res_2"), Translation.of("res_3"))
				.add("list", Translation.of("res_4"));
		}
	},
	
	RESIDENT_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident toggle")
				.add("pvp", "")
				.add("fire", "")
				.add("mobs", "")
				.add("explosion", "")
				.add("plotborder", "")
				.add("constantplotborder", "")
				.add("ignoreplots", "")
				.add("townclaim", "")
				.add("map", "")
				.add("reset|clear", "")
				.add("spy", "");
		}
	},
			
	RESIDENT_JAIL_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident jail", "")
				.add("", "/resident jail", "paybail", "Pays the bail cost to get out of jail.")
				.add(Colors.LightBlue + Translation.of("msg_resident_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmount())
				.add(Colors.LightBlue + Translation.of("msg_mayor_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmountMayor())
				.add(Colors.LightBlue + Translation.of("msg_king_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmountKing());
		}
	},

	PLOT_HELP {
		@Override
		protected MenuBuilder load() {
			String resReq = Translation.of("res_sing");
			return new MenuBuilder("plot", resReq + "/" + Translation.of("mayor_sing"), "")
				.add(resReq, "/plot claim", "", Translation.of("msg_block_claim"))
				.add(resReq, "/plot claim", "[rect/circle] [radius]", "")
				.add(resReq, "/plot perm", "[hud]", "")
				.addCmd("/plot notforsale", "", Translation.of("msg_plot_nfs"))
				.addCmd("/plot notforsale", "[rect/circle] [radius]", "")
				.addCmd("/plot forsale [$]", "", Translation.of("msg_plot_fs"))
				.addCmd("/plot forsale [$]", "within [rect/circle] [radius]", "")
				.addCmd("/plot evict", "", "")
				.addCmd("/plot clear", "", "")
				.addCmd("/plot set ...", "", Translation.of("msg_plot_fs"))
				.add(resReq, "/plot toggle", "[pvp/fire/explosion/mobs]", "")
				.add(resReq, "/plot group", "?", "")
				.add(Translation.of("msg_nfs_abr"));
		}
	},
	
	PLOT_GROUP_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group")
				.add("add | new | create", "[name]", "Ex: /plot group new ExpensivePlots")
				.add("remove", "", "Removes a plot from the specified group.")
				.add("rename", "[newName]", "Renames the group you are standing in.")
				.add("set", "...", "Ex: /plot group set perm resident on.")
				.add("toggle", "...", "Ex: /plot group toggle [pvp|fire|mobs]")
				.add("forsale | fs", "[price]", "Ex: /plot group forsale 50")
				.add("notforsale | nfs", "", "Ex: /plot group notforsale");
		}
	},
	
	PLOT_GROUP_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group toggle")
				.add("pvp", "")
				.add("explosion", "")
				.add("fire", "")
				.add("mobs", "");
		}
	},
	
	PLOT_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot set")
				.add("[plottype]", "Ex: Inn, Wilds, Farm, Embassy etc")
				.add("outpost")
				.add("reset", "Removes a plot type")
				.add("[name]", "Names a plot")
				.add("Level", "[resident/ally/outsider]", "", "")
				.add("Type", "[build/destroy/switch/itemuse]", "", "")
				.add("perm [on/off]", "Toggle all permissions")
				.add("perm [level/type] [on/off]", "")
				.add("perm [level] [type] [on/off]", "")
				.add("perm reset", "")
				.add("Eg", "/plot set perm", "ally off", "")
				.add("Eg", "/plot set perm", "friend build on", "")
				.add(Translation.of("plot_perms", "'friend'", "'resident'"))
				.add(Translation.of("plot_perms_1"));
		}
	},

	PLOT_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot toggle")
				.add("pvp", "")
				.add("explosion", "")
				.add("fire", "")
				.add("mobs", "");
		}
	},
	
	NATION_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation", Translation.of("nation_help_1"))
				.add(Translation.of("nation_help_2"), Translation.of("nation_help_3"))
				.add("list", Translation.of("nation_help_4"))
				.add("townlist (nation)", "")
				.add("allylist (nation)", "")
				.add("enemylist (nation)", "")
				.add("online", Translation.of("nation_help_9"))
				.add("spawn", Translation.of("nation_help_10"))
				.add("join (nation)", "Used to join open nations.")
				.add(Translation.of("res_sing"), "deposit [$]", "")
				.add(Translation.of("mayor_sing"), "leave", Translation.of("nation_help_5"))
				.add(Translation.of("king_sing"), "king ?", Translation.of("nation_help_7"))
				.add(Translation.of("admin_sing"), "new " + Translation.of("nation_help_2") + " [capital]", Translation.of("nation_help_8"))
				.add(Translation.of("admin_sing"), "delete " + Translation.of("nation_help_2"), "")
				.add(Translation.of("admin_sing"), "say", "[message]");
		}
	},
	
	NATION_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation", Translation.of("nation_help_1"))
				.add(Translation.of("nation_help_2"), Translation.of("nation_help_3"))
				.add("list", Translation.of("nation_help_4"));
		}
	},
	
	NATION_LIST {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation list")
				.add("{page #}", "")
				.add("{page #} by residents", "")
				.add("{page #} by towns", "")
				.add("{page #} by open", "")
				.add("{page #} by balance", "")
				.add("{page #} by name", "")
				.add("{page #} by townblocks", "")
				.add("{page #} by online", "");
		}
	},
	
	NATION_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation set")
				.add("king " + Translation.of("res_2"), "")
				.add("capital [town]", "")
				.add("taxes [$]", "")
				.add("name [name]", "")
				.add("title/surname [resident] [text]", "")
				.add("tag [upto 4 letters] or clear", "")
				.add("board [message ... ]", "")
				.add("spawn", "")
				.add("spawncost [$]", "")
				.add("mapcolor [color]", "");
		}
	},

	KING_HELP {
		@Override
		protected MenuBuilder load() {
			MenuBuilder builder = new MenuBuilder("nation", false);
			builder.requirement = Translation.of("king_sing");
			return builder.addTitle(Translation.of("king_help_1"))
				.add("withdraw [$]", "")
				.add("[add/kick] [town] .. [town]", "")
				.add("rank [add/remove] " + Translation.of("res_2"), "[Rank]")
				.add("set [] .. []", "")
				.add("toggle [] .. []", "")
				.add("ally [] .. [] " + Translation.of("nation_help_2"), Translation.of("king_help_2"))
				.add("enemy [add/remove] " + Translation.of("nation_help_2"), Translation.of("king_help_3"))
				.add("delete", "")
				.add("merge {nation}", "")
				.add("say", "[message]");
		}
	},

	ALLIES_STRING {
		@Override
		protected MenuBuilder load() {
			MenuBuilder builder = new MenuBuilder("nation ally")
				.add("add [nation]", Translation.of("nation_ally_help_1"));

			if (TownySettings.isDisallowOneWayAlliance()) {
				builder.add("add -[nation]", Translation.of("nation_ally_help_7"));
			}
			builder.add("remove [nation]", Translation.of("nation_ally_help_2"));
			if (TownySettings.isDisallowOneWayAlliance()) {
				builder.add("sent", Translation.of("nation_ally_help_3"))
					.add("received", Translation.of("nation_ally_help_4"))
					.add("accept [nation]", Translation.of("nation_ally_help_5"))
					.add("deny [nation]", Translation.of("nation_ally_help_6"));
			}
			return builder;
		}
	},

	NATION_INVITE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation invite")
				.add("[town]", Translation.of("nation_invite_help_1"))
				.add("-[town]", Translation.of("nation_invite_help_2"))
				.add("sent", Translation.of("nation_invite_help_3"));
		}
	},

	INVITE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("invite", "")
				.add(TownySettings.getAcceptCommand() + " [town]", Translation.of("invite_help_1"))
				.add(TownySettings.getDenyCommand() + " [town]", Translation.of("invite_help_2"))
				.add("list", Translation.of("invite_help_3"));
		}
	},
	
	TOWN_MAYOR_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("Town Mayor Help", false)
				.add(Translation.of("mayor_sing"), "/town", "withdraw [$]", "")
				.add(Translation.of("mayor_sing"), "/town", "claim", "'/town claim ?' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "unclaim", "'/town " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "[add/kick] " + Translation.of("res_2") + " .. []", Translation.of("res_6"))
				.add(Translation.of("mayor_sing"), "/town", "set [] .. []", "'/town set' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "buy [] .. []", "'/town buy' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "plots", "")
				.add(Translation.of("mayor_sing"), "/town", "toggle", "")
				.add(Translation.of("mayor_sing"), "/town", "rank add/remove [resident] [rank]", "'/town rank ?' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "delete", "");
		}
	},
	
	NATION_TOGGLE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation toggle")
				.add("", "/nation toggle", "peaceful/neutral", "")
				.add("", "/nation toggle", "public", "")
				.add("", "/nation toggle", "open", "");
		}
	}, 
	
	PLOT_JAILCELL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot jailcell")
				.add("", "/plot jailcell", "add", "Adds a JailCell where you stand.")
				.add("", "/plot jailcell", "remove", "Removes a JailCell where you stand.");
		}
	},
	
	PLOT_PERM_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot perm")
				.add("", "/plot perm", "hud", "Opens the permissions hud.")
				.add("", "/plot perm", "remove [resident]", "Removes permission overrides for a player.")
				.add("", "/plot perm", "add [resident]", "Adds default permission overrides for a player.")
				.add("", "/plot perm", "gui", "Opens the permission editor gui.");
		}
	},
	
	PLOT_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot trust")
				.add("", "add [resident]")
				.add("", "remove [resident]");
		}
	},
	
	TOWN_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town trust")
				.add("", "add [resident]")
				.add("", "remove [resident]");
		}
	};


	HelpMenu(String... lines) {
		Collections.addAll(this.lines, lines);
	}

	public void loadMenu() {
		lines.clear();
		lines.addAll(load().lines);
	}

	private final List<String> lines = new ArrayList<>();

	protected MenuBuilder load(MenuBuilder builder) {
		return load();
	}

	protected abstract MenuBuilder load();

	public static void loadMenus() {
		for (HelpMenu menu : values()) {
			menu.loadMenu();
		}
	}
	
	public List<String> getLines() {
		return Collections.unmodifiableList(lines);
	}

	public void send(CommandSender sender) {
		TownyMessaging.sendMessage(sender, lines);
	}

	// Class to ease making menus
	private static class MenuBuilder {
		final List<String> lines = new ArrayList<>();
		private String command;
		String requirement = "";

		MenuBuilder(String cmd, boolean cmdTitle) {
			this.command = cmd;
			if (cmdTitle)
				this.lines.add(ChatTools.formatTitle("/" + command));
		}

		MenuBuilder(String cmd) {
			this(cmd, true);
		}

		MenuBuilder(String cmd, String desc) {
			this(cmd);
			if (!desc.isEmpty())
				add("", desc);
		}

		MenuBuilder(String cmd, String requirement, String desc) {
			this(cmd);
			this.requirement = requirement;
			if (!desc.isEmpty())
				add("", desc);
		}

		MenuBuilder() {
			this.command = "";
		}

		MenuBuilder add(String subCmd, String desc) {
			return add(this.requirement, subCmd, desc);
		}

		MenuBuilder add(String requirement, String subCmd, String desc) {
			this.lines.add(ChatTools.formatCommand(requirement, "/" + command, subCmd, desc));
			return this;
		}

		MenuBuilder add(String requirement, String command, String subCmd, String desc) {
			this.lines.add(ChatTools.formatCommand(requirement, command, subCmd, desc));
			return this;
		}

		MenuBuilder add(String line) {
			this.lines.add(line);
			return this;
		}

		MenuBuilder addTitle(String title) {
			this.lines.add(ChatTools.formatTitle(title));
			return this;
		}

		MenuBuilder addCmd(String cmd, String subCmd, String desc) {
			return add(requirement, cmd, subCmd, desc);
		}
	}
}
