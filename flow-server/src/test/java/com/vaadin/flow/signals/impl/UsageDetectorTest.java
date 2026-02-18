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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.impl.UsageDetector.CircularSignalUsageException;
import com.vaadin.flow.signals.impl.UsageDetector.DeniedSignalUsageException;
import com.vaadin.flow.signals.impl.UsageDetector.MissingSignalUsageException;
import com.vaadin.flow.signals.impl.UsageDetector.PreventPrematureChangeUsage;
import com.vaadin.flow.signals.impl.UsageTracker.CombinedUsage;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsageDetectorTest extends SignalTestBase {

    @Test
    void createCollecting_noUsage_returnsNoUsage() {
        UsageDetector detector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            // No signal reads
            return null;
        }, detector).supply();

        assertSame(UsageTracker.NO_USAGE, detector.dependencies());
    }

    @Test
    void createCollecting_singleUsage_returnsSameUsage() {
        UsageDetector detector = UsageDetector.createCollecting();
        TestUsage testUsage = new TestUsage();

        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(testUsage);
            return null;
        }, detector).supply();

        Usage result = detector.dependencies();
        assertInstanceOf(PreventPrematureChangeUsage.class, result);
    }

    @Test
    void createCollecting_multipleUsages_returnsCombinedUsage() {
        UsageDetector detector = UsageDetector.createCollecting();

        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(new TestUsage());
            UsageTracker.registerUsage(new TestUsage());
            return null;
        }, detector).supply();

        assertInstanceOf(CombinedUsage.class, detector.dependencies());
    }

    @Test
    void createCollecting_dependenciesCalledTwice_returnsSameResult() {
        UsageDetector detector = UsageDetector.createCollecting();
        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(new TestUsage());
            return null;
        }, detector).supply();

        // Call dependencies() twice
        Usage first = detector.dependencies();
        Usage second = detector.dependencies();

        // Should return the same cached result
        assertSame(first, second);
    }

    @Test
    void createNecessary_noUsage_throwsMissingSignalUsageException() {
        UsageDetector detector = UsageDetector
                .createNecessary("Custom context message.");
        UsageTracker.tracked(() -> {
            // No signal reads
            return null;
        }, detector).supply();

        MissingSignalUsageException exception = assertThrows(
                MissingSignalUsageException.class,
                () -> detector.dependencies());
        assertTrue(exception.getMessage().contains("Custom context message."));
    }

    @Test
    void createNecessary_withUsage_returnsUsage() {
        UsageDetector detector = UsageDetector
                .createNecessary("Should not throw.");

        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(new TestUsage());
            return null;
        }, detector).supply();

        // Should not throw
        Usage result = detector.dependencies();
        assertInstanceOf(PreventPrematureChangeUsage.class, result);
    }

    @Test
    void missingSignalUsageException_messageFormat() {
        MissingSignalUsageException exception = new MissingSignalUsageException(
                "Test reason.");

        assertTrue(exception.getMessage().contains("Test reason."));
        assertTrue(exception.getMessage()
                .contains("Expected at least one signal value read"));
    }

    @Test
    void createDenied_registerUsage_throwsDeniedSignalUsageException() {
        UsageDetector detector = UsageDetector
                .createDenied("Signal access not allowed here.");

        DeniedSignalUsageException exception = assertThrows(
                DeniedSignalUsageException.class,
                () -> detector.register(new TestUsage()));
        assertTrue(exception.getMessage()
                .contains("Signal access not allowed here."));
    }

    @Test
    void createDenied_neverRegistered_returnsNoUsage() {
        UsageDetector detector = UsageDetector.createDenied("Test context.");

        // Never call register, just get dependencies
        assertSame(UsageTracker.NO_USAGE, detector.dependencies());
    }

    @Test
    void deniedSignalUsageException_messageFormat() {
        DeniedSignalUsageException exception = new DeniedSignalUsageException(
                "Custom context.");

        assertTrue(exception.getMessage().contains("Custom context."));
        assertTrue(exception.getMessage()
                .contains("Using signals is denied in this context"));
    }

    @Test
    void circularDependency_usageWithChangesAtRegistration_throwsCircularSignalUsageException() {
        UsageDetector detector = UsageDetector.createCollecting();

        // Register a usage that already has changes (simulating
        // write-before-read)
        TestUsage usageWithChanges = new TestUsage();
        usageWithChanges.hasChanges = true;

        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(usageWithChanges);
            return null;
        }, detector).supply();

        // dependencies() should detect circular dependency
        assertThrows(CircularSignalUsageException.class,
                () -> detector.dependencies());
    }

    @Test
    void circularDependency_usageWithoutChangesAtRegistration_noException() {
        UsageDetector detector = UsageDetector.createCollecting();

        // Register a usage that doesn't have changes
        TestUsage usageNoChanges = new TestUsage();
        usageNoChanges.hasChanges = false;

        UsageTracker.tracked(() -> {
            UsageTracker.registerUsage(usageNoChanges);
            return null;
        }, detector).supply();

        // Should not throw
        Usage result = detector.dependencies();
        assertInstanceOf(PreventPrematureChangeUsage.class, result);
    }

    @Test
    void circularSignalUsageException_hasStandardMessage() {
        CircularSignalUsageException exception = new CircularSignalUsageException();

        assertEquals("Infinite loop detected.", exception.getMessage());
    }

    @Test
    void preventPrematureChange_delegatesHasChanges() {
        TestUsage delegate = new TestUsage();
        PreventPrematureChangeUsage wrapper = new PreventPrematureChangeUsage(
                delegate);

        assertFalse(wrapper.hasChanges());

        delegate.hasChanges = true;
        assertTrue(wrapper.hasChanges());
    }

    @Test
    void preventPrematureChange_delegatesOnNextChange() {
        TestUsage delegate = new TestUsage();
        PreventPrematureChangeUsage wrapper = new PreventPrematureChangeUsage(
                delegate);

        TransientListener listener = immediate -> false;
        Registration registration = wrapper.onNextChange(listener);

        assertEquals(1, delegate.listeners.size());

        registration.remove();
        assertEquals(0, delegate.listeners.size());
    }

    @Test
    void preventPrematureChange_capturesChangesAtCreation() {
        TestUsage delegate = new TestUsage();
        delegate.hasChanges = true;

        PreventPrematureChangeUsage wrapper = new PreventPrematureChangeUsage(
                delegate);

        assertTrue(wrapper.hadChangesAtCreation());

        // Even if delegate changes later, the captured value remains
        delegate.hasChanges = false;
        assertTrue(wrapper.hadChangesAtCreation());
    }

    private static class TestUsage implements Usage {
        boolean hasChanges;
        List<TransientListener> listeners = new ArrayList<>();

        @Override
        public boolean hasChanges() {
            return hasChanges;
        }

        @Override
        @NonNull
        public Registration onNextChange(@NonNull TransientListener listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }
    }
}
