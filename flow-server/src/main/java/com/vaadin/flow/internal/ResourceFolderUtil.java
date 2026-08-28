/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lists the files in a folder on the classpath, wherever the folder lives.
 * <p>
 * A folder has no content of its own to read, so its files have to be listed,
 * which depends on whether the folder is a folder on the file system, an entry
 * in a jar or something an application server serves through a protocol of its
 * own.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.3
 */
public final class ResourceFolderUtil {

    /**
     * A file inside a folder on the classpath.
     * <p>
     * The file can only be read while it is being visited.
     */
    public interface FolderFile {

        /**
         * Gets the name of the file, without the folder it is in.
         *
         * @return the file name
         */
        String getName();

        /**
         * Gets the location the file is read from, for logging and for telling
         * files of the same name apart.
         *
         * @return the location of the file
         */
        String getLocation();

        /**
         * Opens the content of the file.
         *
         * @return the content of the file, to be closed by the caller
         * @throws IOException
         *             if the content cannot be read
         */
        InputStream open() throws IOException;
    }

    /**
     * Called for each file in a folder.
     */
    @FunctionalInterface
    public interface FolderFileVisitor {

        /**
         * Handles one file of the folder.
         *
         * @param file
         *            the file, readable until this returns
         * @throws IOException
         *             if the file cannot be handled
         */
        void visit(FolderFile file) throws IOException;
    }

    private ResourceFolderUtil() {
    }

    /**
     * Visits the files directly inside the given folder on the classpath.
     * <p>
     * Folders inside the folder are not visited, and neither are the files in
     * them.
     *
     * @param folder
     *            the folder to visit the files of, as a class loader reports it
     * @param visitor
     *            called for each file in the folder
     * @throws IOException
     *             if the folder cannot be listed, for example because it is not
     *             an existing folder
     */
    public static void visitFiles(URL folder, FolderFileVisitor visitor)
            throws IOException {
        switch (folder.getProtocol()) {
        case "file" -> visitFilesInFolder(folder, visitor);
        // wsjar is the protocol OpenLiberty uses for a jar
        case "jar", "wsjar" -> visitFilesInJar(folder, visitor);
        // WildFly serves a deployed archive through a virtual file system
        case JBossVfsUtil.PROTOCOL -> visitFilesInVfsFolder(folder, visitor);
        // Any other protocol, such as the vfsfile of an exploded WildFly
        // deployment or the bundleresource of OSGi, is served from a folder
        // that the URL points at
        default -> visitFilesInFolder(folder, visitor);
        }
    }

    private static void visitFilesInFolder(URL folder,
            FolderFileVisitor visitor) throws IOException {
        Path path;
        try {
            path = Path.of(folder.toURI());
        } catch (URISyntaxException | IllegalArgumentException
                | FileSystemNotFoundException e) {
            // Not a URL of a file system that is available, such as an
            // exploded deployment served through a protocol of its own, so
            // the path is taken from the URL as is
            getLogger().debug("Reading '{}' as a path instead of a file URL",
                    folder, e);
            try {
                path = Path.of(UrlUtil.decodeURIComponent(folder.getFile()));
            } catch (InvalidPathException pathException) {
                throw new IOException(
                        "Unable to list the files in '" + folder
                                + "' as it is not a folder that can be read",
                        pathException);
            }
        }
        try (Stream<Path> files = Files.list(path)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                visitor.visit(new FileSystemFile(file));
            }
        }
    }

    private static void visitFilesInJar(URL folder, FolderFileVisitor visitor)
            throws IOException {
        String folderEntry = getFolderEntryName(folder);
        try (JarFile jarFile = openJarFile(folder)) {
            for (JarEntry entry : jarFile.stream().filter(
                    entry -> isFileInFolder(folderEntry, entry.getName()))
                    .toList()) {
                visitor.visit(new JarFileEntry(folder, jarFile, entry,
                        folderEntry.length()));
            }
        }
    }

    private static void visitFilesInVfsFolder(URL folder,
            FolderFileVisitor visitor) throws IOException {
        // Read as they are, so that the virtual file system does not have to
        // create them on disk first
        for (JBossVfsUtil.VfsFile file : JBossVfsUtil.listFiles(folder)) {
            visitor.visit(new VfsFolderFile(folder, file));
        }
    }

    /**
     * Gets the name of the jar entry the folder URL points at, which is the
     * part after the last {@code !} separator, without the leading slash. The
     * separator appears more than once for a jar nested inside another jar.
     */
    private static String getFolderEntryName(URL folder) {
        String file = folder.getFile();
        int separatorIndex = file.lastIndexOf("!/");
        String entryName = separatorIndex == -1 ? ""
                : file.substring(separatorIndex + 2);
        entryName = UrlUtil.decodeURIComponent(entryName);
        return entryName.isEmpty() || entryName.endsWith("/") ? entryName
                : entryName + "/";
    }

    /**
     * Checks that the jar entry is a file directly inside the folder.
     */
    private static boolean isFileInFolder(String folderEntry,
            String entryName) {
        return entryName.startsWith(folderEntry)
                && entryName.length() > folderEntry.length()
                && entryName.indexOf('/', folderEntry.length()) == -1;
    }

    /**
     * Opens the jar that a {@code jar:} or {@code wsjar:} resource lives in.
     * <p>
     * The jar is opened through the connection of the resource, so that a jar
     * which is not a plain file on disk, such as one nested inside a Spring
     * Boot executable jar, is read by the handler that knows how to reach it.
     * Only where the connection does not give access to a jar is the location
     * resolved into a file instead.
     *
     * @param resource
     *            the resource URL to open the jar for
     * @return the jar containing the resource, to be closed by the caller
     * @throws IOException
     *             if the jar cannot be opened
     */
    private static JarFile openJarFile(URL resource) throws IOException {
        try {
            URLConnection connection = resource.openConnection();
            if (connection instanceof JarURLConnection jarConnection) {
                // The caller closes the jar, so it must not be the instance
                // shared through the connection cache
                jarConnection.setUseCaches(false);
                return jarConnection.getJarFile();
            }
            getLogger().debug(
                    "Resource '{}' is not served by a jar connection, resolving its jar as a file instead",
                    resource);
        } catch (IOException | RuntimeException e) {
            getLogger().debug(
                    "Cannot open the jar of resource '{}' through its connection, resolving it as a file instead",
                    resource, e);
        }
        return new JarFile(getJarFile(resource));
    }

    /**
     * Resolves the jar file that a {@code jar:} or {@code wsjar:} resource
     * lives in.
     * <p>
     * The jar location is the part of the URL before the {@code !} separator,
     * and it is percent-encoded just like any other URL.
     *
     * @param resource
     *            the resource URL to resolve the jar for
     * @return the jar file containing the resource
     */
    private static File getJarFile(URL resource) {
        String file = resource.getFile();
        int separatorIndex = file.indexOf('!');
        String jarUrl = separatorIndex == -1 ? file
                : file.substring(0, separatorIndex);
        try {
            return Paths.get(URI.create(jarUrl)).toFile();
        } catch (IllegalArgumentException | FileSystemNotFoundException
                | UnsupportedOperationException e) {
            // Not a plain file: URL, for example a jar nested inside a war, or
            // a path on a file system of its own that has no file to point at
            getLogger().debug(
                    "Cannot resolve a file path for the jar of resource '{}'",
                    resource, e);
            return new File(UrlUtil.decodeURIComponent(jarUrl));
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ResourceFolderUtil.class);
    }

    private record VfsFolderFile(URL folder,
            JBossVfsUtil.VfsFile file) implements FolderFile {

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getLocation() {
            String folderUrl = folder.toExternalForm();
            return folderUrl.endsWith("/") ? folderUrl + getName()
                    : folderUrl + "/" + getName();
        }

        @Override
        public InputStream open() throws IOException {
            return file.open();
        }
    }

    private record FileSystemFile(Path path) implements FolderFile {

        @Override
        public String getName() {
            return path.getFileName().toString();
        }

        @Override
        public String getLocation() {
            return path.toUri().toString();
        }

        @Override
        public InputStream open() throws IOException {
            return new FileInputStream(path.toFile());
        }
    }

    private record JarFileEntry(URL folder, JarFile jarFile, JarEntry entry,
            int folderEntryLength) implements FolderFile {

        @Override
        public String getName() {
            return entry.getName().substring(folderEntryLength);
        }

        @Override
        public String getLocation() {
            String folderUrl = folder.toExternalForm();
            return folderUrl.endsWith("/") ? folderUrl + getName()
                    : folderUrl + "/" + getName();
        }

        @Override
        public InputStream open() throws IOException {
            return jarFile.getInputStream(entry);
        }
    }
}
