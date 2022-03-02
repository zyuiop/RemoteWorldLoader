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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author zyuiop
 */
public class CommandReset implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		if (strings.length == 0) {
			commandSender.sendMessage(ChatColor.RED + "Usage : /" + s + " <container name>");
			commandSender.sendMessage(ChatColor.RED + "BEWARE ! This command removes the remote container, making it totally impossible to recover.");
		} else {
			String container = strings[0];
			commandSender.sendMessage(ChatColor.YELLOW + "Starting removal of container " + ChatColor.GREEN + container + ChatColor.YELLOW + " !");

			Bukkit.getScheduler().runTaskAsynchronously(RemoteWorldLoader.instance, () -> {
				RemoteWorldLoader.instance.getCloudManager().deleteContainer(container);
				commandSender.sendMessage(ChatColor.YELLOW + "Container " + ChatColor.GREEN + container + ChatColor.YELLOW + " removed !");
			});
		}

		return true;
	}
}
