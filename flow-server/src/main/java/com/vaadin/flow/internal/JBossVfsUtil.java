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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the folders that WildFly and JBoss serve from their virtual file
 * system, through the {@code vfs} protocol.
 * <p>
 * The virtual file system is reached with reflection, as a dependency to
 * WildFly or JBoss cannot be afforded, and the files of a folder only exist on
 * disk once they have been asked for.
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
     * Creates the folder and the files in it on disk.
     * <p>
     * The files are created inside the folder that is returned, so that they
     * can be read through it.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the folder on disk
     * @throws IOException
     *             if the folder cannot be read, or its files are not created
     *             inside it
     */
    public static File materializeFolder(URL folder) throws IOException {
        return materialize(folder, "getChildren");
    }

    /**
     * Creates the folder and everything below it on disk, so that the files of
     * the folders in it can be read as well.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the folder on disk
     * @throws IOException
     *             if the folder cannot be read, or its files are not created
     *             inside it
     */
    public static File materializeFolderTree(URL folder) throws IOException {
        return materialize(folder, "getChildrenRecursively");
    }

    private static File materialize(URL folder, String childrenMethod)
            throws IOException {
        Object virtualFolder = getVirtualFile(folder);
        // A virtual file only exists on disk once it has been asked for, so
        // every child is asked for even though only the folder is returned
        List<File> files = new ArrayList<>();
        for (Object child : getChildren(virtualFolder, childrenMethod)) {
            files.add(getPhysicalFile(child));
        }
        File physicalFolder = getPhysicalFile(virtualFolder);
        for (File file : files) {
            // The caller reads the files through the folder, so a file that
            // was created elsewhere would go unnoticed
            if (!file.toPath().startsWith(physicalFolder.toPath())) {
                throw new IOException("'" + folder + "' was created as '"
                        + physicalFolder
                        + "', which does not contain its file '" + file + "'");
            }
        }
        return physicalFolder;
    }

    private static Object getVirtualFile(URL folder) throws IOException {
        Object virtualFile = folder.openConnection().getContent();
        if (virtualFile == null) {
            throw new IOException(
                    "'" + folder + "' does not serve a JBoss virtual file");
        }
        return virtualFile;
    }

    private static List<?> getChildren(Object virtualFile, String methodName)
            throws IOException {
        Object children = invoke(virtualFile, methodName);
        if (!(children instanceof List<?> childList)) {
            throw new IOException("The JBoss VFS API method " + methodName
                    + " of '" + virtualFile + "' did not return a list");
        }
        return childList;
    }

    private static File getPhysicalFile(Object virtualFile) throws IOException {
        Object physicalFile = invoke(virtualFile, "getPhysicalFile");
        if (!(physicalFile instanceof File file)) {
            throw new IOException(
                    "The JBoss VFS API method getPhysicalFile of '"
                            + virtualFile + "' did not return a file");
        }
        return file;
    }

    /**
     * Invokes a method of the virtual file, turning a call that does not get
     * through into an {@link IOException}, as an object that is not the virtual
     * file the protocol is expected to serve is a folder that cannot be read.
     */
    private static Object invoke(Object virtualFile, String methodName)
            throws IOException {
        try {
            Method method = virtualFile.getClass().getMethod(methodName);
            return method.invoke(virtualFile);
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            throw new IOException("Failed to invoke the JBoss VFS API method "
                    + methodName + " for '" + virtualFile + "'", e);
        }
    }
}
