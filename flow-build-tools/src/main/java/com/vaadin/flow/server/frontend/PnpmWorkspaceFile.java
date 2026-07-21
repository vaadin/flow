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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import com.vaadin.flow.internal.JacksonUtils;

/**
 * Reads and writes the {@code overrides} block of a project's
 * {@code pnpm-workspace.yaml}, the location pnpm 10+ uses for dependency
 * overrides. All other content in the file is preserved untouched. YAML I/O
 * goes through Jackson ({@link YAMLMapper}) so the whole module uses a single
 * JSON/YAML tool.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class PnpmWorkspaceFile {

    static final String WORKSPACE_FILE = "pnpm-workspace.yaml";
    private static final String OVERRIDES = "overrides";
    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    private final File file;
    private final ObjectNode document;

    PnpmWorkspaceFile(File projectRoot) throws IOException {
        this.file = new File(projectRoot, WORKSPACE_FILE);
        this.document = load();
    }

    private ObjectNode load() throws IOException {
        if (!file.isFile()) {
            return JacksonUtils.createObjectNode();
        }
        JsonNode parsed = YAML.readTree(file);
        return parsed instanceof ObjectNode object ? object
                : JacksonUtils.createObjectNode();
    }

    /**
     * Returns the current overrides as a flat {@code key -> version} map.
     */
    Map<String, String> getOverrides() {
        Map<String, String> result = new LinkedHashMap<>();
        JsonNode overrides = document.get(OVERRIDES);
        if (overrides instanceof ObjectNode object) {
            for (String key : JacksonUtils.getKeys(object)) {
                result.put(key, object.get(key).asString());
            }
        }
        return result;
    }

    /**
     * Replaces the overrides block with the given entries, removing the block
     * entirely when the map is empty.
     */
    void setOverrides(Map<String, String> overrides) {
        if (overrides.isEmpty()) {
            document.remove(OVERRIDES);
        } else {
            ObjectNode node = JacksonUtils.createObjectNode();
            overrides.forEach(node::put);
            document.set(OVERRIDES, node);
        }
    }

    /**
     * Persists the file when its serialized content changed. When the whole
     * document is empty the file is deleted, because an empty
     * {@code pnpm-workspace.yaml} carries no configuration; user-authored
     * override keys and other sections keep the document non-empty and thus
     * keep the file alive.
     *
     * @return {@code true} if the file was written or deleted
     */
    boolean save() throws IOException {
        if (document.isEmpty()) {
            if (file.isFile()) {
                Files.delete(file.toPath());
                return true;
            }
            return false;
        }
        String yaml = YAML.writeValueAsString(document);
        String current = file.isFile()
                ? Files.readString(file.toPath(), StandardCharsets.UTF_8)
                : null;
        if (yaml.equals(current)) {
            return false;
        }
        Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
        return true;
    }
}
