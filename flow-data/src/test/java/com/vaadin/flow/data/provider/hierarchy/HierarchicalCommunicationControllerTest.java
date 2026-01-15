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
package com.vaadin.flow.data.provider.hierarchy;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalArrayUpdater.HierarchicalUpdate;

import elemental.json.JsonValue;

/**
 * Tests for {@link HierarchicalCommunicationController}, specifically the
 * flush() method race condition fix for issue #21731.
 */
public class HierarchicalCommunicationControllerTest {

    private static final String ROOT = "ROOT";
    private static final String PARENT_KEY = "parent-key";

    @Mock
    private DataKeyMapper<String> keyMapper;

    @Mock
    private HierarchyMapper<String, ?> mapper;

    @Mock
    private DataGenerator<String> dataGenerator;

    private boolean updateStarted;
    private boolean commitCalled;

    private class TestUpdate implements HierarchicalUpdate {
        @Override
        public void clear(int start, int length) {
        }

        @Override
        public void set(int start, List<JsonValue> items) {
        }

        @Override
        public void commit(int updateId) {
            commitCalled = true;
        }

        @Override
        public void enqueue(String name, Serializable... arguments) {
        }

        @Override
        public void set(int start, List<JsonValue> items, String parentKey) {
        }

        @Override
        public void clear(int start, int length, String parentKey) {
        }

        @Override
        public void commit(int updateId, String parentKey, int levelSize) {
            commitCalled = true;
        }

        @Override
        public void commit() {
            commitCalled = true;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        updateStarted = false;
        commitCalled = false;
    }

    /**
     * Test that flush is skipped when the parent item is no longer expanded.
     * This is the main fix for issue #21731 - preventing
     * IndexOutOfBoundsException when an item is collapsed before the scheduled
     * flush executes.
     *
     * Without the fix, countChildItems would be called on a collapsed parent,
     * leading to inconsistent state. This test verifies the early return
     * prevents this.
     */
    @Test
    public void flush_parentCollapsedBeforeFlush_flushSkipped() {
        // Setup: parent key exists in mapper but is collapsed
        Mockito.when(keyMapper.get(PARENT_KEY)).thenReturn(ROOT);
        Mockito.when(mapper.isExpanded(ROOT)).thenReturn(false);

        HierarchicalCommunicationController<String> controller = createController(
                PARENT_KEY);
        controller.setRequestRange(0, 10);

        // Execute flush - should be skipped because parent is collapsed
        controller.flush();

        // Verify: no update was started (flush was skipped early)
        Assert.assertFalse(
                "Flush should be skipped when parent is collapsed, but update was started",
                updateStarted);
        Assert.assertFalse(
                "Commit should not be called when parent is collapsed",
                commitCalled);
    }

    /**
     * Test that flush proceeds normally when the parent item is still expanded.
     */
    @Test
    public void flush_parentStillExpanded_flushProceeds() {
        // Setup: parent key exists and is expanded
        Mockito.when(keyMapper.get(PARENT_KEY)).thenReturn(ROOT);
        Mockito.when(mapper.isExpanded(ROOT)).thenReturn(true);

        HierarchicalCommunicationController<String> controller = createController(
                PARENT_KEY);
        controller.setRequestRange(0, 10);

        // Execute flush - should proceed normally
        controller.flush();

        // Verify: update was started (flush proceeded)
        Assert.assertTrue(
                "Flush should proceed when parent is expanded, but update was not started",
                updateStarted);
    }

    /**
     * Test that root-level controller (parentKey is null) always flushes,
     * regardless of expand state.
     */
    @Test
    public void flush_rootController_alwaysFlushes() {
        // Setup: root controller with null parentKey
        Mockito.when(keyMapper.get(null)).thenReturn(null);

        HierarchicalCommunicationController<String> controller = createController(
                null);
        controller.setRequestRange(0, 10);

        // Execute flush - should always proceed for root controller
        controller.flush();

        // Verify: update was started (flush proceeded)
        Assert.assertTrue(
                "Root controller should always flush, but update was not started",
                updateStarted);
    }

    /**
     * Test that flush proceeds when the parent key exists but the item is no
     * longer in the keyMapper (null returned). This preserves existing behavior
     * where the flush continues to handle cleanup scenarios.
     */
    @Test
    public void flush_parentItemRemovedFromKeyMapper_flushProceeds() {
        // Setup: parent key exists but item is null in keyMapper
        Mockito.when(keyMapper.get(PARENT_KEY)).thenReturn(null);

        HierarchicalCommunicationController<String> controller = createController(
                PARENT_KEY);
        controller.setRequestRange(0, 10);

        // Execute flush - should proceed (existing behavior)
        controller.flush();

        // Verify: update was started (flush proceeded)
        Assert.assertTrue(
                "Flush should proceed when parent item is null in keyMapper, but update was not started",
                updateStarted);
    }

    private HierarchicalCommunicationController<String> createController(
            String parentKey) {
        return new HierarchicalCommunicationController<>(parentKey, keyMapper,
                mapper, dataGenerator, size -> {
                    updateStarted = true;
                    return new TestUpdate();
                }, (pkey, range) -> Stream.empty());
    }
}
