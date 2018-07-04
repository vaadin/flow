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
package com.vaadin.flow.router;

import com.vaadin.flow.component.HasElement;

import java.util.Objects;

/**
 * Implementations of this interface represent a parent for a navigation target
 * components via the {@link Route#layout()} parameter.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface RouterLayout extends HasElement {

    /**
     * Shows the content of the layout which is the router target component
     * annotated with a {@link Route @Route}.
     * <p>
     * <strong>Note</strong> implementors should not care about old
     * {@code @Route} content, because {@link Router} automatically removes
     * it before calling the method.
     * </p>
     *
     * @param content the content component or {@code null} if the layout content is to be cleared.
     */
    default void showRouterLayoutContent(HasElement content) {
        if (content != null) {
            getElement().appendChild(Objects.requireNonNull(content.getElement()));
        }
    }

}
