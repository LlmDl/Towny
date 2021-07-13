package com.palmergames.bukkit.towny.permissions;

/**
 * @author ElgarL
 * 
 */
public enum PermissionNodes {
	TOWNY_ADMIN("towny.admin"),
	TOWNY_ADMIN_NATION_ZONE("towny.admin.nation_zone"),
	TOWNY_ADMIN_OUTLAW_TELEPORT_BYPASS("towny.admin.outlaw.teleport_bypass"),
	TOWNY_ADMIN_TOWN_COMMAND_BLACKLIST_BYPASS("towny.admin.town_commands.blacklist_bypass"),
	TOWNY_ADMIN_TOURIST_COMMAND_LIMITATION_BYPASS("towny.globally_welcome"),
	
	/*
	 * Nation command permissions
	 */
	TOWNY_COMMAND_NATION_LIST("towny.command.nation.list.*"),
	    TOWNY_COMMAND_NATION_LIST_RESIDENTS("towny.command.nation.list.residents"),
	    TOWNY_COMMAND_NATION_LIST_TOWNS("towny.command.nation.list.towns"),
	    TOWNY_COMMAND_NATION_LIST_OPEN("towny.command.nation.list.open"),
	    TOWNY_COMMAND_NATION_LIST_PUBLIC("towny.command.nation.list.public"),
	    TOWNY_COMMAND_NATION_LIST_BALANCE("towny.command.nation.list.balance"),
	    TOWNY_COMMAND_NATION_LIST_NAME("towny.command.nation.list.name"),
	    TOWNY_COMMAND_NATION_LIST_TOWNBLOCKS("towny.command.nation.list.townblocks"),
	    TOWNY_COMMAND_NATION_LIST_ONLINE("towny.command.nation.list.online"),
	TOWNY_COMMAND_NATION_TOWNLIST("towny.command.nation.townlist"),
	TOWNY_COMMAND_NATION_ALLYLIST("towny.command.nation.allylist"),
	TOWNY_COMMAND_NATION_ENEMYLIST("towny.command.nation.enemylist"),
	TOWNY_COMMAND_NATION_NEW("towny.command.nation.new"),
	TOWNY_COMMAND_NATION_LEAVE("towny.command.nation.leave"),
	TOWNY_COMMAND_NATION_MERGE("towny.command.nation.merge"),
	TOWNY_COMMAND_NATION_WITHDRAW("towny.command.nation.withdraw"),
	TOWNY_COMMAND_NATION_DEPOSIT("towny.command.nation.deposit"),
	TOWNY_COMMAND_NATION_DEPOSIT_OTHER("towny.command.nation.deposit.other"),
	TOWNY_COMMAND_NATION_OTHERNATION("towny.command.nation.othernation"),
	TOWNY_COMMAND_NATION_JOIN("towny.command.nation.join"),
	
	// Covers all assignable ranks
	TOWNY_COMMAND_NATION_RANK("towny.command.nation.rank.*"),
		TOWNY_COMMAND_NATION_RANK_ASSISTANT("towny.command.nation.rank.assistant"),
	
	TOWNY_COMMAND_NATION_KING("towny.command.nation.king"),
	
	//TOWNY_COMMAND_NATION_ASSISTANT("towny.command.nation.assistant"),
	
	TOWNY_COMMAND_NATION_SET("towny.command.nation.set.*"),
		TOWNY_COMMAND_NATION_SET_KING("towny.command.nation.set.king"),
		TOWNY_COMMAND_NATION_SET_CAPITOL("towny.command.nation.set.capitol"),
		TOWNY_COMMAND_NATION_SET_TAXES("towny.command.nation.set.taxes"),
		TOWNY_COMMAND_NATION_SET_NAME("towny.command.nation.set.name"),
		TOWNY_COMMAND_NATION_SET_TITLE("towny.command.nation.set.title"),
		TOWNY_COMMAND_NATION_SET_SURNAME("towny.command.nation.set.surname"),
		TOWNY_COMMAND_NATION_SET_TAG("towny.command.nation.set.tag"),
		TOWNY_COMMAND_NATION_SET_BOARD("towny.command.nation.set.board"),
		TOWNY_COMMAND_NATION_SET_SPAWN("towny.command.nation.set.spawn"),
		TOWNY_COMMAND_NATION_SET_SPAWNCOST("towny.command.nation.set.spawncost"),
		TOWNY_COMMAND_NATION_SET_MAPCOLOR("towny.command.nation.set.mapcolor"),

	TOWNY_COMMAND_NATION_TOGGLE("towny.command.nation.toggle.*"),
    TOWNY_COMMAND_NATION_TOGGLE_NEUTRAL("towny.command.nation.toggle.neutral"),
    TOWNY_COMMAND_NATION_TOGGLE_PUBLIC("towny.command.nation.toggle.public"),
    TOWNY_COMMAND_NATION_TOGGLE_OPEN("towny.command.nation.toggle.open"),

	TOWNY_COMMAND_NATION_ENEMY("towny.command.nation.enemy"),
	TOWNY_COMMAND_NATION_DELETE("towny.command.nation.delete"),
	TOWNY_COMMAND_NATION_ONLINE("towny.command.nation.online"),
	TOWNY_COMMAND_NATION_SAY("towny.command.nation.say"),
	TOWNY_COMMAND_NATION_BANKHISTORY("towny.command.nation.bankhistory"),
	TOWNY_COMMAND_NATION_KICK("towny.command.nation.kick"),
	// Invite System (Piece of hard work)
	TOWNY_COMMAND_NATION_INVITE_SEE_HOME("towny.command.nation.invite"),
	TOWNY_COMMAND_NATION_INVITE_ADD("towny.command.nation.invite.add"),
	TOWNY_COMMAND_NATION_INVITE_LIST_SENT("towny.command.nation.invite.sent"),

	TOWNY_COMMAND_NATION_ALLY_SEE_HOME("towny.command.nation.ally"),
	TOWNY_COMMAND_NATION_ALLY_ACCEPT("towny.command.nation.ally.accept"),
	TOWNY_COMMAND_NATION_ALLY_DENY("towny.command.nation.ally.deny"),
	TOWNY_COMMAND_NATION_ALLY_ADD("towny.command.nation.ally.add"),
	TOWNY_COMMAND_NATION_ALLY_REMOVE("towny.command.nation.ally.remove"),
	TOWNY_COMMAND_NATION_ALLY_LIST_SENT("towny.command.nation.ally.sent"),
	TOWNY_COMMAND_NATION_ALLY_LIST_RECEIVED("towny.command.nation.ally.received"),

	TOWNY_COMMAND_TOWN_INVITE_SEE_HOME("towny.command.town.invite"),
	TOWNY_COMMAND_TOWN_INVITE_ADD("towny.command.town.invite.add"),
	TOWNY_COMMAND_TOWN_INVITE_LIST_SENT("towny.command.town.invite.sent"),

	TOWNY_COMMAND_TOWN_INVITE_LIST_RECEIVED("towny.command.town.invite.received"),
	TOWNY_COMMAND_TOWN_INVITE_ACCEPT("towny.command.town.invite.accept"),
	TOWNY_COMMAND_TOWN_INVITE_DENY("towny.command.town.invite.deny"),


	
	/*
	 * Town command permissions
	 */
	TOWNY_COMMAND_TOWN("towny.command.town.*"),
		TOWNY_COMMAND_TOWN_OTHERTOWN("towny.command.town.othertown"),
		TOWNY_COMMAND_TOWN_HERE("towny.command.town.here"),
		TOWNY_COMMAND_TOWN_LIST("towny.command.town.list.*"),
		    TOWNY_COMMAND_TOWN_LIST_RESIDENTS("towny.command.town.list.residents"),
		    TOWNY_COMMAND_TOWN_LIST_OPEN("towny.command.town.list.open"),
		    TOWNY_COMMAND_TOWN_LIST_PUBLIC("towny.command.town.list.public"),
		    TOWNY_COMMAND_TOWN_LIST_BALANCE("towny.command.town.list.balance"),
		    TOWNY_COMMAND_TOWN_LIST_NAME("towny.command.town.list.name"),
		    TOWNY_COMMAND_TOWN_LIST_TOWNBLOCKS("towny.command.town.list.townblocks"),
		    TOWNY_COMMAND_TOWN_LIST_ONLINE("towny.command.town.list.online"),
		    TOWNY_COMMAND_TOWN_LIST_RUINED("towny.command.town.list.ruined"),
		    TOWNY_COMMAND_TOWN_LIST_BANKRUPT("towny.command.town.list.bankrupt"),
		TOWNY_COMMAND_TOWN_OUTPOST_LIST("towny.command.town.outpost.list"),
		TOWNY_COMMAND_TOWN_NEW("towny.command.town.new"),
		TOWNY_COMMAND_TOWN_LEAVE("towny.command.town.leave"),
		TOWNY_COMMAND_TOWN_RECLAIM("towny.command.town.reclaim"),
		TOWNY_COMMAND_TOWN_WITHDRAW("towny.command.town.withdraw"),
		TOWNY_COMMAND_TOWN_DEPOSIT("towny.command.town.deposit"),
		TOWNY_COMMAND_TOWN_PLOTS("towny.command.town.plots"),
		TOWNY_COMMAND_TOWN_PURGE("towny.command.town.purge"),
		TOWNY_COMMAND_TOWN_MERGE("towny.command.town.merge"),
		TOWNY_COMMAND_TOWN_BUY("towny.command.town.buy"),
		TOWNY_COMMAND_TOWN_JAIL("towny.command.town.jail"),
		TOWNY_COMMAND_TOWN_JAIL_LIST("towny.command.town.jail.list"),
		TOWNY_COMMAND_TOWN_UNJAIL("towny.command.town.unjail"),
		TOWNY_COMMAND_TOWN_PLOTGROUPLIST("towny.command.town.plotgrouplist"),
		
		// Covers all assignable ranks
		TOWNY_COMMAND_TOWN_RANK("towny.command.town.rank.*"),
		TOWNY_COMMAND_TOWN_RANKLIST("towny.command.town.ranklist"),
		
		TOWNY_COMMAND_TOWN_SET("towny.command.town.set.*"),
			TOWNY_COMMAND_TOWN_SET_BOARD("towny.command.town.set.board"),
			TOWNY_COMMAND_TOWN_SET_MAYOR("towny.command.town.set.mayor"),
			TOWNY_COMMAND_TOWN_SET_HOMEBLOCK("towny.command.town.set.homeblock"),
			TOWNY_COMMAND_TOWN_SET_SPAWN("towny.command.town.set.spawn"),
			TOWNY_COMMAND_TOWN_SET_OUTPOST("towny.command.town.set.outpost"),
			TOWNY_COMMAND_TOWN_SET_PRIMARYJAIL("towny.command.town.set.primaryjail"),
			TOWNY_COMMAND_TOWN_SET_PERM("towny.command.town.set.perm"),
			TOWNY_COMMAND_TOWN_SET_TAXES("towny.command.town.set.taxes"),
			TOWNY_COMMAND_TOWN_SET_PLOTTAX("towny.command.town.set.plottax"),
			TOWNY_COMMAND_TOWN_SET_SHOPTAX("towny.command.town.set.shoptax"),
			TOWNY_COMMAND_TOWN_SET_EMBASSYTAX("towny.command.town.set.embassytax"),
			TOWNY_COMMAND_TOWN_SET_SPAWNCOST("towny.command.town.set.spawncost"),
			TOWNY_COMMAND_TOWN_SET_PLOTPRICE("towny.command.town.set.plotprice"),
			TOWNY_COMMAND_TOWN_SET_NAME("towny.command.town.set.name"),
			TOWNY_COMMAND_TOWN_SET_TAG("towny.command.town.set.tag"),
			TOWNY_COMMAND_TOWN_SET_JAIL("towny.command.town.set.jail"),
			TOWNY_COMMAND_TOWN_SET_TITLE("towny.command.town.set.title"),
			TOWNY_COMMAND_TOWN_SET_SURNAME("towny.command.town.set.surname"),
			TOWNY_COMMAND_TOWN_SET_TAXPERCENTCAP("towny.command.town.set.taxpercentcap"),
		
		TOWNY_COMMAND_TOWN_TOGGLE("towny.command.town.toggle.*"),
			TOWNY_COMMAND_TOWN_TOGGLE_PVP("towny.command.town.toggle.pvp"),
			TOWNY_COMMAND_TOWN_TOGGLE_PUBLIC("towny.command.town.toggle.public"),
			TOWNY_COMMAND_TOWN_TOGGLE_EXPLOSION("towny.command.town.toggle.explosion"),
			TOWNY_COMMAND_TOWN_TOGGLE_FIRE("towny.command.town.toggle.fire"),
			TOWNY_COMMAND_TOWN_TOGGLE_MOBS("towny.command.town.toggle.mobs"),
			TOWNY_COMMAND_TOWN_TOGGLE_TAXPERCENT("towny.command.town.toggle.taxpercent"),
			TOWNY_COMMAND_TOWN_TOGGLE_OPEN("towny.command.town.toggle.open"),
			TOWNY_COMMAND_TOWN_TOGGLE_NEUTRAL("towny.command.town.toggle.neutral"),
		
		
		TOWNY_COMMAND_TOWN_MAYOR("towny.command.town.mayor"),
		TOWNY_COMMAND_TOWN_DELETE("towny.command.town.delete"),
		TOWNY_COMMAND_TOWN_JOIN("towny.command.town.join"),
		TOWNY_COMMAND_TOWN_KICK("towny.command.town.kick"),
		
		TOWNY_COMMAND_TOWN_CLAIM("towny.command.town.claim.*"),
			TOWNY_COMMAND_TOWN_CLAIM_TOWN("towny.command.town.claim.town"),
				TOWNY_COMMAND_TOWN_CLAIM_TOWN_MULTIPLE("towny.command.town.claim.town.multiple"),
			TOWNY_COMMAND_TOWN_CLAIM_OUTPOST("towny.command.town.claim.outpost"),
		
		TOWNY_COMMAND_TOWN_UNCLAIM("towny.command.town.unclaim"),
		TOWNY_COMMAND_TOWN_UNCLAIM_ALL("towny.command.town.unclaim.all"),
		TOWNY_COMMAND_TOWN_ONLINE("towny.command.town.online"),
		TOWNY_COMMAND_TOWN_SAY("towny.command.town.say"),
		TOWNY_COMMAND_TOWN_OUTLAW("towny.command.town.outlaw"),
		TOWNY_COMMAND_TOWN_BANKHISTORY("towny.command.town.bankhistory"),
		TOWNY_COMMAND_TOWN_RESLIST("towny.command.town.reslist"),
		TOWNY_COMMAND_TOWN_OUTLAWLIST("towny.command.town.outlawlist"),
	
	/*
	 * Plot command permissions
	 */
	TOWNY_COMMAND_PLOT("towny.command.plot.*"),
		TOWNY_COMMAND_PLOT_ASMAYOR("towny.command.plot.asmayor"),
		TOWNY_COMMAND_PLOT_CLAIM("towny.command.plot.claim"),
		TOWNY_COMMAND_PLOT_EVICT("towny.command.plot.evict"),
		TOWNY_COMMAND_PLOT_UNCLAIM("towny.command.plot.unclaim"),
		TOWNY_COMMAND_PLOT_NOTFORSALE("towny.command.plot.notforsale"),
		TOWNY_COMMAND_PLOT_FORSALE("towny.command.plot.forsale"),
		TOWNY_COMMAND_PLOT_JAILCELL("towny.command.plot.jailcell"),
		TOWNY_COMMAND_PLOT_PERM("towny.command.plot.perm"),
		TOWNY_COMMAND_PLOT_PERM_HUD("towny.command.plot.perm.hud"),
		
		TOWNY_COMMAND_PLOT_TOGGLE("towny.command.plot.toggle.*"),
			TOWNY_COMMAND_PLOT_TOGGLE_PVP("towny.command.plot.toggle.pvp"),
			TOWNY_COMMAND_PLOT_TOGGLE_EXPLOSION("towny.command.plot.toggle.explosion"),
			TOWNY_COMMAND_PLOT_TOGGLE_FIRE("towny.command.plot.toggle.fire"),
			TOWNY_COMMAND_PLOT_TOGGLE_MOBS("towny.command.plot.toggle.mobs"),
		
		TOWNY_COMMAND_PLOT_SET("towny.command.plot.set.*"),
			TOWNY_COMMAND_PLOT_SET_NAME("towny.command.plot.set.name"),
			TOWNY_COMMAND_PLOT_SET_PERM("towny.command.plot.set.perm"),
			TOWNY_COMMAND_PLOT_SET_RESET("towny.command.plot.set.reset"),
			TOWNY_COMMAND_PLOT_SET_SHOP("towny.command.plot.set.shop"),
			TOWNY_COMMAND_PLOT_SET_EMBASSY("towny.command.plot.set.embassy"),
			TOWNY_COMMAND_PLOT_SET_ARENA("towny.command.plot.set.arena"),
			TOWNY_COMMAND_PLOT_SET_WILDS("towny.command.plot.set.wilds"),
			TOWNY_COMMAND_PLOT_SET_SPLEEF("towny.command.plot.set.spleef"),
			TOWNY_COMMAND_PLOT_SET_INN("towny.command.plot.set.inn"),
			TOWNY_COMMAND_PLOT_SET_JAIL("towny.command.plot.set.jail"),
		
		TOWNY_COMMAND_PLOT_GROUP("towny.command.plot.group.*"),
		    TOWNY_COMMAND_PLOT_GROUP_ADD("towny.command.plot.group.add"),
		    TOWNY_COMMAND_PLOT_GROUP_REMOVE("towny.command.plot.group.remove"),
		    TOWNY_COMMAND_PLOT_GROUP_DELETE("towny.command.plot.group.delete"),
		    TOWNY_COMMAND_PLOT_GROUP_RENAME("towny.command.plot.group.rename"),
		    TOWNY_COMMAND_PLOT_GROUP_SET("towny.command.plot.group.set"),
		    TOWNY_COMMAND_PLOT_GROUP_TOGGLE("towny.command.plot.group.toggle"),
		    TOWNY_COMMAND_PLOT_GROUP_FORSALE("towny.command.plot.group.forsale"),
		    TOWNY_COMMAND_PLOT_GROUP_NOTFORSALE("towny.command.plot.group.notforsale"),
		
		TOWNY_COMMAND_PLOT_CLEAR("towny.command.plot.clear"),
	
	/*
	 * Resident command permissions
	 */
	TOWNY_COMMAND_RESIDENT_OTHERRESIDENT("towny.command.resident.otherresident"),
	TOWNY_COMMAND_RESIDENT_LIST("towny.command.resident.list"),
	TOWNY_COMMAND_RESIDENT_TAX("towny.command.resident.tax"),
	TOWNY_COMMAND_RESIDENT_JAIL("towny.command.resident.jail"),
	TOWNY_COMMAND_RESIDENT_SET("towny.command.resident.set.*"),
		TOWNY_COMMAND_RESIDENT_SET_PERM("towny.command.resident.set.perm"),
		TOWNY_COMMAND_RESIDENT_SET_MODE("towny.command.resident.set.mode"),
	
	TOWNY_COMMAND_RESIDENT_TOGGLE("towny.command.resident.toggle.*"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_PVP("towny.command.resident.toggle.pvp"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_EXPLOSION("towny.command.resident.toggle.explosion"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_FIRE("towny.command.resident.toggle.fire"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_MOBS("towny.command.resident.toggle.mobs"),
	
	TOWNY_COMMAND_RESIDENT_FRIEND("towny.command.resident.friend"),
	TOWNY_COMMAND_RESIDENT_SPAWN("towny.command.resident.spawn"),	
	
	/*
	 * TownyAdmin command permissions
	 */
	TOWNY_COMMAND_TOWNYADMIN("towny.command.townyadmin.*"),
	TOWNY_COMMAND_TOWNYADMIN_SCREEN("towny.command.townyadmin"),
	TOWNY_COMMAND_TOWNYADMIN_SET("towny.command.townyadmin.set.*"),
		TOWNY_COMMAND_TOWNYADMIN_SET_MAYOR("towny.command.townyadmin.set.mayor"),
		TOWNY_COMMAND_TOWNYADMIN_SET_PLOT("towny.command.townyadmin.set.plot"),
		TOWNY_COMMAND_TOWNYADMIN_SET_CAPITAL("towny.command.townyadmin.set.capital"),
		TOWNY_COMMAND_TOWNYADMIN_SET_TITLE("towny.command.townyadmin.set.title"),
		TOWNY_COMMAND_TOWNYADMIN_SET_SURNAME("towny.command.townyadmin.set.surname"),
		
    TOWNY_COMMAND_TOWNYADMIN_PLOT("towny.command.townyadmin.plot.*"),
    	TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIM("towny.command.townyadmin.plot.claim"),
		TOWNY_COMMAND_TOWNYADMIN_PLOT_META("towny.command.townyadmin.plot.meta"),
		TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIMEDAT("towny.command.townyadmin.plot.claimedat"),
	
	TOWNY_COMMAND_TOWNYADMIN_RESIDENT("towny.command.townyadmin.resident.*"),
		TOWNY_COMMMAND_TOWNYADMIN_RESIDENT_RENAME("towny.command.townyadmin.resident.rename"),
		TOWNY_COMMMAND_TOWNYADMIN_RESIDENT_FRIEND("towny.command.townyadmin.resident.friend"),
		TOWNY_COMMMAND_TOWNYADMIN_RESIDENT_UNJAIL("towny.command.townyadmin.resident.unjail"),
		
	TOWNY_COMMAND_TOWNYADMIN_TOWN("towny.command.townyadmin.town.*"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_NEW("towny.command.townyadmin.town.new"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_ADD("towny.command.townyadmin.town.add"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_KICK("towny.command.townyadmin.town.kick"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE("towny.command.townyadmin.town.delete"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_RENAME("towny.command.townyadmin.town.rename"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_TOGGLE("towny.command.townyadmin.town.toggle"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_SET("towny.command.townyadmin.town.set"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE("towny.command.townyadmin.town.spawn.freecharge"),
	    TOWNY_COMMAND_TOWNYADMIN_TOWN_META("towny.command.townyadmin.town.meta"),
	    TOWNY_COMMAND_TOWNYADMIN_TOWN_BANKHISTORY("towny.command.townyadmin.town.bankhistory"),
	
	TOWNY_COMMAND_TOWNYADMIN_NATION("towny.command.townyadmin.nation.*"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_NEW("towny.command.townyadmin.nation.new"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_ADD("towny.command.townyadmin.nation.add"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE("towny.command.townyadmin.nation.delete"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_RENAME("towny.command.townyadmin.nation.rename"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_MERGE("towny.command.townyadmin.nation.merge"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_SET("towny.command.townyadmin.nation.set"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_TOGGLE("towny.command.townyadmin.nation.toggle"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_BANKHISTORY("towny.command.townyadmin.nation.bankhistory"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_ENEMY("towny.command.townyadmin.nation.enemy"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_ALLY("towny.command.townyadmin.nation.ally"),
	
	TOWNY_COMMAND_TOWNYADMIN_TOGGLE("towny.command.townyadmin.toggle.*"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_WAR("towny.command.townyadmin.toggle.war"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NEUTRAL("towny.command.townyadmin.toggle.neutral"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEVMODE("towny.command.townyadmin.toggle.devmode"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEBUG("towny.command.townyadmin.toggle.debug"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_TOWNWITHDRAW("towny.command.townyadmin.toggle.townwithdraw"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NATIONWITHDRAW("towny.command.townyadmin.toggle.nationwithdraw"),
	
	TOWNY_COMMAND_TOWNYADMIN_GIVEBONUS("towny.command.townyadmin.givebonus"),
	TOWNY_COMMAND_TOWNYADMIN_RELOAD("towny.command.townyadmin.reload"),
	TOWNY_COMMAND_TOWNYADMIN_MYSQLDUMP("towny.command.townyadmin.mysqldump"),
	TOWNY_COMMAND_TOWNYADMIN_DATABASE("towny.command.townyadmin.database"),
	TOWNY_COMMAND_TOWNYADMIN_RESET("towny.command.townyadmin.reset"),
	TOWNY_COMMAND_TOWNYADMIN_BACKUP("towny.command.townyadmin.backup"),
	TOWNY_COMMAND_TOWNYADMIN_NEWDAY("towny.command.townyadmin.newday"),
	TOWNY_COMMAND_TOWNYADMIN_PURGE("towny.command.townyadmin.purge"),
	TOWNY_COMMAND_TOWNYADMIN_CHECKPERM("towny.command.townyadmin.checkperm"),
	TOWNY_COMMAND_TOWNYADMIN_UNCLAIM("towny.command.townyadmin.unclaim"),
	TOWNY_COMMAND_TOWNYADMIN_RESIDNET_DELETE("towny.command.townyadmin.resident.delete"),
	TOWNY_COMMAND_TOWNYADMIN_DEPOSITALL("towny.command.townyadmin.depositall"),
	
	/*
	 * Towny command permissions
	 */
	TOWNY_COMMAND_TOWNY("towny.command.towny.*"),
	TOWNY_COMMAND_TOWNY_MAP("towny.command.towny.map"),
	TOWNY_COMMAND_TOWNY_TOP("towny.command.towny.top"),
	TOWNY_COMMAND_TOWNY_TREE("towny.command.towny.tree"),
	TOWNY_COMMAND_TOWNY_TIME("towny.command.towny.time"),
	TOWNY_COMMAND_TOWNY_UNIVERSE("towny.command.towny.universe"),
	TOWNY_COMMAND_TOWNY_VERSION("towny.command.towny.version"),
	TOWNY_COMMAND_TOWNY_WAR("towny.command.towny.war"),
	TOWNY_COMMAND_TOWNY_WAR_HUD("towny.command.towny.war.hud"),
	TOWNY_COMMAND_TOWNY_SPY("towny.command.towny.spy"),
	
	/*
	 * TownyWorld command permissions
	 */
	TOWNY_COMMAND_TOWNYWORLD_LIST("towny.command.townyworld.list"),
	TOWNY_COMMAND_TOWNYWORLD_SET("towny.command.townyworld.set"),
	
	TOWNY_COMMAND_TOWNYWORLD_TOGGLE("towny.command.townyworld.toggle.*"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_CLAIMABLE("towny.command.townyworld.toggle.claimable"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_USINGTOWNY("towny.command.townyworld.toggle.usingtowny"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_PVP("towny.command.townyworld.toggle.pvp"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEPVP("towny.command.townyworld.toggle.forcepvp"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_EXPLOSION("towny.command.townyworld.toggle.explosion"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEEXPLOSION("towny.command.townyworld.toggle.forceexplosion"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FIRE("towny.command.townyworld.toggle.fire"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEFIRE("towny.command.townyworld.toggle.forcefire"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_TOWNMOBS("towny.command.townyworld.toggle.townmobs"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WORLDMOBS("towny.command.townyworld.toggle.worldmobs"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTUNCLAIM("towny.command.townyworld.toggle.revertunclaim"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTEXPL("towny.command.townyworld.toggle.revertexpl"),
	
	TOWNY_COMMAND_TOWNYWORLD_REGEN("towny.command.townyworld.regen"),
	TOWNY_COMMAND_TOWNYWORLD_UNDO("towny.command.townyworld.undo"),
	

	TOWNY_TOWN_RESIDENT("towny.town.resident"),

	TOWNY_SPAWN_ADMIN("towny.admin.spawn"),
	TOWNY_SPAWN_ADMIN_NOWARMUP("towny.admin.spawn.nowarmup"),
	TOWNY_SPAWN_ADMIN_NOCOOLDOWN("towny.admin.spawn.nocooldown"),
	TOWNY_SPAWN_ADMIN_NOCHARGE("towny.admin.spawn.nocharge"),
	
	TOWNY_SPAWN_ALL("towny.town.spawn.*"),

	TOWNY_SPAWN_TOWN("towny.town.spawn.town"),
	TOWNY_SPAWN_OUTPOST("towny.town.spawn.outpost"),
	TOWNY_SPAWN_NATION("towny.town.spawn.nation"),
	TOWNY_SPAWN_ALLY("towny.town.spawn.ally"),
	TOWNY_SPAWN_PUBLIC("towny.town.spawn.public"),
	
	TOWNY_NATION_SPAWN_ALL("towny.nation.spawn.*"),
	
	TOWNY_NATION_SPAWN_NATION("towny.nation.spawn.nation"),
	TOWNY_NATION_SPAWN_ALLY("towny.nation.spawn.ally"),
	TOWNY_NATION_SPAWN_PUBLIC("towny.nation.spawn.public"),

	TOWNY_WILD_ALL("towny.wild.*"),

	TOWNY_WILD_BLOCK_BUILD("towny.wild.build.*"),
	TOWNY_WILD_BLOCK_DESTROY("towny.wild.destroy.*"),
	TOWNY_WILD_BLOCK_SWITCH("towny.wild.switch.*"),
	TOWNY_WILD_BLOCK_ITEM_USE("towny.wild.item_use.*"),

	TOWNY_CLAIMED_ALL("towny.claimed.*"),

	TOWNY_CLAIMED_ALLTOWN_BLOCK_BUILD("towny.claimed.alltown.build.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_DESTROY("towny.claimed.alltown.destroy.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_SWITCH("towny.claimed.alltown.switch.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_ITEM_USE("towny.claimed.alltown.item_use.*"),

	TOWNY_CLAIMED_OWNTOWN_BLOCK_BUILD("towny.claimed.owntown.build.*"),
	TOWNY_CLAIMED_OWNTOWN_BLOCK_DESTROY("towny.claimed.owntown.destroy.*"),
	TOWNY_CLAIMED_OWNTOWN_BLOCK_SWITCH("towny.claimed.owntown.switch.*"),
	TOWNY_CLAIMED_OWNTOWN_BLOCK_ITEM_USE("towny.claimed.owntown.item_use.*"),
	
	TOWNY_CLAIMED_TOWNOWNED_BLOCK_BUILD("towny.claimed.townowned.build.*"),
	TOWNY_CLAIMED_TOWNOWNED_BLOCK_DESTROY("towny.claimed.townowned.destroy.*"),
	TOWNY_CLAIMED_TOWNOWNED_BLOCK_SWITCH("towny.claimed.townowned.switch.*"),
	TOWNY_CLAIMED_TOWNOWNED_BLOCK_ITEM_USE("towny.claimed.townowned.item_use.*"),

	TOWNY_CHAT_ALL("towny.chat.*"),

	TOWNY_CHAT_TOWN("towny.chat.town"),
	TOWNY_CHAT_NATION("towny.chat.nation"),
	TOWNY_CHAT_ADMIN("towny.chat.admin"),
	TOWNY_CHAT_MOD("towny.chat.mod"),
	TOWNY_CHAT_GLOBAL("towny.chat.global"),
	TOWNY_CHAT_SPY("towny.chat.spy"),
	
	TOWNY_OUTLAW_JAILER("towny.outlaw.jailer"),
	TOWNY_BYPASS_DEATH_COSTS("towny.bypass_death_costs"),

	// Info nodes

	TOWNY_DEFAULT_MODES("towny_default_modes"),
	TOWNY_MAX_PLOTS("towny_maxplots"),
	TOWNY_EXTRA_PLOTS("towny_extraplots"),
	TOWNY_MAX_OUTPOSTS("towny_maxoutposts");
	

	private String value;

	/**
	 * Constructor
	 * 
	 * @param permission - Permission.
	 */
	PermissionNodes(String permission) {

		this.value = permission;
	}

	/**
	 * Retrieves the permission node
	 * 
	 * @return The permission node
	 */
	public String getNode() {

		return value;
	}

	/**
	 * Retrieves the permission node
	 * replacing the character *
	 * 
	 * @param replace - String
	 * @return The permission node
	 */
	public String getNode(String replace) {

		return value.replace("*", replace);
	}

	public String getNode(int replace) {

		return value.replace("*", replace + "");
	}
}
