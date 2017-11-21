package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.vaadin.shared.ui.Transport;

public class PushConfigurationMapTest
        extends AbstractNodeFeatureTest<PushConfigurationMap> {

    private PushConfigurationMap ns = createFeature();

    @Test
    public void transportWebsocket() {
        ns.setTransport(Transport.WEBSOCKET);
        assertEquals(Transport.WEBSOCKET.getIdentifier(),
                ns.getParameter("transport"));
        assertFalse(
                ns.contains(PushConfigurationMap.ALWAYS_USE_XHR_TO_SERVER));
        assertEquals(Transport.WEBSOCKET, ns.getTransport());
    }

    @Test
    public void transportLongPolling() {
        ns.setTransport(Transport.LONG_POLLING);
        assertEquals(Transport.LONG_POLLING.getIdentifier(),
                ns.getParameter("transport"));
        assertFalse(
                ns.contains(PushConfigurationMap.ALWAYS_USE_XHR_TO_SERVER));
        assertEquals(Transport.LONG_POLLING, ns.getTransport());
    }

    @Test
    public void transportLongWebsocketXHR() {
        ns.setTransport(Transport.WEBSOCKET_XHR);
        assertEquals(Transport.WEBSOCKET.getIdentifier(),
                ns.getParameter("transport"));
        assertTrue((Boolean) ns
                .get(PushConfigurationMap.ALWAYS_USE_XHR_TO_SERVER));
        assertEquals(Transport.WEBSOCKET_XHR, ns.getTransport());
    }

    @Test
    public void parameterNames() {
        ns.setParameter("foo", "bar");
        assertArrayEquals(new String[] { "foo" },
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
        assertArrayEquals(expected, actual);
    }

}
