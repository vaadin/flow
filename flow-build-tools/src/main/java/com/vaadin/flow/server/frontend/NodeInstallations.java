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
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FileIOUtils;

/**
 * Finds the {@link NodeInstallation}s and the leftover Node.js archives of an
 * install directory, typically {@code ~/.vaadin}, and cleans up the ones that
 * are no longer needed.
 * <p>
 * Everything that concerns a single installation lives in
 * {@link NodeInstallation}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class NodeInstallations {

    /**
     * Installations that have not been used for this long are removed once a
     * new Node.js version is installed.
     */
    static final int UNUSED_RETENTION_MONTHS = 6;

    /**
     * Archives younger than this are left alone, so that an archive another
     * process is downloading right now is never deleted from under it. Archives
     * left behind by older Flow versions are always much older than this.
     */
    private static final Duration ARCHIVE_GRACE_PERIOD = Duration.ofDays(1);

    private static final List<String> ARCHIVE_EXTENSIONS = List.of(".tar.gz",
            ".tar.xz", ".zip", ".msi");

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

    /**
     * Finds the Node.js archives that were left behind in the given install
     * directory by Flow versions that did not delete the archive after
     * unpacking it.
     * <p>
     * Only archives of a version that is installed are reported, since those
     * are the ones that have been unpacked and are of no further use. An
     * archive of a version that is not installed may well have been put there
     * on purpose, for instance to install without network access. Archives
     * modified within the last day are left alone too, so that a download
     * running in another process is never mistaken for a leftover.
     *
     * @param installDirectory
     *            the directory holding the installations, typically
     *            {@code ~/.vaadin}
     * @return the leftover archives, in no particular order
     */
    static List<File> findLeftoverArchives(File installDirectory) {
        List<String> unpacked = findAll(installDirectory).stream().map(
                installation -> installation.getDirectory().getName() + "-")
                .toList();
        Instant youngest = Instant.now().minus(ARCHIVE_GRACE_PERIOD);

        File[] archives = installDirectory.listFiles(
                file -> file.isFile() && isArchiveName(file.getName())
                        && unpacked.stream()
                                .anyMatch(file.getName()::startsWith)
                        && Instant.ofEpochMilli(file.lastModified())
                                .isBefore(youngest));
        return archives == null ? List.of() : Arrays.asList(archives);
    }

    /**
     * Removes the installations in the given install directory that have not
     * been used for the last {@value #UNUSED_RETENTION_MONTHS} months, and the
     * leftover archives of every version.
     * <p>
     * Only directories and archives following the
     * {@value NodeInstallation#DIRECTORY_PREFIX} naming of an auto-installed
     * Node.js are touched; anything else in the directory is left alone.
     * <p>
     * The installation currently in use is never removed, and neither is the
     * last remaining installation, so the directory always keeps at least one
     * Node.js version. An installation whose last use is unknown is not removed
     * either: it gets a marker so that its retention period starts from now.
     *
     * @param installDirectory
     *            the directory holding the installations, typically
     *            {@code ~/.vaadin}
     * @param installationInUse
     *            the installation that was just installed or taken into use,
     *            never removed
     */
    static void removeUnused(File installDirectory,
            NodeInstallation installationInUse) {
        findLeftoverArchives(installDirectory).forEach(archive -> {
            getLogger().debug("Removing leftover Node.js archive {}", archive);
            FileIOUtils.deleteQuietly(archive);
        });
        findInterruptedRemovals(installDirectory).forEach(remains -> {
            getLogger().debug("Removing the remains of {}", remains);
            FileIOUtils.deleteQuietly(remains);
        });

        List<NodeInstallation> installations = findAll(installDirectory);
        Instant threshold = ZonedDateTime.now()
                .minusMonths(UNUSED_RETENTION_MONTHS).toInstant();
        List<NodeInstallation> unused = new ArrayList<>();
        for (NodeInstallation installation : installations) {
            if (installation.equals(installationInUse)) {
                continue;
            }
            Optional<Instant> lastUsed = installation.getLastUsed();
            if (lastUsed.isEmpty()) {
                // Installed by a Flow version that did not write the marker,
                // so there is no way to tell whether it is in active use.
                // Start the retention period now instead of assuming stale.
                getLogger().debug(
                        "Node.js installation {} has no {} marker, starting its retention period now",
                        installation, NodeInstallation.LAST_USED_FILE);
                installation.markUsed();
            } else if (lastUsed.get().isBefore(threshold)) {
                unused.add(installation);
            }
        }

        // Never remove the last version in the folder, even if every
        // installation looks stale. Callers that pass the installation they
        // are using keep one that way already; this covers the ones that
        // clean up without having taken any installation into use.
        if (!unused.isEmpty() && unused.size() == installations.size()) {
            unused.sort(Comparator.comparing(installation -> installation
                    .getLastUsed().orElse(Instant.MIN)));
            unused.remove(unused.size() - 1);
        }

        unused.forEach(NodeInstallation::remove);
    }

    /**
     * Finds what a previous removal left behind because it could not delete
     * every file, see {@link NodeInstallation#remove()}.
     */
    private static List<File> findInterruptedRemovals(File installDirectory) {
        File[] remains = installDirectory.listFiles(file -> file.isDirectory()
                && file.getName().startsWith(NodeInstallation.REMOVED_PREFIX));
        return remains == null ? List.of() : Arrays.asList(remains);
    }

    private static boolean isArchiveName(String name) {
        return name.startsWith(NodeInstallation.DIRECTORY_PREFIX)
                && ARCHIVE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(NodeInstallations.class);
    }
}
