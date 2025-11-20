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
package com.vaadin.flow.server.communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.communication.AtmospherePushConnection.State;
import com.vaadin.tests.util.MockUI;

/**
 * @author Vaadin Ltd
 * @since 1.0
 */
public class AtmospherePushConnectionTest {

    private MockVaadinSession vaadinSession;
    private Broadcaster broadcaster;
    private AtmosphereResource resource;
    private AtmospherePushConnection connection;

    @Before
    public void setup() throws Exception {
        vaadinSession = new MockVaadinSession();
        vaadinSession.lock();
        UI ui = new MockUI(vaadinSession);
        vaadinSession.unlock();
        broadcaster = Mockito.mock(Broadcaster.class);
        resource = Mockito.mock(AtmosphereResource.class);
        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);
        Mockito.doAnswer(i -> {
            // Introduce a small delay to hold the lock during disconnect
            Thread.sleep(30);
            return null;
        }).when(resource).close();
        Mockito.doAnswer(i -> {
            // Introduce a small delay to hold the lock during message push
            Thread.sleep(30);
            return CompletableFuture.completedFuture(null);
        }).when(broadcaster).broadcast(ArgumentMatchers.any(),
                ArgumentMatchers.any(AtmosphereResource.class));

        connection = new AtmospherePushConnection(ui);
        connection.connect(resource);
    }

    @Test
    public void testSerialization() throws Exception {

        UI ui = Mockito.mock(UI.class);
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);

        AtmospherePushConnection connection = new AtmospherePushConnection(ui);
        connection.connect(resource);

        Assert.assertEquals(State.CONNECTED, connection.getState());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        new ObjectOutputStream(baos).writeObject(connection);

        connection = (AtmospherePushConnection) new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray())).readObject();

        Assert.assertEquals(State.DISCONNECTED, connection.getState());
    }

    @Test
    public void pushWhileDisconnect_disconnectedWithoutSendingMessage()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try {
                vaadinSession.runWithLock(() -> {
                    connection.push();
                    return null;
                });
                latch.countDown();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, CompletableFuture.delayedExecutor(5, TimeUnit.MILLISECONDS))
                .exceptionally(error -> {
                    error.printStackTrace();
                    return null;
                });
        connection.disconnect();
        Assert.assertTrue("AtmospherePushConnection not disconnected",
                latch.await(2, TimeUnit.SECONDS));
        Assert.assertEquals(State.PUSH_PENDING, connection.getState());
        Mockito.verifyNoInteractions(broadcaster);
    }

    @Test
    public void disconnectWhilePush_messageSentAndThenDisconnected()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CompletableFuture.runAsync(() -> {
            try {
                vaadinSession.runWithLock(() -> {
                    CompletableFuture.runAsync(() -> {
                        connection.disconnect();
                        latch.countDown();
                    }, CompletableFuture.delayedExecutor(5,
                            TimeUnit.MILLISECONDS)).exceptionally(error -> {
                                error.printStackTrace();
                                return null;
                            });
                    connection.push();
                    return null;
                });
                latch.countDown();
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }).exceptionally(error -> {
            error.printStackTrace();
            return null;
        });

        Assert.assertTrue("Push not completed",
                latch.await(3, TimeUnit.SECONDS));
        Mockito.verify(broadcaster).broadcast(ArgumentMatchers.any(),
                ArgumentMatchers.eq(resource));
    }

    @Test
    public void disconnect_concurrentRequests_preventDeadlocks()
            throws Exception {
        // A deadlock may happen when an HTTP session is invalidated in a
        // thread, causing VaadinSession and UIs to be closed and push
        // connections to be disconnected, but a push disconnection is
        // concurrently requested by another thread.
        // This happens for example with MPR and Vaadin 8: when HTTP session is
        // invalidated Flow closes UI and disconnects PUSH. At the same time V8
        // VaadinSession is nullified on the V8 UI, that in turn spawns a thread
        // to disconnect PUSH connection.
        // So, there are two concurrent calls to disconnect; one of those
        // acquires the AtmospherePushConnection lock and the other one waits
        // for it to be released.
        // Unfortunately, it may happen that HTTP session operation also require
        // locks.
        // In the above case, a lock is held by the thread that is performing
        // session invalidation, but when AtmospherePushConnection.disconnect is
        // invoked on the other thread, it calls AtmosphereResource.close that
        // tries to access the HTTP session attributes, but it gets stuck
        // waiting for the HTTP session lock to be released.
        // At the end, there's a thread holding the AtmosphereConnection lock
        // unable to release it because it cannot complete disconnection due to
        // the HTTP session lock, that is however hold by the other thread that
        // is waiting for AtmosphereConnection lock to be released.
        ReentrantLock sessionLock = new ReentrantLock();
        Mockito.doAnswer(i -> {
            // simulate HTTP session lock attempt because of atmosphere resource
            // accesses session attributes
            // It does not wait indefinitely, but triggers an error if the lock
            // is held by the main thread
            if (sessionLock.tryLock(2, TimeUnit.SECONDS)) {
                sessionLock.unlock();
            } else {
                throw new AssertionError(
                        "Deadlock on AtmosphereResource.close");
            }
            return null;
        }).when(resource).close();

        CountDownLatch latch = new CountDownLatch(2);
        sessionLock.lock();
        CompletableFuture<Throwable> threadErrorFuture;
        try {
            // Simulate PUSH disconnection from a separate thread
            threadErrorFuture = CompletableFuture
                    .<Throwable> supplyAsync(() -> {
                        connection.disconnect();
                        latch.countDown();
                        return null;
                    }).exceptionally(t -> {
                        if (t instanceof CompletionException) {
                            return t.getCause();
                        }
                        return t;
                    });
            // Simulate main thread PUSH disconnection because of session
            // invalidation, delayed a bit to allow the other thread to start
            // disconnection
            Thread.sleep(1);
            connection.disconnect();
            latch.countDown();
        } finally {
            sessionLock.unlock();
        }

        Throwable threadError = threadErrorFuture.get(2, TimeUnit.SECONDS);
        if (threadError != null) {
            StringWriter sw = new StringWriter();
            threadError.printStackTrace(new PrintWriter(sw));
            Assert.fail("Disconnection on spawned thread failed: " + sw);
        }
        Assert.assertTrue("Disconnect calls not completed, missing "
                + latch.getCount() + " call", latch.await(3, TimeUnit.SECONDS));
        Mockito.verify(resource, Mockito.times(1)).close();
    }

    @Test
    public void pushWhileDisconnect_preventDeadlocks() throws Exception {
        // Similar motivation exposed in
        // disconnect_concurrentRequests_preventDeadlocks
        // but when a Vaadin session is unlocked as a consequence of HTTP
        // session invalidation
        ReentrantLock httpSessionLock = new ReentrantLock();
        Mockito.doAnswer(i -> {
            // simulate HTTP session lock attempt because of atmosphere resource
            // accesses session attributes
            // It does not wait indefinitely, but triggers an error if the lock
            // is held by the main thread
            if (httpSessionLock.tryLock(2, TimeUnit.SECONDS)) {
                httpSessionLock.unlock();
            } else {
                throw new AssertionError(
                        "Deadlock on AtmosphereResource.close");
            }
            return null;
        }).when(resource).close();

        CountDownLatch latch = new CountDownLatch(2);
        httpSessionLock.lock();
        CompletableFuture<Throwable> threadErrorFuture;
        try {
            // Simulate PUSH disconnection from a separate thread
            threadErrorFuture = CompletableFuture
                    .<Throwable> supplyAsync(() -> {
                        connection.disconnect();
                        latch.countDown();
                        return null;
                    }).exceptionally(t -> {
                        if (t instanceof CompletionException) {
                            return t.getCause();
                        }
                        return t;
                    });
            // Simulate main thread PUSH disconnection because of session
            // invalidation, delayed a bit to allow the other thread to start
            // disconnection
            Thread.sleep(1);
            vaadinSession.access(() -> {
                connection.push();
            });
            latch.countDown();
        } finally {
            httpSessionLock.unlock();
        }

        Throwable threadError = threadErrorFuture.get(2, TimeUnit.SECONDS);
        if (threadError != null) {
            Assert.fail("Disconnection on spawned thread failed: "
                    + threadError.getMessage());
        }
        Assert.assertTrue("Disconnect calls not completed, missing "
                + latch.getCount() + " call", latch.await(3, TimeUnit.SECONDS));
        Mockito.verify(resource, Mockito.times(1)).close();
    }

}
