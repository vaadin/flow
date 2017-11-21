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
package com.vaadin.flow.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ReflectionCacheTest {

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
}
