package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import org.bukkit.Location;
import org.bukkit.World;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

public final class TownyFlatFileSource extends TownyDatabaseHandler {

	private final String newLine = System.getProperty("line.separator");
	
	public TownyFlatFileSource(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		// Create files and folders if non-existent
		if (!FileMgmt.checkOrCreateFolders(
			rootFolderPath,
			dataFolderPath,
			dataFolderPath + File.separator + "residents",
			dataFolderPath + File.separator + "residents" + File.separator + "deleted",
			dataFolderPath + File.separator + "towns",
			dataFolderPath + File.separator + "towns" + File.separator + "deleted",
			dataFolderPath + File.separator + "nations",
			dataFolderPath + File.separator + "nations" + File.separator + "deleted",
			dataFolderPath + File.separator + "worlds",
			dataFolderPath + File.separator + "worlds" + File.separator + "deleted",
			dataFolderPath + File.separator + "townblocks",
			dataFolderPath + File.separator + "plotgroups"
		) || !FileMgmt.checkOrCreateFiles(
			dataFolderPath + File.separator + "worlds.txt",
			dataFolderPath + File.separator + "plotgroups.txt"
		)) {
			TownyMessaging.sendErrorMsg("Could not create flatfile default files and folders.");
		}
	}
	
	public enum elements {
		VER, novalue;

		public static elements fromString(String Str) {

			try {
				return valueOf(Str);
			} catch (Exception ex) {
				return novalue;
			}
		}
	}

	public String getResidentFilename(Resident resident) {

		return dataFolderPath + File.separator + "residents" + File.separator + resident.getName() + ".txt";
	}

	public String getTownFilename(Town town) {

		return dataFolderPath + File.separator + "towns" + File.separator + town.getName() + ".txt";
	}

	public String getNationFilename(Nation nation) {

		return dataFolderPath + File.separator + "nations" + File.separator + nation.getName() + ".txt";
	}

	public String getWorldFilename(TownyWorld world) {

		return dataFolderPath + File.separator + "worlds" + File.separator + world.getName() + ".txt";
	}

	public String getTownBlockFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
	}
	
	public String getPlotGroupFilename(PlotGroup group) {
		return dataFolderPath + File.separator + "plotgroups" + File.separator + group.getID() + ".data";
	}

	/*
	 * Load keys
	 */
	
	@Override
	public boolean loadTownBlockList() {
		
		TownyMessaging.sendDebugMsg("Loading TownBlock List");

		File townblocksFolder = new File(dataFolderPath + File.separator + "townblocks");
		File[] worldFolders = townblocksFolder.listFiles(File::isDirectory);
		TownyMessaging.sendDebugMsg("Folders found " + worldFolders.length);
		boolean mismatched = false;
		int mismatchedCount = 0;
		try {
			for (File worldfolder : worldFolders) {
				String worldName = worldfolder.getName();
				TownyWorld world;
				try {
					world = getWorld(worldName);
				} catch (NotRegisteredException e) {
					newWorld(worldName);
					world = getWorld(worldName);
				}
				File worldFolder = new File(dataFolderPath + File.separator + "townblocks" + File.separator + worldName);
				File[] townBlockFiles = worldFolder.listFiles((file)->file.getName().endsWith(".data"));
				int total = 0;
				for (File townBlockFile : townBlockFiles) {
					String[] coords = townBlockFile.getName().split("_");
					String[] size = coords[2].split("\\.");
					// Do not load a townBlockFile if it does not use teh currently set town_block_size.
					if (Integer.parseInt(size[0]) != TownySettings.getTownBlockSize()) {
						mismatched = true;
						mismatchedCount++;
						continue;
					}
					int x = Integer.parseInt(coords[0]);
					int z = Integer.parseInt(coords[1]);
	                TownBlock townBlock = new TownBlock(x, z, world);
	                TownyUniverse.getInstance().addTownBlock(townBlock);
					total++;
				}
				TownyMessaging.sendDebugMsg("World: " + worldName + " loaded " + total + " townblocks.");
			}
			if (mismatched)
				TownyMessaging.sendDebugMsg(String.format("%s townblocks were found with a town_block_size that does not match your config, they were not loaded into memory.", mismatchedCount));

			return true;
		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean loadPlotGroupList() {
		TownyMessaging.sendDebugMsg("Loading Group List");
		String line = null;

		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "plotgroups.txt"), StandardCharsets.UTF_8))) {

			while ((line = fin.readLine()) != null) {
				if (!line.equals("")) {
					String[] tokens = line.split(",");
					String townName = null;
					UUID groupID;
					String groupName;
					
					// While in development the PlotGroupList stored a 4th element, a worldname. This was scrapped pre-release. 
					if (tokens.length == 4) {
						townName = tokens[1];
						groupID = UUID.fromString(tokens[2]);
						groupName = tokens[3];
					} else {
						townName = tokens[0];
						groupID = UUID.fromString(tokens[1]);
						groupName = tokens[2];
					}
					Town town = universe.getTown(townName);
					
					if (town != null)
						universe.newGroup(town, groupName, groupID);
				}
			}
			
			return true;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Group List at " + line + ", in towny\\data\\groups.txt");
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean loadResidentList() {
		
		TownyMessaging.sendDebugMsg("Loading Resident List");
		List<String> residents = receiveListFromLegacyFile("residents.txt");
		File[] residentFiles = receiveObjectFiles("residents");
		assert residentFiles != null;

		for (File resident : residentFiles) {
			String name = resident.getName().replace(".txt", "");

			// Don't load resident files if they weren't in the residents.txt file.
			if (residents.size() > 0 && !residents.contains(name)) {
				TownyMessaging.sendDebugMsg("Removing " + resident.getName() + " because they are not found in the residents.txt.");
				deleteFile(resident.getAbsolutePath());
				continue;
			}
				
			try {
				newResident(name);
			} catch (NotRegisteredException e) {
				// Thrown if the resident name does not pass the filters.
				e.printStackTrace();
				return false;
			} catch (AlreadyRegisteredException ignored) {
				// Should not be possible in flatfile.
			}			
		}

		if (residents.size() > 0)
			deleteFile(dataFolderPath + File.separator + "residents.txt");

		return true;
			
	}
	
	@Override
	public boolean loadTownList() {
		
		TownyMessaging.sendDebugMsg("Loading Town List");
		List<String> towns = receiveListFromLegacyFile("towns.txt");
		File[] townFiles = receiveObjectFiles("towns");
		assert townFiles != null;
		
		for (File town : townFiles) {
			String name = town.getName().replace(".txt", "");

			// Don't load town files if they weren't in the towns.txt file.
			if (towns.size() > 0 && !towns.contains(name)) {
				TownyMessaging.sendDebugMsg("Removing " + town.getName() + " because they are not found in the towns.txt.");
				deleteFile(town.getAbsolutePath());
				continue;
			}
			
			try {
				TownyUniverse.getInstance().newTownInternal(name);
			} catch (AlreadyRegisteredException e) {
				// Should not be possible in flatfile.
			} catch (InvalidNameException e) {
				// Thrown if the town name does not pass the filters.
				e.printStackTrace();
				return false;
			}
		}
		
		if (towns.size() > 0)
			deleteFile(dataFolderPath + File.separator + "towns.txt");

		return true;

	}

	@Override
	public boolean loadNationList() {
		
		TownyMessaging.sendDebugMsg("Loading Nation List");
		List<String> nations = receiveListFromLegacyFile("nations.txt");
		File[] nationFiles = receiveObjectFiles("nations");
		assert nationFiles != null;
		for (File nation : nationFiles) {
			String name = nation.getName().replace(".txt", "");

			// Don't load nation files if they weren't in the nations.txt file.
			if (nations.size() > 0 && !nations.contains(name)) {
				TownyMessaging.sendDebugMsg("Removing " + nation.getName() + " because they are not found in the nations.txt.");
				deleteFile(nation.getAbsolutePath());
				continue;
			}
		
			try {
				newNation(name);
			} catch (AlreadyRegisteredException e) {
				// Should not be possible in flatfile.
			} catch (NotRegisteredException e) {
				// Thrown if the town name does not pass the filters.
				e.printStackTrace();
				return false;
			}
		}
		
		if (nations.size() > 0)
			deleteFile(dataFolderPath + File.separator + "nations.txt");
			
		return true;

	}
	
	@Override
	public boolean loadWorldList() {
		
		if (plugin != null) {
			TownyMessaging.sendDebugMsg("Loading Server World List");
			for (World world : plugin.getServer().getWorlds()) {
				try {
					newWorld(world.getName());
				} catch (AlreadyRegisteredException e) {
					//e.printStackTrace();
				}
			}
		}
		
		// Can no longer reply on Bukkit to report ALL available worlds.
		
		TownyMessaging.sendDebugMsg("Loading World List");
		
		String line = null;
		
		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "worlds.txt"), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					newWorld(line);
			
			return true;
			
		} catch (AlreadyRegisteredException e) {
			// Ignore this as the world may have been passed to us by bukkit
			return true;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading World List at " + line + ", in towny\\data\\worlds.txt");
			e.printStackTrace();
			return false;
			
		}
		
	}

	/**
	 * Util method to procur a list of Towny Objects that will no longer be saved.
	 * ex: residents.txt, towns.txt, nations.txt, etc.
	 * 
	 * @param listFile - string representing residents.txt/towns.txt/nations.txt.
	 * @return list - List<String> of names of towny objects which used to be saved to the database. 
	 */
	private List<String> receiveListFromLegacyFile(String listFile) {
		String line;
		List<String> list = new ArrayList<String>();
		// Build up a list of objects from any existing legacy objects.txt files.
		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + listFile), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null && !line.equals(""))
				list.add(line);
		} catch (Exception ignored) {
			// No towns/residents/nations.txt any more.
		}
		return list;
	}

	/**
	 * Util method for gathering towny object .txt files from their parent folder.
	 * ex: "residents" 
	 * @param folder - Towny object folder
	 * @return files - Files from inside the residents\towns\nations folder.
	 */
	private File[] receiveObjectFiles(String folder) {
		File[] files = new File(dataFolderPath + File.separator + folder).listFiles((file) -> file.getName().toLowerCase().endsWith(".txt"));
		return files;
	}
	
	/*
	 * Load individual towny objects
	 */
	
	@Override
	public boolean loadResident(Resident resident) {
		boolean save = true;
		String line = null;
		String path = getResidentFilename(resident);
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			TownyMessaging.sendDebugMsg("Loading Resident: " + resident.getName());
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileResident);
				
				resident.setLastOnline(Long.parseLong(keys.get("lastOnline")));
				
				line = keys.get("uuid");
				if (line != null) {
					UUID uuid = UUID.fromString(line);
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
							save = false;
							return true;
						}
					}					
					resident.setUUID(uuid);
					universe.registerResidentUUID(resident);
				}
				
				line = keys.get("registered");
				if (line != null)
					resident.setRegistered(Long.parseLong(line));
				else
					resident.setRegistered(resident.getLastOnline());
				
				line = keys.get("isNPC");
				if (line != null)
					resident.setNPC(Boolean.parseBoolean(line));
				
				line = keys.get("isJailed");
				if (line != null)
					resident.setJailed(Boolean.parseBoolean(line));
				
				line = keys.get("JailSpawn");
				if (line != null)
					resident.setJailSpawn(Integer.valueOf(line));
				
				line = keys.get("JailDays");
				if (line != null)
					resident.setJailDays(Integer.valueOf(line));
				
				line = keys.get("JailTown");
				if (line != null)
					resident.setJailTown(line);

				line = keys.get("friends");
				if (line != null) {
					List<Resident> friends = getResidents(line.split(","));
					for (Resident friend : friends) {
						resident.addFriend(friend);
					}
				}
				
				line = keys.get("protectionStatus");
				if (line != null)
					resident.setPermissions(line);

				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					resident.setMetadata(line.trim());

				line = keys.get("town");
				if (line != null) {
					Town town = universe.getTown(line);
					
					if (town == null) {
						TownyMessaging.sendErrorMsg("Loading Error: " + resident.getName() + " tried to load the town " + line + " which is invalid, removing town from the resident.");
					}
					else {
						resident.setTown(town);
						
						line = keys.get("title");
						if (line != null)
							resident.setTitle(line);
						
						line = keys.get("surname");
						if (line != null)
							resident.setSurname(line);
						
						try {
							line = keys.get("town-ranks");
							if (line != null)
								resident.setTownRanks(Arrays.asList((line.split(","))));
						} catch (Exception e) {}

						try {
							line = keys.get("nation-ranks");
							if (line != null)
								resident.setNationRanks(Arrays.asList((line.split(","))));
						} catch (Exception e) {}
					}
				}
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading resident file " + resident.getName() + " at line: " + line + ", in towny\\data\\residents\\" + resident.getName() + ".txt");
				e.printStackTrace();
				return false;
			} finally {
				if (save) saveResident(resident);
			}
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public boolean loadTown(Town town) {
		String line = null;
		String[] tokens;
		String path = getTownFilename(town);
		File fileTown = new File(path);		
		if (fileTown.exists() && fileTown.isFile()) {
			TownyMessaging.sendDebugMsg("Loading Town: " + town.getName());
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileTown);

				line = keys.get("mayor");
				if (line != null)
					try {
						Resident res = universe.getResident(line);
						if (res == null)
							throw new TownyException();
						
						town.forceSetMayor(res);
					} catch (TownyException e1) {
						if (town.getResidents().size() == 0)
							deleteTown(town);
						else 
							town.findNewMayor();

						return true;						
					}

				line = keys.get("outlaws");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							TownyMessaging.sendDebugMsg("Town Fetching Outlaw: " + token);
							Resident outlaw = universe.getResident(token);
							if (outlaw != null) {
								try { 
									town.addOutlaw(outlaw);
								} catch (AlreadyRegisteredException ex) {
									TownyMessaging.sendErrorMsg("Loading Error: Exception while reading an outlaw of town file " + town.getName() + ".txt. The outlaw " + token + " is duplicated! Skipping adding duplicate...");
								}
							}
							else {
								TownyMessaging.sendErrorMsg("Loading Error: Exception while reading an outlaw of town file " + town.getName() + ".txt. The outlaw " + token + " does not exist, removing from list...");
							}
						}
					}
				}

				line = "townBoard";
				town.setBoard(keys.get("townBoard"));

				line = keys.get("tag");
				if (line != null)
					try {
						town.setTag(line);
					} catch (TownyException e) {
						town.setTag("");
					}
				
				line = keys.get("protectionStatus");
				if (line != null)
					town.setPermissions(line);
				
				line = keys.get("bonusBlocks");
				if (line != null)
					try {
						town.setBonusBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setBonusBlocks(0);
					}
				
				line = keys.get("purchasedBlocks");
				if (line != null)
					try {
						town.setPurchasedBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setPurchasedBlocks(0);
					}
				
				line = keys.get("plotPrice");
				if (line != null)
					try {
						town.setPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setPlotPrice(0);
					}
				
				line = keys.get("hasUpkeep");
				if (line != null)
					try {
						town.setHasUpkeep(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("taxpercent");
				if (line != null)
					try {
						town.setTaxPercentage(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("taxes");
				if (line != null)
					try {
						town.setTaxes(Double.parseDouble(line));
					} catch (Exception e) {
						town.setTaxes(0);
					}
				
				line = keys.get("plotTax");
				if (line != null)
					try {
						town.setPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setPlotTax(0);
					}
				
				line = keys.get("commercialPlotPrice");
				if (line != null)
					try {
						town.setCommercialPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setCommercialPlotPrice(0);
					}
				
				line = keys.get("commercialPlotTax");
				if (line != null)
					try {
						town.setCommercialPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setCommercialPlotTax(0);
					}
				
				line = keys.get("embassyPlotPrice");
				if (line != null)
					try {
						town.setEmbassyPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setEmbassyPlotPrice(0);
					}
				
				line = keys.get("embassyPlotTax");
				if (line != null)
					try {
						town.setEmbassyPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setEmbassyPlotTax(0);
					}
				
				line = keys.get("spawnCost");
				if (line != null)
					try {
						town.setSpawnCost(Double.parseDouble(line));
					} catch (Exception e) {
						town.setSpawnCost(TownySettings.getSpawnTravelCost());
					}
				
				line = keys.get("adminDisabledPvP");
				if (line != null)
					try {
						town.setAdminDisabledPVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("adminEnabledPvP");
				if (line != null)
					try {
						town.setAdminEnabledPVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("open");
				if (line != null)
					try {
						town.setOpen(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("public");
				if (line != null)
					try {
						town.setPublic(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("conquered");
				if (line != null)
					try {
						town.setConquered(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("conqueredDays");
				if (line != null)
					town.setConqueredDays(Integer.valueOf(line));

				line = keys.get("homeBlock");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 3)
						try {
							TownyWorld world = getWorld(tokens[0]);
							try {
								int x = Integer.parseInt(tokens[1]);
								int z = Integer.parseInt(tokens[2]);
								TownBlock homeBlock = TownyUniverse.getInstance().getTownBlock(new WorldCoord(world.getName(), x, z));
								town.forceSetHomeBlock(homeBlock);
							} catch (NumberFormatException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid location.");
							} catch (NotRegisteredException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid TownBlock.");
							} catch (TownyException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " does not have a home block.");
							}
                        } catch (NotRegisteredException e) {
                            TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid world.");
                        }
				}
				
				line = keys.get("spawn");
				if (line != null) {
					tokens = line.split(",");
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
				line = keys.get("outpostspawns");
				if (line != null) {
					String[] outposts = line.split(";");
					for (String spawn : outposts) {
						tokens = spawn.split(",");
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
				line = keys.get("jailspawns");
				if (line != null) {
					String[] jails = line.split(";");
					for (String spawn : jails) {
						tokens = spawn.split(",");
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
				
				line = keys.get("uuid");
				if (line != null) {
					UUID townUUID = null;
					try {
						townUUID = UUID.fromString(line);
					} catch (IllegalArgumentException ee) {
						townUUID = UUID.randomUUID();
					}
					
					town.setUUID(townUUID);
					TownyUniverse.getInstance().registerTownUUID(town);
				}
				line = keys.get("registered");
				if (line != null) {
					try {
						town.setRegistered(Long.valueOf(line));
					} catch (Exception ee) {
						town.setRegistered(0);
					}
				}

				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					town.setMetadata(line.trim());
				
				line = keys.get("nation");
				if (line != null && !line.isEmpty()) {
					Nation nation = universe.getNation(line);;
					// Only set the nation if it exists
					if (nation != null)
						town.setNation(nation);
				}
					
				line = keys.get("ruined");
				if (line != null)
					try {
						town.setRuined(Boolean.parseBoolean(line));
					} catch (Exception e) {
						town.setRuined(false);
					}
				
				line = keys.get("ruinedTime");
				if (line != null)
					try {
						town.setRuinedTime(Long.valueOf(line));
					} catch (Exception ee) {
						town.setRuinedTime(0);
					}
				
				line = keys.get("neutral");
				if (line != null)
					town.setNeutral(Boolean.parseBoolean(line));
				
				line = keys.get("debtBalance");
				if (line != null)
					try {
						town.setDebtBalance(Double.parseDouble(line));
					} catch (Exception e) {
						town.setDebtBalance(0.0);
					}

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading town file " + town.getName() + " at line: " + line + ", in towny\\data\\towns\\" + town.getName() + ".txt");
				e.printStackTrace();
				return false;
			} finally {
				saveTown(town);
				town.setOfflinePlayer();
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean loadNation(Nation nation) {
		
		String line = "";
		String[] tokens;
		String path = getNationFilename(nation);
		File fileNation = new File(path);
		
		if (fileNation.exists() && fileNation.isFile()) {
			TownyMessaging.sendDebugMsg("Loading Nation: " + nation.getName());
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileNation);
				
				line = keys.get("capital");
				if (line != null) {
					Town town = universe.getTown(line);
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
						TownyMessaging.sendDebugMsg("Nation " + nation.getName() + " could not set capital to " + line + ", selecting a new capital...");
						if (!nation.findNewCapital()) {
							System.out.println("The nation " + nation.getName() + " could not load a capital city and is being disbanded.");
							removeNation(nation);
							return true;
						}
					}
				}
				line = keys.get("nationBoard");
				if (line != null)
					try {
						nation.setBoard(line);
					} catch (Exception e) {
						nation.setBoard("");
					}

				line = keys.get("mapColorHexCode");
				if (line != null) {
					try {
						nation.setMapColorHexCode(line);
					} catch (Exception e) {
						nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());
					}
				} else {
					nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());
				}

				line = keys.get("tag");
				if (line != null)
					try {
						nation.setTag(line);
					} catch (TownyException e) {
						nation.setTag("");
					}
				
				line = keys.get("allies");
				if (line != null) {
					List<Nation> allies = getNations(line.split(","));
					for (Nation ally : allies) {
						nation.addAlly(ally);
					}
				}
				
				line = keys.get("enemies");
				if (line != null) {
					List<Nation> enemies = getNations(line.split(","));
					for (Nation enemy : enemies) {
						nation.addEnemy(enemy);
					}
				}
				
				line = keys.get("taxes");
				if (line != null)
					try {
						nation.setTaxes(Double.parseDouble(line));
					} catch (Exception e) {
						nation.setTaxes(0.0);
					}
				
				line = keys.get("spawnCost");
				if (line != null)
					try {
						nation.setSpawnCost(Double.parseDouble(line));
					} catch (Exception e) {
						nation.setSpawnCost(TownySettings.getSpawnTravelCost());
					}
				
				line = keys.get("neutral");
				if (line != null)
					nation.setNeutral(Boolean.parseBoolean(line));
				
				line = keys.get("uuid");
				if (line != null) {
					try {
						nation.setUUID(UUID.fromString(line));
					} catch (IllegalArgumentException ee) {
						nation.setUUID(UUID.randomUUID());
					}
					universe.registerNationUUID(nation);
				}
				line = keys.get("registered");
				if (line != null) {
					try {
						nation.setRegistered(Long.valueOf(line));
					} catch (Exception ee) {
						nation.setRegistered(0);
					}
				}
				
				line = keys.get("nationSpawn");
				if (line != null) {
					tokens = line.split(",");
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
				
				line = keys.get("isPublic");
				if (line != null)
					try {
						nation.setPublic(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("isOpen");
				if (line != null)
					try {
						nation.setOpen(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					nation.setMetadata(line.trim());

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading nation file " + nation.getName() + " at line: " + line + ", in towny\\data\\nations\\" + nation.getName() + ".txt");
				e.printStackTrace();
				return false;
			} finally {
				saveNation(nation);
				nation.setOfflinePlayer();
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean loadWorld(TownyWorld world) {
		
		String line = "";
		String path = getWorldFilename(world);
		
		// create the world file if it doesn't exist
		if (!FileMgmt.checkOrCreateFile(path)) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading file " + path);
		}
		
		File fileWorld = new File(path);
		if (fileWorld.exists() && fileWorld.isFile()) {
			TownyMessaging.sendDebugMsg("Loading World: " + world.getName());
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileWorld);
				
				line = keys.get("claimable");
				if (line != null)
					try {
						world.setClaimable(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("pvp");
				if (line != null)
					try {
						world.setPVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forcepvp");
				if (line != null)
					try {
						world.setForcePVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("friendlyFire");
				if (line != null)
					try {
						world.setFriendlyFire(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forcetownmobs");
				if (line != null)
					try {
						world.setForceTownMobs(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}

				line = keys.get("wildernessmobs");
				if (line != null)
					try {
						world.setWildernessMobs(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("worldmobs");
				if (line != null)
					try {
						world.setWorldMobs(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("firespread");
				if (line != null)
					try {
						world.setFire(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forcefirespread");
				if (line != null)
					try {
						world.setForceFire(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("explosions");
				if (line != null)
					try {
						world.setExpl(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forceexplosions");
				if (line != null)
					try {
						world.setForceExpl(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("endermanprotect");
				if (line != null)
					try {
						world.setEndermanProtect(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("disableplayertrample");
				if (line != null)
					try {
						world.setDisablePlayerTrample(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("disablecreaturetrample");
				if (line != null)
					try {
						world.setDisableCreatureTrample(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("unclaimedZoneBuild");
				if (line != null)
					try {
						world.setUnclaimedZoneBuild(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneDestroy");
				if (line != null)
					try {
						world.setUnclaimedZoneDestroy(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneSwitch");
				if (line != null)
					try {
						world.setUnclaimedZoneSwitch(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneItemUse");
				if (line != null)
					try {
						world.setUnclaimedZoneItemUse(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneName");
				if (line != null)
					try {
						world.setUnclaimedZoneName(line);
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneIgnoreIds");
				if (line != null)
					try {
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								mats.add(s);
						
						world.setUnclaimedZoneIgnore(mats);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementDelete(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("plotManagementDeleteIds");
				if (line != null)
					try {
						//List<Integer> nums = new ArrayList<Integer>();
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								mats.add(s);
						
						world.setPlotManagementDeleteIds(mats);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementMayorDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementMayorDelete(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("plotManagementMayorDelete");
				if (line != null)
					try {
						List<String> materials = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									materials.add(s.toUpperCase().trim());
								} catch (NumberFormatException ignored) {
								}
						world.setPlotManagementMayorDelete(materials);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementRevert");
				if (line != null)
					try {
						world.setUsingPlotManagementRevert(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}

				line = keys.get("plotManagementIgnoreIds");
				if (line != null)
					try {
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								mats.add(s);
						
						world.setPlotManagementIgnoreIds(mats);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementWildRegen");
				if (line != null)
					try {
						world.setUsingPlotManagementWildEntityRevert(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("PlotManagementWildRegenEntities");
				if (line != null)
					try {
						List<String> entities = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									entities.add(s.trim());
								} catch (NumberFormatException ignored) {
								}
						world.setPlotManagementWildRevertEntities(entities);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementWildRegenDelay");
				if (line != null)
					try {
						world.setPlotManagementWildRevertDelay(Long.parseLong(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementWildRegenBlocks");
				if (line != null)
					try {
						world.setUsingPlotManagementWildBlockRevert(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("PlotManagementWildRegenBlocks");
				if (line != null)
					try {
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									mats.add(s.trim());
								} catch (NumberFormatException ignored) {
								}
						world.setPlotManagementWildRevertMaterials(mats);
					} catch (Exception ignored) {
					}

				line = keys.get("usingTowny");
				if (line != null)
					try {
						world.setUsingTowny(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("warAllowed");
				if (line != null)
					try {
						world.setWarAllowed(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}

				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					world.setMetadata(line.trim());
				
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading world file " + path + " at line: " + line + ", in towny\\data\\worlds\\" + world.getName() + ".txt");
				return false;
			} finally {
				saveWorld(world);
			}
			return true;
		} else {
			TownyMessaging.sendErrorMsg("Loading Error: File error while reading " + world.getName() + " at line: " + line + ", in towny\\data\\worlds\\" + world.getName() + ".txt");
			return false;
		}
	}
	
	@Override
	public boolean loadPlotGroups() {
		String line = "";
		String path;
		
		for (PlotGroup group : getAllPlotGroups()) {
			path = getPlotGroupFilename(group);
			
			File groupFile = new File(path);
			if (groupFile.exists() && groupFile.isFile()) {
				try {
					HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(groupFile);

					line = keys.get("groupName");
					if (line != null)
						group.setName(line.trim());
					
					line = keys.get("groupID");
					if (line != null)
						group.setID(UUID.fromString(line.trim()));
					
					line = keys.get("town");
					if (line != null && !line.isEmpty()) {
						Town town = universe.getTown(line.trim());
						if (town != null) {
							group.setTown(town);	
						}
						else {
							TownyMessaging.sendDebugMsg("Group file missing Town, deleting " + path);
							deletePlotGroup(group);
							TownyMessaging.sendDebugMsg("Missing file: " + path + " deleting entry in group.txt");
							continue;
						}
					}
					else {
						TownyMessaging.sendErrorMsg("Could not add to town!");
						deletePlotGroup(group);
					}
					
					line = keys.get("groupPrice");
					if (line != null && !line.isEmpty())
						group.setPrice(Double.parseDouble(line.trim()));

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg("Loading Error: Exception while reading Group file " + path + " at line: " + line);
					return false;
				}
			} else {
				TownyMessaging.sendDebugMsg("Missing file: " + path + " deleting entry in groups.txt");
			}
		}
		
		savePlotGroupList();
		
		return true;
	}
	
	@Override
	public boolean loadTownBlocks() {
		
		String line = "";
		String path;
		

		for (TownBlock townBlock : getAllTownBlocks()) {
			path = getTownBlockFilename(townBlock);
			
			File fileTownBlock = new File(path);
			if (fileTownBlock.exists() && fileTownBlock.isFile()) {

				try {
					HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileTownBlock);			

					line = keys.get("town");
					if (line != null) {
						if (line.isEmpty()) {
							TownyMessaging.sendErrorMsg("TownBlock file missing Town, deleting " + path);
							TownyUniverse.getInstance().removeTownBlock(townBlock);
							deleteTownBlock(townBlock);
							continue;
						}
						Town town = universe.getTown(line.trim());
						
						if (town == null) {
							TownyMessaging.sendErrorMsg("TownBlock file contains unregistered Town: " + line + ", deleting " + path);
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
					} else {
						// Town line is null, townblock is invalid.
						TownyMessaging.sendErrorMsg("TownBlock file missing Town, deleting " + path);
						TownyUniverse.getInstance().removeTownBlock(townBlock);
						deleteTownBlock(townBlock);
						continue;
					}

					line = keys.get("name");
					if (line != null)
						try {
							townBlock.setName(line.trim());
						} catch (Exception ignored) {
						}
					
					line = keys.get("price");
					if (line != null)
						try {
							townBlock.setPlotPrice(Double.parseDouble(line.trim()));
						} catch (Exception ignored) {
						}

					line = keys.get("resident");
					if (line != null && !line.isEmpty()) {
						Resident res = universe.getResident(line.trim());
						if (res != null) {
							townBlock.setResident(res);
						}
						else {
							TownyMessaging.sendErrorMsg("Invalid resident for townblock " + townBlock.toString() + ". Skipped setting resident for townblock...");
						}
					}
					
					line = keys.get("type");
					if (line != null)
						try {
							townBlock.setType(Integer.parseInt(line));
						} catch (Exception ignored) {
						}
					
					line = keys.get("outpost");
					if (line != null)
						try {
							townBlock.setOutpost(Boolean.parseBoolean(line));
						} catch (Exception ignored) {
						}
					
					line = keys.get("permissions");
					if ((line != null) && !line.isEmpty())
						try {
							townBlock.setPermissions(line.trim());
						} catch (Exception ignored) {
						}
					
					line = keys.get("changed");
					if (line != null)
						try {
							townBlock.setChanged(Boolean.parseBoolean(line.trim()));
						} catch (Exception ignored) {
						}
					
					line = keys.get("locked");
					if (line != null)
						try {
							townBlock.setLocked(Boolean.parseBoolean(line.trim()));
						} catch (Exception ignored) {
						}

					line = keys.get("metadata");
					if (line != null && !line.isEmpty())
						townBlock.setMetadata(line.trim());

					line = keys.get("groupID");
					UUID groupID = null;
					if (line != null && !line.isEmpty()) {
						groupID = UUID.fromString(line.trim());
					}
					
					if (groupID != null) {
						PlotGroup group = getPlotObjectGroup(townBlock.getTown().toString(), groupID);
						townBlock.setPlotObjectGroup(group);
					}

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlock file " + path + " at line: " + line);
					return false;
				}

			} else {
				TownyMessaging.sendErrorMsg("TownBlock file contains unknown error, deleting " + path);
				TownyUniverse.getInstance().removeTownBlock(townBlock);
				deleteTownBlock(townBlock);
			}
		}
		
		return true;
	}

	/*
	 * Save keys
	 */

	@Override
	public boolean savePlotGroupList() {
		List<String> list = new ArrayList<>();
		
		for (PlotGroup group : getAllPlotGroups()) {
			list.add(group.getTown().getName() + "," + group.getID() + "," + group.getName());
		}
		
		this.queryQueue.add(new FlatFileSaveTask(list, dataFolderPath + File.separator + "plotgroups.txt"));
		
		return true;
	}

	@Override
	public boolean saveWorldList() {

		List<String> list = new ArrayList<>();

		for (TownyWorld world : getWorlds()) {

			list.add(world.getName());

		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, dataFolderPath + File.separator + "worlds.txt"));

		return true;

	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveResident(Resident resident) {

		List<String> list = new ArrayList<>();

		if (resident.hasUUID()) {
			list.add("uuid=" + resident.getUUID());
		}
		// Last Online
		list.add("lastOnline=" + resident.getLastOnline());
		// Registered
		list.add("registered=" + resident.getRegistered());
		// isNPC
		list.add("isNPC=" + resident.isNPC());
		// isJailed
		list.add("isJailed=" + resident.isJailed());
		// JailSpawn
		list.add("JailSpawn=" + resident.getJailSpawn());
		// JailDays
		list.add("JailDays=" + resident.getJailDays());
		// JailTown
		list.add("JailTown=" + resident.getJailTown());

		// title
		list.add("title=" + resident.getTitle());
		// surname
		list.add("surname=" + resident.getSurname());

		if (resident.hasTown()) {
			try {
				list.add("town=" + resident.getTown().getName());
			} catch (NotRegisteredException ignored) {
			}
			list.add("town-ranks=" + StringMgmt.join(resident.getTownRanks(), ","));
			list.add("nation-ranks=" + StringMgmt.join(resident.getNationRanks(), ","));
		}

		// Friends
		list.add("friends=" + StringMgmt.join(resident.getFriends(), ","));
		list.add("");

		// Plot Protection
		list.add("protectionStatus=" + resident.getPermissions().toString());

		// Metadata
		list.add("metadata=" + serializeMetadata(resident));
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getResidentFilename(resident)));

		return true;

	}

	@Override
	public boolean saveTown(Town town) {

		List<String> list = new ArrayList<>();

		// Name
		list.add("name=" + town.getName());
		// Mayor
		if (town.hasMayor())
			list.add("mayor=" + town.getMayor().getName());
		// Nation
		if (town.hasNation())
			try {
				list.add("nation=" + town.getNation().getName());
			} catch (NotRegisteredException ignored) {
			}

		// Assistants
		list.add("assistants=" + StringMgmt.join(town.getRank("assistant"), ","));

		list.add(newLine);
		// Town Board
		list.add("townBoard=" + town.getBoard());
		// tag
		list.add("tag=" + town.getTag());
		// Town Protection
		list.add("protectionStatus=" + town.getPermissions().toString());
		// Bonus Blocks
		list.add("bonusBlocks=" + town.getBonusBlocks());
		// Purchased Blocks
		list.add("purchasedBlocks=" + town.getPurchasedBlocks());
		// Taxpercent
		list.add("taxpercent=" + town.isTaxPercentage());
		// Taxes
		list.add("taxes=" + town.getTaxes());
		// Plot Price
		list.add("plotPrice=" + town.getPlotPrice());
		// Plot Tax
		list.add("plotTax=" + town.getPlotTax());
		// Commercial Plot Price
		list.add("commercialPlotPrice=" + town.getCommercialPlotPrice());
		// Commercial Tax
		list.add("commercialPlotTax=" + town.getCommercialPlotTax());
		// Embassy Plot Price
		list.add("embassyPlotPrice=" + town.getEmbassyPlotPrice());
		// Embassy Tax
		list.add("embassyPlotTax=" + town.getEmbassyPlotTax());
		// Town Spawn Cost
		list.add("spawnCost=" + town.getSpawnCost());
		// Upkeep
		list.add("hasUpkeep=" + town.hasUpkeep());
		// Open
		list.add("open=" + town.isOpen());
		// PVP
		list.add("adminDisabledPvP=" + town.isAdminDisabledPVP());
		list.add("adminEnabledPvP=" + town.isAdminEnabledPVP());
		// Public
		list.add("public=" + town.isPublic());
		// Conquered towns setting + date
		list.add("conquered=" + town.isConquered());
		list.add("conqueredDays " + town.getConqueredDays());
		if (town.hasValidUUID()){
			list.add("uuid=" + town.getUUID());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
        list.add("registered=" + town.getRegistered());
        
        // Home Block
		if (town.hasHomeBlock())
			try {
				list.add("homeBlock=" + town.getHomeBlock().getWorld().getName() + "," + town.getHomeBlock().getX() + "," + town.getHomeBlock().getZ());
			} catch (TownyException ignored) {
			}

		// Spawn
		if (town.hasSpawn())
			try {
				list.add("spawn=" + town.getSpawn().getWorld().getName() + "," + town.getSpawn().getX() + "," + town.getSpawn().getY() + "," + town.getSpawn().getZ() + "," + town.getSpawn().getPitch() + "," + town.getSpawn().getYaw());
			} catch (TownyException ignored) {
			}

		// Outpost Spawns
		StringBuilder outpostArray = new StringBuilder("outpostspawns=");
		if (town.hasOutpostSpawn())
			for (Location spawn : new ArrayList<>(town.getAllOutpostSpawns())) {
				outpostArray.append(spawn.getWorld().getName()).append(",").append(spawn.getX()).append(",").append(spawn.getY()).append(",").append(spawn.getZ()).append(",").append(spawn.getPitch()).append(",").append(spawn.getYaw()).append(";");
			}
		list.add(outpostArray.toString());

		// Jail Spawns
		StringBuilder jailArray = new StringBuilder("jailspawns=");
		if (town.hasJailSpawn())
			for (Location spawn : new ArrayList<>(town.getAllJailSpawns())) {
				jailArray.append(spawn.getWorld().getName()).append(",").append(spawn.getX()).append(",").append(spawn.getY()).append(",").append(spawn.getZ()).append(",").append(spawn.getPitch()).append(",").append(spawn.getYaw()).append(";");
			}
		list.add(jailArray.toString());

		// Outlaws
		list.add("outlaws=" + StringMgmt.join(town.getOutlaws(), ","));

		// Metadata
		list.add("metadata=" + serializeMetadata(town));
		
		list.add("ruined=" + town.isRuined());
		list.add("ruinedTime=" + town.getRuinedTime());
		// Peaceful
		list.add("neutral=" + town.isNeutral());
		
		// Debt balance
		list.add("debtBalance=" + town.getDebtBalance());

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getTownFilename(town)));

		return true;

	}
	
	@Override
	public boolean savePlotGroup(PlotGroup group) {
		
		List<String> list = new ArrayList<>();
		
		// Group ID
		list.add("groupID=" + group.getID().toString());
		
		// Group Name
		list.add("groupName=" + group.getName());
		
		// Group Price
		list.add("groupPrice=" + group.getPrice());
		
		// Town
		list.add("town=" + group.getTown().toString());
		
		// Save file
		this.queryQueue.add(new FlatFileSaveTask(list, getPlotGroupFilename(group)));
		
		return true;
	}

	@Override
	public boolean saveNation(Nation nation) {

		List<String> list = new ArrayList<>();

		if (nation.hasCapital())
			list.add("capital=" + nation.getCapital().getName());

		list.add("nationBoard=" + nation.getBoard());

		list.add("mapColorHexCode=" + nation.getMapColorHexCode());

		if (nation.hasTag())
			list.add("tag=" + nation.getTag());

		list.add("allies=" + StringMgmt.join(nation.getAllies(), ","));

		list.add("enemies=" + StringMgmt.join(nation.getEnemies(), ","));

		// Taxes
		list.add("taxes=" + nation.getTaxes());
		// Nation Spawn Cost
		list.add("spawnCost=" + nation.getSpawnCost());
		// Peaceful
		list.add("neutral=" + nation.isNeutral());
		if (nation.hasValidUUID()){
			list.add("uuid=" + nation.getUUID());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
        list.add("registered=" + nation.getRegistered());
        
        // Spawn
		if (nation.hasSpawn()) {
			try {
				list.add("nationSpawn=" + nation.getSpawn().getWorld().getName() + "," + nation.getSpawn().getX() + "," + nation.getSpawn().getY() + "," + nation.getSpawn().getZ() + "," + nation.getSpawn().getPitch() + "," + nation.getSpawn().getYaw());
			} catch (TownyException ignored) { }
		}

		list.add("isPublic=" + nation.isPublic());
		
		list.add("isOpen=" + nation.isOpen());

		// Metadata
		list.add("metadata=" + serializeMetadata(nation));
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getNationFilename(nation)));

		return true;

	}

	@Override
	public boolean saveWorld(TownyWorld world) {

		List<String> list = new ArrayList<>();

		// PvP
		list.add("pvp=" + world.isPVP());
		// Force PvP
		list.add("forcepvp=" + world.isForcePVP());
		// FriendlyFire 
		list.add("friendlyFire=" + world.isFriendlyFireEnabled());		
		// Claimable
		list.add("# Can players found towns and claim plots in this world?");
		list.add("claimable=" + world.isClaimable());
		// has monster spawns
		list.add("worldmobs=" + world.hasWorldMobs());
		// has wilderness spawns
		list.add("wildernessmobs=" + world.hasWildernessMobs());
		// force town mob spawns
		list.add("forcetownmobs=" + world.isForceTownMobs());
		// has firespread enabled
		list.add("firespread=" + world.isFire());
		list.add("forcefirespread=" + world.isForceFire());
		// has explosions enabled
		list.add("explosions=" + world.isExpl());
		list.add("forceexplosions=" + world.isForceExpl());
		// Enderman block protection
		list.add("endermanprotect=" + world.isEndermanProtect());
		// PlayerTrample
		list.add("disableplayertrample=" + world.isDisablePlayerTrample());
		// CreatureTrample
		list.add("disablecreaturetrample=" + world.isDisableCreatureTrample());

		// Unclaimed
		list.add("");
		list.add("# Unclaimed Zone settings.");

		// Unclaimed Zone Build
		if (world.getUnclaimedZoneBuild() != null)
			list.add("unclaimedZoneBuild=" + world.getUnclaimedZoneBuild());
		// Unclaimed Zone Destroy
		if (world.getUnclaimedZoneDestroy() != null)
			list.add("unclaimedZoneDestroy=" + world.getUnclaimedZoneDestroy());
		// Unclaimed Zone Switch
		if (world.getUnclaimedZoneSwitch() != null)
			list.add("unclaimedZoneSwitch=" + world.getUnclaimedZoneSwitch());
		// Unclaimed Zone Item Use
		if (world.getUnclaimedZoneItemUse() != null)
			list.add("unclaimedZoneItemUse=" + world.getUnclaimedZoneItemUse());
		// Unclaimed Zone Name
		if (world.getUnclaimedZoneName() != null)
			list.add("unclaimedZoneName=" + world.getUnclaimedZoneName());

		list.add("");
		list.add("# The following are blocks that will bypass the above build, destroy, switch and itemuse settings.");

		// Unclaimed Zone Ignore Ids
		if (world.getUnclaimedZoneIgnoreMaterials() != null)
			list.add("unclaimedZoneIgnoreIds=" + StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), ","));

		// PlotManagement Delete
		list.add("");
		list.add("# The following settings control what blocks are deleted upon a townblock being unclaimed");
		// Using PlotManagement Delete
		list.add("usingPlotManagementDelete=" + world.isUsingPlotManagementDelete());
		// Plot Management Delete Ids
		if (world.getPlotManagementDeleteIds() != null)
			list.add("plotManagementDeleteIds=" + StringMgmt.join(world.getPlotManagementDeleteIds(), ","));

		// PlotManagement
		list.add("");
		list.add("# The following settings control what blocks are deleted upon a mayor issuing a '/plot clear' command");
		// Using PlotManagement Mayor Delete
		list.add("usingPlotManagementMayorDelete=" + world.isUsingPlotManagementMayorDelete());
		// Plot Management Mayor Delete
		if (world.getPlotManagementMayorDelete() != null)
			list.add("plotManagementMayorDelete=" + StringMgmt.join(world.getPlotManagementMayorDelete(), ","));

		// PlotManagement Revert
		list.add("");
		list.add("# If enabled when a town claims a townblock a snapshot will be taken at the time it is claimed.");
		list.add("# When the townblock is unclaimed its blocks will begin to revert to the original snapshot.");
		// Using PlotManagement Revert
		list.add("usingPlotManagementRevert=" + world.isUsingPlotManagementRevert());

		list.add("# Any block Id's listed here will not be respawned. Instead it will revert to air. This list also world on the WildRegen settings below.");
		// Plot Management Ignore Ids
		if (world.getPlotManagementIgnoreIds() != null)
			list.add("plotManagementIgnoreIds=" + StringMgmt.join(world.getPlotManagementIgnoreIds(), ","));

		// PlotManagement Wild Regen
		list.add("");
		list.add("# The following settings control which entities/blocks' explosions are reverted in the wilderness.");
		list.add("# If enabled any damage caused by entity explosions will repair itself.");
		// Using PlotManagement Wild Regen
		list.add("usingPlotManagementWildRegen=" + world.isUsingPlotManagementWildEntityRevert());

		list.add("# The list of entities whose explosions would be reverted.");
		// Wilderness Explosion Protection entities
		if (world.getPlotManagementWildRevertEntities() != null)
			list.add("PlotManagementWildRegenEntities=" + StringMgmt.join(world.getPlotManagementWildRevertEntities(), ","));

		list.add("# If enabled any damage caused by block explosions will repair itself.");
		// Using PlotManagement Wild Block Regen
		list.add("usingPlotManagementWildRegenBlocks=" + world.isUsingPlotManagementWildBlockRevert());

		list.add("# The list of entities whose explosions would be reverted.");
		// Wilderness Explosion Protection blocks
		if (world.getPlotManagementWildRevertBlocks() != null)
			list.add("PlotManagementWildRegenBlocks=" + StringMgmt.join(world.getPlotManagementWildRevertBlocks(), ","));

		list.add("# The delay after which the explosion reverts will begin.");
		// Using PlotManagement Wild Regen Delay
		list.add("usingPlotManagementWildRegenDelay=" + world.getPlotManagementWildRevertDelay());

		
		// Using Towny
		list.add("");
		list.add("# This setting is used to enable or disable Towny in this world.");
		// Using Towny
		list.add("usingTowny=" + world.isUsingTowny());

		// is War allowed
		list.add("");
		list.add("# This setting is used to enable or disable Event war in this world.");
		list.add("warAllowed=" + world.isWarAllowed());

		// Metadata
		list.add("");
		list.add("metadata=" + serializeMetadata(world));
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getWorldFilename(world)));

		return true;

	}

	@Override
	public boolean saveTownBlock(TownBlock townBlock) {

		FileMgmt.checkOrCreateFolder(dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName());

		List<String> list = new ArrayList<>();

		// name
		list.add("name=" + townBlock.getName());

		// price
		list.add("price=" + townBlock.getPlotPrice());

		// town
		try {
			list.add("town=" + townBlock.getTown().getName());
		} catch (NotRegisteredException ignored) {
		}

		// resident
		if (townBlock.hasResident()) {

			try {
				list.add("resident=" + townBlock.getResident().getName());
			} catch (NotRegisteredException ignored) {
			}
		}

		// type
		list.add("type=" + townBlock.getType().getId());

		// outpost
		list.add("outpost=" + townBlock.isOutpost());

		/*
		 * Only include a permissions line IF the plot perms are custom.
		 */
		if (townBlock.isChanged()) {
			// permissions
			list.add("permissions=" + townBlock.getPermissions().toString());
		}

		// Have permissions been manually changed
		list.add("changed=" + townBlock.isChanged());

		list.add("locked=" + townBlock.isLocked());
		
		// Metadata
		list.add("metadata=" + serializeMetadata(townBlock));
		
		// Group ID
		StringBuilder groupID = new StringBuilder();
		StringBuilder groupName = new StringBuilder();
		if (townBlock.hasPlotObjectGroup()) {
			groupID.append(townBlock.getPlotObjectGroup().getID());
			groupName.append(townBlock.getPlotObjectGroup().getName());
		}
		
		list.add("groupID=" + groupID.toString());
		
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getTownBlockFilename(townBlock)));

		return true;

	}

	private String serializeMetadata(TownyObject obj) {
		if (!obj.hasMeta())
			return "";

		StringJoiner serializer = new StringJoiner(";");
		for (CustomDataField<?> cdf : obj.getMetadata()) {
			serializer.add(cdf.toString());
		}

		return serializer.toString();
	}

	/*
	 * Delete objects
	 */
	
	@Override
	public void deleteResident(Resident resident) {
		File file = new File(getResidentFilename(resident));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteTown(Town town) {
		File file = new File(getTownFilename(town));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteNation(Nation nation) {
		File file = new File(getNationFilename(nation));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteWorld(TownyWorld world) {
		File file = new File(getWorldFilename(world));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {

		File file = new File(getTownBlockFilename(townBlock));
		
		queryQueue.add(() -> {
			if (file.exists()) {
				// TownBlocks can end up being deleted because they do not contain valid towns.
				// This will move a deleted townblock to either: 
				// towny\townblocks\worldname\deleted\townname folder, or the
				// towny\townblocks\worldname\deleted\ folder if there is not valid townname.
				String name = null;
				try {
					name = townBlock.getTown().getName();
				} catch (NotRegisteredException ignored) {
				}
				if (name != null)
					FileMgmt.moveTownBlockFile(file, "deleted", name);
				else
					FileMgmt.moveTownBlockFile(file, "deleted", "");
			}
		});
	}
	
	@Override
	public void deletePlotGroup(PlotGroup group) {
    	File file = new File(getPlotGroupFilename(group));
    	queryQueue.add(new DeleteFileTask(file, false));
	}
}
