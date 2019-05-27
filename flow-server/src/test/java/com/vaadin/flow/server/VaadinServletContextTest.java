package com.vaadin.flow.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

/**
 * @author miki
 * @since 2019-05-27
 */
public class VaadinServletContextTest {
    
    private static String testAttributeProvider() {
        return "RELAX_THIS_IS_A_TEST";
    }
    
    private VaadinServletContext context;
    
    private final Map<String, Object> attributeMap = new HashMap<>();

    @Before
    public void setup() {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(Mockito.anyString())).then(invocationOnMock -> attributeMap.get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
            invocationOnMock.getArguments()[0].toString(),
            invocationOnMock.getArguments()[1]
            )).when(servletContext).setAttribute(Mockito.anyString(), Mockito.any());
        
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
}
