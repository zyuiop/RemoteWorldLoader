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
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author zyuiop
 */
public class CommandSave implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		if (strings.length == 0) {
			commandSender.sendMessage(ChatColor.RED + "Usage : /" + s + " <world> [target container]");
			commandSender.sendMessage(ChatColor.RED + "By default, the target container is the world.");
 		} else {
			String world = strings[0];
			String container = strings.length > 1 ? strings[1] : world;

			World bukkitWorld = Bukkit.getWorld(world);
			if (bukkitWorld == null) {
				commandSender.sendMessage(ChatColor.RED + "This world doesn't exist !");
				return true;
			}
			RemoteWorldLoader.instance.getManager().saveCompressUpload(bukkitWorld, container, commandSender);
		}

		return true;
	}
}
