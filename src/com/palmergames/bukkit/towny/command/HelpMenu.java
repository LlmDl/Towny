package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.ChatTools;
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
				.add("set [] .. []", "'/townyadmin set' " + Translation.of("res_5"))
				.add("unclaim [radius]", "")
				.add("town/nation", "")
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
				.add("delete [] .. []", "delete a residents data files.")
				.add("war", "");
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
	
	TOWN_TOGGLE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town toggle")
				.add("", "/town toggle", "pvp", "")
				.add("", "/town toggle", "public", "")
				.add("", "/town toggle", "explosion", "")
				.add("", "/town toggle", "fire", "")
				.add("", "/town toggle", "mobs", "")
				.add("", "/town toggle", "taxpercent", "")
				.add("", "/town toggle", "open", "")
				.add("", "/town toggle", "jail [number] [resident]", "");
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
