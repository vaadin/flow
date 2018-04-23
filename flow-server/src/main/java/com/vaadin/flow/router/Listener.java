package com.vaadin.flow.router;

import java.util.*;

/**
 * Base-interface for all 'Listener'-classes. Listeners are handlers that can be attached
 * by calling a 'addListener'-method of some form. The other form of handlers being 'Observers',
 * which need to be part of the component-tree and will be recognized automatically.
 * All listeners implement {@link Comparable}, to give users the complete control over
 * the order of execution. It is guaranteed that for every handler-type, all listeners are executed
 * before the first observer is being executed.
 *
 * @param <T> the type of the implementing class itself, so that any class Foo that implements Listener&lt;Foo&gt; also implements Comparable&lt;Foo&gt;
* */
public interface Listener<T, E extends EventObject> extends Handler<E>, Comparable<T> {
    @Override
    default int compareTo(T t) {
        return 0;
    }
}
