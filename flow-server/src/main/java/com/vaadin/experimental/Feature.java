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
package com.vaadin.experimental;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.internal.UsageStatistics;

/**
 * Information about a feature available behind a flag.
 */
public final class Feature implements Serializable {

    private final String title;
    private final String id;
    private final String moreInfoLink;
    private final boolean requiresServerRestart;
    private final String componentClassName;
    private boolean enabled;

    /**
     * Creates a new feature with the given options.
     *
     * @param title
     *            the title of the feature
     * @param id
     *            the unique id of the feature
     * @param moreInfoLink
     *            a link to an issue describing the feature on a high level
     * @param requiresServerRestart
     *            {@code true} if toggling the feature requires a server restart
     * @param componentClassName
     *            If the feature is a component, the qualified name of the class
     *            otherwise null
     */
    public Feature(String title, String id, String moreInfoLink,
            boolean requiresServerRestart, String componentClassName) {
        this.title = Objects.requireNonNull(title);
        this.id = Objects.requireNonNull(id);
        this.moreInfoLink = moreInfoLink;
        this.requiresServerRestart = requiresServerRestart;
        this.componentClassName = componentClassName;
    }

    /**
     * Create a copy of the given feature.
     *
     * @param feature
     *            feature to create a copy of
     */
    public Feature(Feature feature) {
        this.title = feature.getTitle();
        this.id = feature.getId();
        this.moreInfoLink = feature.getMoreInfoLink();
        this.requiresServerRestart = feature.isRequiresServerRestart();
        this.enabled = feature.enabled;
        this.componentClassName = feature.componentClassName;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getMoreInfoLink() {
        return moreInfoLink;
    }

    public String getComponentClassName() {
        return componentClassName;
    }

    public boolean isRequiresServerRestart() {
        return requiresServerRestart;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            UsageStatistics.markAsUsed("flow/featureflags/" + getId(), null);
        }

        this.enabled = enabled;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Feature other = (Feature) obj;
        return (id.equals(other.id));
    }

}
