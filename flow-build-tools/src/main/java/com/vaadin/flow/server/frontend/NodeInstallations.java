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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FileIOUtils;

/**
 * Keeps track of when the Node.js installations in the alternative install
 * directory (typically {@code ~/.vaadin}) were last used, and removes the ones
 * that have not been used for a long time.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class NodeInstallations {

    /**
     * Name of the marker file written into an installation directory every time
     * that installation is taken into use.
     */
    static final String LAST_USED_FILE = "last-used";

    /**
     * Prefix shared by all auto-installed Node.js directories, e.g.
     * {@code node-v24.10.0}, and by their archives, e.g.
     * {@code node-v24.10.0-linux-x64.tar.gz}.
     */
    static final String INSTALLATION_PREFIX = "node-v";

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
     * Records the current time in the {@value #LAST_USED_FILE} file of the
     * given installation directory.
     * <p>
     * Failures are logged and otherwise ignored, as not being able to write the
     * marker must never break the build.
     *
     * @param installationDirectory
     *            the Node.js installation that is being used
     */
    static void markUsed(File installationDirectory) {
        if (!installationDirectory.isDirectory()) {
            return;
        }
        File marker = new File(installationDirectory, LAST_USED_FILE);
        try {
            Files.writeString(marker.toPath(), Instant.now().toString(),
                    StandardCharsets.UTF_8);
        } catch (IOException | UncheckedIOException e) {
            getLogger().debug("Could not update {}", marker, e);
        }
    }

    /**
     * Removes the Node.js installations in {@code rootDirectory} that have not
     * been used for the last {@value #UNUSED_RETENTION_MONTHS} months, and any
     * archive left over from installing a Node.js version.
     * <p>
     * Only directories and archives following the {@value #INSTALLATION_PREFIX}
     * naming of an auto-installed Node.js are touched; anything else in the
     * directory is left alone.
     * <p>
     * The installation currently in use is never removed, and neither is the
     * last remaining installation, so the directory always keeps at least one
     * Node.js version. An installation without a usable
     * {@value #LAST_USED_FILE} marker is not removed either: nothing is known
     * about when it was last used, so it gets a marker and the retention period
     * starts from now.
     *
     * @param rootDirectory
     *            the directory holding the Node.js installations, typically
     *            {@code ~/.vaadin}
     * @param installationInUse
     *            the installation that was just installed or taken into use,
     *            never removed
     */
    static void removeUnusedInstallations(File rootDirectory,
            File installationInUse) {
        removeLeftoverArchives(rootDirectory);

        File[] installations = rootDirectory
                .listFiles(file -> file.isDirectory()
                        && file.getName().startsWith(INSTALLATION_PREFIX));
        if (installations == null || installations.length < 2) {
            // Nothing to clean up, and the only installation is always kept
            return;
        }

        Instant threshold = ZonedDateTime.now()
                .minusMonths(UNUSED_RETENTION_MONTHS).toInstant();
        List<File> unused = new ArrayList<>();
        for (File installation : installations) {
            if (isSameDirectory(installation, installationInUse)) {
                continue;
            }
            Optional<Instant> lastUsed = lastUsed(installation);
            if (lastUsed.isEmpty()) {
                // Installed by a Flow version that did not write the marker,
                // so there is no way to tell whether it is in active use.
                // Start the retention period now instead of assuming stale.
                getLogger().debug(
                        "Node.js installation {} has no {} marker, starting its retention period now",
                        installation, LAST_USED_FILE);
                markUsed(installation);
            } else if (lastUsed.get().isBefore(threshold)) {
                unused.add(installation);
            }
        }

        // Never remove the last version in the folder, even if every
        // installation looks stale
        if (unused.size() == installations.length) {
            unused.sort(
                    Comparator.comparing(installation -> lastUsed(installation)
                            .orElse(Instant.MIN)));
            unused.remove(unused.size() - 1);
        }

        for (File installation : unused) {
            remove(installation);
        }
    }

    private static void remove(File installation) {
        getLogger().info(
                "Removing Node.js installation {} which has not been used for over {} months",
                installation, UNUSED_RETENTION_MONTHS);
        if (!FileIOUtils.deleteQuietly(installation)) {
            getLogger().warn(
                    "Could not remove the unused Node.js installation {}. It can be deleted manually to free up disk space.",
                    installation);
        }
    }

    /**
     * Removes the Node.js archives left behind by Flow versions that did not
     * delete the archive after unpacking it. The archive of
     * {@code node-v24.10.0} is named e.g.
     * {@code node-v24.10.0-linux-x64.tar.gz} and is of no use once it has been
     * unpacked, whether or not its installation is still around.
     */
    private static void removeLeftoverArchives(File rootDirectory) {
        Instant youngest = Instant.now().minus(ARCHIVE_GRACE_PERIOD);
        File[] archives = rootDirectory.listFiles(file -> file.isFile()
                && isArchiveName(file.getName()) && Instant
                        .ofEpochMilli(file.lastModified()).isBefore(youngest));
        if (archives == null) {
            return;
        }
        for (File archive : archives) {
            getLogger().debug("Removing leftover Node.js archive {}", archive);
            FileIOUtils.deleteQuietly(archive);
        }
    }

    private static boolean isArchiveName(String name) {
        return name.startsWith(INSTALLATION_PREFIX) && ARCHIVE_EXTENSIONS
                .stream().anyMatch(extension -> name.endsWith(extension));
    }

    /**
     * Resolves when the given installation was last used, or an empty optional
     * if it has no usable marker, which is the case for installations created
     * before the marker was introduced.
     */
    private static Optional<Instant> lastUsed(File installation) {
        File marker = new File(installation, LAST_USED_FILE);
        if (!marker.isFile()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(
                    Files.readString(marker.toPath(), StandardCharsets.UTF_8)
                            .trim()));
        } catch (IOException | UncheckedIOException
                | DateTimeParseException e) {
            getLogger().debug("Could not read {}", marker, e);
            return Optional.empty();
        }
    }

    private static boolean isSameDirectory(File one, File other) {
        if (other == null) {
            return false;
        }
        try {
            return one.getCanonicalFile().equals(other.getCanonicalFile());
        } catch (IOException e) {
            getLogger().debug("Could not compare {} and {}", one, other, e);
            return one.getAbsoluteFile().equals(other.getAbsoluteFile());
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(NodeInstallations.class);
    }
}
