/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
     * @deprecated use {@link #addInlineDependency(Inline, VaadinService)}
     *             instead
     */
    @Deprecated
    public void addInlineDependency(Inline inline, VaadinRequest request) {
        addInlineDependency(inline, request.getService());
    }

    /**
     * Inline contents from classpath file to head of initial page.
     *
     * @param inline
     *            inline dependency to add to bootstrap page
     * @param service
     *            the service that can find the dependency
     */
    public void addInlineDependency(Inline inline, VaadinService service) {
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
                BootstrapUtils.getDependencyContents(service, inline.value()));

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
    public List<JsonObject> getInlineHead(Inline.Position position) {
        return inlineHead.computeIfAbsent(position, key -> new ArrayList<>());
    }

    /**
     * Get the list of inline objects to add to body.
     *
     * @param position
     *            prepend or append
     * @return current list of inline objects
     */
    public List<JsonObject> getInlineBody(Inline.Position position) {
        return inlineBody.computeIfAbsent(position, key -> new ArrayList<>());
    }
}
