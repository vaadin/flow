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
package com.vaadin.flow.internal;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.tests.util.TestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionCacheTest {

    @Test
    public void generateCachedValues() {
        AtomicInteger count = new AtomicInteger();

        ReflectionCache<Object, Integer> cache = new ReflectionCache<>(
                type -> count.incrementAndGet());

        assertEquals(0, count.get());

        assertEquals(1, cache.get(Object.class).intValue());
        assertEquals(2, cache.get(String.class).intValue());

        assertEquals(1, cache.get(Object.class).intValue());

        assertEquals(2, count.get());
    }

    @Test
    public void cacheContains() {
        ReflectionCache<Object, Object> cache = new ReflectionCache<>(
                type -> type);

        assertFalse(cache.contains(Object.class));

        cache.get(Object.class);
        assertTrue(cache.contains(Object.class));
        assertFalse(cache.contains(String.class));
    }

    @Test
    public void cacheClear() {
        ReflectionCache<Object, Object> cache = new ReflectionCache<>(
                type -> type);

        cache.get(Object.class);
        assertTrue(cache.contains(Object.class));

        cache.clear();
        assertFalse(cache.contains(Object.class));
    }

    @Test
    public void cacheClearEntry() {
        ReflectionCache<Number, Object> cache = new ReflectionCache<>(
                type -> type);

        cache.get(Integer.class);
        cache.get(Double.class);
        cache.get(Long.class);
        assertTrue(cache.contains(Integer.class));
        assertTrue(cache.contains(Double.class));
        assertTrue(cache.contains(Long.class));

        cache.clear(Double.class);
        assertTrue(cache.contains(Integer.class));
        assertFalse(cache.contains(Double.class));
        assertTrue(cache.contains(Long.class));

        cache.clear(Integer.class);
        assertFalse(cache.contains(Integer.class));
        assertFalse(cache.contains(Double.class));
        assertTrue(cache.contains(Long.class));

        cache.clear(Long.class);
        assertFalse(cache.contains(Integer.class));
        assertFalse(cache.contains(Double.class));
        assertFalse(cache.contains(Long.class));

    }

    @Test
    public void clearAll() {
        ReflectionCache<Object, Object> cache1 = new ReflectionCache<>(
                type -> type);
        ReflectionCache<Object, Object> cache2 = new ReflectionCache<>(
                type -> type);

        cache1.get(Object.class);
        cache2.get(Object.class);

        ReflectionCache.clearAll();

        assertFalse(cache1.contains(Object.class));
        assertFalse(cache2.contains(Object.class));
    }

    @Test
    public void clearAllForGivenType() {
        ReflectionCache<Number, Object> cache1 = new ReflectionCache<>(
                type -> type);

        ReflectionCache<Number, Object> cache2 = new ReflectionCache<>(
                type -> type);

        cache1.get(Integer.class);
        cache1.get(Double.class);
        cache2.get(Float.class);
        cache2.get(Double.class);

        ReflectionCache.clearAll(Integer.class);

        assertFalse(cache1.contains(Integer.class));
        assertTrue(cache1.contains(Double.class));
        assertTrue(cache2.contains(Float.class));
        assertTrue(cache2.contains(Double.class));

        ReflectionCache.clearAll(Double.class);
        assertFalse(cache1.contains(Double.class));
        assertTrue(cache2.contains(Float.class));
        assertFalse(cache2.contains(Double.class));
    }

    @Test
    public void cacheIsGarbageCollected() throws InterruptedException {
        ReflectionCache<Object, Object> cache1 = new ReflectionCache<>(
                type -> type);
        WeakReference<ReflectionCache<Object, Object>> ref = new WeakReference<>(
                cache1);

        cache1 = null;
        assertTrue(TestUtil.isGarbageCollected(ref));
    }

    @Test
    public void cacheIsClearedAfterGc() throws InterruptedException {
        ReflectionCache<Object, Object> cache = new ReflectionCache<>(
                type -> type);
        cache.get(Object.class);

        // Ensure garbage is collected before clearing
        TestUtil.isGarbageCollected(new WeakReference<>(new Object()));

        ReflectionCache.clearAll();

        assertFalse(cache.contains(Object.class));
    }

    @Test
    public void currentInstancesNotAvailable() {
        String currentString = "My string";
        CurrentInstance.set(String.class, currentString);

        ReflectionCache<Object, String> cache = new ReflectionCache<>(
                type -> type.getSimpleName() + ": "
                        + CurrentInstance.get(String.class));

        try {
            String result = cache.get(Object.class);

            assertEquals("Object: null", result,
                    "Current instance should not be in the result");
            assertEquals(currentString, CurrentInstance.get(String.class),
                    "Current instance should be preserved after running");
        } finally {
            CurrentInstance.set(String.class, null);
        }
    }
}
