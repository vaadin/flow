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

    private JBossVfsUtil() {
    }

    /**
     * Gets the files of the given folder, creating them on disk.
     * <p>
     * The files of the folders inside the folder are not included.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the files of the folder, in the places they were created
     * @throws IOException
     *             if the folder cannot be read
     */
    public static List<File> materializeFiles(URL folder) throws IOException {
        return materializeChildren(getVirtualFile(folder), false);
    }

    /**
     * Gets the given folder as a folder on disk, creating it and everything
     * inside it, so that the caller can read the folder as a whole.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the folder on disk
     * @throws IOException
     *             if the folder cannot be read
     */
    public static File materializeFolder(URL folder) throws IOException {
        Object virtualFolder = getVirtualFile(folder);
        materializeChildren(virtualFolder, true);
        return getPhysicalFile(virtualFolder);
    }

    /**
     * Packs the given jar into a jar file on disk, as a jar of the virtual file
     * system is not a file the caller can open on its own.
     * <p>
     * The jar is written to a temporary file that is deleted when the JVM
     * exits.
     *
     * @param jar
     *            the {@code vfs} URL of the jar
     * @return the jar file on disk
     * @throws IOException
     *             if the jar cannot be read or written
     */
    public static File materializeJar(URL jar) throws IOException {
        Object virtualJar = getVirtualFile(jar);
        String jarPath = jar.toString();
        String fileNamePrefix = jarPath.substring(
                jarPath.lastIndexOf(jarPath.contains("\\") ? '\\' : '/') + 1,
                jarPath.lastIndexOf(".jar"));
        Path jarFile = Files.createTempFile(fileNamePrefix, ".jar");
        writeJar(virtualJar, jarFile);
        jarFile.toFile().deleteOnExit();
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

    private static List<File> materializeChildren(Object virtualFolder,
            boolean recursive) throws IOException {
        List<File> files = new ArrayList<>();
        for (Object child : getChildren(virtualFolder, recursive)) {
            files.add(getPhysicalFile(child));
        }
        return files;
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

    private static boolean isFile(Object virtualFile) throws IOException {
        return Boolean.TRUE.equals(
                invoke(virtualFile, "isFile", "tell files from folders"));
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
}
