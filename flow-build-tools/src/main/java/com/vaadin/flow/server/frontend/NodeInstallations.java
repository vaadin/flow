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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Finds the {@link NodeInstallation}s of an install directory, typically
 * {@code ~/.vaadin}.
 * <p>
 * Everything that concerns a single installation lives in
 * {@link NodeInstallation}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class NodeInstallations {

    private NodeInstallations() {
        // Static helpers only
    }

    /**
     * Finds the Node.js installations in the given install directory.
     *
     * @param installDirectory
     *            the directory holding the installations, typically
     *            {@code ~/.vaadin}
     * @return the installations, in no particular order
     */
    static List<NodeInstallation> findAll(File installDirectory) {
        File[] directories = installDirectory
                .listFiles(file -> file.isDirectory() && file.getName()
                        .startsWith(NodeInstallation.DIRECTORY_PREFIX));
        if (directories == null) {
            return List.of();
        }
        return Arrays.stream(directories).map(NodeInstallation::new).toList();
    }
}
