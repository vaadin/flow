/*
 * Copyright 2000-2017 Vaadin Ltd.
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

/**
 * A set of predefined {@link HighlightCondition}s.
 */
public final class HighlightConditions {

    /**
     * Highlight if the navigation path is the same of the target
     * {@link RouterLink}.
     *
     * @return the highlight condition
     */
    public static HighlightCondition<RouterLink> sameLocation() {
        return (link, event) -> event.getLocation().getPath()
                .equals(link.getHref());
    }

    /**
     * Highlight if the navigation path starts with the target
     * {@link RouterLink} path.
     *
     * @return the highlight condition
     */
    public static HighlightCondition<RouterLink> locationPrefix() {
        return (link, event) -> event.getLocation().getPath()
                .startsWith(link.getHref());
    }

    /**
     * Highlight if the navigation path starts with {@code prefix}.
     *
     * @return the highlight condition
     */
    public static <C extends HasElement> HighlightCondition<C> locationPrefix(
            String prefix) {
        return (link, event) -> event.getLocation().getPath()
                .startsWith(prefix);
    }

    /**
     * Always highlight.
     *
     * @return an always true highlight condition
     */
    public static <C extends HasElement> HighlightCondition<C> always() {
        return (link, event) -> true;
    }

    /**
     * Never highlight.
     *
     * @return an always false highlight condition
     */
    public static <C extends HasElement> HighlightCondition<C> never() {
        return (link, event) -> false;
    }

    private HighlightConditions() {
    }
}
