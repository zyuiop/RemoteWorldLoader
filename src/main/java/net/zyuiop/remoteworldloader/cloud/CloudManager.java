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

package net.zyuiop.remoteworldloader.cloud;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.common.payloads.FilePayload;
import org.openstack4j.model.identity.Token;
import org.openstack4j.model.storage.object.SwiftContainer;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.CreateUpdateContainerOptions;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.internal.OSClientSession;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author zyuiop
 */
public class CloudManager {

	private final String endpoint;
	private final String username;
	private final String password;
	private final String tenantName;

	public CloudManager(String endpoint, String username, String password, String tenantName) {
		this.endpoint = endpoint;
		this.username = username;
		this.password = password;
		this.tenantName = tenantName;
	}

	public boolean testAuthentication() {
		try {
			OSClient client = getClient();
			Token token = client.getAccess().getToken();
			return token != null;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Gets the client
	 *
	 * @return The OpenStack client
	 */
	public OSClient getClient() {
		if (OSClientSession.getCurrent() != null)
			return OSClientSession.getCurrent();

		try {
			return OSFactory.builder()
					.endpoint(endpoint)
					.credentials(username, password)
					.tenantName(tenantName)
					.authenticate();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean hasContainer(String container) {
		return getContainer(container) != null;
	}

	public SwiftContainer getContainer(String container) {
		OSClient client = getClient();
		if (client == null) {
			Bukkit.getLogger().warning("Failed checking if container exists : no openstack client available.");
			return null;
		}

		for (SwiftContainer container1 : client.objectStorage().containers().list()) {
			if (container1.getName().equalsIgnoreCase(container))
				return container1;
		}
		return null;
	}

	public void clearContainer(String container) {
		OSClient client = getClient();
		if (client == null) {
			Bukkit.getLogger().warning("Failed checking if container exists : no openstack client available.");
			return;
		}

		client.objectStorage().objects().list(container).forEach(obj -> client.objectStorage().objects().delete(container, obj.getName()));
		Bukkit.getLogger().info("Cleared container " + container + ".");
	}

	public boolean createContainer(String container) {
		OSClient client = getClient();
		if (client == null) {
			Bukkit.getLogger().warning("Failed container creation : no openstack client available.");
			return false;
		}

		return client.objectStorage().containers().create(container, CreateUpdateContainerOptions.create().accessAnybodyRead()).isSuccess();
	}

	public boolean deleteContainer(String container) {
		OSClient client = getClient();
		if (client == null) {
			Bukkit.getLogger().warning("Failed container removal : no openstack client available.");
			return false;
		}

		return client.objectStorage().containers().delete(container).isSuccess();
	}

	public Set<File> downloadWholeContainer(String container, File targetDirectory, Consumer<File> applyOnEachFile, CommandSender sender) throws IOException {
		OSClient client = getClient();
		if (client == null) {
			Bukkit.getLogger().warning("Failed container download : no openstack client available.");
			return null;
		}

		HashSet<File> files = new HashSet<>();
		List<? extends SwiftObject> objects = client.objectStorage().objects().list(container);
		int i = 0;
		for (SwiftObject object : objects) {
			i++;
			File targetFile = new File(targetDirectory, object.getName());
			targetFile.delete();
			targetFile.getParentFile().mkdirs();
			DLPayload payload = object.download();
			payload.writeToFile(targetFile);
			Bukkit.getLogger().info("[" + i + "/" + objects.size() + "] Downloaded object " + object.getName() + ", size : " + ((double) targetFile.length() / 1024D) + " kB");
			files.add(targetFile);
			if (applyOnEachFile != null)
				applyOnEachFile.accept(targetFile);

			if (sender != null) {
				sender.sendMessage(ChatColor.GRAY + "Downloaded file " + i + " of " + objects.size() + " : " + object.getName());
			}
		}
		return files;
	}

	public void uploadAllToContainer(File basePath, String container, Function<File, File> applyOnEachFile, Collection<File> paths, Consumer<File> postUpload) throws IOException {
		uploadAllToContainer(basePath, container, applyOnEachFile, paths, postUpload, ObjectPutOptions.NONE);
	}

	public void uploadAllToContainer(File basePath, String container, Function<File, File> applyOnEachFile, Collection<File> paths, Consumer<File> postUpload, ObjectPutOptions objectPutOptions) throws IOException {
		uploadAllToContainer((file) -> {
			try {
				return file.getCanonicalPath().replace(basePath.getCanonicalPath(), "");
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}, container, applyOnEachFile, paths, postUpload, objectPutOptions);
	}

	public void uploadAllToContainer(Function<File, String> filePathRenamer, String container, Function<File, File> applyOnEachFile, Collection<File> paths, Consumer<File> postUpload, ObjectPutOptions objectPutOptions) throws IOException {
		OSClient client = getClient();
		if (client == null) {
			Bukkit.getLogger().warning("Failed container download : no openstack client available.");
			return;
		}

		if (objectPutOptions == null)
			objectPutOptions = ObjectPutOptions.NONE;

		int i = 0;
		for (File file : paths) {
			i++;
			if (file.isDirectory())
				continue;

			file = (applyOnEachFile != null ? applyOnEachFile.apply(file) : file);
			if (file == null) {
				continue;
			}

			String targetName = filePathRenamer.apply(file);
			if (targetName == null) {
				Bukkit.getLogger().warning("Skipped file " + file.getPath() + " (null target name)");
				continue;
			}
			String etag = client.objectStorage().objects().put(container, targetName, new FilePayload(file), objectPutOptions);

			Bukkit.getLogger().info("[" + i + "/" + paths.size() + "] Uploaded file " + targetName + ". {size=" + ((double) file.length() / 1024D) + " kB, etag=" + etag + "}");
			if (postUpload != null)
				postUpload.accept(file);
		}

		Bukkit.getLogger().info("Done, everything uploaded !");
	}
}
