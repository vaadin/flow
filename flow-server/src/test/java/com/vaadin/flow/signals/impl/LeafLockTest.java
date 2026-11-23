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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verifies that the leaf-lock ordering assertion fires when a signal-tree lock
 * is acquired while a {@link LeafLock} is held.
 */
class LeafLockTest extends SignalTestBase {

    private static boolean assertionsEnabled() {
        boolean enabled = false;
        assert enabled = true;
        return enabled;
    }

    // Acquiring a signal-tree lock while holding a LeafLock trips the assertion
    // deterministically, on any thread, without needing the actual timing race
    // to occur.
    @Test
    void treeLockWhileHoldingLeafLock_assertionFires() {
        assumeTrue(assertionsEnabled(),
                "requires -ea; surefire enables assertions by default");

        SharedValueSignal<String> signal = new SharedValueSignal<>("v");
        LeafLock leaf = new LeafLock("probe");

        try (var ignored = leaf.lock()) {
            // peek() -> getWithLock() -> assertNoLeafLockHeld() -> fails
            AssertionError error = assertThrows(AssertionError.class,
                    signal::peek);
            assertTrue(String.valueOf(error.getMessage()).contains("leaf lock"),
                    "unexpected message: " + error.getMessage());
        }

        // Reference direction (no leaf lock held) is fine.
        signal.peek();
    }
}
