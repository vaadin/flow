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

package com.vaadin.flow.internal;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Keeps track of various current instances for the current thread. All the
 * instances are automatically cleared after handling a request from the client
 * to avoid leaking memory.
 * <p>
 * Please note that the instances are stored using {@link WeakReference}. This
 * means that the a current instance value may suddenly disappear if there a no
 * other references to the object.
 * <p>
 * Currently the framework uses the following instances:
 * </p>
 * <p>
 * {@link VaadinRequest}, {@link VaadinResponse}, {@link UI},
 * {@link VaadinService}, {@link VaadinSession}.
 * </p>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class CurrentInstance implements Serializable {
    private static final Object NULL_OBJECT = new Object();
    private static final CurrentInstance CURRENT_INSTANCE_NULL = new CurrentInstance(
            NULL_OBJECT);

    private final WeakReference<Object> instance;

    private static final ThreadLocal<Map<Class<?>, CurrentInstance>> instances = new ThreadLocal<>();

    private CurrentInstance(Object instance) {
        this.instance = new WeakReference<>(instance);
    }

    /**
     * Gets the current instance of a specific type if available.
     *
     * @param <T>
     *            the instance type
     * @param type
     *            the class to get an instance of
     * @return the current instance or the provided type, or <code>null</code>
     *         if there is no current instance.
     */
    public static <T> T get(Class<T> type) {
        Map<Class<?>, CurrentInstance> map = instances.get();
        if (map == null) {
            return null;
        }
        CurrentInstance currentInstance = map.get(type);
        if (currentInstance != null) {
            Object value = currentInstance.instance.get();
            if (value == null) {
                /*
                 * This is believed to never actually happen since the
                 * ThreadLocal should only outlive the referenced object on
                 * threads that are not doing anything related to Vaadin, which
                 * should thus never invoke CurrentInstance.get().
                 *
                 * At this point, there might also be other values that have
                 * been collected, so we'll scan the entire map and remove stale
                 * CurrentInstance objects. Using a ReferenceQueue could make
                 * this assumingly rare case slightly more efficient, but would
                 * significantly increase the complexity of the code for
                 * maintaining a separate ReferenceQueue for each Thread.
                 */
                removeStaleInstances(map);

                if (map.isEmpty()) {
                    instances.remove();
                }

                return null;
            }
            return type.cast(value);
        } else {
            return null;
        }
    }

    private static void removeStaleInstances(
            Map<Class<?>, CurrentInstance> map) {
        for (Iterator<Entry<Class<?>, CurrentInstance>> iterator = map
                .entrySet().iterator(); iterator.hasNext();) {
            Entry<Class<?>, CurrentInstance> entry = iterator.next();
            Object instance = entry.getValue().instance.get();
            if (instance == null) {
                iterator.remove();
                getLogger().debug(
                        "CurrentInstance for {} has been garbage collected.",
                        entry.getKey());
            }
        }
    }

    /**
     * Sets the current instance of the given type.
     *
     * @see ThreadLocal
     *
     * @param <T>
     *            the instance type
     * @param type
     *            the class that should be used when getting the current
     *            instance back
     * @param instance
     *            the actual instance
     */
    public static <T> void set(Class<T> type, T instance) {
        doSet(type, instance);
    }

    /**
     * Sets the current instance of the given type.
     *
     * @see ThreadLocal
     *
     * @param type
     *            the class that should be used when getting the current
     *            instance back
     * @param instance
     *            the actual instance
     * @return previous CurrentInstance wrapper
     */
    private static <T> CurrentInstance doSet(Class<T> type, T instance) {
        Map<Class<?>, CurrentInstance> map = instances.get();
        CurrentInstance previousInstance = null;
        if (instance == null) {
            // remove the instance
            if (map != null) {
                previousInstance = map.remove(type);
                if (map.isEmpty()) {
                    instances.remove();
                    map = null;
                }
            }
        } else {
            assert type.isInstance(instance) : "Invalid instance type";
            if (map == null) {
                map = new HashMap<>();
                instances.set(map);
            }

            previousInstance = map.put(type, new CurrentInstance(instance));
        }
        if (previousInstance == null) {
            previousInstance = CURRENT_INSTANCE_NULL;
        }
        return previousInstance;
    }

    /**
     * Clears all current instances.
     */
    public static void clearAll() {
        instances.remove();
    }

    /**
     * Restores the given instances to the given values. Note that this should
     * only be used internally to restore Vaadin classes.
     *
     *
     * @param old
     *            A Class -&lt; CurrentInstance map to set as current instances
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void restoreInstances(Map<Class<?>, CurrentInstance> old) {
        boolean removeStale = false;
        for (Entry<Class<?>, CurrentInstance> entry : old.entrySet()) {
            Class c = entry.getKey();
            CurrentInstance ci = entry.getValue();
            Object v = ci.instance.get();
            if (v == null) {
                removeStale = true;
            } else if (v == NULL_OBJECT) {
                /*
                 * NULL_OBJECT is used to identify objects that are null when
                 * #setCurrent(UI) or #setCurrent(VaadinSession) are called on a
                 * CurrentInstance. Without this a reference to an already
                 * collected instance may be left in the CurrentInstance when it
                 * really should be restored to null.
                 *
                 * One example case that this fixes:
                 * VaadinService.runPendingAccessTasks() clears all current
                 * instances and then sets everything but the UI. This makes
                 * UI.accessSynchronously() save these values before calling
                 * setCurrent(UI), which stores UI=null in the map it returns.
                 * This map will be restored after UI.accessSync(), which,
                 * unless it respects null values, will just leave the wrong UI
                 * instance registered.
                 */
                v = null;
            }
            set(c, v);
        }

        if (removeStale) {
            removeStaleInstances(old);
        }
    }

    /**
     * Gets the currently set instances so that they can later be restored using
     * {@link #restoreInstances(Map)}.
     *
     * @return a map containing the current instances
     */
    public static Map<Class<?>, CurrentInstance> getInstances() {
        Map<Class<?>, CurrentInstance> map = instances.get();
        if (map == null) {
            return Collections.emptyMap();
        } else {
            Map<Class<?>, CurrentInstance> copy = new HashMap<>();
            boolean removeStale = false;
            for (Entry<Class<?>, CurrentInstance> entry : map.entrySet()) {
                Class<?> c = entry.getKey();
                CurrentInstance ci = entry.getValue();
                if (ci.instance.get() == null) {
                    removeStale = true;
                } else {
                    copy.put(c, ci);
                }
            }
            if (removeStale) {
                removeStaleInstances(map);
                if (map.isEmpty()) {
                    instances.remove();
                }
            }
            return copy;
        }
    }

    /**
     * Sets current instances for the UI and all related classes. The previously
     * defined values can be restored by passing the returned map to
     * {@link #restoreInstances(Map)}.
     *
     *
     * @param ui
     *            The UI
     * @return A map containing the old values of the instances that this method
     *         updated.
     */
    public static Map<Class<?>, CurrentInstance> setCurrent(UI ui) {
        Map<Class<?>, CurrentInstance> old = setCurrent(ui.getSession());
        old.put(UI.class, doSet(UI.class, ui));
        return old;
    }

    /**
     * Sets current instances for the {@link VaadinSession} and all related
     * classes. The previously defined values can be restored by passing the
     * returned map to {@link #restoreInstances(Map)}.
     *
     *
     * @param session
     *            The VaadinSession
     * @return A map containing the old values of the instances this method
     *         updated.
     */
    public static Map<Class<?>, CurrentInstance> setCurrent(
            VaadinSession session) {
        Map<Class<?>, CurrentInstance> old = new HashMap<>();
        old.put(VaadinSession.class, doSet(VaadinSession.class, session));
        VaadinService service = null;
        if (session != null) {
            service = session.getService();
        }
        old.put(VaadinService.class, doSet(VaadinService.class, service));
        return old;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CurrentInstance.class.getName());
    }
}
