/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird.template;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Registry of template nodes received from the server.
 *
 * @since
 * @author Vaadin Ltd
 */
public class Templates {

    // Using Double since it will be unboxed in GWT
    private final JsMap<Double, Template> templates = JsCollections.map();

    /**
     * Imports a set of templates from JSON.
     *
     * @param templatesJson
     *            a JSON object with each key corresponding the template id and
     *            the value is the template itself, not <code>null</code>
     */
    public void importFromJson(JsonObject templatesJson) {
        assert templatesJson != null;

        for (String key : templatesJson.keys()) {
            JsonValue templateJson = templatesJson.get(key);

            Template template = WidgetUtil.crazyJsCast(templateJson);

            addTemplate(Integer.parseInt(key), template);
        }
    }

    /**
     * Registers the given template with the given id.
     *
     * @param id
     *            the id of the template to register
     * @param template
     *            the template to register, not null
     */
    public void addTemplate(int id, Template template) {
        assert template != null;

        Double key = Double.valueOf(id);
        assert !templates.has(key);

        templates.set(key, template);
    }

    /**
     * Gets the template registered with the given id
     * 
     * @param id
     *            the id of the template to get
     * @return the template, or <code>null</code> if no template has been
     *         registered
     */
    public Template getTemplate(int id) {
        return templates.get(Double.valueOf(id));
    }

}
