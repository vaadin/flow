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

import com.vaadin.flow.server.connect.Export;

public class ConnectExportsValidatorTest {

    @Export
    public static class WithConnectExport {
    }

    public static class WithoutConnectExport {
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
        ConnectExportsValidator validator = new ConnectExportsValidator();
        classes.add(WithConnectExport.class);
        validator.onStartup(classes, servletContext);
    }

    @Test
    public void should_trow_when_spring_not_in_classpath() throws Exception {
        exception.expect(ServletException.class);
        ConnectExportsValidator validator = new ConnectExportsValidator();
        validator.setClassToCheck("foo.bar.Baz");
        classes.add(WithConnectExport.class);
        validator.onStartup(classes, servletContext);

    }

    @Test
    public void should_start_when_no_exports_and_spring_not_in_classpath()
            throws Exception {
        ConnectExportsValidator validator = new ConnectExportsValidator();
        classes.add(WithoutConnectExport.class);
        validator.onStartup(classes, servletContext);
    }

    @Test
    public void should_start_when_CDI_environment()
            throws Exception {
        ConnectExportsValidator validator = new ConnectExportsValidator();
        classes = null;
        validator.onStartup(classes, servletContext);
    }
}
