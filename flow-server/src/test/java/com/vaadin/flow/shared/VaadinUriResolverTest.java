package com.vaadin.flow.shared;

import org.junit.Test;

import com.vaadin.flow.server.WebBrowser;

import static org.junit.Assert.assertEquals;

public class VaadinUriResolverTest {

    private final class TestVaadinUriResolver extends VaadinUriResolver {
        public String resolveVaadinUri(String uri) {
            return super.resolveVaadinUri(uri, "http://someplace/");
        }
    }

    private WebBrowser browser;

    private final class ContextVaadinUriResolver extends VaadinUriResolver {
        public String resolveVaadinUri(String uri) {
            return super.resolveVaadinUri(uri, "http://someplace/");
        }
    }

    @Test
    public void testContextProtocol() {
        ContextVaadinUriResolver resolver = new ContextVaadinUriResolver();
        assertEquals("http://someplace/my-component.html",
                resolver.resolveVaadinUri("context://my-component.html"));
    }

}
