/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.tests.util.TestUtil;

public class ReflectionCacheTest {

    @Test
    public void generateCachedValues() {
        AtomicInteger count = new AtomicInteger();

        ReflectionCache<Object, Integer> cache = new ReflectionCache<>(
                type -> count.incrementAndGet());

        Assert.assertEquals(0, count.get());

        Assert.assertEquals(1, cache.get(Object.class).intValue());
        Assert.assertEquals(2, cache.get(String.class).intValue());

        Assert.assertEquals(1, cache.get(Object.class).intValue());

        Assert.assertEquals(2, count.get());
    }

    @Test
    public void cacheContains() {
        ReflectionCache<Object, Object> cache = new ReflectionCache<>(
                type -> type);

        Assert.assertFalse(cache.contains(Object.class));

        cache.get(Object.class);
        Assert.assertTrue(cache.contains(Object.class));
        Assert.assertFalse(cache.contains(String.class));
    }

    @Test
    public void cacheClear() {
        ReflectionCache<Object, Object> cache = new ReflectionCache<>(
                type -> type);

        cache.get(Object.class);
        Assert.assertTrue(cache.contains(Object.class));

        cache.clear();
        Assert.assertFalse(cache.contains(Object.class));
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

        Assert.assertFalse(cache1.contains(Object.class));
        Assert.assertFalse(cache2.contains(Object.class));
    }

    @Test
    public void cacheIsGarbageCollected() throws InterruptedException {
        ReflectionCache<Object, Object> cache1 = new ReflectionCache<>(
                type -> type);
        WeakReference<ReflectionCache<Object, Object>> ref = new WeakReference<>(
                cache1);

        cache1 = null;
        Assert.assertTrue(TestUtil.isGarbageCollected(ref));
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

            Assert.assertEquals("Current instance should not be in the result",
                    "Object: null", result);
            Assert.assertEquals(
                    "Current instance should be preserved after running",
                    currentString, CurrentInstance.get(String.class));
        } finally {
            CurrentInstance.set(String.class, null);
        }
    }
}
