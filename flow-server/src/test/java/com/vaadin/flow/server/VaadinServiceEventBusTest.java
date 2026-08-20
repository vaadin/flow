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
package com.vaadin.flow.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

public class VaadinServiceEventBusTest {

    public static class TestEvent extends EventObject {
        public TestEvent(VaadinService service) {
            super(service);
        }
    }

    public static class SubTestEvent extends TestEvent {
        public SubTestEvent(VaadinService service) {
            super(service);
        }
    }

    public static class OtherEvent extends EventObject {
        public OtherEvent(VaadinService service) {
            super(service);
        }
    }

    private final VaadinService service = new MockVaadinServletService();

    private final VaadinServiceEventBus eventBus = service.getEventBus();

    @Test
    public void listenersNotifiedInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        eventBus.addListener(TestEvent.class, event -> calls.add("first"));
        eventBus.addListener(TestEvent.class, event -> calls.add("second"));

        eventBus.fireEvent(new TestEvent(service),
                VaadinServiceEventBus.logErrors());

        Assert.assertEquals(List.of("first", "second"), calls);
    }

    @Test
    public void eventDeliveredToListenersOfExactTypeOnly() {
        List<String> calls = new ArrayList<>();
        eventBus.addListener(TestEvent.class, event -> calls.add("super"));
        eventBus.addListener(SubTestEvent.class, event -> calls.add("sub"));
        eventBus.addListener(OtherEvent.class, event -> calls.add("other"));

        eventBus.fireEvent(new SubTestEvent(service),
                VaadinServiceEventBus.logErrors());

        Assert.assertEquals(List.of("sub"), calls);
    }

    @Test
    public void firingEventWithoutListeners_doesNothing() {
        eventBus.fireEvent(new TestEvent(service),
                VaadinServiceEventBus.logErrors());
    }

    @Test
    public void hasListener_tracksRegistrationAndRemoval() {
        Assert.assertFalse(eventBus.hasListener(TestEvent.class));

        Registration registration = eventBus.addListener(TestEvent.class,
                event -> {
                });
        Assert.assertTrue(eventBus.hasListener(TestEvent.class));
        Assert.assertFalse(eventBus.hasListener(SubTestEvent.class));

        registration.remove();
        Assert.assertFalse(eventBus.hasListener(TestEvent.class));
    }

    @Test
    public void removedListener_isNotNotified() {
        List<String> calls = new ArrayList<>();
        Registration registration = eventBus.addListener(TestEvent.class,
                event -> calls.add("removed"));
        eventBus.addListener(TestEvent.class, event -> calls.add("kept"));

        registration.remove();
        // Removal is idempotent and must not drop the other listener
        registration.remove();
        eventBus.fireEvent(new TestEvent(service),
                VaadinServiceEventBus.logErrors());

        Assert.assertEquals(List.of("kept"), calls);
    }

    @Test
    public void sameListenerAddedTwice_notifiedOncePerRegistration() {
        List<String> calls = new ArrayList<>();
        SerializableConsumer<TestEvent> listener = event -> calls.add("call");
        eventBus.addListener(TestEvent.class, listener);
        Registration second = eventBus.addListener(TestEvent.class, listener);

        eventBus.fireEvent(new TestEvent(service),
                VaadinServiceEventBus.logErrors());
        Assert.assertEquals(List.of("call", "call"), calls);

        // Removing one registration leaves the other one in place
        calls.clear();
        second.remove();
        eventBus.fireEvent(new TestEvent(service),
                VaadinServiceEventBus.logErrors());
        Assert.assertEquals(List.of("call"), calls);
    }

    @Test
    public void getListeners_returnsListenersOfGivenType() {
        Assert.assertTrue(eventBus.getListeners(TestEvent.class).isEmpty());

        eventBus.addListener(TestEvent.class, event -> {
        });
        eventBus.addListener(OtherEvent.class, event -> {
        });

        Assert.assertEquals(1, eventBus.getListeners(TestEvent.class).size());
    }

    @Test
    public void rethrowingErrorHandler_propagatesAndStopsDelivery() {
        List<String> calls = new ArrayList<>();
        eventBus.addListener(TestEvent.class, event -> {
            throw new IllegalStateException("boom");
        });
        eventBus.addListener(TestEvent.class, event -> calls.add("second"));

        IllegalStateException thrown = Assert
                .assertThrows(IllegalStateException.class, () -> eventBus
                        .fireEvent(new TestEvent(service), (event, error) -> {
                            throw (RuntimeException) error;
                        }));

        Assert.assertEquals("boom", thrown.getMessage());
        Assert.assertTrue(calls.isEmpty());
    }

    @Test
    public void firingWithoutErrorHandler_isRejected() {
        Assert.assertThrows(NullPointerException.class,
                () -> eventBus.fireEvent(new TestEvent(service), null));
        Assert.assertThrows(NullPointerException.class, () -> eventBus
                .fireEventInReverseOrder(new TestEvent(service), null));
    }

    @Test
    public void errorHandler_receivesCheckedExceptionThrownByListener() {
        List<Exception> errors = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        Exception checked = new Exception("checked");
        eventBus.addListener(TestEvent.class, event -> {
            sneakyThrow(checked);
        });
        eventBus.addListener(TestEvent.class, event -> calls.add("second"));

        eventBus.fireEvent(new TestEvent(service),
                (event, error) -> errors.add(error));

        Assert.assertEquals(List.of(checked), errors);
        Assert.assertEquals(List.of("second"), calls);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable throwable)
            throws E {
        throw (E) throwable;
    }

    @Test
    public void withErrorHandler_failingListenerDoesNotStopDelivery() {
        List<String> calls = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        eventBus.addListener(TestEvent.class, event -> {
            throw new IllegalStateException("boom");
        });
        eventBus.addListener(TestEvent.class, event -> calls.add("second"));

        TestEvent event = new TestEvent(service);
        eventBus.fireEvent(event, (firedEvent, error) -> {
            Assert.assertSame(event, firedEvent);
            errors.add(error);
        });

        Assert.assertEquals(List.of("second"), calls);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("boom", errors.get(0).getMessage());
    }

    @Test
    public void fireEventInReverseOrder_notifiesLastRegisteredFirst() {
        List<String> calls = new ArrayList<>();
        eventBus.addListener(TestEvent.class, event -> calls.add("first"));
        eventBus.addListener(TestEvent.class, event -> calls.add("second"));
        eventBus.addListener(TestEvent.class, event -> calls.add("third"));

        eventBus.fireEventInReverseOrder(new TestEvent(service),
                VaadinServiceEventBus.logErrors());

        Assert.assertEquals(List.of("third", "second", "first"), calls);
    }

    @Test
    public void listenerAddedWhileFiring_isNotNotifiedForTheOngoingEvent() {
        List<String> calls = new ArrayList<>();
        eventBus.addListener(TestEvent.class, event -> eventBus
                .addListener(TestEvent.class, nested -> calls.add("nested")));

        eventBus.fireEvent(new TestEvent(service),
                VaadinServiceEventBus.logErrors());

        Assert.assertTrue(calls.isEmpty());
    }

    @Test
    public void getService_returnsOwningService() {
        Assert.assertSame(service, eventBus.getService());
    }

    @Test
    public void concurrentAddFireAndRemove_everyListenerIsNotified()
            throws Exception {
        int threads = 4;
        int rounds = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                start.await();
                for (int round = 0; round < rounds; round++) {
                    AtomicBoolean notified = new AtomicBoolean();
                    Registration registration = eventBus.addListener(
                            TestEvent.class, event -> notified.set(true));
                    eventBus.fireEvent(new TestEvent(service),
                            VaadinServiceEventBus.logErrors());
                    registration.remove();
                    if (!notified.get()) {
                        throw new AssertionError(
                                "A registered listener was not notified");
                    }
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> result : results) {
            result.get();
        }
        executor.shutdown();

        Assert.assertFalse(eventBus.hasListener(TestEvent.class));
        Assert.assertTrue(eventBus.getListeners(TestEvent.class).isEmpty());
    }

    @Test
    public void registration_isSerializableWithoutTheService()
            throws Exception {
        Registration registration = eventBus.addListener(TestEvent.class,
                new SerializableTestListener());

        // The service itself is not serializable, so this also asserts that
        // holding on to a registration does not keep the service reachable
        byte[] serialized;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(registration);
            serialized = bytes.toByteArray();
        }

        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(serialized))) {
            Assert.assertNotNull(in.readObject());
        }
    }

    private static class SerializableTestListener
            implements SerializableConsumer<TestEvent> {
        @Override
        public void accept(TestEvent event) {
            // Does nothing, only needs to be serializable
        }
    }
}
