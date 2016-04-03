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

package net.zyuiop.remoteworldloader.managers;

import net.zyuiop.remoteworldloader.RemoteWorldLoader;
import net.zyuiop.remoteworldloader.utils.CompressionUtils;
import net.zyuiop.remoteworldloader.version.WorldSaver;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zyuiop
 */
public class SavingManager implements Listener {
	private final RemoteWorldLoader loader;
	private final WorldSaver        worldSaver;

	public SavingManager(RemoteWorldLoader loader, WorldSaver worldSaver) {
		this.loader = loader;
		this.worldSaver = worldSaver;
	}

	private static void getAllFiles(File dir, List<File> fileList) {
		File[] files = dir.listFiles();
		for (File file : files) {
			fileList.add(file);
			if (file.isDirectory())
				getAllFiles(file, fileList);
		}
	}

	/**
	 * Compresses and uploads the world to the container
	 *
	 * @param targetContainer The target container name
	 */
	private void compressAndUpload(File worldFile, String targetContainer, CommandSender sender) {
		logAndSend(sender, ChatColor.YELLOW + "Uploading world " + worldFile.getName() + " to container " + targetContainer + "...");

		List<File> fileList = new ArrayList<>();
		getAllFiles(worldFile, fileList);

		// 2. Upload
		if (!loader.getCloudManager().hasContainer(targetContainer)) {
			loader.getCloudManager().createContainer(targetContainer);
		}

		// Count
		long totalFiles = (long) fileList.stream().filter(file -> !file.isDirectory()).count();
		final long[] cpt = {0};

		try {
			loader.getCloudManager().uploadAllToContainer(worldFile, targetContainer, (file) -> {
				cpt[0]++;
				sender.sendMessage(ChatColor.GRAY + "Processing file " + cpt[0] + " of " + totalFiles + " : " + file.getName() + "...");

				try {
					return CompressionUtils.compressFile(file);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}, fileList, File::delete); // We delete the file as it's compressed, we don't care about the compressed files
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void save(World world) {
		worldSaver.save(world);
	}

	public void saveCompressUpload(World world, String container, CommandSender sender) {
		save(world);
		logAndSend(sender, ChatColor.YELLOW + "World saved locally.");
		Bukkit.getScheduler().runTaskAsynchronously(loader, () -> {
			compressAndUpload(world.getWorldFolder(), container, sender);
			logAndSend(sender, ChatColor.GREEN + "Upload finished ! Your world has been completely saved on container " + ChatColor.YELLOW + container);
		}); // Async might be buggy
	}

	private void logAndSend(CommandSender player, String log) {
		Bukkit.getLogger().info(log);
		if (player != null) {
			player.sendMessage(ChatColor.GRAY + log);
		}
	}

	/**
	 * Remotely loads a world
	 *
	 * @return true if the load succeeded, false if it didnt
	 */
	public boolean load(String container, String targetWorld, CommandSender player) throws IOException {
		if (!loader.getCloudManager().hasContainer(container)) {
			logAndSend(player, "This container doesn't exist, aborting.");
			return false;
		}

		World world = Bukkit.getWorld(targetWorld);
		if (world != null) {
			logAndSend(player, "World exists, unloading...");
			Bukkit.unloadWorld(world, false);
			FileUtils.deleteDirectory(world.getWorldFolder());
		}

		File targetFile = new File(loader.getDataFolder().getAbsoluteFile().getParentFile().getParentFile(), targetWorld);
		if (targetFile.exists()) {
			if (targetFile.isDirectory())
				FileUtils.deleteDirectory(targetFile);
			else
				targetFile.delete();
		}

		// Download container
		Set<File> files = loader.getCloudManager().downloadWholeContainer(container, targetFile, (file) -> {
			File parent = file.getParentFile();
			String name = file.getName().replace(".tar", "").replace(".gz", "").replace(".xz", "").replace(".bz2", "");
			try {
				CompressionUtils.uncompressFile(file, new File(parent, name));
			} catch (IOException e) {
				e.printStackTrace();
			}
			file.delete();
		}, player);

		logAndSend(player, ChatColor.GREEN + "World downloaded ! Loading it...");

		if (files == null)
			return false;

		// Load world
		return loadWorld(targetWorld);
	}

	private final Set<String> scheduledWorld = new HashSet<>();

	private boolean loadWorld(String name) {
		if (Bukkit.getWorlds().size() == 0) {
			scheduledWorld.add(name);
			return true;
		}

		return Bukkit.createWorld(new WorldCreator(name)) != null;
	}

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		if (scheduledWorld.size() > 0) {
			Set<String> copy = new HashSet<>(scheduledWorld);
			scheduledWorld.clear(); // Clear queued worlds

			Bukkit.getScheduler().runTask(loader, () -> {
				copy.stream()
						.filter(name -> Bukkit.getWorld(name) == null)
						.map(WorldCreator::new)
						.forEach(Bukkit::createWorld); // Create queued worlds
			});
		}
	}
}
