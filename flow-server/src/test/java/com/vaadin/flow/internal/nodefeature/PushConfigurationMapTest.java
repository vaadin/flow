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
package com.vaadin.flow.internal.nodefeature;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.shared.ui.Transport;

public class PushConfigurationMapTest
        extends AbstractNodeFeatureTest<PushConfigurationMap> {

    private PushConfigurationMap ns = createFeature();

    @Test
    public void transportWebsocket() {
        ns.setTransport(Transport.WEBSOCKET);
        Assert.assertEquals(Transport.WEBSOCKET.getIdentifier(),
                ns.getParameter("transport"));
        Assert.assertFalse(
                ns.contains(PushConfigurationMap.ALWAYS_USE_XHR_TO_SERVER));
        Assert.assertEquals(Transport.WEBSOCKET, ns.getTransport());
    }

    @Test
    public void transportLongPolling() {
        ns.setTransport(Transport.LONG_POLLING);
        Assert.assertEquals(Transport.LONG_POLLING.getIdentifier(),
                ns.getParameter("transport"));
        Assert.assertFalse(
                ns.contains(PushConfigurationMap.ALWAYS_USE_XHR_TO_SERVER));
        Assert.assertEquals(Transport.LONG_POLLING, ns.getTransport());
    }

    @Test
    public void transportLongWebsocketXHR() {
        ns.setTransport(Transport.WEBSOCKET_XHR);
        Assert.assertEquals(Transport.WEBSOCKET.getIdentifier(),
                ns.getParameter("transport"));
        Assert.assertTrue((Boolean) ns
                .get(PushConfigurationMap.ALWAYS_USE_XHR_TO_SERVER));
        Assert.assertEquals(Transport.WEBSOCKET_XHR, ns.getTransport());
    }

    @Test
    public void fallbackTransportLongPolling() {
        ns.setFallbackTransport(Transport.LONG_POLLING);
        Assert.assertEquals(Transport.LONG_POLLING.getIdentifier(),
                ns.getParameter("fallbackTransport"));
        Assert.assertEquals(Transport.LONG_POLLING, ns.getFallbackTransport());
    }

    @Test
    public void fallbackTransportWebsocket() {
        ns.setFallbackTransport(Transport.WEBSOCKET);
        Assert.assertEquals(Transport.WEBSOCKET.getIdentifier(),
                ns.getParameter("fallbackTransport"));
        Assert.assertEquals(Transport.WEBSOCKET, ns.getFallbackTransport());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fallbackTransportWebsocketXHR() {
        ns.setFallbackTransport(Transport.WEBSOCKET_XHR);
    }

    @Test
    public void parameterNames() {
        ns.setParameter("foo", "bar");
        Assert.assertArrayEquals(new String[] { "foo" },
                ns.getParameterNames().toArray());

        ns.setTransport(Transport.WEBSOCKET);
        ns.setFallbackTransport(Transport.LONG_POLLING);

        String[] expected = new String[] { "foo", "transport",
                "fallbackTransport" };
        Collection<String> paramNames = ns.getParameterNames();
        String[] actual = paramNames.toArray(new String[paramNames.size()]);

        // getParmeterNames does not guarantee order
        Arrays.sort(expected);
        Arrays.sort(actual);
        Assert.assertArrayEquals(expected, actual);
    }

}
