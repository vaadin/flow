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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Container class for containing left over listeners on postponed navigation.
 *
 * @since 1.0
 */
public class Postpone implements Serializable {
    private final ArrayDeque<BeforeLeaveHandler> remainingLeaveListeners;
    private final ArrayDeque<BeforeEnterHandler> remainingNavigationListeners;

    private Postpone(Deque<BeforeLeaveHandler> beforeLeave,
            Deque<BeforeEnterHandler> beforeNavigation) {
        remainingLeaveListeners = new ArrayDeque<>(beforeLeave);
        remainingNavigationListeners = new ArrayDeque<>(beforeNavigation);
    }

    /**
     * Set any remaining {@link BeforeLeaveHandler}s to be continued from.
     * 
     * @param beforeLeave
     *            listeners to continue calling
     * @return uncalled listeners to continue from
     */
    public static Postpone withLeaveObservers(
            Deque<BeforeLeaveHandler> beforeLeave) {
        return new Postpone(beforeLeave, new ArrayDeque<>());
    }

    /**
     * Set any remaining {@link BeforeEnterHandler}s to be continued from.
     *
     * @param beforeNavigation
     *            listeners to continue calling
     * @return uncalled listeners to continue from
     */
    public static Postpone withNavigationObservers(
            Deque<BeforeEnterHandler> beforeNavigation) {
        return new Postpone(new ArrayDeque<>(), beforeNavigation);
    }

    /**
     * Get {@link BeforeLeaveHandler}s that have been left over from a postpone.
     * 
     * @return remaining BeforeLeaveObservers or empty ArrayDeque
     */
    public Deque<BeforeLeaveHandler> getLeaveObservers() {
        return remainingLeaveListeners;
    }

    /**
     * Get {@link BeforeEnterHandler}s that have been left over from a postpone.
     * 
     * @return remaining BeforeNavigationObservers or empty ArrayDeque
     */
    public Deque<BeforeEnterHandler> getNavigationObservers() {
        return remainingNavigationListeners;
    }
}
