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

package net.zyuiop.remoteworldloader.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author zyuiop
 */
public class CommandChangeWorld implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		if (strings.length == 0) {
			commandSender.sendMessage(ChatColor.RED + "Usage : /" + s + " <world name>");
		} else {
			if (commandSender instanceof Player) {
				World world = Bukkit.getWorld(strings[0]);
				if (world == null) {
					commandSender.sendMessage(ChatColor.RED + "This world doesn't exist.");
				} else {
					if (!commandSender.hasPermission("remoteworldloader.accessworld.*") && commandSender.hasPermission("remoteworldloader.accessworld." + world.getName())) {
						commandSender.sendMessage(ChatColor.RED + "You don't have the permission to access this world.");
						return true;
					}

					commandSender.sendMessage(ChatColor.YELLOW + "Successfully teleported to world " + ChatColor.GREEN + strings[0] + ChatColor.YELLOW + " !");
					((Player) commandSender).teleport(world.getSpawnLocation());
				}
			}
		}
		return true;
	}
}
