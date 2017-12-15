package com.vaadin.shared;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.WebBrowser;

public class VaadinUriResolverTest {

    private VaadinUriResolver resolver;
    private WebBrowser browser;

    @Test
    public void testProtocolChain() {
        browser = Mockito.mock(WebBrowser.class);

        resolver = new VaadinUriResolver() {
            @Override
            protected String getFrontendRootUrl() {
                if (browser.isEs6Supported()) {
                    return "context://es6/";
                }
                return "context://es5/";
            }

            @Override
            protected String getContextRootUrl() {
                return "http://someplace/";
            }
        };

        Mockito.when(browser.isEs6Supported()).thenReturn(true);
        assertEquals("http://someplace/es6/my-component.html",
                resolver.resolveVaadinUri("frontend://my-component.html"));
        Mockito.when(browser.isEs6Supported()).thenReturn(false);
        assertEquals("http://someplace/es5/my-component.html",
                resolver.resolveVaadinUri("frontend://my-component.html"));
    }

    @Test
    public void testFrontendProtocol() {
        resolver = new VaadinUriResolver() {
            @Override
            protected String getFrontendRootUrl() {
                return "http://someplace/";
            }

            @Override
            protected String getContextRootUrl() {
                return null;
            }
        };

        assertEquals("http://someplace/my-component.html",
                resolver.resolveVaadinUri("frontend://my-component.html"));
    }

    @Test
    public void testContextProtocol() {
        resolver = new VaadinUriResolver() {
            @Override
            protected String getFrontendRootUrl() {
                return null;
            }

            @Override
            protected String getContextRootUrl() {
                return "http://someplace/";
            }
        };

        assertEquals("http://someplace/my-component.html",
                resolver.resolveVaadinUri("context://my-component.html"));
    }

}
