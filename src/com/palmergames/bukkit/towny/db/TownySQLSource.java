/*
 * Towny MYSQL Source by StPinker
 *
 * Released under LGPL
 */
package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.tasks.GatherResidentUUIDTask;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class TownySQLSource extends TownyDatabaseHandler {

	private final Queue<SQL_Task> queryQueue = new ConcurrentLinkedQueue<>();
	private BukkitTask task = null;

	private final String dsn;
	private final String db_name;
	private final String username;
	private final String password;
	private final String tb_prefix;

	private Connection cntx = null;

	private final HikariConfig config;
	private final HikariDataSource hikariDataSource;

	public TownySQLSource(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		if (!FileMgmt.checkOrCreateFolders(rootFolderPath, dataFolderPath,
				dataFolderPath + File.separator + "plot-block-data")
				|| !FileMgmt.checkOrCreateFiles(dataFolderPath + File.separator + "regen.txt",
						dataFolderPath + File.separator + "snapshot_queue.txt")) {
			TownyMessaging.sendErrorMsg("Could not create flatfile default files and folders.");

		}
		/*
		 * Setup SQL connection
		 */
		db_name = TownySettings.getSQLDBName();
		tb_prefix = TownySettings.getSQLTablePrefix().toUpperCase();
		
		this.dsn = ("jdbc:mysql://" + TownySettings.getSQLHostName() + ":" + TownySettings.getSQLPort() + "/" + db_name + TownySettings.getSQLFlags());
		this.config = new HikariConfig();
		
		config.setPoolName("Towny MySQL");
		config.setJdbcUrl(this.dsn);

		username = TownySettings.getSQLUsername();
		password = TownySettings.getSQLPassword();

		config.setUsername(username);
		config.setPassword(password);

		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		config.addDataSourceProperty("useLocalSessionState", "true");
		config.addDataSourceProperty("rewriteBatchedStatements", "true");
		config.addDataSourceProperty("cacheResultSetMetadata", "true");
		config.addDataSourceProperty("cacheServerConfiguration", "true");
		config.addDataSourceProperty("elideSetAutoCommits", "true");
		config.addDataSourceProperty("maintainTimeStats", "false");
		config.addDataSourceProperty("cacheCallableStmts", "true");

		config.setMaximumPoolSize(TownySettings.getMaxPoolSize());
		config.setMaxLifetime(TownySettings.getMaxLifetime());
		config.setConnectionTimeout(TownySettings.getConnectionTimeout());

		this.hikariDataSource = new HikariDataSource(config);

		/*
		 * Register the driver (if possible)
		 */
		try {
			Driver driver = (Driver) Class.forName("com.mysql.jdbc.Driver").newInstance();
			DriverManager.registerDriver(driver);
		} catch (Exception e) {
			System.out.println("[Towny] Driver error: " + e);
		}

		/*
		 * Attempt to get a connection to the database
		 */
		if (getContext()) {

			TownyMessaging.sendDebugMsg("[Towny] Connected to Database");

		} else {

			TownyMessaging.sendErrorMsg("Failed when connecting to Database");
			return;

		}

		/*
		 * Initialise database Schema.
		 */
		SQL_Schema.initTables(cntx, db_name);

		/*
		 * Start our Async queue for pushing data to the database.
		 */
		task = BukkitTools.getScheduler().runTaskTimerAsynchronously(plugin, () -> {

			while (!TownySQLSource.this.queryQueue.isEmpty()) {

				SQL_Task query = TownySQLSource.this.queryQueue.poll();

				if (query.update) {
					TownySQLSource.this.QueueUpdateDB(query.tb_name, query.args, query.keys);
				} else {
					TownySQLSource.this.QueueDeleteDB(query.tb_name, query.args);
				}

			}

		}, 5L, 5L);
	}

	@Override
	public void finishTasks() {
		// Cancel the repeating task as its not needed anymore.
		task.cancel();

		// Make sure that *all* tasks are saved before shutting down.
		while (!queryQueue.isEmpty()) {
			SQL_Task query = TownySQLSource.this.queryQueue.poll();

			if (query.update) {
				TownySQLSource.this.QueueUpdateDB(query.tb_name, query.args, query.keys);
			} else {
				TownySQLSource.this.QueueDeleteDB(query.tb_name, query.args);
			}
		}
		// Close the database sources on shutdown to get GC
		hikariDataSource.close();
	}

	/**
	 * open a connection to the SQL server.
	 *
	 * @return true if we successfully connected to the db.
	 */
	public boolean getContext() {

		try {
			if (cntx == null || cntx.isClosed() || !cntx.isValid(1)) {

				if (cntx != null && !cntx.isClosed()) {

					try {

						cntx.close();

					} catch (SQLException e) {
						/*
						 * We're disposing of an old stale connection just be nice to the GC as well as
						 * mysql, so ignore the error as there's nothing we can do if it fails
						 */
					}
					cntx = null;
				}

				cntx = hikariDataSource.getConnection();

				return cntx != null && !cntx.isClosed();
			}

			return true;

		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error could not Connect to db " + this.dsn + ": " + e.getMessage());
		}

		return false;
	}

	/**
	 * Build the SQL string and execute to INSERT/UPDATE
	 *
	 * @param tb_name - Database Table name.
	 * @param args    - Arguments.
	 * @param keys    - Table keys.
	 * @return true if the update was successful.
	 */
	public boolean UpdateDB(String tb_name, HashMap<String, Object> args, List<String> keys) {

		/*
		 * Make sure we only execute queries in async
		 */

		this.queryQueue.add(new SQL_Task(tb_name, args, keys));

		return true;

	}

	public boolean QueueUpdateDB(String tb_name, HashMap<String, Object> args, List<String> keys) {

		/*
		 * Attempt to get a database connection.
		 */
		if (!getContext())
			return false;

		StringBuilder code;
		PreparedStatement stmt = null;
		List<Object> parameters = new ArrayList<>();
		int rs = 0;

		try {

			if (keys == null) {

				/*
				 * No keys so this is an INSERT not an UPDATE.
				 */

				// Push all values to a parameter list.

				parameters.addAll(args.values());

				String[] aKeys = args.keySet().toArray(new String[0]);

				// Build the prepared statement string appropriate for
				// the number of keys/values we are inserting.

				code = new StringBuilder("REPLACE INTO " + tb_prefix + (tb_name.toUpperCase()) + " ");
				StringBuilder keycode = new StringBuilder("(");
				StringBuilder valuecode = new StringBuilder(" VALUES (");

				for (int count = 0; count < args.size(); count++) {

					keycode.append("`").append(aKeys[count]).append("`");
					valuecode.append("?");

					if ((count < (args.size() - 1))) {
						keycode.append(", ");
						valuecode.append(",");
					} else {
						keycode.append(")");
						valuecode.append(")");
					}
				}

				code.append(keycode);
				code.append(valuecode);

			} else {

				/*
				 * We have keys so this is a conditional UPDATE.
				 */

				String[] aKeys = args.keySet().toArray(new String[0]);

				// Build the prepared statement string appropriate for
				// the number of keys/values we are inserting.

				code = new StringBuilder("UPDATE " + tb_prefix + (tb_name.toUpperCase()) + " SET ");

				for (int count = 0; count < args.size(); count++) {

					code.append("`").append(aKeys[count]).append("` = ?");

					// Push value for each entry.

					parameters.add(args.get(aKeys[count]));

					if ((count < (args.size() - 1))) {
						code.append(",");
					}
				}

				code.append(" WHERE ");

				for (int count = 0; count < keys.size(); count++) {

					code.append("`").append(keys.get(count)).append("` = ?");

					// Add extra values for the WHERE conditionals.

					parameters.add(args.get(keys.get(count)));

					if ((count < (keys.size() - 1))) {
						code.append(" AND ");
					}
				}

			}

			// Populate the prepared statement parameters.

			stmt = cntx.prepareStatement(code.toString());

			for (int count = 0; count < parameters.size(); count++) {

				Object element = parameters.get(count);

				if (element instanceof String) {

					stmt.setString(count + 1, (String) element);

				} else if (element instanceof Boolean) {

					stmt.setString(count + 1, ((Boolean) element) ? "1" : "0");

				} else {

					stmt.setObject(count + 1, element.toString());

				}

			}

			rs = stmt.executeUpdate();

		} catch (SQLException e) {

			TownyMessaging.sendErrorMsg("SQL: " + e.getMessage() + " --> " + stmt.toString());

		} finally {

			try {

				if (stmt != null) {
					stmt.close();
				}

				if (rs == 0) // if entry doesn't exist then try to insert
					return UpdateDB(tb_name, args, null);

			} catch (SQLException e) {
				TownyMessaging.sendErrorMsg("SQL closing: " + e.getMessage() + " --> " + stmt.toString());
			}

		}

		// Failed?
		return rs != 0;

		// Success!

	}

	/**
	 * Build the SQL string and execute to DELETE
	 *
	 * @param tb_name - Database Table name
	 * @param args    - Arguments
	 * @return true if the delete was a success.
	 */
	public boolean DeleteDB(String tb_name, HashMap<String, Object> args) {

		// Make sure we only execute queries in async

		this.queryQueue.add(new SQL_Task(tb_name, args));

		return true;

	}

	public boolean QueueDeleteDB(String tb_name, HashMap<String, Object> args) {

		if (!getContext())
			return false;
		try {
			StringBuilder wherecode = new StringBuilder(
					"DELETE FROM " + tb_prefix + (tb_name.toUpperCase()) + " WHERE ");
			Set<Map.Entry<String, Object>> set = args.entrySet();
			Iterator<Map.Entry<String, Object>> i = set.iterator();
			while (i.hasNext()) {
				Map.Entry<String, Object> me = i.next();
				wherecode.append("`").append(me.getKey()).append("` = ");
				if (me.getValue() instanceof String)
					wherecode.append("'").append(((String) me.getValue()).replace("'", "''")).append("'");
				else if (me.getValue() instanceof Boolean)
					wherecode.append("'").append(((Boolean) me.getValue()) ? "1" : "0").append("'");
				else
					wherecode.append("'").append(me.getValue()).append("'");

				wherecode.append(i.hasNext() ? " AND " : "");
			}
			int rs;
			try (Statement statement = cntx.createStatement()) {
				rs = statement.executeUpdate(wherecode.toString());
			}
			if (rs == 0) {
				TownyMessaging.sendDebugMsg("SQL: delete returned 0: " + wherecode);
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Error delete : " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean cleanup() {

		/*
		 * Attempt to get a database connection.
		 */
		if (!getContext())
			return false;

		SQL_Schema.cleanup(cntx, db_name);

		return true;
	}
	
	/*
	 * Load keys
	 */
	
	@Override
	public boolean loadTownBlockList() {

		TownyMessaging.sendDebugMsg("Loading TownBlock List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs;
				int total = 0;
				rs = s.executeQuery("SELECT world,x,z FROM " + tb_prefix + "TOWNBLOCKS");
				while (rs.next()) {

					TownyWorld world = getWorld(rs.getString("world"));
					int x = Integer.parseInt(rs.getString("x"));
					int z = Integer.parseInt(rs.getString("z"));

					TownBlock townBlock = new TownBlock(x, z, world);
					TownyUniverse.getInstance().addTownBlock(townBlock);
					total++;

				}
				TownyMessaging.sendDebugMsg("Loaded " + total + " townblocks.");

			}
			
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	@Override
	public boolean loadResidentList() {

		TownyMessaging.sendDebugMsg("Loading Resident List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "RESIDENTS");

				while (rs.next()) {
					try {
						newResident(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {
					}
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadTownList() {

		TownyMessaging.sendDebugMsg("Loading Town List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "TOWNS");

				while (rs.next()) {
					try {
						TownyUniverse.getInstance().newTownInternal(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {
					}
				}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: town list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: town list unknown error: ");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadNationList() {

		TownyMessaging.sendDebugMsg("Loading Nation List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "NATIONS");
				while (rs.next()) {
					try {
						newNation(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {
					}
				}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: nation list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: nation list unknown error : ");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadWorldList() {

		TownyMessaging.sendDebugMsg("Loading World List");

		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "WORLDS");
				while (rs.next()) {
					try {
						newWorld(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {
					}
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: world list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: world list unknown error : ");
			e.printStackTrace();
		}

		// Check for any new worlds registered with bukkit.
		if (plugin != null) {
			for (World world : plugin.getServer().getWorlds())
				try {
					newWorld(world.getName());
				} catch (AlreadyRegisteredException ignored) {
				}
		}
		return true;
	}
	
	public boolean loadPlotGroupList() {
		TownyMessaging.sendDebugMsg("Loading PlotGroup List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT groupID,town,groupName FROM " + tb_prefix + "PLOTGROUPS");

				while (rs.next()) {

					UUID id = UUID.fromString(rs.getString("groupID"));
					String groupName = rs.getString("groupName");
					Town town = universe.getTown(rs.getString("town"));

					if (town == null)
						continue;

					try {
						TownyUniverse.getInstance().newGroup(town, groupName, id);
					} catch (AlreadyRegisteredException ignored) {
					}

				}
			}
			
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadResidents() {

		TownyMessaging.sendDebugMsg("Loading Residents");

		TownySettings.setUUIDCount(0);

		if (!getContext())
			return false;
		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "RESIDENTS")) {

			while (rs.next()) {
				String residentName;
				try {
					residentName = rs.getString("name");
				} catch (SQLException ex) {
					System.out.println("[Towny] Loading Error: Error fetching a resident name from SQL Database. Skipping loading resident..");
					ex.printStackTrace();
					continue;
				}
				
				Resident resident = universe.getResident(residentName);
				
				if (resident == null) {
					System.out.println(String.format("[Towny] Loading Error: Could not fetch resident '%s' from Towny universe while loading from SQL DB.", residentName));
					continue;
				}

				if (!loadResident(resident, rs)) {
					System.out.println("[Towny] Loading Error: Could not read resident data '" + resident.getName() + "'.");
					return false;
				}
				
				if (resident.hasUUID())
					TownySettings.incrementUUIDCount();
				else
					GatherResidentUUIDTask.addResident(resident);
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident sql error : " + e.getMessage());
		}

		return true;
	}

	@Override
	public boolean loadResident(Resident resident) {

		TownyMessaging.sendDebugMsg("Loading resident " + resident.getName());
		if (!getContext())
			return false;

		try (PreparedStatement ps = cntx
				.prepareStatement("SELECT * FROM " + tb_prefix + "RESIDENTS" + " WHERE name=?")) {
			ps.setString(1, resident.getName());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return loadResident(resident, rs);
				}
			}

		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident sql error : " + e.getMessage());
		}
		return false;
	}

	private boolean loadResident(Resident resident, ResultSet rs) {
		try {
			String search;

			try {
				if (rs.getString("uuid") != null && !rs.getString("uuid").isEmpty()) {
					
					UUID uuid = UUID.fromString(rs.getString("uuid"));
					if (TownyUniverse.getInstance().hasResident(uuid)) {
						Resident olderRes = TownyUniverse.getInstance().getResident(uuid);
						if (resident.getLastOnline() > olderRes.getLastOnline()) {
							TownyMessaging.sendDebugMsg("Deleting : " + olderRes.getName() + " which is a dupe of " + resident.getName());
							try {
								TownyUniverse.getInstance().unregisterResident(olderRes);
							} catch (NotRegisteredException ignored) {}
							// Check if the older resident is a part of a town
							if (olderRes.hasTown()) {
								try {
									// Resident#removeTown saves the resident, so we can't use it.
									olderRes.getTown().removeResident(olderRes);
								} catch (NotRegisteredException nre) {}
							}
							deleteResident(olderRes);					
						} else {
							TownyMessaging.sendDebugMsg("Deleting resident : " + resident.getName() + " which is a dupe of " + olderRes.getName());
							try {
								TownyUniverse.getInstance().unregisterResident(resident);
							} catch (NotRegisteredException ignored) {}
							deleteResident(resident);
							return true;
						}
					}	
					resident.setUUID(uuid);
					universe.registerResidentUUID(resident);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				resident.setLastOnline(rs.getLong("lastOnline"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setRegistered(rs.getLong("registered"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setNPC(rs.getBoolean("isNPC"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setJailed(rs.getBoolean("isJailed"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setJailSpawn(rs.getInt("JailSpawn"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setJailDays(rs.getInt("JailDays"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setJailTown(rs.getString("JailTown"));
			} catch (Exception e) {
				e.printStackTrace();
			}

			String line;
			try {
				line = rs.getString("friends");
				if (line != null) {
					search = (line.contains("#")) ? "#" : ",";
					List<Resident> friends = getResidents(line.split(search));
					for (Resident friend : friends) {
						resident.addFriend(friend);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setPermissions(rs.getString("protectionStatus").replaceAll("#", ","));
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					resident.setMetadata(line);
				}
			} catch (SQLException ignored) {
			}

			line = rs.getString("town");
			if ((line != null) && (!line.isEmpty())) {
				Town town = universe.getTown(line);
				if (town == null) {
					TownyMessaging.sendErrorMsg("Loading Error: " + resident.getName() + " tried to load the town " + line + " which is invalid, removing town from the resident.");
					resident.setTown(null);
				}
				else {
					resident.setTown(town);

					try {
						resident.setTitle(rs.getString("title"));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						resident.setSurname(rs.getString("surname"));
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						line = rs.getString("town-ranks");
						if ((line != null) && (!line.isEmpty())) {
							search = (line.contains("#")) ? "#" : ",";
							resident.setTownRanks(Arrays.asList((line.split(search))));
						}
					} catch (Exception e) {
					}

					try {
						line = rs.getString("nation-ranks");
						if ((line != null) && (!line.isEmpty())) {
							search = (line.contains("#")) ? "#" : ",";
							resident.setNationRanks(Arrays.asList((line.split(search))));
						}
					} catch (Exception e) {
					}
				}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadTowns() {
		TownyMessaging.sendDebugMsg("Loading Towns");
		if (!getContext())
			return false;

		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNS ")) {
			while (rs.next()) {
				if (!loadTown(rs)) {
					System.out.println("[Towny] Loading Error: Could not read town data properly.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town sql Error - " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean loadTown(Town town) {
		TownyMessaging.sendDebugMsg("Loading town " + town.getName());
		if (!getContext())
			return false;

		try (PreparedStatement ps = cntx.prepareStatement("SELECT * FROM " + tb_prefix + "TOWNS " + " WHERE name=?")) {
			ps.setString(1, town.getName());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return loadTown(rs);
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town sql Error - " + e.getMessage());
		}

		return false;
	}

	private boolean loadTown(ResultSet rs) {
		String line;
		String[] tokens;
		String search;
		String name = null;
		try {
			Town town = universe.getTown(rs.getString("name"));
			
			if (town == null) {
				TownyMessaging.sendErrorMsg("SQL: Load Town " + name + ". Town was not registered properly on load!");
				return false;
			}
			
			name = town.getName();

			TownyMessaging.sendDebugMsg("Loading town " + name);

			try {
				Resident res = universe.getResident(rs.getString("mayor"));
				
				if (res == null)
					throw new TownyException();
					
				town.forceSetMayor(res);
			} catch (TownyException e1) {
				e1.getMessage();
				if (town.getResidents().size() == 0) {
					deleteTown(town);
					return true;
				} else {
					town.findNewMayor();
				}
			}

			town.setBoard(rs.getString("townBoard"));
			line = rs.getString("tag");
			if (line != null)
				try {
					town.setTag(line);
				} catch (TownyException e) {
					town.setTag("");
				}
			town.setPermissions(rs.getString("protectionStatus").replaceAll("#", ","));
			town.setBonusBlocks(rs.getInt("bonus"));
			town.setTaxPercentage(rs.getBoolean("taxpercent"));
			town.setTaxes(rs.getFloat("taxes"));
			town.setHasUpkeep(rs.getBoolean("hasUpkeep"));
			town.setPlotPrice(rs.getFloat("plotPrice"));
			town.setPlotTax(rs.getFloat("plotTax"));
			town.setEmbassyPlotPrice(rs.getFloat("embassyPlotPrice"));
			town.setEmbassyPlotTax(rs.getFloat("embassyPlotTax"));
			town.setCommercialPlotPrice(rs.getFloat("commercialPlotPrice"));
			town.setCommercialPlotTax(rs.getFloat("commercialPlotTax"));
			town.setSpawnCost(rs.getFloat("spawnCost"));
			town.setOpen(rs.getBoolean("open"));
			town.setPublic(rs.getBoolean("public"));
			town.setConquered(rs.getBoolean("conquered"));
			town.setAdminDisabledPVP(rs.getBoolean("admindisabledpvp"));
			town.setAdminEnabledPVP(rs.getBoolean("adminenabledpvp"));

			town.setPurchasedBlocks(rs.getInt("purchased"));

			line = rs.getString("homeBlock");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				if (tokens.length == 3)
					try {
						TownyWorld world = getWorld(tokens[0]);

						try {
							int x = Integer.parseInt(tokens[1]);
							int z = Integer.parseInt(tokens[2]);
							TownBlock homeBlock = TownyUniverse.getInstance()
									.getTownBlock(new WorldCoord(world.getName(), x, z));
							town.forceSetHomeBlock(homeBlock);
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(
									"[Warning] " + town.getName() + " homeBlock tried to load invalid location.");
						} catch (NotRegisteredException e) {
							TownyMessaging.sendErrorMsg(
									"[Warning] " + town.getName() + " homeBlock tried to load invalid TownBlock.");
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " does not have a home block.");
						}

					} catch (NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(
								"[Warning] " + town.getName() + " homeBlock tried to load invalid world.");
					}
			}

			line = rs.getString("spawn");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				if (tokens.length >= 4)
					try {
						World world = plugin.getServerWorld(tokens[0]);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double z = Double.parseDouble(tokens[3]);

						Location loc = new Location(world, x, y, z);
						if (tokens.length == 6) {
							loc.setPitch(Float.parseFloat(tokens[4]));
							loc.setYaw(Float.parseFloat(tokens[5]));
						}
						town.forceSetSpawn(loc);
					} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
					}
			}
			// Load outpost spawns
			line = rs.getString("outpostSpawns");
			if (line != null) {
				String[] outposts = line.split(";");
				for (String spawn : outposts) {
					search = (line.contains("#")) ? "#" : ",";
					tokens = spawn.split(search);
					if (tokens.length >= 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);

							Location loc = new Location(world, x, y, z);
							if (tokens.length == 6) {
								loc.setPitch(Float.parseFloat(tokens[4]));
								loc.setYaw(Float.parseFloat(tokens[5]));
							}
							town.forceAddOutpostSpawn(loc);
						} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
						}
				}
			}
			// Load jail spawns
			line = rs.getString("jailSpawns");
			if (line != null) {
				String[] jails = line.split(";");
				for (String spawn : jails) {
					search = (line.contains("#")) ? "#" : ",";
					tokens = spawn.split(search);
					if (tokens.length >= 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);

							Location loc = new Location(world, x, y, z);
							if (tokens.length == 6) {
								loc.setPitch(Float.parseFloat(tokens[4]));
								loc.setYaw(Float.parseFloat(tokens[5]));
							}
							town.forceAddJailSpawn(loc);
						} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
						}
				}
			}
			line = rs.getString("outlaws");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				for (String token : tokens) {
					if (!token.isEmpty()) {
						Resident resident = universe.getResident(token);
						if (resident != null)
							town.addOutlaw(resident);
						else {
							System.out.println(String.format(
								"[Towny] Loading Error: Cannot load outlaw with name '%s' for town '%s'! Skipping adding outlaw to town...",
								token, town.getName()
							));
						}
					}
				}
			}

			try {
				town.setUUID(UUID.fromString(rs.getString("uuid")));
			} catch (IllegalArgumentException | NullPointerException ee) {
				town.setUUID(UUID.randomUUID());
			}
			TownyUniverse.getInstance().registerTownUUID(town);

			int conqueredDays = rs.getInt("conqueredDays");
			town.setConqueredDays(conqueredDays);

			try {
				long registered = rs.getLong("registered");
				town.setRegistered(registered);
			} catch (Exception ignored) {
				town.setRegistered(0);
			}

			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					town.setMetadata(line);
				}
			} catch (SQLException ignored) {
			}

			try {
				line = rs.getString("nation");
				if (line != null && !line.isEmpty()) {
					Nation nation = universe.getNation(line);
					// Only set nation if it exists
					if (nation != null)
						town.setNation(nation);
				}
			} catch (SQLException ignored) {
			}

			town.setRuined(rs.getBoolean("ruined"));
			town.setRuinedTime(rs.getLong("ruinedTime"));
			town.setNeutral(rs.getBoolean("neutral"));

			town.setDebtBalance(rs.getFloat("debtBalance"));
			
			town.setOfflinePlayer();

			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town " + name + " sql Error - " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town " + name + " unknown Error - ");
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean loadNations() {
		if (!getContext())
			return false;

		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "NATIONS")) {
			while (rs.next()) {
				if (!loadNation(rs)) {
					System.out.println("[Towny] Loading Error: Could not properly read nation data.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation sql error " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public boolean loadNation(Nation nation) {
		if (!getContext())
			return false;

		try (PreparedStatement ps = cntx.prepareStatement("SELECT * FROM " + tb_prefix + "NATIONS WHERE name=?")) {
			ps.setString(1, nation.getName());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return loadNation(rs);
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation sql error " + e.getMessage());
		}
		return false;
	}

	private boolean loadNation(ResultSet rs) {
		String line;
		String[] tokens;
		String search;
		String name = null;
		try {
			Nation nation = universe.getNation(rs.getString("name"));
			
			// Could not find nation in universe maps
			if (nation == null) {
				System.out.println(String.format("[Towny] Error: The nation with the name '%s' was not registered and cannot be loaded!"));
				return false;
			}
			
			name = nation.getName();

			TownyMessaging.sendDebugMsg("Loading nation " + nation.getName());

			Town town = universe.getTown(rs.getString("capital"));
			if (town != null) {
				try {
					nation.forceSetCapital(town);
				} catch (EmptyNationException e1) {
					System.out.println("The nation " + nation.getName() + " could not load a capital city and is being disbanded.");
					removeNation(nation);
					return true;
				}
			}
			else {
				TownyMessaging.sendDebugMsg("Nation " + name + " could not set capital to " + rs.getString("capital") + ", selecting a new capital...");
				if (!nation.findNewCapital()) {
					System.out.println("The nation " + nation.getName() + " could not load a capital city and is being disbanded.");
					removeNation(nation);
					return true;
				}
			}

			line = rs.getString("nationBoard");
			if (line != null)
				nation.setBoard(rs.getString("nationBoard"));
			else
				nation.setBoard("");

			line = rs.getString("mapColorHexCode");
			if (line != null)
				nation.setMapColorHexCode(line);
			else
				nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());

			nation.setTag(rs.getString("tag"));

			line = rs.getString("allies");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				List<Nation> allies = getNations(line.split(search));
				for (Nation ally : allies)
					nation.addAlly(ally);
			}

			line = rs.getString("enemies");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				List<Nation> enemies = getNations(line.split(search));
				for (Nation enemy : enemies) 
					nation.addEnemy(enemy);
			}

			nation.setTaxes(rs.getDouble("taxes"));
			nation.setSpawnCost(rs.getFloat("spawnCost"));
			nation.setNeutral(rs.getBoolean("neutral"));
			try {
				nation.setUUID(UUID.fromString(rs.getString("uuid")));
			} catch (IllegalArgumentException | NullPointerException ee) {
				nation.setUUID(UUID.randomUUID());
			}
			universe.registerNationUUID(nation);

			line = rs.getString("nationSpawn");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				if (tokens.length >= 4)
					try {
						World world = plugin.getServerWorld(tokens[0]);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double z = Double.parseDouble(tokens[3]);

						Location loc = new Location(world, x, y, z);
						if (tokens.length == 6) {
							loc.setPitch(Float.parseFloat(tokens[4]));
							loc.setYaw(Float.parseFloat(tokens[5]));
						}
						nation.forceSetNationSpawn(loc);
					} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
					}
			}

			nation.setPublic(rs.getBoolean("isPublic"));

			nation.setOpen(rs.getBoolean("isOpen"));

			try {
				line = rs.getString("registered");
				if (line != null) {
					nation.setRegistered(Long.parseLong(line));
				} else {
					nation.setRegistered(0);
				}
			} catch (SQLException ignored) {
			} catch (NumberFormatException | NullPointerException e) {
				nation.setRegistered(0);
			}

			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					nation.setMetadata(line);
				}
			} catch (SQLException ignored) {
			}

			nation.setOfflinePlayer();
			
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " SQL Error - " + e.getMessage());
		} catch (TownyException ex) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " unknown Error - ");
			ex.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean loadWorlds() {
		if (!getContext())
			return false;

		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "WORLDS")) {

			while (rs.next()) {
				if (!loadWorld(rs)) {
					System.out.println("[Towny] Loading Error: Could not read properly world data.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Error reading worlds from SQL database!");
			return false;
		}
		return true;
	}

	@Override
	public boolean loadWorld(TownyWorld world) {
		if (!getContext())
			return false;

		try (PreparedStatement ps = cntx.prepareStatement("SELECT * FROM " + tb_prefix + "WORLDS WHERE name=?")) {
			ps.setString(1, world.getName());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return loadWorld(rs);
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load world sql error (" + world.getName() + ")" + e.getMessage());
		}
		return false;
	}

	private boolean loadWorld(ResultSet rs) {
		String line;
		boolean result;
		long resultLong;
		String search;
		String worldName = null;
		try {
			worldName = rs.getString("name");
			TownyWorld world = getWorld(worldName);

			TownyMessaging.sendDebugMsg("Loading world " + world.getName());

			result = rs.getBoolean("claimable");
			try {
				world.setClaimable(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("pvp");
			try {
				world.setPVP(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forcepvp");
			try {
				world.setForcePVP(result);
			} catch (Exception ignored) {
			}
			
			result = rs.getBoolean("friendlyFire");
			try {
				world.setFriendlyFire(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forcetownmobs");
			try {
				world.setForceTownMobs(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("wildernessmobs");
			try {
				world.setWildernessMobs(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("worldmobs");
			try {
				world.setWorldMobs(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("firespread");
			try {
				world.setFire(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forcefirespread");
			try {
				world.setForceFire(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("explosions");
			try {
				world.setExpl(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forceexplosions");
			try {
				world.setForceExpl(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("endermanprotect");
			try {
				world.setEndermanProtect(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("disableplayertrample");
			try {
				world.setDisablePlayerTrample(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("disablecreaturetrample");
			try {
				world.setDisableCreatureTrample(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneBuild");
			try {
				world.setUnclaimedZoneBuild(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneDestroy");
			try {
				world.setUnclaimedZoneDestroy(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneSwitch");
			try {
				world.setUnclaimedZoneSwitch(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneItemUse");
			try {
				world.setUnclaimedZoneItemUse(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("unclaimedZoneName");
			try {
				world.setUnclaimedZoneName(line);
			} catch (Exception ignored) {
			}

			line = rs.getString("unclaimedZoneIgnoreIds");
			if (line != null)
				try {
					List<String> mats = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							mats.add(split);

					world.setUnclaimedZoneIgnore(mats);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementDelete");
			try {
				world.setUsingPlotManagementDelete(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementDeleteIds");
			if (line != null)
				try {
					List<String> mats = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							mats.add(split);

					world.setPlotManagementDeleteIds(mats);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementMayorDelete");
			try {
				world.setUsingPlotManagementMayorDelete(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementMayorDelete");
			if (line != null)
				try {
					List<String> materials = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								materials.add(split.toUpperCase().trim());
							} catch (NumberFormatException ignored) {
							}
					world.setPlotManagementMayorDelete(materials);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementRevert");
			try {
				world.setUsingPlotManagementRevert(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementIgnoreIds");
			if (line != null)
				try {
					List<String> mats = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							mats.add(split);

					world.setPlotManagementIgnoreIds(mats);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementWildRegen");
			try {
				world.setUsingPlotManagementWildEntityRevert(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementWildRegenEntities");
			if (line != null)
				try {
					List<String> entities = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								entities.add(split.trim());
							} catch (NumberFormatException ignored) {
							}
					world.setPlotManagementWildRevertEntities(entities);
				} catch (Exception ignored) {
				}

			resultLong = rs.getLong("plotManagementWildRegenSpeed");
			try {
				world.setPlotManagementWildRevertDelay(resultLong);
			} catch (Exception ignored) {
			}
			
			result = rs.getBoolean("usingPlotManagementWildRegenBlocks");
			try {
				world.setUsingPlotManagementWildBlockRevert(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementWildRegenBlocks");
			if (line != null)
				try {
					List<String> materials = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								materials.add(split.trim());
							} catch (NumberFormatException ignored) {
							}
					world.setPlotManagementWildRevertMaterials(materials);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingTowny");
			try {
				world.setUsingTowny(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("warAllowed");
			try {
				world.setWarAllowed(result);
			} catch (Exception ignored) {
			}

			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					world.setMetadata(line);
				}
			} catch (SQLException ignored) {
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg(
					"SQL: Load world sql error (" + (worldName != null ? worldName : "NULL") + ")" + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load world unknown error - ");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadTownBlocks() {

		String line = "";
		boolean result;
		TownyMessaging.sendDebugMsg("Loading Town Blocks.");

		// Load town blocks
		if (!getContext())
			return false;

		TownBlock townBlock = null;
		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNBLOCKS")) {

			while (rs.next()) {
				String worldName = rs.getString("world");
				int x = rs.getInt("x");
				int z = rs.getInt("z");

				try {
					townBlock = TownyUniverse.getInstance().getTownBlock(new WorldCoord(worldName, x, z));
				} catch (NotRegisteredException ex) {
					TownyMessaging.sendErrorMsg("Loading Error: Exception while fetching townblock: " + worldName + " "
							+ x + " " + z + " from memory!");
					return false;
				}

				line = rs.getString("name");
				if (line != null)
					try {
						townBlock.setName(line.trim());
					} catch (Exception ignored) {
					}

				line = rs.getString("price");
				if (line != null)
					try {
						townBlock.setPlotPrice(Float.parseFloat(line.trim()));
					} catch (Exception ignored) {
					}

				line = rs.getString("town");
				if (line != null) {
					Town town = universe.getTown(line.trim());
					
					if (town == null) {
						TownyMessaging.sendErrorMsg("TownBlock file contains unregistered Town: " + line
							+ " , deleting " + townBlock.getWorld().getName() + "," + townBlock.getX() + ","
							+ townBlock.getZ());
						TownyUniverse.getInstance().removeTownBlock(townBlock);
						deleteTownBlock(townBlock);
						continue;
					}
					
					townBlock.setTown(town);
					try {
						town.addTownBlock(townBlock);
						TownyWorld townyWorld = townBlock.getWorld();
						if (townyWorld != null && !townyWorld.hasTown(town))
							townyWorld.addTown(town);
					} catch (AlreadyRegisteredException ignored) {
					}
				}
				line = rs.getString("resident");
				if (line != null && !line.isEmpty()) {
					Resident res = universe.getResident(line.trim());
					if (res != null)
						townBlock.setResident(res);
					else {
						TownyMessaging.sendErrorMsg(String.format(
							"Error fetching resident '%s' for townblock '%s'!",
							line.trim(), townBlock.toString()
						));
					}
				}

				line = rs.getString("type");
				if (line != null)
					try {
						townBlock.setType(Integer.parseInt(line));
					} catch (Exception ignored) {
					}

				boolean outpost = rs.getBoolean("outpost");
				if (line != null && !line.isEmpty())
					try {
						townBlock.setOutpost(outpost);
					} catch (Exception ignored) {
					}

				line = rs.getString("permissions");
				if ((line != null) && !line.isEmpty())
					try {
						townBlock.setPermissions(line.trim().replaceAll("#", ","));
						// set = true;
					} catch (Exception ignored) {
					}

				result = rs.getBoolean("changed");
				try {
					townBlock.setChanged(result);
				} catch (Exception ignored) {
				}

				result = rs.getBoolean("locked");
				try {
					townBlock.setLocked(result);
				} catch (Exception ignored) {
				}

				try {
					line = rs.getString("metadata");
					if (line != null && !line.isEmpty()) {
						townBlock.setMetadata(line);
					}
				} catch (SQLException ignored) {
				}

				try {
					line = rs.getString("groupID");
					if (line != null && !line.isEmpty()) {
						try {
							UUID groupID = UUID.fromString(line.trim());
							PlotGroup group = getPlotObjectGroup(townBlock.getTown().toString(), groupID);
							townBlock.setPlotObjectGroup(group);
						} catch (Exception ignored) {
						}

					}
				} catch (SQLException ignored) {
				}
			}

		} catch (SQLException ex) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlock: "
					+ (townBlock != null ? townBlock : "NULL") + " at line: " + line + " in the sql database");
			ex.printStackTrace();
			return false;
		}

		return true;
	}
	
	@Override
	public boolean loadPlotGroups() {
		String line = "";
		TownyMessaging.sendDebugMsg("Loading plot groups.");

		// Load town blocks
		if (!getContext())
			return false;

		ResultSet rs;

		for (PlotGroup plotGroup : getAllPlotGroups()) {
			try {
				try (Statement s = cntx.createStatement()) {
					rs = s.executeQuery("SELECT * FROM " + tb_prefix + "PLOTGROUPS" + " WHERE groupID='"
							+ plotGroup.getID().toString() + "'");

					while (rs.next()) {
						line = rs.getString("groupName");
						if (line != null)
							try {
								plotGroup.setName(line.trim());
							} catch (Exception ignored) {
							}

						line = rs.getString("groupID");
						if (line != null) {
							try {
								plotGroup.setID(UUID.fromString(line.trim()));
							} catch (Exception ignored) {
							}
						}

						line = rs.getString("town");
						if (line != null) {
							Town town = universe.getTown(line.trim());
							if (town != null) {
								plotGroup.setTown(town);
							}
						}

						line = rs.getString("groupPrice");
						if (line != null) {
							try {
								plotGroup.setPrice(Float.parseFloat(line.trim()));
							} catch (Exception ignored) {
							}
						}
					}
				}
			} catch (SQLException e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading plot group: " + plotGroup.getName()
						+ " at line: " + line + " in the sql database");
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public synchronized boolean saveResident(Resident resident) {

		TownyMessaging.sendDebugMsg("Saving Resident " + resident.getName());
		try {
			HashMap<String, Object> res_hm = new HashMap<>();
			res_hm.put("name", resident.getName());
			res_hm.put("uuid", resident.hasUUID() ? resident.getUUID().toString() : "");
			res_hm.put("lastOnline", resident.getLastOnline());
			res_hm.put("registered", resident.getRegistered());
			res_hm.put("isNPC", resident.isNPC());
			res_hm.put("isJailed", resident.isJailed());
			res_hm.put("JailSpawn", resident.getJailSpawn());
			res_hm.put("JailDays", resident.getJailDays());
			res_hm.put("JailTown", resident.getJailTown());
			res_hm.put("title", resident.getTitle());
			res_hm.put("surname", resident.getSurname());
			res_hm.put("town", resident.hasTown() ? resident.getTown().getName() : "");
			res_hm.put("town-ranks", resident.hasTown() ? StringMgmt.join(resident.getTownRanks(), "#") : "");
			res_hm.put("nation-ranks", resident.hasTown() ? StringMgmt.join(resident.getNationRanks(), "#") : "");
			res_hm.put("friends", StringMgmt.join(resident.getFriends(), "#"));
			res_hm.put("protectionStatus", resident.getPermissions().toString().replaceAll(",", "#"));

			if (resident.hasMeta())
				res_hm.put("metadata", StringMgmt.join(new ArrayList<CustomDataField<?>>(resident.getMetadata()), ";"));
			else
				res_hm.put("metadata", "");

			UpdateDB("RESIDENTS", res_hm, Collections.singletonList("name"));
			return true;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public synchronized boolean saveTown(Town town) {

		TownyMessaging.sendDebugMsg("Saving town " + town.getName());
		try {
			HashMap<String, Object> twn_hm = new HashMap<>();
			twn_hm.put("name", town.getName());
			twn_hm.put("outlaws", StringMgmt.join(town.getOutlaws(), "#"));
			twn_hm.put("mayor", town.hasMayor() ? town.getMayor().getName() : "");
			twn_hm.put("nation", town.hasNation() ? town.getNation().getName() : "");
			twn_hm.put("assistants", StringMgmt.join(town.getRank("assistant"), "#"));
			twn_hm.put("townBoard", town.getBoard());
			twn_hm.put("tag", town.getTag());
			twn_hm.put("protectionStatus", town.getPermissions().toString().replaceAll(",", "#"));
			twn_hm.put("bonus", town.getBonusBlocks());
			twn_hm.put("purchased", town.getPurchasedBlocks());
			twn_hm.put("commercialPlotPrice", town.getCommercialPlotPrice());
			twn_hm.put("commercialPlotTax", town.getCommercialPlotTax());
			twn_hm.put("embassyPlotPrice", town.getEmbassyPlotPrice());
			twn_hm.put("embassyPlotTax", town.getEmbassyPlotTax());
			twn_hm.put("spawnCost", town.getSpawnCost());
			twn_hm.put("plotPrice", town.getPlotPrice());
			twn_hm.put("plotTax", town.getPlotTax());
			twn_hm.put("taxes", town.getTaxes());
			twn_hm.put("hasUpkeep", town.hasUpkeep());
			twn_hm.put("taxpercent", town.isTaxPercentage());
			twn_hm.put("open", town.isOpen());
			twn_hm.put("public", town.isPublic());
			twn_hm.put("conquered", town.isConquered());
			twn_hm.put("conqueredDays", town.getConqueredDays());
			twn_hm.put("admindisabledpvp", town.isAdminDisabledPVP());
			twn_hm.put("adminenabledpvp", town.isAdminEnabledPVP());
			if (town.hasMeta())
				twn_hm.put("metadata", StringMgmt.join(town.getMetadata(), ";"));
			else
				twn_hm.put("metadata", "");

			twn_hm.put("homeblock",
					town.hasHomeBlock()
							? town.getHomeBlock().getWorld().getName() + "#" + town.getHomeBlock().getX() + "#"
									+ town.getHomeBlock().getZ()
							: "");
			twn_hm.put("spawn",
					town.hasSpawn()
							? town.getSpawn().getWorld().getName() + "#" + town.getSpawn().getX() + "#"
									+ town.getSpawn().getY() + "#" + town.getSpawn().getZ() + "#"
									+ town.getSpawn().getPitch() + "#" + town.getSpawn().getYaw()
							: "");
			// Outpost Spawns
			StringBuilder outpostArray = new StringBuilder();
			if (town.hasOutpostSpawn())
				for (Location spawn : new ArrayList<>(town.getAllOutpostSpawns())) {
					outpostArray.append(spawn.getWorld().getName()).append("#").append(spawn.getX()).append("#")
							.append(spawn.getY()).append("#").append(spawn.getZ()).append("#").append(spawn.getPitch())
							.append("#").append(spawn.getYaw()).append(";");
				}
			twn_hm.put("outpostSpawns", outpostArray.toString());
			StringBuilder jailArray = new StringBuilder();
			if (town.hasJailSpawn())
				for (Location spawn : new ArrayList<>(town.getAllJailSpawns())) {
					jailArray.append(spawn.getWorld().getName()).append("#").append(spawn.getX()).append("#")
							.append(spawn.getY()).append("#").append(spawn.getZ()).append("#").append(spawn.getPitch())
							.append("#").append(spawn.getYaw()).append(";");
				}
			twn_hm.put("jailSpawns", jailArray.toString());
			if (town.hasValidUUID()) {
				twn_hm.put("uuid", town.getUUID());
			} else {
				twn_hm.put("uuid", UUID.randomUUID());
			}
			twn_hm.put("registered", town.getRegistered());

			twn_hm.put("ruined", town.isRuined());
			twn_hm.put("ruinedTime", town.getRuinedTime());
			twn_hm.put("neutral", town.isNeutral());
			
			twn_hm.put("debtBalance", town.getDebtBalance());

			UpdateDB("TOWNS", twn_hm, Collections.singletonList("name"));
			return true;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Town unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean savePlotGroup(PlotGroup group) {
		TownyMessaging.sendDebugMsg("Saving group " + group.getName());
		try {
			HashMap<String, Object> pltgrp_hm = new HashMap<>();
			pltgrp_hm.put("groupName", group.getName());
			pltgrp_hm.put("groupID", group.getID());
			pltgrp_hm.put("groupPrice", group.getPrice());
			pltgrp_hm.put("town", group.getTown().toString());

			UpdateDB("PLOTGROUPS", pltgrp_hm, Collections.singletonList("name"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Plot groups unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveNation(Nation nation) {

		TownyMessaging.sendDebugMsg("Saving nation " + nation.getName());
		try {
			HashMap<String, Object> nat_hm = new HashMap<>();
			nat_hm.put("name", nation.getName());
			nat_hm.put("capital", nation.hasCapital() ? nation.getCapital().getName() : "");
			nat_hm.put("nationBoard", nation.getBoard());
			nat_hm.put("mapColorHexCode", nation.getMapColorHexCode());
			nat_hm.put("tag", nation.hasTag() ? nation.getTag() : "");
			nat_hm.put("allies", StringMgmt.join(nation.getAllies(), "#"));
			nat_hm.put("enemies", StringMgmt.join(nation.getEnemies(), "#"));
			nat_hm.put("taxes", nation.getTaxes());
			nat_hm.put("spawnCost", nation.getSpawnCost());
			nat_hm.put("neutral", nation.isNeutral());
			nat_hm.put("nationSpawn",
					nation.hasSpawn()
							? nation.getSpawn().getWorld().getName() + "#" + nation.getSpawn().getX() + "#"
									+ nation.getSpawn().getY() + "#" + nation.getSpawn().getZ() + "#"
									+ nation.getSpawn().getPitch() + "#" + nation.getSpawn().getYaw()
							: "");
			if (nation.hasValidUUID()) {
				nat_hm.put("uuid", nation.getUUID());
			} else {
				nat_hm.put("uuid", UUID.randomUUID());
			}
			nat_hm.put("registered", nation.getRegistered());
			nat_hm.put("isPublic", nation.isPublic());
			nat_hm.put("isOpen", nation.isOpen());

			if (nation.hasMeta())
				nat_hm.put("metadata", StringMgmt.join(nation.getMetadata(), ";"));
			else
				nat_hm.put("metadata", "");

			UpdateDB("NATIONS", nat_hm, Collections.singletonList("name"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Nation unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveWorld(TownyWorld world) {

		TownyMessaging.sendDebugMsg("Saving world " + world.getName());
		try {
			HashMap<String, Object> nat_hm = new HashMap<>();

			nat_hm.put("name", world.getName());

			// PvP
			nat_hm.put("pvp", world.isPVP());
			// Force PvP
			nat_hm.put("forcepvp", world.isForcePVP());
			// Friendly Fire
			nat_hm.put("friendlyFire", world.isFriendlyFireEnabled());
			// Claimable
			nat_hm.put("claimable", world.isClaimable());
			// has monster spawns
			nat_hm.put("worldmobs", world.hasWorldMobs());
			// has wilderness monster spawns
			nat_hm.put("wildernessmobs", world.hasWildernessMobs());
			// force town mob spawns
			nat_hm.put("forcetownmobs", world.isForceTownMobs());
			// has firespread enabled
			nat_hm.put("firespread", world.isFire());
			nat_hm.put("forcefirespread", world.isForceFire());
			// has explosions enabled
			nat_hm.put("explosions", world.isExpl());
			nat_hm.put("forceexplosions", world.isForceExpl());
			// Enderman block protection
			nat_hm.put("endermanprotect", world.isEndermanProtect());
			// PlayerTrample
			nat_hm.put("disableplayertrample", world.isDisablePlayerTrample());
			// CreatureTrample
			nat_hm.put("disablecreaturetrample", world.isDisableCreatureTrample());

			// Unclaimed Zone Build
			nat_hm.put("unclaimedZoneBuild", world.getUnclaimedZoneBuild());
			// Unclaimed Zone Destroy
			nat_hm.put("unclaimedZoneDestroy", world.getUnclaimedZoneDestroy());
			// Unclaimed Zone Switch
			nat_hm.put("unclaimedZoneSwitch", world.getUnclaimedZoneSwitch());
			// Unclaimed Zone Item Use
			nat_hm.put("unclaimedZoneItemUse", world.getUnclaimedZoneItemUse());
			// Unclaimed Zone Name
			if (world.getUnclaimedZoneName() != null)
				nat_hm.put("unclaimedZoneName", world.getUnclaimedZoneName());

			// Unclaimed Zone Ignore Ids
			if (world.getUnclaimedZoneIgnoreMaterials() != null)
				nat_hm.put("unclaimedZoneIgnoreIds", StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), "#"));

			// Using PlotManagement Delete
			nat_hm.put("usingPlotManagementDelete", world.isUsingPlotManagementDelete());
			// Plot Management Delete Ids
			if (world.getPlotManagementDeleteIds() != null)
				nat_hm.put("plotManagementDeleteIds", StringMgmt.join(world.getPlotManagementDeleteIds(), "#"));

			// Using PlotManagement Mayor Delete
			nat_hm.put("usingPlotManagementMayorDelete", world.isUsingPlotManagementMayorDelete());
			// Plot Management Mayor Delete
			if (world.getPlotManagementMayorDelete() != null)
				nat_hm.put("plotManagementMayorDelete", StringMgmt.join(world.getPlotManagementMayorDelete(), "#"));

			// Using PlotManagement Revert
			nat_hm.put("usingPlotManagementRevert", world.isUsingPlotManagementRevert());

			// Plot Management Ignore Ids
			if (world.getPlotManagementIgnoreIds() != null)
				nat_hm.put("plotManagementIgnoreIds", StringMgmt.join(world.getPlotManagementIgnoreIds(), "#"));

			// Using PlotManagement Wild Regen
			nat_hm.put("usingPlotManagementWildRegen", world.isUsingPlotManagementWildEntityRevert());

			// Wilderness Explosion Protection entities
			if (world.getPlotManagementWildRevertEntities() != null)
				nat_hm.put("PlotManagementWildRegenEntities",
						StringMgmt.join(world.getPlotManagementWildRevertEntities(), "#"));

			// Using PlotManagement Wild Regen Delay
			nat_hm.put("plotManagementWildRegenSpeed", world.getPlotManagementWildRevertDelay());
			
			// Using PlotManagement Wild Block Regen
			nat_hm.put("usingPlotManagementWildRegenBlocks", world.isUsingPlotManagementWildBlockRevert());

			// Wilderness Explosion Protection blocks
			if (world.getPlotManagementWildRevertBlocks() != null)
				nat_hm.put("PlotManagementWildRegenBlocks",
						StringMgmt.join(world.getPlotManagementWildRevertBlocks(), "#"));

			// Using Towny
			nat_hm.put("usingTowny", world.isUsingTowny());

			// War allowed in this world.
			nat_hm.put("warAllowed", world.isWarAllowed());

			if (world.hasMeta())
				nat_hm.put("metadata", StringMgmt.join(world.getMetadata(), ";"));
			else
				nat_hm.put("metadata", "");

			UpdateDB("WORLDS", nat_hm, Collections.singletonList("name"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save world unknown error (" + world.getName() + ")");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public synchronized boolean saveTownBlock(TownBlock townBlock) {

		TownyMessaging.sendDebugMsg("Saving town block " + townBlock.getWorld().getName() + ":" + townBlock.getX() + "x"
				+ townBlock.getZ());
		try {
			HashMap<String, Object> tb_hm = new HashMap<>();
			tb_hm.put("world", townBlock.getWorld().getName());
			tb_hm.put("x", townBlock.getX());
			tb_hm.put("z", townBlock.getZ());
			tb_hm.put("name", townBlock.getName());
			tb_hm.put("price", townBlock.getPlotPrice());
			tb_hm.put("town", townBlock.getTown().getName());
			tb_hm.put("resident", (townBlock.hasResident()) ? townBlock.getResident().getName() : "");
			tb_hm.put("type", townBlock.getType().getId());
			tb_hm.put("outpost", townBlock.isOutpost());
			tb_hm.put("permissions",
					(townBlock.isChanged()) ? townBlock.getPermissions().toString().replaceAll(",", "#") : "");
			tb_hm.put("locked", townBlock.isLocked());
			tb_hm.put("changed", townBlock.isChanged());
			if (townBlock.hasPlotObjectGroup())
				tb_hm.put("groupID", townBlock.getPlotObjectGroup().getID().toString());
			else
				tb_hm.put("groupID", "");
			if (townBlock.hasMeta())
				tb_hm.put("metadata", StringMgmt.join(new ArrayList<CustomDataField<?>>(townBlock.getMetadata()), ";"));
			else
				tb_hm.put("metadata", "");

			UpdateDB("TOWNBLOCKS", tb_hm, Arrays.asList("world", "x", "z"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save TownBlock unknown error");
			e.printStackTrace();
		}
		return true;
	}

	/*
	 * Delete objects
	 */
	
	@Override
	public void deleteResident(Resident resident) {

		HashMap<String, Object> res_hm = new HashMap<>();
		res_hm.put("name", resident.getName());
		DeleteDB("RESIDENTS", res_hm);
	}

	@Override
	public void deleteTown(Town town) {

		HashMap<String, Object> twn_hm = new HashMap<>();
		twn_hm.put("name", town.getName());
		DeleteDB("TOWNS", twn_hm);
	}

	@Override
	public void deleteNation(Nation nation) {

		HashMap<String, Object> nat_hm = new HashMap<>();
		nat_hm.put("name", nation.getName());
		DeleteDB("NATIONS", nat_hm);
	}

	@Override
	public void deleteWorld(TownyWorld world) {

	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {
		HashMap<String, Object> twn_hm = new HashMap<>();
		twn_hm.put("world", townBlock.getWorld().getName());
		twn_hm.put("x", townBlock.getX());
		twn_hm.put("z", townBlock.getZ());
		DeleteDB("TOWNBLOCKS", twn_hm);
	}

	@Override
	public void deletePlotGroup(PlotGroup group) {

		HashMap<String, Object> pltgrp_hm = new HashMap<>();
		pltgrp_hm.put("name", group.getName());
		DeleteDB("PLOTGROUPS", pltgrp_hm);
	}

	/*
	 * Save keys (Unused by SQLSource)
	 */

	@Override
	public boolean savePlotGroupList() {
		return true;
	}

	@Override
	public boolean saveWorldList() {

		return true;
	}

	public HikariDataSource getHikariDataSource() {
		return hikariDataSource;
	}
}
