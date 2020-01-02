package com.vaadin.flow.shared;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.WebBrowser;

import static org.junit.Assert.assertEquals;

public class VaadinUriResolverTest {

    private final class TestVaadinUriResolver extends VaadinUriResolver {
        public String resolveVaadinUri(String uri) {
            String frontendUrl;
            frontendUrl = "context://es6/";
            return super.resolveVaadinUri(uri, frontendUrl,
                    "http://someplace/");
        }
    }

    private WebBrowser browser;

    @Test
    public void testProtocolChain() {
        browser = Mockito.mock(WebBrowser.class);

        TestVaadinUriResolver resolver = new TestVaadinUriResolver();

        assertEquals("http://someplace/es6/my-component.html",
                resolver.resolveVaadinUri("frontend://my-component.html"));
    }

    private final class NullContextVaadinUriResolver extends VaadinUriResolver {
        public String resolveVaadinUri(String uri) {
            return super.resolveVaadinUri(uri, "http://someplace/", null);
        }
    }

    private final class NullFrontendVaadinUriResolver
            extends VaadinUriResolver {
        public String resolveVaadinUri(String uri) {
            return super.resolveVaadinUri(uri, null, "http://someplace/");
        }
    }

    @Test
    public void testFrontendProtocol() {
        NullContextVaadinUriResolver resolver = new NullContextVaadinUriResolver();
        assertEquals("http://someplace/my-component.html",
                resolver.resolveVaadinUri("frontend://my-component.html"));
    }

    @Test
    public void testContextProtocol() {
        NullFrontendVaadinUriResolver resolver = new NullFrontendVaadinUriResolver();
        assertEquals("http://someplace/my-component.html",
                resolver.resolveVaadinUri("context://my-component.html"));
    }

}
