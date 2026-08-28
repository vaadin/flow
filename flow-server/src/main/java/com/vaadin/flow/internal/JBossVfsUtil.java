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
     * Gets the files of the given folder, creating them on disk.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the files of the folder
     * @throws IOException
     *             if the folder cannot be read
     */
    public static List<File> listFolder(URL folder) throws IOException {
        Object virtualFolder = getVirtualFile(folder);
        List<File> files = new ArrayList<>();
        for (Object child : getChildren(virtualFolder, "getChildren")) {
            // side effect: create real-world files
            files.add(getPhysicalFile(child));
        }
        return files;
    }

    /**
     * Gets the folder itself as a folder on disk, creating it and everything
     * inside it, so that its contents can be read as regular files.
     *
     * @param folder
     *            the {@code vfs} URL of the folder
     * @return the folder on disk
     * @throws IOException
     *             if the folder cannot be read
     */
    public static File materializeFolder(URL folder) throws IOException {
        Object virtualFolder = getVirtualFile(folder);
        // The physical files of the children only exist once they have been
        // asked for, and the caller reads them through the root folder
        for (Object child : getChildren(virtualFolder,
                "getChildrenRecursively")) {
            getPhysicalFile(child);
        }
        return getPhysicalFile(virtualFolder);
    }

    private static Object getVirtualFile(URL folder) throws IOException {
        return folder.openConnection().getContent();
    }

    private static List<?> getChildren(Object virtualFile, String methodName)
            throws IOException {
        return (List<?>) invoke(virtualFile, methodName);
    }

    private static File getPhysicalFile(Object virtualFile) throws IOException {
        return (File) invoke(virtualFile, "getPhysicalFile");
    }

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
