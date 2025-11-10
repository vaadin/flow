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
package com.vaadin.flow.hotswap;

/**
 * Strategy for updating the UI after a hotswap event.
 * <p>
 * This enum defines how the browser UI should respond when classes are
 * hot-swapped during development. The strategy determines whether a partial
 * refresh or a full page reload is needed.
 * <p>
 * Note that {@link #RELOAD} has higher priority than {@link #REFRESH}. Once a
 * RELOAD strategy is set, it cannot be downgraded to REFRESH.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public enum UIUpdateStrategy {
    /**
     * Performs a partial UI refresh without reloading the entire page.
     * <p>
     * This strategy attempts to update only the affected UI components,
     * preserving the current application state and providing a faster update
     * experience during development.
     */
    REFRESH,

    /**
     * Performs a full browser page reload.
     * <p>
     * This strategy forces the browser to reload the entire page, which is
     * necessary when changes cannot be applied through a partial refresh (e.g.,
     * structural changes, route modifications, or critical component updates).
     * <p>
     * This strategy has higher priority than {@link #REFRESH} and cannot be
     * downgraded once set.
     */
    RELOAD
}
