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
package com.vaadin.flow.plugin.maven;

import java.util.HashSet;
import java.util.Set;

/**
 * Intended to be used by Maven  to specify fragments.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class Fragment {
    private String name;
    private Set<String> files = new HashSet<>();

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
        return "Fragment{" +
                "name='" + name + '\'' +
                ", files=" + files +
                '}';
    }
}
