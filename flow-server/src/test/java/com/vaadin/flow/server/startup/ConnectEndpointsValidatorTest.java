package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.server.connect.Endpoint;

public class ConnectEndpointsValidatorTest {

    @Endpoint
    public static class WithConnectEndpoint {
    }

    public static class WithoutConnectEndpoint {
    }

    private Set<Class<?>> classes;
    private VaadinContext vaadinContext;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        classes = new HashSet<Class<?>>();
        vaadinContext = new VaadinServletContext(Mockito.mock(ServletContext.class));
    }

    @Test
    public void should_start_when_spring_in_classpath() throws Exception {
        ConnectEndpointsValidator validator = new ConnectEndpointsValidator();
        classes.add(WithConnectEndpoint.class);
        validator.process(classes, vaadinContext);
    }

    @Test
    public void should_throw_when_spring_not_in_classpath() throws Exception {
        exception.expect(VaadinInitializerException.class);
        ConnectEndpointsValidator validator = new ConnectEndpointsValidator();
        validator.setClassToCheck("foo.bar.Baz");
        classes.add(WithConnectEndpoint.class);
        validator.process(classes, vaadinContext);

    }

    @Test
    public void should_start_when_no_endpoints_and_spring_not_in_classpath()
            throws Exception {
        ConnectEndpointsValidator validator = new ConnectEndpointsValidator();
        classes.add(WithoutConnectEndpoint.class);
        validator.process(classes, vaadinContext);
    }

    @Test
    public void should_start_when_CDI_environment() throws Exception {
        ConnectEndpointsValidator validator = new ConnectEndpointsValidator();
        classes = null;
        validator.process(classes, vaadinContext);
    }
}
