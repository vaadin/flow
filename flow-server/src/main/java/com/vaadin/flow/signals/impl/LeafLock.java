/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.signals.impl;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A component-level ("leaf") lock used to protect small pieces of in-memory
 * state in signal-reactivity building blocks such as effects, cached signals
 * and usage trackers.
 * <p>
 * These building blocks are notified from within a {@code SignalTree} while the
 * tree lock is held (through {@code notifyObservers}), and they also call back
 * into signal trees while updating their own state. If such a component held
 * its own lock while calling into a tree, two threads acquiring the component
 * lock and the tree lock in opposite orders would form an ABBA deadlock.
 * <p>
 * The invariant that avoids this is: <em>a leaf lock is a leaf</em> -- never
 * acquire a signal-tree lock while holding one. This class makes that invariant
 * enforceable: every held leaf lock is recorded in a thread-local, and
 * {@code SignalTree} asserts that none is held (ignoring reentrant tree
 * re-locks) before acquiring its lock. Because the tree cannot enumerate
 * arbitrary intrinsic monitors held by a caller, the leaf locks make themselves
 * known instead.
 * <p>
 * The check only runs under {@code -ea} (assertions), so it costs nothing in
 * production but turns a latent, timing-dependent ABBA into a deterministic
 * failure on any thread that violates the ordering.
 * 
 * @since 25.3
 */
public class LeafLock implements Serializable {

    /**
     * Represents a held {@link LeafLock}. Closing the handle releases the lock,
     * which makes it convenient to acquire the lock with try-with-resources.
     */
    public interface Handle extends AutoCloseable {
        @Override
        void close();
    }

    /*
     * Leaf locks currently held by the running thread, most-recent last. Used
     * only by assertions to detect the forbidden leaf-lock -> tree-lock
     * ordering. Never serialized.
     *
     * Deliberately not initialized with withInitial(): the read paths (invoked
     * by SignalTree on every tree-lock acquisition, often on pooled threads)
     * must not create a lingering thread-local entry. The deque is created
     * lazily when a leaf lock is first acquired and removed again as soon as
     * the last one is released, so a thread that never holds a leaf lock keeps
     * no thread-local state.
     */
    private static final ThreadLocal<Deque<LeafLock>> HELD = new ThreadLocal<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final String name;

    /**
     * Creates a new leaf lock with a descriptive name used in assertion
     * messages.
     *
     * @param name
     *            a human-readable name for diagnostics, not <code>null</code>
     */
    public LeafLock(String name) {
        this.name = name;
    }

    /**
     * Acquires this lock, recording it as held by the current thread. Close the
     * returned handle to release the lock, typically using try-with-resources.
     *
     * @return a handle that releases the lock when closed, not
     *         <code>null</code>
     */
    public Handle lock() {
        lock.lock();
        Deque<LeafLock> held = HELD.get();
        if (held == null) {
            held = new ArrayDeque<>();
            HELD.set(held);
        }
        held.addLast(this);
        return this::release;
    }

    private void release() {
        Deque<LeafLock> held = HELD.get();
        // Remove the most recent occurrence; with try-with-resources the held
        // locks are perfectly nested so this is the tail.
        if (held != null) {
            held.removeLastOccurrence(this);
            // Clean up the thread-local so pooled threads don't retain an
            // empty deque indefinitely.
            if (held.isEmpty()) {
                HELD.remove();
            }
        }
        lock.unlock();
    }

    /**
     * Checks whether the current thread holds this specific leaf lock.
     *
     * @return <code>true</code> if the current thread holds this lock
     */
    public boolean isHeldByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }

    /**
     * Checks whether the current thread holds any leaf lock.
     *
     * @return <code>true</code> if at least one leaf lock is held
     */
    public static boolean isAnyHeldByCurrentThread() {
        Deque<LeafLock> held = HELD.get();
        return held != null && !held.isEmpty();
    }

    /**
     * Describes the leaf locks currently held by the thread, for diagnostics.
     *
     * @return a comma-separated list of held leaf-lock names
     */
    public static String describeHeld() {
        Deque<LeafLock> held = HELD.get();
        if (held == null) {
            return "";
        }
        return held.stream().map(l -> l.name).collect(Collectors.joining(", "));
    }
}
