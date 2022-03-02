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

import net.zyuiop.remoteworldloader.RemoteWorldLoader;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;

/**
 * @author zyuiop
 */
public class CommandLoad implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		if (strings.length == 0) {
			commandSender.sendMessage(ChatColor.RED + "Usage : /" + s + " <container name> [target world name]");
			commandSender.sendMessage(ChatColor.RED + "No spaces are admitted in container name and target world name");
			commandSender.sendMessage(ChatColor.RED + "If no world name is provided, the world is loaded under the container name");
		} else {
			String container = strings[0];
			String world = strings.length > 1 ? strings[1] : container;

			try {
				if (RemoteWorldLoader.instance.getManager().load(container, world, commandSender)) {
					commandSender.sendMessage(ChatColor.YELLOW + "Successfully loaded world " + ChatColor.GREEN + world + ChatColor.YELLOW + " !");
				} else {
					commandSender.sendMessage(ChatColor.RED + "Failed to load the world !");
				}
			} catch (IOException e) {
				e.printStackTrace();
				commandSender.sendMessage(ChatColor.RED + "An error occurred, please check the logs for further information.");
			}
		}
		return false;
	}
}
