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
package com.vaadin.flow.component.template.internal;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

/**
 *
 * Immutable parser data which may be stored in cache.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public class ParserData {

    private final Map<String, String> tagById;
    private final Map<Field, String> idByField;

    private final Map<String, Map<String, String>> attributesById;

    /**
     * Constructs an immutable data object with the given information.
     *
     * @param fields
     *            a map of fields to their ids
     * @param tags
     *            a map of ids to their tags
     * @param attributes
     *            a map of attributes values to the element id
     */
    public ParserData(Map<Field, String> fields, Map<String, String> tags,
            Map<String, Map<String, String>> attributes) {
        tagById = Collections.unmodifiableMap(tags);
        idByField = Collections.unmodifiableMap(fields);
        attributesById = Collections.unmodifiableMap(attributes);
    }

    /**
     * Applies the given consumer to each mapped field.
     *
     * @param consumer
     *            the consumer to call for each mapped field
     */
    public void forEachInjectedField(InjectableFieldConsumer consumer) {
        idByField.forEach(
                (field, id) -> consumer.apply(field, id, tagById.get(id)));
    }

    /**
     * Gets template element data (attribute values).
     *
     * @param id
     *            the id of the element
     * @return template data
     */
    public Map<String, String> getAttributes(String id) {
        Map<String, String> attrs = attributesById.get(id);
        if (attrs == null) {
            return Collections.emptyMap();
        }
        return attrs;
    }

}
