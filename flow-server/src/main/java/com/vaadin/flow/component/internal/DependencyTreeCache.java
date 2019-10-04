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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.vaadin.flow.function.SerializableFunction;

/**
 * A caching tree traverser for collecting and parsing dependencies.
 *
 * @author Vaadin Ltd
 * @since 1.2
 * @param <T>
 *            the value type
 */
public class DependencyTreeCache<T> implements Serializable {
    /**
     * Maps a path to a list of dependencies or a placeholder indicating that
     * parsing is in progress.
     */
    private final ConcurrentHashMap<T, Object> cache = new ConcurrentHashMap<>();

    private final SerializableFunction<T, Collection<T>> dependencyParser;

    /**
     * Creates a dependency cache with the given dependency parser.
     *
     * @param dependencyParser
     *            a potentially slow callback function that finds the direct
     *            dependencies for any given value
     */
    public DependencyTreeCache(
            SerializableFunction<T, Collection<T>> dependencyParser) {
        this.dependencyParser = dependencyParser;
    }

    /**
     * Collects all transitive dependencies of the given node, including the
     * node itself.
     *
     * @param node
     *            the node for which to collect dependencies
     * @return the transitive dependencies of the given node
     */
    public Set<T> getDependencies(T node) {
        HashSet<T> result = new HashSet<>();

        LinkedList<T> pendingKeys = new LinkedList<>();
        pendingKeys.add(node);

        try {
            while (!pendingKeys.isEmpty()) {
                T path = pendingKeys.removeLast();

                if (result.add(path)) {
                    getOrParseDependencies(path).forEach(pendingKeys::add);
                }
            }
        } catch (InterruptedException e) {
            // Restore interrupted state
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Interrputed while finding dependencies for " + node, e);
        }

        return result;
    }

    private Collection<T> getOrParseDependencies(T node)
            throws InterruptedException {
        Object placeholder = new Object();
        Object valueOrPlaceholder = cache.putIfAbsent(node, placeholder);
        if (valueOrPlaceholder instanceof Collection<?>) {
            /*
             * Happy path: cache contained the dependencies.
             */
            @SuppressWarnings("unchecked")
            Collection<T> dependencies = (Collection<T>) valueOrPlaceholder;

            return dependencies;
        } else if (valueOrPlaceholder == null) {
            /*
             * No previous value in the cache. This means that we were the first
             * to look for this node. In that case, we should use the parser to
             * find dependencies and then notify anyone else who have found the
             * placeholder we put into the cache and is now waiting for the real
             * result.
             */
            Collection<T> dependencies = dependencyParser.apply(node);

            cache.put(node, dependencies);

            synchronized (placeholder) {
                placeholder.notifyAll();
            }

            return dependencies;
        } else {
            /*
             * We got a placeholder that has been put there by another thread.
             * Wait until the other thread is done parsing and has put the real
             * dependencies into the cache.
             */
            return waitForDependencies(node, valueOrPlaceholder);
        }
    }

    private Collection<T> waitForDependencies(T node, Object placeholder)
            throws InterruptedException {
        synchronized (placeholder) {
            // Loop because of spurious wakeups
            while (true) {
                Object valueOrPlaceholder = cache.get(node);
                if (valueOrPlaceholder == null) {
                    /*
                     * Special case if clear() happens after a result was added
                     * to the cache, but before this thread got notified. Ensure
                     * that parsing happens again. The end result is thus the
                     * same as if clear() happened already before this thread
                     * found the placeholder.
                     */
                    return getOrParseDependencies(node);
                } else if (valueOrPlaceholder != placeholder) {

                    @SuppressWarnings("unchecked")
                    Collection<T> dependencies = (Collection<T>) valueOrPlaceholder;

                    /*
                     * The thread that did the parsing will most likely be
                     * working on the first dependency by the time we get here.
                     * To reduce the risk that we'll end up waiting for the same
                     * thread again when traversing children, we randomize our
                     * own traversal order. This increases the probability that
                     * all threads querying for the same information will
                     * contribute the needed parsing work.
                     */
                    List<T> shuffledDependencies = new ArrayList<>(
                            dependencies);
                    Collections.shuffle(shuffledDependencies,
                            ThreadLocalRandom.current());
                    return shuffledDependencies;
                }

                placeholder.wait();
            }
        }
    }

    /**
     * Clears all the contents of the cache. A lookup that is in progress while
     * the cache is cleared may return a result that combines previously cached
     * dependencies with newly parsed dependencies.
     */
    public void clear() {
        cache.clear();
    }
}
