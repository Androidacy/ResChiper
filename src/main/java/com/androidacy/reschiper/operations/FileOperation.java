package com.androidacy.reschiper.operations;

import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.build.bundletool.model.utils.ZipUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility class for various file operations.
 */
public class FileOperation {
    private static final Logger logger = Logger.getLogger(FileOperation.class.getName());

    /**
     * Recursively deletes a directory and its contents.
     *
     * @param file The directory to delete.
     * @return true if the directory was successfully deleted, false otherwise.
     */
    public static boolean deleteDir(File file) {
        if (file == null || (!file.exists())) {
            return false;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File value : files) {
                    deleteDir(value);
                }
            }
        }
        file.delete();
        return true;
    }

    /**
     * Gets a human-readable file size description from a file size in bytes.
     *
     * @param size The file size in bytes.
     * @return A string representing the file size with appropriate units (B, KB, MB, GB).
     */
    public static @NotNull String getNetFileSizeDescription(long size) {
        StringBuilder bytes = new StringBuilder();
        DecimalFormat format = new DecimalFormat("###.0");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            bytes.append(format.format(i)).append("GB");
        } else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            bytes.append(format.format(i)).append("MB");
        } else if (size >= 1024) {
            double i = (size / (1024.0));
            bytes.append(format.format(i)).append("KB");
        } else {
            if (size <= 0) {
                bytes.append("0B");
            } else {
                bytes.append((int) size).append("B");
            }
        }
        return bytes.toString();
    }

    /**
     * Gets the size of a file in bytes.
     *
     * @param f The file to get the size of.
     * @return The file size in bytes.
     */
    public static long getFileSizes(@NotNull File f) {
        if (f.exists() && f.isFile()) {
            return f.length();
        }
        return 0;
    }

    /**
     * Gets the size of a file within a ZIP archive.
     *
     * @param zipFile   The ZIP file.
     * @param zipEntry  The ZIP entry representing the file.
     * @return The size of the file in bytes.
     */
    public static long getZipPathFileSize(ZipFile zipFile, ZipEntry zipEntry) {
        long size = zipEntry.getSize();
        if (size >= 0) {
            return size;
        }
        // Fallback: read the full content length
        try (InputStream is = ZipUtils.asByteSource(zipFile, zipEntry).openStream()) {
            size = is.readAllBytes().length;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to get ZipPath file size", e);
            size = 0;
        }
        return size;
    }

    /**
     * Gets the simple name of a file from a ZipPath.
     *
     * @param zipPath The ZipPath representing the file.
     * @return The simple name of the file.
     */
    public static @NotNull String getFileSimpleName(@NotNull ZipPath zipPath) {
        return zipPath.getFileName().toString();
    }

    /**
     * Gets the file suffix (extension) from a ZipPath.
     *
     * @param zipPath The ZipPath representing the file.
     * @return The file suffix (extension).
     */
    public static @NotNull String getFileSuffix(@NotNull ZipPath zipPath) {
        String fileName = zipPath.getName(zipPath.getNameCount() - 1).toString();
        if (!fileName.contains(".")) {
            return fileName;
        }
        String[] values = fileName.replace(".", "/").split("/");
        return fileName.substring(values[0].length());
    }

    /**
     * Gets the parent directory path from a Zip file path.
     *
     * @param zipPath The Zip file path.
     * @return The parent directory path.
     */
    public static @NotNull String getParentFromZipFilePath(@NotNull String zipPath) {
        if (!zipPath.contains("/")) {
            throw new IllegalArgumentException("invalid zipPath: " + zipPath);
        }
        String[] values = zipPath.split("/");
        return zipPath.substring(0, zipPath.indexOf(values[values.length - 1]) - 1);
    }

    /**
     * Gets the name of a file from a Zip file path.
     *
     * @param zipPath The Zip file path.
     * @return The file name.
     */
    public static String getNameFromZipFilePath(@NotNull String zipPath) {
        if (!zipPath.contains("/")) {
            throw new IllegalArgumentException("invalid zipPath: " + zipPath);
        }
        String[] values = zipPath.split("/");
        return values[values.length - 1];
    }

    /**
     * Gets the file prefix (name without extension) from a file name.
     *
     * @param fileName The file name.
     * @return The file prefix.
     */
    public static String getFilePrefixByFileName(@NotNull String fileName) {
        if (!fileName.contains(".")) {
            throw new IllegalArgumentException("invalid file name: " + fileName);
        }
        String[] values = fileName.replace(".", "/").split("/");
        return values[0];
    }
}