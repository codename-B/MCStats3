package com.ubempire.MCStats3;
//Copyright (C) 2010  Ryan Michela
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.ubempire.MCStats3.reporting.*;
import com.ubempire.MCStats3.service.*;
import com.ubempire.MCStats3.service.econ.*;
import com.ubempire.MCStats3.controller.*;
import com.ubempire.MCStats3.model.*;

public class StatsPlugin extends JavaPlugin {

	private boolean loadError = false;
	private boolean initialized = false;
	
	private StatsModel model;
	private StatsConfig config;
	private StatsController controller;
	private Logger log;
	// This is public so we can
	public static Method Method = null;
	public PluginDescriptionFile info = null;
	public PlayerInfo infoservice;
	public static Server currentServer;
	public static GroupService groupService;
	
	private ShutdownHook hook = new ShutdownHook();

	@Override
	public void onLoad() {
		// Initialize logging
		log = this.getServer().getLogger();
		log.info("[MCStats] Loading MCStats");
		
		try {
			// Initialize the data folder
			if(!getDataFolder().exists()) {
				getDataFolder().mkdir();
			}
			
			File configFile = new File(getDataFolder(), "config.yml");			
			if(!configFile.exists()) {
				log.info("[MCStats] Populating initial config file");
				FileOutputStream stream = new FileOutputStream(new File(getDataFolder(), "config.yml"));
				OutputStreamWriter out = new OutputStreamWriter(stream);
				out.write(StatsConfig.getInitialConfig());
				out.close();
				stream.close();
			}
			
			// Initialize state
			currentServer = getServer();
			config = new StatsConfig(getConfiguration(), getDataFolder().toString());
			model = new StatsModel(config, log);
			controller = new StatsController(model.getStats(), config);
			
			initialized = true;
		} catch (Exception e) {
			log.log(Level.SEVERE, "[MCStats] Error in initialization.", e);
			loadError = true;
		}
	}
	
	@Override
	//Attach listener hooks
	public void onEnable() {
		if (!initialized) {
			onLoad();
		}
		
		if (!loadError) {
			log.info("[MCStats] Enabling MCStats");
			
			// Initialize services
			try {
			    infoservice = getServer().getServicesManager().load(PlayerInfo.class);
			      }
			catch (java.lang.NoClassDefFoundError e) {
			    System.out.println("bInfo not installed!");
			}
			
			groupService = new GroupService(this, config.getIgnoreGroups());
			PluginManager pluginManager = getServer().getPluginManager();
			pluginManager.registerEvent(Event.Type.PLUGIN_ENABLE, new server(this),
					Priority.Monitor, this);
			pluginManager.registerEvent(Event.Type.PLUGIN_DISABLE,
					new server(this), Priority.Monitor, this);
			
			// Configure the serializer cache
			StatsSerializer.enableSerializerCache = config
					.getEnableSerializerCache();
			// Register command
			getCommand("played").setExecutor(new PlayedCommand(controller));
			//configure event hooks
			StatsPlayerListener spl = new StatsPlayerListener(controller);
			getServer().getPluginManager().registerEvent(Type.PLAYER_JOIN, spl, Priority.Monitor, this);
			getServer().getPluginManager().registerEvent(Type.PLAYER_QUIT, spl, Priority.Monitor, this);
			getServer().getPluginManager().registerEvent(Type.PLAYER_KICK, spl, Priority.Monitor, this);
			getServer().getPluginManager().registerEvent(Type.PLAYER_MOVE, spl, Priority.Monitor, this);
			getServer().getPluginManager().registerEvent(Type.PLAYER_DROP_ITEM, spl, Priority.Monitor, this);
			
			StatsEntityListener sel = new StatsEntityListener(controller);
			getServer().getPluginManager().registerEvent(Type.ENTITY_DEATH, sel, Priority.Monitor, this);
			getServer().getPluginManager().registerEvent(Type.ENTITY_DAMAGE, sel, Priority.Monitor, this);
			
			StatsBlockListener sbl = new StatsBlockListener(controller);
			getServer().getPluginManager().registerEvent(Type.BLOCK_PLACE, sbl, Priority.Monitor, this);
			getServer().getPluginManager().registerEvent(Type.BLOCK_BREAK, sbl, Priority.Monitor, this);
			
			//purge any users marked for removal
			if (config.getPlayersToPurge().length > 0) {
				for (String playerName : config.getPlayersToPurge()) {
					model.purgePlayer(playerName);
				}
				config.clearPlayersToPurge();
			}
			//reset all playtimes if requested
			if (config.getResetPlaytime()) {
				log.info("[MCStats] Resetting all player play times");
				model.resetAllPlaytimes();
				config.clearResetPlaytime();
			}
			//register a shutdown hook
			Runtime.getRuntime().addShutdownHook(hook);
			controller.logOutAllPlayers();
			controller.logInOnlinePlayers();
			model.startPersisting();
		}
	}

	@Override
	//Detach listener hooks
	public void onDisable() {
		if (!loadError) {
			log.info("[MCStats] Disabling MCStats");
			
			Runtime.getRuntime().removeShutdownHook(hook);
			controller.logOutAllPlayers();
			model.stopPersisting();
			model.saveStats();
			model.saveUserFiles();
		}
	}
	
	private class ShutdownHook extends Thread {
		public void run() { 
			controller.logOutAllPlayers();
			model.saveStats(); 
			System.out.println("[MCStats] Persisting player statistics on dirty exit!");
		}
	}
}
