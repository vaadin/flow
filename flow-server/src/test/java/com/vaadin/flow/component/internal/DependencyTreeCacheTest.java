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
package com.vaadin.flow.component.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.SerializableFunction;

public class DependencyTreeCacheTest {
    @Test
    public void multipleLevels_allIncluded_noneParsedAgain() {
        MockParser parser = new MockParser().addResult("/a", "/b")
                .addResult("/b", "/c").addResult("/c");

        DependencyTreeCache<String> cache = new DependencyTreeCache<>(parser);
        Set<String> dependencies = cache.getDependencies("/a");

        parser.assertConsumed();
        Assert.assertEquals(new HashSet<>(Arrays.asList("/a", "/b", "/c")),
                dependencies);

        // Parse again, should not trigger exception because things are parsed
        // multiple times
        Set<String> dependencies2 = cache.getDependencies("/a");
        Assert.assertEquals(dependencies, dependencies2);
    }

    @Test
    public void sharedDependency_onlyParsedOnce() {
        MockParser parser = new MockParser().addResult("/a", "/b", "/c")
                .addResult("/b", "/d").addResult("/c", "/d").addResult("/d");

        DependencyTreeCache<String> cache = new DependencyTreeCache<>(parser);
        Set<String> dependencies = cache.getDependencies("/a");

        parser.assertConsumed();
        Assert.assertEquals(
                new HashSet<>(Arrays.asList("/a", "/b", "/c", "/d")),
                dependencies);
    }

    @Test
    public void concurrentParse_onlyParsedOnce() throws InterruptedException {
        MockParser parser = new MockParser();
        parser.addResult("/a", "/b");
        parser.addResult("/b", 100);

        DependencyTreeCache<String> cache = new DependencyTreeCache<>(parser);

        long start = System.currentTimeMillis();

        Set<String> threadResult = new HashSet<>();
        Thread thread = new Thread(() -> {
            threadResult.addAll(cache.getDependencies("/a"));
        });
        thread.start();

        Set<String> dependencies = cache.getDependencies("/a");

        thread.join();

        long end = System.currentTimeMillis();

        Assert.assertEquals(new HashSet<>(Arrays.asList("/a", "/b")),
                dependencies);
        Assert.assertEquals(dependencies, threadResult);

        long duration = (end - start);

        Assert.assertTrue("Parsing should take less than 200 ms",
                duration < 200);
        Assert.assertTrue(
                "Parsing should take at least 100 ms, was " + duration,
                duration >= 100);
    }

    @Test
    public void parallelParsing_potentialSpeedup() throws InterruptedException {
        // Eventually, we should see both a case when the randomization makes
        // parsing progress in parallel and a case when it happens sequentially
        int maxDuration = Integer.MIN_VALUE;
        int minDuration = Integer.MAX_VALUE;

        int iterationCount = 0;
        while (minDuration > 75 || maxDuration < 75) {
            if (iterationCount++ > 30) {
                // Less than 1/10^9 chance that 50/50 randomization will give
                // the same result 30 times in a row
                Assert.fail(
                        "Did not observe both slowdown and speedup. Max duration: "
                                + maxDuration + ", min duration: "
                                + minDuration);
            }

            MockParser parser = new MockParser().addResult("/a", 25, "/b", "/c")
                    .addResult("/b", 25).addResult("/c", 25);
            DependencyTreeCache<String> cache = new DependencyTreeCache<>(
                    parser);

            Thread thread = new Thread(() -> cache.getDependencies("/a"));

            long start = System.currentTimeMillis();

            thread.start();
            cache.getDependencies("/a");
            thread.join();

            long end = System.currentTimeMillis();
            int duration = (int) (end - start);

            Assert.assertTrue(
                    "Duration should never be less than 50, was " + duration,
                    duration >= 50);

            maxDuration = Math.max(maxDuration, duration);
            minDuration = Math.min(minDuration, duration);
        }
    }

    @FunctionalInterface
    private interface Blocker {
        public void block() throws InterruptedException;
    }

    private static class MockParser
            implements SerializableFunction<String, Collection<String>> {
        private static final Supplier<Collection<String>> USED_PLACEHOLDER = () -> Collections
                .emptySet();

        private Map<String, Supplier<Collection<String>>> items = new ConcurrentHashMap<>();

        @Override
        public Collection<String> apply(String path) {
            path = path.replaceFirst("^frontend://", "");

            // Get value and mark it as used
            Supplier<Collection<String>> supplier = items.put(path,
                    USED_PLACEHOLDER);

            if (supplier == USED_PLACEHOLDER) {
                Assert.fail("Path " + path + " has already been parsed");
            } else if (supplier == null) {
                Assert.fail("Parser cannot find " + path);
            }

            return supplier.get();
        }

        public MockParser addResult(String path, String... dependencies) {
            return addResult(path, null, dependencies);
        }

        public MockParser addResult(String path, int duration,
                String... dependencies) {
            return addResult(path, () -> Thread.sleep(duration), dependencies);
        }

        public MockParser addResult(String path, Blocker blocker,
                String... dependencies) {
            items.put(path, () -> {
                if (blocker != null) {
                    try {
                        blocker.block();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                return Arrays.asList(dependencies);
            });
            return this;
        }

        public void assertConsumed() {
            items.forEach((path, value) -> Assert.assertSame(
                    path + " should have been parsed", USED_PLACEHOLDER,
                    value));
        }
    }

}
