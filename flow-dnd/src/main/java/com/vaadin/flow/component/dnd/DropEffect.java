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
package com.vaadin.flow.component.dnd;

import java.util.Locale;

/**
 * Used to specify the drop effect to use on dragenter or dragover events.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public enum DropEffect {
    /**
     * A copy of the source item is made at the new location.
     */
    COPY,

    /**
     * An item is moved to a new location.
     */
    MOVE,

    /**
     * A link is established to the source at the new location.
     */
    LINK,

    /**
     * The item may not be dropped.
     */
    NONE;

    /**
     * Parses drop effect from given non-null string.
     *
     * @param string
     *            the string to parse
     * @return the matching drop effect
     */
    static DropEffect fromString(String string) {
        return DropEffect.valueOf(string.toUpperCase(Locale.ENGLISH));
    }
}
