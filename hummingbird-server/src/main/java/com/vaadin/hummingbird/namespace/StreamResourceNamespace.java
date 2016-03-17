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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StreamResource;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.communication.StreamResourceWrapper;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 *
 */
public class StreamResourceNamespace extends Namespace {

    private transient Map<String, StreamResourceWrapper> values = new HashMap<>();

    private transient Set<StreamResourceWrapper> added = new HashSet<>();

    private transient Set<StreamResourceWrapper> removed = new HashSet<>();

    private class ResourceChange extends NodeChange {

        public ResourceChange(StateNode node) {
            super(node);
        }

        @Override
        protected void populateJson(JsonObject json) {
        }

        @Override
        public JsonObject toJson() {
            // XXX: this requires NodeChange redesign
            UI ui = UI.getCurrent();
            if (!added.isEmpty()) {
                ui.addStreamResources(added);
            }
            ui.removeStreamResource(removed);
            return null;
        }
    }

    public StreamResourceNamespace(StateNode node) {
        super(node);
    }

    public StreamResource getResource(String property) {
        return Optional.ofNullable(values.get(property))
                .map(StreamResourceWrapper::getResource).orElse(null);
    }

    public void setResource(String property, StreamResource resource) {
        StreamResourceWrapper current = values.remove(property);

        if (current != null) {
            removed.add(current);
        }
        if (resource == null) {
            return;
        }

        String url = getUrl(resource);
        Element element = Element.get(getNode());
        element.setProperty(property, url);

        if (resource.equals(Optional.ofNullable(current)
                .map(StreamResourceWrapper::getResource))) {
            return;
        }
        StreamResourceWrapper wrapper = new StreamResourceWrapper(url,
                getNode(), resource);
        added.add(wrapper);
        values.put(property, wrapper);
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        collector.accept(new ResourceChange(getNode()));
        clearChanges();
    }

    @Override
    public void resetChanges() {
        clearChanges();
        added.addAll(values.values());
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
    }

    private void clearChanges() {
        added.clear();
        removed.clear();
    }

    private String getUrl(StreamResource resource) {
        // XXX: improve logic
        StringBuilder builder = new StringBuilder(resource.getUri());
        if (resource.getUri().contains("?")) {
            if (resource.getUri().lastIndexOf("?") < resource.getUri().length()
                    - 1) {
                builder.append("&");
            }
        } else {
            builder.append('?');
        }
        builder.append(ApplicationConstants.UI_ID_PARAMETER).append('=')
                .append(UI.getCurrent().getUIId());
        return builder.toString();
    }
}
