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
package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Data holder class for collected {@link Inline} annotations to be added to the
 * initial page.
 *
 * @since 1.0
 */
public class InlineTargets {

    private final Map<Inline.Position, List<JsonObject>> inlineHead = new EnumMap<>(
            Inline.Position.class);
    private final Map<Inline.Position, List<JsonObject>> inlineBody = new EnumMap<>(
            Inline.Position.class);

    /**
     * Inline contents from classpath file to head of initial page.
     *
     * @param inline
     *            inline dependency to add to bootstrap page
     * @param request
     *            the request that is handled
     */
    public void addInlineDependency(Inline inline, VaadinRequest request) {
        Inline.Wrapping type;
        // Determine the type as given or try to automatically decide
        if (inline.wrapping().equals(Inline.Wrapping.AUTOMATIC)) {
            type = determineDependencyType(inline);
        } else {
            type = inline.wrapping();
        }

        JsonObject dependency = Json.createObject();
        dependency.put(Dependency.KEY_TYPE, type.toString());
        dependency.put("LoadMode", LoadMode.INLINE.toString());

        dependency.put(Dependency.KEY_CONTENTS,
                BootstrapUtils.getDependencyContents(request, inline.value()));

        // Add to correct element target
        if (inline.target() == TargetElement.BODY) {
            getInlineBody(inline.position()).add(dependency);
        } else {
            getInlineHead(inline.position()).add(dependency);

        }
    }

    private Inline.Wrapping determineDependencyType(Inline inline) {
        Inline.Wrapping type;
        if (inline.value().endsWith(".js")) {
            type = Inline.Wrapping.JAVASCRIPT;
        } else if (inline.value().endsWith(".css")) {
            type = Inline.Wrapping.STYLESHEET;
        } else {
            type = Inline.Wrapping.NONE;
        }
        return type;
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
