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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveObserver;

/**
 * Container class for containing left over listeners on postponed navigation.
 */
public class Postpone implements Serializable {
    private final ArrayDeque<BeforeLeaveObserver> remainingLeaveListeners;
    private final ArrayDeque<BeforeEnterObserver> remainingNavigationListeners;

    private Postpone(Deque<BeforeLeaveObserver> beforeLeave,
            Deque<BeforeEnterObserver> beforeNavigation) {
        remainingLeaveListeners = new ArrayDeque<>(beforeLeave);
        remainingNavigationListeners = new ArrayDeque<>(beforeNavigation);
    }

    /**
     * Set any remaining {@link BeforeLeaveObserver}s to be continued from.
     * 
     * @param beforeLeave
     *            listeners to continue calling
     * @return uncalled listeners to continue from
     */
    public static Postpone withLeaveObservers(
            Deque<BeforeLeaveObserver> beforeLeave) {
        return new Postpone(beforeLeave, new ArrayDeque<>());
    }

    /**
     * Set any remaining {@link BeforeEnterObserver}s to be continued from.
     *
     * @param beforeNavigation
     *            listeners to continue calling
     * @return uncalled listeners to continue from
     */
    public static Postpone withNavigationObservers(
            Deque<BeforeEnterObserver> beforeNavigation) {
        return new Postpone(new ArrayDeque<>(), beforeNavigation);
    }

    /**
     * Get {@link BeforeLeaveObserver}s that have been left over from a
     * postpone.
     * 
     * @return remaining BeforeLeaveObservers or empty ArrayDeque
     */
    public Deque<BeforeLeaveObserver> getLeaveObservers() {
        return remainingLeaveListeners;
    }

    /**
     * Get {@link BeforeEnterObserver}s that have been left over from a
     * postpone.
     * 
     * @return remaining BeforeNavigationObservers or empty ArrayDeque
     */
    public Deque<BeforeEnterObserver> getNavigationObservers() {
        return remainingNavigationListeners;
    }
}
