/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.communication.AtmospherePushConnection.State;

/**
 * @author Vaadin Ltd
 * @since 1.0
 */
public class AtmospherePushConnectionTest {
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

        UI ui = Mockito.spy(new UI());
        MockVaadinSession vaadinSession = new MockVaadinSession();
        Mockito.when(ui.getSession()).thenReturn(vaadinSession);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);

        AtmospherePushConnection connection = new AtmospherePushConnection(ui);
        connection.connect(resource);

        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try {
                vaadinSession.runWithLock(() -> {
                    connection.push();
                    return null;
                });
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                latch.countDown();
            }
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

        UI ui = Mockito.spy(new UI());
        MockVaadinSession vaadinSession = new MockVaadinSession();
        Mockito.when(ui.getSession()).thenReturn(vaadinSession);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);

        AtmospherePushConnection connection = new AtmospherePushConnection(ui);
        connection.connect(resource);

        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try {
                vaadinSession.runWithLock(() -> {
                    CompletableFuture.runAsync(connection::disconnect);
                    connection.push();
                    return null;
                });
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            } finally {
                latch.countDown();
            }
        });
        Assert.assertTrue("Push not completed",
                latch.await(2, TimeUnit.SECONDS));
        Mockito.verify(broadcaster).broadcast(ArgumentMatchers.any(),
                ArgumentMatchers.eq(resource));
    }

}
