/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.hotswap.HotswapClassEvent;
import com.vaadin.flow.server.MockVaadinServletService;

public class ReflectionCacheHotswapperTest {

    private MockVaadinServletService service = new MockVaadinServletService(
            false);
    private ReflectionCacheHotswapper hotswapper = new ReflectionCacheHotswapper();
    private ReflectionCache<CacheKey, CacheValue> cache = new ReflectionCache<>(
            CacheValue::new);

    @Test
    public void onClassesChange_classCached_clearCache() {
        cache.get(CacheKey.class).value = "BASE";
        cache.get(CacheSubKey.class).value = "SUBCLASS";
        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(CacheKey.class), true));

        Assert.assertNull("Should have clean cache for cached class change",
                cache.get(CacheKey.class).value);
        Assert.assertEquals(
                "Should not have cleared cache for other cached class",
                "SUBCLASS", cache.get(CacheSubKey.class).value);
    }

    @Test
    public void onClassesChange_classNotCached_doNotClearCache() {
        cache.get(CacheKey.class).value = "BEFORE";
        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(String.class), true));

        Assert.assertEquals(
                "Should not have cleared cache for non cached class change",
                "BEFORE", cache.get(CacheKey.class).value);
    }

    static class CacheKey {
    }

    static class CacheSubKey extends CacheKey {
    }

    static class CacheValue {
        private final Class<CacheKey> key;
        private String value;

        CacheValue(Class<CacheKey> key) {
            this.key = key;
        }

    }
}
