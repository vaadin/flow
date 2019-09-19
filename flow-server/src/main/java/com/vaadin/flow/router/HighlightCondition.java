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

import java.io.Serializable;

/**
 * A condition to meet to determine the highlight state of the target.
 *
 * @param <T>
 *            the target type of the highlight condition
 * @since 1.0
 */
@FunctionalInterface
public interface HighlightCondition<T> extends Serializable {

    /**
     * Tests if the target should be highlighted based on the navigation
     * {@code event}.
     *
     * @param t
     *            the target of the highlight condition
     * @param event
     *            the navigation event
     * @return true if the condition is met, false otherwise
     */
    boolean shouldHighlight(T t, AfterNavigationEvent event);
}
