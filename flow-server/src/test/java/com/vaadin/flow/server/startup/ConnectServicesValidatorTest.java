package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.server.connect.VaadinService;

public class ConnectServicesValidatorTest {

    @VaadinService
    public static class WithConnectService {
    }

    public static class WithoutConnectService {
    }

    private Set<Class<?>> classes;
    private ServletContext servletContext;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        classes = new HashSet<Class<?>>();
        servletContext = Mockito.mock(ServletContext.class);
    }

    @Test
    public void should_start_when_spring_in_classpath() throws Exception {
        ConnectServicesValidator validator = new ConnectServicesValidator();
        classes.add(WithConnectService.class);
        validator.onStartup(classes, servletContext);
    }

    @Test
    public void should_trow_when_spring_not_in_classpath() throws Exception {
        exception.expect(ServletException.class);
        ConnectServicesValidator validator = new ConnectServicesValidator();
        validator.setClassToCheck("foo.bar.Baz");
        classes.add(WithConnectService.class);
        validator.onStartup(classes, servletContext);

    }

    @Test
    public void should_start_when_no_services_and_spring_not_in_classpath()
            throws Exception {
        ConnectServicesValidator validator = new ConnectServicesValidator();
        classes.add(WithoutConnectService.class);
        validator.onStartup(classes, servletContext);
    }

    @Test
    public void should_start_when_CDI_environment()
            throws Exception {
        ConnectServicesValidator validator = new ConnectServicesValidator();
        classes = null;
        validator.onStartup(classes, servletContext);
    }
}
