package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for VaadinServletContext attribute storage and property delegation.
 *
 * @since 2.0.0
 */
public class VaadinServletContextTest {
    
    private static String testAttributeProvider() {
        return "RELAX_THIS_IS_A_TEST";
    }
    
    private VaadinServletContext context;
    
    private final Map<String, Object> attributeMap = new HashMap<>();
    private Map<String, String> properties;

    @Before
    public void setup() {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(Mockito.anyString())).then(invocationOnMock -> attributeMap.get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
            invocationOnMock.getArguments()[0].toString(),
            invocationOnMock.getArguments()[1]
            )).when(servletContext).setAttribute(Mockito.anyString(), Mockito.any());

        properties = new HashMap<>();
        properties.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE, "false");
        properties.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, "true");
        properties.put(Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER, "false");

        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.enumeration(properties.keySet()));
        Mockito.when(servletContext.getInitParameter(Mockito.anyString()))
                .then(invocation -> properties
                        .get(invocation.getArguments()[0]));
        context = new VaadinServletContext(servletContext);
    }

    @Test
    public void getAttributeWithProvider() {
        Assert.assertNull(context.getAttribute(String.class));

        String value = context.getAttribute(String.class,
            VaadinServletContextTest::testAttributeProvider);
        Assert.assertEquals(testAttributeProvider(), value);

        Assert.assertEquals("Value from provider should be persisted",
            testAttributeProvider(), context.getAttribute(String.class));
    }

    @Test(expected = AssertionError.class)
    public void setNullAttributeNotAllowed() {
        context.setAttribute(null);
    }

    @Test
    public void getMissingAttributeWithoutProvider() {
        String value = context.getAttribute(String.class);
        Assert.assertNull(value);
    }

    @Test
    public void setAndGetAttribute() {
        String value = testAttributeProvider();
        context.setAttribute(value);
        String result = context.getAttribute(String.class);
        Assert.assertEquals(value, result);
        // overwrite
        String newValue = "this is a new value";
        context.setAttribute(newValue);
        result = context.getAttribute(String.class);
        Assert.assertEquals(newValue, result);
        // now the provider should not be called, so value should be still there
        result = context.getAttribute(String.class,
            () -> {
                throw new AssertionError("Should not be called");
            });
        Assert.assertEquals(newValue, result);
    }

    @Test
    public void getPropertyNames_returnsExpectedProperties() {
        List<String> list = Collections.list(context.getContextParameterNames());
        Assert.assertEquals(
                "Context should return only keys defined in ServletContext",
                properties.size(), list.size());
        for (String key : properties.keySet()) {
            Assert.assertEquals(String.format(
                    "Value should be same from context for key '%s'", key),
                    properties.get(key), context.getContextParameter(key));
        }
    }
}
