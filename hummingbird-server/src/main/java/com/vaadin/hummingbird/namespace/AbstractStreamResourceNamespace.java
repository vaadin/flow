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
package com.vaadin.hummingbird.namespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StreamResource;
import com.vaadin.hummingbird.change.ChangeVisitor;
import com.vaadin.hummingbird.change.StreamResourceChange;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.communication.StreamResourceReference;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;

/**
 * @author Vaadin Ltd
 *
 */
abstract class AbstractStreamResourceNamespace extends Namespace {

    private Map<String, StreamResourceReference> values = new HashMap<>();

    private Set<StreamResourceReference> added = new HashSet<>();

    private Set<StreamResourceReference> removed = new HashSet<>();

    private static final String PARAMETERS_SEPARATOR = "?";

    protected AbstractStreamResourceNamespace(StateNode node) {
        super(node);
    }

    public StreamResource getResource(String key) {
        return Optional.ofNullable(values.get(key))
                .map(StreamResourceReference::getResource).orElse(null);
    }

    public void setResource(String key, StreamResource resource) {
        StreamResourceReference current = values.remove(key);

        if (current != null) {
            removed.add(current);
        }
        if (resource == null) {
            return;
        }

        String url = getUrl(resource);
        Element element = Element.get(getNode());
        setApplicationUri(element, key, url);

        if (resource.equals(Optional.ofNullable(current)
                .map(StreamResourceReference::getResource))) {
            return;
        }
        StreamResourceReference wrapper = new StreamResourceReference(url,
                getNode().getId(), resource);
        added.add(wrapper);
        values.put(key, wrapper);
    }

    @Override
    public void accept(ChangeVisitor visitor) {
        visitor.visit(new StreamResourceChange(copy(added), copy(removed)));
        clearChanges();
    }

    @Override
    public void nodeAttached() {
        clearChanges();
        updateReferences();
        added.addAll(values.values());
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
    }

    @Override
    public void resetChanges() {
    }

    protected abstract void setApplicationUri(Element element, String key, String url);

    private void updateReferences() {
        Map<String, StreamResourceReference> map = new HashMap<>();
        values.entrySet().forEach(entry -> map.put(entry.getKey(),
                new StreamResourceReference(
                        entry.getValue().getApplicationResourceUri(),
                        getNode().getId(), entry.getValue().getResource())));
        values = map;
    }

    private void clearChanges() {
        added.clear();
        removed.clear();
    }

    private Collection<StreamResourceReference> copy(
            Collection<StreamResourceReference> collection) {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(collection);
        }
    }

    private String getUrl(StreamResource resource) {
        StringBuilder builder = new StringBuilder(resource.getUri());
        if (resource.getUri().contains(PARAMETERS_SEPARATOR)) {
            if (resource.getUri().lastIndexOf(
                    PARAMETERS_SEPARATOR) < resource.getUri().length() - 1) {
                builder.append(PARAMETERS_SEPARATOR);
            }
        } else {
            builder.append(PARAMETERS_SEPARATOR);
        }
        builder.append(ApplicationConstants.UI_ID_PARAMETER).append('=')
                .append(UI.getCurrent().getUIId());
        return builder.toString();
    }
}
