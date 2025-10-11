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

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.AtmosphereResourceSessionFactory;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction;
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction.ACTION;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterCache;
import org.atmosphere.cpr.BroadcasterConfig;
import org.atmosphere.cpr.DefaultAtmosphereResourceSessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class LongPollingCacheFilterTest {

    static final String RESOURCE_UUID = "resourceUUID";
    LongPollingCacheFilter filter = new LongPollingCacheFilter();

    AtmospherePushConnection.PushMessage originalMessage = new AtmospherePushConnection.PushMessage(
            5, "PUSH ME");
    Object nonPushMessage = new Object();
    Object message = new Object();
    private AtmosphereResource resource;
    private BroadcasterCache cache;
    private AtmosphereResourceSessionFactory sessionFactory;

    @Test
    public void filter_notPushMessage_continueWithCurrentMessage() {
        setTransport(AtmosphereResource.TRANSPORT.LONG_POLLING);
        setSeenServerSyncIdHeader(5);
        simulatePushConnection();
        BroadcastAction action = filter.filter("broadcasterId", resource,
                nonPushMessage, message);
        Assert.assertEquals(ACTION.CONTINUE, action.action());
        Assert.assertSame("Message should not be altered by filter", message,
                action.message());
        verifyMessageIsNotCached();
    }

    @Test
    public void filter_notLongPollingTransport_continueWithCurrentMessage() {
        setSeenServerSyncIdHeader(5);
        simulatePushConnection();
        Stream.of(AtmosphereResource.TRANSPORT.values())
                .filter(t -> t != AtmosphereResource.TRANSPORT.LONG_POLLING)
                .forEach(transport -> {
                    setTransport(transport);
                    BroadcastAction action = filter.filter("broadcasterId",
                            resource, originalMessage, message);
                    Assert.assertEquals(ACTION.CONTINUE, action.action());
                    Assert.assertSame(
                            "Message should not be altered by filter when transport is "
                                    + transport,
                            message, action.message());
                });
        verifyMessageIsNotCached();
    }

    @Test
    public void filter_syncIdCheckDisabled_continueWithCurrentMessage() {
        setTransport(AtmosphereResource.TRANSPORT.LONG_POLLING);
        setSeenServerSyncIdHeader(-1);
        BroadcastAction action = filter.filter("broadcasterId", resource,
                originalMessage, message);
        Assert.assertEquals(ACTION.CONTINUE, action.action());
        Assert.assertSame(
                "Message should not be altered by filter if syncId check is disabled",
                message, action.message());
        verifyMessageIsNotCached();
    }

    @Test
    public void filter_missingLastSeenServerSyncId_continueWithCurrentMessage() {
        setTransport(AtmosphereResource.TRANSPORT.LONG_POLLING);
        simulatePushConnection();
        BroadcastAction action = filter.filter("broadcasterId", resource,
                originalMessage, message);
        Assert.assertEquals(ACTION.CONTINUE, action.action());
        Assert.assertSame(
                "Message should not be altered by filter if server sync id header is missing",
                message, action.message());
        verifyMessageIsNotCached();
    }

    @Test
    public void filter_messageAlreadySeen_abort() {
        setTransport(AtmosphereResource.TRANSPORT.LONG_POLLING);
        setSeenServerSyncIdHeader(5, 6);
        simulatePushConnection();

        // seen server sync id == push message server sync id
        BroadcastAction action = filter.filter("broadcasterId", resource,
                originalMessage, message);
        Assert.assertEquals("Expecting message seen on client to be skipped",
                ACTION.ABORT, action.action());
        Assert.assertSame(
                "Message should not be altered by filter when aborting",
                message, action.message());

        // seen server sync id > push message server sync id
        simulatePushConnection();
        action = filter.filter("broadcasterId", resource, originalMessage,
                message);
        Assert.assertEquals("Expecting message seen on client to be skipped",
                ACTION.ABORT, action.action());
        Assert.assertSame(
                "Message should not be altered by filter when aborting",
                message, action.message());
        verifyMessageIsNotCached();
    }

    @Test
    public void filter_messageNotYetSeen_addToCacheAndContinue() {
        setTransport(AtmosphereResource.TRANSPORT.LONG_POLLING);
        setSeenServerSyncIdHeader(2);
        simulatePushConnection();
        String broadcasterId = "broadcasterId";
        BroadcastAction action = filter.filter(broadcasterId, resource,
                originalMessage, message);
        Assert.assertEquals("Expecting message not seen on client to be sent",
                ACTION.CONTINUE, action.action());
        Assert.assertSame(
                "Message should not be altered by filter when continuing",
                message, action.message());
        Mockito.verify(cache).addToCache(ArgumentMatchers.eq(broadcasterId),
                ArgumentMatchers.eq(RESOURCE_UUID),
                ArgumentMatchers.argThat(m -> m.message() == originalMessage));
    }

    @Test
    public void onConnect_longPollingAndSeenServerSyncIdHeaderSent_sessionAttributeStored() {
        int syncId = 5;
        setTransport(AtmosphereResource.TRANSPORT.LONG_POLLING);
        setSeenServerSyncIdHeader(syncId);
        simulatePushConnection();

        AtmosphereResourceSession session = sessionFactory.getSession(resource,
                false);
        Assert.assertNotNull(
                "Expecting AtmosphereResourceSession to exist, but was not created",
                session);
        Assert.assertEquals(session.getAttribute(
                LongPollingCacheFilter.SEEN_SERVER_SYNC_ID), syncId);
    }

    @Test
    public void onConnect_seenServerSyncIdHeaderMissing_sessionAttributeNotSet() {
        simulatePushConnection();

        AtmosphereResourceSession session = sessionFactory.getSession(resource,
                false);
        Assert.assertNull(
                "AtmosphereResourceSession exist, but was server sync id was not sent",
                session);
    }

    @Test
    public void onConnect_notLongPollingTransport_sessionAttributeNotSet() {
        setSeenServerSyncIdHeader(5);
        AtmosphereResourceSession session = sessionFactory.getSession(resource,
                false);

        Stream.of(AtmosphereResource.TRANSPORT.values())
                .filter(t -> t != AtmosphereResource.TRANSPORT.LONG_POLLING)
                .forEach(transport -> {
                    setTransport(transport);
                    simulatePushConnection();
                    Assert.assertNull(
                            "AtmosphereResourceSession exist, but transport is not LONG POLLING",
                            session);
                });

    }

    @Before
    public void setUp() {
        resource = Mockito.mock(AtmosphereResource.class);
        AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        BroadcasterConfig broadcasterConfig = Mockito
                .mock(BroadcasterConfig.class);
        cache = Mockito.mock(BroadcasterCache.class);
        Mockito.when(broadcaster.getBroadcasterConfig())
                .thenReturn(broadcasterConfig);
        Mockito.when(broadcasterConfig.getBroadcasterCache()).thenReturn(cache);

        sessionFactory = new DefaultAtmosphereResourceSessionFactory();
        AtmosphereConfig config = Mockito.mock(AtmosphereConfig.class);
        Mockito.when(config.sessionFactory()).thenReturn(sessionFactory);

        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);
        Mockito.when(resource.getRequest()).thenReturn(request);
        Mockito.when(resource.uuid()).thenReturn(RESOURCE_UUID);
        Mockito.when(resource.getAtmosphereConfig()).thenReturn(config);
    }

    private void setTransport(AtmosphereResource.TRANSPORT transport) {
        Mockito.when(resource.transport()).thenReturn(transport);
    }

    private void setSeenServerSyncIdHeader(int id, int... ids) {
        Mockito.when(resource.getRequest()
                .getHeader(LongPollingCacheFilter.SEEN_SERVER_SYNC_ID))
                .thenReturn(Integer.toString(id), IntStream.of(ids)
                        .mapToObj(Integer::toString).toArray(String[]::new));
    }

    private void simulatePushConnection() {
        LongPollingCacheFilter.onConnect(resource);
    }

    private void verifyMessageIsNotCached() {
        Mockito.verifyNoInteractions(cache);
    }
}
