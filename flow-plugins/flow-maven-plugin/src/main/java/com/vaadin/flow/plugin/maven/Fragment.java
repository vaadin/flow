/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import java.util.HashSet;
import java.util.Set;

/**
 * Intended to be used by Maven to specify fragments.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class Fragment {
    private String name;
    private final Set<String> files = new HashSet<>();

    /**
     * Gets the name of a fragment.
     *
     * @return the name of a fragment, may be {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the files that belong to the fragment.
     *
     * @return the files that belong to a fragment
     */
    public Set<String> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return "Fragment{name='" + name + "\', files=" + files + '}';
    }
}
