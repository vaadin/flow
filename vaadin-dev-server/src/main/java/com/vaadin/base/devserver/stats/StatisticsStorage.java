/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.base.devserver.stats;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.node.ObjectNode;
import com.vaadin.flow.server.Command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development more usage statistic storage and methods for updating the data.
 */
public class StatisticsStorage {

    private String projectId;
    File usageStatisticsFile;

    /**
     * Creates an instance.
     */
    public StatisticsStorage() {
        // Intentionally empty
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(StatisticsStorage.class);
    }

    /**
     * Sets the active project id.
     * <p>
     * The project id should be unique enough to avoid collisions and data
     * overwrites.
     * <p>
     * Used in {@link #update(BiConsumer)}.
     *
     * @param projectId
     *            The unique project id.
     */
    void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /*
     * Gets the id for the active project.
     */
    String getProjectId() {
        return projectId;
    }

    /**
     * Runs the given command with the store locked.
     *
     * @param whenLocked
     *            the command to run
     */
    public void access(Command whenLocked) {
        synchronized (DevModeUsageStatistics.class) { // Lock data for init
            whenLocked.execute();
        }
    }

    /**
     * Updates the store in a safe way.
     *
     * @param updater
     *            the update logic which receives a global and a project
     *            specific container to update
     */
    void update(BiConsumer<StatisticsContainer, StatisticsContainer> updater) {
        access(() -> {
            ObjectNode fullJson = internalRead();
            ObjectNode projectJson = getProjectData(fullJson, projectId);

            updater.accept(new StatisticsContainer(fullJson),
                    new StatisticsContainer(projectJson));
            internalWrite(fullJson);
        });
    }

    private static ObjectNode getProjectData(ObjectNode fullJson,
            String projectId) {
        if (projectId == null) {
            return null;
        }
        return JsonHelpers.getOrCreate(projectId,
                fullJson.get(StatisticsConstants.FIELD_PROJECTS),
                StatisticsConstants.FIELD_PROJECT_ID, true);
    }

    /**
     * Reads all data from the statistics file.
     *
     * @return
     *
     * @see #getUsageStatisticsFile()
     */
    ObjectNode read() {
        AtomicReference<ObjectNode> data = new AtomicReference<>(null);
        access(() -> data.set(internalRead()));
        return data.get();
    }

    /**
     * Reads the active project data from the statistics file.
     *
     * @return
     *
     * @see #getUsageStatisticsFile()
     */
    ObjectNode readProject() {
        ObjectNode data = read();
        return getProjectData(data, projectId);
    }

    /**
     * Read the data from local project statistics file.
     *
     * @return
     *
     * @see #getUsageStatisticsFile()
     */
    private ObjectNode internalRead() {
        File file = getUsageStatisticsFile();
        getLogger().debug("Reading statistics from {}", file.getAbsolutePath());
        try {
            if (file.exists()) {
                return (ObjectNode) JsonHelpers.getJsonMapper().readTree(file);
            }
        } catch (Exception e) {
            getLogger().debug("Failed to parse json", e);
        }

        // Empty node if nothing is found
        ObjectNode json = JsonHelpers.getJsonMapper().createObjectNode();
        json.set(StatisticsConstants.FIELD_PROJECTS,
                JsonHelpers.getJsonMapper().createArrayNode());
        return json;
    }

    /**
     * Writes the data to local project statistics json file.
     *
     * @see #getUsageStatisticsFile()
     */
    private void internalWrite(ObjectNode json) {
        try {
            getUsageStatisticsFile().getParentFile().mkdirs();
            JsonHelpers.getJsonMapper().writeValue(getUsageStatisticsFile(),
                    json);
        } catch (JacksonException e) {
            getLogger().debug("Failed to write json", e);
        }
    }

    /**
     * Get usage statistics json file location.
     *
     * @return the location of statistics storage file.
     * @see ProjectHelpers#resolveStatisticsStore()
     */
    File getUsageStatisticsFile() {
        if (this.usageStatisticsFile == null) {
            this.usageStatisticsFile = ProjectHelpers.resolveStatisticsStore();
        }
        return this.usageStatisticsFile;
    }

    void clearAllProjectData() {
        update((global, project) -> {
            global.setValue(StatisticsConstants.FIELD_PROJECTS,
                    JsonHelpers.getJsonMapper().createArrayNode());
        });
    }

}
