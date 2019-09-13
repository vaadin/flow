/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * Shared code for managing contents of jar files.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class JarContentsManager {
    private static final String JAR_PATH_SEPARATOR = "/";

    /**
     * Checks if a jar file contains a path specified (case sensitive).
     *
     * @param jar
     *            jar file to look for file in, not {@code null}
     * @param filePath
     *            file path relative to jar root, not {@code null}
     * @return {@code true} if path is contained in the jar, {@code false}
     *         otherwise
     * @throws IllegalArgumentException
     *             if jar file specified is not a file or does not exist
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws UncheckedIOException
     *             if {@link IOException} occurs during file search, for
     *             instance, when jar file specified is not a jar file
     */
    public boolean containsPath(File jar, String filePath) {
        requireFileExistence(jar);
        Objects.requireNonNull(filePath);

        try (JarFile jarFile = new JarFile(jar, false)) {
            boolean containsEntry = jarFile.getJarEntry(filePath) != null;
            // in case #6241, the directory structure is omitted from the
            // jar's metadata, and the entry point cannot be found. Thus we
            // scan files just in case
            if (!containsEntry) {
                containsEntry = jarFile.stream().anyMatch(
                        entry -> entry.getName().startsWith(filePath));
            }

            return containsEntry;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to retrieve file '%s' from jar '%s'",
                            filePath, jar),
                    e);
        }
    }

    /**
     * Tries to find a file by its path (case sensitive) in jar file. If found,
     * its contents is returned, {@code null} otherwise.
     *
     * @param jar
     *            jar file to look for file in, not {@code null}
     * @param filePath
     *            file path relative to jar root, not {@code null}
     * @return an array of bytes, if file was found or {@code null} if not found
     * @throws IllegalArgumentException
     *             if jar file specified is not a file or does not exist
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws UncheckedIOException
     *             if {@link IOException} occurs during file retrieval, for
     *             instance, when jar file specified is not a jar file
     */
    public byte[] getFileContents(File jar, String filePath) {
        requireFileExistence(jar);
        Objects.requireNonNull(filePath);

        try (JarFile jarFile = new JarFile(jar, false)) {
            return getJarEntryContents(jarFile, jarFile.getJarEntry(filePath));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to retrieve file '%s' from jar '%s'",
                            filePath, jar),
                    e);
        }
    }

    /**
     * Finds all files (not directories) in the jar with the name specified and
     * in the directory specified.
     *
     * @param jar
     *            jar file to look for file in, not {@code null}
     * @param baseDirectoryName
     *            the directory to search in the jar, not {@code null}
     * @param fileName
     *            a string that should required files' paths end with, not
     *            {@code null}
     * @return list of files from the directory specified with required file
     *         names
     * @throws IllegalArgumentException
     *             if jar file specified is not a file or does not exist
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws UncheckedIOException
     *             if {@link IOException} occurs during jar file search
     */
    public List<String> findFiles(File jar, String baseDirectoryName,
            String fileName) {
        requireFileExistence(jar);
        Objects.requireNonNull(baseDirectoryName);
        Objects.requireNonNull(fileName);

        try (JarFile jarFile = new JarFile(jar, false)) {
            return jarFile.stream().filter(entry -> !entry.isDirectory())
                    .map(ZipEntry::getName)
                    .filter(path -> path.startsWith(baseDirectoryName))
                    .filter(path -> path
                            .endsWith(JAR_PATH_SEPARATOR + fileName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void requireFileExistence(File jar) {
        if (!Objects.requireNonNull(jar).isFile()) {
            throw new IllegalArgumentException(
                    String.format("Expect '%s' to be an existing file", jar));
        }
    }

    private byte[] getJarEntryContents(JarFile jarFile, JarEntry entry) {
        if (entry == null) {
            return null;
        }

        try (InputStream entryStream = jarFile.getInputStream(entry)) {
            return IOUtils.toByteArray(entryStream);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to get entry '%s' contents from jar file '%s'",
                    entry, jarFile), e);
        }
    }

    /**
     * Copies files from the jar file to the output directory.
     *
     * @param jar
     *            jar file to look for files in, not {@code null}
     * @param jarDirectoryToCopyFrom
     *            a path relative to jar root, only files from this path will be
     *            copied, can be {@code null}, which is treated as a root of the
     *            jar. Files will be copied relative to this path (i.e. only
     *            path part after this path is preserved in output directory)
     * @param outputDirectory
     *            the directory to copy files to, not {@code null}
     * @param wildcardPathExclusions
     *            wildcard exclusions that are used to check each path against
     *            before copying
     * @throws IllegalArgumentException
     *             if jar file specified is not a file or does not exist or if
     *             output directory is not a directory or does not exist
     * @throws NullPointerException
     *             if jar file or output directory is {@code null}
     * @throws UncheckedIOException
     *             if {@link IOException} occurs during the operation, for
     *             instance, when jar file specified is not a jar file
     */
    public void copyFilesFromJarTrimmingBasePath(File jar,
            String jarDirectoryToCopyFrom, File outputDirectory,
            String... wildcardPathExclusions) {
        requireFileExistence(jar);

        if (!Objects.requireNonNull(outputDirectory).isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("Expect '%s' to be an existing directory",
                            outputDirectory));
        }

        String basePath = normalizeJarBasePath(jarDirectoryToCopyFrom);

        try (JarFile jarFile = new JarFile(jar, false)) {
            jarFile.stream().filter(file -> !file.isDirectory())
                    .filter(file -> file.getName().toLowerCase(Locale.ENGLISH)
                            .startsWith(basePath.toLowerCase(Locale.ENGLISH)))
                    .filter(file -> isFileIncluded(file,
                            wildcardPathExclusions))
                    .forEach(jarEntry -> copyJarEntryTrimmingBasePath(jarFile,
                            jarEntry, basePath, outputDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to extract files from jarFile '%s' to directory '%s'",
                    jar, outputDirectory), e);
        }
    }

    /**
     * Copies files matching the inclusion filters from the jar file to the
     * output directory.
     *
     * @param jar
     *            jar file to look for files in, not {@code null}
     * @param jarDirectoryToCopyFrom
     *            a path relative to jar root, only files from this path will be
     *            copied, can be {@code null}, which is treated as a root of the
     *            jar. Files will be copied relative to this path (i.e. only
     *            path part after this path is preserved in output directory)
     * @param outputDirectory
     *            the directory to copy files to, not {@code null}
     * @param wildcardPathInclusions
     *            wildcard inclusions that are used to check each path against
     *            before copying
     * @throws IllegalArgumentException
     *             if jar file specified is not a file or does not exist or if
     *             output directory is not a directory or does not exist
     * @throws NullPointerException
     *             if jar file or output directory is {@code null}
     * @throws UncheckedIOException
     *             if {@link IOException} occurs during the operation, for
     *             instance, when jar file specified is not a jar file
     */
    public void copyIncludedFilesFromJarTrimmingBasePath(File jar,
            String jarDirectoryToCopyFrom, File outputDirectory,
            String... wildcardPathInclusions) {
        requireFileExistence(jar);

        if (!Objects.requireNonNull(outputDirectory).isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("Expect '%s' to be an existing directory",
                            outputDirectory));
        }

        String basePath = normalizeJarBasePath(jarDirectoryToCopyFrom);

        try (JarFile jarFile = new JarFile(jar, false)) {
            jarFile.stream().filter(file -> !file.isDirectory())
                    .filter(file -> file.getName().toLowerCase(Locale.ENGLISH)
                            .startsWith(basePath.toLowerCase(Locale.ENGLISH)))
                    .filter(file -> includeFile(file, wildcardPathInclusions))
                    .forEach(jarEntry -> copyJarEntryTrimmingBasePath(jarFile,
                            jarEntry, basePath, outputDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to extract files from jarFile '%s' to directory '%s'",
                    jar, outputDirectory), e);
        }
    }

    private String normalizeJarBasePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "";
        }
        return rawPath.endsWith(JAR_PATH_SEPARATOR) ? rawPath
                : rawPath + JAR_PATH_SEPARATOR;
    }

    private boolean isFileIncluded(ZipEntry file, String... pathExclusions) {
        String filePath = file.getName();
        return Stream.of(pathExclusions)
                .noneMatch(exclusionRule -> FilenameUtils
                        .wildcardMatch(filePath, exclusionRule));
    }

    private boolean includeFile(ZipEntry file, String... pathInclusions) {
        String filePath = file.getName();
        return Stream.of(pathInclusions).anyMatch(inclusionRule -> FilenameUtils
                .wildcardMatch(filePath, inclusionRule));
    }

    private void copyJarEntryTrimmingBasePath(JarFile jarFile,
            ZipEntry jarEntry, String basePath, File outputDirectory) {
        String fullPath = jarEntry.getName();
        String relativePath = fullPath
                .substring(fullPath.toLowerCase(Locale.ENGLISH)
                        .indexOf(basePath.toLowerCase(Locale.ENGLISH))
                        + basePath.length());
        File target = new File(outputDirectory, relativePath);
        try {
            if (target.exists()) {
                File tempFile = File.createTempFile(fullPath, null);
                FileUtils.copyInputStreamToFile(
                        jarFile.getInputStream(jarEntry), tempFile);
                if (!FileUtils.contentEquals(tempFile, target)) {
                    FileUtils.forceDelete(target);
                    FileUtils.moveFile(tempFile, target);
                }
            } else {
                FileUtils.copyInputStreamToFile(
                        jarFile.getInputStream(jarEntry), target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to extract jar entry '%s' from jarFile '%s'",
                    jarEntry, outputDirectory), e);
        }
    }

}
