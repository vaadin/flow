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
package com.vaadin.flow.devloop.daemon;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * What Maven itself resolved a module to be, as the build left it behind.
 * <p>
 * Whether a profile is active, and what a module inherits from a parent outside
 * the checkout, are Maven's to decide, while {@link Reactor} reads poms with
 * the JDK alone. Both are settled facts inside Maven by the time the dev loop's
 * own build extension runs, so it writes them down - see
 * {@code DevLoopBuildExtension#writeModel} - and this reads them back. That is
 * how the daemon knows what starts an application rather than guessing at it.
 * <p>
 * There is nothing here until a build has run, and nothing needs there to be:
 * the daemon resolves before it launches anything. A {@code status} on a
 * project nothing has built yet is the one caller that finds none, and it
 * leaves the runtime unnamed.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class EffectiveModel {

    /**
     * Where the extension leaves it, relative to the module directory.
     * <p>
     * A fixed path rather than the module's configured build directory, which
     * Maven alone knows: a project that moves it would otherwise be written in
     * one place and read in another, and the model would look missing to a
     * daemon that has no way to ask. The same literal is
     * {@code DevLoopBuildExtension#MODEL_FILE} on the writing side, and it is
     * where the rest of the daemon's per-module files live - see
     * {@link Launch#workDir}.
     */
    static final String FILE = "target/devloop/model.properties";

    private final Properties values;

    private EffectiveModel(Properties values) {
        this.values = values;
    }

    /**
     * Maven's answer for one module, when there is a current one.
     * <p>
     * Current means newer than every pom it was built from. A pom edit is
     * exactly what moves the answer, and the daemon re-resolves for one - but
     * between the edit and the resolve the file on disk describes the project
     * as it was, and a stale answer is worse than none: the whole point of
     * reading it is that it is more trustworthy than the poms.
     *
     * @param moduleDir
     *            the module whose model to read
     * @param poms
     *            the poms the module is built from, its own and its ancestors'
     * @return the model, or empty when there is none or it has been overtaken
     */
    static Optional<EffectiveModel> read(Path moduleDir,
            Collection<Path> poms) {
        Path file = moduleDir.resolve(FILE);
        try {
            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }
            long written = Files.getLastModifiedTime(file).toMillis();
            for (Path pom : poms) {
                if (Files.isRegularFile(pom) && Files.getLastModifiedTime(pom)
                        .toMillis() > written) {
                    return Optional.empty();
                }
            }
            Properties values = new Properties();
            try (Reader reader = Files.newBufferedReader(file)) {
                values.load(reader);
            }
            return Optional.of(new EffectiveModel(values));
        } catch (IOException | IllegalArgumentException e) {
            // A half-written or hand-edited file is not worth a failure: the
            // poms are still there to read.
            return Optional.empty();
        }
    }

    /**
     * A plugin this build runs, as Maven assembled it.
     * <p>
     * Only {@code <build><plugins>} is in here - Maven's own
     * {@code getBuildPlugins} - so a version pinned in
     * {@code <pluginManagement>} is absent by construction, and a plugin from a
     * profile is present exactly when that profile was active - which is the
     * distinction reading the pom cannot draw, and why {@link Reactor#plugin}
     * reads nothing else.
     *
     * @param groupId
     *            the plugin's group
     * @param artifactId
     *            the plugin's artifact
     * @return the plugin, or empty when this build does not run it
     */
    Optional<Reactor.PluginConfig> plugin(String groupId, String artifactId) {
        for (int index = 0; index < count(); index++) {
            String key = "plugin." + index;
            String[] coordinates = values.getProperty(key, "").split(":", 3);
            if (coordinates.length < 2 || !groupId.equals(coordinates[0])
                    || !artifactId.equals(coordinates[1])) {
                continue;
            }
            return Optional.of(new Reactor.PluginConfig(groupId, artifactId,
                    coordinates.length < 3 ? "" : coordinates[2],
                    configurationOf(key)));
        }
        return Optional.empty();
    }

    /** The profiles this build ran with, for a log line that has to say why. */
    List<String> activeProfiles() {
        String profiles = values.getProperty("profiles", "");
        return profiles.isBlank() ? List.of() : List.of(profiles.split(","));
    }

    private int count() {
        try {
            return Integer.parseInt(values.getProperty("plugins", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Map<String, String> configurationOf(String key) {
        Map<String, String> configuration = new LinkedHashMap<>();
        String prefix = key + ".";
        List<String> names = new ArrayList<>(values.stringPropertyNames()
                .stream().filter(name -> name.startsWith(prefix)).sorted()
                .toList());
        names.forEach(name -> configuration.put(name.substring(prefix.length()),
                values.getProperty(name)));
        return configuration;
    }
}
