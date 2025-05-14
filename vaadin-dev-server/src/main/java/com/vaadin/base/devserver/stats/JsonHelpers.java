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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class JsonHelpers {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /*
     * Avoid instantiation.
     */
    private JsonHelpers() {
        // Utility class only
    }

    /**
     * Helper to find an ObjectNode by id in the given array node.
     *
     * @param id
     *            Node ID to find or create
     * @param arrayNode
     *            Json array node containing list of arrayNode
     * @param idField
     *            Name of the ID field in ObjectNode to match with ID.
     * @param createNew
     *            true if a new {@link ObjectNode} should be created if not
     *            found.
     * @return Json {@link ObjectNode} if found or null. Always returns a node
     *         if <code>createNew</code> is <code>true</code> and
     *         <code>arrayNode</code> is not null.
     * @see StatisticsConstants#FIELD_PROJECT_ID
     */
    static ObjectNode getOrCreate(String id, JsonNode arrayNode, String idField,
            boolean createNew) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return null;
        }

        for (final JsonNode p : arrayNode) {
            if (p != null && p.has(idField)
                    && id.equals(p.get(idField).asText())) {
                return (ObjectNode) p;
            }
        }

        if (createNew) {
            ArrayNode newNode = (ArrayNode) arrayNode;
            ObjectNode p = newNode.addObject();
            p.put(StatisticsConstants.FIELD_PROJECT_ID, id);
            return p;
        }

        return null;
    }

    /**
     * Helper to update a single autoincrement integer value in a node.
     *
     * @param node
     *            Json node which contains the field
     * @param fieldName
     *            name of the field to increment
     */
    static void incrementJsonValue(ObjectNode node, String fieldName) {
        if (node.has(fieldName)) {
            JsonNode f = node.get(fieldName);
            node.put(fieldName, f.asInt() + 1);
        } else {
            node.put(fieldName, 1);
        }
    }

    /**
     * Get instance of a ObjectMapper for mapping object to Json.
     *
     * @return Shared ObjectMapper instance.
     */
    static ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

}
