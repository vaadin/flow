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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads what WildFly and JBoss serve from their virtual file system, through
 * the {@code vfs} protocol.
 * <p>
 * The virtual file system is reached with reflection, as a dependency to
 * WildFly or JBoss cannot be afforded, and a virtual file only exists on disk
 * once its physical file has been asked for.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.3
 */
public final class JBossVfsUtil {

    /**
     * The protocol WildFly and JBoss serve a deployed archive through.
     */
    public static final String PROTOCOL = "vfs";

    private static final String JAR_SUFFIX = ".jar";

    private JBossVfsUtil() {
    }

    /**
     * A file of the virtual file system, read without creating it on disk.
     */
    public interface VfsFile {

        /**
         * Gets the name of the file, without the folder it is in.
         *
         * @return the file name
         */
        String getName();

        /**
         * Opens the content of the file.
         *
         * @return the content of the file, to be closed by the caller
         * @throws IOException
         *             if the content cannot be read
         */
        InputStream open() throws IOException;
    }

    private record VirtualFileEntry(String name,
            Object virtualFile) implements VfsFile {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream open() throws IOException {
            return openStream(virtualFile);
        }
    }

    /**
     * Gets the files directly inside the given folder, to be read as they are
     * without creating anything on disk.
     * <p>
     * The folders of the folder and what is inside them are not included.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the files of the folder
     * @throws IOException
     *             if the folder cannot be read
     */
    public static List<VfsFile> listFiles(URL folder) throws IOException {
        List<VfsFile> files = new ArrayList<>();
        for (Object child : getChildren(getVirtualFile(folder), false)) {
            if (isFile(child)) {
                files.add(new VirtualFileEntry(getName(child), child));
            }
        }
        return files;
    }

    /**
     * Gets the given folder as a folder on disk, creating it and everything
     * inside it, so that the caller can read the folder as a whole.
     * <p>
     * The caller only gets what the virtual file system created inside the
     * folder. A mount that creates something of its own elsewhere, such as a
     * jar inside the folder, is not part of the folder that is returned, which
     * is not expected to happen and is warned about.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the folder on disk
     * @throws IOException
     *             if the folder cannot be read
     */
    public static File materializeFolder(URL folder) throws IOException {
        Object virtualFolder = getVirtualFile(folder);
        // A virtual file only exists on disk once it has been asked for, so
        // everything below the folder is asked for even though only the folder
        // is returned
        List<File> files = new ArrayList<>();
        for (Object child : getChildren(virtualFolder, true)) {
            files.add(getPhysicalFile(child));
        }
        File physicalFolder = getPhysicalFile(virtualFolder);
        files.stream().filter(
                file -> !file.toPath().startsWith(physicalFolder.toPath()))
                .forEach(file -> getLogger().warn(
                        "'{}' of '{}' was created as '{}', which is not inside the folder '{}' that is read."
                                + " What it contains is not seen by the build.",
                        file.getName(), folder, file, physicalFolder));
        return physicalFolder;
    }

    /**
     * Packs the given jar into a jar file on disk, as a jar of the virtual file
     * system is not a file the caller can open on its own.
     * <p>
     * The jar is written into a temporary folder of its own, rather than into
     * the temporary folder shared by everything on the machine, and both are
     * deleted when the JVM exits.
     *
     * @param jar
     *            the {@code vfs} URL of the jar
     * @return the jar file on disk
     * @throws IOException
     *             if the jar cannot be read or written
     */
    public static File materializeJar(URL jar) throws IOException {
        String jarPath = jar.toString();
        if (!jarPath.endsWith(JAR_SUFFIX)) {
            throw new IOException("'" + jar + "' is not the URL of a jar");
        }
        Object virtualJar = getVirtualFile(jar);
        String fileNamePrefix = jarPath.substring(
                jarPath.lastIndexOf(jarPath.contains("\\") ? '\\' : '/') + 1,
                jarPath.length() - JAR_SUFFIX.length());
        // A folder of its own inside the temporary folder of the machine,
        // created for this jar alone, and with the permissions of its owner
        // where the file system has them
        Path folder = Files.createTempDirectory("vaadin-jboss-vfs"); // NOSONAR
        Path jarFile = folder.resolve(fileNamePrefix + ".jar");
        // The caller reads the jar for as long as the JVM runs. Both are
        // registered before the jar is written, as a folder is only deleted
        // once what was registered after it is gone, and a jar that could not
        // be written has to go as well
        folder.toFile().deleteOnExit(); // NOSONAR
        jarFile.toFile().deleteOnExit(); // NOSONAR
        writeJar(virtualJar, jarFile);
        return jarFile.toFile();
    }

    private static void writeJar(Object virtualJar, Path jarFile)
            throws IOException {
        try (ZipOutputStream jarStream = new ZipOutputStream(
                Files.newOutputStream(jarFile))) {
            for (Object child : getChildren(virtualJar, true)) {
                if (!isFile(child)) {
                    continue;
                }
                jarStream.putNextEntry(
                        new ZipEntry(getPathNameRelativeTo(child, virtualJar)));
                try (InputStream content = openStream(child)) {
                    content.transferTo(jarStream);
                }
                jarStream.closeEntry();
            }
        }
    }

    private static Object getVirtualFile(URL url) throws IOException {
        Object virtualFile = url.openConnection().getContent();
        if (virtualFile == null) {
            throw new IOException(
                    "'" + url + "' does not serve a JBoss virtual file");
        }
        return virtualFile;
    }

    private static List<?> getChildren(Object virtualFile, boolean recursive)
            throws IOException {
        Object children = invoke(virtualFile,
                recursive ? "getChildrenRecursively" : "getChildren",
                "list the contents");
        if (!(children instanceof List<?> childList)) {
            throw new IOException("The contents of the JBoss virtual file '"
                    + virtualFile + "' are not a list of virtual files");
        }
        return childList;
    }

    private static File getPhysicalFile(Object virtualFile) throws IOException {
        Object physicalFile = invoke(virtualFile, "getPhysicalFile",
                "create the file on disk");
        if (!(physicalFile instanceof File file)) {
            throw new IOException("The file created on disk for the JBoss"
                    + " virtual file '" + virtualFile + "' is not a file");
        }
        return file;
    }

    private static String getName(Object virtualFile) throws IOException {
        Object name = invoke(virtualFile, "getName", "read the name");
        if (!(name instanceof String fileName)) {
            throw new IOException("The name of the JBoss virtual file '"
                    + virtualFile + "' is not a name");
        }
        return fileName;
    }

    private static boolean isFile(Object virtualFile) throws IOException {
        Object isFile = invoke(virtualFile, "isFile",
                "tell files from folders");
        if (!(isFile instanceof Boolean file)) {
            throw new IOException("The JBoss virtual file '" + virtualFile
                    + "' does not tell whether it is a file");
        }
        return file;
    }

    private static InputStream openStream(Object virtualFile)
            throws IOException {
        Object content = invoke(virtualFile, "openStream", "read the content");
        if (!(content instanceof InputStream stream)) {
            throw new IOException("The content of the JBoss virtual file '"
                    + virtualFile + "' cannot be read");
        }
        return stream;
    }

    private static String getPathNameRelativeTo(Object virtualFile,
            Object parent) throws IOException {
        try {
            Method method = virtualFile.getClass()
                    .getMethod("getPathNameRelativeTo", parent.getClass());
            Object path = method.invoke(virtualFile, parent);
            if (!(path instanceof String pathName)) {
                throw new IOException("The path of the JBoss virtual file '"
                        + virtualFile + "' is not a path");
            }
            return pathName;
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            throw failedToInvoke(virtualFile, "resolve the path", e);
        }
    }

    /**
     * Invokes a method of the virtual file, turning a call that does not get
     * through into an {@link IOException}, as an object that is not the virtual
     * file the protocol is expected to serve is something that cannot be read.
     */
    private static Object invoke(Object virtualFile, String methodName,
            String what) throws IOException {
        try {
            Method method = virtualFile.getClass().getMethod(methodName);
            return method.invoke(virtualFile);
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            throw failedToInvoke(virtualFile, what, e);
        }
    }

    private static IOException failedToInvoke(Object virtualFile, String what,
            Exception cause) {
        return new IOException("Unable to " + what
                + " of the JBoss virtual file '" + virtualFile + "'", cause);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(JBossVfsUtil.class);
    }
}
