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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Wraps a JSON data structure and provides a slightly easier API for it.
 */
public class StatisticsContainer {

    private ObjectNode json;

    /**
     * Wraps the given json with this API.
     *
     * @param json
     *            the json to wrap
     */
    public StatisticsContainer(ObjectNode json) {
        this.json = json;
    }

    /**
     * Stores a string using the given field name.
     *
     * @param name
     *            name of the field to store
     * @param value
     *            the value to store
     */
    public void setValue(String name, String value) {
        json.put(name, value);
    }

    /**
     * Stores a long using the given field name.
     *
     * @param name
     *            name of the field to store
     * @param value
     *            the value to store
     */
    public void setValue(String name, long value) {
        json.put(name, value);
    }

    /**
     * Stores a JSON object using the given field name.
     *
     * @param name
     *            name of the field to store
     * @param value
     *            the value to store
     */
    public void setValue(String name, JsonNode value) {
        json.set(name, value);
    }

    /**
     * Increments the value for the given field.
     *
     * @param name
     *            the name of the field to increment
     */
    public void increment(String name) {
        JsonHelpers.incrementJsonValue(json, name);
    }

    /**
     * Sets the value to the given field and updates related aggregate fields.
     * <p>
     * Updates the following fields:
     * <ul>
     * <li><code>name</code> The new value</li>
     * <li><code>name_min</code> The minimum value</li>
     * <li><code>name_max</code> The maximum value</li>
     * <li><code>name_count</code> Number of values collected</li>
     * </ul>
     *
     * @param name
     *            Name of the field to update.
     * @param newValue
     *            The new value to store.
     */
    public void aggregate(String name, double newValue) {
        // Update count
        JsonHelpers.incrementJsonValue(json, name + "_count");
        double count = json.get(name + "_count").asInt();

        // Update min & max
        double min = newValue;
        if (json.has(name + "_min") && json.get(name + "_min").isDouble()) {
            min = json.get(name + "_min").asDouble(newValue);
        }
        json.put(name + "_min", Math.min(newValue, min));

        double max = newValue;
        if (json.has(name + "_max") && json.get(name + "_max").isDouble()) {
            max = json.get(name + "_max").asDouble(newValue);
        }
        json.put(name + "_max", Math.max(newValue, max));

        // Update average
        double avg = newValue;
        if (json.has(name + "_avg") && json.get(name + "_avg").isDouble()) {
            // Calcalate new incremental average
            avg = json.get(name + "_avg").asDouble(newValue);
            avg += (newValue - avg) / count;
        }
        json.put(name + "_avg", avg);
        json.put(name, newValue);
    }

    /**
     * Returns the given field value as a string.
     *
     * @param name
     *            name of the field to get
     * @return the value of the field as a string
     */
    public String getValue(String name) {
        return json.get(name).asString();
    }

    /**
     * Returns the given field value as an integer.
     *
     * @param name
     *            name of the field to get
     * @return the value of the field as an integer, 0 if missing or not an
     *         integer
     */
    int getValueAsInt(String name) {
        if (json.has(name) && json.get(name).isInt()) {
            return json.get(name).asInt(0);
        }
        return 0;
    }

    /**
     * Returns the given field value as a double.
     *
     * @param name
     *            name of the field to get
     * @return the value of the field as a double, 0 if missing or not a double
     */
    public double getValueAsDouble(String name) {
        if (json.has(name) && json.get(name).isDouble()) {
            return json.get(name).asDouble(0);
        }
        return 0.0;
    }

}
