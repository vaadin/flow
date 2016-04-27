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
public class TemplateRegistry {

    // Using Double since it will be unboxed in GWT
    private final JsMap<Double, TemplateNode> instanceMap = JsCollections.map();

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

            TemplateNode templateNode = WidgetUtil.crazyJsCast(templateJson);

            register(Integer.parseInt(key), templateNode);
        }
    }

    /**
     * Registers the given template with the given id.
     *
     * @param id
     *            the id of the template to register
     * @param templateNode
     *            the template to register, not null
     */
    public void register(int id, TemplateNode templateNode) {
        assert templateNode != null;

        assert templateNode.getId() == null;
        templateNode.setId(Double.valueOf(id));

        Double key = Double.valueOf(id);
        assert !instanceMap.has(key);

        instanceMap.set(key, templateNode);
    }

    /**
     * Checks whether this registry contains a template with the given id.
     *
     * @param id
     *            the id to check
     * @return <code>true</code> if a template is found, <code>false</code> if
     *         there is no template with the given id
     */
    public boolean has(int id) {
        return instanceMap.has(Double.valueOf(id));
    }

    /**
     * Gets the template registered with the given id.
     *
     * @param id
     *            the id of the template to get
     * @return the template, not <code>null</code>
     */
    public TemplateNode get(int id) {
        TemplateNode templateNode = instanceMap.get(Double.valueOf(id));

        assert templateNode != null;
        assert Double.valueOf(id).equals(templateNode.getId());

        return templateNode;
    }

}
