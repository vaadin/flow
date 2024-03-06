/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Container class for containing left over listeners on postponed navigation.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
