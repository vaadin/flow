package com.vaadin.flow.router;

import com.vaadin.flow.component.*;

import java.util.*;

/**
 * A base-interface for all {@link Handler} that are part of the component-tree of a UI
 * and are to be recognized as a {@link Handler} automatically.
 * @param <E> the {@link EventObject} to handle, see {@link Handler}
 */
public interface Observer<E extends EventObject> extends Handler<E>, HasElement {
}
