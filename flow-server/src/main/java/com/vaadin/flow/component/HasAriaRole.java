/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.util.Optional;

/**
 * A generic interface for components and other user interface objects that may
 * have a role DOM attribute to define the semantic meaning of the component for
 * assistive technologies such as screen readers.
 * <p>
 * The default implementation sets the role of the component to the given
 * {@link #getElement()}. Override the methods in this interface if the role
 * should be added to some other element.
 * <p>
 * See: https://www.w3.org/TR/wai-aria/#usage_intro
 *
 * @author Vaadin Ltd
 */
public interface HasAriaRole extends HasElement {

    /**
     * Sets the ARIA role attribute of the component to the given role.
     *
     * @param role
     *            the role to set, or {@code null} to clear
     */
    default void setAriaRole(String role) {
        if (role != null) {
            getElement().setAttribute("role", role);
        } else {
            getElement().removeAttribute("role");
        }
    }

    /**
     * Gets the ARIA role attribute of the component.
     *
     * @return an optional ARIA role of the component if no ARIA role has been
     *         set
     */
    default Optional<String> getAriaRole() {
        return Optional.ofNullable(getElement().getAttribute("role"));
    }
}
