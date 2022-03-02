/*
 * RemoteWorldLoader for Bukkit
 * Copyright (C) 2016  zyuiop
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zyuiop.remoteworldloader;

import net.zyuiop.remoteworldloader.cloud.CloudManager;
import net.zyuiop.remoteworldloader.commands.CommandChangeWorld;
import net.zyuiop.remoteworldloader.commands.CommandLoad;
import net.zyuiop.remoteworldloader.commands.CommandReset;
import net.zyuiop.remoteworldloader.commands.CommandSave;
import net.zyuiop.remoteworldloader.managers.SavingManager;
import net.zyuiop.remoteworldloader.version.WorldSaverImpl;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

/**
 * @author zyuiop
 */
public class RemoteWorldLoader extends JavaPlugin {

	private SavingManager manager;
	private CloudManager  cloudManager;

	public static RemoteWorldLoader instance;

	public void onEnable() {
		instance = this;

		Server server = Bukkit.getServer();
		Class<?> serverClass = server.getClass();
		String[] parts = serverClass.getName().split("\\.");
		// [org, bukkit, craftbukkit, v1..., ...]

		if (parts.length < 4) {
			Bukkit.getLogger().severe("Unknown minecraft version. Cannot load plugin.");
			getServer().shutdown();
			return;
		}

		String nameSpace = parts[3];
		try {
			Class.forName("net.minecraft.server." + nameSpace + ".WorldServer");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Bukkit.getLogger().severe("Unknown minecraft version. Cannot load plugin.");
			getServer().shutdown();
			return;
		}

		manager = new SavingManager(this, new WorldSaverImpl(nameSpace));

		Bukkit.getLogger().info("Loading remoteWorldLoader : trying to find a world to load...");
		getServer().getPluginManager().registerEvents(manager, this);
		saveDefaultConfig();

		ConfigurationSection section = getConfig().getConfigurationSection("swift");
		if (section == null) {
			getLogger().severe("Cannot load plugin : you need to provide a swift configuration.");
			getServer().shutdown();
			return;
		}

		String endPoint = section.getString("endPoint");
		String username = section.getString("username");
		String password = section.getString("password");
		String tenantName = section.getString("tenantName");
		cloudManager = new CloudManager(endPoint, username, password, tenantName);

		if (!cloudManager.testAuthentication()) {
			getLogger().severe("Cannot load plugin : Invalid openstack credentials");
			getServer().shutdown();
			return;
		}

		getCommand("load").setExecutor(new CommandLoad());
		getCommand("save").setExecutor(new CommandSave());
		getCommand("changeworld").setExecutor(new CommandChangeWorld());
		getCommand("reset").setExecutor(new CommandReset());

		if (getConfig().contains("forcedworlds")) {
			for (Map<?, ?> map : getConfig().getMapList("forcedworlds")) {
				String world = (String) map.get("world");
				String container = world;
				if (map.containsKey("container"))
					container = (String) map.get("container");

				// Load
				getLogger().info("Loading world " + world + " automatically from container " + container + "...");
				try {
					manager.load(container, world, null);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public SavingManager getManager() {
		return manager;
	}

	public CloudManager getCloudManager() {
		return cloudManager;
	}
}
