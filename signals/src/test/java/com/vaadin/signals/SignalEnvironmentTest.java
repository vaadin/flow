package com.vaadin.signals;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SignalEnvironmentTest extends SignalTestBase {
    /*
     * Some parts of SignalEnvironment are designed as global variables that are
     * initialized only once. This means that there's no reasonable way of
     * testing behavior for the initial state before initialization has
     * happened.
     */

    @Test
    void initialize_nullValues_throwsNpe() {
        assertThrows(NullPointerException.class, () -> {
            SignalEnvironment.initialize(null, Runnable::run);
        });

        assertThrows(NullPointerException.class, () -> {
            SignalEnvironment.initialize(new ObjectMapper(), null);
        });
    }

    @Test
    void initialize_alradyInitialzed_throwsIse() {
        assertThrows(IllegalStateException.class, () -> {
            SignalEnvironment.initialize(new ObjectMapper(), Runnable::run);
        });
    }

    @Test
    void tryInitialize_nullValues_throwsNpe() {
        assertThrows(NullPointerException.class, () -> {
            SignalEnvironment.tryInitialize(null, Runnable::run);
        });

        assertThrows(NullPointerException.class, () -> {
            SignalEnvironment.tryInitialize(new ObjectMapper(), null);
        });
    }

    @Test
    void tryInitialize_alreadyInitialzed_returnsFalse() {
        boolean result = SignalEnvironment.tryInitialize(new ObjectMapper(),
                Runnable::run);

        assertFalse(result);
    }

    @Test
    void initialized_initialized_returnsTrue() {
        assertTrue(SignalEnvironment.initialized());
    }

    @Test
    void defaultDispatcher_asInitialized() {
        Executor dispatcher = SignalEnvironment.defaultDispatcher();

        assertSame(SignalTestBase.dispatcher, dispatcher);
    }

    @Test
    void asynchronousDispatcher_noOverride_usesDefaultDispatcher() {
        Executor dispatcher = SignalEnvironment.asynchronousDispatcher();

        assertSame(SignalEnvironment.defaultDispatcher(), dispatcher);
    }

    @Test
    void asynchronousDispatcher_hasOverride_usesOverrideDispatcher() {
        TestExecutor overrideDispatcher = useTestOverrideDispatcher();

        Executor dispatcher = SignalEnvironment.asynchronousDispatcher();

        assertSame(overrideDispatcher, dispatcher);
    }

    @Test
    void synchronousDispatcher_noOverride_runsDirectly() {
        Thread currentThread = Thread.currentThread();
        AtomicReference<Thread> dispatcherThread = new AtomicReference<>();

        Executor dispatcher = SignalEnvironment.synchronousDispatcher();
        dispatcher.execute(() -> {
            dispatcherThread.set(Thread.currentThread());
        });

        assertSame(currentThread, dispatcherThread.get());
    }

    @Test
    void synchronousDispatcher_hasOverride_usesOverrideDispatcher() {
        TestExecutor overrideDispatcher = useTestOverrideDispatcher();

        Executor dispatcher = SignalEnvironment.synchronousDispatcher();

        assertSame(overrideDispatcher, dispatcher);
    }

    @Test
    void addDispatcherOverride_addAndRemove_notUsedAfterRemove() {
        Thread currentThread = Thread.currentThread();
        Executor override = Runnable::run;

        Runnable remover = SignalEnvironment.addDispatcherOverride(() -> {
            if (Thread.currentThread() == currentThread) {
                return override;
            } else {
                return null;
            }
        });

        Executor dispatcher = SignalEnvironment.asynchronousDispatcher();
        assertSame(override, dispatcher);

        remover.run();

        dispatcher = SignalEnvironment.asynchronousDispatcher();
        assertSame(SignalEnvironment.defaultDispatcher(), dispatcher);
    }
}
