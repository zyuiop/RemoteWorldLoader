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

package net.zyuiop.remoteworldloader.version;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author zyuiop
 */
public class WorldSaverImpl implements WorldSaver {
	private final String namespace;

	public WorldSaverImpl(String namespace) {
		this.namespace = namespace;
	}

	private Class<? extends World> getWorldClass() throws ClassNotFoundException {
		return (Class<? extends World>) Class.forName("org.bukkit.craftbukkit." + namespace + ".CraftWorld");
	}

	private Class getNmsWorldClass() throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + namespace + ".WorldServer");
	}

	private Class getProgressUpdate() throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + namespace + ".IProgressUpdate");
	}

	private void executeSaveChunks() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Class nms = Class.forName("net.minecraft.server." + namespace + ".MinecraftServer");
		Class dedicatedPlayerListClass = Class.forName("net.minecraft.server." + namespace + ".DedicatedPlayerList");
		Class obs = Class.forName("org.bukkit.craftbukkit." + namespace + ".CraftServer");

		Object obj = obs.cast(Bukkit.getServer());
		Object server = dedicatedPlayerListClass.getMethod("getServer").invoke(obs.getMethod("getHandle").invoke(obj));

		Method method = nms.getDeclaredMethod("saveChunks", boolean.class);
		method.setAccessible(true);
		method.invoke(server, false);
	}

	@Override
	public void save(World world) {
		try {
			Class<? extends World> craftWorld = getWorldClass();
			Class nmsWorldClass = getNmsWorldClass();

			if (craftWorld.isInstance(world)) {
				Bukkit.getLogger().info("Saving world " + world + "...");
				craftWorld.getMethod("save", boolean.class).invoke(world, true);
				Object nmsWorld = craftWorld.getMethod("getHandle").invoke(world);

				nmsWorldClass.getMethod("save", boolean.class, getProgressUpdate()).invoke(nmsWorld, true, null);
				nmsWorldClass.getMethod("saveLevel").invoke(nmsWorld);
			}

			executeSaveChunks();
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}

