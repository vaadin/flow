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
package com.vaadin.router;

import java.util.ArrayDeque;

import com.vaadin.router.event.BeforeLeaveObserver;
import com.vaadin.router.event.BeforeNavigationObserver;

/**
 * Container class for containing left over listeners on postponed navigation.
 */
public class Postpone {
    private ArrayDeque<BeforeLeaveObserver> remainingLeaveListeners = new ArrayDeque<>();
    private ArrayDeque<BeforeNavigationObserver> remainingNavigationListeners = new ArrayDeque<>();

    /**
     * Set any remaining {@link BeforeLeaveObserver}s to be continued from.
     * 
     * @param beforeLeave
     *            listeners to continue calling
     * @return uncalled listeners to continue from
     */
    public Postpone setLeaveObservers(
            ArrayDeque<BeforeLeaveObserver> beforeLeave) {
        remainingLeaveListeners = beforeLeave;
        return this;
    }

    /**
     * Set any remaining {@link BeforeNavigationObserver}s to be continued from.
     *
     * @param beforeNavigation
     *            listeners to continue calling
     * @return uncalled listeners to continue from
     */
    public Postpone setNavigationObservers(
            ArrayDeque<BeforeNavigationObserver> beforeNavigation) {
        remainingNavigationListeners = beforeNavigation;
        return this;
    }

    /**
     * Get {@link BeforeLeaveObserver}s that have been left over from a
     * postpone.
     * 
     * @return remaining BeforeLeaveObservers or empty ArrayDeque
     */
    public ArrayDeque<BeforeLeaveObserver> getLeaveObservers() {
        return remainingLeaveListeners;
    }

    /**
     * Get {@link BeforeNavigationObserver}s that have been left over from a
     * postpone.
     * 
     * @return remaining BeforeNavigationObservers or empty ArrayDeque
     */
    public ArrayDeque<BeforeNavigationObserver> getNavigationObservers() {
        return remainingNavigationListeners;
    }
}
