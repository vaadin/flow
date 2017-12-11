/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.Inline;

import elemental.json.Json;
import elemental.json.JsonObject;

public class InlineTargets {

    private final Map<Inline.Position, List<JsonObject>> inlineHead = new EnumMap<>(Inline.Position.class);
    private final Map<Inline.Position, List<JsonObject>> inlineBody = new EnumMap<>(Inline.Position.class);

    public InlineTargets() {
    }

    /**
     * Inline contents from classpath file to head of initial page.
     *
     * @param inline inline dependency to add to bootstrap page
     */
    public void addInlineDependency(Inline inline, VaadinRequest request) {
        Dependency.Type type;
        // Determine the type as given or try to automatically decide
        switch (inline.wrapping()) {
        case CSS:
            type = Dependency.Type.STYLESHEET;
            break;
        case HTML:
            type = Dependency.Type.HTML_IMPORT;
            break;
        case SCRIPT:
            type = Dependency.Type.JAVASCRIPT;
            break;
        default:
            type = inline.value().endsWith(".js")
                    ? Dependency.Type.JAVASCRIPT
                    : inline.value().endsWith(".css")
                    ? Dependency.Type.STYLESHEET
                    : Dependency.Type.HTML_IMPORT;
        }

        JsonObject dependency = Json.createObject();
        dependency.put(Dependency.KEY_TYPE, type.toString());
        dependency.put("LoadMode", LoadMode.INLINE.toString());

        dependency.put(Dependency.KEY_CONTENTS, BootstrapUtils.getDependencyContents(request, inline.value()));

        // Add to correct element target
        switch(inline.target()) {
        case BODY:
            getInlineBody(inline.position()).add(dependency);
            break;
        default:
            getInlineHead(inline.position()).add(dependency);

        }
    }
    /**
     * Get the list of inline objects to add to head.
     *
     * @param position
     *            prepend or append
     * @return current list of inline objects
     */
    protected List<JsonObject> getInlineHead(Inline.Position position) {
        return inlineHead.computeIfAbsent(position, key -> new ArrayList<>());
    }
    /**
     * Get the list of inline objects to add to body.
     *
     * @param position
     *            prepend or append
     * @return current list of inline objects
     */
    protected List<JsonObject> getInlineBody(Inline.Position position) {
        return inlineBody.computeIfAbsent(position, key -> new ArrayList<>());
    }
}
