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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Set;

/**
 * Representation of the theme names for an {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ThemeList extends Set<String>, Serializable {

    /**
     * Sets or removes the given theme name, based on the {@code set} parameter.
     *
     * @param themeName
     *            the theme name to set or remove
     * @param set
     *            true to set the theme name, false to remove it
     * @return true if the theme list was modified (theme name added or
     *         removed), false otherwise
     */
    default boolean set(String themeName, boolean set) {
        return set ? add(themeName) : remove(themeName);
    }
}
