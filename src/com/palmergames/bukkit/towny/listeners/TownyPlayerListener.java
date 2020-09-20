package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.PlayerEnterTownEvent;
import com.palmergames.bukkit.towny.event.PlayerLeaveTownEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.common.WarZoneConfig;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.citizensnpcs.api.CitizensAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle events for all Player related events
 * Players deaths are handled both here and in the TownyEntityMonitorListener
 * 
 * @author Shade/ElgarL
 * 
 */
public class TownyPlayerListener implements Listener {

	private final Towny plugin;

	public TownyPlayerListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event) {

		Player player = event.getPlayer();

		if (plugin.isError()) {
			player.sendMessage(Colors.Rose + "[Towny Error] Locked in Safe mode!");
			return;
		}

		if (!player.isOnline()) {
			return;
		}

		// Test and kick any players with invalid names.
		if (player.getName().contains(" ")) {
			player.kickPlayer("Invalid name!");
			return;
		}

		// Perform login code in it's own thread to update Towny data.
		if (BukkitTools.scheduleSyncDelayedTask(new OnPlayerLogin(Towny.getPlugin(), player), 0L) == -1) {
			TownyMessaging.sendErrorMsg("Could not schedule OnLogin.");
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {

		if (plugin.isError()) {
			return;
		}
		
		Player quittingPlayer = event.getPlayer();
		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();
		try {
			Resident resident = dataSource.getResident(quittingPlayer);
			resident.setLastOnline(System.currentTimeMillis());
			resident.clearModes();
			dataSource.saveResident(resident);
		} catch (NotRegisteredException ignored) {
		}

		// Remove from teleport queue (if exists)
		try {
			if (TownyTimerHandler.isTeleportWarmupRunning()) {
				TownyAPI.getInstance().abortTeleportRequest(dataSource.getResident(quittingPlayer));
			}
		} catch (NotRegisteredException ignored) {
		}

		plugin.deleteCache(quittingPlayer);
		TownyPerms.removeAttachment(quittingPlayer.getName());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (plugin.isError()) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (!TownySettings.isTownRespawning()) {
			return;
		}
		
		// If respawn anchors have higher precedence than town spawns, use them instead.
		if (Towny.is116Plus() && event.isAnchorSpawn() && TownySettings.isRespawnAnchorHigherPrecedence()) {
			return;
		}
		
		Location respawn;
		respawn = TownyAPI.getInstance().getTownSpawnLocation(player);
		if (respawn == null) {
			// Town has not set respawn location. Using default.
			return;
		}
		// Check if only respawning in the same world as the town's spawn.
		if (TownySettings.isTownRespawningInOtherWorlds() && !player.getWorld().equals(respawn.getWorld()))
			return;
		
		// Bed spawn or town.
		if (TownySettings.getBedUse() && (player.getBedSpawnLocation() != null)) {
			event.setRespawnLocation(player.getBedSpawnLocation());
		} else {
			event.setRespawnLocation(respawn);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJailRespawn(PlayerRespawnEvent event) {

		if (plugin.isError()) {
			return;
		}
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (!TownySettings.isTownRespawning())
			return;
	
		try {
			Location respawn = null;			
			Resident resident = townyUniverse.getDataSource().getResident(event.getPlayer());
			// If player is jailed send them to their jailspawn.
			if (resident.isJailed()) {
				Town respawnTown = townyUniverse.getDataSource().getTown(resident.getJailTown());
				respawn = respawnTown.getJailSpawn(resident.getJailSpawn());
				event.setRespawnLocation(respawn);
			}
		} catch (TownyException e) {
			// Town has not set respawn location. Using default.
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		// Test against the item in hand as we need to test the bucket contents
		// we are trying to empty.
		event.setCancelled(onPlayerInteract(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace()), event.getPlayer().getInventory().getItemInMainHand()));

		// Test on the resulting empty bucket to see if we have permission to
		// empty a bucket.
		if (!event.isCancelled())
			event.setCancelled(onPlayerInteract(event.getPlayer(), event.getBlockClicked(), event.getItemStack()));

	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		// test against the bucket we will finish up with to see if we are
		// allowed to fill this item.
		event.setCancelled(onPlayerInteract(event.getPlayer(), event.getBlockClicked(), event.getItemStack()));

	}

	
	/*
	* PlayerInteractEvent 
	* 
	*  Used to stop trampling of crops,
	*  admin infotool,
	*  item use check,
	*  switch use check
	*/
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		if (event.hasItem()) {

			/*
			 * Info Tool
			 */
			if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.getMaterial(TownySettings.getTool())) {

				if (TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)) {
					if (event.getClickedBlock() != null) {

						block = event.getClickedBlock();
						
						if (Tag.SIGNS.isTagged(block.getType())) {
							BlockFace facing = null;
							if (block.getBlockData() instanceof Sign) {
								org.bukkit.block.data.type.Sign sign = (org.bukkit.block.data.type.Sign) block.getBlockData();
								facing = sign.getRotation();
							}
							if (block.getBlockData() instanceof WallSign)  { 
								org.bukkit.block.data.type.WallSign sign = (org.bukkit.block.data.type.WallSign) block.getBlockData();
								facing = sign.getFacing();	
							}
							TownyMessaging.sendMessage(player, Arrays.asList(
									ChatTools.formatTitle("Sign Info"),
									ChatTools.formatCommand("", "Sign Type", "", block.getType().name()),
									ChatTools.formatCommand("", "Facing", "", facing.toString())
									));
						} else if (Tag.DOORS.isTagged(block.getType())) {
							org.bukkit.block.data.type.Door door = (org.bukkit.block.data.type.Door) block.getBlockData();
							TownyMessaging.sendMessage(player, Arrays.asList(
									ChatTools.formatTitle("Door Info"),
									ChatTools.formatCommand("", "Door Type", "", block.getType().name()),
									ChatTools.formatCommand("", "hinged on ", "", String.valueOf(door.getHinge())),
									ChatTools.formatCommand("", "isOpen", "", String.valueOf(door.isOpen())),
									ChatTools.formatCommand("", "getFacing", "", door.getFacing().name())
									));
						} else {
							TownyMessaging.sendMessage(player, Arrays.asList(
									ChatTools.formatTitle("Block Info"),
									ChatTools.formatCommand("", "Material", "", block.getType().name()),								      
									ChatTools.formatCommand("", "MaterialData", "", block.getBlockData().getAsString())
									));
						}
						event.setUseInteractedBlock(Event.Result.DENY);
						event.setCancelled(true);
					}
				}

			}
			if (TownySettings.isItemUseMaterial(event.getItem().getType().name())) {
				TownyMessaging.sendDebugMsg("ItemUse Material found: " + event.getItem().getType().name());
				event.setCancelled(onPlayerInteract(player, event.getClickedBlock(), event.getItem()));
			}
		}
		if (!event.useItemInHand().equals(Event.Result.DENY))
			if (event.getClickedBlock() != null) {
				if (TownySettings.isSwitchMaterial(event.getClickedBlock().getType().name()) || event.getAction() == Action.PHYSICAL) {
					onPlayerSwitchEvent(event, null);
				}
			}

	}

	
	/*
	* PlayerInteractAtEntity event
	* 
	* Handles protection of Armor Stands,
	* Admin infotool for entities.
	* 
	*/	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (event.getRightClicked() != null) {

			if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
				return;

			Player player = event.getPlayer();
			boolean bBuild = true;
			Material block = null;

			/*
			 * Protect specific entity interactions.
			 */
			switch (event.getRightClicked().getType()) {

			case ARMOR_STAND:
				
				TownyMessaging.sendDebugMsg("ArmorStand Right Clicked");
				block = Material.ARMOR_STAND;
				// Get permissions (updates if none exist)
				bBuild = PlayerCacheUtil.getCachePermission(player, event.getRightClicked().getLocation(), block, TownyPermission.ActionType.DESTROY);
				break;

			case ITEM_FRAME:
				
				TownyMessaging.sendDebugMsg("Item_Frame Right Clicked");
				block = Material.ITEM_FRAME;
				// Get permissions (updates if none exist)
				bBuild = PlayerCacheUtil.getCachePermission(player, event.getRightClicked().getLocation(), block, TownyPermission.ActionType.SWITCH);
				break;
				
			case LEASH_HITCH:

				TownyMessaging.sendDebugMsg("Leash Hitch Right Clicked");
				block = Material.LEAD;
				// Get permissions (updates if none exist)
				bBuild = PlayerCacheUtil.getCachePermission(player, event.getRightClicked().getLocation(), block, TownyPermission.ActionType.DESTROY);
				break;				
			
			default:
				break;

			}

			if (block != null) {

				// Allow the removal if we are permitted
				if (bBuild)
					return;

				event.setCancelled(true);

				/*
				 * Fetch the players cache
				 */
				PlayerCache cache = plugin.getCache(player);

				if (cache.hasBlockErrMsg())
					TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

				return;
			}

			/*
			 * Item_use protection.
			 */
			if (event.getPlayer().getInventory().getItemInMainHand() != null) {

				if (TownySettings.isItemUseMaterial(event.getPlayer().getInventory().getItemInMainHand().getType().name())) {
					event.setCancelled(onPlayerInteract(event.getPlayer(), null, event.getPlayer().getInventory().getItemInMainHand()));
				}
			}
		}
	}
	
	/*
	* PlayerInteractEntity event
	* 
	* Handles right clicking of entities: Item Frames, Paintings, Minecarts,
	* Admin infotool for entities.
	*/
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (event.getRightClicked() != null) {

			TownyWorld World = null;

			try {
				World = TownyUniverse.getInstance().getDataSource().getWorld(event.getPlayer().getWorld().getName());
				if (!World.isUsingTowny())
					return;

			} catch (NotRegisteredException e) {
				// World not registered with Towny.
				e.printStackTrace();
				return;
			}

			Player player = event.getPlayer();
			Material block = null;
			ActionType actionType = ActionType.SWITCH;
			
			switch (event.getRightClicked().getType()) {
				case ITEM_FRAME:
					block = Material.ITEM_FRAME;
					actionType = ActionType.DESTROY;
					break;
					
				case PAINTING:
					block = Material.PAINTING;
					actionType = ActionType.DESTROY;
					break;
					
				case LEASH_HITCH:
					block = Material.LEAD;
					actionType = ActionType.DESTROY;
					break;
					
				case MINECART:
				case MINECART_MOB_SPAWNER:
					block = Material.MINECART;
					break;
					
				case MINECART_CHEST:
					block = Material.CHEST_MINECART;
					break;
					
				case MINECART_FURNACE:
					block = Material.FURNACE_MINECART;
					break;
				
				case MINECART_COMMAND:
					block = Material.COMMAND_BLOCK_MINECART;
					break;
					
				case MINECART_HOPPER:
					block = Material.HOPPER_MINECART;
					break;
					
				case MINECART_TNT:
					block = Material.TNT_MINECART;
					break;
			}
			
			if (block != null && TownySettings.isSwitchMaterial(block.name())) {
				// Check if the player has valid permission for interacting with the entity based on the action type.
				if (!PlayerCacheUtil.getCachePermission(player, event.getRightClicked().getLocation(), block, actionType)) {
					event.setCancelled(true); // Cancel the event
					/*
					 * Fetch the players cache
					 */
					PlayerCache cache = plugin.getCache(player);

					if (cache.hasBlockErrMsg())
						TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());
				}
				
				return;
			}

			/*
			 * Item_use protection.
			 */
			if (event.getPlayer().getInventory().getItemInMainHand() != null) {

				/*
				 * Info Tool
				 */
				if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.getMaterial(TownySettings.getTool())) {
					if (event.getHand().equals(EquipmentSlot.OFF_HAND))
						return;

					Entity entity = event.getRightClicked();

					TownyMessaging.sendMessage(player, Arrays.asList(
							ChatTools.formatTitle("Entity Info"),
							ChatTools.formatCommand("", "Entity Class", "", entity.getType().getEntityClass().getSimpleName())
							));

					event.setCancelled(true);
				}

				if (TownySettings.isItemUseMaterial(event.getPlayer().getInventory().getItemInMainHand().getType().name())) {
					event.setCancelled(onPlayerInteract(event.getPlayer(), null, event.getPlayer().getInventory().getItemInMainHand()));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		/*
		 * Abort if we havn't really moved
		 */
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
			return;
		}

		Player player = event.getPlayer();
		Location to = event.getTo();
		Location from;
		PlayerCache cache = plugin.getCache(player);
		Resident resident = null;
		try {
			resident = townyUniverse.getDataSource().getResident(player);
		} catch (NotRegisteredException ignored) {
		}
		
		if (resident != null
				&& TownyTimerHandler.isTeleportWarmupRunning()				 
				&& TownySettings.getTeleportWarmupTime() > 0 
				&& TownySettings.isMovementCancellingSpawnWarmup() 
				&& !townyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_ADMIN.getNode()) 
				&& resident.getTeleportRequestTime() > 0) {
			TeleportWarmupTimerTask.abortTeleportRequest(resident);
			TownyMessaging.sendMsg(resident, ChatColor.RED + Translation.of("msg_err_teleport_cancelled"));
		}

		try {
			from = cache.getLastLocation();
		} catch (NullPointerException e) {
			from = event.getFrom();
		}
		
		if (WorldCoord.cellChanged(from, to)) {
			try {
				TownyWorld fromWorld = townyUniverse.getDataSource().getWorld(from.getWorld().getName());
				WorldCoord fromCoord = new WorldCoord(fromWorld.getName(), Coord.parseCoord(from));
				TownyWorld toWorld = townyUniverse.getDataSource().getWorld(to.getWorld().getName());
				WorldCoord toCoord = new WorldCoord(toWorld.getName(), Coord.parseCoord(to));
				
				onPlayerMoveChunk(player, fromCoord, toCoord, from, to, event);
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
		}

		// Update the cached players current location
		cache.setLastLocation(to);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {

		if (plugin.isError()) {
			// Citizens stores their NPCs at the world spawn and when players load chunks the NPC is teleported there. 
			// Towny was preventing them being teleported and causing NPCs to be at a world spawn, even after the Safe Mode was cleaned up. 
			if (plugin.isCitizens2() && CitizensAPI.getNPCRegistry().isNPC(event.getPlayer()))
				return;
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		// Cancel teleport if Jailed by Towny.
		try {
			if (TownyUniverse.getInstance().getDataSource().getResident(player).isJailed()) {
				if ((event.getCause() == TeleportCause.COMMAND)) {
					TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_jailed_players_no_teleport"));
					event.setCancelled(true);
					return;
				}
				if (event.getCause() == TeleportCause.PLUGIN) 
					return;
				if ((event.getCause() != TeleportCause.ENDER_PEARL) || (!TownySettings.JailAllowsEnderPearls())) {
					TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_jailed_players_no_teleport"));
					event.setCancelled(true);
				}
			}
		} catch (NotRegisteredException ignored) {
			// Not a valid resident, probably an NPC from Citizens.
		}
		

		/*
		 * Test to see if CHORUS_FRUIT is in the item_use list.
		 */
		if (event.getCause() == TeleportCause.CHORUS_FRUIT)
			if (TownySettings.isItemUseMaterial(Material.CHORUS_FRUIT.name()))
				if (onPlayerInteract(event.getPlayer(), event.getTo().getBlock(), new ItemStack(Material.CHORUS_FRUIT))) {
					event.setCancelled(true);					
					return;
				}	
			
		/*
		 * Test to see if Ender pearls are disabled.
		 */		
		if (event.getCause() == TeleportCause.ENDER_PEARL)
			if (TownySettings.isItemUseMaterial(Material.ENDER_PEARL.name()))
				if (onPlayerInteract(event.getPlayer(), event.getTo().getBlock(), new ItemStack(Material.ENDER_PEARL))) {
					event.setCancelled(true);
					TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_ender_pearls_disabled"));
					return;
				}
		
		onPlayerMove(event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) { // has changed worlds
		if (event.getPlayer().isOnline())
			TownyPerms.assignPermissions(null, event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {

		if (!TownyAPI.getInstance().isTownyWorld(event.getBed().getWorld()))
			return;
		
		if (!TownySettings.getBedUse())
			return;

		boolean isOwner = false;
		boolean isInnPlot = false;

		try {
			
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(event.getPlayer());
			
			WorldCoord worldCoord = new WorldCoord(event.getPlayer().getWorld().getName(), Coord.parseCoord(event.getBed().getLocation()));

			TownBlock townblock = worldCoord.getTownBlock();
			
			isOwner = townblock.isOwner(resident);

			isInnPlot = townblock.getType() == TownBlockType.INN;			
			
			if (resident.hasNation() && townblock.getTown().hasNation()) {
				
				Nation residentNation = resident.getTown().getNation();
				
				Nation townblockNation = townblock.getTown().getNation();			
				
				if (townblockNation.hasEnemy(residentNation)) {
					event.setCancelled(true);
					TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_no_sleep_in_enemy_inn"));
					return;
				}
			}
			
		} catch (NotRegisteredException e) {
			// Wilderness as it error'd getting a townblock.
		}
		
		if (!isOwner && !isInnPlot) {

			event.setCancelled(true);
			TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_cant_use_bed"));

		}
		
	}

	/*
	*  ItemUse protection handling
	*/
	public boolean onPlayerInteract(Player player, Block block, ItemStack item) {

		boolean cancelState = false;
		WorldCoord worldCoord;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {
			String worldName = player.getWorld().getName();

			if (block != null)
				worldCoord = new WorldCoord(worldName, Coord.parseCoord(block));
			else
				worldCoord = new WorldCoord(worldName, Coord.parseCoord(player));

			// Get itemUse permissions (updates if none exist)
			boolean bItemUse;

			if (block != null)
				bItemUse = PlayerCacheUtil.getCachePermission(player, block.getLocation(), item.getType(), TownyPermission.ActionType.ITEM_USE);
			else
				bItemUse = PlayerCacheUtil.getCachePermission(player, player.getLocation(), item.getType(), TownyPermission.ActionType.ITEM_USE);

			boolean wildOverride = townyUniverse.getPermissionSource().hasWildOverride(worldCoord.getTownyWorld(), player, item.getType(), TownyPermission.ActionType.ITEM_USE);

			PlayerCache cache = plugin.getCache(player);
			// cache.updateCoord(worldCoord);
			try {

				TownBlockStatus status = cache.getStatus();
				if (status == TownBlockStatus.UNCLAIMED_ZONE && wildOverride)
					return cancelState;

				// Allow item_use if we have an override
				if (((status == TownBlockStatus.TOWN_RESIDENT) && (townyUniverse.getPermissionSource().hasOwnTownOverride(player, item.getType(), TownyPermission.ActionType.ITEM_USE)))
						|| (((status == TownBlockStatus.OUTSIDER) || (status == TownBlockStatus.TOWN_ALLY) || (status == TownBlockStatus.ENEMY)) 
						&& (townyUniverse.getPermissionSource().hasAllTownOverride(player, item.getType(), TownyPermission.ActionType.ITEM_USE))))
					return cancelState;
				
				// Allow item_use for Event War if isAllowingItemUseInWarZone is true, FlagWar also handled here
				if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
						|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
					if (!WarZoneConfig.isAllowingItemUseInWarZone()) {
						cancelState = true;
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_use_item"));
					}
					return cancelState;
				}

				// Non-Override Wilderness & Non-Override Claimed Land Handled here.
				if (((status == TownBlockStatus.UNCLAIMED_ZONE) && (!wildOverride)) // Wilderness 
						|| ((!bItemUse) && (status != TownBlockStatus.UNCLAIMED_ZONE))) { // Claimed Land
					cancelState = true;
				}

				if ((cache.hasBlockErrMsg())) 
					TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

			} catch (NullPointerException e) {
				System.out.print("NPE generated!");
				System.out.print("Player: " + player.getName());
				System.out.print("Item: " + item.getType().name());
			}

		} catch (NotRegisteredException e1) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_configured"));
			cancelState = true;
			return cancelState;
		}

		return cancelState;

	}

	/*
	*  Switch protection handling
	*/	
	public void onPlayerSwitchEvent(PlayerInteractEvent event, String errMsg) {

		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		
		event.setCancelled(onPlayerSwitchEvent(player, block, errMsg));

	}

	public boolean onPlayerSwitchEvent(Player player, Block block, String errMsg) {

		if (!TownySettings.isSwitchMaterial(block.getType().name()))
			return false;

		// Get switch permissions (updates if none exist)
		boolean bSwitch = PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.SWITCH);

		// Allow switch if we are permitted
		if (bSwitch)
			return false;

		/*
		 * Fetch the players cache
		 */
		PlayerCache cache = plugin.getCache(player);
		TownBlockStatus status = cache.getStatus();

		/*
		 * Flag war & now Event War
		 */
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingSwitchesInWarZone()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_use_switches"));
				return true;
			}
			return false;
		} else {
			/*
			 * display any error recorded for this plot
			 */
			if (cache.hasBlockErrMsg())
				TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());
			return true;
		}

	}
	
	/*
	 * PlayerFishEvent
	 * 
	 * Prevents players from fishing for entities in protected regions.
	 * - Armorstands, animals, players, any entity affected by rods.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerFishEvent(PlayerFishEvent event) {
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		if (event.getState().equals(PlayerFishEvent.State.CAUGHT_ENTITY)) {
			Player player = event.getPlayer();
			Entity caught = event.getCaught();
			boolean test = false;
			
			// Caught players are tested for pvp at the location of the catch.
			if (caught.getType().equals(EntityType.PLAYER)) {
				TownyWorld townyWorld = TownyUniverse.getInstance().getDataSource().getTownWorld(caught.getWorld().getName());
				TownBlock tb = null;
				try {
					tb = townyWorld.getTownBlock(Coord.parseCoord(event.getCaught()));
				} catch (NotRegisteredException e1) {
				}
				test = !CombatUtil.preventPvP(townyWorld, tb);
			// Non-player catches are tested for destroy permissions.
			} else {
				test = PlayerCacheUtil.getCachePermission(player, caught.getLocation(), Material.GRASS, TownyPermission.ActionType.DESTROY);
			}
			if (!test) {
				event.setCancelled(true);
				event.getHook().remove();
			}
		}	
	}

	/*
	* PlayerMoveEvent that can fire the PlayerChangePlotEvent
	*/
	public void onPlayerMoveChunk(Player player, WorldCoord from, WorldCoord to, Location fromLoc, Location toLoc, PlayerMoveEvent moveEvent) {

		plugin.getCache(player).setLastLocation(toLoc);
		plugin.getCache(player).updateCoord(to);

		PlayerChangePlotEvent event = new PlayerChangePlotEvent(player, from, to, moveEvent);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}
	
	/*
	* PlayerChangePlotEvent that can fire the PlayerLeaveTownEvent and PlayerEnterTownEvent
	*/
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChangePlotEvent(PlayerChangePlotEvent event) {

		PlayerMoveEvent pme = event.getMoveEvent();
		Player player = event.getPlayer();		
		WorldCoord from = event.getFrom();
		WorldCoord to = event.getTo();
		try {
			@SuppressWarnings("unused")
			// Required so we don't fire events on NPCs from plugins like citizens.
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player);
			try {
				to.getTownBlock();
				if (to.getTownBlock().hasTown()) { 
					try {
						Town fromTown = from.getTownBlock().getTown();
						if (!to.getTownBlock().getTown().equals(fromTown)){
							Bukkit.getServer().getPluginManager().callEvent(new PlayerEnterTownEvent(player,to,from,to.getTownBlock().getTown(), pme)); // From Town into different Town.
							Bukkit.getServer().getPluginManager().callEvent(new PlayerLeaveTownEvent(player,to,from,from.getTownBlock().getTown(), pme));//
						}
						// Both are the same town, do nothing, no Event should fire here.
					} catch (NotRegisteredException e) { // From Wilderness into Town.
						Bukkit.getServer().getPluginManager().callEvent(new PlayerEnterTownEvent(player,to, from, to.getTownBlock().getTown(), pme));
					}
				} else {
					if (from.getTownBlock().hasTown() && !(to.getTownBlock().hasTown())){ // From has a town, to doesn't so: From Town into Wilderness
						Bukkit.getServer().getPluginManager().callEvent(new PlayerLeaveTownEvent(player,to,from, from.getTownBlock().getTown(), pme));
					}
				}
			} catch (NotRegisteredException e) {
				Bukkit.getServer().getPluginManager().callEvent(new PlayerLeaveTownEvent(player,to,from, from.getTownBlock().getTown(), pme));
			}

		} catch (NotRegisteredException e) {
			// If not registered, it is most likely an NPC			
		}		
	}
	
	/*
	 * onOutlawEnterTown
	 * - Shows message to outlaws entering towns in which they are considered an outlaw.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onOutlawEnterTown(PlayerEnterTownEvent event) throws NotRegisteredException {
		
		Player player = event.getPlayer();		
		WorldCoord to = event.getTo();
		Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player);

		if (to.getTownBlock().getTown().hasOutlaw(resident))
			TownyMessaging.sendMsg(player, Translation.of("msg_you_are_an_outlaw_in_this_town", to.getTownBlock().getTown()));
	}


	/**
	 * onPlayerDieInTown
	 * - Handles death events and the KeepInventory/KeepLevel options are being used.
	 * 
	 * @author - Articdive
	 * @param event - PlayerDeathEvent
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	// Why Highest??, so that we are the last ones to check for if it keeps their inventory, and then have no problems with it.
	public void onPlayerDieInTown(PlayerDeathEvent event) {
		boolean keepInventory = event.getKeepInventory();
		boolean keepLevel = event.getKeepLevel();
		Player player = event.getEntity();
		Location deathloc = player.getLocation();
		if (TownySettings.getKeepInventoryInTowns()) {
			if (!keepInventory) { // If you don't keep your inventory via any other plugin or the server
				TownBlock tb = TownyAPI.getInstance().getTownBlock(deathloc);
				if (tb != null) { // So a valid TownBlock appears, how wonderful
					if (tb.hasTown()) { // So the townblock has a town, and we keep inventory in towns, deathloc in a town. Do it!
						event.setKeepInventory(true);
						event.getDrops().clear();
					}
				}
			}
		}
		if (TownySettings.getKeepExperienceInTowns()) {
			if (!keepLevel) { // If you don't keep your levels via any other plugin or the server, other events fire first, we just ignore it if they do save thier invs.
				TownBlock tb = TownyAPI.getInstance().getTownBlock(deathloc);
				if (tb != null) { // So a valid TownBlock appears, how wonderful
					if (tb.hasTown()) { // So the townblock has atown, and is at the death location
						event.setKeepLevel(true);
						event.setDroppedExp(0);
					}
				}

			}
		}
		if (TownySettings.getKeepInventoryInArenas()) {
			if (!keepInventory) {
				TownBlock tb = TownyAPI.getInstance().getTownBlock(deathloc);
				if (tb != null && tb.getType() == TownBlockType.ARENA) {
					event.setKeepInventory(true);
					event.getDrops().clear();
				}
			}
		}
	}


	/**
	 * PlayerEnterTownEvent
	 * Currently used for:
	 *   - showing NotificationsUsingTitles upon entering a town.
	 *   
	 * @param event - PlayerEnterTownEvent
	 * @throws TownyException - Generic TownyException
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerEnterTown(PlayerEnterTownEvent event) throws TownyException {
		
		Resident resident = TownyUniverse.getInstance().getDataSource().getResident(event.getPlayer());
		WorldCoord to = event.getTo();
		if (TownySettings.isNotificationUsingTitles()) {
			String title = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesTownTitle());
			String subtitle = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesTownSubtitle());
			
			HashMap<String, Object> placeholders = new HashMap<>();
			placeholders.put("{townname}", StringMgmt.remUnderscore(to.getTownBlock().getTown().getName()));
			placeholders.put("{town_motd}", to.getTownBlock().getTown().getBoard());
			placeholders.put("{town_residents}", to.getTownBlock().getTown().getNumResidents());
			placeholders.put("{town_residents_online}", TownyAPI.getInstance().getOnlinePlayers(to.getTownBlock().getTown()).size());

			for(Map.Entry<String, Object> placeholder: placeholders.entrySet()) {
				title = title.replace(placeholder.getKey(), placeholder.getValue().toString());
				subtitle = subtitle.replace(placeholder.getKey(), placeholder.getValue().toString());
			}
			TownyMessaging.sendTitleMessageToResident(resident, title, subtitle);
		}
	}
	
	/**
	 * PlayerLeaveTownEvent
	 * Currently used for:
	 *   - showing NotificationsUsingTitles upon entering the wilderness.
	 *   - unjailing residents
	 *   
	 * @param event - PlayerLeaveTownEvent
	 * @throws TownyException - Generic TownyException   
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLeaveTown(PlayerLeaveTownEvent event) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		Resident resident = townyUniverse.getDataSource().getResident(event.getPlayer());
		WorldCoord to = event.getTo();
		if (TownySettings.isNotificationUsingTitles()) {
			try {
				@SuppressWarnings("unused")
				Town toTown = to.getTownBlock().getTown();
			} catch (NotRegisteredException e) { // No town being entered so this is a move into the wilderness.
				String title = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesWildTitle());
				String subtitle = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesWildSubtitle());
				if (title.contains("{wilderness}")) {
					title = title.replace("{wilderness}", StringMgmt.remUnderscore(townyUniverse.getDataSource().getWorld(event.getPlayer().getLocation().getWorld().getName()).getUnclaimedZoneName()));
				}
				if (subtitle.contains("{wilderness}")) {
					subtitle = subtitle.replace("{wilderness}", StringMgmt.remUnderscore(townyUniverse.getDataSource().getWorld(event.getPlayer().getLocation().getWorld().getName()).getUnclaimedZoneName()));
				}
				TownyMessaging.sendTitleMessageToResident(resident, title, subtitle);
			}			
		}

		Player player = event.getPlayer();
		if (townyUniverse.getDataSource().getResident(player).isJailed()) {
			resident.freeFromJail(resident.getJailSpawn(), true);
			townyUniverse.getDataSource().saveResident(resident);
		}		
	}
	
	/**
	 * Any player that can break the lectern will be able to get the book anyways.
	 * @param event - PlayerTakeLecternBookEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerTakeLecternBookEvent(PlayerTakeLecternBookEvent event) {
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getLectern().getWorld()))
			return;
		
		Player player = event.getPlayer();
		org.bukkit.block.Lectern lectern = event.getLectern();
		Location location = lectern.getLocation();
		
		boolean bDestroy = PlayerCacheUtil.getCachePermission(player, location, Material.LECTERN, ActionType.DESTROY);
		event.setCancelled(!bDestroy);
	}

	/**
	 * Blocks jailed players using blacklisted commands.
	 * @param event - PlayerCommandPreprocessEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onJailedPlayerUsesCommand(PlayerCommandPreprocessEvent event) {
		if (plugin.isError()) {
			return;
		}
		Resident resident = null;
		try {
			resident = TownyAPI.getInstance().getDataSource().getResident(event.getPlayer());
		} catch (NotRegisteredException e) {
			// More than likely another plugin using a fake player to run a command. 
		} 
		if (resident == null || !resident.isJailed())
			return;
				
		String[] split = event.getMessage().substring(1).split(" ");
		if (TownySettings.getJailBlacklistedCommands().contains(split[0])) {
			TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_you_cannot_use_that_command_while_jailed"));
			event.setCancelled(true);
		}
	}
	
}
