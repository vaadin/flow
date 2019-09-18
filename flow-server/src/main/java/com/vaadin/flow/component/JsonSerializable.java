/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.io.Serializable;

import elemental.json.JsonObject;

/**
 * Base interface for classes that are serializable to and from
 * {@link JsonObject}.
 *
 * @since 1.0
 */
public interface JsonSerializable extends Serializable {

    /**
     * Converts this object to its JSON format.
     * 
     * @return the JSON representation of the object
     */
    JsonObject toJson();

    /**
     * Sets the JSON object data into the Java object.
     * 
     * @param value
     *            the JSON representation of the object
     * @return this instance, for method chaining
     */
    JsonSerializable readJson(JsonObject value);

}
