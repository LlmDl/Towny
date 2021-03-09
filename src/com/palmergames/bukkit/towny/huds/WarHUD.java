package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.war.eventwar.War;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Hashtable;

public class WarHUD {

	final static int home_health = TownySettings.getWarzoneHomeBlockHealth();
	final static int town_health = TownySettings.getWarzoneTownBlockHealth();

	public static void updateLocation(Player p, WorldCoord at) {
		String nation_loc, town_loc, homeblock;
		try {
			town_loc = at.getTownBlock().getTown().getName();
			if (at.getTownBlock().isHomeBlock())
				homeblock = Translation.of("war_hud_homeblock");
			else
				homeblock = "";
		} catch (NotRegisteredException e) {town_loc = Translation.of("war_hud_wilderness"); homeblock = "";}
		try {
			nation_loc = at.getTownBlock().getTown().getNation().getName();
		} catch (NotRegisteredException e) {nation_loc = "";}
		p.getScoreboard().getTeam("nation").setSuffix(HUDManager.check(nation_loc));
		p.getScoreboard().getTeam("town").setSuffix(HUDManager.check(town_loc));
		p.getScoreboard().getTeam("home").setSuffix(HUDManager.check(homeblock));
	}

	public static void updateAttackable(Player p, WorldCoord at, War war) {
		if (!TownySettings.getOnlyAttackEdgesInWar())
			return;
		String onEdge;
		if (isOnEdgeOfTown(at, war))
			onEdge = Translation.of("war_hud_true");
		else
			onEdge = Translation.of("war_hud_false");
		p.getScoreboard().getTeam("edge").setSuffix(HUDManager.check(onEdge));
	}

	public static void updateHealth(Player p, WorldCoord at, War war) {
		String health;
		boolean isTown = false;
		try { 
			if (war.getWarZoneManager().isWarZone(at.getTownBlock().getWorldCoord())) {
				health = war.getWarZoneManager().getWarZone().get(at) + "" + ChatColor.AQUA + "/" + (at.getTownBlock().isHomeBlock() ? home_health : town_health);
			} else {
				isTown = true;
				if (at.getTownBlock().getTown().getNation().isNeutral())
					health = Translation.of("war_hud_peaceful");
				else
					health = Translation.of("war_hud_fallen");
			}
		} catch (NotRegisteredException e) {
			if (isTown)
				health = Translation.of("war_hud_peaceful");
			else
				health = "";
		}
		p.getScoreboard().getTeam("health").setSuffix(health);
	}

	public static void updateHealth (Player p, int health, boolean home) {
		if (health > 0) 
			p.getScoreboard().getTeam("health").setSuffix(health + "" + ChatColor.AQUA + "/" + (home ? home_health : town_health));
		else {
			p.getScoreboard().getTeam("health").setSuffix(Translation.of("war_hud_fallen"));
			if (TownySettings.getOnlyAttackEdgesInWar())
				p.getScoreboard().getTeam("edge").setSuffix("war_hud_false");
		}
	}

	public static void updateHomeTown(Player p) {
		Resident res = TownyUniverse.getInstance().getResident(p.getUniqueId());
		String homeTown;
		if (res == null || !res.hasTown())
			homeTown = Translation.of("war_hud_townless");
		else {
			try {
				homeTown = res.getTown().getName();
			} catch (NotRegisteredException ignore) {
				// Will never happen
				homeTown = "";
			}
		}
		
		p.getScoreboard().getTeam("town_title").setSuffix(HUDManager.check(homeTown));
	}

	public static void updateScore(Player p, War war) {
		String score = "";
		Resident res = TownyUniverse.getInstance().getResident(p.getUniqueId());
		Hashtable<Town, Integer> scores = war.getScoreManager().getTownScores();
		try {
			if (res != null && res.hasTown() && scores.containsKey(res.getTown())) {
				score = String.valueOf(scores.get(res.getTown()));
			}
		} catch (NotRegisteredException ignore) {
		}

		p.getScoreboard().getTeam("town_score").setSuffix(HUDManager.check(score));
	}

	public static void updateTopScores(Player p, String[] top) {
		String fprefix = top[0].contains("-") ? ChatColor.GOLD + top[0].split("-")[0] + ChatColor.WHITE + "-": "";
		String sprefix = top[1].contains("-") ? ChatColor.GRAY + top[1].split("-")[0] + ChatColor.WHITE + "-": "";
		String tprefix = top[2].contains("-") ? ChatColor.GRAY + top[2].split("-")[0] + ChatColor.WHITE + "-": "";
		String fsuffix = top[0].contains("-") ? top[0].split("-")[1] : "";
		String ssuffix = top[1].contains("-") ? top[1].split("-")[1] : "";
		String tsuffix = top[2].contains("-") ? top[2].split("-")[1] : "";
		p.getScoreboard().getTeam("first").setPrefix(HUDManager.check(fprefix));
		p.getScoreboard().getTeam("first").setSuffix(HUDManager.check(fsuffix));
		p.getScoreboard().getTeam("second").setPrefix(HUDManager.check(sprefix));
		p.getScoreboard().getTeam("second").setSuffix(HUDManager.check(ssuffix));
		p.getScoreboard().getTeam("third").setPrefix(HUDManager.check(tprefix));
		p.getScoreboard().getTeam("third").setSuffix(HUDManager.check(tsuffix));
	}

	public static void updateScore(Player p, int score) {
		p.getScoreboard().getTeam("town_score").setSuffix(HUDManager.check(score + ""));
	}

	public static void toggleOn (Player p, War war) {
		boolean edges = TownySettings.getOnlyAttackEdgesInWar();
		String WAR_HUD_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + Translation.of("war_hud_war");
		String space1_entry = ChatColor.DARK_PURPLE.toString();
		String town_title_entry = ChatColor.YELLOW + "" + ChatColor.UNDERLINE;
		String town_score_entry = ChatColor.WHITE + Translation.of("war_hud_score") + ChatColor.RED;
		String space2_entry = ChatColor.DARK_BLUE.toString();
		String location_title_entry = ChatColor.YELLOW + "" + ChatColor.UNDERLINE + Translation.of("war_hud_location");
		String nation_entry = ChatColor.WHITE + Translation.of("war_hud_nation") + ChatColor.GOLD;
		String town_entry = ChatColor.WHITE + Translation.of("war_hud_town") + ChatColor.DARK_AQUA;
		String edge_entry = ChatColor.WHITE + Translation.of("war_hud_attackable") + ChatColor.RED;
		String health_entry = ChatColor.WHITE + Translation.of("war_hud_health") + ChatColor.RED;
		String home_entry = ChatColor.RED + "";
		String space3_entry = ChatColor.DARK_GREEN.toString();
		String top_title_entry = ChatColor.YELLOW + "" + ChatColor.UNDERLINE + Translation.of("war_hud_top_towns");
		String first_entry = ChatColor.DARK_GREEN + "" + ChatColor.DARK_AQUA + "";
		String second_entry = ChatColor.BLACK + "" + ChatColor.DARK_AQUA + "";
		String third_entry = ChatColor.YELLOW + "" + ChatColor.DARK_AQUA + "";
		//init objective
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective obj = board.registerNewObjective("WAR_HUD_OBJ", "dummy", WAR_HUD_TITLE);
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		obj.setDisplayName(WAR_HUD_TITLE);
		//register teams
		Team space1 = board.registerNewTeam("space1");
		Team town_title = board.registerNewTeam("town_title");
		Team town_score = board.registerNewTeam("town_score");
		Team space2 = board.registerNewTeam("space2");
		Team location_title = board.registerNewTeam("location_title");
		Team nation = board.registerNewTeam("nation");
		Team town = board.registerNewTeam("town");
		Team health = board.registerNewTeam("health");
		Team home = board.registerNewTeam("home");
		Team space3 = board.registerNewTeam("space3");
		Team top_title = board.registerNewTeam("top_title");
		Team first = board.registerNewTeam("first");
		Team second = board.registerNewTeam("second");
		Team third = board.registerNewTeam("third");
		//register players
		space1.addEntry(space1_entry);
		town_title.addEntry(town_title_entry);
		town_score.addEntry(town_score_entry);
		space2.addEntry(space2_entry);
		location_title.addEntry(location_title_entry);
		nation.addEntry(nation_entry);
		town.addEntry(town_entry);
		health.addEntry(health_entry);
		home.addEntry(home_entry);
		space3.addEntry(space3_entry);
		top_title.addEntry(top_title_entry);
		first.addEntry(first_entry);
		second.addEntry(second_entry);
		third.addEntry(third_entry);
		//set scores for positioning
		obj.getScore(space1_entry).setScore(14);
		obj.getScore(town_title_entry).setScore(13);
		obj.getScore(town_score_entry).setScore(12);
		obj.getScore(space2_entry).setScore(11);
		obj.getScore(location_title_entry).setScore(10);
		obj.getScore(nation_entry).setScore(9);
		obj.getScore(town_entry).setScore(8);
		obj.getScore(health_entry).setScore(edges ? 6 : 7);
		obj.getScore(home_entry).setScore(edges ? 5 : 6);
		obj.getScore(space3_entry).setScore(edges ? 4 : 5);
		obj.getScore(top_title_entry).setScore(edges ? 3 : 4);
		obj.getScore(first_entry).setScore(edges ? 2 : 3);
		obj.getScore(second_entry).setScore(edges ? 1 : 2);
		obj.getScore(third_entry).setScore(edges ? 0 : 1);

		if (edges) {
			Team edge = board.registerNewTeam("edge");
			edge.addEntry(edge_entry);
			obj.getScore(edge_entry).setScore(7);
		}
		//set the board
		p.setScoreboard(board);
		WorldCoord at = new WorldCoord(p.getWorld().getName(), Coord.parseCoord(p));
		updateLocation(p, at);
		updateAttackable(p, at, war);
		updateHealth(p, at, war);
		updateHomeTown(p);
		updateScore(p, war);
		updateTopScores(p, war.getScoreManager().getTopThree());
	}

	public static boolean isOnEdgeOfTown(WorldCoord worldCoord, War war) {

		Town currentTown;
		
		//Checks to make sure the worldCoord is actually in war
		try {
			currentTown = worldCoord.getTownBlock().getTown();
			if (!war.getWarZoneManager().isWarZone(worldCoord))
				return false;
		} catch (NotRegisteredException e) {
			return false;
		}
		
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				boolean sameTown = edgeTownBlock.getTown() == currentTown;
				if (!sameTown || (sameTown && !war.getWarZoneManager().isWarZone(edgeTownBlock.getWorldCoord()))) {
					return true;
				}
			} catch (NotRegisteredException e) {
				return true;
			}
		return false;
	}
}
