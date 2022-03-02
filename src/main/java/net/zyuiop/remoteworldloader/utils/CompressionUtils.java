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

package net.zyuiop.remoteworldloader.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author zyuiop
 */
public class CompressionUtils {
	public static void compressDirectory(File parentDirectory, File[] directories, File target, File... add) {
		ArrayList<File> files = new ArrayList<>();
		for (File directory : directories)
			getAllFiles(directory, files);

		for (File a : add)
			files.add(a);

		writeZipFile(parentDirectory, files, target);
	}

	private static void getAllFiles(File dir, List<File> fileList) {
		File[] files = dir.listFiles();
		for (File file : files) {
			fileList.add(file);
			if (file.isDirectory())
				getAllFiles(file, fileList);
		}
	}

	private static void writeZipFile(File directoryToZip, List<File> fileList, File target) {
		try {
			FileOutputStream fos = new FileOutputStream(target);
			GzipParameters parameters = new GzipParameters();
			parameters.setCompressionLevel(9);
			GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(fos, parameters);
			TarArchiveOutputStream stream = new TarArchiveOutputStream(gzip);

			for (File file : fileList) {
				if (!file.isDirectory()) { // we only zip files, not directories
					int retryCount = 0;
					while (retryCount < 10) {
						try {
							addToZip(directoryToZip, file, stream);
							break;
						} catch (Exception e) {
							retryCount++;
							if (retryCount > 9)
								e.printStackTrace();
						}
					}
				}
			}

			stream.close();
			gzip.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addToZip(File directoryToZip, File file, TarArchiveOutputStream zos) throws IOException {

		FileInputStream fis = new FileInputStream(file);

		String filePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1, file.getCanonicalPath().length());
		Bukkit.getLogger().info(filePath);
		ArchiveEntry zipEntry = zos.createArchiveEntry(file, filePath);
		zos.putArchiveEntry(zipEntry);

		final byte[] buf = new byte[8192];
		int bytesRead;
		while (-1 != (bytesRead = fis.read(buf)))
			zos.write(buf, 0, bytesRead);

		zos.closeArchiveEntry();
		fis.close();
	}

	public static void uncompressArchive(File archive, File target) throws IOException, CompressorException {
		CompressorInputStream compressor = new GzipCompressorInputStream(new FileInputStream(archive));
		TarArchiveInputStream stream = new TarArchiveInputStream(compressor);

		TarArchiveEntry entry;
		while ((entry = stream.getNextTarEntry()) != null) {
			File f = new File(target.getCanonicalPath(), entry.getName());
			if (f.exists()) {
				Bukkit.getLogger().warning("The file " + f.getCanonicalPath() + " already exists, deleting it.");
				if (!f.delete()) {
					Bukkit.getLogger().warning("Cannot remove, skipping file.");
				}
			}

			if (entry.isDirectory()) {
				f.mkdirs();
				continue;
			}

			f.getParentFile().mkdirs();
			f.createNewFile();

			try {
				try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(f))) {
					final byte[] buf = new byte[8192];
					int bytesRead;
					while (-1 != (bytesRead = stream.read(buf)))
						fos.write(buf, 0, bytesRead);
				}
				Bukkit.getLogger().info("Extracted file " + f.getName() + "...");
			} catch (IOException ioe) {
				f.delete();
				throw ioe;
			}
		}
	}

	public static void uncompressFile(File archive, File target) throws IOException {
		byte[] buffer = new byte[1024];
		if (target.exists()) target.delete();
		target.createNewFile();

		try {
			GZIPInputStream stream = new GZIPInputStream(new FileInputStream(archive));
			FileOutputStream out = new FileOutputStream(target);

			int len;
			while ((len = stream.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

			stream.close();
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		Bukkit.getLogger().info("Extracted file " + target.getName() + ".");
	}

	public static File compressFile(File source) throws IOException {
		String tmpName = source.getName() + ".tmp";
		File tempFile = new File(source.getParentFile(), tmpName);
		FileUtils.copyFile(source, tempFile);

		String targetName = source.getName() + ".gz";
		File file = new File(source.getParentFile(), targetName);

		if (file.exists())
			file.delete();
		file.createNewFile();

		byte[] buffer = new byte[1024];

		try {
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(file));
			FileInputStream in = new FileInputStream(tempFile);

			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

			in.close();
			out.close();
			tempFile.delete();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		Bukkit.getLogger().info("Compressed file " + source.getName() + " to " + file.getName() + ".");
		return file;
	}
}
