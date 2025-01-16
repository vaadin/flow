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

import java.io.Serializable;
import java.util.Optional;

import org.atmosphere.cache.BroadcastMessage;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.BroadcasterCache;
import org.atmosphere.cpr.PerRequestBroadcastFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link PerRequestBroadcastFilter} implementation that handles
 * {@link com.vaadin.flow.server.communication.AtmospherePushConnection.PushMessage}s
 * to ensure that a message is preserved in the {@link BroadcasterCache} until
 * the client has received it.
 *
 * The filter acts only on {@literal LONG POLLING} transport and expects that
 * the client sends the {@literal X-Vaadin-LastSeenServerSyncId} header with the
 * identifier of the last message seen, every time the connection is
 * established.
 *
 * Messages already seen are discarded, whereas messages not yet sent to the
 * client are added again to the cache to preserve them until client confirms
 * reception by sending the last seen message identifier.
 */
public class LongPollingCacheFilter
        implements PerRequestBroadcastFilter, Serializable {
    public static final String SEEN_SERVER_SYNC_ID = "X-Vaadin-LastSeenServerSyncId";

    private static Logger getLogger() {
        return LoggerFactory.getLogger(LongPollingCacheFilter.class.getName());
    }

    static void onConnect(AtmosphereResource resource) {
        Integer syncId = Optional
                .ofNullable(
                        resource.getRequest().getHeader(SEEN_SERVER_SYNC_ID))
                .map(Integer::parseInt).orElse(null);
        if (resource.transport() == AtmosphereResource.TRANSPORT.LONG_POLLING
                && syncId != null) {
            AtmosphereResourceSession session = resource.getAtmosphereConfig()
                    .sessionFactory().getSession(resource);
            session.setAttribute(SEEN_SERVER_SYNC_ID, syncId);
        }
    }

    @Override
    public BroadcastAction filter(String broadcasterId, AtmosphereResource r,
            Object originalMessage, Object message) {
        AtmosphereResourceSession session = r.getAtmosphereConfig()
                .sessionFactory().getSession(r, false);
        if (originalMessage instanceof AtmospherePushConnection.PushMessage
                && r.transport() == AtmosphereResource.TRANSPORT.LONG_POLLING
                && session != null
                && session.getAttribute(SEEN_SERVER_SYNC_ID) != null) {
            AtmospherePushConnection.PushMessage pushMessage = (AtmospherePushConnection.PushMessage) originalMessage;
            String uuid = r.uuid();
            int lastSeenOnClient = session.getAttribute(SEEN_SERVER_SYNC_ID,
                    Integer.class);
            if (lastSeenOnClient == -1) {
                return new BroadcastAction(BroadcastAction.ACTION.CONTINUE,
                        message);
            } else if (pushMessage.alreadySeen(lastSeenOnClient)) {
                getLogger().trace(
                        "Discarding message {} for resource {} as client already seen {}. {}",
                        pushMessage.serverSyncId, uuid, lastSeenOnClient,
                        pushMessage.message);
                // Client has already seen this message, discard it
                return new BroadcastAction(BroadcastAction.ACTION.ABORT,
                        message);
            } else {
                // In rare cases with long polling, message may be lost during
                // write operation and the client may never receive it.
                // To prevent this kind of issues we move the message back to
                // the cache until we get confirmation that the message has been
                // seen
                getLogger().trace(
                        "Put message {} for resource {} back to the cache because it may not have reached the client, as the last seen message is {}. {}",
                        pushMessage.serverSyncId, uuid, lastSeenOnClient,
                        pushMessage.message);
                BroadcasterCache cache = r.getBroadcaster()
                        .getBroadcasterConfig().getBroadcasterCache();
                cache.addToCache(broadcasterId, uuid,
                        new BroadcastMessage(originalMessage));
            }
        }
        return new BroadcastAction(message);
    }

    @Override
    public BroadcastAction filter(String broadcasterId, Object originalMessage,
            Object message) {
        return new BroadcastAction(message);
    }

}
